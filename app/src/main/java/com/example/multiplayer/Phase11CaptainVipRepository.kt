package com.example.multiplayer

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val P11_TAG = "Phase11CaptainVipRepo"

class Phase11CaptainVipRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid ?: throw IllegalStateException("User must be authenticated.")

  private fun seasonDoc(seasonId: String) =
    db.collection("seasons").document(seasonId)

  fun observeVipPrivileges(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
  ): Flow<List<VipPrivilege>> = flow {
    requireUserId()
    emitAll(
      seasonDoc(seasonId).collection("vipPrivileges")
        .snapshots()
        .map { snap ->
          snap.documents.mapNotNull { doc ->
            doc.toObject(VipPrivilege::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          }.sortedBy { it.displayName.lowercase() }
        }
        .catch { err ->
          if (err is CancellationException) throw err
          Log.w(P11_TAG, "observeVipPrivileges error", err)
          emit(emptyList())
        }
    )
  }

  fun observePrivilegeHistory(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
  ): Flow<List<PrivilegeHistoryRecord>> = flow {
    requireUserId()
    emitAll(
      seasonDoc(seasonId).collection("privilegeHistory")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(40)
        .snapshots()
        .map { snap ->
          snap.documents.mapNotNull { doc ->
            doc.toObject(PrivilegeHistoryRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          }
        }
        .catch { err ->
          if (err is CancellationException) throw err
          Log.w(P11_TAG, "observePrivilegeHistory error", err)
          emit(emptyList())
        }
    )
  }

  fun observeGameSettings(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
  ): Flow<GameSettings?> = flow {
    requireUserId()
    emitAll(
      seasonDoc(seasonId).collection("gameSettings").document("config")
        .snapshots()
        .map { doc ->
          if (!doc.exists()) null
          else doc.toObject(GameSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { err ->
          if (err is CancellationException) throw err
          Log.w(P11_TAG, "observeGameSettings error", err)
          emit(null)
        }
    )
  }

  fun observeAdminAuditLogs(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
  ): Flow<List<AdminAuditLog>> = flow {
    requireUserId()
    emitAll(
      seasonDoc(seasonId).collection("adminAuditLogs")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(40)
        .snapshots()
        .map { snap ->
          snap.documents.mapNotNull { doc ->
            doc.toObject(AdminAuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          }
        }
        .catch { err ->
          if (err is CancellationException) throw err
          Log.w(P11_TAG, "observeAdminAuditLogs error", err)
          emit(emptyList())
        }
    )
  }

  suspend fun assignOrReplaceCaptain(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    member: SeasonMember,
    title: String,
    powers: CaptainPowersConfig,
    scoreAwarded: Long = 100L,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val cleanTitle = title.trim().ifBlank { "House Captain" }.take(80)
      val batch = db.batch()
      batch.update(
        seasonDoc(seasonId),
        mapOf(
          "captainUid" to member.userId,
          "captainPlayerId" to member.playerId,
          "captainDisplayName" to member.displayName,
          "captainTitle" to cleanTitle,
          "captainVipAccess" to powers.vipRoomAccess,
          "captainImmunity" to powers.nominationImmunity,
          "captainDoubleVote" to powers.doubleVoteWeight,
          "captainTieBreaker" to powers.tieBreakerPower,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("captaincyHistory").document(),
        mapOf(
          "seasonId" to seasonId,
          "taskId" to null,
          "captainUid" to member.userId,
          "captainPlayerId" to member.playerId,
          "captainDisplayName" to member.displayName,
          "captainCharacterId" to member.characterId,
          "title" to cleanTitle,
          "scoreAwarded" to scoreAwarded.coerceIn(0L, 1000L),
          "assignedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("privilegeHistory").document(),
        mapOf(
          "seasonId" to seasonId,
          "targetUid" to member.userId,
          "targetDisplayName" to member.displayName.take(40),
          "actionType" to "CAPTAIN_ASSIGNED",
          "detail" to "Captain assigned: ${member.displayName} ($cleanTitle)".take(240),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("adminControls").document(),
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "CAPTAINCY_UPDATE",
          "payloadSummary" to "Captain set to ${member.displayName} (${member.playerId})".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("adminAuditLogs").document(),
        mapOf(
          "seasonId" to seasonId,
          "actionType" to "CAPTAIN_ASSIGNED",
          "summary" to "Assigned ${member.displayName} as $cleanTitle".take(240),
          "actorUid" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun removeCaptain(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    previousCaptainUid: String?,
    previousCaptainName: String?,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val batch = db.batch()
      batch.update(
        seasonDoc(seasonId),
        mapOf(
          "captainUid" to null,
          "captainPlayerId" to null,
          "captainDisplayName" to null,
          "captainTitle" to null,
          "captainVipAccess" to true,
          "captainImmunity" to false,
          "captainDoubleVote" to false,
          "captainTieBreaker" to false,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("privilegeHistory").document(),
        mapOf(
          "seasonId" to seasonId,
          "targetUid" to (previousCaptainUid?.takeIf { it.isNotBlank() } ?: "HOUSE"),
          "targetDisplayName" to (previousCaptainName?.takeIf { it.isNotBlank() } ?: "Former Captain").take(40),
          "actionType" to "CAPTAIN_REMOVED",
          "detail" to "House Captain removed (${previousCaptainName ?: "None"})".take(240),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("adminControls").document(),
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "CAPTAINCY_UPDATE",
          "payloadSummary" to "Removed House Captain (${previousCaptainName ?: "None"})".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("adminAuditLogs").document(),
        mapOf(
          "seasonId" to seasonId,
          "actionType" to "CAPTAIN_REMOVED",
          "summary" to "Cleared captaincy from ${previousCaptainName ?: "None"}".take(240),
          "actorUid" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateCaptainPowers(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    powers: CaptainPowersConfig,
    captainDisplayName: String?,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val summary = "Powers -> VIP:${powers.vipRoomAccess} Immunity:${powers.nominationImmunity} 2xVote:${powers.doubleVoteWeight} TieBreak:${powers.tieBreakerPower}"
      val batch = db.batch()
      batch.update(
        seasonDoc(seasonId),
        mapOf(
          "captainVipAccess" to powers.vipRoomAccess,
          "captainImmunity" to powers.nominationImmunity,
          "captainDoubleVote" to powers.doubleVoteWeight,
          "captainTieBreaker" to powers.tieBreakerPower,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("privilegeHistory").document(),
        mapOf(
          "seasonId" to seasonId,
          "targetUid" to "HOUSE",
          "targetDisplayName" to (captainDisplayName?.takeIf { it.isNotBlank() } ?: "Captain").take(40),
          "actionType" to "CAPTAIN_POWER_UPDATED",
          "detail" to summary.take(240),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("adminControls").document(),
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "CAPTAIN_POWER",
          "payloadSummary" to summary.take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun setVipPrivilege(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    targetUid: String,
    playerId: String,
    displayName: String,
    enabled: Boolean,
    vipLabel: String = "VIP PASS",
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val cleanLabel = vipLabel.trim().ifBlank { "VIP PASS" }.take(30)
      val vipRef = seasonDoc(seasonId).collection("vipPrivileges").document(targetUid)
      val existing = vipRef.get().await()
      val batch = db.batch()
      if (existing.exists()) {
        batch.update(
          vipRef,
          mapOf(
            "vipEnabled" to enabled,
            "vipRoomAccess" to enabled,
            "vipActive" to enabled,
            "vipLabel" to cleanLabel,
            "updatedBy" to adminUid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      } else {
        batch.set(
          vipRef,
          mapOf(
            "seasonId" to seasonId,
            "userId" to targetUid,
            "playerId" to playerId.take(24),
            "displayName" to displayName.take(40).ifBlank { targetUid },
            "vipEnabled" to enabled,
            "vipRoomAccess" to enabled,
            "vipActive" to enabled,
            "vipLabel" to cleanLabel,
            "updatedBy" to adminUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      val evt = if (enabled) "VIP_GRANTED" else "VIP_REVOKED"
      batch.set(
        seasonDoc(seasonId).collection("privilegeHistory").document(),
        mapOf(
          "seasonId" to seasonId,
          "targetUid" to targetUid,
          "targetDisplayName" to displayName.take(40).ifBlank { targetUid },
          "actionType" to evt,
          "detail" to "${if (enabled) "Granted" else "Revoked"} $cleanLabel for $displayName".take(240),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("adminControls").document(),
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "VIP_PRIVILEGE",
          "payloadSummary" to "$evt: $displayName ($playerId)".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun saveGameSettings(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    settings: GameSettings,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val cleanTitle = settings.houseTitle.trim().ifBlank { "Season 01 • Bigg Boss – The House" }.take(60)
      val batch = db.batch()
      batch.set(
        seasonDoc(seasonId).collection("gameSettings").document("config"),
        mapOf(
          "seasonId" to seasonId,
          "seasonTitle" to cleanTitle,
          "houseTitle" to cleanTitle,
          "defaultCaptainVipAccess" to settings.defaultCaptainVipAccess,
          "defaultCaptainImmunity" to settings.defaultCaptainImmunity,
          "defaultCaptainDoubleVote" to settings.defaultCaptainDoubleVote,
          "defaultCaptainTieBreaker" to settings.defaultCaptainTieBreaker,
          "votingDurationMinutes" to settings.defaultVotingDurationMinutes.coerceIn(1L, 1440L),
          "defaultVotingDurationMinutes" to settings.defaultVotingDurationMinutes.coerceIn(1L, 1440L),
          "allowNpcFillers" to settings.allowNpcVotesByDefault,
          "allowNpcVotesByDefault" to settings.allowNpcVotesByDefault,
          "updatedBy" to adminUid,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        SetOptions.merge(),
      )
      batch.update(
        seasonDoc(seasonId),
        mapOf(
          "title" to cleanTitle,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("adminControls").document(),
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "GAME_SETTINGS",
          "payloadSummary" to "Updated game settings ($cleanTitle, ${settings.defaultVotingDurationMinutes}m vote)".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun appendAdminAuditLog(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    actionType: String,
    summary: String,
  ) {
    try {
      val adminUid = requireUserId()
      seasonDoc(seasonId).collection("adminAuditLogs").document().set(
        mapOf(
          "seasonId" to seasonId,
          "actionType" to actionType.take(40),
          "summary" to summary.take(240),
          "actorUid" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(P11_TAG, "appendAdminAuditLog skipped: ${e.message}")
    }
  }

  suspend fun writeTallyToVotingCycleFallback(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    cycle: VotingCycle,
    weightedOutcome: WeightedVotingOutcome,
    closeVoting: Boolean,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val nextStatus = if (closeVoting && cycle.status in listOf("OPEN", "NOMINATIONS")) "CLOSED" else cycle.status
      val jsonStr = Phase11VipLogic.serializeTallyRowsJson(weightedOutcome.weightedTallies)
      val cycleRef = seasonDoc(seasonId).collection("votingCycles").document(cycle.id)
      val batch = db.batch()
      batch.update(
        cycleRef,
        mapOf(
          "status" to nextStatus,
          "serverTallyJson" to jsonStr.take(1950),
          "serverTotalRealVotes" to weightedOutcome.baseResult.totalRealVotes.toLong(),
          "serverTotalNpcVotes" to weightedOutcome.baseResult.totalNpcVotes.toLong(),
          "serverIsTie" to weightedOutcome.isTie,
          "serverTalliedAtMillis" to System.currentTimeMillis(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("adminAuditLogs").document(),
        mapOf(
          "seasonId" to seasonId,
          "actionType" to "VOTING_TALLY",
          "summary" to "Tallied cycle '${cycle.title}' (close=$closeVoting, tie=${weightedOutcome.isTie})".take(240),
          "actorUid" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
