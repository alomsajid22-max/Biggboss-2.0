const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";
const ADMIN_UID = "trusted_admin_999";
const SEASON_ID = "season_01";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated client is completely denied on all collections", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("users").doc(ALICE_UID).get());
  await assertFails(db.collection("seasons").doc(SEASON_ID).get());
  await assertFails(db.collection("displayNames").doc("aria_star").get());
  await assertFails(db.collection("seasons").doc(SEASON_ID).collection("members").where("seasonId", "==", SEASON_ID).get());
  await assertFails(
    db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "AriaStar",
      normalizedName: "ariastar",
      playerId: "GH-ALICE1",
      characterId: "Player 01",
      role: "PLAYER",
      seasonId: SEASON_ID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("New player registration creates PLAYER account & unique display name, blocks self-assigned ADMIN & ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const now = pastDate();

  // Reserve unique display name
  await assertSucceeds(
    aliceDb.collection("displayNames").doc("ariastar").set({
      userId: ALICE_UID,
      displayName: "AriaStar",
      createdAt: now,
    })
  );

  // Create profile as PLAYER
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "AriaStar",
      normalizedName: "ariastar",
      playerId: "GH-ALICE1",
      characterId: "Player 01",
      role: "PLAYER",
      seasonId: SEASON_ID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Allowed partial update (character change)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      characterId: "Player 03",
      updatedAt: pastDate(500),
    })
  );

  // Bob cannot claim Alice's reserved display name
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("displayNames").doc("ariastar").set({
      userId: BOB_UID,
      displayName: "AriaStar",
      createdAt: pastDate(),
    })
  );

  // Bob cannot register himself as ADMIN
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "BobHacker",
      normalizedName: "bobhacker",
      playerId: "GH-BOB002",
      characterId: "Player 02",
      role: "ADMIN",
      seasonId: SEASON_ID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice cannot escalate her own role via update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "ADMIN",
      updatedAt: pastDate(200),
    })
  );

  // Alice cannot write ghost @DocumentId `id` field
  await assertFails(
    aliceDb.collection("users").doc("alice_ghost_id").set({
      id: "alice_ghost_id",
      userId: "alice_ghost_id",
      displayName: "GhostTest",
      normalizedName: "ghosttest",
      playerId: "GH-GHOST1",
      characterId: "Player 01",
      role: "PLAYER",
      seasonId: SEASON_ID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice cannot forge future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      characterId: "Player 04",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("PII Isolation: Bob cannot read or modify Alice's private /users/{uid} document", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "private-alice@example.com",
      displayName: "AriaStar",
      normalizedName: "ariastar",
      playerId: "GH-ALICE1",
      characterId: "Player 01",
      role: "PLAYER",
      seasonId: SEASON_ID,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: pastDate(),
    })
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").where("userId", "==", ALICE_UID).get());
});

