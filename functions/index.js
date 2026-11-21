const { setGlobalOptions } = require('firebase-functions/v2');
const { defineSecret } = require('firebase-functions/params');
const { onCall, onRequest, HttpsError } = require('firebase-functions/v2/https');
const { onSchedule } = require('firebase-functions/v2/scheduler');
const crypto = require('crypto');
const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { getDatabase } = require('firebase-admin/database');

const adminInitialPasswordSecret = defineSecret('BIGGBOSS_ADMIN_INITIAL_PASSWORD');

function getInitialAdminPasswordSecret() {
  try {
    const secretVal = String(adminInitialPasswordSecret.value() || '').trim();
    if (secretVal) return secretVal;
  } catch (_) {
    // Fallback to process.env for local emulators, .env files, or test harnesses.
  }
  return String(process.env.BIGGBOSS_ADMIN_INITIAL_PASSWORD || '').trim();
}

setGlobalOptions({ region: 'us-central1' });
initializeApp();

const DATABASE_ID = 'ai-studio-android-glasshou-6f63f3be-6956-4275-928c-87e38913e873';
const MAX_SEASON_PLAYERS = 15;

function db() {
  return getFirestore(DATABASE_ID);
}

async function isTrustedAdmin(uid, authToken) {
  if (!uid) return false;
  const firestore = db();
  const adminSnap = await firestore.collection('admins').doc(uid).get();
  if (!adminSnap.exists && !(authToken && authToken.admin === true)) {
    return false;
  }
  if (authToken && authToken.adminExpiresAt && Number(authToken.adminExpiresAt) <= Date.now()) {
    return false;
  }
  if (authToken && authToken.adminSessionId) {
    const sessSnap = await firestore.collection('adminSessions').doc(uid).get();
    if (!sessSnap.exists) return false;
    const sess = sessSnap.data() || {};
    if (sess.sessionId !== authToken.adminSessionId) return false;
    if (sess.status !== 'ACTIVE') return false;
    if (Number(sess.expiresAtMillis || 0) <= Date.now()) return false;
  }
  return true;
}

async function requireAdmin(request) {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required.');
  }
  const ok = await isTrustedAdmin(request.auth.uid, request.auth.token);
  if (!ok) {
    throw new HttpsError('permission-denied', 'Access denied: ADMIN privileges required.');
  }
  return request.auth.uid;
}

/**
 * Registers or updates a player's profile while atomically enforcing unique displayName.
 * Always assigns default role PLAYER (never elevates privilege).
 */
exports.registerHousemateProfile = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required.');
  }
  const uid = request.auth.uid;
  const rawName = String(request.data?.displayName || '').trim();
  const characterId = String(request.data?.characterId || 'Player 01').trim();
  const seasonId = String(request.data?.seasonId || 'season_01').trim();

  if (rawName.length < 3 || rawName.length > 24) {
    throw new HttpsError('invalid-argument', 'Display name must be between 3 and 24 characters.');
  }
  const normalizedName = rawName.toLowerCase().replace(/[^a-z0-9_-]/g, '_');
  if (normalizedName.length < 3) {
    throw new HttpsError('invalid-argument', 'Display name must include at least 3 letters or digits.');
  }

  const firestore = db();
  const userRef = firestore.collection('users').doc(uid);
  const nameRef = firestore.collection('displayNames').doc(normalizedName);

  return firestore.runTransaction(async (tx) => {
    const [userSnap, nameSnap, adminSnap] = await Promise.all([
      tx.get(userRef),
      tx.get(nameRef),
      tx.get(firestore.collection('admins').doc(uid))
    ]);

    if (userSnap.exists && userSnap.data().banned === true) {
      throw new HttpsError('permission-denied', 'This account has been banned by an administrator.');
    }

    if (nameSnap.exists && nameSnap.data().userId !== uid) {
      throw new HttpsError('already-exists', 'Display name is already taken by another housemate.');
    }

    const oldNorm = userSnap.exists ? userSnap.data().normalizedName : null;
    if (oldNorm && oldNorm !== normalizedName) {
      tx.delete(firestore.collection('displayNames').doc(oldNorm));
    }

    const playerId = userSnap.exists
      ? userSnap.data().playerId
      : `GH-${uid.substring(0, 6).toUpperCase()}`;
    const role = adminSnap.exists ? 'ADMIN' : 'PLAYER';

    tx.set(nameRef, {
      userId: uid,
      displayName: rawName,
      createdAt: FieldValue.serverTimestamp()
    });

    if (userSnap.exists) {
      tx.update(userRef, {
        displayName: rawName,
        normalizedName,
        characterId,
        seasonId,
        updatedAt: FieldValue.serverTimestamp()
      });
    } else {
      tx.set(userRef, {
        userId: uid,
        email: request.auth.token.email || null,
        displayName: rawName,
        normalizedName,
        playerId,
        characterId,
        role,
        seasonId,
        banned: false,
        evictedSeasons: 0,
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp()
      });
    }

    return { ok: true, playerId, role, displayName: rawName, normalizedName };
  });
});

/**
 * Joins a shared house season atomically, enforcing max 15 real players,
 * blocking banned or evicted players, and replacing duplicate session tokens.
 */
exports.joinHouseSeason = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required.');
  }
  const uid = request.auth.uid;
  const seasonId = String(request.data?.seasonId || 'season_01').trim();
  const characterId = String(request.data?.characterId || 'Player 01').trim();
  const sessionId = String(request.data?.sessionId || '').trim();

  if (!sessionId || sessionId.length < 6) {
    throw new HttpsError('invalid-argument', 'Valid sessionId required.');
  }

  const firestore = db();
  const seasonRef = firestore.collection('seasons').doc(seasonId);
  const membersCol = seasonRef.collection('members');
  const memberRef = membersCol.doc(uid);
  const userRef = firestore.collection('users').doc(uid);
  const adminRef = firestore.collection('admins').doc(uid);

  const result = await firestore.runTransaction(async (tx) => {
    const [seasonSnap, memberSnap, allMembersSnap, userSnap, adminSnap] = await Promise.all([
      tx.get(seasonRef),
      tx.get(memberRef),
      tx.get(membersCol),
      tx.get(userRef),
      tx.get(adminRef)
    ]);

    if (!userSnap.exists) {
      throw new HttpsError('failed-precondition', 'Create your housemate profile before joining.');
    }
    if (userSnap.data().banned === true) {
      throw new HttpsError('permission-denied', 'Account is banned from entering the Glass House.');
    }
    if (memberSnap.exists && (memberSnap.data().status === 'EVICTED' || memberSnap.data().status === 'BANNED')) {
      throw new HttpsError('permission-denied', 'Evicted or banned housemates cannot rejoin this season.');
    }

    const existingCount = allMembersSnap.size;
    if (!seasonSnap.exists) {
      tx.set(seasonRef, {
        title: 'Season 01 • Glass House',
        status: 'LIVE',
        maxPlayers: MAX_SEASON_PLAYERS,
        activeCount: 0,
        dayNightMode: 'day',
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp()
      });
    } else if (seasonSnap.data().status === 'ENDED') {
      throw new HttpsError('failed-precondition', 'This season has already ended.');
    }

    if (!memberSnap.exists && existingCount >= MAX_SEASON_PLAYERS) {
      throw new HttpsError('resource-exhausted', 'Season is full (15/15 real players).');
    }

    const usedSlots = new Set();
    allMembersSnap.forEach((doc) => {
      if (doc.id !== uid) {
        usedSlots.add(Number(doc.data().slotNumber));
      }
    });

    let slotNumber = memberSnap.exists ? Number(memberSnap.data().slotNumber) : 1;
    if (!memberSnap.exists || usedSlots.has(slotNumber)) {
      slotNumber = 1;
      while (usedSlots.has(slotNumber) && slotNumber <= MAX_SEASON_PLAYERS) {
        slotNumber += 1;
      }
    }

    if (slotNumber > MAX_SEASON_PLAYERS) {
      throw new HttpsError('resource-exhausted', 'No player slots available (15/15).');
    }

    const userData = userSnap.data();
    if (adminSnap.exists) {
      // Secret Admin Identity: NEVER write Admin to public members roster or increment activeCount
      if (memberSnap.exists) {
        tx.delete(memberRef);
      }
      const adminPresenceRef = seasonRef.collection('adminPresence').doc(uid);
      tx.set(adminPresenceRef, {
        seasonId,
        userId: uid,
        characterId,
        status: 'CONTROL_ROOM',
        online: true,
        sessionId,
        joinedAt: FieldValue.serverTimestamp(),
        lastSeenAt: FieldValue.serverTimestamp()
      });
      return { ok: true, slotNumber: 0, role: 'ADMIN', activeCount: existingCount };
    }

    const role = 'PLAYER';
    const nextActiveCount = memberSnap.exists ? existingCount : existingCount + 1;

    tx.set(memberRef, {
      seasonId,
      userId: uid,
      playerId: userData.playerId,
      displayName: userData.displayName,
      characterId,
      slotNumber,
      role,
      status: 'IN_HOUSE',
      online: true,
      sessionId,
      joinedAt: memberSnap.exists ? memberSnap.data().joinedAt : FieldValue.serverTimestamp(),
      lastSeenAt: FieldValue.serverTimestamp()
    });

    tx.update(seasonRef, {
      activeCount: nextActiveCount,
      status: 'LIVE',
      updatedAt: FieldValue.serverTimestamp()
    });

    return { ok: true, slotNumber, role, activeCount: nextActiveCount };
  });

  try {
    const rtdb = getDatabase();
    if (result.role === 'ADMIN') {
      await rtdb.ref(`admins/${uid}`).set(true);
      await rtdb.ref(`seasons/${seasonId}/players/${uid}`).remove();
    }
    await rtdb.ref(`seasons/${seasonId}/kicks/${uid}`).remove();
    await rtdb.ref(`seasons/${seasonId}/sessionLocks/${uid}`).set({
      sessionId,
      updatedAt: Date.now()
    });
  } catch (err) {
    console.warn('RTDB sync skipped or unavailable:', err.message);
  }

  return result;
});

/**
 * Trusted server-side Admin grant. Only an existing trusted ADMIN can promote another UID.
 */
exports.grantAdminRole = onCall(async (request) => {
  const adminUid = await requireAdmin(request);
  const targetUid = String(request.data?.targetUid || '').trim();
  if (!targetUid) {
    throw new HttpsError('invalid-argument', 'targetUid is required.');
  }

  const firestore = db();
  await firestore.collection('admins').doc(targetUid).set({
    userId: targetUid,
    role: 'ADMIN',
    grantedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });

  await firestore.collection('users').doc(targetUid).set(
    { role: 'ADMIN', updatedAt: FieldValue.serverTimestamp() },
    { merge: true }
  );

  await getAuth().setCustomUserClaims(targetUid, { admin: true, role: 'ADMIN' });

  try {
    await getDatabase().ref(`admins/${targetUid}`).set(true);
  } catch (err) {
    console.warn('RTDB admin mirror skipped:', err.message);
  }

  return { ok: true, targetUid, role: 'ADMIN' };
});

/**
 * Admin: Start, Pause (LOBBY), or End a reality-show season.
 */
exports.setSeasonStatus = onCall(async (request) => {
  const adminUid = await requireAdmin(request);
  const seasonId = String(request.data?.seasonId || 'season_01').trim();
  const nextStatus = String(request.data?.status || '').trim();
  if (!['LOBBY', 'LIVE', 'ENDED'].includes(nextStatus)) {
    throw new HttpsError('invalid-argument', 'status must be LOBBY, LIVE, or ENDED.');
  }

  const firestore = db();
  const seasonRef = firestore.collection('seasons').doc(seasonId);
  await seasonRef.update({
    status: nextStatus,
    updatedAt: FieldValue.serverTimestamp()
  });
  await seasonRef.collection('adminControls').doc().set({
    seasonId,
    commandType: 'SEASON_STATE',
    payloadSummary: `Season transitioned to ${nextStatus}`,
    issuedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });

  return { ok: true, seasonId, status: nextStatus };
});

