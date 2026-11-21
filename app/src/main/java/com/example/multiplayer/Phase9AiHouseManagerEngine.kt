package com.example.multiplayer

import kotlin.math.max

data class AiKitchenActionPlan(
  val executionKey: String,
  val itemId: String,
  val itemName: String,
  val addQuantity: Long,
  val hindiAnnouncement: String,
)

data class AiTaskSuggestionPlan(
  val executionKey: String,
  val title: String,
  val description: String,
  val instructions: String,
  val activityType: String,
  val category: String,
  val difficulty: String,
  val targetRoom: String,
  val durationSeconds: Long,
  val maxPoints: Long,
  val requiredCheckpoints: List<String>,
  val assignedUids: List<String>,
  val hindiAnnouncement: String,
)

data class AiTimerExpiryPlan(
  val executionKey: String,
  val taskId: String,
  val taskTitle: String,
  val hindiAnnouncement: String,
)

data class AiScheduleTransition(
  val scheduleId: String,
  val nextStatus: String,
  val nextTargetTimeMillis: Long,
  val lastExecutedAtMillis: Long,
)

data class AiTickEvaluationResult(
  val skippedReason: String? = null,
  val directAnnouncements: List<Pair<String, String>> = emptyList(), // executionKey to hindiText
  val directKitchenReplenishes: List<AiKitchenActionPlan> = emptyList(),
  val directTaskSuggestions: List<AiTaskSuggestionPlan> = emptyList(),
  val directExpiredTasks: List<AiTimerExpiryPlan> = emptyList(),
  val proposalsToCreate: List<AiApprovalProposal> = emptyList(),
  val scheduleTransitions: List<AiScheduleTransition> = emptyList(),
  val logsToAppend: List<AiActivityLog> = emptyList(),
)

/**
 * Pure deterministic rule-based AI House Manager engine for Phase 9.
 * Operates reliably without requiring an external OpenAI/ChatGPT API key and uses
 * deterministic idempotency executionKeys so offline catch-ups never double-fire.
 */
object Phase9AiHouseManagerEngine {

  fun buildSlotExecutionKey(
    baseKey: String,
    recurrenceMinutes: Long,
    nowMillis: Long,
  ): String {
    val cleanBase = baseKey.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(96)
    return if (recurrenceMinutes > 0L) {
      val slot = nowMillis / max(60_000L, recurrenceMinutes * 60_000L)
      "${cleanBase}_slot_$slot"
    } else {
      cleanBase
    }
  }

