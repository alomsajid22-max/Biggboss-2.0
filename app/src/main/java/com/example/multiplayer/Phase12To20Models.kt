package com.example.multiplayer

import org.json.JSONArray
import org.json.JSONObject

data class WardrobeAppearance(
  val topHex: String = "#1F7A8C",
  val pantsHex: String = "#1E2433",
  val outfitStyle: String = "bomber",
  val shoesHex: String = "#F8FAFC",
  val accentHex: String = "#EAB954",
  val hairStyle: String = "ponytail",
  val hairColorHex: String = "#261C17",
  val skinToneHex: String = "#F1C29B",
  val accessory: String = "none",
  val glasses: Boolean = false,
) {
  fun toJsonObject(): JSONObject = JSONObject().apply {
    put("top", topHex)
    put("pants", pantsHex)
    put("outfit", outfitStyle)
    put("shoes", shoesHex)
    put("accent", accentHex)
    put("hairStyle", hairStyle)
    put("hairColor", hairColorHex)
    put("skin", skinToneHex)
    put("accessory", accessory)
    put("glasses", glasses)
  }

  companion object {
    val HAIR_STYLES = listOf(
      "ponytail", "topBun", "bobCut", "curlyCrown",
      "spiky", "sidePart", "undercut", "shortCrop"
    )
    val OUTFIT_STYLES = listOf("bomber", "hoodie", "blazer", "vest")
    val ACCESSORIES = listOf("none", "glasses", "earrings", "headphones")

    val PRESETS: List<Pair<String, WardrobeAppearance>> = listOf(
      "Studio Bomber" to WardrobeAppearance(
        topHex = "#1F7A8C", pantsHex = "#1E2433", outfitStyle = "bomber",
        shoesHex = "#F8FAFC", accentHex = "#EAB954", hairStyle = "ponytail",
        hairColorHex = "#261C17", skinToneHex = "#F1C29B", accessory = "earrings", glasses = false,
      ),
      "Captain Blazer" to WardrobeAppearance(
        topHex = "#D97706", pantsHex = "#18181B", outfitStyle = "blazer",
        shoesHex = "#FEF3C7", accentHex = "#FFFFFF", hairStyle = "curlyCrown",
        hairColorHex = "#1C130F", skinToneHex = "#8D5524", accessory = "glasses", glasses = true,
      ),
      "Lounge Hoodie" to WardrobeAppearance(
        topHex = "#B91C1C", pantsHex = "#0F172A", outfitStyle = "hoodie",
        shoesHex = "#EF4444", accentHex = "#FEF08A", hairStyle = "spiky",
        hairColorHex = "#111111", skinToneHex = "#DCA47F", accessory = "headphones", glasses = false,
      ),
      "Royal Vest" to WardrobeAppearance(
        topHex = "#7C3AED", pantsHex = "#1E1B4B", outfitStyle = "vest",
        shoesHex = "#E2E8F0", accentHex = "#67E8F9", hairStyle = "topBun",
        hairColorHex = "#E2E8F0", skinToneHex = "#F5CBA7", accessory = "none", glasses = false,
      ),
    )

    fun fromJson(obj: JSONObject?): WardrobeAppearance {
      if (obj == null) return WardrobeAppearance()
      return WardrobeAppearance(
        topHex = obj.optString("top", "#1F7A8C").ifBlank { "#1F7A8C" },
        pantsHex = obj.optString("pants", "#1E2433").ifBlank { "#1E2433" },
        outfitStyle = obj.optString("outfit", "bomber").ifBlank { "bomber" },
        shoesHex = obj.optString("shoes", "#F8FAFC").ifBlank { "#F8FAFC" },
        accentHex = obj.optString("accent", "#EAB954").ifBlank { "#EAB954" },
        hairStyle = obj.optString("hairStyle", "ponytail").ifBlank { "ponytail" },
        hairColorHex = obj.optString("hairColor", "#261C17").ifBlank { "#261C17" },
        skinToneHex = obj.optString("skin", "#F1C29B").ifBlank { "#F1C29B" },
        accessory = obj.optString("accessory", "none").ifBlank { "none" },
        glasses = obj.optBoolean("glasses", false),
      )
    }
  }
}

data class HouseClockState(
  val hour: Int = 10,
  val minute: Int = 30,
  val dayNumber: Int = 1,
  val timeMode: String = "day", // "day", "dusk", "night"
  val autoCycleEnabled: Boolean = true,
  val weather: String = "CLEAR", // "CLEAR", "RAIN", "GOLDEN", "NIGHT_MIST"
  val partyMode: Boolean = false,
  val emergencySirenActive: Boolean = false,
) {
  fun formattedClock(): String = "Day $dayNumber • %02d:%02d".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
}

enum class CelebrationType(val label: String) {
  BIRTHDAY("Birthday Bash"),
  HOUSE_CELEBRATION("House Celebration"),
  FESTIVAL("Grand Festival"),
  FINALE("Grand Finale"),
}

data class HouseCelebrationEvent(
  val id: String = "",
  val type: String = CelebrationType.HOUSE_CELEBRATION.name,
  val title: String = "",
  val description: String = "",
  val scheduledTimeMillis: Long = 0L,
  val status: String = "SCHEDULED", // SCHEDULED, ACTIVE, COMPLETED
  val triggeredBy: String = "BIGGBOSS",
  val createdAtMillis: Long = System.currentTimeMillis(),
)