/**
 * Admin: Remove a player from active session, Ban account, or Unban account.
 */
exports.moderateHousemate = onCall(async (request) => {
  const adminUid = await requireAdmin(request);
  const seasonId = String(request.data?.seasonId || 'season_01').trim();
  const targetUid = String(request.data?.targetUid || '').trim();
  const action = String(request.data?.action || '').trim(); // 'KICK' | 'BAN' | 'UNBAN'
  const reason = String(request.data?.reason || '').trim().slice(0, 200);

  if (!targetUid || !['KICK', 'BAN', 'UNBAN'].includes(action)) {
    throw new HttpsError('invalid-argument', 'targetUid and valid action (KICK/BAN/UNBAN) required.');
  }

  const firestore = db();
  const seasonRef = firestore.collection('seasons').doc(seasonId);
  const memberRef = seasonRef.collection('members').doc(targetUid);
  const userRef = firestore.collection('users').doc(targetUid);

  await firestore.runTransaction(async (tx) => {
    const [seasonSnap, memberSnap, userSnap] = await Promise.all([
      tx.get(seasonRef),
      tx.get(memberRef),
      tx.get(userRef)
    ]);

    if (action === 'BAN' || action === 'UNBAN') {
      if (userSnap.exists) {
        tx.update(userRef, {
          banned: action === 'BAN',
          banReason: action === 'BAN' ? (reason || 'Banned by Admin Control Room') : null,
          updatedAt: FieldValue.serverTimestamp()
        });
      }
    }

    if (action === 'KICK' || action === 'BAN') {
      if (memberSnap.exists) {
        tx.delete(memberRef);
        if (seasonSnap.exists) {
          const cur = Number(seasonSnap.data().activeCount || 0);
          tx.update(seasonRef, {
            activeCount: Math.max(0, cur - 1),
            updatedAt: FieldValue.serverTimestamp()
          });
        }
      }
    }
  });

  await seasonRef.collection('adminControls').doc().set({
    seasonId,
    commandType: action === 'KICK' ? 'KICK_SESSION' : 'BAN_USER',
    payloadSummary: `${action} ${targetUid}${reason ? ` (${reason})` : ''}`,
    issuedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });

  try {
    const rtdb = getDatabase();
    if (action === 'KICK' || action === 'BAN') {
      await rtdb.ref(`seasons/${seasonId}/kicks/${targetUid}`).set({
        reason: reason || (action === 'BAN' ? 'Account banned by Admin.' : 'Removed from active session by Admin.'),
        issuedBy: adminUid,
        updatedAt: Date.now()
      });
      await rtdb.ref(`seasons/${seasonId}/players/${targetUid}`).update({
        online: false,
        state: 'offline',
        seatId: null,
        updatedAt: Date.now()
      });
    } else if (action === 'UNBAN') {
      await rtdb.ref(`seasons/${seasonId}/kicks/${targetUid}`).remove();
    }
  } catch (err) {
    console.warn('RTDB moderation sync skipped:', err.message);
  }

  return { ok: true, targetUid, action };
});

/**
 * Admin: Publish nominations and open/close voting cycles.
 */
exports.manageVotingCycle = onCall(async (request) => {
  const adminUid = await requireAdmin(request);
  const seasonId = String(request.data?.seasonId || 'season_01').trim();
  const cycleId = String(request.data?.cycleId || `cycle_${Date.now()}`).trim();
  const title = String(request.data?.title || 'Elimination Nomination').trim().slice(0, 80);
  const status = String(request.data?.status || 'OPEN').trim();
  const nominatedUids = Array.isArray(request.data?.nominatedUids) ? request.data.nominatedUids.slice(0, 15) : [];
  const nomineeSummaries = Array.isArray(request.data?.nomineeSummaries) ? request.data.nomineeSummaries.slice(0, 15) : [];
  const npcVotesEnabled = Boolean(request.data?.npcVotesEnabled);
  const voteType = ['EVICT', 'SAVE'].includes(String(request.data?.voteType || 'EVICT')) ? String(request.data.voteType) : 'EVICT';
  const nominationsPerPlayer = Math.min(3, Math.max(1, Number(request.data?.nominationsPerPlayer || 1)));
  const nominationEndTimeMillis = Math.max(0, Number(request.data?.nominationEndTimeMillis || 0));
  const votingDurationMinutes = Math.min(1440, Math.max(1, Number(request.data?.votingDurationMinutes || 15)));
  const votingEndTimeMillis = Math.max(0, Number(request.data?.votingEndTimeMillis || 0));
  const resultVisibility = ['HIDDEN_UNTIL_CLOSED', 'LIVE_TOTALS', 'ADMIN_ONLY'].includes(String(request.data?.resultVisibility || 'HIDDEN_UNTIL_CLOSED'))
    ? String(request.data.resultVisibility)
    : 'HIDDEN_UNTIL_CLOSED';
  const tieBreakerPolicy = ['ADMIN_DECIDES', 'CAPTAIN_DECIDES', 'RANDOM_DRAW', 'NO_EVICTION'].includes(String(request.data?.tieBreakerPolicy || 'ADMIN_DECIDES'))
    ? String(request.data.tieBreakerPolicy)
    : 'ADMIN_DECIDES';
  const cancelledReason = request.data?.cancelledReason ? String(request.data.cancelledReason).slice(0, 200) : null;

  if (!['NOMINATIONS', 'OPEN', 'CLOSED', 'EVICTED', 'CANCELLED'].includes(status)) {
    throw new HttpsError('invalid-argument', 'Invalid voting status.');
  }
  if (!['NOMINATIONS', 'CANCELLED'].includes(status) && nominatedUids.length < 1) {
    throw new HttpsError('invalid-argument', 'At least 1 nominated housemate is required for voting.');
  }
  if (nomineeSummaries.length !== nominatedUids.length) {
    throw new HttpsError('invalid-argument', 'nomineeSummaries must match nominatedUids length.');
  }

  const firestore = db();
  const cycleRef = firestore.collection('seasons').doc(seasonId).collection('votingCycles').doc(cycleId);
  const snap = await cycleRef.get();

  const payload = {
    title,
    status,
    nominatedUids,
    nomineeSummaries,
    npcVotesEnabled,
    voteType,
    nominationsPerPlayer,
    nominationEndTimeMillis,
    votingDurationMinutes,
    votingEndTimeMillis,
    resultVisibility,
    tieBreakerPolicy,
    cancelledReason,
    updatedAt: FieldValue.serverTimestamp()
  };

  if (snap.exists) {
    await cycleRef.update(payload);
  } else {
    await cycleRef.set({
      seasonId,
      ...payload,
      evictedUid: null,
      evictedDisplayName: null,
      createdBy: adminUid,
      createdAt: FieldValue.serverTimestamp()
    });
  }

  await firestore.collection('seasons').doc(seasonId).collection('adminControls').doc().set({
    seasonId,
    commandType: status === 'CANCELLED' ? 'VOTING_CANCEL' : 'VOTING_STATE',
    payloadSummary: `Cycle ${title}: ${status} (${voteType}, ${nominatedUids.length} nominees)`,
    issuedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });

  return { ok: true, cycleId, status };
});

/**
 * Authenticated Housemate: Submit duplicate-proof peer nominations during NOMINATIONS phase.
 */
exports.submitPeerNomination = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required.');
  }
  const uid = request.auth.uid;
  const seasonId = String(request.data?.seasonId || 'season_01').trim();
  const cycleId = String(request.data?.cycleId || '').trim();
  const rawTargets = Array.isArray(request.data?.targetUids) ? request.data.targetUids : [];
  const reason = request.data?.reason ? String(request.data.reason).trim().slice(0, 200) : null;

  if (!cycleId || rawTargets.length < 1) {
    throw new HttpsError('invalid-argument', 'cycleId and at least 1 targetUid required.');
  }

  const uniqueTargets = [...new Set(rawTargets.map((t) => String(t).trim()).filter(Boolean))];
  if (uniqueTargets.includes(uid)) {
    throw new HttpsError('invalid-argument', 'You cannot nominate yourself.');
  }

  const firestore = db();
  const seasonRef = firestore.collection('seasons').doc(seasonId);
  const cycleRef = seasonRef.collection('votingCycles').doc(cycleId);
  const memberRef = seasonRef.collection('members').doc(uid);
  const userRef = firestore.collection('users').doc(uid);
  const nominationRef = cycleRef.collection('nominations').doc(uid);

  await firestore.runTransaction(async (tx) => {
    const [cycleSnap, memberSnap, userSnap, existingNomSnap] = await Promise.all([
      tx.get(cycleRef),
      tx.get(memberRef),
      tx.get(userRef),
      tx.get(nominationRef)
    ]);

    if (!cycleSnap.exists || cycleSnap.data().status !== 'NOMINATIONS') {
      throw new HttpsError('failed-precondition', 'Nominations are currently closed.');
    }
    const maxAllowed = Number(cycleSnap.data().nominationsPerPlayer || 1);
    if (uniqueTargets.length > maxAllowed) {
      throw new HttpsError('invalid-argument', `Select at most ${maxAllowed} housemate(s).`);
    }
    if (!userSnap.exists || userSnap.data().banned === true) {
      throw new HttpsError('permission-denied', 'Account is not eligible to nominate.');
    }
    if (!memberSnap.exists || ['EVICTED', 'BANNED', 'REMOVED'].includes(memberSnap.data().status)) {
      throw new HttpsError('permission-denied', 'Only active housemates may nominate.');
    }
    if (existingNomSnap.exists) {
      throw new HttpsError('already-exists', 'You have already submitted nominations for this round.');
    }

    const targetNames = [];
    for (const targetUid of uniqueTargets) {
      const [tAdminSnap, tMemberSnap] = await Promise.all([
        tx.get(firestore.collection('admins').doc(targetUid)),
        tx.get(seasonRef.collection('members').doc(targetUid))
      ]);
      if (tAdminSnap.exists) {
        throw new HttpsError('permission-denied', 'Invalid nomination target.');
      }
      if (!tMemberSnap.exists || ['EVICTED', 'BANNED', 'REMOVED'].includes(tMemberSnap.data().status)) {
        throw new HttpsError('failed-precondition', 'Nominated housemate is no longer active.');
      }
      targetNames.push(tMemberSnap.data().displayName || 'Housemate');
    }

    tx.set(nominationRef, {
      seasonId,
      cycleId,
      voterUid: uid,
      voterName: userSnap.data().displayName,
      targetUids: uniqueTargets,
      targetNames,
      reason,
      createdAt: FieldValue.serverTimestamp()
    });
  });

  return { ok: true, cycleId, targetUids: uniqueTargets };
});

/**
 * Authenticated Housemate: Cast a real verified vote during an OPEN cycle.
 */
exports.castHousemateVote = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required.');
  }
  const uid = request.auth.uid;
  const seasonId = String(request.data?.seasonId || 'season_01').trim();
  const cycleId = String(request.data?.cycleId || '').trim();
  const targetUid = String(request.data?.targetUid || '').trim();

  if (!cycleId || !targetUid) {
    throw new HttpsError('invalid-argument', 'cycleId and targetUid required.');
  }

  const firestore = db();
  const seasonRef = firestore.collection('seasons').doc(seasonId);
  const cycleRef = seasonRef.collection('votingCycles').doc(cycleId);
  const memberRef = seasonRef.collection('members').doc(uid);
  const userRef = firestore.collection('users').doc(uid);
  const ballotRef = cycleRef.collection('votes').doc(uid);

  await firestore.runTransaction(async (tx) => {
    const [cycleSnap, memberSnap, userSnap, ballotSnap] = await Promise.all([
      tx.get(cycleRef),
      tx.get(memberRef),
      tx.get(userRef),
      tx.get(ballotRef)
    ]);

    if (!cycleSnap.exists || cycleSnap.data().status !== 'OPEN') {
      throw new HttpsError('failed-precondition', 'Voting is currently closed.');
    }
    if (!cycleSnap.data().nominatedUids.includes(targetUid)) {
      throw new HttpsError('invalid-argument', 'Selected player is not nominated in this cycle.');
    }
    if (!userSnap.exists || userSnap.data().banned === true) {
      throw new HttpsError('permission-denied', 'Account is not eligible to vote.');
    }
    if (!memberSnap.exists || ['EVICTED', 'BANNED', 'REMOVED'].includes(memberSnap.data().status)) {
      throw new HttpsError('permission-denied', 'Only active housemates may cast a ballot.');
    }
    if (ballotSnap.exists) {
      throw new HttpsError('already-exists', 'You have already voted in this cycle.');
    }

    tx.set(ballotRef, {
      seasonId,
      cycleId,
      voterUid: uid,
      voterName: userSnap.data().displayName,
      targetUid,
      sourceType: 'REAL_PLAYER',
      createdAt: FieldValue.serverTimestamp()
    });
  });

  return { ok: true, cycleId, targetUid };
});