  fun evaluateHouseTick(
    config: AiControlConfig,
    schedules: List<AiScheduledEvent>,
    existingExecutionKeys: Set<String>,
    rations: List<RationItem>,
    tasks: List<HouseTask>,
    eligibleMembers: List<SeasonMember>,
    seasonId: String,
    actorUid: String,
    source: String, // ADMIN_TICK | OFFLINE_CATCHUP | CLOUD_SCHEDULER
    nowMillis: Long,
  ): AiTickEvaluationResult {
    if (!config.enabled || config.emergencyStopped) {
      return AiTickEvaluationResult(skippedReason = "AI is OFF or Emergency Stopped.")
    }
    val globalMode = config.parsedMode()
    if (globalMode == AiControlMode.MANUAL) {
      return AiTickEvaluationResult(skippedReason = "AI is in MANUAL mode.")
    }

    val announcements = mutableListOf<Pair<String, String>>()
    val kitchenReplenishes = mutableListOf<AiKitchenActionPlan>()
    val taskSuggestions = mutableListOf<AiTaskSuggestionPlan>()
    val expiredTasks = mutableListOf<AiTimerExpiryPlan>()
    val proposals = mutableListOf<AiApprovalProposal>()
    val scheduleTransitions = mutableListOf<AiScheduleTransition>()
    val logs = mutableListOf<AiActivityLog>()
    val seenKeys = existingExecutionKeys.toMutableSet()

    // 1. Evaluate pending due schedules (supports missed offline catch-up)
    schedules
      .filter { it.status == "PENDING" && it.targetTimeMillis in 1..nowMillis }
      .forEach { sched ->
        val effectiveMode = if (sched.modeOverride != "INHERIT") {
          AiControlMode.fromRaw(sched.modeOverride)
        } else {
          globalMode
        }
        if (effectiveMode == AiControlMode.MANUAL) return@forEach

        val execKey = buildSlotExecutionKey(
          baseKey = sched.executionKey.ifBlank { "sched_${sched.id}" },
          recurrenceMinutes = sched.recurrenceMinutes,
          nowMillis = nowMillis,
        )
        if (!seenKeys.add(execKey)) return@forEach

        val hindiText = sched.messageOrPayload.ifBlank {
          "बिग बॉस घोषणा: ${sched.title} अब शुरू हो रहा है।"
        }

        if (effectiveMode == AiControlMode.ASSIST) {
          proposals += AiApprovalProposal(
            id = "prop_$execKey".take(120),
            seasonId = seasonId,
            category = sched.actionType,
            title = sched.title,
            summary = "Scheduled ${AiActionType.fromRaw(sched.actionType).label} is due and awaiting approval.",
            hindiAnnouncement = hindiText,
            payloadJson = """{"scheduleId":"${sched.id}","actionType":"${sched.actionType}","message":"${hindiText.replace("\"", "'")}"}""",
            status = "PENDING",
            executionKey = execKey,
          )
          logs += AiActivityLog(
            id = "log_$execKey".take(120),
            seasonId = seasonId,
            category = sched.actionType,
            mode = AiControlMode.ASSIST.name,
            source = source,
            title = "Proposal Queued: ${sched.title}".take(100),
            detail = "Queued scheduled action for Admin approval (${config.parsedProvider().name}).",
            hindiAnnouncement = hindiText,
            executionKey = execKey,
            executedAtMillis = nowMillis,
            actorUid = actorUid,
            acknowledgedByAdmin = source != "OFFLINE_CATCHUP" && source != "CLOUD_SCHEDULER",
          )
        } else {
          // AUTO mode
          when (AiActionType.fromRaw(sched.actionType)) {
            AiActionType.HINDI_ANNOUNCEMENT,
            AiActionType.HOUSE_ACTIVITY,
            AiActionType.KITCHEN_CHECK -> {
              announcements += execKey to hindiText
            }
            AiActionType.DAILY_TASK -> {
              val preset = pickBalancedPreset(tasks)
              val assigned = eligibleMembers.filter { it.role != "ADMIN" && it.status in listOf("IN_HOUSE", "LOBBY") }
                .take(4)
                .map { it.userId }
              taskSuggestions += AiTaskSuggestionPlan(
                executionKey = execKey,
                title = sched.title.ifBlank { preset.title },
                description = preset.description,
                instructions = preset.instructions,
                activityType = preset.activityType.name,
                category = preset.defaultCategory.name,
                difficulty = preset.defaultDifficulty.name,
                targetRoom = preset.targetRoom,
                durationSeconds = preset.defaultDurationSec,
                maxPoints = Phase8Logic.calculateRewardPoints(100L, preset.defaultDifficulty),
                requiredCheckpoints = preset.requiredCheckpoints,
                assignedUids = assigned,
                hindiAnnouncement = hindiText,
              )
              announcements += "${execKey}_ann" to hindiText
            }
            AiActionType.TIMER_CHECK -> {
              // handled below alongside active task expiry scan
              announcements += execKey to hindiText
            }
          }
          logs += AiActivityLog(
            id = "log_$execKey".take(120),
            seasonId = seasonId,
            category = sched.actionType,
            mode = AiControlMode.AUTO.name,
            source = source,
            title = "Auto-Executed: ${sched.title}".take(100),
            detail = hindiText.take(480),
            hindiAnnouncement = hindiText,
            executionKey = execKey,
            executedAtMillis = nowMillis,
            actorUid = actorUid,
            acknowledgedByAdmin = source != "OFFLINE_CATCHUP" && source != "CLOUD_SCHEDULER",
          )
        }

        val nextStatus = if (sched.recurrenceMinutes > 0L) "PENDING" else if (effectiveMode == AiControlMode.ASSIST) "PROPOSED" else "EXECUTED"
        val nextTarget = if (sched.recurrenceMinutes > 0L) {
          nowMillis + sched.recurrenceMinutes * 60_000L
        } else {
          sched.targetTimeMillis
        }
        scheduleTransitions += AiScheduleTransition(
          scheduleId = sched.id,
          nextStatus = nextStatus,
          nextTargetTimeMillis = nextTarget,
          lastExecutedAtMillis = nowMillis,
        )
      }

    // 2. Phase 8 Task Timer Monitoring (auto-detect expired tasks)
    if (config.autoTimerMonitor) {
      tasks.filter { task ->
        task.status in listOf("ACTIVE", "IN_PROGRESS", "ASSIGNED") &&
          task.endTimeMillis > 0L &&
          nowMillis >= task.endTimeMillis
      }.forEach { expired ->
        val execKey = "timer_exp_${expired.id}_${expired.endTimeMillis / 60000L}"
        if (!seenKeys.add(execKey)) return@forEach

        val hindiText = "समय समाप्त! कार्य '${expired.title}' का समय पूरा हो चुका है।"
        if (globalMode == AiControlMode.ASSIST) {
          proposals += AiApprovalProposal(
            id = "prop_$execKey".take(120),
            seasonId = seasonId,
            category = AiActionType.TIMER_CHECK.name,
            title = "Close Expired Task: ${expired.title}".take(100),
            summary = "Task '${expired.title}' countdown reached 00:00. Approve to mark COMPLETED & announce.",
            hindiAnnouncement = hindiText,
            payloadJson = """{"taskId":"${expired.id}","action":"EXPIRE_TASK"}""",
            status = "PENDING",
            executionKey = execKey,
          )
        } else {
          expiredTasks += AiTimerExpiryPlan(
            executionKey = execKey,
            taskId = expired.id,
            taskTitle = expired.title,
            hindiAnnouncement = hindiText,
          )
          if (config.autoHindiAnnouncements) {
            announcements += "${execKey}_ann" to hindiText
          }
          logs += AiActivityLog(
            id = "log_$execKey".take(120),
            seasonId = seasonId,
            category = AiActionType.TIMER_CHECK.name,
            mode = AiControlMode.AUTO.name,
            source = source,
            title = "Timer Closed: ${expired.title}".take(100),
            detail = "AI closed expired task '${expired.title}' and notified housemates.",
            hindiAnnouncement = hindiText,
            executionKey = execKey,
            executedAtMillis = nowMillis,
            actorUid = actorUid,
            acknowledgedByAdmin = source != "OFFLINE_CATCHUP" && source != "CLOUD_SCHEDULER",
          )
        }
      }
    }

    // 3. Phase 7 Kitchen Ration Coordination (detect depleted/critical stock items)
    if (config.autoKitchenMonitor) {
      val slotBucket = nowMillis / (15 * 60_000L) // max once per 15 minutes per item
      rations.filter { it.permitted && it.quantity <= it.minThreshold }.take(2).forEach { lowItem ->
        val execKey = "kitchen_low_${lowItem.itemId}_$slotBucket"
        if (!seenKeys.add(execKey)) return@forEach

        val refillAmount = max(5L, (lowItem.maxQuantity - lowItem.quantity).coerceAtMost(10L))
        val hindiText = "रसोई सूचना: '${lowItem.name}' का राशन कम हो गया है। कृपया रसोई में सहयोग करें।"
        if (globalMode == AiControlMode.ASSIST) {
          proposals += AiApprovalProposal(
            id = "prop_$execKey".take(120),
            seasonId = seasonId,
            category = AiActionType.KITCHEN_CHECK.name,
            title = "Restock ${lowItem.name} (+$refillAmount)".take(100),
            summary = "${lowItem.name} dropped to ${lowItem.quantity}/${lowItem.maxQuantity} (min threshold ${lowItem.minThreshold}).",
            hindiAnnouncement = hindiText,
            payloadJson = """{"itemId":"${lowItem.itemId}","addQuantity":$refillAmount,"action":"REPLENISH_RATION"}""",
            status = "PENDING",
            executionKey = execKey,
          )
        } else {
          kitchenReplenishes += AiKitchenActionPlan(
            executionKey = execKey,
            itemId = lowItem.itemId,
            itemName = lowItem.name,
            addQuantity = refillAmount,
            hindiAnnouncement = hindiText,
          )
          logs += AiActivityLog(
            id = "log_$execKey".take(120),
            seasonId = seasonId,
            category = AiActionType.KITCHEN_CHECK.name,
            mode = AiControlMode.AUTO.name,
            source = source,
            title = "Auto-Replenished ${lowItem.name}".take(100),
            detail = "Added +$refillAmount ${lowItem.unit} to ${lowItem.name} pantry stock.",
            hindiAnnouncement = hindiText,
            executionKey = execKey,
            executedAtMillis = nowMillis,
            actorUid = actorUid,
            acknowledgedByAdmin = source != "OFFLINE_CATCHUP" && source != "CLOUD_SCHEDULER",
          )
        }
      }
    }

    return AiTickEvaluationResult(
      directAnnouncements = announcements,
      directKitchenReplenishes = kitchenReplenishes,
      directTaskSuggestions = taskSuggestions,
      directExpiredTasks = expiredTasks,
      proposalsToCreate = proposals,
      scheduleTransitions = scheduleTransitions,
      logsToAppend = logs,
    )
  }

