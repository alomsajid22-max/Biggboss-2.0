package com.example.multiplayer

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONObject

data class AdminSessionSnapshot(
  val uid: String = "",
  val username: String = "BIGGBOSS",
  val sessionId: String = "",
  val expiresAtMillis: Long = 0L,
  val mustChangeInitialPassword: Boolean = false,
  val isLocalOnly: Boolean = false,
)

object AdminAuthValidation {
  private const val REQUIRED_ACCESS_KEYWORD = "mehnaz"

  fun isValidAccessKeyword(input: String): Boolean = input.trim() == REQUIRED_ACCESS_KEYWORD

  /**
   * Defense-in-depth invariant: Entering the keyword opens the Admin Authentication screen
   * ONLY and can NEVER elevate permissions or grant admin role on the client.
   */
  fun canGrantAdminOnKeyword(@Suppress("UNUSED_PARAMETER") keywordAccepted: Boolean): Boolean = false

  fun isSessionExpired(snapshot: AdminSessionSnapshot?, nowMs: Long = System.currentTimeMillis()): Boolean {
    if (snapshot == null) return false
    return snapshot.expiresAtMillis > 0L && nowMs >= snapshot.expiresAtMillis
  }

  fun validatePasswordChange(
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
  ): String? {
    if (currentPassword.isEmpty()) return "Enter your current Admin password."
    if (newPassword.length !in 8..128) return "New password must be 8 to 128 characters."
    if (newPassword == currentPassword) return "New password must differ from current password."
    if (newPassword != confirmPassword) return "New password and confirmation do not match."
    return null
  }
}

@Keep
@IgnoreExtraProperties
data class VipPrivilege(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val userId: String = "",
  val playerId: String = "",
  val displayName: String = "",
  val vipActive: Boolean = false,
  val vipEnabled: Boolean? = null,
  val vipRoomAccess: Boolean? = null,
  val vipLabel: String = "VIP PASS",
  val grantedBy: String = "",
  val updatedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val effectiveVip: Boolean
    get() = vipEnabled ?: vipActive
}

@Keep
@IgnoreExtraProperties
data class PrivilegeHistoryRecord(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val eventType: String = "",
  val actionType: String = "",
  val targetUid: String = "",
  val targetName: String = "",
  val targetDisplayName: String = "",
  val summary: String = "",
  val detail: String = "",
  val actorUid: String = "",
  val issuedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
) {
  val displayEventType: String
    get() = actionType.ifBlank { eventType }

  val displaySummary: String
    get() = detail.ifBlank { summary }
}