/**
 * Admin: Confirm eviction using verified stored votes, record historical eviction,
 * remove evicted player from active season roster, and notify connected sessions.
 */
exports.confirmEviction = onCall(async (request) => {
  const adminUid = await requireAdmin(request);
  const seasonId = String(request.data?.seasonId || 'season_01').trim();
  const cycleId = String(request.data?.cycleId || '').trim();
  const targetUid = String(request.data?.targetUid || '').trim();
  const announcementText = String(request.data?.announcementText || 'A housemate has been evicted from the Glass House.').trim().slice(0, 300);

  if (!cycleId || !targetUid) {
    throw new HttpsError('invalid-argument', 'cycleId and targetUid required.');
  }

  const firestore = db();
  const seasonRef = firestore.collection('seasons').doc(seasonId);
  const cycleRef = seasonRef.collection('votingCycles').doc(cycleId);
  const votesSnap = await cycleRef.collection('votes').where('targetUid', '==', targetUid).get();

  let realVotes = 0;
  let npcVotes = 0;
  votesSnap.forEach((doc) => {
    if (doc.data().sourceType === 'NPC') npcVotes += 1;
    else realVotes += 1;
  });

  const userRef = firestore.collection('users').doc(targetUid);
  const memberRef = seasonRef.collection('members').doc(targetUid);
  const evictionRef = seasonRef.collection('evictions').doc(`${cycleId}_${targetUid}`);

  const outcome = await firestore.runTransaction(async (tx) => {
    const [seasonSnap, cycleSnap, memberSnap, userSnap] = await Promise.all([
      tx.get(seasonRef),
      tx.get(cycleRef),
      tx.get(memberRef),
      tx.get(userRef)
    ]);

    if (!cycleSnap.exists) {
      throw new HttpsError('not-found', 'Voting cycle not found.');
    }
    if (!userSnap.exists) {
      throw new HttpsError('not-found', 'Target player account not found.');
    }

    const userData = userSnap.data();
    tx.set(evictionRef, {
      seasonId,
      cycleId,
      userId: targetUid,
      playerId: userData.playerId,
      displayName: userData.displayName,
      characterId: userData.characterId,
      realVotes,
      npcVotes,
      announcementText,
      evictedBy: adminUid,
      createdAt: FieldValue.serverTimestamp()
    });

    tx.update(cycleRef, {
      status: 'EVICTED',
      evictedUid: targetUid,
      evictedDisplayName: userData.displayName,
      updatedAt: FieldValue.serverTimestamp()
    });

    tx.update(userRef, {
      evictedSeasons: FieldValue.increment(1),
      updatedAt: FieldValue.serverTimestamp()
    });

    if (memberSnap.exists) {
      tx.update(memberRef, {
        status: 'EVICTED',
        online: false,
        lastSeenAt: FieldValue.serverTimestamp()
      });
      if (seasonSnap.exists) {
        const cur = Number(seasonSnap.data().activeCount || 0);
        tx.update(seasonRef, {
          activeCount: Math.max(0, cur - 1),
          updatedAt: FieldValue.serverTimestamp()
        });
      }
    }

    return {
      ok: true,
      evictedUid: targetUid,
      displayName: userData.displayName,
      realVotes,
      npcVotes
    };
  });

  await seasonRef.collection('adminControls').doc().set({
    seasonId,
    commandType: 'EVICT_PLAYER',
    payloadSummary: `Evicted ${outcome.displayName} (Real votes: ${realVotes}, NPC votes: ${npcVotes})`,
    issuedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });

  try {
    const rtdb = getDatabase();
    await rtdb.ref(`seasons/${seasonId}/kicks/${targetUid}`).set({
      reason: announcementText,
      issuedBy: adminUid,
      updatedAt: Date.now()
    });
    await rtdb.ref(`seasons/${seasonId}/players/${targetUid}`).update({
      online: false,
      state: 'offline',
      seatId: null,
      updatedAt: Date.now()
    });
  } catch (err) {
    console.warn('RTDB eviction sync skipped:', err.message);
  }

  return outcome;
});

/**
 * Admin: Broadcast Indian Hindi / Housemate Announcement to every connected player.
 */
exports.broadcastAdminAnnouncement = onCall(async (request) => {
  const adminUid = await requireAdmin(request);
  const seasonId = String(request.data?.seasonId || 'season_01').trim();
  const message = String(request.data?.message || '').trim().slice(0, 320);
  const presetId = request.data?.presetId ? String(request.data.presetId).trim().slice(0, 64) : null;
  const languageTag = ['hi-IN', 'en-IN'].includes(request.data?.languageTag) ? request.data.languageTag : 'hi-IN';
  const voiceGender = request.data?.voiceGender === 'MALE' ? 'MALE' : 'FEMALE';

  if (message.length < 2) {
    throw new HttpsError('invalid-argument', 'Announcement message is required.');
  }

  const firestore = db();
  const seasonRef = firestore.collection('seasons').doc(seasonId);
  const annRef = seasonRef.collection('announcements').doc();

  await annRef.set({
    seasonId,
    message,
    presetId,
    languageTag,
    voiceGender,
    issuedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });

  await seasonRef.collection('adminControls').doc().set({
    seasonId,
    commandType: 'BROADCAST',
    payloadSummary: `[${languageTag}/${voiceGender}] ${message}`.slice(0, 300),
    issuedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });

  try {
    await getDatabase().ref(`seasons/${seasonId}/announcements/latest`).set({
      id: annRef.id,
      message,
      languageTag,
      voiceGender,
      issuedBy: adminUid,
      updatedAt: Date.now()
    });
  } catch (err) {
    console.warn('RTDB announcement mirror skipped:', err.message);
  }

  return { ok: true, announcementId: annRef.id };
});

/**
 * Server-verified Admin Control Room action (CCTV / Door Override / Broadcast).
 */
exports.castAdminControlAction = onCall(async (request) => {
  const adminUid = await requireAdmin(request);

  const seasonId = String(request.data?.seasonId || 'season_01').trim();
  const commandType = String(request.data?.commandType || 'BROADCAST').trim();
  const payloadSummary = String(request.data?.payloadSummary || '').trim().slice(0, 300);

  if (!payloadSummary) {
    throw new HttpsError('invalid-argument', 'payloadSummary is required.');
  }

  const controlRef = db()
    .collection('seasons')
    .doc(seasonId)
    .collection('adminControls')
    .doc();

  await controlRef.set({
    seasonId,
    commandType,
    payloadSummary,
    issuedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });

  return { ok: true, controlId: controlRef.id };
});

/**
 * Admin: Create, Edit, Start, Pause, Stop, or Cancel a House Task with server-isolated answerKey.
 */
exports.manageHouseTask = onCall(async (request) => {
  const adminUid = await requireAdmin(request);
  const seasonId = String(request.data?.seasonId || 'season_01').trim();
  const taskId = String(request.data?.taskId || `task_${Date.now()}`).trim();
  const action = String(request.data?.action || 'UPSERT').trim(); // UPSERT | START | PAUSE | STOP | CANCEL
  const title = String(request.data?.title || 'Daily House Task').trim().slice(0, 100);
  const description = String(request.data?.description || 'Indoor house activity').trim().slice(0, 400);
  const instructions = String(request.data?.instructions || 'Follow instructions carefully.').trim().slice(0, 800);
  const activityType = String(request.data?.activityType || 'QUIZ').trim();
  const assignmentMode = request.data?.assignmentMode === 'TEAM' ? 'TEAM' : 'INDIVIDUAL';
  const isCaptaincy = Boolean(request.data?.isCaptaincy);
  const targetRoom = String(request.data?.targetRoom || 'Living Atrium').trim().slice(0, 60);
  const durationSeconds = Math.max(30, Math.min(7200, Number(request.data?.durationSeconds || 180)));
  const promptQuestion = String(request.data?.promptQuestion || '').trim().slice(0, 400);
  const options = Array.isArray(request.data?.options) ? request.data.options.slice(0, 6).map((o) => String(o).slice(0, 120)) : [];
  const maxPoints = Math.max(1, Math.min(1000, Number(request.data?.maxPoints || 100)));
  const expectedAnswer = String(request.data?.expectedAnswer || '').trim().slice(0, 240);

  const firestore = db();
  const seasonRef = firestore.collection('seasons').doc(seasonId);
  const taskRef = seasonRef.collection('tasks').doc(taskId);
  const snap = await taskRef.get();

  const nowMs = Date.now();
  let status = snap.exists ? snap.data().status : 'DRAFT';
  let startTimeMillis = snap.exists ? Number(snap.data().startTimeMillis || 0) : 0;
  let endTimeMillis = snap.exists ? Number(snap.data().endTimeMillis || 0) : 0;

  if (action === 'START') {
    status = 'ACTIVE';
    startTimeMillis = nowMs;
    endTimeMillis = nowMs + durationSeconds * 1000;
  } else if (action === 'PAUSE') {
    status = 'PAUSED';
  } else if (action === 'STOP') {
    status = 'COMPLETED';
    endTimeMillis = nowMs;
  } else if (action === 'CANCEL') {
    status = 'CANCELLED';
    endTimeMillis = nowMs;
  }

  const batch = firestore.batch();
  if (snap.exists) {
    batch.update(taskRef, {
      title,
      description,
      instructions,
      activityType,
      assignmentMode,
      isCaptaincy,
      status,
      targetRoom,
      durationSeconds,
      startTimeMillis,
      endTimeMillis,
      promptQuestion,
      options,
      maxPoints,
      updatedAt: FieldValue.serverTimestamp()
    });
  } else {
    batch.set(taskRef, {
      seasonId,
      title,
      description,
      instructions,
      activityType,
      assignmentMode,
      isCaptaincy,
      status,
      targetRoom,
      durationSeconds,
      startTimeMillis,
      endTimeMillis,
      promptQuestion,
      options,
      maxPoints,
      winningUid: null,
      winningTeamId: null,
      winnerSummary: null,
      createdBy: adminUid,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp()
    });
  }

  if (expectedAnswer) {
    batch.set(taskRef.collection('answerKey').doc('config'), {
      seasonId,
      taskId,
      expectedAnswer,
      updatedBy: adminUid,
      updatedAt: FieldValue.serverTimestamp()
    });
  }

  if (status === 'ACTIVE') {
    batch.update(seasonRef, { activeTaskId: taskId, updatedAt: FieldValue.serverTimestamp() });
  } else if (status === 'CANCELLED' || status === 'COMPLETED') {
    batch.update(seasonRef, { activeTaskId: null, updatedAt: FieldValue.serverTimestamp() });
  }

  batch.set(seasonRef.collection('adminControls').doc(), {
    seasonId,
    commandType: 'TASK_CONTROL',
    payloadSummary: `${action}: ${title} (${status})`.slice(0, 300),
    issuedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });
  await batch.commit();

  try {
    await getDatabase().ref(`seasons/${seasonId}/activeTask`).set({
      taskId,
      title,
      description,
      activityType,
      assignmentMode,
      isCaptaincy,
      status,
      targetRoom,
      startTimeMillis,
      endTimeMillis,
      winnerSummary: null,
      updatedAt: nowMs
    });
  } catch (err) {
    console.warn('RTDB activeTask mirror skipped:', err.message);
  }

  return { ok: true, taskId, status, startTimeMillis, endTimeMillis };
});