  fun buildHouseSuggestion(
    rations: List<RationItem>,
    tasks: List<HouseTask>,
    eligibleMembers: List<SeasonMember>,
    nowMillis: Long,
  ): AiApprovalProposal {
    val activeTaskCount = tasks.count { it.status in listOf("ACTIVE", "IN_PROGRESS", "ASSIGNED") }
    val lowRation = rations.firstOrNull { it.permitted && it.quantity <= it.minThreshold }
    val assignedUids = eligibleMembers
      .filter { it.role != "ADMIN" && it.status in listOf("IN_HOUSE", "LOBBY") }
      .take(5)
      .map { it.userId }

    return if (lowRation != null) {
      val addQty = max(5L, (lowRation.maxQuantity - lowRation.quantity).coerceAtMost(10L))
      val key = "manual_sug_ration_${lowRation.itemId}_${nowMillis / 1000L}"
      AiApprovalProposal(
        id = "prop_$key".take(120),
        category = AiActionType.KITCHEN_CHECK.name,
        title = "AI Suggestion: Restock ${lowRation.name} (+$addQty)".take(100),
        summary = "${lowRation.name} is low (${lowRation.quantity} left). Approve to replenish and alert kitchen crew.",
        hindiAnnouncement = "बिग बॉस रसोई अपडेट: ${lowRation.name} का नया राशन स्टोर रूम में भेज दिया गया है।",
        payloadJson = """{"itemId":"${lowRation.itemId}","addQuantity":$addQty,"action":"REPLENISH_RATION"}""",
        status = "PENDING",
        executionKey = key,
      )
    } else if (activeTaskCount == 0) {
      val preset = pickBalancedPreset(tasks)
      val presetKey = preset.activityType.name.lowercase()
      val key = "manual_sug_task_${presetKey}_${nowMillis / 1000L}"
      val pts = Phase8Logic.calculateRewardPoints(100L, preset.defaultDifficulty)
      AiApprovalProposal(
        id = "prop_$key".take(120),
        category = AiActionType.DAILY_TASK.name,
        title = "AI Task Idea: ${preset.title}".take(100),
        summary = "${preset.description} Target room: ${preset.targetRoom} (${preset.defaultDifficulty.name}, $pts pts). Players: ${assignedUids.size}.",
        hindiAnnouncement = "घरवालों तैयार हो जाइए! नया कार्य '${preset.title}' शुरू होने जा रहा है।",
        payloadJson = """{"presetId":"$presetKey","action":"START_PRESET_TASK"}""",
        status = "PENDING",
        executionKey = key,
      )
    } else {
      val key = "manual_sug_ann_${nowMillis / 1000L}"
      val preset = Phase9AiCatalog.hindiPresets.first()
      AiApprovalProposal(
        id = "prop_$key".take(120),
        category = AiActionType.HINDI_ANNOUNCEMENT.name,
        title = "AI House Broadcast: ${preset.title}".take(100),
        summary = "Active task is running smoothly. Broadcast an engaging Hindi check-in to living area.",
        hindiAnnouncement = preset.hindiMessage,
        payloadJson = """{"action":"HINDI_ANNOUNCEMENT","message":"${preset.hindiMessage}"}""",
        status = "PENDING",
        executionKey = key,
      )
    }
  }

