package com.example.multiplayer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardBg = Color(0xFF151E33)
private val ElevatedBg = Color(0xFF1C2742)
private val Gold = Color(0xFFEAB954)
private val Cyan = Color(0xFF22D3EE)
private val Emerald = Color(0xFF34D399)
private val Danger = Color(0xFFF87171)
private val Muted = Color(0xFF94A3B8)

@Composable
private fun ruleFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedBorderColor = Cyan,
  unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
  focusedTextColor = Color.White,
  unfocusedTextColor = Color.White,
  focusedLabelColor = Cyan,
  unfocusedLabelColor = Muted,
)

@Composable
fun AdminHouseRulesTab(
  rules: List<HouseRule>,
  violations: List<RuleViolation>,
  members: List<SeasonMember>,
  isBusy: Boolean,
  onSaveRule: (
    ruleId: String?,
    ruleNumber: Long,
    title: String,
    description: String,
    severity: String,
    active: Boolean,
  ) -> Unit,
  onBroadcastRule: (String) -> Unit,
  onIssueWarning: (HouseRule, SeasonMember, String, String) -> Unit,
) {
  val activeMembers = remember(members) {
    members.filter { it.status !in listOf("EVICTED", "BANNED", "REMOVED") }
  }

  var editingRuleId by remember { mutableStateOf<String?>(null) }
  var ruleNumberText by remember { mutableStateOf((rules.size + 1).toString()) }
  var ruleTitle by remember { mutableStateOf("") }
  var ruleDesc by remember { mutableStateOf("") }
  var ruleSeverity by remember { mutableStateOf("MEDIUM") }

  var selectedRuleForWarning by remember(rules) { mutableStateOf(rules.firstOrNull()) }
  var selectedTargetUid by remember(activeMembers) { mutableStateOf(activeMembers.firstOrNull()?.userId) }
  var warningLevel by remember { mutableStateOf("WARNING") }
  var warningNote by remember { mutableStateOf("Please follow House Rules immediately.") }

  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Issue Rule Violation Warning
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = CardBg,
      border = BorderStroke(1.dp, Danger.copy(alpha = 0.45f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ISSUE RULE VIOLATION WARNING", color = Danger, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        if (activeMembers.isEmpty()) {
          Text("No active housemates in house.", color = Muted, style = MaterialTheme.typography.bodySmall)
        } else {
          Text("Select Contestant:", color = Muted, style = MaterialTheme.typography.labelSmall)
          activeMembers.forEach { m ->
            val picked = m.userId == selectedTargetUid
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (picked) Danger.copy(alpha = 0.2f) else ElevatedBg)
                .clickable { selectedTargetUid = m.userId }
                .padding(horizontal = 10.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text("${m.displayName} (${m.playerId})", color = Color.White, fontWeight = FontWeight.Bold)
              if (picked) Text("SELECTED", color = Danger, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
            }
          }

          if (rules.isNotEmpty()) {
            Text("Rule Violated:", color = Muted, style = MaterialTheme.typography.labelSmall)
            rules.take(6).forEach { r ->
              val picked = r.id == selectedRuleForWarning?.id
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (picked) Gold.copy(alpha = 0.16f) else ElevatedBg)
                  .clickable { selectedRuleForWarning = r }
                  .padding(horizontal = 10.dp, vertical = 8.dp),
              ) {
                Text("#${r.ruleNumber} ${r.title}", color = Color.White, style = MaterialTheme.typography.bodySmall)
              }
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("NOTICE", "WARNING", "FINAL_WARNING").forEach { lvl ->
              val picked = warningLevel == lvl
              OutlinedButton(
                onClick = { warningLevel = lvl },
                border = BorderStroke(1.dp, if (picked) Danger else Color.White.copy(alpha = 0.2f)),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
              ) {
                Text(lvl.replace("_", " "), color = if (picked) Danger else Color.White, fontSize = 10.sp)
              }
            }
          }

          OutlinedTextField(
            value = warningNote,
            onValueChange = { warningNote = it },
            label = { Text("Warning Reason / Note") },
            colors = ruleFieldColors(),
            modifier = Modifier.fillMaxWidth(),
          )

          Button(
            onClick = {
              val target = activeMembers.firstOrNull { it.userId == selectedTargetUid } ?: return@Button
              val rule = selectedRuleForWarning ?: HouseRule(id = "rule_general", title = "House Discipline")
              onIssueWarning(rule, target, warningLevel, warningNote)
            },
            enabled = !isBusy && selectedTargetUid != null,
            colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color(0xFF1F0808)),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("issue_rule_warning_btn"),
          ) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Issue Official Warning", fontWeight = FontWeight.ExtraBold)
          }
        }
      }
    }

    // Add / Edit Rule Form
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = CardBg,
      border = BorderStroke(1.dp, Cyan.copy(alpha = 0.35f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          if (editingRuleId == null) "ADD HOUSE RULE" else "EDIT HOUSE RULE",
          color = Cyan,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = ruleNumberText,
            onValueChange = { if (it.all(Char::isDigit)) ruleNumberText = it },
            label = { Text("#") },
            singleLine = true,
            colors = ruleFieldColors(),
            modifier = Modifier.weight(0.28f),
          )
          OutlinedTextField(
            value = ruleTitle,
            onValueChange = { ruleTitle = it },
            label = { Text("Rule Title") },
            singleLine = true,
            colors = ruleFieldColors(),
            modifier = Modifier.weight(0.72f).testTag("rule_title_input"),
          )
        }
        OutlinedTextField(
          value = ruleDesc,
          onValueChange = { ruleDesc = it },
          label = { Text("Rule Description") },
          colors = ruleFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
          listOf("LOW", "MEDIUM", "HIGH", "CRITICAL").forEach { sev ->
            val picked = ruleSeverity == sev
            OutlinedButton(
              onClick = { ruleSeverity = sev },
              border = BorderStroke(1.dp, if (picked) Gold else Color.White.copy(alpha = 0.2f)),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
              Text(sev, color = if (picked) Gold else Color.White, fontSize = 10.sp)
            }
          }
        }
        Button(
          onClick = {
            onSaveRule(
              editingRuleId,
              ruleNumberText.toLongOrNull() ?: (rules.size + 1L),
              ruleTitle,
              ruleDesc,
              ruleSeverity,
              true,
            )
            editingRuleId = null
            ruleTitle = ""
            ruleDesc = ""
          },
          enabled = !isBusy && ruleTitle.trim().length >= 3 && ruleDesc.trim().length >= 3,
          colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF10141F)),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_house_rule_btn"),
        ) {
          Text("Save House Rule", fontWeight = FontWeight.ExtraBold)
        }
      }
    }

    // Active Rules List
    Surface(shape = RoundedCornerShape(18.dp), color = CardBg) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("HOUSE RULES (${rules.size})", color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        rules.forEach { rule ->
          Surface(shape = RoundedCornerShape(12.dp), color = ElevatedBg) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("#${rule.ruleNumber} • ${rule.title} (${rule.severity})", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(
                  checked = rule.active,
                  onCheckedChange = { next ->
                    onSaveRule(rule.id, rule.ruleNumber, rule.title, rule.description, rule.severity, next)
                  },
                  colors = SwitchDefaults.colors(checkedThumbColor = Emerald),
                )
              }
              Text(rule.description, color = Muted, style = MaterialTheme.typography.bodySmall)
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                  onClick = { onBroadcastRule("House Rule #${rule.ruleNumber}: ${rule.title}. ${rule.description}") },
                  enabled = !isBusy,
                  border = BorderStroke(1.dp, Cyan),
                  modifier = Modifier.heightIn(min = 48.dp),
                ) {
                  Icon(Icons.Rounded.Campaign, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Broadcast Rule", color = Cyan, fontSize = 11.sp)
                }
                OutlinedButton(
                  onClick = {
                    editingRuleId = rule.id
                    ruleNumberText = rule.ruleNumber.toString()
                    ruleTitle = rule.title
                    ruleDesc = rule.description
                    ruleSeverity = rule.severity
                  },
                  modifier = Modifier.heightIn(min = 48.dp),
                ) {
                  Text("Edit", color = Gold, fontSize = 11.sp)
                }
              }
            }
          }
        }
      }
    }

    if (violations.isNotEmpty()) {
      Surface(shape = RoundedCornerShape(18.dp), color = CardBg) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("VIOLATION HISTORY (${violations.size})", color = Danger, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          violations.forEach { v ->
            Text(
              "[${v.warningLevel}] ${v.targetDisplayName} (${v.targetPlayerId}) — ${v.ruleTitle}: ${v.note}",
              color = Color(0xFFFECACA),
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      }
    }
  }
}