/**
 * Authenticated Housemate: Submit one validated entry for an ACTIVE task.
 * Prevents duplicate submissions and checks secret answerKey server-side.
 */
exports.submitTaskEntry = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required.');
  }
  const uid = request.auth.uid;
  const seasonId = String(request.data?.seasonId || 'season_01').trim();
  const taskId = String(request.data?.taskId || '').trim();
  const answerText = String(request.data?.answerText || '').trim().slice(0, 300);
  const teamId = request.data?.teamId ? String(request.data.teamId).trim() : null;

  if (!taskId || !answerText) {
    throw new HttpsError('invalid-argument', 'taskId and answerText are required.');
  }

  const firestore = db();
  const seasonRef = firestore.collection('seasons').doc(seasonId);
  const taskRef = seasonRef.collection('tasks').doc(taskId);
  const subRef = taskRef.collection('submissions').doc(uid);
  const userRef = firestore.collection('users').doc(uid);
  const memberRef = seasonRef.collection('members').doc(uid);
  const keyRef = taskRef.collection('answerKey').doc('config');

  return firestore.runTransaction(async (tx) => {
    const [taskSnap, subSnap, userSnap, memberSnap, keySnap] = await Promise.all([
      tx.get(taskRef),
      tx.get(subRef),
      tx.get(userRef),
      tx.get(memberRef),
      tx.get(keyRef)
    ]);

    if (!taskSnap.exists || taskSnap.data().status !== 'ACTIVE') {
      throw new HttpsError('failed-precondition', 'This task is no longer active.');
    }
    const taskData = taskSnap.data();
    const nowMs = Date.now();
    if (taskData.endTimeMillis && nowMs > Number(taskData.endTimeMillis) + 2000) {
      throw new HttpsError('deadline-exceeded', 'Task timer has expired.');
    }
    if (subSnap.exists) {
      throw new HttpsError('already-exists', 'Duplicate submission blocked: already submitted for this task.');
    }
    if (!userSnap.exists || userSnap.data().banned === true) {
      throw new HttpsError('permission-denied', 'Account is not eligible to submit.');
    }
    if (!memberSnap.exists || ['EVICTED', 'BANNED', 'REMOVED'].includes(memberSnap.data().status)) {
      throw new HttpsError('permission-denied', 'Only active housemates can submit task entries.');
    }

    const elapsedSeconds = taskData.startTimeMillis
      ? Math.max(0, Math.min(7200, Math.floor((nowMs - Number(taskData.startTimeMillis)) / 1000)))
      : 0;

    let verificationStatus = 'SUBMITTED';
    let awardedScore = 0;
    if (keySnap.exists && keySnap.data().expectedAnswer) {
      const expected = String(keySnap.data().expectedAnswer).trim().toLowerCase();
      if (answerText.toLowerCase() === expected) {
        verificationStatus = 'VERIFIED';
        awardedScore = Number(taskData.maxPoints || 100);
      } else {
        verificationStatus = 'REJECTED';
      }
    }

    tx.set(subRef, {
      seasonId,
      taskId,
      userId: uid,
      playerId: userSnap.data().playerId,
      displayName: userSnap.data().displayName,
      teamId,
      answerText,
      elapsedSeconds,
      verificationStatus,
      awardedScore,
      reviewedBy: verificationStatus === 'SUBMITTED' ? null : 'cloud-function-verifier',
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp()
    });

    return { ok: true, verificationStatus, awardedScore, elapsedSeconds };
  });
});

/**
 * Admin: Assign teams, member lists, and team scores in Firestore & RTDB.
 */
exports.manageHouseTeams = onCall(async (request) => {
  const adminUid = await requireAdmin(request);
  const seasonId = String(request.data?.seasonId || 'season_01').trim();
  const teamId = String(request.data?.teamId || `team_${Date.now()}`).trim();
  const name = String(request.data?.name || 'House Team').trim().slice(0, 40);
  const colorHex = /^#[0-9a-fA-F]{6}$/.test(String(request.data?.colorHex || '')) ? request.data.colorHex : '#EAB954';
  const memberUids = Array.isArray(request.data?.memberUids) ? request.data.memberUids.slice(0, 15) : [];
  const score = Math.max(0, Math.min(100000, Number(request.data?.score || 0)));
  const winsCount = Math.max(0, Math.min(1000, Number(request.data?.winsCount || 0)));

  const firestore = db();
  const seasonRef = firestore.collection('seasons').doc(seasonId);
  const teamRef = seasonRef.collection('teams').doc(teamId);
  const snap = await teamRef.get();

  if (snap.exists) {
    await teamRef.update({
      name,
      colorHex,
      memberUids,
      score,
      winsCount,
      updatedBy: adminUid,
      updatedAt: FieldValue.serverTimestamp()
    });
  } else {
    await teamRef.set({
      seasonId,
      name,
      colorHex,
      memberUids,
      score,
      winsCount,
      updatedBy: adminUid,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp()
    });
  }

  await seasonRef.collection('adminControls').doc().set({
    seasonId,
    commandType: 'TEAM_ASSIGN',
    payloadSummary: `Team ${name} (${memberUids.length} members, score ${score})`,
    issuedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });

  try {
    await getDatabase().ref(`seasons/${seasonId}/teams/${teamId}`).set({
      name,
      colorHex,
      score,
      winsCount,
      memberCount: memberUids.length,
      updatedAt: Date.now()
    });
  } catch (err) {
    console.warn('RTDB team sync skipped:', err.message);
  }

  return { ok: true, teamId };
});

/**
 * Admin: Assign, replace, or remove the House Captain, update Captain powers, record history, and sync RTDB.
 */
exports.assignHouseCaptain = onCall(async (request) => {
  const adminUid = await requireAdmin(request);
  const seasonId = String(request.data?.seasonId || 'season_01').trim();
  const clearCaptain = Boolean(request.data?.clearCaptain);
  const captainUid = String(request.data?.captainUid || '').trim();
  const title = String(request.data?.title || 'House Captain').trim().slice(0, 80);
  const taskId = request.data?.taskId ? String(request.data.taskId).trim() : null;
  const scoreAwarded = Math.max(0, Math.min(1000, Number(request.data?.scoreAwarded || 100)));
  const captainVipAccess = request.data?.captainVipAccess !== false;
  const captainImmunity = Boolean(request.data?.captainImmunity);
  const captainDoubleVote = Boolean(request.data?.captainDoubleVote);
  const captainTieBreaker = Boolean(request.data?.captainTieBreaker);

  const firestore = db();
  const seasonRef = firestore.collection('seasons').doc(seasonId);

  if (clearCaptain) {
    const seasonSnap = await seasonRef.get();
    const prevUid = seasonSnap.exists ? (seasonSnap.data()?.captainUid || '') : '';
    const prevName = seasonSnap.exists ? (seasonSnap.data()?.captainDisplayName || 'Previous Captain') : 'Previous Captain';

    const batch = firestore.batch();
    batch.update(seasonRef, {
      captainUid: null,
      captainPlayerId: null,
      captainDisplayName: null,
      captainTitle: null,
      updatedAt: FieldValue.serverTimestamp()
    });
    if (prevUid) {
      batch.set(seasonRef.collection('privilegeHistory').doc(), {
        seasonId,
        targetUid: prevUid,
        targetDisplayName: prevName,
        actionType: 'CAPTAIN_REMOVED',
        detail: `Captaincy removed from ${prevName} by Admin.`,
        issuedBy: adminUid,
        createdAt: FieldValue.serverTimestamp()
      });
    }
    batch.set(seasonRef.collection('adminControls').doc(), {
      seasonId,
      commandType: 'CAPTAINCY_UPDATE',
      payloadSummary: `Captain removed (${prevName})`,
      issuedBy: adminUid,
      createdAt: FieldValue.serverTimestamp()
    });
    await batch.commit();

    try {
      await getDatabase().ref(`seasons/${seasonId}/captain`).set({
        uid: '',
        playerId: '',
        displayName: '',
        title: '',
        vipAccess: false,
        updatedAt: Date.now()
      });
    } catch (err) {
      console.warn('RTDB captain clear sync skipped:', err.message);
    }

    return { ok: true, cleared: true };
  }

  if (!captainUid) {
    throw new HttpsError('invalid-argument', 'captainUid is required.');
  }

  const userSnap = await firestore.collection('users').doc(captainUid).get();
  if (!userSnap.exists) {
    throw new HttpsError('not-found', 'Captain player account not found.');
  }
  const u = userSnap.data();

  const batch = firestore.batch();
  batch.update(seasonRef, {
    captainUid,
    captainPlayerId: u.playerId,
    captainDisplayName: u.displayName,
    captainTitle: title,
    captainVipAccess,
    captainImmunity,
    captainDoubleVote,
    captainTieBreaker,
    updatedAt: FieldValue.serverTimestamp()
  });
  batch.set(seasonRef.collection('captaincyHistory').doc(), {
    seasonId,
    taskId,
    captainUid,
    captainPlayerId: u.playerId,
    captainDisplayName: u.displayName,
    captainCharacterId: u.characterId,
    title,
    scoreAwarded,
    assignedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });
  batch.set(seasonRef.collection('privilegeHistory').doc(), {
    seasonId,
    targetUid: captainUid,
    targetDisplayName: u.displayName,
    actionType: 'CAPTAIN_ASSIGNED',
    detail: `Assigned ${title} to ${u.displayName} (VIP:${captainVipAccess}, Immunity:${captainImmunity}, 2xVote:${captainDoubleVote}, TieBreak:${captainTieBreaker})`,
    issuedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });
  batch.set(seasonRef.collection('adminControls').doc(), {
    seasonId,
    commandType: 'CAPTAINCY_UPDATE',
    payloadSummary: `Captain assigned: ${u.displayName} (${u.playerId})`,
    issuedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });
  await batch.commit();

  try {
    await getDatabase().ref(`seasons/${seasonId}/captain`).set({
      uid: captainUid,
      playerId: u.playerId,
      displayName: u.displayName,
      title,
      vipAccess: captainVipAccess,
      updatedAt: Date.now()
    });
  } catch (err) {
    console.warn('RTDB captain sync skipped:', err.message);
  }

  return { ok: true, captainUid, displayName: u.displayName };
});

/**
 * Admin: Issue a Rule Violation Warning to a housemate.
 */
exports.issueRuleWarning = onCall(async (request) => {
  const adminUid = await requireAdmin(request);
  const seasonId = String(request.data?.seasonId || 'season_01').trim();
  const ruleId = String(request.data?.ruleId || 'rule_general').trim();
  const ruleTitle = String(request.data?.ruleTitle || 'House Rule Violation').trim().slice(0, 80);
  const targetUid = String(request.data?.targetUid || '').trim();
  const warningLevel = ['NOTICE', 'WARNING', 'FINAL_WARNING'].includes(request.data?.warningLevel)
    ? request.data.warningLevel
    : 'WARNING';
  const note = String(request.data?.note || 'Please follow House Rules immediately.').trim().slice(0, 240);

  if (!targetUid) {
    throw new HttpsError('invalid-argument', 'targetUid is required.');
  }

  const firestore = db();
  const seasonRef = firestore.collection('seasons').doc(seasonId);
  const userSnap = await firestore.collection('users').doc(targetUid).get();
  if (!userSnap.exists) {
    throw new HttpsError('not-found', 'Target housemate not found.');
  }
  const u = userSnap.data();

  const violRef = seasonRef.collection('ruleViolations').doc();
  await violRef.set({
    seasonId,
    ruleId,
    ruleTitle,
    targetUid,
    targetPlayerId: u.playerId,
    targetDisplayName: u.displayName,
    warningLevel,
    note,
    issuedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });

  await seasonRef.collection('adminControls').doc().set({
    seasonId,
    commandType: 'RULE_WARNING',
    payloadSummary: `${warningLevel} to ${u.displayName}: ${ruleTitle}`.slice(0, 300),
    issuedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });

  try {
    await getDatabase().ref(`seasons/${seasonId}/ruleWarnings/${targetUid}`).set({
      id: violRef.id,
      ruleTitle,
      warningLevel,
      note,
      issuedBy: adminUid,
      updatedAt: Date.now()
    });
  } catch (err) {
    console.warn('RTDB rule warning sync skipped:', err.message);
  }

  return { ok: true, violationId: violRef.id };
});

