package com.example.multiplayer

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TaskActivityType(
  val label: String,
  val defaultRoom: String,
  val shortHint: String,
) {
  MEMORY("Memory Challenge", "Living Atrium", "Recall sequence shown during briefing"),
  QUIZ("Quiz Competition", "Confession Room", "Answer the house trivia prompt"),
  PUZZLE("Puzzle Challenge", "Activity Room", "Solve the pattern/cipher riddle"),
  TREASURE_HUNT("Treasure Hunt", "Indoor Mansion", "Find hidden 3D golden relics inside house"),
  TEAM_COORDINATION("Team Coordination", "Dining Area", "Team consensus & synchronization code"),
  OBSERVATION("Observation Challenge", "Living Atrium", "Spot details inside the 3D Living Atrium"),
  OBSTACLE("Indoor Obstacle", "Activity Room & Poolside", "Complete 3D indoor glowing rings in order"),
  COOKING("Kitchen Culinary Duty", "Gourmet Kitchen", "Collect rations, prepare a virtual house meal & serve"),
  CHECKPOINT("Room Sequence Tour", "Indoor Mansion", "Reach indoor checkpoints in exact sequence");

  companion object {
    fun fromRaw(raw: String?): TaskActivityType =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: QUIZ
  }
}

@Keep
@IgnoreExtraProperties
data class HouseTask(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val title: String = "",
  val description: String = "",
  val instructions: String = "",
  val activityType: String = TaskActivityType.QUIZ.name,
  val assignmentMode: String = "INDIVIDUAL", // INDIVIDUAL | GROUP | TEAM | HOUSE
  val category: String = TaskCategory.DAILY.name,
  val difficulty: String = TaskDifficulty.MEDIUM.name,
  val isCaptaincy: Boolean = false,
  val status: String = "DRAFT", // DRAFT | AVAILABLE | ASSIGNED | IN_PROGRESS | ACTIVE | PAUSED | COMPLETED | FAILED | EXPIRED | CANCELLED
  val targetRoom: String = "Living Atrium",
  val durationSeconds: Long = 180L,
  val startTimeMillis: Long = 0L,
  val endTimeMillis: Long = 0L,
  val promptQuestion: String = "",
  val options: List<String> = emptyList(),
  val assignedUids: List<String> = emptyList(),
  val assignedTeamId: String? = null,
  val requiredCheckpoints: List<String> = emptyList(),
  val requiredRecipeId: String? = null,
  val progressPercent: Long = 0L,
  val rewardAchievement: String? = null,
  val maxPoints: Long = 100L,
  val winningUid: String? = null,
  val winningTeamId: String? = null,
  val winnerSummary: String? = null,
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class TaskAnswerSecret(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val taskId: String = "",
  val expectedAnswer: String = "",
  val updatedBy: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class TaskSubmission(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val taskId: String = "",
  val userId: String = "",
  val playerId: String = "",
  val displayName: String = "",
  val teamId: String? = null,
  val answerText: String = "",
  val elapsedSeconds: Long = 0L,
  val verificationStatus: String = "SUBMITTED", // SUBMITTED | VERIFIED | REJECTED
  val awardedScore: Long = 0L,
  val reviewedBy: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class HouseTeam(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val name: String = "",
  val colorHex: String = "#EAB954",
  val memberUids: List<String> = emptyList(),
  val score: Long = 0L,
  val winsCount: Long = 0L,
  val updatedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CaptaincyRecord(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val taskId: String? = null,
  val captainUid: String = "",
  val captainPlayerId: String = "",
  val captainDisplayName: String = "",
  val captainCharacterId: String = "Player 01",
  val title: String = "House Captain",
  val scoreAwarded: Long = 100L,
  val assignedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class HouseRule(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val ruleNumber: Long = 1L,
  val title: String = "",
  val description: String = "",
  val severity: String = "MEDIUM", // LOW | MEDIUM | HIGH | CRITICAL
  val active: Boolean = true,
  val updatedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class RuleViolation(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val ruleId: String = "",
  val ruleTitle: String = "",
  val targetUid: String = "",
  val targetPlayerId: String = "",
  val targetDisplayName: String = "",
  val warningLevel: String = "WARNING", // NOTICE | WARNING | FINAL_WARNING
  val note: String = "",
  val issuedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DailyScheduleSlot(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val orderIndex: Long = 1L,
  val activityKey: String = "",
  val title: String = "",
  val timeLabel: String = "",
  val location: String = "",
  val description: String = "",
  val isCurrent: Boolean = false,
  val updatedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class RealtimeTaskSnapshot(
  val taskId: String = "",
  val title: String = "",
  val activityType: String = "QUIZ",
  val assignmentMode: String = "INDIVIDUAL",
  val isCaptaincy: Boolean = false,
  val status: String = "DRAFT",
  val targetRoom: String = "Living Atrium",
  val startTimeMillis: Long = 0L,
  val endTimeMillis: Long = 0L,
  val winnerSummary: String? = null,
  val updatedAt: Long = 0L,
)

data class RealtimeCaptainSnapshot(
  val uid: String = "",
  val playerId: String = "",
  val displayName: String = "",
  val title: String = "House Captain",
  val updatedAt: Long = 0L,
)

data class RealtimeWarningSnapshot(
  val id: String = "",
  val ruleTitle: String = "",
  val warningLevel: String = "WARNING",
  val note: String = "",
  val updatedAt: Long = 0L,
)

data class TaskPresetTemplate(
  val activityType: TaskActivityType,
  val title: String,
  val description: String,
  val instructions: String,
  val targetRoom: String,
  val promptQuestion: String,
  val options: List<String>,
  val expectedAnswer: String,
  val defaultDurationSec: Long,
  val defaultMode: String,
  val defaultCategory: TaskCategory = TaskCategory.DAILY,
  val defaultDifficulty: TaskDifficulty = TaskDifficulty.MEDIUM,
  val requiredCheckpoints: List<String> = emptyList(),
  val requiredRecipeId: String? = null,
  val rewardAchievement: String = "TASK_FINISHER",
)

object Phase6Catalog {
  val taskPresets: List<TaskPresetTemplate> = listOf(
    TaskPresetTemplate(
      activityType = TaskActivityType.MEMORY,
      title = "Atrium Sequence Memory",
      description = "Memorize the exact 4-zone order inside Bigg Boss – The House.",
      instructions = "Remember the sequence: Living Tree -> Kitchen Island -> Botanical Garden -> Swimming Pool. Submit the exact code below before the clock hits 00:00.",
      targetRoom = "Living Atrium",
      promptQuestion = "Which code matches the official morning tour order?",
      options = listOf("TREE-KITCH-GARD-POOL", "POOL-GARD-KITCH-TREE", "BED-DIN-STORE-POOL", "TREE-BED-POOL-GARD"),
      expectedAnswer = "TREE-KITCH-GARD-POOL",
      defaultDurationSec = 180L,
      defaultMode = "INDIVIDUAL",
      defaultCategory = TaskCategory.DAILY,
      defaultDifficulty = TaskDifficulty.EASY,
      rewardAchievement = "TASK_FINISHER",
    ),
    TaskPresetTemplate(
      activityType = TaskActivityType.QUIZ,
      title = "Bigg Boss House Trivia Showdown",
      description = "Fastest accurate answer wins points for your team or Captaincy.",
      instructions = "Read the house architecture question carefully. You can submit ONLY ONCE per task.",
      targetRoom = "Living Atrium",
      promptQuestion = "How many indoor rooms & zones make up Bigg Boss – The House (including the restricted Admin wing)?",
      options = listOf("10", "13", "15", "18"),
      expectedAnswer = "13",
      defaultDurationSec = 150L,
      defaultMode = "INDIVIDUAL",
      defaultCategory = TaskCategory.DAILY,
      defaultDifficulty = TaskDifficulty.EASY,
      rewardAchievement = "TASK_FINISHER",
    ),
    TaskPresetTemplate(
      activityType = TaskActivityType.PUZZLE,
      title = "Secret Vault Cipher",
      description = "Decode the numeric lock sequence from clues around the house.",
      instructions = "Combine: [Max House Capacity (15)] + [Centerpiece Vine Trees (1)] + [Private Bathrooms Excluded from CCTV (1)] -> Enter as 15-1-1 or pick matching key.",
      targetRoom = "Activity Room",
      promptQuestion = "Select the decrypted Vault Combination:",
      options = listOf("15-1-1", "15-6-13", "13-1-15", "01-15-06"),
      expectedAnswer = "15-1-1",
      defaultDurationSec = 240L,
      defaultMode = "TEAM",
      defaultCategory = TaskCategory.TEAM,
      defaultDifficulty = TaskDifficulty.HARD,
      rewardAchievement = "TEAM_MVP",
    ),
    TaskPresetTemplate(
      activityType = TaskActivityType.TREASURE_HUNT,
      title = "Golden Eye Relic Hunt",
      description = "Explore indoor rooms in 3D to collect 3 Golden Eye Clues.",
      instructions = "Walk inside the 3D house (Living Atrium, Gourmet Kitchen, Botanical Garden) and walk over the glowing golden relics. Once 3/3 are collected, submit your Hunt Code!",
      targetRoom = "Living Atrium, Kitchen & Garden",
      promptQuestion = "Walk & collect all 3 indoor 3D relics, then submit verified completion:",
      options = emptyList(),
      expectedAnswer = "RELICS_3_OF_3",
      defaultDurationSec = 300L,
      defaultMode = "INDIVIDUAL",
      defaultCategory = TaskCategory.INDIVIDUAL,
      defaultDifficulty = TaskDifficulty.MEDIUM,
      requiredCheckpoints = listOf("relic_atrium", "relic_kitchen", "relic_garden"),
      rewardAchievement = "TASK_FINISHER",
    ),
    TaskPresetTemplate(
      activityType = TaskActivityType.TEAM_COORDINATION,
      title = "Dining Table Sync Strategy",
      description = "All teammates submit the agreed team synchronization passkey.",
      instructions = "Coordinate with your assigned team inside the Dining Area or Living Atrium and submit your team's unified synchronization signal.",
      targetRoom = "Dining Area",
      promptQuestion = "Choose the synchronized team signal confirmed by your squad:",
      options = listOf("ALPHA-SYNC-100", "BETA-WAVE-200", "GAMMA-HOLD-300", "DELTA-RUN-400"),
      expectedAnswer = "ALPHA-SYNC-100",
      defaultDurationSec = 240L,
      defaultMode = "TEAM",
      defaultCategory = TaskCategory.TEAM,
      defaultDifficulty = TaskDifficulty.MEDIUM,
      rewardAchievement = "TEAM_MVP",
    ),
    TaskPresetTemplate(
      activityType = TaskActivityType.OBSERVATION,
      title = "Center Vine Tree Inspection",
      description = "Observe the Central Living Atrium chandelier & vine tree details.",
      instructions = "Walk to the center of the Living Atrium, inspect the twisted vines around the luxury centerpiece tree, and answer accurately.",
      targetRoom = "Living Atrium",
      promptQuestion = "Which natural centerpiece anchors the heart of the Living Atrium?",
      options = listOf("Spiral Vine-Wrapped Luxury Tree", "Stone Water Fountain", "Golden Piano Stage", "Crystal Staircase"),
      expectedAnswer = "Spiral Vine-Wrapped Luxury Tree",
      defaultDurationSec = 180L,
      defaultMode = "INDIVIDUAL",
      defaultCategory = TaskCategory.HOUSE,
      defaultDifficulty = TaskDifficulty.EASY,
      rewardAchievement = "TASK_FINISHER",
    ),
    TaskPresetTemplate(
      activityType = TaskActivityType.OBSTACLE,
      title = "Indoor Checkpoint Sprint",
      description = "Pass through all 3 glowing 3D obstacle rings across the house.",
      instructions = "Sprint through Checkpoint Ring 1 (Activity Room), Ring 2 (Botanical Garden), and Ring 3 (Pool Deck) in 3D, then submit your verified obstacle clear!",
      targetRoom = "Activity Room & Poolside",
      promptQuestion = "Clear all 3 glowing 3D obstacle rings in the house and submit your run:",
      options = emptyList(),
      expectedAnswer = "RINGS_3_OF_3",
      defaultDurationSec = 300L,
      defaultMode = "INDIVIDUAL",
      defaultCategory = TaskCategory.CAPTAIN,
      defaultDifficulty = TaskDifficulty.HARD,
      requiredCheckpoints = listOf("ring_activity", "ring_garden", "ring_pool"),
      rewardAchievement = "SPEED_RUNNER",
    ),
    TaskPresetTemplate(
      activityType = TaskActivityType.COOKING,
      title = "Grand Gourmet Feast Duty",
      description = "Gather kitchen rations, prepare Golden Atrium Platter, and serve housemates.",
      instructions = "Visit the Gourmet Kitchen Fridge & Stove in 3D, collect required rations, cook the Golden Atrium Platter, and verify completion.",
      targetRoom = "Gourmet Kitchen",
      promptQuestion = "Cook & serve the Golden Atrium Platter in the Gourmet Kitchen:",
      options = emptyList(),
      expectedAnswer = "MEAL_SERVED",
      defaultDurationSec = 360L,
      defaultMode = "GROUP",
      defaultCategory = TaskCategory.GROUP,
      defaultDifficulty = TaskDifficulty.MEDIUM,
      requiredCheckpoints = listOf("kitchen_collect", "kitchen_cook", "kitchen_serve"),
      requiredRecipeId = "golden_atrium_platter",
      rewardAchievement = "MASTER_CHEF",
    ),
    TaskPresetTemplate(
      activityType = TaskActivityType.CHECKPOINT,
      title = "Mansion Patrol Circuit",
      description = "Reach all 4 glowing room beacons across Bedroom, Kitchen, Garden & Lounge.",
      instructions = "Walk through all 4 glowing 3D patrol beacons around the house before the weekly countdown ends.",
      targetRoom = "Indoor Mansion",
      promptQuestion = "Reach all 4 indoor room checkpoints in 3D and submit:",
      options = emptyList(),
      expectedAnswer = "ZONES_4_OF_4",
      defaultDurationSec = 360L,
      defaultMode = "HOUSE",
      defaultCategory = TaskCategory.WEEKLY,
      defaultDifficulty = TaskDifficulty.EXTREME,
      requiredCheckpoints = listOf("zone_bedroom", "zone_kitchen", "zone_garden", "zone_lounge"),
      rewardAchievement = "SPEED_RUNNER",
    ),
  )

  fun presetFor(type: TaskActivityType): TaskPresetTemplate =
    taskPresets.firstOrNull { it.activityType == type } ?: taskPresets.first()

  data class DefaultRuleTemplate(
    val id: String,
    val ruleNumber: Long,
    val title: String,
    val description: String,
    val severity: String,
  )

  val defaultHouseRules: List<DefaultRuleTemplate> = listOf(
    DefaultRuleTemplate("rule_1", 1L, "Respect All Housemates", "Maintain respectful conduct during tasks, gatherings, and nominations.", "HIGH"),
    DefaultRuleTemplate("rule_2", 2L, "Restricted Admin Wing", "Ordinary players must never attempt to tamper with or enter the Admin Control Room.", "CRITICAL"),
    DefaultRuleTemplate("rule_3", 3L, "One Submission Per Task", "Duplicate, forged, or outside-help task submissions are disqualified immediately.", "HIGH"),
    DefaultRuleTemplate("rule_4", 4L, "Mandatory Morning & Evening Gathering", "All active housemates must assemble in the Living Atrium on Admin announcements.", "MEDIUM"),
    DefaultRuleTemplate("rule_5", 5L, "Kitchen & Dining Cleanliness", "Keep the Gourmet Kitchen, Dining Area, and Smoking Lounge orderly after use.", "LOW"),
  )

  data class DefaultScheduleTemplate(
    val id: String,
    val orderIndex: Long,
    val activityKey: String,
    val title: String,
    val timeLabel: String,
    val location: String,
    val description: String,
  )

  val defaultDailySchedule: List<DefaultScheduleTemplate> = listOf(
    DefaultScheduleTemplate("slot_1", 1L, "MORNING_WAKEUP", "Morning Wake-Up", "08:00 AM", "15-Player Bedroom", "House lights brighten and wake-up track plays across the mansion."),
    DefaultScheduleTemplate("slot_2", 2L, "MORNING_GATHERING", "Morning Gathering", "08:45 AM", "Living Atrium", "All housemates assemble around the Center Vine Tree for House Rules & briefing."),
    DefaultScheduleTemplate("slot_3", 3L, "BREAKFAST", "Breakfast", "09:30 AM", "Dining Area & Kitchen", "Housemates prepare morning meals and coordinate daily kitchen duties."),
    DefaultScheduleTemplate("slot_4", 4L, "DAILY_TASK", "Daily Task", "11:30 AM", "Activity Room / Mansion", "Main Daily or Captaincy competition runs with synchronized countdown timer."),
    DefaultScheduleTemplate("slot_5", 5L, "LUNCH", "Lunch", "01:30 PM", "Dining Area", "Midday meal break and team debrief after the main house task."),
    DefaultScheduleTemplate("slot_6", 6L, "EVENING_GATHERING", "Evening Gathering", "05:30 PM", "Living Atrium & Garden", "Captaincy review, task results announcement, and voting prep."),
    DefaultScheduleTemplate("slot_7", 7L, "DINNER", "Dinner", "08:30 PM", "Dining Area", "Evening house dinner together before night lock-in."),
    DefaultScheduleTemplate("slot_8", 8L, "NIGHT_SCHEDULE", "Night Schedule", "11:00 PM", "15-Player Bedroom", "Night ambient lighting mode begins; lights dim inside the common bedroom."),
  )
}

object Phase6Logic {
  fun remainingSeconds(endTimeMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long {
    if (endTimeMillis <= 0L) return 0L
    return ((endTimeMillis - nowMillis).coerceAtLeast(0L)) / 1000L
  }

  fun formatCountdown(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val mins = safe / 60L
    val secs = safe % 60L
    return "%02d:%02d".format(mins, secs)
  }

  fun isAnswerMatch(submitted: String, expected: String?): Boolean {
    val cleanSub = submitted.trim()
    val cleanExp = expected?.trim().orEmpty()
    if (cleanExp.isEmpty()) return true
    return cleanSub.equals(cleanExp, ignoreCase = true)
  }

  fun pickAutoWinner(
    submissions: List<TaskSubmission>,
    expectedAnswer: String?,
  ): TaskSubmission? {
    val eligible = submissions.filter { sub ->
      sub.verificationStatus == "VERIFIED" ||
        (sub.verificationStatus == "SUBMITTED" && isAnswerMatch(sub.answerText, expectedAnswer))
    }
    return eligible.sortedWith(
      compareByDescending<TaskSubmission> { it.awardedScore }
        .thenBy { it.elapsedSeconds }
    ).firstOrNull()
  }
}
