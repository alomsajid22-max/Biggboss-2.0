package com.example.multiplayer

import com.example.multiplayer.MultiplayerFirestoreRepository.Companion.DEFAULT_SEASON_ID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.max

/**
 * Firestore repository extension handling Phase 9 (ChatGPT AI Control Room)
 * and Phase 10 (Peer Nominations, Voting Timer Rules & Eviction Extensions).
 */
class Phase9And10FirestoreRepository(
  private val auth: FirebaseAuth,
  private val db: FirebaseFirestore,
) {

  private fun requireUserId(): String =
    auth.currentUser?.uid ?: throw IllegalStateException("Authentication required.")

  fun observeAiConfig(seasonId: String = DEFAULT_SEASON_ID): Flow<AiControlConfig?> = flow {
    val uid = requireUserId()
    val ref = db.collection("seasons").document(seasonId)
      .collection("aiConfig").document(Phase9AiCatalog.DEFAULT_CONFIG_ID)
    emitAll(
      ref.snapshots()
        .map { snap ->
          if (!snap.exists()) null
          else snap.toObject(AiControlConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { err ->
          if (err is CancellationException) throw err
          if (auth.currentUser?.uid == uid) {
            emit(null)
          }
        }
    )
  }

  fun observeAiSchedules(seasonId: String = DEFAULT_SEASON_ID): Flow<List<AiScheduledEvent>> = flow {
    val uid = requireUserId()
    val col = db.collection("seasons").document(seasonId).collection("aiSchedules")
    emitAll(
      col.whereEqualTo("seasonId", seasonId)
        .snapshots()
        .map { snap ->
          snap.toObjects(AiScheduledEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.targetTimeMillis }
        }
        .catch { err ->
          if (err is CancellationException) throw err
          if (auth.currentUser?.uid == uid) emit(emptyList())
        }
    )
  }

  fun observeAiProposals(seasonId: String = DEFAULT_SEASON_ID): Flow<List<AiApprovalProposal>> = flow {
    val uid = requireUserId()
    val col = db.collection("seasons").document(seasonId).collection("aiProposals")
    emitAll(
      col.whereEqualTo("seasonId", seasonId)
        .snapshots()
        .map { snap ->
          snap.toObjects(AiApprovalProposal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
        .catch { err ->
          if (err is CancellationException) throw err
          if (auth.currentUser?.uid == uid) emit(emptyList())
        }
    )
  }

  fun observeAiLogs(seasonId: String = DEFAULT_SEASON_ID): Flow<List<AiActivityLog>> = flow {
    val uid = requireUserId()
    val col = db.collection("seasons").document(seasonId).collection("aiLogs")
    emitAll(
      col.whereEqualTo("seasonId", seasonId)
        .snapshots()
        .map { snap ->
          snap.toObjects(AiActivityLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.executedAtMillis }
        }
        .catch { err ->
          if (err is CancellationException) throw err
          if (auth.currentUser?.uid == uid) emit(emptyList())
        }
    )
  }

  suspend fun ensureAiConfigInitialized(seasonId: String = DEFAULT_SEASON_ID): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val configRef = db.collection("seasons").document(seasonId)
        .collection("aiConfig").document(Phase9AiCatalog.DEFAULT_CONFIG_ID)
      val snap = configRef.get().await()
      if (!snap.exists()) {
        val now = System.currentTimeMillis()
        configRef.set(
          mapOf(
            "seasonId" to seasonId,
            "enabled" to true,
            "mode" to AiControlMode.ASSIST.name,
            "emergencyStopped" to false,
            "apiProvider" to AiApiProviderStatus.RULE_ENGINE_NO_API_KEY.key,
            "autoHindiAnnouncements" to true,
            "autoKitchenMonitor" to true,
            "autoTaskCoordinator" to true,
            "autoTimerMonitor" to true,
            "tickIntervalMinutes" to 1L,
            "lastTickAtMillis" to now,
            "lastAdminSeenAtMillis" to now,
            "unseenOfflineCount" to 0L,
            "updatedBy" to adminUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateAiConfig(
    seasonId: String = DEFAULT_SEASON_ID,
    enabled: Boolean? = null,
    mode: AiControlMode? = null,
    clearEmergencyStop: Boolean = false,
    autoHindiAnnouncements: Boolean? = null,
    autoKitchenMonitor: Boolean? = null,
    autoTaskCoordinator: Boolean? = null,
    autoTimerMonitor: Boolean? = null,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      ensureAiConfigInitialized(seasonId)
      val configRef = db.collection("seasons").document(seasonId)
        .collection("aiConfig").document(Phase9AiCatalog.DEFAULT_CONFIG_ID)

      val updates = mutableMapOf<String, Any>(
        "updatedBy" to adminUid,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      if (enabled != null) updates["enabled"] = enabled
      if (mode != null) updates["mode"] = mode.name
      if (clearEmergencyStop) updates["emergencyStopped"] = false
      if (autoHindiAnnouncements != null) updates["autoHindiAnnouncements"] = autoHindiAnnouncements
      if (autoKitchenMonitor != null) updates["autoKitchenMonitor"] = autoKitchenMonitor
      if (autoTaskCoordinator != null) updates["autoTaskCoordinator"] = autoTaskCoordinator
      if (autoTimerMonitor != null) updates["autoTimerMonitor"] = autoTimerMonitor

      configRef.update(updates).await()

      val logKey = "cfg_${System.currentTimeMillis()}"
      db.collection("seasons").document(seasonId).collection("aiLogs").document(logKey).set(
        mapOf(
          "seasonId" to seasonId,
          "category" to "AI_CONFIG",
          "mode" to (mode?.name ?: AiControlMode.ASSIST.name),
          "source" to "UI_CONTROL",
          "title" to "AI Config Updated",
          "detail" to "enabled=${enabled ?: "keep"}, mode=${mode?.name ?: "keep"}, resetEStop=$clearEmergencyStop",
          "hindiAnnouncement" to null,
          "executionKey" to logKey,
          "executedAtMillis" to System.currentTimeMillis(),
          "actorUid" to adminUid,
          "acknowledgedByAdmin" to true,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun triggerEmergencyStop(
    seasonId: String = DEFAULT_SEASON_ID,
    pendingProposalIds: List<String>,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      ensureAiConfigInitialized(seasonId)
      val seasonRef = db.collection("seasons").document(seasonId)
      val configRef = seasonRef.collection("aiConfig").document(Phase9AiCatalog.DEFAULT_CONFIG_ID)
      val now = System.currentTimeMillis()
      val stopKey = "estop_$now"

      db.runTransaction { tx ->
        tx.update(
          configRef,
          mapOf(
            "enabled" to false,
            "mode" to AiControlMode.MANUAL.name,
            "emergencyStopped" to true,
            "updatedBy" to adminUid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        pendingProposalIds.take(15).forEach { propId ->
          tx.update(
            seasonRef.collection("aiProposals").document(propId),
            mapOf(
              "status" to "CANCELLED",
              "reviewedBy" to adminUid,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        tx.set(
          seasonRef.collection("aiLogs").document(stopKey),
          mapOf(
            "seasonId" to seasonId,
            "category" to "EMERGENCY_STOP",
            "mode" to "OFF",
            "source" to "EMERGENCY_STOP",
            "title" to "EMERGENCY STOP TRIGGERED",
            "detail" to "Admin halted all AI automation immediately and cancelled ${pendingProposalIds.size} pending proposal(s).",
            "hindiAnnouncement" to null,
            "executionKey" to stopKey,
            "executedAtMillis" to now,
            "actorUid" to adminUid,
            "acknowledgedByAdmin" to true,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()

      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun acknowledgeWhileAway(
    seasonId: String = DEFAULT_SEASON_ID,
    unseenLogIds: List<String>,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val seasonRef = db.collection("seasons").document(seasonId)
      val configRef = seasonRef.collection("aiConfig").document(Phase9AiCatalog.DEFAULT_CONFIG_ID)
      val now = System.currentTimeMillis()
      val batch = db.batch()
      batch.update(
        configRef,
        mapOf(
          "lastAdminSeenAtMillis" to now,
          "unseenOfflineCount" to 0L,
          "updatedBy" to adminUid,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      unseenLogIds.take(25).forEach { logId ->
        batch.update(
          seasonRef.collection("aiLogs").document(logId),
          mapOf("acknowledgedByAdmin" to true),
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun createAiSchedule(
    seasonId: String = DEFAULT_SEASON_ID,
    actionType: AiActionType,
    title: String,
    messageOrPayload: String,
    targetTimeMillis: Long,
    recurrenceMinutes: Long = 0L,
    modeOverride: String = "INHERIT",
  ): Result<String> {
    return try {
      val adminUid = requireUserId()
      val schedId = "sched_${System.currentTimeMillis()}_${(100..999).random()}"
      val execKey = "sched_${schedId}"
      db.collection("seasons").document(seasonId).collection("aiSchedules").document(schedId).set(
        mapOf(
          "seasonId" to seasonId,
          "actionType" to actionType.name,
          "title" to title.trim().ifBlank { actionType.label }.take(100),
          "messageOrPayload" to messageOrPayload.trim().take(600),
          "targetTimeMillis" to max(1L, targetTimeMillis),
          "recurrenceMinutes" to recurrenceMinutes.coerceIn(0L, 1440L),
          "status" to "PENDING",
          "modeOverride" to modeOverride,
          "executionKey" to execKey,
          "lastExecutedAtMillis" to 0L,
          "createdBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(schedId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun cancelAiSchedule(
    seasonId: String = DEFAULT_SEASON_ID,
    scheduleId: String,
  ): Result<Unit> {
    return try {
      requireUserId()
      db.collection("seasons").document(seasonId).collection("aiSchedules").document(scheduleId)
        .update(
          mapOf(
            "status" to "CANCELLED",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun createAiProposal(
    seasonId: String = DEFAULT_SEASON_ID,
    proposal: AiApprovalProposal,
  ): Result<Unit> {
    return try {
      requireUserId()
      val docId = proposal.id.ifBlank { "prop_${System.currentTimeMillis()}" }.take(120)
      val ref = db.collection("seasons").document(seasonId).collection("aiProposals").document(docId)
      if (ref.get().await().exists()) return Result.success(Unit)
      ref.set(
        mapOf(
          "seasonId" to seasonId,
          "category" to proposal.category,
          "title" to proposal.title.take(100),
          "summary" to proposal.summary.take(400),
          "hindiAnnouncement" to proposal.hindiAnnouncement?.take(320),
          "payloadJson" to proposal.payloadJson?.take(1000),
          "status" to "PENDING",
          "executionKey" to proposal.executionKey.ifBlank { docId },
          "reviewedBy" to null,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun markProposalReviewed(
    seasonId: String = DEFAULT_SEASON_ID,
    proposalId: String,
    approved: Boolean,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val status = if (approved) "EXECUTED" else "REJECTED"
      db.collection("seasons").document(seasonId).collection("aiProposals").document(proposalId)
        .update(
          mapOf(
            "status" to status,
            "reviewedBy" to adminUid,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Atomically claims an executionKey inside aiLogs/{logId} before running side effects,
   * guaranteeing offline catch-ups and reconnects never duplicate executions.
   */
  suspend fun claimIdempotentLogAndTransitionSchedule(
    seasonId: String = DEFAULT_SEASON_ID,
    log: AiActivityLog,
    scheduleTransition: AiScheduleTransition? = null,
    isOfflineCatchUp: Boolean = false,
  ): Boolean {
    return try {
      val adminUid = requireUserId()
      val seasonRef = db.collection("seasons").document(seasonId)
      val logId = "log_${log.executionKey}".replace(Regex("[^a-zA-Z0-9_-]"), "_").take(120)
      val logRef = seasonRef.collection("aiLogs").document(logId)
      val configRef = seasonRef.collection("aiConfig").document(Phase9AiCatalog.DEFAULT_CONFIG_ID)

      db.runTransaction { tx ->
        if (tx.get(logRef).exists()) {
          return@runTransaction false
        }
        val cfgSnap = tx.get(configRef)
        if (!cfgSnap.exists() || cfgSnap.getBoolean("enabled") != true || cfgSnap.getBoolean("emergencyStopped") == true) {
          return@runTransaction false
        }
        tx.set(
          logRef,
          mapOf(
            "seasonId" to seasonId,
            "category" to log.category,
            "mode" to log.mode,
            "source" to log.source,
            "title" to log.title.take(100),
            "detail" to log.detail.take(500),
            "hindiAnnouncement" to log.hindiAnnouncement?.take(320),
            "executionKey" to log.executionKey,
            "executedAtMillis" to log.executedAtMillis,
            "actorUid" to adminUid,
            "acknowledgedByAdmin" to !isOfflineCatchUp,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
        if (scheduleTransition != null) {
          tx.update(
            seasonRef.collection("aiSchedules").document(scheduleTransition.scheduleId),
            mapOf(
              "status" to scheduleTransition.nextStatus,
              "targetTimeMillis" to scheduleTransition.nextTargetTimeMillis,
              "lastExecutedAtMillis" to scheduleTransition.lastExecutedAtMillis,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        tx.update(
          configRef,
          mapOf(
            "lastTickAtMillis" to System.currentTimeMillis(),
            "unseenOfflineCount" to if (isOfflineCatchUp) FieldValue.increment(1L) else 0L,
            "updatedBy" to adminUid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        true
      }.await()
    } catch (_: FirebaseFirestoreException) {
      false
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      false
    }
  }

  // ---------------- PHASE 10 PEER NOMINATIONS ----------------

  fun observeNominationsForCycle(
    seasonId: String = DEFAULT_SEASON_ID,
    cycleId: String,
  ): Flow<List<NominationBallot>> = flow {
    val uid = requireUserId()
    val col = db.collection("seasons").document(seasonId)
      .collection("votingCycles").document(cycleId)
      .collection("nominations")
    emitAll(
      col.whereEqualTo("seasonId", seasonId)
        .whereEqualTo("cycleId", cycleId)
        .snapshots()
        .map { snap ->
          snap.toObjects(NominationBallot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
        .catch { err ->
          if (err is CancellationException) throw err
          if (auth.currentUser?.uid == uid) emit(emptyList())
        }
    )
  }

  suspend fun submitPeerNomination(
    seasonId: String = DEFAULT_SEASON_ID,
    cycleId: String,
    voterDisplayName: String,
    targets: List<NomineeSummary>,
    reason: String? = null,
  ): Result<Unit> {
    return try {
      val voterUid = requireUserId()
      require(targets.isNotEmpty()) { "Select at least one housemate to nominate." }
      require(targets.none { it.uid == voterUid }) { "You cannot nominate yourself." }

      val nominationRef = db.collection("seasons").document(seasonId)
        .collection("votingCycles").document(cycleId)
        .collection("nominations").document(voterUid)

      if (nominationRef.get().await().exists()) {
        throw IllegalStateException("You have already submitted your nominations for this cycle.")
      }

      val distinctTargets = targets.distinctBy { it.uid }
      nominationRef.set(
        mapOf(
          "seasonId" to seasonId,
          "cycleId" to cycleId,
          "voterUid" to voterUid,
          "voterName" to voterDisplayName.take(40),
          "targetUids" to distinctTargets.map { it.uid },
          "targetNames" to distinctTargets.map { it.displayName },
          "reason" to reason?.trim()?.take(200)?.ifBlank { null },
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