data class HouseChatMessage(
  val id: String = "",
  val senderUid: String = "",
  val senderName: String = "",
  val senderRole: String = "PLAYER",
  val recipientUid: String? = null, // null = Public house chat; non-null = Private 1:1 DM (never spoken via TTS)
  val recipientName: String? = null,
  val text: String = "",
  val emote: String? = null,
  val timestampMillis: Long = System.currentTimeMillis(),
) {
  val isPrivateDm: Boolean get() = !recipientUid.isNullOrBlank()
}

data class TtsPlaybackSettings(
  val enabled: Boolean = true,
  val languageTag: String = "hi-IN", // "hi-IN" or "en-IN"
  val volume: Float = 1.0f,
  val speechRate: Float = 0.95f,
  val welcomePlayed: Boolean = false,
)

data class CameraPreferences(
  val mode: String = "THIRD_PERSON", // "THIRD_PERSON", "FIRST_PERSON", "FREE"
  val distance: Float = 4.6f,
  val sensitivity: Float = 1.0f,
  val smoothness: Float = 0.72f,
  val freeCamAllowed: Boolean = true,
) {
  fun toJsonObject(): JSONObject = JSONObject().apply {
    put("mode", mode)
    put("distance", distance.toDouble())
    put("sensitivity", sensitivity.toDouble())
    put("smoothness", smoothness.toDouble())
    put("freeCamAllowed", freeCamAllowed)
  }
}

data class PlayerModerationFlags(
  val userId: String = "",
  val muted: Boolean = false,
  val restrictedFromTasks: Boolean = false,
  val currentRoom: String = "Living Atrium",
)

data class PlayerExtendedProfile(
  val userId: String,
  val displayName: String,
  val playerId: String,
  val characterId: String,
  val rank: Int,
  val pointsTotal: Long,
  val tasksCompleted: Int,
  val tasksWon: Int,
  val captainCount: Int,
  val badges: List<String>,
  val activityHistory: List<String>,
)

data class FinaleCeremonyState(
  val active: Boolean = false,
  val winnerUid: String = "",
  val winnerName: String = "",
  val runnerUpName: String = "",
  val trophyTitle: String = "BIGG BOSS – THE HOUSE SEASON 01 CHAMPION",
  val declaredAtMillis: Long = 0L,
)

data class SecretTaskAssignment(
  val id: String = "",
  val targetUid: String = "",
  val targetName: String = "",
  val title: String = "",
  val secretInstructions: String = "",
  val rewardPoints: Long = 75L,
  val status: String = "ACTIVE", // ACTIVE, COMPLETED, EXPIRED
  val createdAtMillis: Long = System.currentTimeMillis(),
)

// --- PHASE 20: MAIN ADMIN PHONE, BIGGGRAM (NPC-ONLY), NPC VOTING & 24 CCTV CAMERAS ---

data class BiggGramNpcProfile(
  val npcId: String,
  val name: String,
  val characterId: String,
  val handle: String,
  val archetype: String,
  val bio: String,
  val avatarColorHex: String,
  val personalityStyle: String,
)

data class BiggGramComment(
  val id: String,
  val authorHandle: String,
  val authorName: String,
  val text: String,
  val timestampMillis: Long = System.currentTimeMillis(),
)

data class BiggGramPost(
  val id: String,
  val authorHandle: String,
  val authorName: String,
  val authorAvatarHex: String,
  val caption: String,
  val category: String = "LIFESTYLE", // LIFESTYLE, NOMINATION_POLL, FINALE, DRAMA
  val cycleId: String? = null,
  val nomineeUids: List<String> = emptyList(),
  val likes: Int = 0,
  val likedByAdmin: Boolean = false,
  val comments: List<BiggGramComment> = emptyList(),
  val timestampMillis: Long = System.currentTimeMillis(),
)

data class BiggGramStory(
  val id: String,
  val npcId: String,
  val handle: String,
  val displayName: String,
  val avatarHex: String,
  val headline: String,
  val caption: String,
  val timestampMillis: Long = System.currentTimeMillis(),
  val viewed: Boolean = false,
)

data class NpcVoteRecord(
  val id: String,
  val cycleId: String,
  val npcId: String,
  val npcName: String,
  val npcHandle: String,
  val targetUid: String,
  val targetName: String,
  val postId: String = "",
  val castAtMillis: Long = System.currentTimeMillis(),
)

data class NpcVotingSummaryRow(
  val nominee: NomineeSummary,
  val voteCount: Int,
  val percentage: Double,
)

data class NpcVotingOutcome(
  val cycleId: String,
  val totalValidNpcVotes: Int,
  val rows: List<NpcVotingSummaryRow>,
  val highestNominee: NpcVotingSummaryRow?,
  val lowestNominee: NpcVotingSummaryRow?,
  val isTieAtTop: Boolean,
)

data class AdminPhoneNotification(
  val id: String,
  val title: String,
  val body: String,
  val category: String = "SYSTEM",
  val timestampMillis: Long = System.currentTimeMillis(),
  val read: Boolean = false,
)

data class CctvCameraMeta(
  val id: String,
  val code: String,
  val label: String,
  val roomName: String,
  val roomId: String,
  val online: Boolean = true,
)

object Phase12To20JsonHelper {
  fun encodeStringList(items: List<String>): JSONArray = JSONArray().apply {
    items.forEach { put(it) }
  }

  fun decodeStringList(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    val out = ArrayList<String>(arr.length())
    for (i in 0 until arr.length()) {
      val s = arr.optString(i, "")
      if (s.isNotBlank()) out.add(s)
    }
    return out
  }
}