// --- UPGRADE 3: USERNAME + PASSWORD SERVER-SIDE AUTH (SCRYPT + CUSTOM TOKENS) ---

const VALID_CHAR_IDS = new Set(
  Array.from({ length: 15 }, (_, idx) => `Player ${String(idx + 1).padStart(2, '0')}`)
);

function normalizeSlug(raw) {
  return String(raw || '').trim().toLowerCase().replace(/[^a-z0-9_-]/g, '_');
}

function hashWithSalt(secret, existingSalt) {
  const salt = existingSalt || crypto.randomBytes(16).toString('hex');
  const hash = crypto.scryptSync(String(secret), salt, 64).toString('hex');
  return { hash, salt };
}

function safeCompareSecret(secret, expectedHash, salt) {
  if (!expectedHash || !salt) return false;
  const computed = crypto.scryptSync(String(secret), String(salt), 64);
  const expectedBuf = Buffer.from(String(expectedHash), 'hex');
  if (computed.length !== expectedBuf.length) return false;
  return crypto.timingSafeEqual(computed, expectedBuf);
}

function safeComparePlain(a, b) {
  const bufA = Buffer.from(String(a || ''), 'utf8');
  const bufB = Buffer.from(String(b || ''), 'utf8');
  if (bufA.length === 0 || bufA.length !== bufB.length) return false;
  return crypto.timingSafeEqual(bufA, bufB);
}

function generateRecoveryCode() {
  const raw = crypto.randomBytes(6).toString('hex').toUpperCase();
  return `${raw.slice(0, 4)}-${raw.slice(4, 8)}-${raw.slice(8, 12)}`;
}

function parseJsonBody(req) {
  if (req.body && typeof req.body === 'object') return req.body;
  if (typeof req.body === 'string') {
    try {
      return JSON.parse(req.body);
    } catch (_) {
      return {};
    }
  }
  return {};
}

const AUTH_HTTP_OPTIONS = {
  region: 'us-central1',
  cors: true,
  invoker: 'public',
};

function formatTokenMintError(err) {
  const msg = String(err && err.message ? err.message : err || '');
  if (msg.includes('signBlob') || msg.includes('iam.serviceAccounts') || msg.includes('PERMISSION_DENIED')) {
    return 'Server IAM permission missing for custom token signing: grant "Service Account Token Creator" (roles/iam.serviceAccountTokenCreator) to the Cloud Functions runtime service account.';
  }
  return msg || 'Authentication service failure.';
}

exports.authRegister = onRequest(AUTH_HTTP_OPTIONS, async (req, res) => {
  if (req.method !== 'POST') {
    res.status(405).json({ ok: false, error: 'Method not allowed. Use POST.' });
    return;
  }

  const body = parseJsonBody(req);
  const rawUsername = String(body.username || '').trim();
  const password = String(body.password || '');
  const rawDisplayName = String(body.displayName || rawUsername).trim();
  const characterId = VALID_CHAR_IDS.has(String(body.characterId || '').trim())
    ? String(body.characterId).trim()
    : 'Player 01';
  const seasonId = String(body.seasonId || 'season_01').trim() || 'season_01';
  const adminSetupKey = String(body.adminSetupKey || '').trim();

  const username = rawUsername.toLowerCase();
  if (!/^[a-z0-9_]{3,24}$/.test(username)) {
    res.status(400).json({
      ok: false,
      error: 'Username must be 3-24 characters using letters, numbers, or underscores.'
    });
    return;
  }
  if (password.length < 8 || password.length > 128) {
    res.status(400).json({ ok: false, error: 'Password must be between 8 and 128 characters.' });
    return;
  }
  if (rawDisplayName.length < 3 || rawDisplayName.length > 24) {
    res.status(400).json({ ok: false, error: 'Display name must be between 3 and 24 characters.' });
    return;
  }
  const normalizedDisplay = normalizeSlug(rawDisplayName);
  if (normalizedDisplay.length < 3) {
    res.status(400).json({ ok: false, error: 'Display name must include at least 3 letters or digits.' });
    return;
  }

  const configuredAdminKey = String(process.env.BIGGBOSS_ADMIN_KEY || '').trim();
  let grantAdmin = false;
  if (adminSetupKey.length > 0) {
    if (!configuredAdminKey || configuredAdminKey.length < 12) {
      res.status(403).json({
        ok: false,
        error: 'Server BIGGBOSS_ADMIN_KEY is not configured; cannot provision ADMIN account.'
      });
      return;
    }
    if (!safeComparePlain(adminSetupKey, configuredAdminKey)) {
      res.status(403).json({ ok: false, error: 'Invalid Admin setup key.' });
      return;
    }
    grantAdmin = true;
  } else if (username === 'biggboss' || username === 'bigg_boss') {
    res.status(403).json({
      ok: false,
      error: 'Reserved BIGGBOSS account requires a verified Admin Setup Key.'
    });
    return;
  }

  const firestore = db();
  const usernameRef = firestore.collection('usernames').doc(username);
  const credRef = firestore.collection('authCredentials').doc(username);
  const displayRef = firestore.collection('displayNames').doc(normalizedDisplay);

  const [preUserSnap, preCredSnap, preNameSnap] = await Promise.all([
    usernameRef.get(),
    credRef.get(),
    displayRef.get()
  ]);
  if (preUserSnap.exists || preCredSnap.exists) {
    res.status(409).json({ ok: false, error: `Username "${username}" is already taken.` });
    return;
  }
  if (preNameSnap.exists) {
    res.status(409).json({ ok: false, error: `Display name "${rawDisplayName}" is already taken.` });
    return;
  }

  let createdAuthUser = null;
  try {
    createdAuthUser = await getAuth().createUser({ displayName: rawDisplayName });
    const uid = createdAuthUser.uid;
    const playerId = `GH-${uid.substring(0, 6).toUpperCase()}`;
    const role = grantAdmin ? 'ADMIN' : 'PLAYER';
    const recoveryCode = generateRecoveryCode();
    const pwdDigest = hashWithSalt(password);
    const recDigest = hashWithSalt(recoveryCode.replace(/-/g, ''));

    await firestore.runTransaction(async (tx) => {
      const [uSnap, cSnap, dSnap] = await Promise.all([
        tx.get(usernameRef),
        tx.get(credRef),
        tx.get(displayRef)
      ]);
      if (uSnap.exists || cSnap.exists) {
        throw Object.assign(new Error(`Username "${username}" is already taken.`), { statusCode: 409 });
      }
      if (dSnap.exists) {
        throw Object.assign(new Error(`Display name "${rawDisplayName}" is already taken.`), { statusCode: 409 });
      }

      tx.set(usernameRef, {
        userId: uid,
        username,
        createdAt: FieldValue.serverTimestamp()
      });

      tx.set(credRef, {
        userId: uid,
        username,
        passwordHash: pwdDigest.hash,
        passwordSalt: pwdDigest.salt,
        recoveryHash: recDigest.hash,
        recoverySalt: recDigest.salt,
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp()
      });

      tx.set(displayRef, {
        userId: uid,
        displayName: rawDisplayName,
        createdAt: FieldValue.serverTimestamp()
      });

      tx.set(firestore.collection('users').doc(uid), {
        userId: uid,
        username,
        displayName: rawDisplayName,
        normalizedName: normalizedDisplay,
        playerId,
        characterId,
        role,
        seasonId,
        banned: false,
        evictedSeasons: 0,
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp()
      });

      if (grantAdmin) {
        tx.set(firestore.collection('admins').doc(uid), {
          userId: uid,
          role: 'ADMIN',
          grantedBy: 'biggboss-server-key',
          createdAt: FieldValue.serverTimestamp()
        });
      }
    });

    if (grantAdmin) {
      try {
        await getDatabase().ref(`admins/${createdAuthUser.uid}`).set(true);
      } catch (rtdbErr) {
        console.warn('RTDB admin mirror skipped:', rtdbErr.message);
      }
    }

    const claims = grantAdmin ? { admin: true, role: 'ADMIN' } : { admin: false, role: 'PLAYER' };
    await getAuth().setCustomUserClaims(createdAuthUser.uid, claims);
    const customToken = await getAuth().createCustomToken(createdAuthUser.uid, claims);

    res.status(201).json({
      ok: true,
      customToken,
      recoveryCode,
      uid: createdAuthUser.uid,
      playerId,
      username,
      displayName: rawDisplayName,
      characterId,
      role
    });
  } catch (err) {
    if (createdAuthUser) {
      try {
        await getAuth().deleteUser(createdAuthUser.uid);
      } catch (_) {}
    }
    const status = err.statusCode || 500;
    const msg = status >= 500 ? formatTokenMintError(err) : (err.message || 'Registration failed.');
    res.status(status).json({ ok: false, error: msg });
  }
});

exports.authLogin = onRequest(AUTH_HTTP_OPTIONS, async (req, res) => {
  if (req.method !== 'POST') {
    res.status(405).json({ ok: false, error: 'Method not allowed. Use POST.' });
    return;
  }

  const body = parseJsonBody(req);
  const username = String(body.username || '').trim().toLowerCase();
  const password = String(body.password || '');

  if (!/^[a-z0-9_]{3,24}$/.test(username) || !password) {
    res.status(400).json({ ok: false, error: 'Valid username (3-24 chars) and password are required.' });
    return;
  }

  try {
    const firestore = db();
    const credSnap = await firestore.collection('authCredentials').doc(username).get();
    if (!credSnap.exists) {
      res.status(401).json({ ok: false, error: 'Invalid username or password.' });
      return;
    }

    const cred = credSnap.data();
    const valid = safeCompareSecret(password, cred.passwordHash, cred.passwordSalt);
    if (!valid) {
      res.status(401).json({ ok: false, error: 'Invalid username or password.' });
      return;
    }

    const uid = cred.userId;
    const [userSnap, adminSnap] = await Promise.all([
      firestore.collection('users').doc(uid).get(),
      firestore.collection('admins').doc(uid).get()
    ]);

    if (userSnap.exists && userSnap.data().banned === true) {
      res.status(403).json({
        ok: false,
        error: userSnap.data().banReason || 'Account is banned by Admin Control Room.'
      });
      return;
    }

    const isAdmin = adminSnap.exists && adminSnap.data().role === 'ADMIN';
    const role = isAdmin ? 'ADMIN' : 'PLAYER';
    const claims = isAdmin ? { admin: true, role: 'ADMIN' } : { admin: false, role: 'PLAYER' };

    if (isAdmin) {
      try {
        await getDatabase().ref(`admins/${uid}`).set(true);
      } catch (_) {}
    }

    await getAuth().setCustomUserClaims(uid, claims);
    const customToken = await getAuth().createCustomToken(uid, claims);
    const u = userSnap.exists ? userSnap.data() : {};

    res.status(200).json({
      ok: true,
      customToken,
      uid,
      playerId: u.playerId || `GH-${uid.substring(0, 6).toUpperCase()}`,
      username,
      displayName: u.displayName || username,
      characterId: u.characterId || 'Player 01',
      role
    });
  } catch (err) {
    res.status(500).json({ ok: false, error: formatTokenMintError(err) });
  }
});

