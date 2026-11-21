package com.example.multiplayer

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AiControlMode(val label: String, val description: String) {
  MANUAL(
    label = "Manual",
    description = "AI suggests actions only when Admin asks; nothing runs automatically.",
  ),
  ASSIST(
    label = "Assist (Approve)",
    description = "AI detects tasks, low rations, & schedules and queues proposals for Admin approval.",
  ),
  AUTO(
    label = "Auto Pilot",
    description = "AI runs scheduled Hindi announcements, timer monitoring, and safe house upkeep automatically.",
  );

  companion object {
    fun fromRaw(raw: String?): AiControlMode =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ASSIST
  }
}

enum class AiActionType(val label: String) {
  HINDI_ANNOUNCEMENT("Hindi Announcement"),
  DAILY_TASK("Daily Task Dispatch"),
  KITCHEN_CHECK("Kitchen Ration Sync"),
  TIMER_CHECK("Task Timer Check"),
  HOUSE_ACTIVITY("House Activity Update");

  companion object {
    fun fromRaw(raw: String?): AiActionType =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: HINDI_ANNOUNCEMENT
  }
}

enum class AiApiProviderStatus(
  val key: String,
  val displayTitle: String,
  val isExternalAiConfigured: Boolean,
  val detailNotice: String,
) {
  RULE_ENGINE_NO_API_KEY(
    key = "RULE_ENGINE_NO_API_KEY",
    displayTitle = "Deterministic Rule-Based Engine (No OpenAI API Key Configured)",
    isExternalAiConfigured = false,
    detailNotice = "No OPENAI_API_KEY is configured in this project. Chair 1 runs a deterministic rule-based Hindi announcer, Phase 7 kitchen monitor, Phase 8 task scheduler, and offline Firebase Cloud Scheduler.",
  ),
  OPENAI_ENV_KEY(
    key = "OPENAI_ENV_KEY",
    displayTitle = "OpenAI ChatGPT API Active (Cloud Secret)",
    isExternalAiConfigured = true,
    detailNotice = "Cloud Functions detected OPENAI_API_KEY in Firebase Secrets for dynamic Hindi script generation.",
  );

  companion object {
    fun fromRaw(raw: String?): AiApiProviderStatus =
      entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: RULE_ENGINE_NO_API_KEY
  }
}

@Keep
@IgnoreExtraProperties
data class AiControlConfig(
  @DocumentId val id: String = "house_manager",
  val seasonId: String = "season_01",
  val enabled: Boolean = true,
  val mode: String = AiControlMode.ASSIST.name,
  val emergencyStopped: Boolean = false,
  val apiProvider: String = AiApiProviderStatus.RULE_ENGINE_NO_API_KEY.key,
  val autoHindiAnnouncements: Boolean = true,
  val autoKitchenMonitor: Boolean = true,
  val autoTaskCoordinator: Boolean = true,
  val autoTimerMonitor: Boolean = true,
  val tickIntervalMinutes: Long = 1L,
  val lastTickAtMillis: Long = 0L,
  val lastAdminSeenAtMillis: Long = 0L,
  val unseenOfflineCount: Long = 0L,
  val updatedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun parsedMode(): AiControlMode = AiControlMode.fromRaw(mode)
  fun parsedProvider(): AiApiProviderStatus = AiApiProviderStatus.fromRaw(apiProvider)
  fun isOperational(): Boolean = enabled && !emergencyStopped
}

@Keep
@IgnoreExtraProperties
data class AiScheduledEvent(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val actionType: String = AiActionType.HINDI_ANNOUNCEMENT.name,
  val title: String = "",
  val messageOrPayload: String = "",
  val targetTimeMillis: Long = 0L,
  val recurrenceMinutes: Long = 0L,
  val status: String = "PENDING", // PENDING | EXECUTED | PROPOSED | CANCELLED | PAUSED
  val modeOverride: String = "INHERIT", // INHERIT | MANUAL | ASSIST | AUTO
  val executionKey: String = "",
  val lastExecutedAtMillis: Long = 0L,
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AiApprovalProposal(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val category: String = "",
  val title: String = "",
  val summary: String = "",
  val hindiAnnouncement: String? = null,
  val payloadJson: String? = null,
  val status: String = "PENDING", // PENDING | APPROVED | REJECTED | EXECUTED | CANCELLED
  val executionKey: String = "",
  val reviewedBy: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AiActivityLog(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val category: String = "",
  val mode: String = AiControlMode.ASSIST.name,
  val source: String = "ADMIN_TICK", // CLOUD_SCHEDULER | ADMIN_TICK | OFFLINE_CATCHUP | EMERGENCY_STOP | APPROVAL | UI_CONTROL
  val title: String = "",
  val detail: String = "",
  val hindiAnnouncement: String? = null,
  val executionKey: String = "",
  val executedAtMillis: Long = 0L,
  val actorUid: String = "",
  val acknowledgedByAdmin: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

data class AiWhileAwaySummary(
  val missedExecutionsCount: Int = 0,
  val missedAnnouncementsCount: Int = 0,
  val missedProposalsCount: Int = 0,
  val lastSeenAtMillis: Long = 0L,
  val highlights: List<String> = emptyList(),
)

data class AiHindiPreset(
  val id: String,
  val title: String,
  val category: AiActionType,
  val hindiMessage: String,
)

object Phase9AiCatalog {
  const val DEFAULT_CONFIG_ID = "house_manager"

  val hindiPresets = listOf(
    AiHindiPreset(
      id = "ai_morning",
      title = "Morning Wake-Up Call",
      category = AiActionType.HINDI_ANNOUNCEMENT,
      hindiMessage = "सुप्रभात घरवालों! बिग बॉस चाहते हैं कि सभी सदस्य तुरंत लिविंग रूम में एकत्रित हों।",
    ),
    AiHindiPreset(
      id = "ai_kitchen_ration",
      title = "Kitchen Ration Discipline",
      category = AiActionType.KITCHEN_CHECK,
      hindiMessage = "बिग बॉस की चेतावनी: रसोई के राशन का समझदारी से उपयोग करें और खाना बर्बाद न करें।",
    ),
    AiHindiPreset(
      id = "ai_task_alert",
      title = "Daily Task Readiness",
      category = AiActionType.DAILY_TASK,
      hindiMessage = "ध्यान दीजिए घरवालों! आज का मुख्य कार्य कुछ ही क्षणों में शुरू होने जा रहा है।",
    ),
    AiHindiPreset(
      id = "ai_nomination_warn",
      title = "Nomination Room Call",
      category = AiActionType.HOUSE_ACTIVITY,
      hindiMessage = "सभी सदस्य लिविंग एरिया में बैठ जाएं। इस सप्ताह की नॉमिनेशन प्रक्रिया शुरू होने वाली है।",
    ),
    AiHindiPreset(
      id = "ai_lights_out",
      title = "Night Lights Off",
      category = AiActionType.HINDI_ANNOUNCEMENT,
      hindiMessage = "शुभ रात्रि घरवालों। अब सोने का समय हो गया है, कृपया बेडरूम की लाइट्स बंद रखें।",
    ),
  )
}