@Keep
@IgnoreExtraProperties
data class GameSettings(
  @DocumentId val id: String = "config",
  val seasonId: String = "season_01",
  val houseTitle: String = "Season 01 • Bigg Boss – The House",
  val defaultCaptainVipAccess: Boolean = true,
  val defaultCaptainImmunity: Boolean = false,
  val defaultCaptainDoubleVote: Boolean = false,
  val defaultCaptainTieBreaker: Boolean = false,
  val defaultVotingDurationMinutes: Long = 15L,
  val allowNpcVotesByDefault: Boolean = false,
  val updatedBy: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AdminAuditLog(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val actionType: String = "",
  val summary: String = "",
  val actorUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

data class CaptainPowersConfig(
  val vipRoomAccess: Boolean = true,
  val nominationImmunity: Boolean = false,
  val doubleVoteWeight: Boolean = false,
  val tieBreakerPower: Boolean = false,
) {
  companion object {
    fun fromSeason(season: SeasonSession?): CaptainPowersConfig {
      if (season == null) return CaptainPowersConfig()
      return CaptainPowersConfig(
        vipRoomAccess = season.captainVipAccess,
        nominationImmunity = season.captainImmunity,
        doubleVoteWeight = season.captainDoubleVote,
        tieBreakerPower = season.captainTieBreaker,
      )
    }
  }
}

data class ServerTallyRow(
  val uid: String,
  val displayName: String,
  val realVotes: Int,
  val npcVotes: Int,
  val totalVotes: Int,
)

data class WeightedVotingOutcome(
  val baseResult: VotingTallyResult,
  val weightedTallies: List<NomineeVoteTally>,
  val eligibleCandidates: List<NomineeVoteTally>,
  val isTie: Boolean,
  val immuneCaptainUid: String?,
  val immuneCaptainExcluded: Boolean,
  val doubleVoteApplied: Boolean,
)

object Phase11VipLogic {
  fun hasVipRoomAccess(
    uid: String?,
    isAdmin: Boolean,
    season: SeasonSession?,
    vipPrivileges: List<VipPrivilege>,
  ): Boolean {
    if (isAdmin) return true
    if (uid.isNullOrBlank()) return false
    val isEligibleCaptain = !season?.captainUid.isNullOrBlank() &&
      season?.captainUid == uid &&
      (season.captainVipAccess)
    if (isEligibleCaptain) return true
    return vipPrivileges.any { it.userId == uid && it.effectiveVip }
  }

  fun activeVipUids(vipPrivileges: List<VipPrivilege>): List<String> =
    vipPrivileges.filter { it.effectiveVip && it.userId.isNotBlank() }.map { it.userId }.distinct()

  fun resolvePlayerBadges(
    uid: String,
    season: SeasonSession?,
    vipPrivileges: List<VipPrivilege>,
  ): List<String> {
    val badges = mutableListOf<String>()
    if (!season?.captainUid.isNullOrBlank() && season?.captainUid == uid) {
      badges.add("👑 CAPTAIN")
    }
    if (vipPrivileges.any { it.userId == uid && it.effectiveVip }) {
      badges.add("💎 VIP")
    }
    return badges
  }

  fun isProtectedByCaptainImmunity(uid: String, season: SeasonSession?): Boolean {
    return !season?.captainUid.isNullOrBlank() &&
      season?.captainUid == uid &&
      season.captainImmunity
  }

  fun calculateWeightedVotingTally(
    cycle: VotingCycle,
    ballots: List<VotingBallot>,
    season: SeasonSession?,
  ): WeightedVotingOutcome {
    val captainUid = season?.captainUid?.takeIf { it.isNotBlank() }
    val doubleVoteActive = captainUid != null && season.captainDoubleVote
    val immunityActive = captainUid != null && season.captainImmunity

    val nominees = cycle.parsedNominees()
    var doubleVoteWasApplied = false
    val tallies = nominees.map { nominee ->
      val forNominee = ballots.filter { it.targetUid == nominee.uid }
      var realWeight = 0
      var npcCount = 0
      forNominee.forEach { b ->
        if (b.sourceType == "NPC") {
          if (cycle.npcVotesEnabled) npcCount += 1
        } else {
          val weight = if (doubleVoteActive && b.voterUid == captainUid) {
            doubleVoteWasApplied = true
            2
          } else {
            1
          }
          realWeight += weight
        }
      }
      NomineeVoteTally(
        nominee = nominee,
        realVotes = realWeight,
        npcVotes = npcCount,
        totalVotes = realWeight + npcCount,
      )
    }

    val isSaveRule = cycle.voteType.equals("SAVE", ignoreCase = true)
    val sortedAll = if (isSaveRule) {
      tallies.sortedWith(compareBy<NomineeVoteTally> { it.totalVotes }.thenBy { it.realVotes })
    } else {
      tallies.sortedWith(compareByDescending<NomineeVoteTally> { it.totalVotes }.thenByDescending { it.realVotes })
    }

    val withoutImmuneCaptain = if (immunityActive) {
      sortedAll.filter { it.nominee.uid != captainUid }
    } else {
      sortedAll
    }
    val immuneExcluded = immunityActive && sortedAll.any { it.nominee.uid == captainUid }
    val effectivePool = if (withoutImmuneCaptain.isNotEmpty()) withoutImmuneCaptain else sortedAll

    val topScore = effectivePool.firstOrNull()?.totalVotes
    val leaders = if (topScore == null || effectivePool.isEmpty()) {
      emptyList()
    } else {
      effectivePool.filter { it.totalVotes == topScore }
    }
    val hasValidVotes = ballots.isNotEmpty()
    val isTie = hasValidVotes && leaders.size > 1

    val base = VotingTallyResult(
      cycleId = cycle.id,
      voteType = cycle.voteType,
      rankedTallies = sortedAll,
      leadingEvictionCandidates = leaders,
      isTie = isTie,
      totalRealVotes = tallies.sumOf { it.realVotes },
      totalNpcVotes = tallies.sumOf { it.npcVotes },
    )

    return WeightedVotingOutcome(
      baseResult = base,
      weightedTallies = sortedAll,
      eligibleCandidates = leaders,
      isTie = isTie,
      immuneCaptainUid = if (immunityActive) captainUid else null,
      immuneCaptainExcluded = immuneExcluded,
      doubleVoteApplied = doubleVoteWasApplied,
    )
  }

  fun serializeTallyRowsJson(tallies: List<NomineeVoteTally>): String {
    val root = JSONObject()
    val arr = org.json.JSONArray()
    tallies.forEach { t ->
      arr.put(
        JSONObject().apply {
          put("uid", t.nominee.uid)
          put("displayName", t.nominee.displayName)
          put("realVotes", t.realVotes)
          put("npcVotes", t.npcVotes)
          put("totalVotes", t.totalVotes)
        }
      )
    }
    root.put("tallies", arr)
    return root.toString()
  }

  fun parseServerTallySummary(serverTallyJson: String?): List<ServerTallyRow> {
    if (serverTallyJson.isNullOrBlank()) return emptyList()
    return runCatching {
      val trimmed = serverTallyJson.trim()
      val arr = if (trimmed.startsWith("[")) {
        org.json.JSONArray(trimmed)
      } else {
        JSONObject(trimmed).optJSONArray("tallies") ?: org.json.JSONArray()
      }
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          add(
            ServerTallyRow(
              uid = obj.optString("uid", ""),
              displayName = obj.optString("displayName", obj.optString("name", "Nominee")),
              realVotes = obj.optInt("realVotes", 0),
              npcVotes = obj.optInt("npcVotes", 0),
              totalVotes = obj.optInt("totalVotes", 0),
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }
}