exports.authResetPassword = onRequest(AUTH_HTTP_OPTIONS, async (req, res) => {
  if (req.method !== 'POST') {
    res.status(405).json({ ok: false, error: 'Method not allowed. Use POST.' });
    return;
  }

  const body = parseJsonBody(req);
  const username = String(body.username || '').trim().toLowerCase();
  const recoveryCode = String(body.recoveryCode || '').trim().toUpperCase().replace(/[^A-Z0-9]/g, '');
  const newPassword = String(body.newPassword || '');

  if (!/^[a-z0-9_]{3,24}$/.test(username) || !recoveryCode || newPassword.length < 8 || newPassword.length > 128) {
    res.status(400).json({
      ok: false,
      error: 'Provide a valid username, recovery code, and a new password (8-128 characters).'
    });
    return;
  }

  try {
    const firestore = db();
    const credRef = firestore.collection('authCredentials').doc(username);
    const credSnap = await credRef.get();
    if (!credSnap.exists) {
      res.status(401).json({ ok: false, error: 'Invalid username or recovery code.' });
      return;
    }

    const cred = credSnap.data();
    const valid = safeCompareSecret(recoveryCode, cred.recoveryHash, cred.recoverySalt);
    if (!valid) {
      res.status(401).json({ ok: false, error: 'Invalid username or recovery code.' });
      return;
    }

    const nextPwd = hashWithSalt(newPassword);
    const nextRecoveryCode = generateRecoveryCode();
    const nextRec = hashWithSalt(nextRecoveryCode.replace(/-/g, ''));

    await credRef.update({
      passwordHash: nextPwd.hash,
      passwordSalt: nextPwd.salt,
      recoveryHash: nextRec.hash,
      recoverySalt: nextRec.salt,
      updatedAt: FieldValue.serverTimestamp()
    });

    await getAuth().revokeRefreshTokens(cred.userId);
    res.status(200).json({
      ok: true,
      newRecoveryCode: nextRecoveryCode,
      message: 'Password reset succeeded. Save your new one-time recovery code.'
    });
  } catch (err) {
    res.status(500).json({ ok: false, error: formatTokenMintError(err) });
  }
});

// ============================================================================
// PHASE 9: CHATGPT AI CONTROL ROOM — SERVER-SIDE SCHEDULER & EMERGENCY STOP
// ============================================================================

async function generateHindiHouseText({ category, title, fallbackHindi }) {
  const openAiKey = process.env.OPENAI_API_KEY;
  if (!openAiKey) {
    return { text: fallbackHindi, provider: 'RULE_ENGINE_NO_API_KEY' };
  }
  try {
    const resp = await fetch('https://api.openai.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${openAiKey}`
      },
      body: JSON.stringify({
        model: 'gpt-4o-mini',
        temperature: 0.5,
        max_tokens: 120,
        messages: [
          {
            role: 'system',
            content: 'You are Bigg Boss House Manager AI. Write one crisp dramatic Hindi house announcement under 180 chars.'
          },
          {
            role: 'user',
            content: `Category: ${category}. Context: ${title}. Base message: ${fallbackHindi}`
          }
        ]
      })
    });
    if (!resp.ok) {
      return { text: fallbackHindi, provider: 'RULE_ENGINE_NO_API_KEY' };
    }
    const json = await resp.json();
    const out = String(json?.choices?.[0]?.message?.content || '').trim();
    return { text: out ? out.slice(0, 280) : fallbackHindi, provider: 'OPENAI_ENV_KEY' };
  } catch (_) {
    return { text: fallbackHindi, provider: 'RULE_ENGINE_NO_API_KEY' };
  }
}

async function runPhase9AiAutomationForSeason({ firestore, seasonId, actorUid, source }) {
  const seasonRef = firestore.collection('seasons').doc(seasonId);
  const configRef = seasonRef.collection('aiConfig').doc('house_manager');
  const now = Date.now();
  const hasOpenAiKey = Boolean(process.env.OPENAI_API_KEY);
  const providerTag = hasOpenAiKey ? 'OPENAI_ENV_KEY' : 'RULE_ENGINE_NO_API_KEY';

  const configSnap = await configRef.get();
  if (!configSnap.exists) {
    return { ok: true, skipped: 'ai_config_not_initialized', executedCount: 0, proposedCount: 0 };
  }
  const config = configSnap.data();
  if (!config.enabled || config.emergencyStopped || config.mode === 'MANUAL') {
    return { ok: true, skipped: 'ai_disabled_or_manual', executedCount: 0, proposedCount: 0 };
  }

  const effectiveMode = config.mode; // ASSIST | AUTO
  const schedulesSnap = await seasonRef.collection('aiSchedules').where('status', '==', 'PENDING').get();
  let executedCount = 0;
  let proposedCount = 0;

  for (const schedDoc of schedulesSnap.docs) {
    const sched = schedDoc.data();
    if (Number(sched.targetTimeMillis || 0) > now) continue;

    const slotKey = sched.recurrenceMinutes > 0
      ? `${sched.executionKey}_${Math.floor(now / (sched.recurrenceMinutes * 60000))}`
      : sched.executionKey;
    const logId = `log_${slotKey}`.replace(/[^a-zA-Z0-9_-]/g, '_').slice(0, 120);
    const modeToUse = sched.modeOverride && sched.modeOverride !== 'INHERIT' ? sched.modeOverride : effectiveMode;
    if (modeToUse === 'MANUAL') continue;

    const logRef = seasonRef.collection('aiLogs').doc(logId);
    const proposalRef = seasonRef.collection('aiProposals').doc(`prop_${slotKey}`.slice(0, 120));

    const aiGen = await generateHindiHouseText({
      category: sched.actionType,
      title: sched.title,
      fallbackHindi: sched.messageOrPayload || `Bigg Boss chahte hain: ${sched.title}`
    });

    const outcome = await firestore.runTransaction(async (tx) => {
      const [freshConfigSnap, existingLogSnap, existingPropSnap] = await Promise.all([
        tx.get(configRef),
        tx.get(logRef),
        tx.get(proposalRef)
      ]);
      const freshConfig = freshConfigSnap.data();
      if (!freshConfig || !freshConfig.enabled || freshConfig.emergencyStopped) {
        return 'STOPPED';
      }
      if (existingLogSnap.exists) {
        return 'DUPLICATE';
      }

      if (modeToUse === 'ASSIST') {
        if (existingPropSnap.exists) return 'DUPLICATE';
        tx.set(proposalRef, {
          seasonId,
          category: sched.actionType,
          title: sched.title,
          summary: `AI Schedule Ready: ${sched.title} (${source})`,
          hindiAnnouncement: aiGen.text,
          payloadJson: JSON.stringify({ scheduleId: schedDoc.id, actionType: sched.actionType }),
          status: 'PENDING',
          executionKey: slotKey,
          reviewedBy: null,
          createdAt: FieldValue.serverTimestamp(),
          updatedAt: FieldValue.serverTimestamp()
        });
        tx.set(logRef, {
          seasonId,
          category: sched.actionType,
          mode: 'ASSIST',
          source,
          title: `Proposal: ${sched.title}`.slice(0, 100),
          detail: `Queued approval proposal (${aiGen.provider})`,
          hindiAnnouncement: aiGen.text,
          executionKey: slotKey,
          executedAtMillis: now,
          actorUid,
          acknowledgedByAdmin: source !== 'CLOUD_SCHEDULER' && source !== 'OFFLINE_CATCHUP',
          createdAt: FieldValue.serverTimestamp()
        });
        if (Number(sched.recurrenceMinutes || 0) > 0) {
          tx.update(schedDoc.ref, {
            lastExecutedAtMillis: now,
            targetTimeMillis: now + Number(sched.recurrenceMinutes) * 60000,
            updatedAt: FieldValue.serverTimestamp()
          });
        } else {
          tx.update(schedDoc.ref, {
            status: 'PROPOSED',
            lastExecutedAtMillis: now,
            updatedAt: FieldValue.serverTimestamp()
          });
        }
        return 'PROPOSED';
      }

      // AUTO mode: execute side-effect idempotently
      if (sched.actionType === 'HINDI_ANNOUNCEMENT') {
        const annRef = seasonRef.collection('announcements').doc(`ai_ann_${slotKey}`.slice(0, 120));
        tx.set(annRef, {
          seasonId,
          message: aiGen.text,
          presetId: 'ai_house_manager',
          languageTag: 'hi-IN',
          voiceGender: 'MALE',
          issuedBy: actorUid,
          createdAt: FieldValue.serverTimestamp()
        });
      }

      tx.set(logRef, {
        seasonId,
        category: sched.actionType,
        mode: 'AUTO',
        source,
        title: `Executed: ${sched.title}`.slice(0, 100),
        detail: `${aiGen.text}`.slice(0, 480),
        hindiAnnouncement: aiGen.text,
        executionKey: slotKey,
        executedAtMillis: now,
        actorUid,
        acknowledgedByAdmin: source !== 'CLOUD_SCHEDULER' && source !== 'OFFLINE_CATCHUP',
        createdAt: FieldValue.serverTimestamp()
      });

      if (Number(sched.recurrenceMinutes || 0) > 0) {
        tx.update(schedDoc.ref, {
          lastExecutedAtMillis: now,
          targetTimeMillis: now + Number(sched.recurrenceMinutes) * 60000,
          updatedAt: FieldValue.serverTimestamp()
        });
      } else {
        tx.update(schedDoc.ref, {
          status: 'EXECUTED',
          lastExecutedAtMillis: now,
          updatedAt: FieldValue.serverTimestamp()
        });
      }

      tx.update(configRef, {
        apiProvider: providerTag,
        lastTickAtMillis: now,
        unseenOfflineCount: FieldValue.increment(source === 'CLOUD_SCHEDULER' ? 1 : 0),
        updatedAt: FieldValue.serverTimestamp()
      });
      return 'EXECUTED';
    });

    if (outcome === 'EXECUTED') executedCount += 1;
    if (outcome === 'PROPOSED') proposedCount += 1;
  }

  return { ok: true, apiProvider: providerTag, executedCount, proposedCount };
}

exports.aiSchedulerCron = onSchedule('every 1 minutes', async () => {
  const firestore = db();
  const seasonsSnap = await firestore.collection('seasons').where('status', 'in', ['LOBBY', 'LIVE']).get();
  for (const seasonDoc of seasonsSnap.docs) {
    await runPhase9AiAutomationForSeason({
      firestore,
      seasonId: seasonDoc.id,
      actorUid: 'system_ai_scheduler',
      source: 'CLOUD_SCHEDULER'
    });
  }
});

exports.runAiAutomationTick = onCall(async (request) => {
  const adminUid = await requireAdmin(request);
  const seasonId = String(request.data?.seasonId || 'season_01').trim();
  const source = String(request.data?.source || 'ADMIN_TICK').trim();
  return runPhase9AiAutomationForSeason({
    firestore: db(),
    seasonId,
    actorUid: adminUid,
    source: ['ADMIN_TICK', 'OFFLINE_CATCHUP'].includes(source) ? source : 'ADMIN_TICK'
  });
});

exports.emergencyStopAi = onCall(async (request) => {
  const adminUid = await requireAdmin(request);
  const seasonId = String(request.data?.seasonId || 'season_01').trim();
  const firestore = db();
  const seasonRef = firestore.collection('seasons').doc(seasonId);
  const now = Date.now();

  await firestore.runTransaction(async (tx) => {
    const configRef = seasonRef.collection('aiConfig').doc('house_manager');
    const snap = await tx.get(configRef);
    const providerTag = process.env.OPENAI_API_KEY ? 'OPENAI_ENV_KEY' : 'RULE_ENGINE_NO_API_KEY';
    if (snap.exists) {
      tx.update(configRef, {
        enabled: false,
        mode: 'MANUAL',
        emergencyStopped: true,
        apiProvider: providerTag,
        updatedBy: adminUid,
        updatedAt: FieldValue.serverTimestamp()
      });
    } else {
      tx.set(configRef, {
        seasonId,
        enabled: false,
        mode: 'MANUAL',
        emergencyStopped: true,
        apiProvider: providerTag,
        autoHindiAnnouncements: false,
        autoKitchenMonitor: false,
        autoTaskCoordinator: false,
        autoTimerMonitor: false,
        tickIntervalMinutes: 1,
        lastTickAtMillis: now,
        lastAdminSeenAtMillis: now,
        unseenOfflineCount: 0,
        updatedBy: adminUid,
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp()
      });
    }
    tx.set(seasonRef.collection('aiLogs').doc(`estop_${now}`), {
      seasonId,
      category: 'EMERGENCY_STOP',
      mode: 'OFF',
      source: 'EMERGENCY_STOP',
      title: 'Emergency Stop Triggered',
      detail: 'AI House Manager immediately halted by Admin Control Room.',
      hindiAnnouncement: null,
      executionKey: `estop_${now}`,
      executedAtMillis: now,
      actorUid: adminUid,
      acknowledgedByAdmin: true,
      createdAt: FieldValue.serverTimestamp()
    });
  });

  return { ok: true, emergencyStopped: true };
});