test("Shared Season Lobby: enforces 15-player capacity and query filter alignment", async () => {
  const created = pastDate(10000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Bootstrap season_01 cleanly with activeCount: 0
  await assertSucceeds(
    aliceDb.collection("seasons").doc(SEASON_ID).set({
      title: "Season 01 • Glass House",
      status: "LOBBY",
      maxPlayers: 15,
      activeCount: 0,
      dayNightMode: "day",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Filtered season query matches Kotlin repository
  await assertSucceeds(aliceDb.collection("seasons").where("status", "in", ["LOBBY", "LIVE"]).get());
  // Unfiltered list on seasons fails
  await assertFails(aliceDb.collection("seasons").get());

  // Join transactionally within capacity
  const batch = aliceDb.batch();
  batch.update(aliceDb.collection("seasons").doc(SEASON_ID), {
    activeCount: 1,
    updatedAt: pastDate(),
  });
  batch.set(aliceDb.collection("seasons").doc(SEASON_ID).collection("members").doc(ALICE_UID), {
    seasonId: SEASON_ID,
    userId: ALICE_UID,
    playerId: "GH-ALICE1",
    displayName: "AriaStar",
    characterId: "Player 01",
    slotNumber: 1,
    role: "PLAYER",
    status: "IN_HOUSE",
    online: true,
    sessionId: "sess_alice_001",
    joinedAt: pastDate(),
    lastSeenAt: pastDate(),
  });
  await assertSucceeds(batch.commit());

  // Filtered members query matching Kotlin repository succeeds
  await assertSucceeds(
    aliceDb.collection("seasons").doc(SEASON_ID).collection("members").where("seasonId", "==", SEASON_ID).get()
  );

  // Set season activeCount to 15 (full), verify 16th join fails
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("seasons").doc(SEASON_ID).update({
      activeCount: 15,
      updatedAt: pastDate(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const overflowBatch = bobDb.batch();
  overflowBatch.update(bobDb.collection("seasons").doc(SEASON_ID), {
    activeCount: 16,
    updatedAt: pastDate(),
  });
  overflowBatch.set(bobDb.collection("seasons").doc(SEASON_ID).collection("members").doc(BOB_UID), {
    seasonId: SEASON_ID,
    userId: BOB_UID,
    playerId: "GH-BOB002",
    displayName: "BobHousemate",
    characterId: "Player 02",
    slotNumber: 2,
    role: "PLAYER",
    status: "IN_HOUSE",
    online: true,
    sessionId: "sess_bob_002",
    joinedAt: pastDate(),
    lastSeenAt: pastDate(),
  });
  await assertFails(overflowBatch.commit());
});

test("Trusted Admin RBAC: ordinary PLAYER blocked from /admins and /adminControls; trusted /admins/{uid} allowed", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("seasons").doc(SEASON_ID).set({
      title: "Season 01 • Glass House",
      status: "LIVE",
      maxPlayers: 15,
      activeCount: 1,
      dayNightMode: "day",
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
    await db.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      role: "ADMIN",
      grantedBy: "firebase-admin-sdk",
      createdAt: pastDate(10000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Ordinary player cannot write to /admins/{uid}
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      userId: ALICE_UID,
      role: "ADMIN",
      grantedBy: "self",
      createdAt: pastDate(),
    })
  );

  // Ordinary player cannot read or write adminControls
  await assertFails(aliceDb.collection("seasons").doc(SEASON_ID).collection("adminControls").get());
  await assertFails(
    aliceDb.collection("seasons").doc(SEASON_ID).collection("adminControls").doc("cmd_1").set({
      seasonId: SEASON_ID,
      commandType: "DOOR_OVERRIDE",
      payloadSummary: "Open admin door",
      issuedBy: ALICE_UID,
      createdAt: pastDate(),
    })
  );

  // Trusted admin can check own /admins/{uid} grant and issue adminControls
  await assertSucceeds(adminDb.collection("admins").doc(ADMIN_UID).get());
  await assertSucceeds(
    adminDb.collection("seasons").doc(SEASON_ID).collection("adminControls").doc("cmd_1").set({
      seasonId: SEASON_ID,
      commandType: "DOOR_OVERRIDE",
      payloadSummary: "Unlocked Admin Door",
      issuedBy: ADMIN_UID,
      createdAt: pastDate(),
    })
  );
  await assertSucceeds(adminDb.collection("seasons").doc(SEASON_ID).collection("adminControls").get());
});

test("Phase 5 Admin Actions & Denials: Season controls, User listing/banning, Voting Phone, Real vs NPC votes, Evictions & Announcements", async () => {
  const created = pastDate(15000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      role: "ADMIN",
      grantedBy: "firebase-admin-sdk",
      createdAt: created,
    });
    await db.collection("seasons").doc(SEASON_ID).set({
      title: "Season 01 • Glass House",
      status: "LOBBY",
      maxPlayers: 15,
      activeCount: 2,
      dayNightMode: "day",
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "AriaStar",
      normalizedName: "ariastar",
      playerId: "GH-ALICE1",
      characterId: "Player 01",
      role: "PLAYER",
      seasonId: SEASON_ID,
      banned: false,
      evictedSeasons: 0,
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "BobContender",
      normalizedName: "bobcontender",
      playerId: "GH-BOB002",
      characterId: "Player 02",
      role: "PLAYER",
      seasonId: SEASON_ID,
      banned: false,
      evictedSeasons: 0,
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("seasons").doc(SEASON_ID).collection("members").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      userId: ALICE_UID,
      playerId: "GH-ALICE1",
      displayName: "AriaStar",
      characterId: "Player 01",
      slotNumber: 1,
      role: "PLAYER",
      status: "IN_HOUSE",
      online: true,
      sessionId: "sess_alice_001",
      joinedAt: created,
      lastSeenAt: created,
    });
    await db.collection("seasons").doc(SEASON_ID).collection("members").doc(BOB_UID).set({
      seasonId: SEASON_ID,
      userId: BOB_UID,
      playerId: "GH-BOB002",
      displayName: "BobContender",
      characterId: "Player 02",
      slotNumber: 2,
      role: "PLAYER",
      status: "IN_HOUSE",
      online: true,
      sessionId: "sess_bob_002",
      joinedAt: created,
      lastSeenAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // 1. Ordinary player denied on admin-only writes
  await assertFails(aliceDb.collection("users").get());
  await assertFails(aliceDb.collection("seasons").doc(SEASON_ID).update({ status: "LIVE", updatedAt: pastDate() }));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({ banned: true, updatedAt: pastDate() }));
  await assertFails(
    aliceDb.collection("seasons").doc(SEASON_ID).collection("votingCycles").doc("cycle_1").set({
      seasonId: SEASON_ID,
      title: "Fake Cycle",
      status: "OPEN",
      nominatedUids: [BOB_UID],
      nomineeSummaries: [`${BOB_UID}|GH-BOB002|BobContender|Player 02`],
      npcVotesEnabled: false,
      evictedUid: null,
      evictedDisplayName: null,
      createdBy: ALICE_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    aliceDb.collection("seasons").doc(SEASON_ID).collection("announcements").doc("ann_fake").set({
      seasonId: SEASON_ID,
      message: "Fake announcement",
      languageTag: "hi-IN",
      voiceGender: "FEMALE",
      issuedBy: ALICE_UID,
      createdAt: pastDate(),
    })
  );

  // 2. Trusted Admin lists users, starts season, publishes nominations & opens voting
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(adminDb.collection("seasons").doc(SEASON_ID).update({ status: "LIVE", updatedAt: pastDate() }));
  await assertSucceeds(
    adminDb.collection("seasons").doc(SEASON_ID).collection("votingCycles").doc("cycle_1").set({
      seasonId: SEASON_ID,
      title: "Week 1 Eviction",
      status: "OPEN",
      nominatedUids: [BOB_UID],
      nomineeSummaries: [`${BOB_UID}|GH-BOB002|BobContender|Player 02`],
      npcVotesEnabled: true,
      evictedUid: null,
      evictedDisplayName: null,
      createdBy: ADMIN_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // 3. Real player casts REAL_PLAYER vote; denied from forging NPC vote
  await assertFails(
    aliceDb.collection("seasons").doc(SEASON_ID).collection("votingCycles").doc("cycle_1").collection("votes").doc("npc_cheat").set({
      seasonId: SEASON_ID,
      cycleId: "cycle_1",
      voterUid: "npc_cheat",
      voterName: "FakeNPC",
      targetUid: BOB_UID,
      sourceType: "NPC",
      createdAt: pastDate(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("seasons").doc(SEASON_ID).collection("votingCycles").doc("cycle_1").collection("votes").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      cycleId: "cycle_1",
      voterUid: ALICE_UID,
      voterName: "AriaStar",
      targetUid: BOB_UID,
      sourceType: "REAL_PLAYER",
      createdAt: pastDate(),
    })
  );

  // Admin casts optional NPC vote (strictly marked NPC)
  await assertSucceeds(
    adminDb.collection("seasons").doc(SEASON_ID).collection("votingCycles").doc("cycle_1").collection("votes").doc("npc_001").set({
      seasonId: SEASON_ID,
      cycleId: "cycle_1",
      voterUid: "npc_001",
      voterName: "NPC-1",
      targetUid: BOB_UID,
      sourceType: "NPC",
      createdAt: pastDate(),
    })
  );

  // 4. Admin confirms eviction -> records eviction archive, updates cycle, sets member EVICTED, decrements activeCount
  const evictBatch = adminDb.batch();
  evictBatch.set(adminDb.collection("seasons").doc(SEASON_ID).collection("evictions").doc("cycle_1_bob"), {
    seasonId: SEASON_ID,
    cycleId: "cycle_1",
    userId: BOB_UID,
    playerId: "GH-BOB002",
    displayName: "BobContender",
    characterId: "Player 02",
    realVotes: 1,
    npcVotes: 1,
    announcementText: "BobContender has been evicted from the Glass House.",
    evictedBy: ADMIN_UID,
    createdAt: pastDate(),
  });
  evictBatch.update(adminDb.collection("seasons").doc(SEASON_ID).collection("votingCycles").doc("cycle_1"), {
    status: "EVICTED",
    evictedUid: BOB_UID,
    evictedDisplayName: "BobContender",
    updatedAt: pastDate(),
  });
  evictBatch.update(adminDb.collection("seasons").doc(SEASON_ID).collection("members").doc(BOB_UID), {
    status: "EVICTED",
    online: false,
    lastSeenAt: pastDate(),
  });
  evictBatch.update(adminDb.collection("seasons").doc(SEASON_ID), {
    activeCount: 1,
    updatedAt: pastDate(),
  });
  await assertSucceeds(evictBatch.commit());

  // Evicted player cannot rejoin IN_HOUSE
  await assertFails(
    bobDb.collection("seasons").doc(SEASON_ID).collection("members").doc(BOB_UID).update({
      status: "IN_HOUSE",
      online: true,
      lastSeenAt: pastDate(),
    })
  );

  // Admin can broadcast Hindi TTS announcement
  await assertSucceeds(
    adminDb.collection("seasons").doc(SEASON_ID).collection("announcements").doc("ann_hi_1").set({
      seasonId: SEASON_ID,
      message: "बिग बॉस चाहते हैं कि सभी घरवाले लिविंग एरिया में आएं।",
      presetId: "living_gather",
      languageTag: "hi-IN",
      voiceGender: "FEMALE",
      issuedBy: ADMIN_UID,
      createdAt: pastDate(),
    })
  );

  // Admin can ban & unban a player
  await assertSucceeds(
    adminDb.collection("users").doc(BOB_UID).update({
      banned: true,
      banReason: "Rule violation",
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    adminDb.collection("users").doc(BOB_UID).update({
      banned: false,
      banReason: null,
      updatedAt: pastDate(),
    })
  );
});

test("Phase 6 Multi-Account Tasks, Teams, Captaincy, Rules, Violations & Daily Schedule RBAC", async () => {
  const created = pastDate(20000);
  const nowMs = Date.now();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      role: "ADMIN",
      grantedBy: "firebase-admin-sdk",
      createdAt: created,
    });
    await db.collection("seasons").doc(SEASON_ID).set({
      title: "Season 01 • Glass House",
      status: "LIVE",
      maxPlayers: 15,
      activeCount: 2,
      dayNightMode: "day",
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "AriaStar",
      normalizedName: "ariastar",
      playerId: "GH-ALICE1",
      characterId: "Player 01",
      role: "PLAYER",
      seasonId: SEASON_ID,
      banned: false,
      evictedSeasons: 0,
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "BobContender",
      normalizedName: "bobcontender",
      playerId: "GH-BOB002",
      characterId: "Player 02",
      role: "PLAYER",
      seasonId: SEASON_ID,
      banned: false,
      evictedSeasons: 0,
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("seasons").doc(SEASON_ID).collection("members").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      userId: ALICE_UID,
      playerId: "GH-ALICE1",
      displayName: "AriaStar",
      characterId: "Player 01",
      slotNumber: 1,
      role: "PLAYER",
      status: "IN_HOUSE",
      online: true,
      sessionId: "sess_alice_001",
      joinedAt: created,
      lastSeenAt: created,
    });
    await db.collection("seasons").doc(SEASON_ID).collection("members").doc(BOB_UID).set({
      seasonId: SEASON_ID,
      userId: BOB_UID,
      playerId: "GH-BOB002",
      displayName: "BobContender",
      characterId: "Player 02",
      slotNumber: 2,
      role: "PLAYER",
      status: "IN_HOUSE",
      online: true,
      sessionId: "sess_bob_002",
      joinedAt: created,
      lastSeenAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const seasonRef = (db) => db.collection("seasons").doc(SEASON_ID);

  // 1. Ordinary players strictly denied from creating/editing tasks, teams, captaincy, rules, violations, schedule
  await assertFails(
    seasonRef(aliceDb).collection("tasks").doc("task_1").set({
      seasonId: SEASON_ID,
      title: "Rogue Task",
      description: "Player attempt",
      instructions: "Should fail",
      activityType: "QUIZ",
      assignmentMode: "INDIVIDUAL",
      isCaptaincy: false,
      status: "ACTIVE",
      targetRoom: "Living Atrium",
      durationSeconds: 180,
      startTimeMillis: nowMs,
      endTimeMillis: nowMs + 180000,
      promptQuestion: "Q?",
      options: ["A", "B"],
      maxPoints: 100,
      createdBy: ALICE_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    seasonRef(bobDb).collection("teams").doc("team_alpha").set({
      seasonId: SEASON_ID,
      name: "Rogue Team",
      colorHex: "#EAB954",
      memberUids: [BOB_UID],
      score: 100,
      winsCount: 1,
      updatedBy: BOB_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    seasonRef(aliceDb).collection("captaincyHistory").doc("cap_fake").set({
      seasonId: SEASON_ID,
      captainUid: ALICE_UID,
      captainPlayerId: "GH-ALICE1",
      captainDisplayName: "AriaStar",
      captainCharacterId: "Player 01",
      title: "Self Captain",
      scoreAwarded: 100,
      assignedBy: ALICE_UID,
      createdAt: pastDate(),
    })
  );
  await assertFails(
    seasonRef(bobDb).collection("houseRules").doc("rule_fake").set({
      seasonId: SEASON_ID,
      ruleNumber: 9,
      title: "Fake Rule",
      description: "Blocked",
      severity: "LOW",
      active: true,
      updatedBy: BOB_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    seasonRef(aliceDb).collection("dailySchedule").doc("slot_1").set({
      seasonId: SEASON_ID,
      orderIndex: 1,
      activityKey: "MORNING_WAKEUP",
      title: "Morning Wake-Up",
      timeLabel: "08:00 AM",
      location: "Bedroom",
      description: "Blocked",
      isCurrent: true,
      updatedBy: ALICE_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // 2. Trusted Admin creates & starts a live Captaincy Quiz + stores secret answerKey/config
  const taskBatch = adminDb.batch();
  const taskDocAdmin = seasonRef(adminDb).collection("tasks").doc("task_cap_1");
  taskBatch.set(taskDocAdmin, {
    seasonId: SEASON_ID,
    title: "Captaincy Quiz Showdown",
    description: "Answer fast to claim House Captaincy.",
    instructions: "Submit once before countdown ends.",
    activityType: "QUIZ",
    assignmentMode: "TEAM",
    isCaptaincy: true,
    status: "ACTIVE",
    targetRoom: "Living Atrium",
    durationSeconds: 180,
    startTimeMillis: nowMs,
    endTimeMillis: nowMs + 180000,
    promptQuestion: "How many indoor rooms make up the Glass House?",
    options: ["10", "13", "15"],
    maxPoints: 100,
    winningUid: null,
    winningTeamId: null,
    winnerSummary: null,
    createdBy: ADMIN_UID,
    createdAt: pastDate(5000),
    updatedAt: pastDate(5000),
  });
  taskBatch.set(taskDocAdmin.collection("answerKey").doc("config"), {
    seasonId: SEASON_ID,
    taskId: "task_cap_1",
    expectedAnswer: "13",
    updatedBy: ADMIN_UID,
    updatedAt: pastDate(5000),
  });
  await assertSucceeds(taskBatch.commit());

  // Both players read the synchronized task, but are strictly denied from reading secret answerKey
  await assertSucceeds(seasonRef(aliceDb).collection("tasks").where("seasonId", "==", SEASON_ID).get());
  await assertSucceeds(seasonRef(bobDb).collection("tasks").doc("task_cap_1").get());
  await assertFails(seasonRef(aliceDb).collection("tasks").doc("task_cap_1").collection("answerKey").doc("config").get());
  await assertFails(seasonRef(bobDb).collection("tasks").doc("task_cap_1").collection("answerKey").doc("config").get());

  // 3. Admin creates & syncs two Teams; both players can list synchronized teams
  await assertSucceeds(
    seasonRef(adminDb).collection("teams").doc("team_gold").set({
      seasonId: SEASON_ID,
      name: "Team Gold",
      colorHex: "#EAB954",
      memberUids: [ALICE_UID],
      score: 0,
      winsCount: 0,
      updatedBy: ADMIN_UID,
      createdAt: pastDate(4000),
      updatedAt: pastDate(4000),
    })
  );
  await assertSucceeds(
    seasonRef(adminDb).collection("teams").doc("team_cyan").set({
      seasonId: SEASON_ID,
      name: "Team Cyan",
      colorHex: "#22D3EE",
      memberUids: [BOB_UID],
      score: 0,
      winsCount: 0,
      updatedBy: ADMIN_UID,
      createdAt: pastDate(4000),
      updatedAt: pastDate(4000),
    })
  );
  await assertSucceeds(seasonRef(aliceDb).collection("teams").where("seasonId", "==", SEASON_ID).get());
  await assertSucceeds(seasonRef(bobDb).collection("teams").where("seasonId", "==", SEASON_ID).get());

  // 4. Both Alice & Bob submit unique answers; anti-cheat rejects forging VERIFIED or awardedScore > 0 or duplicates
  await assertFails(
    seasonRef(aliceDb).collection("tasks").doc("task_cap_1").collection("submissions").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      taskId: "task_cap_1",
      userId: ALICE_UID,
      playerId: "GH-ALICE1",
      displayName: "AriaStar",
      teamId: "team_gold",
      answerText: "13",
      elapsedSeconds: 12,
      verificationStatus: "VERIFIED",
      awardedScore: 100,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    seasonRef(aliceDb).collection("tasks").doc("task_cap_1").collection("submissions").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      taskId: "task_cap_1",
      userId: ALICE_UID,
      playerId: "GH-ALICE1",
      displayName: "AriaStar",
      teamId: "team_gold",
      answerText: "13",
      elapsedSeconds: 14,
      verificationStatus: "SUBMITTED",
      awardedScore: 0,
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );
  // Duplicate submission or overwriting another player's submission fails
  await assertFails(
    seasonRef(aliceDb).collection("tasks").doc("task_cap_1").collection("submissions").doc(ALICE_UID).update({
      answerText: "15",
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    seasonRef(bobDb).collection("tasks").doc("task_cap_1").collection("submissions").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      taskId: "task_cap_1",
      userId: BOB_UID,
      playerId: "GH-BOB002",
      displayName: "BobContender",
      teamId: "team_cyan",
      answerText: "10",
      elapsedSeconds: 18,
      verificationStatus: "SUBMITTED",
      awardedScore: 0,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    seasonRef(bobDb).collection("tasks").doc("task_cap_1").collection("submissions").doc(BOB_UID).set({
      seasonId: SEASON_ID,
      taskId: "task_cap_1",
      userId: BOB_UID,
      playerId: "GH-BOB002",
      displayName: "BobContender",
      teamId: "team_cyan",
      answerText: "10",
      elapsedSeconds: 20,
      verificationStatus: "SUBMITTED",
      awardedScore: 0,
      createdAt: pastDate(1500),
      updatedAt: pastDate(1500),
    })
  );
  // Alice cannot peek at Bob's submission while task is still ACTIVE
  await assertFails(
    seasonRef(aliceDb).collection("tasks").doc("task_cap_1").collection("submissions").doc(BOB_UID).get()
  );

  // 5. Admin grades submissions, marks task COMPLETED, updates Team Gold score, and promotes Alice to Captain
  const winBatch = adminDb.batch();
  winBatch.update(seasonRef(adminDb).collection("tasks").doc("task_cap_1"), {
    status: "COMPLETED",
    winningUid: ALICE_UID,
    winningTeamId: "team_gold",
    winnerSummary: "AriaStar (100 pts, 14s)",
    updatedAt: pastDate(),
  });
  winBatch.update(seasonRef(adminDb).collection("tasks").doc("task_cap_1").collection("submissions").doc(ALICE_UID), {
    verificationStatus: "VERIFIED",
    awardedScore: 100,
    reviewedBy: ADMIN_UID,
    updatedAt: pastDate(),
  });
  winBatch.update(seasonRef(adminDb).collection("tasks").doc("task_cap_1").collection("submissions").doc(BOB_UID), {
    verificationStatus: "REJECTED",
    awardedScore: 0,
    reviewedBy: ADMIN_UID,
    updatedAt: pastDate(),
  });
  winBatch.update(seasonRef(adminDb).collection("teams").doc("team_gold"), {
    score: 100,
    winsCount: 1,
    updatedBy: ADMIN_UID,
    updatedAt: pastDate(),
  });
  winBatch.update(seasonRef(adminDb), {
    captainUid: ALICE_UID,
    captainPlayerId: "GH-ALICE1",
    captainDisplayName: "AriaStar",
    captainTitle: "House Captain",
    activeTaskId: "task_cap_1",
    updatedAt: pastDate(),
  });
  winBatch.set(seasonRef(adminDb).collection("captaincyHistory").doc("cap_rec_1"), {
    seasonId: SEASON_ID,
    taskId: "task_cap_1",
    captainUid: ALICE_UID,
    captainPlayerId: "GH-ALICE1",
    captainDisplayName: "AriaStar",
    captainCharacterId: "Player 01",
    title: "House Captain",
    scoreAwarded: 100,
    assignedBy: ADMIN_UID,
    createdAt: pastDate(),
  });
  await assertSucceeds(winBatch.commit());

  // Now that task is COMPLETED, players can read the synchronized submissions/results & Captaincy history
  await assertSucceeds(
    seasonRef(bobDb).collection("tasks").doc("task_cap_1").collection("submissions")
      .where("seasonId", "==", SEASON_ID)
      .where("taskId", "==", "task_cap_1")
      .get()
  );
  await assertSucceeds(
    seasonRef(aliceDb).collection("captaincyHistory").where("seasonId", "==", SEASON_ID).get()
  );

  // 6. Admin publishes House Rules, issues RuleViolation warning to Bob, and publishes Daily Schedule
  await assertSucceeds(
    seasonRef(adminDb).collection("houseRules").doc("rule_1").set({
      seasonId: SEASON_ID,
      ruleNumber: 1,
      title: "Respect Housemates",
      description: "Maintain discipline in all tasks.",
      severity: "HIGH",
      active: true,
      updatedBy: ADMIN_UID,
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    seasonRef(adminDb).collection("ruleViolations").doc("viol_1").set({
      seasonId: SEASON_ID,
      ruleId: "rule_1",
      ruleTitle: "Respect Housemates",
      targetUid: BOB_UID,
      targetPlayerId: "GH-BOB002",
      targetDisplayName: "BobContender",
      warningLevel: "WARNING",
      note: "Please assemble in Living Atrium promptly.",
      issuedBy: ADMIN_UID,
      createdAt: pastDate(),
    })
  );
  await assertSucceeds(
    seasonRef(adminDb).collection("dailySchedule").doc("slot_4").set({
      seasonId: SEASON_ID,
      orderIndex: 4,
      activityKey: "DAILY_TASK",
      title: "Daily Task",
      timeLabel: "11:30 AM",
      location: "Living Atrium",
      description: "Main daily house competition.",
      isCurrent: true,
      updatedBy: ADMIN_UID,
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  // Players can read rules, violation history, and daily schedule, but cannot modify or issue warnings
  await assertSucceeds(seasonRef(bobDb).collection("houseRules").where("seasonId", "==", SEASON_ID).get());
  await assertSucceeds(seasonRef(bobDb).collection("ruleViolations").where("seasonId", "==", SEASON_ID).get());
  await assertSucceeds(seasonRef(aliceDb).collection("dailySchedule").where("seasonId", "==", SEASON_ID).get());
  await assertFails(
    seasonRef(bobDb).collection("ruleViolations").doc("viol_2").set({
      seasonId: SEASON_ID,
      ruleId: "rule_1",
      ruleTitle: "Fake Warning",
      targetUid: ALICE_UID,
      targetPlayerId: "GH-ALICE1",
      targetDisplayName: "AriaStar",
      warningLevel: "WARNING",
      note: "Attempted spoof",
      issuedBy: BOB_UID,
      createdAt: pastDate(),
    })
  );
});

test("Upgrades 2 & 3 RBAC: authCredentials/usernames deny-all, optional username, and Admin-only privacy-enforced cctvRecordings", async () => {
  const created = pastDate(15000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      role: "ADMIN",
      grantedBy: "firebase-admin-sdk",
      createdAt: created,
    });
    await db.collection("seasons").doc(SEASON_ID).set({
      title: "Season 01 • Glass House",
      status: "LIVE",
      maxPlayers: 15,
      activeCount: 1,
      dayNightMode: "day",
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("authCredentials").doc("aria_01").set({
      uid: ALICE_UID,
      passwordHash: "scrypt-salt-hash",
    });
    await db.collection("usernames").doc("aria_01").set({
      uid: ALICE_UID,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const seasonRef = (db) => db.collection("seasons").doc(SEASON_ID);

  // 1. /authCredentials and /usernames must remain strictly unreadable and unwritable from client SDKs (even Admin!)
  await assertFails(aliceDb.collection("authCredentials").doc("aria_01").get());
  await assertFails(adminDb.collection("authCredentials").doc("aria_01").get());
  await assertFails(aliceDb.collection("authCredentials").doc("attacker").set({ uid: ALICE_UID }));
  await assertFails(aliceDb.collection("usernames").doc("aria_01").get());
  await assertFails(adminDb.collection("usernames").doc("aria_01").get());
  await assertFails(aliceDb.collection("usernames").doc("attacker").set({ uid: ALICE_UID }));

  // 2. /users/{uid} optional username format enforcement
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "ab",
      displayName: "AriaStar",
      normalizedName: "ariastar",
      playerId: "GH-ALICE1",
      characterId: "Player 01",
      role: "PLAYER",
      seasonId: SEASON_ID,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "aria_01",
      displayName: "AriaStar",
      normalizedName: "ariastar",
      playerId: "GH-ALICE1",
      characterId: "Player 01",
      role: "PLAYER",
      seasonId: SEASON_ID,
      createdAt: created,
      updatedAt: created,
    })
  );

  // 3. /seasons/{seasonId}/cctvRecordings — Admin-only + privacy ban on bathroom/washroom/changing/private/smoking
  const validRec = {
    seasonId: SEASON_ID,
    cameraId: "cam_dining",
    cameraLabel: "CAM 03 • DINING AREA",
    storagePath: "seasons/season_01/cctvRecordings/rec_1_cam_dining.webm",
    downloadUrl: "https://firebasestorage.googleapis.com/v0/b/demo/o/rec_1.webm?alt=media",
    dateKey: "2026-11-18",
    startedAtMillis: Date.now() - 10000,
    durationMillis: 6000,
    sizeBytes: 245000,
    mimeType: "video/webm",
    recordedBy: ADMIN_UID,
    createdAt: pastDate(),
  };

  await assertFails(seasonRef(aliceDb).collection("cctvRecordings").doc("rec_dining_1").set({
    ...validRec,
    recordedBy: ALICE_UID,
  }));
  await assertSucceeds(seasonRef(adminDb).collection("cctvRecordings").doc("rec_dining_1").set(validRec));

  // Ordinary player cannot read or list CCTV recordings
  await assertFails(seasonRef(aliceDb).collection("cctvRecordings").doc("rec_dining_1").get());
  await assertFails(seasonRef(aliceDb).collection("cctvRecordings").where("seasonId", "==", SEASON_ID).get());

  // Privacy ban rejects bathroom or smoking camera IDs/labels even for Admin
  await assertFails(seasonRef(adminDb).collection("cctvRecordings").doc("rec_bath").set({
    ...validRec,
    cameraId: "cam_bathroom",
    cameraLabel: "Spa Bathroom",
  }));
  await assertFails(seasonRef(adminDb).collection("cctvRecordings").doc("rec_smoke").set({
    ...validRec,
    cameraId: "cam_smoking",
    cameraLabel: "Smoking Lounge",
  }));

  // Admin can query and delete archived recordings
  await assertSucceeds(seasonRef(adminDb).collection("cctvRecordings").where("seasonId", "==", SEASON_ID).get());
  await assertSucceeds(seasonRef(adminDb).collection("cctvRecordings").doc("rec_dining_1").delete());
});

test("Phase 7 & 8 RBAC: Kitchen Rations, Virtual Cooking, Task Progress, Anti-Duplicate Rewards & Leaderboard", async () => {
  const created = pastDate(20000);
  const nowMs = Date.now();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      role: "ADMIN",
      grantedBy: "firebase-admin-sdk",
      createdAt: created,
    });
    await db.collection("seasons").doc(SEASON_ID).set({
      title: "Season 01 • Glass House",
      status: "LIVE",
      maxPlayers: 15,
      activeCount: 2,
      dayNightMode: "day",
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "AriaStar",
      normalizedName: "ariastar",
      playerId: "GH-ALICE1",
      characterId: "Player 01",
      role: "PLAYER",
      seasonId: SEASON_ID,
      banned: false,
      evictedSeasons: 0,
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("seasons").doc(SEASON_ID).collection("members").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      userId: ALICE_UID,
      playerId: "GH-ALICE1",
      displayName: "AriaStar",
      characterId: "Player 01",
      slotNumber: 1,
      role: "PLAYER",
      status: "IN_HOUSE",
      online: true,
      sessionId: "sess_alice_001",
      joinedAt: created,
      lastSeenAt: created,
    });
    await db.collection("seasons").doc(SEASON_ID).collection("members").doc(BOB_UID).set({
      seasonId: SEASON_ID,
      userId: BOB_UID,
      playerId: "GH-BOB002",
      displayName: "BobContender",
      characterId: "Player 02",
      slotNumber: 2,
      role: "PLAYER",
      status: "IN_HOUSE",
      online: true,
      sessionId: "sess_bob_002",
      joinedAt: created,
      lastSeenAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const s = (db) => db.collection("seasons").doc(SEASON_ID);

  // 1. Kitchen Ration Pantry: seeded default item, player can decrement quantity, player CANNOT inflate or toggle permitted
  await assertSucceeds(
    s(aliceDb).collection("kitchenRations").doc("rice").set({
      seasonId: SEASON_ID,
      itemId: "rice",
      name: "Basmati Rice",
      category: "Grains",
      unit: "scoops",
      quantity: 20,
      maxQuantity: 40,
      minThreshold: 5,
      permitted: true,
      energyValue: 18,
      lastUpdatedBy: ALICE_UID,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );
  // Claim 2 scoops succeeds
  await assertSucceeds(
    s(aliceDb).collection("kitchenRations").doc("rice").update({
      quantity: 18,
      lastUpdatedBy: ALICE_UID,
      updatedAt: pastDate(3000),
    })
  );
  // Player cannot replenish stock or restrict items
  await assertFails(
    s(aliceDb).collection("kitchenRations").doc("rice").update({
      quantity: 40,
      lastUpdatedBy: ALICE_UID,
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    s(aliceDb).collection("kitchenRations").doc("rice").update({
      permitted: false,
      lastUpdatedBy: ALICE_UID,
      updatedAt: pastDate(),
    })
  );
  // Admin CAN replenish and restrict
  await assertSucceeds(
    s(adminDb).collection("kitchenRations").doc("rice").update({
      quantity: 35,
      permitted: true,
      lastUpdatedBy: ADMIN_UID,
      updatedAt: pastDate(2000),
    })
  );

  // 2. Kitchen Virtual Cooking Session & Shared Claiming (no duplicate claims)
  await assertSucceeds(
    s(aliceDb).collection("kitchenCooking").doc("cook_1").set({
      seasonId: SEASON_ID,
      stationId: "kitchen_stove_1",
      recipeId: "golden_atrium_platter",
      recipeName: "Golden Atrium Platter",
      chefUid: ALICE_UID,
      chefDisplayName: "AriaStar",
      status: "COOKING",
      ingredientsUsed: { rice: 2 },
      startedAtMillis: nowMs - 5000,
      readyAtMillis: nowMs + 1000,
      energyRestore: 30,
      servingsTotal: 2,
      servingsRemaining: 2,
      claimedByUids: [],
      updatedBy: ALICE_UID,
      createdAt: pastDate(4000),
      updatedAt: pastDate(4000),
    })
  );
  // Chef marks ready
  await assertSucceeds(
    s(aliceDb).collection("kitchenCooking").doc("cook_1").update({
      status: "READY",
      updatedBy: ALICE_UID,
      updatedAt: pastDate(2500),
    })
  );
  // Bob claims 1 serving
  await assertSucceeds(
    s(bobDb).collection("kitchenCooking").doc("cook_1").update({
      servingsRemaining: 1,
      claimedByUids: [BOB_UID],
      status: "READY",
      updatedBy: BOB_UID,
      updatedAt: pastDate(1500),
    })
  );
  // Bob cannot double-claim the same cooked meal
  await assertFails(
    s(bobDb).collection("kitchenCooking").doc("cook_1").update({
      servingsRemaining: 0,
      claimedByUids: [BOB_UID, BOB_UID],
      status: "CLAIMED",
      updatedBy: BOB_UID,
      updatedAt: pastDate(500),
    })
  );

  // 3. Phase 8 Task with Category, Difficulty, Checkpoints, Progress & One-Time Reward Grant + Leaderboard
  await assertSucceeds(
    s(adminDb).collection("tasks").doc("task_p8_1").set({
      seasonId: SEASON_ID,
      title: "Secret Chef Checkpoint Run",
      description: "Complete kitchen prep and 3 house checkpoints.",
      instructions: "Visit target rooms and cook a platter.",
      activityType: "COOKING",
      assignmentMode: "GROUP",
      category: "GROUP",
      difficulty: "HARD",
      isCaptaincy: false,
      status: "IN_PROGRESS",
      targetRoom: "Gourmet Kitchen",
      durationSeconds: 300,
      startTimeMillis: nowMs - 5000,
      endTimeMillis: nowMs + 295000,
      promptQuestion: "Complete all checkpoints & serve platter.",
      options: [],
      assignedUids: [ALICE_UID],
      requiredCheckpoints: ["cp_fridge", "cp_stove", "cp_dining"],
      requiredRecipeId: "golden_atrium_platter",
      progressPercent: 0,
      rewardAchievement: "MASTER_CHEF",
      maxPoints: 150,
      createdBy: ADMIN_UID,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );
  await assertSucceeds(
    s(aliceDb).collection("tasks").doc("task_p8_1").collection("progress").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      taskId: "task_p8_1",
      userId: ALICE_UID,
      displayName: "AriaStar",
      completedSteps: ["cp_fridge", "cp_stove", "cp_dining"],
      percent: 100,
      status: "COMPLETED",
      elapsedSeconds: 45,
      updatedAt: pastDate(1000),
    })
  );
  // Initialize leaderboard entry at 0 pts
  await assertSucceeds(
    s(aliceDb).collection("leaderboard").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      userId: ALICE_UID,
      playerId: "GH-ALICE1",
      displayName: "AriaStar",
      characterId: "Player 01",
      points: 0,
      housePointsContribution: 0,
      tasksCompleted: 0,
      mealsCooked: 0,
      achievements: [],
      lastGrantId: "init",
      updatedAt: pastDate(2000),
    })
  );
  // Unauthorized direct score inflation on leaderboard fails
  await assertFails(
    s(aliceDb).collection("leaderboard").doc(ALICE_UID).update({
      points: 500,
      housePointsContribution: 500,
      updatedAt: pastDate(),
    })
  );
  // Atomic batch: submission + deterministic TASK rewardGrant + leaderboard points increment succeeds ONCE
  const grantId = `TASK_task_p8_1_${ALICE_UID}`;
  const rewardBatch = aliceDb.batch();
  rewardBatch.set(s(aliceDb).collection("tasks").doc("task_p8_1").collection("submissions").doc(ALICE_UID), {
    seasonId: SEASON_ID,
    taskId: "task_p8_1",
    userId: ALICE_UID,
    playerId: "GH-ALICE1",
    displayName: "AriaStar",
    answerText: "ALL_CHECKPOINTS_DONE",
    elapsedSeconds: 45,
    verificationStatus: "SUBMITTED",
    awardedScore: 0,
    createdAt: pastDate(1000),
    updatedAt: pastDate(1000),
  });
  rewardBatch.set(s(aliceDb).collection("rewardGrants").doc(grantId), {
    seasonId: SEASON_ID,
    grantId: grantId,
    userId: ALICE_UID,
    sourceType: "TASK",
    sourceId: "task_p8_1",
    awardedPoints: 150,
    achievementId: "MASTER_CHEF",
    grantedBy: ALICE_UID,
    createdAt: pastDate(1000),
  });
  rewardBatch.update(s(aliceDb).collection("leaderboard").doc(ALICE_UID), {
    points: 150,
    housePointsContribution: 150,
    tasksCompleted: 1,
    achievements: ["MASTER_CHEF"],
    lastGrantId: grantId,
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(rewardBatch.commit());

  // Duplicate reward claim with same grantId fails!
  await assertFails(
    s(aliceDb).collection("rewardGrants").doc(grantId).set({
      seasonId: SEASON_ID,
      grantId: grantId,
      userId: ALICE_UID,
      sourceType: "TASK",
      sourceId: "task_p8_1",
      awardedPoints: 150,
      grantedBy: ALICE_UID,
      createdAt: pastDate(),
    })
  );
});

test("Phase 9: Secret Admin Identity & strictly Admin-only AI House Manager collections", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({ role: "ADMIN" });
    await db.collection("seasons").doc(SEASON_ID).set({
      seasonId: SEASON_ID,
      title: "Glass House Season 1",
      status: "LIVE",
      maxPlayers: 15,
      activeCount: 1,
      dayNightMode: "DAY",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const s = (db) => db.collection("seasons").doc(SEASON_ID);
  const nowMs = Date.now();

  // Normal player cannot write role == 'ADMIN' into season members roster
  await assertFails(
    s(aliceDb).collection("members").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      userId: ALICE_UID,
      playerId: "GH-ALICE1",
      displayName: "AriaStar",
      characterId: "Player 01",
      slotNumber: 1,
      role: "ADMIN",
      status: "IN_HOUSE",
      online: true,
      sessionId: "sess_1",
      joinedAt: pastDate(),
      lastSeenAt: pastDate(),
    })
  );

  // Admin presence lives ONLY in private adminPresence collection, denied to players
  await assertSucceeds(
    s(adminDb).collection("adminPresence").doc(ADMIN_UID).set({
      seasonId: SEASON_ID,
      userId: ADMIN_UID,
      online: true,
      lastSeenAt: pastDate(),
    })
  );
  await assertFails(s(aliceDb).collection("adminPresence").doc(ADMIN_UID).get());

  // AI Config, Schedules, Proposals, and Logs are strictly Admin-only
  const validAiConfig = {
    seasonId: SEASON_ID,
    enabled: true,
    mode: "ASSIST",
    emergencyStopped: false,
    apiProvider: "RULE_ENGINE_NO_API_KEY",
    autoHindiAnnouncements: true,
    autoKitchenMonitor: true,
    autoTaskCoordinator: true,
    autoTimerMonitor: true,
    tickIntervalMinutes: 1,
    lastTickAtMillis: nowMs,
    lastAdminSeenAtMillis: nowMs,
    unseenOfflineCount: 0,
    updatedBy: ADMIN_UID,
    createdAt: pastDate(4000),
    updatedAt: pastDate(4000),
  };
  await assertFails(s(aliceDb).collection("aiConfig").doc("default").set(validAiConfig));
  await assertSucceeds(s(adminDb).collection("aiConfig").doc("default").set(validAiConfig));
  await assertFails(s(aliceDb).collection("aiConfig").doc("default").get());
  await assertSucceeds(s(adminDb).collection("aiConfig").doc("default").get());

  const validSchedule = {
    seasonId: SEASON_ID,
    actionType: "HINDI_ANNOUNCEMENT",
    title: "Evening Check-in",
    messageOrPayload: "बिग बॉस घोषणा: शाम की चर्चा शुरू करें।",
    targetTimeMillis: nowMs + 60000,
    recurrenceMinutes: 0,
    status: "PENDING",
    modeOverride: "INHERIT",
    executionKey: "sched_evening_1",
    lastExecutedAtMillis: 0,
    createdBy: ADMIN_UID,
    createdAt: pastDate(3000),
    updatedAt: pastDate(3000),
  };
  await assertFails(s(aliceDb).collection("aiSchedules").doc("sched_1").set(validSchedule));
  await assertSucceeds(s(adminDb).collection("aiSchedules").doc("sched_1").set(validSchedule));
  await assertFails(s(aliceDb).collection("aiSchedules").doc("sched_1").get());

  const validProposal = {
    seasonId: SEASON_ID,
    category: "DAILY_TASK",
    title: "AI Task Idea: Atrium Memory",
    summary: "Suggest starting Atrium Memory task.",
    status: "PENDING",
    executionKey: "prop_exec_1",
    createdAt: pastDate(2000),
    updatedAt: pastDate(2000),
  };
  await assertFails(s(aliceDb).collection("aiProposals").doc("prop_1").set(validProposal));
  await assertSucceeds(s(adminDb).collection("aiProposals").doc("prop_1").set(validProposal));
  await assertFails(s(aliceDb).collection("aiProposals").doc("prop_1").get());

  const validLog = {
    seasonId: SEASON_ID,
    category: "HINDI_ANNOUNCEMENT",
    mode: "AUTO",
    source: "CLOUD_SCHEDULER",
    title: "Auto-Executed: Evening Check-in",
    detail: "Broadcasted Hindi announcement while owner offline.",
    executionKey: "log_exec_1",
    executedAtMillis: nowMs,
    actorUid: "cloud-scheduler",
    acknowledgedByAdmin: false,
    createdAt: pastDate(1000),
  };
  await assertFails(s(aliceDb).collection("aiLogs").doc("log_1").set(validLog));
  await assertSucceeds(s(adminDb).collection("aiLogs").doc("log_1").set(validLog));
  await assertFails(s(aliceDb).collection("aiLogs").doc("log_1").get());
});

test("Phase 10: Peer nominations, self/admin/duplicate guards, vote visibility rules, and round cancellation", async () => {
  const nowMs = Date.now();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({ role: "ADMIN" });
    await db.collection("seasons").doc(SEASON_ID).set({
      seasonId: SEASON_ID,
      title: "Glass House Season 1",
      status: "LIVE",
      maxPlayers: 15,
      activeCount: 2,
      dayNightMode: "DAY",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    });
    const seasonRef = db.collection("seasons").doc(SEASON_ID);
    await seasonRef.collection("members").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      userId: ALICE_UID,
      playerId: "GH-ALICE1",
      displayName: "AriaStar",
      characterId: "Player 01",
      slotNumber: 1,
      role: "PLAYER",
      status: "IN_HOUSE",
      online: true,
      sessionId: "sess_a",
      joinedAt: pastDate(),
      lastSeenAt: pastDate(),
    });
    await seasonRef.collection("members").doc(BOB_UID).set({
      seasonId: SEASON_ID,
      userId: BOB_UID,
      playerId: "GH-BOB002",
      displayName: "BobNova",
      characterId: "Player 02",
      slotNumber: 2,
      role: "PLAYER",
      status: "IN_HOUSE",
      online: true,
      sessionId: "sess_b",
      joinedAt: pastDate(),
      lastSeenAt: pastDate(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const s = (db) => db.collection("seasons").doc(SEASON_ID);

  // 1. Admin creates NOMINATIONS cycle
  await assertSucceeds(
    s(adminDb).collection("votingCycles").doc("cycle_p10").set({
      seasonId: SEASON_ID,
      title: "Week 1 Nominations",
      status: "NOMINATIONS",
      nominatedUids: [],
      nomineeSummaries: [],
      npcVotesEnabled: false,
      voteType: "EVICT",
      nominationsPerPlayer: 2,
      votingDurationMinutes: 30,
      votingEndTimeMillis: 0,
      resultVisibility: "HIDDEN_UNTIL_CLOSED",
      tieBreakerPolicy: "ADMIN_DECIDES",
      createdBy: ADMIN_UID,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );

  // Player cannot self-nominate
  await assertFails(
    s(aliceDb).collection("votingCycles").doc("cycle_p10").collection("nominations").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      cycleId: "cycle_p10",
      voterUid: ALICE_UID,
      voterName: "AriaStar",
      targetUids: [ALICE_UID],
      targetNames: ["AriaStar"],
      createdAt: pastDate(4000),
    })
  );

  // Player cannot nominate secret Admin UID
  await assertFails(
    s(aliceDb).collection("votingCycles").doc("cycle_p10").collection("nominations").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      cycleId: "cycle_p10",
      voterUid: ALICE_UID,
      voterName: "AriaStar",
      targetUids: [ADMIN_UID],
      targetNames: ["SecretOwner"],
      createdAt: pastDate(4000),
    })
  );

  // Alice successfully nominates Bob once
  await assertSucceeds(
    s(aliceDb).collection("votingCycles").doc("cycle_p10").collection("nominations").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      cycleId: "cycle_p10",
      voterUid: ALICE_UID,
      voterName: "AriaStar",
      targetUids: [BOB_UID],
      targetNames: ["BobNova"],
      reason: "Silent gameplay",
      createdAt: pastDate(3500),
    })
  );

  // Duplicate nomination fails
  await assertFails(
    s(aliceDb).collection("votingCycles").doc("cycle_p10").collection("nominations").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      cycleId: "cycle_p10",
      voterUid: ALICE_UID,
      voterName: "AriaStar",
      targetUids: [BOB_UID],
      targetNames: ["BobNova"],
      createdAt: pastDate(3000),
    })
  );

  // 2. Promote cycle to OPEN voting with HIDDEN_UNTIL_CLOSED visibility
  await assertSucceeds(
    s(adminDb).collection("votingCycles").doc("cycle_p10").update({
      status: "OPEN",
      nominatedUids: [ALICE_UID, BOB_UID],
      nomineeSummaries: [
        { userId: ALICE_UID, playerId: "GH-ALICE1", displayName: "AriaStar", characterId: "Player 01" },
        { userId: BOB_UID, playerId: "GH-BOB002", displayName: "BobNova", characterId: "Player 02" },
      ],
      votingEndTimeMillis: nowMs + 600000,
      updatedAt: pastDate(2500),
    })
  );

  // Alice & Bob cast single verified REAL_PLAYER votes
  await assertSucceeds(
    s(aliceDb).collection("votingCycles").doc("cycle_p10").collection("votes").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      cycleId: "cycle_p10",
      voterUid: ALICE_UID,
      voterName: "AriaStar",
      targetUid: BOB_UID,
      sourceType: "REAL_PLAYER",
      createdAt: pastDate(2000),
    })
  );
  await assertSucceeds(
    s(bobDb).collection("votingCycles").doc("cycle_p10").collection("votes").doc(BOB_UID).set({
      seasonId: SEASON_ID,
      cycleId: "cycle_p10",
      voterUid: BOB_UID,
      voterName: "BobNova",
      targetUid: ALICE_UID,
      sourceType: "REAL_PLAYER",
      createdAt: pastDate(2000),
    })
  );

  // Duplicate vote by Alice is blocked
  await assertFails(
    s(aliceDb).collection("votingCycles").doc("cycle_p10").collection("votes").doc(ALICE_UID).set({
      seasonId: SEASON_ID,
      cycleId: "cycle_p10",
      voterUid: ALICE_UID,
      voterName: "AriaStar",
      targetUid: BOB_UID,
      sourceType: "REAL_PLAYER",
      createdAt: pastDate(1500),
    })
  );

  // Result visibility HIDDEN_UNTIL_CLOSED hides Bob's ballot from Alice while OPEN
  await assertFails(
    s(aliceDb).collection("votingCycles").doc("cycle_p10").collection("votes").doc(BOB_UID).get()
  );
  // Admin CAN always inspect ballots
  await assertSucceeds(
    s(adminDb).collection("votingCycles").doc("cycle_p10").collection("votes").doc(BOB_UID).get()
  );

  // Once CLOSED, Alice CAN view Bob's ballot under HIDDEN_UNTIL_CLOSED
  await assertSucceeds(
    s(adminDb).collection("votingCycles").doc("cycle_p10").update({
      status: "CLOSED",
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    s(aliceDb).collection("votingCycles").doc("cycle_p10").collection("votes").doc(BOB_UID).get()
  );

  // Switching to ADMIN_ONLY re-locks other players' ballots even while CLOSED
  await assertSucceeds(
    s(adminDb).collection("votingCycles").doc("cycle_p10").update({
      resultVisibility: "ADMIN_ONLY",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    s(aliceDb).collection("votingCycles").doc("cycle_p10").collection("votes").doc(BOB_UID).get()
  );

  // Admin can cancel a tied/disputed cycle safely
  await assertSucceeds(
    s(adminDb).collection("votingCycles").doc("cycle_p10").update({
      status: "CANCELLED",
      cancelledReason: "Tie detected — round cancelled.",
      updatedAt: pastDate(200),
    })
  );
});

test("Phase 10 & 11 Security, Admin Session Isolation, Weighted Server Tally & VIP Privileges RBAC", async () => {
  const created = pastDate(20000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      role: "ADMIN",
      grantedBy: "firebase-admin-sdk",
      createdAt: created,
    });
    await db.collection("seasons").doc(SEASON_ID).set({
      title: "Season 01 • Glass House",
      status: "LIVE",
      maxPlayers: 15,
      activeCount: 2,
      dayNightMode: "day",
      captainUid: ALICE_UID,
      captainPlayerId: "GH-ALICE1",
      captainDisplayName: "AriaStar",
      captainTitle: "House Captain",
      captainVipAccess: true,
      captainImmunity: true,
      captainDoubleVote: true,
      captainTieBreaker: true,
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("adminCredentials").doc("BIGGBOSS").set({
      username: "BIGGBOSS",
      passwordHash: "scrypt-salt-hash",
      salt: "salt",
      createdAt: created,
    });
    await db.collection("adminSessions").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      username: "BIGGBOSS",
      sessionId: "sess_1",
      expiresAtMillis: Date.now() + 3600000,
    });
    await db.collection("adminRateLimits").doc("biggboss").set({
      failedAttempts: 0,
      lockedUntilMillis: 0,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const s = (db) => db.collection("seasons").doc(SEASON_ID);

  // 1. Server-only Admin authentication & rate-limit collections must deny ALL client access (even Admin!)
  await assertFails(aliceDb.collection("adminCredentials").doc("BIGGBOSS").get());
  await assertFails(adminDb.collection("adminCredentials").doc("BIGGBOSS").get());
  await assertFails(aliceDb.collection("adminCredentials").doc("BIGGBOSS").set({ username: "BIGGBOSS" }));
  await assertFails(aliceDb.collection("adminSessions").doc(ADMIN_UID).get());
  await assertFails(adminDb.collection("adminSessions").doc(ADMIN_UID).get());
  await assertFails(aliceDb.collection("adminRateLimits").doc("biggboss").get());
  await assertFails(adminDb.collection("adminRateLimits").doc("biggboss").set({ failedAttempts: 0 }));

  // 2. Non-admin cannot mutate captain power flags on season or set serverTallyJson
  await assertFails(
    s(aliceDb).update({
      captainDoubleVote: false,
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    s(aliceDb).collection("votingCycles").doc("cycle_weighted").set({
      seasonId: SEASON_ID,
      title: "Weighted Vote",
      status: "OPEN",
      nominatedUids: [BOB_UID],
      nomineeSummaries: [`${BOB_UID}|GH-BOB002|BobNova|Player 02`],
      npcVotesEnabled: false,
      serverTallyJson: "{\"tallies\":[]}",
      createdBy: ALICE_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Trusted Admin creates & records weighted server tally on votingCycle
  await assertSucceeds(
    s(adminDb).collection("votingCycles").doc("cycle_weighted").set({
      seasonId: SEASON_ID,
      title: "Weighted Vote",
      status: "CLOSED",
      nominatedUids: [ALICE_UID, BOB_UID],
      nomineeSummaries: [
        `${ALICE_UID}|GH-ALICE1|AriaStar|Player 01`,
        `${BOB_UID}|GH-BOB002|BobNova|Player 02`,
      ],
      npcVotesEnabled: false,
      serverTallyJson: "{\"tallies\":[{\"uid\":\"bob_uid_002\",\"displayName\":\"BobNova\",\"realVotes\":2,\"npcVotes\":0,\"totalVotes\":2}]}",
      serverTotalRealVotes: 2,
      serverTotalNpcVotes: 0,
      serverIsTie: false,
      serverTalliedAtMillis: Date.now(),
      createdBy: ADMIN_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // 3. Phase 11 VIP Privileges: Admin can grant/revoke, players can read, players cannot write
  await assertFails(
    s(aliceDb).collection("vipPrivileges").doc(BOB_UID).set({
      seasonId: SEASON_ID,
      userId: BOB_UID,
      playerId: "GH-BOB002",
      displayName: "BobNova",
      vipEnabled: true,
      vipRoomAccess: true,
      vipLabel: "VIP PASS",
      updatedBy: ALICE_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    s(adminDb).collection("vipPrivileges").doc(BOB_UID).set({
      seasonId: SEASON_ID,
      userId: BOB_UID,
      playerId: "GH-BOB002",
      displayName: "BobNova",
      vipEnabled: true,
      vipRoomAccess: true,
      vipActive: true,
      vipLabel: "VIP PASS",
      updatedBy: ADMIN_UID,
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    s(aliceDb).collection("vipPrivileges").where("seasonId", "==", SEASON_ID).get()
  );

  // 4. Phase 11 Privilege History & Game Settings: Admin write, Player read-only
  await assertFails(
    s(aliceDb).collection("privilegeHistory").doc("ph_fake").set({
      seasonId: SEASON_ID,
      targetUid: BOB_UID,
      targetDisplayName: "BobNova",
      actionType: "VIP_GRANTED",
      detail: "Fake privilege grant",
      issuedBy: ALICE_UID,
      createdAt: pastDate(),
    })
  );
  await assertSucceeds(
    s(adminDb).collection("privilegeHistory").doc("ph_1").set({
      seasonId: SEASON_ID,
      targetUid: BOB_UID,
      targetDisplayName: "BobNova",
      actionType: "VIP_GRANTED",
      detail: "Granted VIP PASS for BobNova",
      issuedBy: ADMIN_UID,
      createdAt: pastDate(),
    })
  );
  await assertSucceeds(s(aliceDb).collection("privilegeHistory").where("seasonId", "==", SEASON_ID).get());

  await assertFails(
    s(aliceDb).collection("gameSettings").doc("config").set({
      seasonId: SEASON_ID,
      seasonTitle: "Hijacked Title",
      updatedBy: ALICE_UID,
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    s(adminDb).collection("gameSettings").doc("config").set({
      seasonId: SEASON_ID,
      seasonTitle: "Season 01 • Glass House",
      defaultCaptainVipAccess: true,
      votingDurationMinutes: 15,
      allowNpcFillers: false,
      updatedBy: ADMIN_UID,
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(s(aliceDb).collection("gameSettings").doc("config").get());

  // 5. Phase 11 Admin Audit Logs: Strictly Admin-only read & write
  await assertFails(s(aliceDb).collection("adminAuditLogs").get());
  await assertFails(
    s(aliceDb).collection("adminAuditLogs").doc("audit_fake").set({
      seasonId: SEASON_ID,
      actionType: "ADMIN_LOGIN",
      summary: "Fake log",
      actorUid: ALICE_UID,
      createdAt: pastDate(),
    })
  );
  await assertSucceeds(
    s(adminDb).collection("adminAuditLogs").doc("audit_1").set({
      seasonId: SEASON_ID,
      actionType: "VIP_GRANTED",
      summary: "Granted VIP pass to BobNova",
      actorUid: ADMIN_UID,
      createdAt: pastDate(),
    })
  );
  await assertSucceeds(s(adminDb).collection("adminAuditLogs").get());

  // 6. Admin can remove reigning captain and clear powers safely
  await assertSucceeds(
    s(adminDb).update({
      captainUid: null,
      captainPlayerId: null,
      captainDisplayName: null,
      captainTitle: null,
      captainVipAccess: true,
      captainImmunity: false,
      captainDoubleVote: false,
      captainTieBreaker: false,
      updatedAt: pastDate(),
    })
  );
});
