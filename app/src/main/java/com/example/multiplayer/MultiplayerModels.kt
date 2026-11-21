package com.example.multiplayer

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PlayerRole {
  PLAYER,
  ADMIN;

  companion object {
    fun fromRaw(raw: String?): PlayerRole =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PLAYER
  }
}

@Keep
@IgnoreExtraProperties
data class UserAccount(
  @DocumentId val id: String = "",
  val userId: String = "",
  val email: String? = null,
  val username: String? = null,
  val displayName: String = "",
  val normalizedName: String = "",
  val playerId: String = "",
  val characterId: String = "Player 01",
  val role: String = PlayerRole.PLAYER.name,
  val seasonId: String = "season_01",
  val banned: Boolean = false,
  val banReason: String? = null,
  val evictedSeasons: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SeasonSession(
  @DocumentId val id: String = "",
  val title: String = "Season 01 • Bigg Boss – The House",
  val status: String = "LOBBY",
  val maxPlayers: Long = 15L,
  val activeCount: Long = 0L,
  val dayNightMode: String = "day",
  val captainUid: String? = null,
  val captainPlayerId: String? = null,
  val captainDisplayName: String? = null,
  val captainTitle: String? = null,
  val captainVipAccess: Boolean = true,
  val captainImmunity: Boolean = false,
  val captainDoubleVote: Boolean = false,
  val captainTieBreaker: Boolean = false,
  val activeTaskId: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SeasonMember(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val userId: String = "",
  val playerId: String = "",
  val displayName: String = "",
  val characterId: String = "Player 01",
  val slotNumber: Long = 1L,
  val role: String = PlayerRole.PLAYER.name,
  val status: String = "LOBBY",
  val online: Boolean = false,
  val sessionId: String = "",
  @ServerTimestamp val joinedAt: Timestamp? = null,
  @ServerTimestamp val lastSeenAt: Timestamp? = null,
)

data class NomineeSummary(
  val uid: String,
  val playerId: String,
  val displayName: String,
  val characterId: String,
) {
  fun encode(): String = listOf(
    uid.replace("|", ""),
    playerId.replace("|", ""),
    displayName.replace("|", ""),
    characterId.replace("|", ""),
  ).joinToString("|")

  companion object {
    fun decode(raw: String): NomineeSummary? {
      val parts = raw.split("|")
      if (parts.size < 4) return null
      return NomineeSummary(
        uid = parts[0],
        playerId = parts[1],
        displayName = parts[2],
        characterId = parts[3],
      )
    }
  }
}

@Keep
@IgnoreExtraProperties
data class VotingCycle(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val title: String = "Weekly Eviction Nomination",
  val status: String = "NOMINATIONS", // NOMINATIONS | OPEN | CLOSED | EVICTED | CANCELLED
  val nominatedUids: List<String> = emptyList(),
  val nomineeSummaries: List<String> = emptyList(),
  val npcVotesEnabled: Boolean = false,
  val evictedUid: String? = null,
  val evictedDisplayName: String? = null,
  val voteType: String = "EVICT", // EVICT | SAVE
  val nominationsPerPlayer: Long = 1L,
  val nominationEndTimeMillis: Long = 0L,
  val votingDurationMinutes: Long = 15L,
  val votingEndTimeMillis: Long = 0L,
  val resultVisibility: String = "HIDDEN_UNTIL_CLOSED", // HIDDEN_UNTIL_CLOSED | LIVE_TOTALS | ADMIN_ONLY
  val tieBreakerPolicy: String = "ADMIN_DECIDES", // ADMIN_DECIDES | CAPTAIN_DECIDES | RANDOM_DRAW | NO_EVICTION
  val cancelledReason: String? = null,
  val serverTallyJson: String? = null,
  val serverTotalRealVotes: Long = 0L,
  val serverTotalNpcVotes: Long = 0L,
  val serverIsTie: Boolean = false,
  val serverTalliedAtMillis: Long = 0L,
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun parsedNominees(): List<NomineeSummary> =
    nomineeSummaries.mapNotNull { NomineeSummary.decode(it) }
}

@Keep
@IgnoreExtraProperties
data class NominationBallot(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val cycleId: String = "",
  val voterUid: String = "",
  val voterName: String = "",
  val targetUids: List<String> = emptyList(),
  val targetNames: List<String> = emptyList(),
  val reason: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class VotingBallot(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val cycleId: String = "",
  val voterUid: String = "",
  val voterName: String? = null,
  val targetUid: String = "",
  val sourceType: String = "REAL_PLAYER", // REAL_PLAYER | NPC
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class EvictionRecord(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val cycleId: String = "",
  val userId: String = "",
  val playerId: String = "",
  val displayName: String = "",
  val characterId: String = "Player 01",
  val realVotes: Long = 0L,
  val npcVotes: Long = 0L,
  val announcementText: String = "",
  val evictedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

data class NominationTallyEntry(
  val uid: String,
  val displayName: String,
  val playerId: String,
  val characterId: String,
  val nominationCount: Int,
  val nominatedByNames: List<String>,
)

data class NomineeVoteTally(
  val nominee: NomineeSummary,
  val realVotes: Int,
  val npcVotes: Int,
  val totalVotes: Int,
)

data class VotingTallyResult(
  val cycleId: String,
  val voteType: String,
  val rankedTallies: List<NomineeVoteTally>,
  val leadingEvictionCandidates: List<NomineeVoteTally>,
  val isTie: Boolean,
  val totalRealVotes: Int,
  val totalNpcVotes: Int,
)

object Phase10VotingLogic {
  fun tallyNominations(
    nominations: List<NominationBallot>,
    eligibleMembers: List<SeasonMember>,
  ): List<NominationTallyEntry> {
    val cleanMembers = eligibleMembers.filter {
      it.role != PlayerRole.ADMIN.name && it.status !in listOf("EVICTED", "BANNED", "REMOVED")
    }
    val byTarget = mutableMapOf<String, MutableList<String>>()
    nominations.forEach { ballot ->
      ballot.targetUids.distinct().forEach { targetUid ->
        byTarget.getOrPut(targetUid) { mutableListOf() }.add(ballot.voterName)
      }
    }
    return cleanMembers.map { member ->
      val voters = byTarget[member.userId].orEmpty()
      NominationTallyEntry(
        uid = member.userId,
        displayName = member.displayName,
        playerId = member.playerId,
        characterId = member.characterId,
        nominationCount = voters.size,
        nominatedByNames = voters,
      )
    }.sortedWith(compareByDescending<NominationTallyEntry> { it.nominationCount }.thenBy { it.displayName.lowercase() })
  }

  fun calculateVotingTally(
    cycle: VotingCycle,
    ballots: List<VotingBallot>,
  ): VotingTallyResult {
    val nominees = cycle.parsedNominees()
    val tallies = nominees.map { nominee ->
      val forNominee = ballots.filter { it.targetUid == nominee.uid }
      val realCount = forNominee.count { it.sourceType != "NPC" }
      val npcCount = if (cycle.npcVotesEnabled) forNominee.count { it.sourceType == "NPC" } else 0
      NomineeVoteTally(
        nominee = nominee,
        realVotes = realCount,
        npcVotes = npcCount,
        totalVotes = realCount + npcCount,
      )
    }

    val isSaveRule = cycle.voteType.equals("SAVE", ignoreCase = true)
    val ranked = if (isSaveRule) {
      tallies.sortedWith(compareBy<NomineeVoteTally> { it.totalVotes }.thenBy { it.realVotes })
    } else {
      tallies.sortedWith(compareByDescending<NomineeVoteTally> { it.totalVotes }.thenByDescending { it.realVotes })
    }

    val topScore = ranked.firstOrNull()?.totalVotes
    val leaders = if (topScore == null || ranked.isEmpty()) {
      emptyList()
    } else {
      ranked.filter { it.totalVotes == topScore }
    }
    val hasValidVotes = ballots.isNotEmpty()
    val isTie = hasValidVotes && leaders.size > 1

    return VotingTallyResult(
      cycleId = cycle.id,
      voteType = cycle.voteType,
      rankedTallies = ranked,
      leadingEvictionCandidates = leaders,
      isTie = isTie,
      totalRealVotes = tallies.sumOf { it.realVotes },
      totalNpcVotes = tallies.sumOf { it.npcVotes },
    )
  }

  fun canPlayerViewTotals(cycle: VotingCycle, isAdmin: Boolean): Boolean {
    if (isAdmin) return true
    return when (cycle.resultVisibility) {
      "LIVE_TOTALS" -> cycle.status in listOf("OPEN", "CLOSED", "EVICTED")
      "ADMIN_ONLY" -> false
      else -> cycle.status in listOf("CLOSED", "EVICTED")
    }
  }

  fun formatHindiEvictionAnnouncement(
    displayName: String,
    voteType: String,
    totalVotes: Int,
  ): String {
    return if (voteType.equals("SAVE", ignoreCase = true)) {
      "बिग बॉस का फैसला: सबसे कम सेव वोट्स ($totalVotes) मिलने के कारण $displayName को इस घर से बेघर किया जाता है!"
    } else {
      "बिग बॉस का फैसला: सर्वाधिक इविक्शन वोट्स ($totalVotes) मिलने के कारण $displayName को इस घर से बेघर किया जाता है!"
    }
  }

  fun formatHindiCancelledAnnouncement(title: String, reason: String?): String {
    val cleanReason = reason?.trim().orEmpty()
    return if (cleanReason.isNotBlank()) {
      "बिग बॉस घोषणा: '$title' की वोटिंग प्रक्रिया रद्द कर दी गई है। कारण: $cleanReason"
    } else {
      "बिग बॉस घोषणा: इस सप्ताह की '$title' वोटिंग प्रक्रिया रद्द कर दी गई है।"
    }
  }
}

@Keep
@IgnoreExtraProperties
data class AdminAnnouncement(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val message: String = "",
  val presetId: String? = null,
  val languageTag: String = "hi-IN", // hi-IN | en-IN
  val voiceGender: String = "FEMALE", // FEMALE | MALE
  val issuedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

enum class VoiceGender {
  FEMALE,
  MALE,
}

data class TtsStatusSnapshot(
  val ready: Boolean = false,
  val hindiAvailable: Boolean = false,
  val hasFemaleVoice: Boolean = false,
  val hasMaleVoice: Boolean = false,
  val statusMessage: String = "Initializing Indian Hindi TTS...",
)

data class RemotePlayerState(
  val uid: String = "",
  val playerId: String = "",
  val displayName: String = "",
  val characterId: String = "Player 01",
  val x: Double = 0.0,
  val y: Double = 0.0,
  val z: Double = 0.0,
  val rotY: Double = 0.0,
  val state: String = "idle",
  val expression: String = "neutral",
  val seatId: String? = null,
  val online: Boolean = false,
  val sessionId: String = "",
  val updatedAt: Long = 0L,
)

@Keep
@IgnoreExtraProperties
data class CctvRecordingRecord(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val cameraId: String = "",
  val cameraLabel: String = "",
  val storagePath: String = "",
  val downloadUrl: String = "",
  val dateKey: String = "",
  val startedAtMillis: Long = 0L,
  val durationMillis: Long = 0L,
  val sizeBytes: Long = 0L,
  val mimeType: String = "video/webm",
  val recordedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

data class RemoteDoorState(
  val doorId: String = "",
  val open: Boolean = false,
  val updatedBy: String = "",
  val updatedAt: Long = 0L,
)

data class RemoteSeatState(
  val seatId: String = "",
  val occupiedByUid: String? = null,
  val occupiedByName: String? = null,
  val updatedAt: Long = 0L,
)

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}