// =========================================================================
// PHASE 10 + 11: SERVER-SIDE ADMIN AUTH, VOTING TALLY & VIP PRIVILEGES
// =========================================================================

const ADMIN_ACCOUNT_UID = 'biggboss_admin';
const ADMIN_CRED_DOC = 'BIGGBOSS';
const ADMIN_SESSION_TTL_MS = 2 * 60 * 60 * 1000; // 2 hours
const ADMIN_RATE_WINDOW_MS = 15 * 60 * 1000; // 15 minutes
const ADMIN_MAX_FAILED_ATTEMPTS = 5;

async function verifyBearerAdmin(req) {
  const authHeader = String(req.headers?.authorization || '');
  const match = /^Bearer\s+(.+)$/i.exec(authHeader.trim());
  if (!match) {
    throw new HttpsError('unauthenticated', 'Missing Authorization Bearer token.');
  }
  const decoded = await getAuth().verifyIdToken(match[1]);
  const ok = await isTrustedAdmin(decoded.uid, decoded);
  if (!ok) {
    throw new HttpsError('permission-denied', 'Admin session is missing, expired, or unauthorized.');
  }
  return decoded;
}

async function appendServerAdminAudit(firestore, seasonId, actionType, summary, actorUid) {
  await firestore.collection('seasons').doc(seasonId).collection('adminAuditLogs').doc().set({
    seasonId,
    actionType,
    summary: String(summary || actionType).slice(0, 300),
    actorUid,
    createdAt: FieldValue.serverTimestamp()
  });
}

exports.adminVerifyLogin = onRequest(
  { ...AUTH_HTTP_OPTIONS, secrets: [adminInitialPasswordSecret] },
  async (req, res) => {
  if (req.method !== 'POST') {
    res.status(405).json({ ok: false, error: 'Method not allowed. Use POST.' });
    return;
  }

  const body = parseJsonBody(req);
  const rawUsername = String(body.username || '').trim();
  const password = String(body.password || '');
  const seasonId = String(body.seasonId || 'season_01').trim() || 'season_01';

  if (!rawUsername || !password) {
    res.status(400).json({ ok: false, error: 'Admin username and password are required.' });
    return;
  }

  const firestore = db();
  const rateRef = firestore.collection('adminRateLimits').doc('biggboss');
  const now = Date.now();

  try {
    const rateCheck = await firestore.runTransaction(async (tx) => {
      const snap = await tx.get(rateRef);
      const data = snap.exists ? (snap.data() || {}) : {};
      const lockedUntil = Number(data.lockedUntilMillis || 0);
      if (lockedUntil > now) {
        return { blocked: true, retryAfterSeconds: Math.ceil((lockedUntil - now) / 1000) };
      }
      return { blocked: false, data };
    });

    if (rateCheck.blocked) {
      res.status(429).json({
        ok: false,
        error: `Too many failed Admin login attempts. Try again in ${rateCheck.retryAfterSeconds}s.`,
        retryAfterSeconds: rateCheck.retryAfterSeconds
      });
      return;
    }

    const recordFailure = async () => {
      await firestore.runTransaction(async (tx) => {
        const snap = await tx.get(rateRef);
        const cur = snap.exists ? (snap.data() || {}) : {};
        const windowStart = Number(cur.windowStartMillis || 0);
        const inWindow = windowStart > 0 && (now - windowStart) <= ADMIN_RATE_WINDOW_MS;
        const attempts = (inWindow ? Number(cur.attempts || 0) : 0) + 1;
        const lockedUntilMillis = attempts >= ADMIN_MAX_FAILED_ATTEMPTS ? now + ADMIN_RATE_WINDOW_MS : 0;
        tx.set(rateRef, {
          attempts,
          windowStartMillis: inWindow ? windowStart : now,
          lockedUntilMillis,
          updatedAt: FieldValue.serverTimestamp()
        });
      });
    };

    if (rawUsername.toLowerCase() !== 'biggboss') {
      await recordFailure();
      res.status(401).json({ ok: false, error: 'Invalid Admin username or password.' });
      return;
    }

    const credRef = firestore.collection('adminCredentials').doc(ADMIN_CRED_DOC);
    let credSnap = await credRef.get();

    if (!credSnap.exists) {
      const initialSecret = getInitialAdminPasswordSecret();
      if (!initialSecret || initialSecret.length < 6) {
        res.status(503).json({
          ok: false,
          error: 'Server secret BIGGBOSS_ADMIN_INITIAL_PASSWORD is not configured in Firebase Cloud Functions.'
        });
        return;
      }
      const { hash, salt } = hashWithSalt(initialSecret);
      await credRef.set({
        username: ADMIN_CRED_DOC,
        passwordHash: hash,
        passwordSalt: salt,
        version: 1,
        rotatedInApp: false,
        mustChangeInitialPassword: true,
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp()
      });
      credSnap = await credRef.get();
    }

    let cred = credSnap.data() || {};
    let passwordMatches = safeCompareSecret(password, cred.passwordHash, cred.passwordSalt);

    // If the password hasn't been rotated in-app yet and Secret Manager holds a newer secret,
    // verify against the configured secret and re-seed the v1 scrypt hash cleanly.
    if (!passwordMatches && cred.rotatedInApp !== true && Number(cred.version || 1) === 1) {
      const initialSecret = getInitialAdminPasswordSecret();
      if (initialSecret && initialSecret.length >= 6 && safeComparePlain(password, initialSecret)) {
        const { hash, salt } = hashWithSalt(initialSecret);
        await credRef.set({
          username: ADMIN_CRED_DOC,
          passwordHash: hash,
          passwordSalt: salt,
          version: 1,
          rotatedInApp: false,
          mustChangeInitialPassword: true,
          updatedAt: FieldValue.serverTimestamp()
        }, { merge: true });
        cred = { ...cred, passwordHash: hash, passwordSalt: salt, rotatedInApp: false, mustChangeInitialPassword: true };
        passwordMatches = true;
      }
    }

    if (!passwordMatches) {
      await recordFailure();
      res.status(401).json({ ok: false, error: 'Invalid Admin username or password.' });
      return;
    }

    const mustChangeInitialPassword = cred.rotatedInApp === true
      ? false
      : Boolean(cred.mustChangeInitialPassword !== false && Number(cred.version || 1) === 1);

    // Clear failed attempts counter
    await rateRef.delete().catch(() => {});

    // Ensure Auth user & Firestore/RTDB Admin markers exist
    try {
      await getAuth().getUser(ADMIN_ACCOUNT_UID);
    } catch (_) {
      await getAuth().createUser({
        uid: ADMIN_ACCOUNT_UID,
        displayName: ADMIN_CRED_DOC
      });
    }

    const adminSessionId = crypto.randomBytes(16).toString('hex');
    const expiresAtMillis = now + ADMIN_SESSION_TTL_MS;

    await Promise.all([
      firestore.collection('users').doc(ADMIN_ACCOUNT_UID).set({
        userId: ADMIN_ACCOUNT_UID,
        displayName: ADMIN_CRED_DOC,
        normalizedName: 'biggboss',
        playerId: 'GH-ADMIN',
        characterId: 'Player 01',
        role: 'ADMIN',
        seasonId,
        banned: false,
        evictedSeasons: 0,
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp()
      }, { merge: true }),
      firestore.collection('admins').doc(ADMIN_ACCOUNT_UID).set({
        userId: ADMIN_ACCOUNT_UID,
        username: ADMIN_CRED_DOC,
        role: 'ADMIN',
        grantedBy: 'adminVerifyLogin',
        updatedAt: FieldValue.serverTimestamp()
      }, { merge: true }),
      firestore.collection('adminSessions').doc(ADMIN_ACCOUNT_UID).set({
        uid: ADMIN_ACCOUNT_UID,
        username: ADMIN_CRED_DOC,
        sessionId: adminSessionId,
        status: 'ACTIVE',
        expiresAtMillis,
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp()
      }),
      getAuth().setCustomUserClaims(ADMIN_ACCOUNT_UID, {
        admin: true,
        role: 'ADMIN',
        adminSessionId,
        adminExpiresAt: expiresAtMillis
      }),
      appendServerAdminAudit(
        firestore,
        seasonId,
        'ADMIN_LOGIN',
        `Admin Control Room authenticated as ${ADMIN_CRED_DOC}.`,
        ADMIN_ACCOUNT_UID
      )
    ]);

    try {
      await getDatabase().ref(`admins/${ADMIN_ACCOUNT_UID}`).set(true);
    } catch (rtdbErr) {
      console.warn('RTDB admin marker skipped:', rtdbErr.message);
    }

    const customToken = await getAuth().createCustomToken(ADMIN_ACCOUNT_UID, {
      admin: true,
      role: 'ADMIN',
      adminSessionId,
      adminExpiresAt: expiresAtMillis
    });

    res.status(200).json({
      ok: true,
      customToken,
      uid: ADMIN_ACCOUNT_UID,
      username: ADMIN_CRED_DOC,
      role: 'ADMIN',
      sessionId: adminSessionId,
      expiresAtMillis,
      mustChangeInitialPassword
    });
  } catch (err) {
    res.status(500).json({ ok: false, error: formatTokenMintError(err) });
  }
});

exports.adminChangePassword = onRequest(AUTH_HTTP_OPTIONS, async (req, res) => {
  if (req.method !== 'POST') {
    res.status(405).json({ ok: false, error: 'Method not allowed. Use POST.' });
    return;
  }

  try {
    const decoded = await verifyBearerAdmin(req);
    const body = parseJsonBody(req);
    const currentPassword = String(body.currentPassword || '');
    const newPassword = String(body.newPassword || '');
    const seasonId = String(body.seasonId || 'season_01').trim() || 'season_01';

    if (newPassword.length < 8 || newPassword.length > 128) {
      res.status(400).json({ ok: false, error: 'New password must be between 8 and 128 characters.' });
      return;
    }

    const firestore = db();
    const credRef = firestore.collection('adminCredentials').doc(ADMIN_CRED_DOC);
    const credSnap = await credRef.get();
    if (!credSnap.exists) {
      res.status(404).json({ ok: false, error: 'Admin credential record not initialized.' });
      return;
    }
    const cred = credSnap.data() || {};
    if (!safeCompareSecret(currentPassword, cred.passwordHash, cred.passwordSalt)) {
      res.status(401).json({ ok: false, error: 'Current Admin password is incorrect.' });
      return;
    }

    const { hash, salt } = hashWithSalt(newPassword);
    const now = Date.now();
    const adminSessionId = crypto.randomBytes(16).toString('hex');
    const expiresAtMillis = now + ADMIN_SESSION_TTL_MS;

    await Promise.all([
      credRef.update({
        passwordHash: hash,
        passwordSalt: salt,
        version: Number(cred.version || 1) + 1,
        rotatedInApp: true,
        mustChangeInitialPassword: false,
        updatedAt: FieldValue.serverTimestamp()
      }),
      firestore.collection('adminSessions').doc(decoded.uid).set({
        uid: decoded.uid,
        username: ADMIN_CRED_DOC,
        sessionId: adminSessionId,
        status: 'ACTIVE',
        expiresAtMillis,
        updatedAt: FieldValue.serverTimestamp()
      }, { merge: true }),
      getAuth().revokeRefreshTokens(decoded.uid),
      getAuth().setCustomUserClaims(decoded.uid, {
        admin: true,
        role: 'ADMIN',
        adminSessionId,
        adminExpiresAt: expiresAtMillis
      }),
      appendServerAdminAudit(
        firestore,
        seasonId,
        'ADMIN_PASSWORD_CHANGED',
        'Admin password rotated server-side and active session refreshed.',
        decoded.uid
      )
    ]);

    const customToken = await getAuth().createCustomToken(decoded.uid, {
      admin: true,
      role: 'ADMIN',
      adminSessionId,
      adminExpiresAt: expiresAtMillis
    });

    res.status(200).json({
      ok: true,
      customToken,
      sessionId: adminSessionId,
      expiresAtMillis,
      mustChangeInitialPassword: false
    });
  } catch (err) {
    const code = err?.code === 'unauthenticated' ? 401 : (err?.code === 'permission-denied' ? 403 : 500);
    res.status(code).json({ ok: false, error: formatTokenMintError(err) });
  }
});

