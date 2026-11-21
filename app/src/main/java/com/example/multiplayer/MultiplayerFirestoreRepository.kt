package com.example.multiplayer

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class MultiplayerFirestoreRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
  }

  fun observeUserAccount(userId: String): Flow<UserAccount?> = flow {
    require(userId.isNotBlank()) { "userId must not be blank" }
    val path = "users/$userId"
    emitAll(
      db.collection("users").document(userId)
        .snapshots()
        .map { snapshot ->
          if (!snapshot.exists()) null
          else snapshot.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  fun observeAdminGrant(userId: String): Flow<Boolean> = flow {
    require(userId.isNotBlank()) { "userId must not be blank" }
    val path = "admins/$userId"
    emitAll(
      db.collection("admins").document(userId)
        .snapshots()
        .map { snapshot ->
          snapshot.exists() && snapshot.getString("role") == PlayerRole.ADMIN.name
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          emit(false)
        }
    )
  }

  fun observeActiveSeason(seasonId: String = DEFAULT_SEASON_ID): Flow<SeasonSession?> = flow {
    val uid = requireUserId()
    val path = "seasons/$seasonId"
    emitAll(
      db.collection("seasons").document(seasonId)
        .snapshots()
        .map { snapshot ->
          if (!snapshot.exists()) null
          else snapshot.toObject(SeasonSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  fun observeSeasonMembers(seasonId: String = DEFAULT_SEASON_ID): Flow<List<SeasonMember>> = flow {
    val uid = requireUserId()
    val path = "seasons/$seasonId/members"
    emitAll(
      db.collection("seasons").document(seasonId).collection("members")
        .whereEqualTo("seasonId", seasonId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(SeasonMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .filter { it.role != PlayerRole.ADMIN.name }
            .sortedBy { it.slotNumber }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeAllRegisteredUsers(): Flow<List<UserAccount>> = flow {
    val uid = requireUserId()
    val path = "users"
    emitAll(
      db.collection("users")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.displayName.lowercase() }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeVotingCycles(seasonId: String = DEFAULT_SEASON_ID): Flow<List<VotingCycle>> = flow {
    val uid = requireUserId()
    val path = "seasons/$seasonId/votingCycles"
    emitAll(
      db.collection("seasons").document(seasonId).collection("votingCycles")
        .whereEqualTo("seasonId", seasonId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(VotingCycle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt?.toDate()?.time ?: 0L }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeVotesForCycle(
    seasonId: String = DEFAULT_SEASON_ID,
    cycleId: String,
    canReadAllVotes: Boolean,
  ): Flow<List<VotingBallot>> = flow {
    val uid = requireUserId()
    val path = "seasons/$seasonId/votingCycles/$cycleId/votes"
    val votesCol = db.collection("seasons").document(seasonId)
      .collection("votingCycles").document(cycleId)
      .collection("votes")

    if (canReadAllVotes) {
      emitAll(
        votesCol
          .whereEqualTo("seasonId", seasonId)
          .whereEqualTo("cycleId", cycleId)
          .snapshots()
          .map { snapshot ->
            snapshot.toObjects(VotingBallot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          }
          .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && auth.currentUser?.uid == uid) {
              handleFirestoreError(exceptionToLog, OperationType.LIST, path)
            }
            emit(emptyList())
          }
      )
    } else {
      // Ordinary players can only read their own ballot while voting is OPEN (zero vote leakage).
      emitAll(
        votesCol.document(uid)
          .snapshots()
          .map { snap ->
            if (!snap.exists()) emptyList()
            else listOfNotNull(snap.toObject(VotingBallot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
          }
          .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
          }
      )
    }
  }

  fun observeEvictions(seasonId: String = DEFAULT_SEASON_ID): Flow<List<EvictionRecord>> = flow {
    val uid = requireUserId()
    val path = "seasons/$seasonId/evictions"
    emitAll(
      db.collection("seasons").document(seasonId).collection("evictions")
        .whereEqualTo("seasonId", seasonId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(EvictionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          emit(emptyList())
        }
    )
  }

  fun observeAnnouncements(seasonId: String = DEFAULT_SEASON_ID): Flow<List<AdminAnnouncement>> = flow {
    val uid = requireUserId()
    val path = "seasons/$seasonId/announcements"
    emitAll(
      db.collection("seasons").document(seasonId).collection("announcements")
        .whereEqualTo("seasonId", seasonId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(AdminAnnouncement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          emit(emptyList())
        }
    )
  }

  suspend fun ensureDefaultSeasonExists(seasonId: String = DEFAULT_SEASON_ID): Result<Unit> {
    val path = "seasons/$seasonId"
    return try {
      requireUserId()
      val seasonRef = db.collection("seasons").document(seasonId)
      val snap = seasonRef.get().await()
      if (!snap.exists()) {
        val payload = mapOf(
          "title" to "Season 01 • Bigg Boss – The House",
          "status" to "LOBBY",
          "maxPlayers" to MAX_PLAYERS,
          "activeCount" to 0L,
          "dayNightMode" to "day",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        seasonRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun registerOrUpdateProfile(
    rawDisplayName: String,
    characterId: String,
    seasonId: String = DEFAULT_SEASON_ID,
  ): Result<UserAccount> {
    var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
    return try {
      val uid = requireUserId()
      path = "users/$uid"
      val trimmed = rawDisplayName.trim()
      require(trimmed.length in 3..24) { "Display name must be between 3 and 24 characters." }
      require(trimmed.count { it.isLetterOrDigit() } >= 3) {
        "Display name must include at least 3 letters or digits."
      }

      val normalized = trimmed.lowercase().replace(Regex("[^a-z0-9_-]"), "_")
      require(normalized.length in 3..24) { "Display name must include at least 3 letters or digits." }
      require(HousemateCatalog.all.any { it.id == characterId }) { "Invalid character selection." }

      ensureDefaultSeasonExists(seasonId)

      val userRef = db.collection("users").document(uid)
      val newNameRef = db.collection("displayNames").document(normalized)
      val email = auth.currentUser?.email?.take(160)

      val savedAccount = db.runTransaction { tx ->
        val userSnap = tx.get(userRef)
        val nameSnap = tx.get(newNameRef)

        if (userSnap.exists() && userSnap.getBoolean("banned") == true) {
          throw IllegalStateException("This account has been banned by an administrator.")
        }

        if (nameSnap.exists() && nameSnap.getString("userId") != uid) {
          throw IllegalStateException("Display name \"$trimmed\" is already taken. Choose another unique name.")
        }

        val oldNormalized = userSnap.getString("normalizedName")
        if (!oldNormalized.isNullOrBlank() && oldNormalized != normalized) {
          tx.delete(db.collection("displayNames").document(oldNormalized))
        }

        val playerId = userSnap.getString("playerId")
          ?: "GH-${uid.take(6).uppercase()}"
        val role = userSnap.getString("role") ?: PlayerRole.PLAYER.name

        tx.set(
          newNameRef,
          mapOf(
            "userId" to uid,
            "displayName" to trimmed,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )

        if (userSnap.exists()) {
          tx.update(
            userRef,
            mapOf(
              "displayName" to trimmed,
              "normalizedName" to normalized,
              "characterId" to characterId,
              "seasonId" to seasonId,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        } else {
          val createPayload = mapOf(
            "userId" to uid,
            "email" to email,
            "displayName" to trimmed,
            "normalizedName" to normalized,
            "playerId" to playerId,
            "characterId" to characterId,
            "role" to PlayerRole.PLAYER.name,
            "seasonId" to seasonId,
            "banned" to false,
            "evictedSeasons" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
          tx.set(userRef, createPayload)
        }

        UserAccount(
          id = uid,
          userId = uid,
          email = email,
          displayName = trimmed,
          normalizedName = normalized,
          playerId = playerId,
          characterId = characterId,
          role = role,
          seasonId = seasonId,
        )
      }.await()

      Result.success(savedAccount)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun joinSeasonHouse(
    account: UserAccount,
    isAdminVerified: Boolean,
    characterId: String,
    sessionId: String,
    seasonId: String = DEFAULT_SEASON_ID,
  ): Result<SeasonMember> {
    val path = "seasons/$seasonId/members/${auth.currentUser?.uid ?: "unauthenticated"}"
    return try {
      val uid = requireUserId()
      require(account.userId == uid) { "Account mismatch." }
      require(!account.banned) { account.banReason ?: "This account has been banned by Admin." }
      require(sessionId.length in 6..64) { "Invalid session token." }

      ensureDefaultSeasonExists(seasonId)

      val existingMembersSnap = db.collection("seasons").document(seasonId)
        .collection("members")
        .whereEqualTo("seasonId", seasonId)
        .get()
        .await()

      val usedSlots = existingMembersSnap.documents
        .filter {
          it.id != uid &&
            it.getString("status") !in listOf("EVICTED", "BANNED", "REMOVED")
        }
        .mapNotNull { (it.getLong("slotNumber") ?: 0L).toInt().takeIf { s -> s in 1..15 } }
        .toSet()

      val seasonRef = db.collection("seasons").document(seasonId)
      val memberRef = seasonRef.collection("members").document(uid)
      val adminPresenceRef = seasonRef.collection("adminPresence").document(uid)
      val userRef = db.collection("users").document(uid)

      val joined = db.runTransaction { tx ->
        val seasonSnap = tx.get(seasonRef)
        val memberSnap = tx.get(memberRef)
        val userSnap = tx.get(userRef)

        if (userSnap.getBoolean("banned") == true) {
          throw IllegalStateException("This account is banned from Bigg Boss – The House.")
        }

        val currentCount = seasonSnap.getLong("activeCount") ?: 0L
        val status = seasonSnap.getString("status") ?: "LOBBY"

        if (status == "ENDED") {
          throw IllegalStateException("This season has ended.")
        }

        // Secret Admin Identity: NEVER place the Admin account in public /members or activeCount
        if (isAdminVerified) {
          if (memberSnap.exists()) {
            val prevStatus = memberSnap.getString("status")
            tx.delete(memberRef)
            if (prevStatus !in listOf("EVICTED", "BANNED", "REMOVED")) {
              tx.update(
                seasonRef,
                mapOf(
                  "activeCount" to max(0L, currentCount - 1L),
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
              )
            }
          }
          tx.set(
            adminPresenceRef,
            mapOf(
              "seasonId" to seasonId,
              "userId" to uid,
              "characterId" to characterId,
              "status" to "CONTROL_ROOM",
              "online" to true,
              "sessionId" to sessionId,
              "joinedAt" to FieldValue.serverTimestamp(),
              "lastSeenAt" to FieldValue.serverTimestamp(),
            ),
          )
          return@runTransaction SeasonMember(
            id = uid,
            seasonId = seasonId,
            userId = uid,
            playerId = account.playerId,
            displayName = account.displayName,
            characterId = characterId,
            slotNumber = 0L,
            role = PlayerRole.ADMIN.name,
            status = "CONTROL_ROOM",
            online = true,
            sessionId = sessionId,
          )
        }

        val existingMemberStatus = memberSnap.getString("status")
        if (existingMemberStatus == "EVICTED" || existingMemberStatus == "BANNED") {
          throw IllegalStateException("Evicted or banned housemates cannot rejoin this season.")
        }

        if (!memberSnap.exists() && currentCount >= MAX_PLAYERS) {
          throw IllegalStateException("Season is at full capacity (15/15 real players).")
        }

        val currentSlot = memberSnap.getLong("slotNumber")?.toInt()
        val chosenSlot = if (currentSlot != null && currentSlot in 1..15 && !usedSlots.contains(currentSlot)) {
          currentSlot
        } else {
          (1..15).firstOrNull { !usedSlots.contains(it) }
            ?: throw IllegalStateException("All 15 housemate slots are currently occupied.")
        }

        val resolvedRole = PlayerRole.PLAYER.name
        val nextCount = if (memberSnap.exists()) currentCount else (currentCount + 1L)

        tx.update(
          seasonRef,
          mapOf(
            "activeCount" to nextCount,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        if (memberSnap.exists()) {
          tx.update(
            memberRef,
            mapOf(
              "displayName" to account.displayName,
              "characterId" to characterId,
              "status" to "IN_HOUSE",
              "online" to true,
              "sessionId" to sessionId,
              "lastSeenAt" to FieldValue.serverTimestamp(),
            ),
          )
        } else {
          tx.set(
            memberRef,
            mapOf(
              "seasonId" to seasonId,
              "userId" to uid,
              "playerId" to account.playerId,
              "displayName" to account.displayName,
              "characterId" to characterId,
              "slotNumber" to chosenSlot.toLong(),
              "role" to resolvedRole,
              "status" to "IN_HOUSE",
              "online" to true,
              "sessionId" to sessionId,
              "joinedAt" to FieldValue.serverTimestamp(),
              "lastSeenAt" to FieldValue.serverTimestamp(),
            ),
          )
        }

        SeasonMember(
          id = uid,
          seasonId = seasonId,
          userId = uid,
          playerId = account.playerId,
          displayName = account.displayName,
          characterId = characterId,
          slotNumber = chosenSlot.toLong(),
          role = resolvedRole,
          status = "IN_HOUSE",
          online = true,
          sessionId = sessionId,
        )
      }.await()

      Result.success(joined)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun updateMemberStatus(
    seasonId: String = DEFAULT_SEASON_ID,
    status: String,
    online: Boolean,
    characterId: String? = null,
    sessionId: String? = null,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.success(Unit)
    val path = "seasons/$seasonId/members/$uid"
    return try {
      val memberRef = db.collection("seasons").document(seasonId).collection("members").document(uid)
      val snap = memberRef.get().await()
      if (!snap.exists()) {
        // Update private adminPresence silently if the current user is an Admin in Control Room
        val adminSnap = db.collection("admins").document(uid).get().await()
        if (adminSnap.exists()) {
          val adminUpdates = mutableMapOf<String, Any>(
            "online" to online,
            "lastSeenAt" to FieldValue.serverTimestamp(),
          )
          if (!characterId.isNullOrBlank()) adminUpdates["characterId"] = characterId
          if (!sessionId.isNullOrBlank()) adminUpdates["sessionId"] = sessionId
          db.collection("seasons").document(seasonId).collection("adminPresence").document(uid)
            .set(adminUpdates, SetOptions.merge()).await()
        }
        return Result.success(Unit)
      }
      val curStatus = snap.getString("status")
      if (curStatus == "EVICTED" || curStatus == "BANNED") return Result.success(Unit)

      val updates = mutableMapOf<String, Any>(
        "status" to status,
        "online" to online,
        "lastSeenAt" to FieldValue.serverTimestamp(),
      )
      if (!characterId.isNullOrBlank()) updates["characterId"] = characterId
      if (!sessionId.isNullOrBlank()) updates["sessionId"] = sessionId

      memberRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun releaseSeasonSlot(seasonId: String = DEFAULT_SEASON_ID): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.success(Unit)
    val path = "seasons/$seasonId/members/$uid"
    return try {
      val seasonRef = db.collection("seasons").document(seasonId)
      val memberRef = seasonRef.collection("members").document(uid)
      val adminPresenceRef = seasonRef.collection("adminPresence").document(uid)
      db.runTransaction { tx ->
        val seasonSnap = tx.get(seasonRef)
        val memberSnap = tx.get(memberRef)
        val adminPresenceSnap = tx.get(adminPresenceRef)
        if (adminPresenceSnap.exists()) {
          tx.delete(adminPresenceRef)
        }
        if (memberSnap.exists()) {
          val curStatus = memberSnap.getString("status")
          if (curStatus == "EVICTED" || curStatus == "BANNED") {
            return@runTransaction
          }
          val currentCount = seasonSnap.getLong("activeCount") ?: 0L
          tx.delete(memberRef)
          if (seasonSnap.exists()) {
            tx.update(
              seasonRef,
              mapOf(
                "activeCount" to max(0L, currentCount - 1L),
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            )
          }
        }
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun setSeasonStatus(
    seasonId: String = DEFAULT_SEASON_ID,
    nextStatus: String,
  ): Result<Unit> {
    val path = "seasons/$seasonId"
    return try {
      val adminUid = requireUserId()
      require(nextStatus in listOf("LOBBY", "LIVE", "ENDED")) { "Invalid season status." }
      val seasonRef = db.collection("seasons").document(seasonId)
      seasonRef.update(
        mapOf(
          "status" to nextStatus,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
      seasonRef.collection("adminControls").document().set(
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "SEASON_STATE",
          "payloadSummary" to "Season state set to $nextStatus",
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun moderateHousemate(
    seasonId: String = DEFAULT_SEASON_ID,
    targetUid: String,
    action: String, // "KICK" | "BAN" | "UNBAN"
    reason: String? = null,
  ): Result<Unit> {
    val path = "seasons/$seasonId/members/$targetUid"
    return try {
      val adminUid = requireUserId()
      require(action in listOf("KICK", "BAN", "UNBAN")) { "Invalid moderation action." }
      require(targetUid != adminUid || action == "UNBAN") { "Admin cannot kick or ban their own account." }

      val seasonRef = db.collection("seasons").document(seasonId)
      val memberRef = seasonRef.collection("members").document(targetUid)
      val userRef = db.collection("users").document(targetUid)
      val cleanReason = reason?.trim()?.take(200)

      db.runTransaction { tx ->
        val seasonSnap = tx.get(seasonRef)
        val memberSnap = tx.get(memberRef)
        val userSnap = tx.get(userRef)

        if (action == "BAN" || action == "UNBAN") {
          if (userSnap.exists()) {
            tx.update(
              userRef,
              mapOf(
                "banned" to (action == "BAN"),
                "banReason" to if (action == "BAN") (cleanReason ?: "Banned by Admin Control Room") else null,
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            )
          }
        }

        if (action == "KICK" || action == "BAN") {
          if (memberSnap.exists()) {
            val prevStatus = memberSnap.getString("status")
            if (action == "KICK" && prevStatus != "EVICTED" && prevStatus != "BANNED") {
              tx.delete(memberRef)
            } else {
              tx.update(
                memberRef,
                mapOf(
                  "status" to "BANNED",
                  "online" to false,
                  "lastSeenAt" to FieldValue.serverTimestamp(),
                ),
              )
            }
            if (seasonSnap.exists() && prevStatus !in listOf("EVICTED", "BANNED", "REMOVED")) {
              val curCount = seasonSnap.getLong("activeCount") ?: 0L
              tx.update(
                seasonRef,
                mapOf(
                  "activeCount" to max(0L, curCount - 1L),
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
              )
            }
          }
        }
      }.await()

      seasonRef.collection("adminControls").document().set(
        mapOf(
          "seasonId" to seasonId,
          "commandType" to if (action == "KICK") "KICK_SESSION" else "BAN_USER",
          "payloadSummary" to "$action $targetUid${if (!cleanReason.isNullOrBlank()) " ($cleanReason)" else ""}".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      ).await()

      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun publishOrUpdateVotingCycle(
    seasonId: String = DEFAULT_SEASON_ID,
    cycleId: String? = null,
    title: String,
    status: String,
    nominees: List<NomineeSummary>,
    npcVotesEnabled: Boolean,
    voteType: String = "EVICT",
    nominationsPerPlayer: Long = 1L,
    nominationEndTimeMillis: Long = 0L,
    votingDurationMinutes: Long = 15L,
    votingEndTimeMillis: Long = 0L,
    resultVisibility: String = "HIDDEN_UNTIL_CLOSED",
    tieBreakerPolicy: String = "ADMIN_DECIDES",
    cancelledReason: String? = null,
  ): Result<String> {
    val cleanTitle = title.trim().ifBlank { "Weekly Eviction Nomination" }.take(80)
    val resolvedId = cycleId?.takeIf { it.isNotBlank() } ?: "cycle_${System.currentTimeMillis()}"
    val path = "seasons/$seasonId/votingCycles/$resolvedId"
    return try {
      val adminUid = requireUserId()
      require(status in listOf("NOMINATIONS", "OPEN", "CLOSED", "EVICTED", "CANCELLED")) {
        "Invalid voting cycle status."
      }
      if (status !in listOf("NOMINATIONS", "CANCELLED")) {
        require(nominees.isNotEmpty() && nominees.size <= 15) { "Select 1 to 15 nominees." }
      }

      val cleanVoteType = if (voteType == "SAVE") "SAVE" else "EVICT"
      val cleanNomsPerPlayer = nominationsPerPlayer.coerceIn(1L, 3L)
      val cleanDurationMins = votingDurationMinutes.coerceIn(1L, 1440L)
      val cleanVisibility = if (resultVisibility in listOf("HIDDEN_UNTIL_CLOSED", "LIVE_TOTALS", "ADMIN_ONLY")) {
        resultVisibility
      } else {
        "HIDDEN_UNTIL_CLOSED"
      }
      val cleanTiePolicy = if (tieBreakerPolicy in listOf("ADMIN_DECIDES", "CAPTAIN_DECIDES", "RANDOM_DRAW", "NO_EVICTION")) {
        tieBreakerPolicy
      } else {
        "ADMIN_DECIDES"
      }

      val cycleRef = db.collection("seasons").document(seasonId)
        .collection("votingCycles").document(resolvedId)
      val snap = cycleRef.get().await()

      val nominatedUids = nominees.map { it.uid }
      val nomineeSummaries = nominees.map { it.encode() }

      if (snap.exists()) {
        cycleRef.update(
          mapOf(
            "title" to cleanTitle,
            "status" to status,
            "nominatedUids" to nominatedUids,
            "nomineeSummaries" to nomineeSummaries,
            "npcVotesEnabled" to npcVotesEnabled,
            "voteType" to cleanVoteType,
            "nominationsPerPlayer" to cleanNomsPerPlayer,
            "nominationEndTimeMillis" to max(0L, nominationEndTimeMillis),
            "votingDurationMinutes" to cleanDurationMins,
            "votingEndTimeMillis" to max(0L, votingEndTimeMillis),
            "resultVisibility" to cleanVisibility,
            "tieBreakerPolicy" to cleanTiePolicy,
            "cancelledReason" to cancelledReason?.trim()?.take(200),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      } else {
        cycleRef.set(
          mapOf(
            "seasonId" to seasonId,
            "title" to cleanTitle,
            "status" to status,
            "nominatedUids" to nominatedUids,
            "nomineeSummaries" to nomineeSummaries,
            "npcVotesEnabled" to npcVotesEnabled,
            "evictedUid" to null,
            "evictedDisplayName" to null,
            "voteType" to cleanVoteType,
            "nominationsPerPlayer" to cleanNomsPerPlayer,
            "nominationEndTimeMillis" to max(0L, nominationEndTimeMillis),
            "votingDurationMinutes" to cleanDurationMins,
            "votingEndTimeMillis" to max(0L, votingEndTimeMillis),
            "resultVisibility" to cleanVisibility,
            "tieBreakerPolicy" to cleanTiePolicy,
            "cancelledReason" to cancelledReason?.trim()?.take(200),
            "createdBy" to adminUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      }

      db.collection("seasons").document(seasonId).collection("adminControls").document().set(
        mapOf(
          "seasonId" to seasonId,
          "commandType" to if (status == "CANCELLED") "VOTING_CANCEL" else "VOTING_STATE",
          "payloadSummary" to "$cleanTitle -> $status ($cleanVoteType, ${nominees.size} nominees)".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      ).await()

      Result.success(resolvedId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun castRealVote(
    seasonId: String = DEFAULT_SEASON_ID,
    cycleId: String,
    targetUid: String,
    voterDisplayName: String,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid
    val path = "seasons/$seasonId/votingCycles/$cycleId/votes/${uid ?: "unauth"}"
    return try {
      val voterUid = requireUserId()
      val ballotRef = db.collection("seasons").document(seasonId)
        .collection("votingCycles").document(cycleId)
        .collection("votes").document(voterUid)

      val existing = ballotRef.get().await()
      if (existing.exists()) {
        throw IllegalStateException("You have already cast your ballot for this cycle.")
      }

      ballotRef.set(
        mapOf(
          "seasonId" to seasonId,
          "cycleId" to cycleId,
          "voterUid" to voterUid,
          "voterName" to voterDisplayName.take(40),
          "targetUid" to targetUid,
          "sourceType" to "REAL_PLAYER",
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun castNpcVote(
    seasonId: String = DEFAULT_SEASON_ID,
    cycleId: String,
    npcLabel: String,
    targetUid: String,
  ): Result<Unit> {
    val ballotId = "npc_${System.currentTimeMillis()}_${(100..999).random()}"
    val path = "seasons/$seasonId/votingCycles/$cycleId/votes/$ballotId"
    return try {
      requireUserId()
      db.collection("seasons").document(seasonId)
        .collection("votingCycles").document(cycleId)
        .collection("votes").document(ballotId)
        .set(
          mapOf(
            "seasonId" to seasonId,
            "cycleId" to cycleId,
            "voterUid" to ballotId,
            "voterName" to npcLabel.take(40),
            "targetUid" to targetUid,
            "sourceType" to "NPC",
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun confirmEviction(
    seasonId: String = DEFAULT_SEASON_ID,
    cycleId: String,
    nominee: NomineeSummary,
    realVotes: Long,
    npcVotes: Long,
    announcementText: String,
  ): Result<Unit> {
    val evictionId = "${cycleId}_${nominee.uid}"
    val path = "seasons/$seasonId/evictions/$evictionId"
    return try {
      val adminUid = requireUserId()
      val cleanAnn = announcementText.trim().ifBlank {
        "${nominee.displayName} has been evicted from Bigg Boss – The House."
      }.take(300)

      val seasonRef = db.collection("seasons").document(seasonId)
      val cycleRef = seasonRef.collection("votingCycles").document(cycleId)
      val evictionRef = seasonRef.collection("evictions").document(evictionId)
      val memberRef = seasonRef.collection("members").document(nominee.uid)
      val userRef = db.collection("users").document(nominee.uid)

      db.runTransaction { tx ->
        val seasonSnap = tx.get(seasonRef)
        val memberSnap = tx.get(memberRef)
        val userSnap = tx.get(userRef)

        tx.set(
          evictionRef,
          mapOf(
            "seasonId" to seasonId,
            "cycleId" to cycleId,
            "userId" to nominee.uid,
            "playerId" to nominee.playerId,
            "displayName" to nominee.displayName,
            "characterId" to nominee.characterId,
            "realVotes" to max(0L, realVotes),
            "npcVotes" to max(0L, npcVotes),
            "announcementText" to cleanAnn,
            "evictedBy" to adminUid,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )

        tx.update(
          cycleRef,
          mapOf(
            "status" to "EVICTED",
            "evictedUid" to nominee.uid,
            "evictedDisplayName" to nominee.displayName,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        if (userSnap.exists()) {
          tx.update(
            userRef,
            mapOf(
              "evictedSeasons" to FieldValue.increment(1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }

        if (memberSnap.exists()) {
          val prevStatus = memberSnap.getString("status")
          tx.update(
            memberRef,
            mapOf(
              "status" to "EVICTED",
              "online" to false,
              "lastSeenAt" to FieldValue.serverTimestamp(),
            ),
          )
          if (seasonSnap.exists() && prevStatus !in listOf("EVICTED", "BANNED", "REMOVED")) {
            val curCount = seasonSnap.getLong("activeCount") ?: 0L
            tx.update(
              seasonRef,
              mapOf(
                "activeCount" to max(0L, curCount - 1L),
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            )
          }
        }
      }.await()

      seasonRef.collection("adminControls").document().set(
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "EVICT_PLAYER",
          "payloadSummary" to "Evicted ${nominee.displayName} (Real: $realVotes, NPC: $npcVotes)".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      ).await()

      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun publishAnnouncement(
    seasonId: String = DEFAULT_SEASON_ID,
    message: String,
    presetId: String? = null,
    languageTag: String = "hi-IN",
    voiceGender: VoiceGender = VoiceGender.FEMALE,
  ): Result<String> {
    val path = "seasons/$seasonId/announcements"
    return try {
      val adminUid = requireUserId()
      val cleanMsg = message.trim().take(320)
      require(cleanMsg.length >= 2) { "Announcement message cannot be empty." }
      val lang = if (languageTag in listOf("hi-IN", "en-IN")) languageTag else "hi-IN"

      val annRef = db.collection("seasons").document(seasonId)
        .collection("announcements").document()

      val payload = mutableMapOf<String, Any>(
        "seasonId" to seasonId,
        "message" to cleanMsg,
        "languageTag" to lang,
        "voiceGender" to voiceGender.name,
        "issuedBy" to adminUid,
        "createdAt" to FieldValue.serverTimestamp(),
      )
      if (!presetId.isNullOrBlank()) {
        payload["presetId"] = presetId.take(64)
      }

      annRef.set(payload).await()

      db.collection("seasons").document(seasonId).collection("adminControls").document().set(
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "BROADCAST",
          "payloadSummary" to "[$lang/${voiceGender.name}] $cleanMsg".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      ).await()

      Result.success(annRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun recordAdminControlAction(
    seasonId: String = DEFAULT_SEASON_ID,
    commandType: String,
    payloadSummary: String,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid
    val path = "seasons/$seasonId/adminControls"
    return try {
      val activeUid = requireUserId()
      val cleanSummary = payloadSummary.trim().take(300).ifBlank { commandType }
      db.collection("seasons").document(seasonId)
        .collection("adminControls")
        .document()
        .set(
          mapOf(
            "seasonId" to seasonId,
            "commandType" to commandType,
            "payloadSummary" to cleanSummary,
            "issuedBy" to activeUid,
            "createdAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (uid != null) {
        handleFirestoreError(e, OperationType.CREATE, path)
      }
      Result.failure(e)
    }
  }

  fun observeCctvRecordings(seasonId: String = DEFAULT_SEASON_ID): Flow<List<CctvRecordingRecord>> = flow {
    val uid = requireUserId()
    val path = "seasons/$seasonId/cctvRecordings"
    emitAll(
      db.collection("seasons").document(seasonId).collection("cctvRecordings")
        .whereEqualTo("seasonId", seasonId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(CctvRecordingRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.startedAtMillis }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          emit(emptyList())
        }
    )
  }

  suspend fun uploadCctvRecording(
    seasonId: String = DEFAULT_SEASON_ID,
    cameraId: String,
    cameraLabel: String,
    startedAtMillis: Long,
    durationMillis: Long,
    mimeType: String,
    videoBytes: ByteArray,
  ): Result<CctvRecordingRecord> {
    val path = "seasons/$seasonId/cctvRecordings"
    return try {
      val adminUid = requireUserId()
      val cleanCamId = cameraId.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "_").take(40)
      require(cleanCamId.length >= 3) { "Invalid camera identifier." }
      require(FORBIDDEN_CCTV_KEYWORDS.none { cleanCamId.contains(it) || cameraLabel.lowercase().contains(it) }) {
        "Privacy Policy Violation: Bathrooms, washrooms, and private smoking areas cannot be recorded."
      }
      require(videoBytes.isNotEmpty() && videoBytes.size <= 15 * 1024 * 1024) {
        "Recorded clip size must be between 1 byte and 15 MB."
      }

      // Refresh custom claims token before Storage upload so rules see latest Admin status
      auth.currentUser?.getIdToken(false)?.await()

      val recId = "rec_${startedAtMillis}_${cleanCamId}"
      val ext = if (mimeType.contains("mp4", ignoreCase = true)) "mp4" else "webm"
      val storagePath = "seasons/$seasonId/cctvRecordings/$recId.$ext"
      val cleanMime = mimeType.substringBefore(';').trim().ifBlank { "video/$ext" }.take(80)
      val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(startedAtMillis))
      val cleanLabel = cameraLabel.trim().take(80).ifBlank { cleanCamId }
      val clampedDuration = durationMillis.coerceIn(500L, 300_000L)

      val storageRef = Firebase.storage.reference.child(storagePath)
      val metadata = StorageMetadata.Builder()
        .setContentType(cleanMime)
        .setCustomMetadata("seasonId", seasonId)
        .setCustomMetadata("cameraId", cleanCamId)
        .setCustomMetadata("recordedBy", adminUid)
        .build()

      storageRef.putBytes(videoBytes, metadata).await()
      val downloadUrl = storageRef.downloadUrl.await().toString()

      val payload = mapOf(
        "seasonId" to seasonId,
        "cameraId" to cleanCamId,
        "cameraLabel" to cleanLabel,
        "storagePath" to storagePath,
        "downloadUrl" to downloadUrl,
        "dateKey" to dateKey,
        "startedAtMillis" to startedAtMillis,
        "durationMillis" to clampedDuration,
        "sizeBytes" to videoBytes.size.toLong(),
        "mimeType" to cleanMime,
        "recordedBy" to adminUid,
        "createdAt" to FieldValue.serverTimestamp(),
      )

      db.collection("seasons").document(seasonId)
        .collection("cctvRecordings").document(recId)
        .set(payload)
        .await()

      db.collection("seasons").document(seasonId)
        .collection("adminControls").document()
        .set(
          mapOf(
            "seasonId" to seasonId,
            "commandType" to "CCTV_RECORDING",
            "payloadSummary" to "Saved $cleanLabel (${clampedDuration / 1000}s) to Cloud Archive".take(300),
            "issuedBy" to adminUid,
            "createdAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      Result.success(
        CctvRecordingRecord(
          id = recId,
          seasonId = seasonId,
          cameraId = cleanCamId,
          cameraLabel = cleanLabel,
          storagePath = storagePath,
          downloadUrl = downloadUrl,
          dateKey = dateKey,
          startedAtMillis = startedAtMillis,
          durationMillis = clampedDuration,
          sizeBytes = videoBytes.size.toLong(),
          mimeType = cleanMime,
          recordedBy = adminUid,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteCctvRecording(
    seasonId: String = DEFAULT_SEASON_ID,
    record: CctvRecordingRecord,
  ): Result<Unit> {
    val path = "seasons/$seasonId/cctvRecordings/${record.id}"
    return try {
      requireUserId()
      if (record.storagePath.isNotBlank()) {
        try {
          Firebase.storage.reference.child(record.storagePath).delete().await()
        } catch (_: Exception) {
          // Proceed with Firestore index cleanup even if storage object was already removed
        }
      }
      db.collection("seasons").document(seasonId)
        .collection("cctvRecordings").document(record.id)
        .delete()
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  companion object {
    const val DEFAULT_SEASON_ID = "season_01"
    const val MAX_PLAYERS = 15L
    val FORBIDDEN_CCTV_KEYWORDS = listOf("bathroom", "washroom", "changing", "private", "smoking")
  }
}
