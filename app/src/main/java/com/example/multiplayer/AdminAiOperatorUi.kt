package com.example.multiplayer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val AiConsoleBg = Color(0xFF0A1020)
private val AiCard = Color(0xFF131D34)
private val AiElevated = Color(0xFF1A2642)
private val AiCyan = Color(0xFF22D3EE)
private val AiGold = Color(0xFFEAB954)
private val AiEmerald = Color(0xFF34D399)
private val AiDanger = Color(0xFFF87171)
private val AiMuted = Color(0xFF94A3B8)

@Composable
fun AiOperatorConsoleDialog(
  isAdminVerified: Boolean,
  config: AiControlConfig?,
  schedules: List<AiScheduledEvent>,
  proposals: List<AiApprovalProposal>,
  logs: List<AiActivityLog>,
  whileAwaySummary: AiWhileAwaySummary,
  isBusy: Boolean,
  actionMessage: String?,
  actionError: String?,
  onDismiss: () -> Unit,
  onClearFeedback: () -> Unit,
  onSetEnabled: (Boolean) -> Unit,
  onSetMode: (AiControlMode) -> Unit,
  onSetModules: (autoHindi: Boolean, autoKitchen: Boolean, autoTasks: Boolean, autoTimer: Boolean) -> Unit,
  onEmergencyStop: () -> Unit,
  onAcknowledgeWhileAway: () -> Unit,
  onCreateSchedule: (AiActionType, String, String, Long, Long) -> Unit,
  onCancelSchedule: (String) -> Unit,
  onRunAiTick: (String) -> Unit,
  onGenerateSuggestion: () -> Unit,
  onApproveProposal: (AiApprovalProposal) -> Unit,
  onRejectProposal: (AiApprovalProposal) -> Unit,
) {
  if (!isAdminVerified) return

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = AiConsoleBg,
      border = BorderStroke(1.5.dp, AiCyan.copy(alpha = 0.55f)),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.92f)
        .widthIn(max = 920.dp)
        .testTag("chair1_ai_operator_dialog"),
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AiCyan.copy(alpha = 0.18f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = AiCyan)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "CHAIR 1 • CHATGPT AI OPERATOR",
                color = AiCyan,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp,
              )
              Text(
                text = "Independent House Manager • Offline Cloud Scheduler",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp).testTag("close_ai_console_button"),
          ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close Chair 1 Console", tint = Color.White)
          }
        }

        if (!actionError.isNullOrBlank() || !actionMessage.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(8.dp))
          val isErr = !actionError.isNullOrBlank()
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = (if (isErr) AiDanger else AiEmerald).copy(alpha = 0.16f),
            border = BorderStroke(1.dp, if (isErr) AiDanger else AiEmerald),
            modifier = Modifier.fillMaxWidth().clickable { onClearFeedback() },
          ) {
            Text(
              text = actionError ?: actionMessage.orEmpty(),
              color = Color.White,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(10.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        AdminAiOperatorTab(
          config = config,
          schedules = schedules,
          proposals = proposals,
          logs = logs,
          whileAwaySummary = whileAwaySummary,
          isBusy = isBusy,
          onSetEnabled = onSetEnabled,
          onSetMode = onSetMode,
          onSetModules = onSetModules,
          onEmergencyStop = onEmergencyStop,
          onAcknowledgeWhileAway = onAcknowledgeWhileAway,
          onCreateSchedule = onCreateSchedule,
          onCancelSchedule = onCancelSchedule,
          onRunAiTick = onRunAiTick,
          onGenerateSuggestion = onGenerateSuggestion,
          onApproveProposal = onApproveProposal,
          onRejectProposal = onRejectProposal,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
fun AdminAiOperatorTab(
  config: AiControlConfig?,
  schedules: List<AiScheduledEvent>,
  proposals: List<AiApprovalProposal>,
  logs: List<AiActivityLog>,
  whileAwaySummary: AiWhileAwaySummary,
  isBusy: Boolean,
  onSetEnabled: (Boolean) -> Unit,
  onSetMode: (AiControlMode) -> Unit,
  onSetModules: (autoHindi: Boolean, autoKitchen: Boolean, autoTasks: Boolean, autoTimer: Boolean) -> Unit,
  onEmergencyStop: () -> Unit,
  onAcknowledgeWhileAway: () -> Unit,
  onCreateSchedule: (AiActionType, String, String, Long, Long) -> Unit,
  onCancelSchedule: (String) -> Unit,
  onRunAiTick: (String) -> Unit,
  onGenerateSuggestion: () -> Unit,
  onApproveProposal: (AiApprovalProposal) -> Unit,
  onRejectProposal: (AiApprovalProposal) -> Unit,
  modifier: Modifier = Modifier,
) {
  val effectiveCfg = config ?: AiControlConfig()
  val providerStatus = effectiveCfg.parsedProvider()
  val currentMode = effectiveCfg.parsedMode()
  val pendingProposals = remember(proposals) { proposals.filter { it.status == "PENDING" } }

  var selectedActionType by remember { mutableStateOf(AiActionType.HINDI_ANNOUNCEMENT) }
  var scheduleTitle by remember { mutableStateOf("Evening Hindi Assembly Call") }
  var schedulePayload by remember { mutableStateOf(Phase9AiCatalog.hindiPresets.first().hindiMessage) }
  var scheduleDelayMins by remember { mutableLongStateOf(0L) }
  var scheduleRecurrenceMins by remember { mutableLongStateOf(0L) }

  Column(
    modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // 1. Honest AI API Provider Transparency Card
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = AiCard,
      border = BorderStroke(
        1.dp,
        if (providerStatus.isExternalAiConfigured) AiEmerald.copy(alpha = 0.55f) else AiGold.copy(alpha = 0.55f),
      ),
      modifier = Modifier.testTag("ai_api_transparency_card"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Icon(
          Icons.Rounded.Info,
          contentDescription = null,
          tint = if (providerStatus.isExternalAiConfigured) AiEmerald else AiGold,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = providerStatus.displayTitle,
            color = if (providerStatus.isExternalAiConfigured) AiEmerald else AiGold,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = providerStatus.detailNotice,
            color = AiMuted,
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    }

    // 2. While You Were Away Summary (Offline Cloud / Catch-up activity)
    if (whileAwaySummary.missedExecutionsCount > 0 || whileAwaySummary.missedProposalsCount > 0) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = AiCyan.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, AiCyan.copy(alpha = 0.6f)),
        modifier = Modifier.testTag("while_you_were_away_card"),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "WHILE YOU WERE AWAY • ${whileAwaySummary.missedExecutionsCount} OFFLINE ACTION(S)",
              color = AiCyan,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.ExtraBold,
            )
            TextButton(
              onClick = onAcknowledgeWhileAway,
              modifier = Modifier.heightIn(min = 48.dp).testTag("acknowledge_while_away_button"),
            ) {
              Text("Acknowledge", color = AiCyan, fontWeight = FontWeight.Bold)
            }
          }
          Text(
            text = "Announcements sent: ${whileAwaySummary.missedAnnouncementsCount} • Missed proposals: ${whileAwaySummary.missedProposalsCount}",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
          )
          whileAwaySummary.highlights.forEach { hl ->
            Text("• $hl", color = AiMuted, style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }

    // 3. Master Toggle, Operating Mode & Emergency Stop
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = AiCard,
      border = BorderStroke(
        1.5.dp,
        if (effectiveCfg.emergencyStopped) AiDanger else AiCyan.copy(alpha = 0.35f),
      ),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (effectiveCfg.emergencyStopped) "AI HALTED (EMERGENCY STOP)"
              else if (effectiveCfg.enabled) "AI HOUSE MANAGER ACTIVE"
              else "AI HOUSE MANAGER OFF",
              color = if (effectiveCfg.emergencyStopped) AiDanger else if (effectiveCfg.enabled) AiEmerald else AiMuted,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.ExtraBold,
            )
            Text(
              text = "Chair 1 status synced to Control Room & offline Cloud Scheduler.",
              color = AiMuted,
              style = MaterialTheme.typography.bodySmall,
            )
          }
          Switch(
            checked = effectiveCfg.enabled && !effectiveCfg.emergencyStopped,
            onCheckedChange = onSetEnabled,
            modifier = Modifier.testTag("ai_enabled_switch"),
          )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          AiControlMode.entries.forEach { mode ->
            val picked = currentMode == mode
            OutlinedButton(
              onClick = { onSetMode(mode) },
              enabled = !isBusy,
              border = BorderStroke(1.dp, if (picked) AiCyan else Color.White.copy(alpha = 0.16f)),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (picked) AiCyan.copy(alpha = 0.16f) else Color.Transparent,
              ),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("ai_mode_${mode.name}"),
            ) {
              Text(
                text = mode.label,
                color = if (picked) AiCyan else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
        Text(
          text = currentMode.description,
          color = AiMuted,
          style = MaterialTheme.typography.labelSmall,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = onEmergencyStop,
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = AiDanger, contentColor = Color(0xFF1F0808)),
            modifier = Modifier.weight(1f).height(48.dp).testTag("ai_emergency_stop_button"),
          ) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (effectiveCfg.emergencyStopped) "Emergency Stopped (Tap ON to Resume)" else "EMERGENCY STOP AI",
              fontWeight = FontWeight.ExtraBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    // 4. AI Subsystems (Hindi, Kitchen Phase 7, Tasks Phase 8, Timers)
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = AiCard,
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("AUTOMATION MODULES", color = AiGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        AiModuleToggleRow(
          label = "Auto Hindi Announcements (hi-IN)",
          checked = effectiveCfg.autoHindiAnnouncements,
          onToggle = {
            onSetModules(it, effectiveCfg.autoKitchenMonitor, effectiveCfg.autoTaskCoordinator, effectiveCfg.autoTimerMonitor)
          },
        )
        AiModuleToggleRow(
          label = "Phase 7 Kitchen & Ration Coordinator",
          checked = effectiveCfg.autoKitchenMonitor,
          onToggle = {
            onSetModules(effectiveCfg.autoHindiAnnouncements, it, effectiveCfg.autoTaskCoordinator, effectiveCfg.autoTimerMonitor)
          },
        )
        AiModuleToggleRow(
          label = "Phase 8 Daily Task Coordinator",
          checked = effectiveCfg.autoTaskCoordinator,
          onToggle = {
            onSetModules(effectiveCfg.autoHindiAnnouncements, effectiveCfg.autoKitchenMonitor, it, effectiveCfg.autoTimerMonitor)
          },
        )
        AiModuleToggleRow(
          label = "Task Timer Expiry Monitor",
          checked = effectiveCfg.autoTimerMonitor,
          onToggle = {
            onSetModules(effectiveCfg.autoHindiAnnouncements, effectiveCfg.autoKitchenMonitor, effectiveCfg.autoTaskCoordinator, it)
          },
        )
      }
    }

    // 5. Admin Approval Queue (Assist Mode Proposals)
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = AiCard,
      border = BorderStroke(1.dp, AiGold.copy(alpha = 0.35f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "ADMIN APPROVAL QUEUE (${pendingProposals.size})",
            color = AiGold,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
          OutlinedButton(
            onClick = onGenerateSuggestion,
            enabled = !isBusy,
            border = BorderStroke(1.dp, AiCyan),
            modifier = Modifier.heightIn(min = 48.dp).testTag("generate_ai_suggestion_button"),
          ) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = AiCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Suggest Action", color = AiCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
        if (pendingProposals.isEmpty()) {
          Text(
            text = "No pending proposals. Tap 'Suggest Action' or run a scheduler tick in Assist mode.",
            color = AiMuted,
            style = MaterialTheme.typography.bodySmall,
          )
        } else {
          pendingProposals.forEach { prop ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = AiElevated,
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            ) {
              Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${prop.title} [${prop.category}]", color = Color.White, fontWeight = FontWeight.Bold)
                Text(prop.summary, color = AiMuted, style = MaterialTheme.typography.bodySmall)
                if (!prop.hindiAnnouncement.isNullOrBlank()) {
                  Text("hi-IN: ${prop.hindiAnnouncement}", color = AiGold, style = MaterialTheme.typography.labelSmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Button(
                    onClick = { onApproveProposal(prop) },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = AiEmerald, contentColor = Color(0xFF06281E)),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("approve_proposal_${prop.id}"),
                  ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve & Run", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  }
                  OutlinedButton(
                    onClick = { onRejectProposal(prop) },
                    enabled = !isBusy,
                    border = BorderStroke(1.dp, AiDanger.copy(alpha = 0.7f)),
                    modifier = Modifier.height(48.dp).testTag("reject_proposal_${prop.id}"),
                  ) {
                    Text("Reject", color = AiDanger, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  }
                }
              }
            }
          }
        }
      }
    }

    // 6. Offline Scheduler & Presets
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = AiCard,
      border = BorderStroke(1.dp, AiCyan.copy(alpha = 0.35f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "OFFLINE CLOUD SCHEDULER & HINDI PRESETS",
          color = AiCyan,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
        )

        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Phase9AiCatalog.hindiPresets.forEach { preset ->
            OutlinedButton(
              onClick = {
                selectedActionType = preset.category
                scheduleTitle = preset.title
                schedulePayload = preset.hindiMessage
              },
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
              modifier = Modifier.heightIn(min = 48.dp),
            ) {
              Text(preset.title, color = Color.White, fontSize = 11.sp)
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          AiActionType.entries.forEach { act ->
            val picked = selectedActionType == act
            OutlinedButton(
              onClick = { selectedActionType = act },
              border = BorderStroke(1.dp, if (picked) AiCyan else Color.White.copy(alpha = 0.15f)),
              modifier = Modifier.heightIn(min = 48.dp),
            ) {
              Text(act.label, color = if (picked) AiCyan else AiMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        OutlinedTextField(
          value = scheduleTitle,
          onValueChange = { if (it.length <= 100) scheduleTitle = it },
          label = { Text("Schedule Title") },
          singleLine = true,
          colors = aiFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("ai_schedule_title_input"),
        )
        OutlinedTextField(
          value = schedulePayload,
          onValueChange = { if (it.length <= 600) schedulePayload = it },
          label = { Text("Hindi Announcement / Action Payload") },
          minLines = 2,
          colors = aiFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("ai_schedule_payload_input"),
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(0L to "Due Now", 5L to "+5m", 15L to "+15m", 60L to "+60m").forEach { (mins, label) ->
            val picked = scheduleDelayMins == mins
            OutlinedButton(
              onClick = { scheduleDelayMins = mins },
              border = BorderStroke(1.dp, if (picked) AiGold else Color.White.copy(alpha = 0.15f)),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
              Text(label, color = if (picked) AiGold else AiMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(0L to "Once", 15L to "Every 15m", 60L to "Every 60m").forEach { (rec, label) ->
            val picked = scheduleRecurrenceMins == rec
            OutlinedButton(
              onClick = { scheduleRecurrenceMins = rec },
              border = BorderStroke(1.dp, if (picked) AiCyan else Color.White.copy(alpha = 0.15f)),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
              Text(label, color = if (picked) AiCyan else AiMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        Button(
          onClick = {
            onCreateSchedule(selectedActionType, scheduleTitle, schedulePayload, scheduleDelayMins, scheduleRecurrenceMins)
          },
          enabled = !isBusy && scheduleTitle.trim().length >= 2 && schedulePayload.trim().length >= 2,
          colors = ButtonDefaults.buttonColors(containerColor = AiCyan, contentColor = Color(0xFF061622)),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_ai_schedule_button"),
        ) {
          Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Queue Scheduled Action", fontWeight = FontWeight.ExtraBold)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = { onRunAiTick("ADMIN_TICK") },
            enabled = !isBusy,
            border = BorderStroke(1.dp, AiEmerald),
            modifier = Modifier.weight(1f).height(48.dp).testTag("run_ai_tick_button"),
          ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = AiEmerald, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Run Scheduler Tick", color = AiEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
          OutlinedButton(
            onClick = { onRunAiTick("OFFLINE_CATCHUP") },
            enabled = !isBusy,
            border = BorderStroke(1.dp, AiGold),
            modifier = Modifier.weight(1f).height(48.dp).testTag("run_offline_catchup_button"),
          ) {
            Text("Offline Catch-Up", color = AiGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }

        if (schedules.isNotEmpty()) {
          HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
          schedules.take(8).forEach { sch ->
            Row(
              modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AiElevated).padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${sch.title} [${sch.status}]", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text("${sch.actionType} • rec=${sch.recurrenceMinutes}m", color = AiMuted, style = MaterialTheme.typography.labelSmall)
              }
              if (sch.status == "PENDING" || sch.status == "PROPOSED") {
                IconButton(
                  onClick = { onCancelSchedule(sch.id) },
                  modifier = Modifier.size(48.dp).testTag("cancel_schedule_${sch.id}"),
                ) {
                  Icon(Icons.Rounded.DeleteOutline, contentDescription = "Cancel Schedule", tint = AiDanger)
                }
              }
            }
          }
        }
      }
    }

    // 7. Activity Log & Hindi History
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = AiCard,
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.History, contentDescription = null, tint = AiMuted, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("AI ACTIVITY & HINDI ANNOUNCEMENT LOG (${logs.size})", color = AiMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        if (logs.isEmpty()) {
          Text("No AI actions logged yet.", color = AiMuted, style = MaterialTheme.typography.bodySmall)
        } else {
          logs.take(12).forEach { entry ->
            Surface(shape = RoundedCornerShape(10.dp), color = AiElevated) {
              Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Text("${entry.title} • [${entry.source}/${entry.mode}]", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(entry.detail, color = AiMuted, style = MaterialTheme.typography.bodySmall)
                if (!entry.hindiAnnouncement.isNullOrBlank()) {
                  Text("hi-IN: ${entry.hindiAnnouncement}", color = AiGold, style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AiModuleToggleRow(
  label: String,
  checked: Boolean,
  onToggle: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = onToggle)
  }
}

@Composable
private fun aiFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedBorderColor = AiCyan,
  unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
  focusedTextColor = Color.White,
  unfocusedTextColor = Color.White,
)