exports.adminLogout = onRequest(AUTH_HTTP_OPTIONS, async (req, res) => {
  if (req.method !== 'POST') {
    res.status(405).json({ ok: false, error: 'Method not allowed. Use POST.' });
    return;
  }

  try {
    const decoded = await verifyBearerAdmin(req);
    const body = parseJsonBody(req);
    const seasonId = String(body.seasonId || 'season_01').trim() || 'season_01';
    const firestore = db();

    await Promise.all([
      firestore.collection('adminSessions').doc(decoded.uid).set({
        status: 'REVOKED',
        updatedAt: FieldValue.serverTimestamp()
      }, { merge: true }),
      getAuth().revokeRefreshTokens(decoded.uid),
      appendServerAdminAudit(firestore, seasonId, 'ADMIN_LOGOUT', 'Admin logged out safely.', decoded.uid)
    ]);

    try {
      await getDatabase().ref(`seasons/${seasonId}/adminPresence/${decoded.uid}`).update({
        online: false,
        state: 'offline',
        updatedAt: Date.now()
      });
    } catch (_) {}

    res.status(200).json({ ok: true });
  } catch (err) {
    const code = err?.code === 'unauthenticated' ? 401 : (err?.code === 'permission-denied' ? 403 : 500);
    res.status(code).json({ ok: false, error: err?.message || 'Admin logout failed.' });
  }
});

exports.adminCheckSession = onRequest(AUTH_HTTP_OPTIONS, async (req, res) => {
  if (req.method !== 'GET' && req.method !== 'POST') {
    res.status(405).json({ ok: false, error: 'Method not allowed.' });
    return;
  }

  try {
    const decoded = await verifyBearerAdmin(req);
    const sessSnap = await db().collection('adminSessions').doc(decoded.uid).get();
    const sess = sessSnap.exists ? (sessSnap.data() || {}) : {};
    const exp = Number(sess.expiresAtMillis || 0);
    if (!sessSnap.exists || sess.status !== 'ACTIVE' || exp <= Date.now()) {
      res.status(401).json({
        ok: false,
        active: false,
        error: 'Admin session invalid or expired.'
      });
      return;
    }
    res.status(200).json({
      ok: true,
      active: true,
      uid: decoded.uid,
      username: sess.username || ADMIN_CRED_DOC,
      sessionId: sess.sessionId || '',
      expiresAtMillis: exp
    });
  } catch (err) {
    const code = err?.code === 'unauthenticated' ? 401 : (err?.code === 'permission-denied' ? 403 : 401);
    res.status(code).json({ ok: false, active: false, error: err?.message || 'Admin session invalid or expired.' });
  }
});

async function computeVotingTallyOnServer({ seasonId, cycleId, closeVoting, adminUid }) {
  const firestore = db();
  const seasonRef = firestore.collection('seasons').doc(seasonId);
  const cycleRef = seasonRef.collection('votingCycles').doc(cycleId);

  const [seasonSnap, cycleSnap, votesSnap] = await Promise.all([
    seasonRef.get(),
    cycleRef.get(),
    cycleRef.collection('votes').where('seasonId', '==', seasonId).where('cycleId', '==', cycleId).get()
  ]);

  if (!cycleSnap.exists) {
    throw new HttpsError('not-found', `Voting cycle ${cycleId} not found.`);
  }

  const season = seasonSnap.exists ? (seasonSnap.data() || {}) : {};
  const cycle = cycleSnap.data() || {};
  const captainUid = String(season.captainUid || '');
  const captainImmunity = Boolean(season.captainImmunity);
  const captainDoubleVote = Boolean(season.captainDoubleVote);
  const npcEnabled = Boolean(cycle.npcVotesEnabled);
  const voteType = String(cycle.voteType || 'EVICT').toUpperCase() === 'SAVE' ? 'SAVE' : 'EVICT';

  const nomineesRaw = Array.isArray(cycle.nomineeSummaries) ? cycle.nomineeSummaries : [];
  const tallies = {};
  nomineesRaw.forEach((raw) => {
    let uid = '';
    let displayName = '';
    if (typeof raw === 'string') {
      const parts = raw.split('|');
      uid = parts[0] || '';
      displayName = parts[2] || parts[0] || '';
    } else if (raw && typeof raw === 'object') {
      uid = String(raw.userId || raw.uid || '');
      displayName = String(raw.displayName || uid);
    }
    if (uid) {
      tallies[uid] = {
        uid,
        displayName,
        realVotes: 0,
        npcVotes: 0,
        totalVotes: 0,
        immune: Boolean(captainImmunity && captainUid && uid === captainUid)
      };
    }
  });

  let totalReal = 0;
  let totalNpc = 0;
  votesSnap.forEach((doc) => {
    const v = doc.data() || {};
    const targetUid = String(v.targetUid || '');
    const slot = tallies[targetUid];
    if (!slot) return;
    const isNpc = String(v.sourceType || '').toUpperCase() === 'NPC';
    if (isNpc) {
      if (!npcEnabled) return;
      slot.npcVotes += 1;
      slot.totalVotes += 1;
      totalNpc += 1;
    } else {
      const weight = (captainDoubleVote && captainUid && v.voterUid === captainUid) ? 2 : 1;
      slot.realVotes += weight;
      slot.totalVotes += weight;
      totalReal += weight;
    }
  });

  const entries = Object.values(tallies).sort((a, b) =>
    voteType === 'SAVE' ? (a.totalVotes - b.totalVotes) : (b.totalVotes - a.totalVotes)
  );
  const eligibleForEviction = entries.filter((e) => !e.immune);
  const topScore = eligibleForEviction.length > 0 ? eligibleForEviction[0].totalVotes : null;
  const leadingEvictionUids = topScore === null
    ? []
    : eligibleForEviction.filter((e) => e.totalVotes === topScore).map((e) => e.uid);
  const isTie = leadingEvictionUids.length > 1;
  const now = Date.now();

  const summaryJson = JSON.stringify({
    voteType,
    isTie,
    leadingEvictionUids,
    entries
  }).slice(0, 1950);

  const cycleUpdates = {
    serverTallyJson: summaryJson,
    serverTotalRealVotes: totalReal,
    serverTotalNpcVotes: totalNpc,
    serverIsTie: isTie,
    serverTalliedAtMillis: now,
    updatedAt: FieldValue.serverTimestamp()
  };
  if (closeVoting && ['OPEN', 'NOMINATIONS'].includes(String(cycle.status || '').toUpperCase())) {
    cycleUpdates.status = 'CLOSED';
  }

  const batch = firestore.batch();
  batch.update(cycleRef, cycleUpdates);
  batch.set(seasonRef.collection('adminControls').doc(), {
    seasonId,
    commandType: 'VOTING_STATE',
    payloadSummary: `Server tally for ${cycle.title || cycleId}: Real=${totalReal}, NPC=${totalNpc}, Tie=${isTie}`,
    issuedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });
  await batch.commit();
  await appendServerAdminAudit(
    firestore,
    seasonId,
    'VOTING_SERVER_TALLY',
    `Server calculated voting tally (${voteType}) for ${cycle.title || cycleId}: Real=${totalReal}, NPC=${totalNpc}, Tie=${isTie}`,
    adminUid
  );

  return {
    ok: true,
    cycleId,
    closed: Boolean(cycleUpdates.status === 'CLOSED'),
    isTie,
    leadingEvictionUids,
    totalRealVotes: totalReal,
    totalNpcVotes: totalNpc,
    entries,
    talliedAtMillis: now
  };
}

exports.calculateAndCloseVoting = onRequest(AUTH_HTTP_OPTIONS, async (req, res) => {
  if (req.method !== 'POST') {
    res.status(405).json({ ok: false, error: 'Method not allowed. Use POST.' });
    return;
  }
  try {
    const decoded = await verifyBearerAdmin(req);
    const body = parseJsonBody(req);
    const seasonId = String(body.seasonId || 'season_01').trim() || 'season_01';
    const cycleId = String(body.cycleId || '').trim();
    const closeVoting = body.closeVoting !== false;
    if (!cycleId) {
      res.status(400).json({ ok: false, error: 'cycleId is required.' });
      return;
    }
    const result = await computeVotingTallyOnServer({
      seasonId,
      cycleId,
      closeVoting,
      adminUid: decoded.uid
    });
    res.status(200).json(result);
  } catch (err) {
    const code = err?.code === 'unauthenticated' ? 401 : (err?.code === 'permission-denied' ? 403 : (err?.code === 'not-found' ? 404 : 500));
    res.status(code).json({ ok: false, error: err?.message || 'Failed to compute voting tally.' });
  }
});

exports.calculateAndCloseVotingCall = onCall(async (request) => {
  const adminUid = await requireAdmin(request);
  const seasonId = String(request.data?.seasonId || 'season_01').trim() || 'season_01';
  const cycleId = String(request.data?.cycleId || '').trim();
  const closeVoting = request.data?.closeVoting !== false;
  if (!cycleId) {
    throw new HttpsError('invalid-argument', 'cycleId is required.');
  }
  return computeVotingTallyOnServer({ seasonId, cycleId, closeVoting, adminUid });
});

/**
 * Admin: Grant or revoke independent VIP Room & badge privileges for any player.
 */
exports.manageVipPrivilege = onCall(async (request) => {
  const adminUid = await requireAdmin(request);
  const seasonId = String(request.data?.seasonId || 'season_01').trim() || 'season_01';
  const targetUid = String(request.data?.targetUid || '').trim();
  const vipEnabled = Boolean(request.data?.vipEnabled);
  const vipLabel = String(request.data?.vipLabel || 'VIP Pass').trim().slice(0, 40) || 'VIP Pass';

  if (!targetUid) {
    throw new HttpsError('invalid-argument', 'targetUid is required.');
  }

  const firestore = db();
  const seasonRef = firestore.collection('seasons').doc(seasonId);
  const userSnap = await firestore.collection('users').doc(targetUid).get();
  const displayName = userSnap.exists ? (userSnap.data()?.displayName || targetUid) : targetUid;
  const playerId = userSnap.exists ? (userSnap.data()?.playerId || '') : '';

  const batch = firestore.batch();
  batch.set(seasonRef.collection('vipPrivileges').doc(targetUid), {
    seasonId,
    userId: targetUid,
    playerId,
    displayName,
    vipEnabled,
    vipRoomAccess: vipEnabled,
    vipLabel,
    updatedBy: adminUid,
    createdAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp()
  });
  batch.set(seasonRef.collection('privilegeHistory').doc(), {
    seasonId,
    targetUid,
    targetDisplayName: displayName,
    actionType: vipEnabled ? 'VIP_GRANTED' : 'VIP_REVOKED',
    detail: vipEnabled ? `Granted ${vipLabel} to ${displayName}` : `Revoked VIP Pass from ${displayName}`,
    issuedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });
  batch.set(seasonRef.collection('adminControls').doc(), {
    seasonId,
    commandType: 'VIP_PRIVILEGE',
    payloadSummary: `${vipEnabled ? 'Granted' : 'Revoked'} VIP (${vipLabel}) for ${displayName}`,
    issuedBy: adminUid,
    createdAt: FieldValue.serverTimestamp()
  });
  await batch.commit();

  try {
    const rtdbRef = getDatabase().ref(`seasons/${seasonId}/vipPlayers/${targetUid}`);
    if (vipEnabled) {
      await rtdbRef.set(true);
    } else {
      await rtdbRef.remove();
    }
  } catch (err) {
    console.warn('RTDB vipPlayers sync skipped:', err.message);
  }

  return { ok: true, targetUid, vipEnabled };
});