  fun computeWhileAwaySummary(
    logs: List<AiActivityLog>,
    lastAdminSeenAtMillis: Long,
  ): AiWhileAwaySummary {
    val unseen = logs.filter { log ->
      !log.acknowledgedByAdmin ||
        (log.executedAtMillis > lastAdminSeenAtMillis &&
          log.source in listOf("CLOUD_SCHEDULER", "OFFLINE_CATCHUP"))
    }
    if (unseen.isEmpty()) {
      return AiWhileAwaySummary(lastSeenAtMillis = lastAdminSeenAtMillis)
    }
    return AiWhileAwaySummary(
      missedExecutionsCount = unseen.size,
      missedAnnouncementsCount = unseen.count { !it.hindiAnnouncement.isNullOrBlank() },
      missedProposalsCount = unseen.count { it.title.startsWith("Proposal", ignoreCase = true) },
      lastSeenAtMillis = lastAdminSeenAtMillis,
      highlights = unseen.sortedByDescending { it.executedAtMillis }
        .take(4)
        .map { "${it.title}: ${it.detail.take(80)}" },
    )
  }

  private fun pickBalancedPreset(existingTasks: List<HouseTask>): TaskPresetTemplate {
    val usedTitles = existingTasks.map { it.title.lowercase() }.toSet()
    return Phase6Catalog.taskPresets.firstOrNull { it.title.lowercase() !in usedTitles }
      ?: Phase6Catalog.taskPresets.first()
  }
}
