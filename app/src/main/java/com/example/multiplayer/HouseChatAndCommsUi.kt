package com.example.multiplayer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val ChatSurface = Color(0xFF0E1526)
private val ChatCard = Color(0xFF162038)
private val ChatGold = Color(0xFFEAB954)
private val ChatCyan = Color(0xFF22D3EE)
private val ChatEmerald = Color(0xFF34D399)
private val ChatMuted = Color(0xFF94A3B8)

@Composable
fun HouseChatAndCommsDialog(
  currentUserId: String,
  isAdminVerified: Boolean,
  members: List<SeasonMember>,
  publicMessages: List<HouseChatMessage>,
  privateDms: List<HouseChatMessage>,
  announcements: List<AdminAnnouncement>,
  ttsSettings: TtsPlaybackSettings,
  secretTasks: List<SecretTaskAssignment>,
  isMutedInChat: Boolean,
  onSendPublicMessage: (String) -> Unit,
  onSendPrivateDm: (recipientUid: String, recipientName: String, text: String) -> Unit,
  onUpdateTtsSettings: (TtsPlaybackSettings) -> Unit,
  onReplayWelcome: () -> Unit,
  onCompleteSecretTask: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var activeTab by remember { mutableIntStateOf(0) }
  var publicDraft by remember { mutableStateOf("") }
  var dmDraft by remember { mutableStateOf("") }

  // Exclude Secret Admin and self from player DM target selector
  val dmTargets = remember(members, currentUserId, isAdminVerified) {
    members.filter { m ->
      m.userId != currentUserId && (isAdminVerified || m.role != PlayerRole.ADMIN.name)
    }
  }
  var selectedDmTarget by remember(dmTargets) { mutableStateOf(dmTargets.firstOrNull()) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = ChatSurface,
      border = BorderStroke(1.dp, ChatCyan.copy(alpha = 0.45f)),
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .widthIn(max = 580.dp)
        .heightIn(max = 700.dp)
        .padding(10.dp)
        .testTag("house_chat_comms_dialog"),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ChatBubble, contentDescription = null, tint = ChatCyan)
            Spacer(Modifier.width(8.dp))
            Column {
              Text("BIGG BOSS – THE HOUSE COMMS", color = ChatGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
              Text("Text Chat, DMs & Speakers", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Close Chat", tint = Color.White)
          }
        }

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("Public Chat", "Private DMs", "Speakers & TTS", "Secret Tasks").forEachIndexed { idx, label ->
            val picked = activeTab == idx
            OutlinedButton(
              onClick = { activeTab = idx },
              border = BorderStroke(1.dp, if (picked) ChatCyan else Color.White.copy(alpha = 0.16f)),
              modifier = Modifier.heightIn(min = 44.dp).testTag("chat_tab_$idx"),
            ) { Text(label, color = if (picked) ChatCyan else ChatMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
          }
        }

        Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          when (activeTab) {
            0 -> {
              if (isMutedInChat && !isAdminVerified) {
                Text("🔇 You are currently muted by Bigg Boss.", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
              }
              publicMessages.forEach { msg ->
                Surface(shape = RoundedCornerShape(12.dp), color = ChatCard) {
                  Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                    Text("${msg.senderName} (${msg.senderRole})", color = if (msg.senderRole == "ADMIN") ChatGold else ChatCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(msg.text, color = Color.White, style = MaterialTheme.typography.bodySmall)
                  }
                }
              }
            }
            1 -> {
              Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF0E2920), border = BorderStroke(1.dp, ChatEmerald.copy(alpha = 0.5f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Rounded.Lock, contentDescription = null, tint = ChatEmerald, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Private 1:1 DMs are encrypted from House Speakers & never played aloud via TTS.", color = ChatEmerald, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
              }
              if (dmTargets.isEmpty()) {
                Text("No other active housemates available for DM.", color = ChatMuted, fontSize = 12.sp)
              } else {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  dmTargets.forEach { tgt ->
                    val picked = selectedDmTarget?.userId == tgt.userId
                    OutlinedButton(
                      onClick = { selectedDmTarget = tgt },
                      border = BorderStroke(1.dp, if (picked) ChatEmerald else Color.White.copy(alpha = 0.16f)),
                      modifier = Modifier.heightIn(min = 42.dp),
                    ) { Text(tgt.displayName, color = if (picked) ChatEmerald else ChatMuted, fontSize = 11.sp) }
                  }
                }
              }
              val myDms = privateDms.filter {
                it.senderUid == currentUserId || it.recipientUid == currentUserId || isAdminVerified
              }
              if (myDms.isEmpty()) {
                Text("No private direct messages yet.", color = ChatMuted, fontSize = 12.sp)
              } else {
                myDms.forEach { dm ->
                  Surface(shape = RoundedCornerShape(12.dp), color = ChatCard) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                      Text("${dm.senderName} -> ${dm.recipientName ?: dm.recipientUid}", color = ChatEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      Text(dm.text, color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                  }
                }
              }
            }
            2 -> {
              Surface(shape = RoundedCornerShape(14.dp), color = ChatCard) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = ChatGold)
                      Spacer(Modifier.width(6.dp))
                      Text("House Speakers (TTS)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Switch(checked = ttsSettings.enabled, onCheckedChange = { onUpdateTtsSettings(ttsSettings.copy(enabled = it)) })
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("hi-IN" to "Hindi (hi-IN)", "en-IN" to "English (en-IN)").forEach { (code, lbl) ->
                      OutlinedButton(
                        onClick = { onUpdateTtsSettings(ttsSettings.copy(languageTag = code)) },
                        border = BorderStroke(1.dp, if (ttsSettings.languageTag == code) ChatGold else Color.White.copy(alpha = 0.16f)),
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                      ) { Text(lbl, color = if (ttsSettings.languageTag == code) ChatGold else ChatMuted, fontSize = 11.sp) }
                    }
                  }
                  Text("Speed: ${"%.2f".format(ttsSettings.speechRate)}x", color = Color.White, fontSize = 12.sp)
                  Slider(
                    value = ttsSettings.speechRate,
                    onValueChange = { onUpdateTtsSettings(ttsSettings.copy(speechRate = it)) },
                    valueRange = 0.7f..1.4f,
                    colors = SliderDefaults.colors(thumbColor = ChatGold, activeTrackColor = ChatGold),
                  )
                  OutlinedButton(onClick = onReplayWelcome, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    Text("Replay Show Welcome Announcement", color = ChatCyan, fontWeight = FontWeight.Bold)
                  }
                }
              }
              Text("RECENT SPEAKER ANNOUNCEMENTS", color = ChatGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              announcements.take(10).forEach { ann ->
                Surface(shape = RoundedCornerShape(10.dp), color = ChatCard) {
                  Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                    Text("${ann.languageTag} • ${ann.voiceGender}", color = ChatCyan, fontSize = 10.sp)
                    Text(ann.message, color = Color.White, fontSize = 12.sp)
                  }
                }
              }
            }
            3 -> {
              val visibleTasks = if (isAdminVerified) secretTasks else secretTasks.filter { it.targetUid == currentUserId }
              if (visibleTasks.isEmpty()) {
                Text("No secret tasks assigned to you right now.", color = ChatMuted)
              } else {
                visibleTasks.forEach { st ->
                  Surface(shape = RoundedCornerShape(12.dp), color = ChatCard, border = BorderStroke(1.dp, ChatGold.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Text("${st.title} (+${st.rewardPoints} pts) • ${st.status}", color = ChatGold, fontWeight = FontWeight.Bold)
                      Text("For: ${st.targetName}", color = ChatCyan, fontSize = 11.sp)
                      Text(st.secretInstructions, color = Color.White, style = MaterialTheme.typography.bodySmall)
                      if (st.status == "ACTIVE") {
                        Button(
                          onClick = { onCompleteSecretTask(st.id) },
                          colors = ButtonDefaults.buttonColors(containerColor = ChatEmerald, contentColor = Color(0xFF06281E)),
                          modifier = Modifier.height(44.dp),
                        ) { Text("Mark Secret Mission Complete", fontWeight = FontWeight.Bold) }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        if (activeTab == 0) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
              value = publicDraft,
              onValueChange = { if (it.length <= 280) publicDraft = it },
              placeholder = { Text("Message public housemates...") },
              singleLine = true,
              enabled = !isMutedInChat || isAdminVerified,
              colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
              modifier = Modifier.weight(1f).testTag("public_chat_input"),
            )
            IconButton(
              onClick = {
                onSendPublicMessage(publicDraft)
                publicDraft = ""
              },
              enabled = publicDraft.isNotBlank() && (!isMutedInChat || isAdminVerified),
              modifier = Modifier.size(48.dp).testTag("public_chat_send"),
            ) { Icon(Icons.Rounded.Send, contentDescription = "Send Public Message", tint = ChatCyan) }
          }
        } else if (activeTab == 1) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
              value = dmDraft,
              onValueChange = { if (it.length <= 280) dmDraft = it },
              placeholder = { Text("Send private DM...") },
              singleLine = true,
              enabled = selectedDmTarget != null,
              colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
              modifier = Modifier.weight(1f).testTag("private_dm_input"),
            )
            IconButton(
              onClick = {
                selectedDmTarget?.let { tgt ->
                  onSendPrivateDm(tgt.userId, tgt.displayName, dmDraft)
                  dmDraft = ""
                }
              },
              enabled = dmDraft.isNotBlank() && selectedDmTarget != null,
              modifier = Modifier.size(48.dp).testTag("private_dm_send"),
            ) { Icon(Icons.Rounded.Send, contentDescription = "Send Private DM", tint = ChatEmerald) }
          }
        }
      }
    }
  }
}