@Composable
fun AdminDailyScheduleTab(
  scheduleSlots: List<DailyScheduleSlot>,
  isBusy: Boolean,
  onSaveSlot: (DailyScheduleSlot) -> Unit,
  onPublishSchedule: (activeSlotId: String, broadcastUpdate: Boolean) -> Unit,
) {
  var editingSlotId by remember { mutableStateOf<String?>(null) }
  var timeLabel by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var broadcastOnPublish by remember { mutableStateOf(true) }

  val activeSlotId = scheduleSlots.firstOrNull { it.isCurrent }?.id ?: scheduleSlots.firstOrNull()?.id ?: "slot_1"

  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = CardBg,
      border = BorderStroke(1.dp, Gold.copy(alpha = 0.45f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("DAILY HOUSE ACTIVITIES ROUTINE", color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Announce schedule updates to all players", color = Color.White, style = MaterialTheme.typography.bodySmall)
          Switch(
            checked = broadcastOnPublish,
            onCheckedChange = { broadcastOnPublish = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Gold),
          )
        }
        Button(
          onClick = { onPublishSchedule(activeSlotId, broadcastOnPublish) },
          enabled = !isBusy && scheduleSlots.isNotEmpty(),
          colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF10141F)),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("publish_schedule_btn"),
        ) {
          Icon(Icons.Rounded.CheckCircle, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text("Publish Schedule Changes", fontWeight = FontWeight.ExtraBold)
        }
      }
    }

    scheduleSlots.forEach { slot ->
      val isEditing = editingSlotId == slot.id
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (slot.isCurrent) ElevatedBg else CardBg,
        border = BorderStroke(1.dp, if (slot.isCurrent) Emerald else Color.White.copy(alpha = 0.08f)),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                "${slot.orderIndex}. ${slot.title} • ${slot.timeLabel}",
                color = if (slot.isCurrent) Emerald else Color.White,
                fontWeight = FontWeight.Bold,
              )
              Text("Location: ${slot.location}", color = Cyan, style = MaterialTheme.typography.labelSmall)
              Text(slot.description, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = { onPublishSchedule(slot.id, broadcastOnPublish) },
              enabled = !isBusy && !slot.isCurrent,
              border = BorderStroke(1.dp, if (slot.isCurrent) Emerald else Gold),
              modifier = Modifier.heightIn(min = 48.dp),
            ) {
              Text(
                if (slot.isCurrent) "Active Now" else "Set Active",
                color = if (slot.isCurrent) Emerald else Gold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
              )
            }
            OutlinedButton(
              onClick = {
                if (isEditing) {
                  editingSlotId = null
                } else {
                  editingSlotId = slot.id
                  timeLabel = slot.timeLabel
                  location = slot.location
                  description = slot.description
                }
              },
              border = BorderStroke(1.dp, Cyan),
              modifier = Modifier.heightIn(min = 48.dp),
            ) {
              Text(if (isEditing) "Close Edit" else "Edit Slot", color = Cyan, fontSize = 11.sp)
            }
          }

          if (isEditing) {
            OutlinedTextField(
              value = timeLabel,
              onValueChange = { timeLabel = it },
              label = { Text("Time") },
              singleLine = true,
              colors = ruleFieldColors(),
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = location,
              onValueChange = { location = it },
              label = { Text("House Zone / Location") },
              singleLine = true,
              colors = ruleFieldColors(),
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = description,
              onValueChange = { description = it },
              label = { Text("Activity Description") },
              colors = ruleFieldColors(),
              modifier = Modifier.fillMaxWidth(),
            )
            Button(
              onClick = {
                onSaveSlot(
                  slot.copy(
                    timeLabel = timeLabel.trim().ifBlank { slot.timeLabel },
                    location = location.trim().ifBlank { slot.location },
                    description = description.trim().ifBlank { slot.description },
                  ),
                )
                editingSlotId = null
              },
              enabled = !isBusy,
              colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color(0xFF061622)),
              modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
              Text("Save Slot", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
