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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Date

private val TabCard = Color(0xFF151E33)
private val TabElevated = Color(0xFF1C2742)
private val TabGold = Color(0xFFEAB954)
private val TabCyan = Color(0xFF22D3EE)
private val TabEmerald = Color(0xFF34D399)
private val TabPurple = Color(0xFFC084FC)
private val TabDanger = Color(0xFFF87171)
private val TabMuted = Color(0xFF94A3B8)

@Composable
fun AdminCaptainAndVipTab(
  season: SeasonSession?,
  members: List<SeasonMember>,
  vipPrivileges: List<VipPrivilege>,
  privilegeHistory: List<PrivilegeHistoryRecord>,
  isBusy: Boolean,
  onAssignOrReplaceCaptain: (SeasonMember, String, CaptainPowersConfig) -> Unit,
  onRemoveCaptain: () -> Unit,
  onUpdateCaptainPowers: (CaptainPowersConfig) -> Unit,
  onSetPlayerVip: (SeasonMember, Boolean, String) -> Unit,
) {
  val eligibleMembers = remember(members) {
    members.filter { it.role != PlayerRole.ADMIN.name && it.status !in listOf("EVICTED", "BANNED", "REMOVED") }
  }
  val activeVipUids = remember(vipPrivileges) {
    vipPrivileges.filter { it.effectiveVip }.associateBy { it.userId }
  }

  var pickedCaptainUid by remember(season?.captainUid) {
    mutableStateOf(season?.captainUid ?: eligibleMembers.firstOrNull()?.userId)
  }
  var captainTitle by remember(season?.captainTitle) {
    mutableStateOf(season?.captainTitle ?: "House Captain")
  }
  var powerVipAccess by remember(season?.captainVipAccess) {
    mutableStateOf(season?.captainVipAccess ?: true)
  }
  var powerImmunity by remember(season?.captainImmunity) {
    mutableStateOf(season?.captainImmunity ?: false)
  }
  var powerDoubleVote by remember(season?.captainDoubleVote) {
    mutableStateOf(season?.captainDoubleVote ?: false)
  }
  var powerTieBreaker by remember(season?.captainTieBreaker) {
    mutableStateOf(season?.captainTieBreaker ?: false)
  }
  var vipLabelInput by remember { mutableStateOf("VIP PASS") }

  val currentPowers = CaptainPowersConfig(
    vipRoomAccess = powerVipAccess,
    nominationImmunity = powerImmunity,
    doubleVoteWeight = powerDoubleVote,
    tieBreakerPower = powerTieBreaker,
  )

  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Active Captain Status & Remove Control
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = TabCard,
      border = BorderStroke(1.dp, TabGold.copy(alpha = 0.5f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = TabGold, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("REIGNING HOUSE CAPTAIN", color = TabGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          }
          if (!season?.captainUid.isNullOrBlank()) {
            OutlinedButton(
              onClick = onRemoveCaptain,
              enabled = !isBusy,
              border = BorderStroke(1.dp, TabDanger),
              modifier = Modifier.heightIn(min = 48.dp).testTag("remove_captain_button"),
            ) {
              Icon(Icons.Rounded.PersonRemove, contentDescription = null, tint = TabDanger, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Remove Captain", color = TabDanger, fontWeight = FontWeight.Bold)
            }
          }
        }
        Text(
          text = if (!season?.captainDisplayName.isNullOrBlank()) {
            "👑 ${season?.captainDisplayName} (${season?.captainPlayerId}) — ${season?.captainTitle ?: "House Captain"}"
          } else {
            "No Captain currently assigned. Existing Captain VIP Room is locked to VIP holders & Admin."
          },
          color = Color.White,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    // Assign or Replace Captain Card
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = TabCard,
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ASSIGN OR REPLACE CAPTAIN", color = TabCyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
          value = captainTitle,
          onValueChange = { if (it.length <= 60) captainTitle = it },
          label = { Text("Captain Title / Badge") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TabGold,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
          ),
          modifier = Modifier.fillMaxWidth().testTag("captain_title_input"),
        )
        if (eligibleMembers.isEmpty()) {
          Text("No active housemates joined in season yet.", color = TabMuted, style = MaterialTheme.typography.bodySmall)
        } else {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            eligibleMembers.forEach { m ->
              val picked = m.userId == pickedCaptainUid
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (picked) TabGold.copy(alpha = 0.22f) else TabElevated,
                border = BorderStroke(1.dp, if (picked) TabGold else Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.heightIn(min = 48.dp).clickable { pickedCaptainUid = m.userId },
              ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                  Text(m.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  Text(m.playerId, color = if (picked) TabGold else TabMuted, fontSize = 10.sp)
                }
              }
            }
          }
          val chosen = eligibleMembers.firstOrNull { it.userId == pickedCaptainUid }
          Button(
            onClick = { if (chosen != null) onAssignOrReplaceCaptain(chosen, captainTitle, currentPowers) },
            enabled = !isBusy && chosen != null,
            colors = ButtonDefaults.buttonColors(containerColor = TabGold, contentColor = Color(0xFF10141F)),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("assign_replace_captain_button"),
          ) {
            Text(
              if (season?.captainUid.isNullOrBlank()) "Assign Captain" else "Replace Current Captain",
              fontWeight = FontWeight.ExtraBold,
            )
          }
        }
      }
    }

    // Admin-Controlled Captain Powers
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = TabCard,
      border = BorderStroke(1.dp, TabGold.copy(alpha = 0.3f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("CAPTAIN POWERS & PRIVILEGES", color = TabGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        CaptainPowerSwitchRow("Existing VIP Room Access (Captain Room)", powerVipAccess) { powerVipAccess = it }
        CaptainPowerSwitchRow("Nomination & Eviction Immunity", powerImmunity) { powerImmunity = it }
        CaptainPowerSwitchRow("2x Eviction / Save Vote Weight", powerDoubleVote) { powerDoubleVote = it }
        CaptainPowerSwitchRow("Official Tie-Breaker Power", powerTieBreaker) { powerTieBreaker = it }
        Button(
          onClick = { onUpdateCaptainPowers(currentPowers) },
          enabled = !isBusy,
          colors = ButtonDefaults.buttonColors(containerColor = TabCyan, contentColor = Color(0xFF061622)),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_captain_powers_button"),
        ) {
          Text("Apply Captain Powers", fontWeight = FontWeight.ExtraBold)
        }
      }
    }

    // Separate VIP Room Access Manager (Independent of Captaincy)
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = TabCard,
      border = BorderStroke(1.dp, TabPurple.copy(alpha = 0.45f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.Diamond, contentDescription = null, tint = TabPurple, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("VIP ROOM PRIVILEGES (SEPARATE FROM CAPTAINCY)", color = TabPurple, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Text(
          "Grant VIP access to existing VIP Captain Room (X:10..19, Z:-22..-14) independently of Captain status.",
          color = TabMuted,
          style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
          value = vipLabelInput,
          onValueChange = { if (it.length <= 24) vipLabelInput = it },
          label = { Text("VIP Pass Label") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TabPurple,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
          ),
          modifier = Modifier.fillMaxWidth(),
        )

        if (eligibleMembers.isEmpty()) {
          Text("No players in season to manage VIP access.", color = TabMuted, style = MaterialTheme.typography.bodySmall)
        } else {
          eligibleMembers.forEach { m ->
            val hasVip = activeVipUids.containsKey(m.userId)
            val isCaptain = !season?.captainUid.isNullOrBlank() && season?.captainUid == m.userId
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (hasVip) TabPurple.copy(alpha = 0.14f) else TabElevated)
                .padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${if (isCaptain) "👑 " else ""}${if (hasVip) "💎 " else ""}${m.displayName}",
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "${m.playerId} • ${if (hasVip) activeVipUids[m.userId]?.vipLabel ?: "VIP PASS" else "Standard Access"}",
                  color = if (hasVip) TabPurple else TabMuted,
                  style = MaterialTheme.typography.labelSmall,
                )
              }
              OutlinedButton(
                onClick = { onSetPlayerVip(m, !hasVip, vipLabelInput) },
                enabled = !isBusy,
                border = BorderStroke(1.dp, if (hasVip) TabDanger else TabPurple),
                modifier = Modifier.heightIn(min = 48.dp).testTag("vip_toggle_${m.userId}"),
              ) {
                Text(
                  text = if (hasVip) "Revoke VIP" else "Grant VIP",
                  color = if (hasVip) TabDanger else TabPurple,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                )
              }
            }
          }
        }
      }
    }

    // Privilege Audit History
    if (privilegeHistory.isNotEmpty()) {
      Surface(shape = RoundedCornerShape(18.dp), color = TabCard) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("CAPTAIN & VIP PRIVILEGE HISTORY", color = TabCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          privilegeHistory.take(10).forEach { rec ->
            Text("• [${rec.displayEventType}] ${rec.displaySummary}", color = TabMuted, style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
  }
}

@Composable
private fun CaptainPowerSwitchRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(TabElevated)
      .padding(horizontal = 12.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
fun AdminGameAndSecurityTab(
  season: SeasonSession?,
  gameSettings: GameSettings?,
  adminSession: AdminSessionSnapshot?,
  adminAuditLogs: List<AdminAuditLog>,
  isBusy: Boolean,
  onSaveGameSettings: (GameSettings) -> Unit,
  onChangeAdminPassword: (current: String, next: String, confirm: String, onClear: () -> Unit) -> Unit,
  onAdminLogout: () -> Unit,
) {
  var houseTitle by remember(gameSettings?.houseTitle, season?.title) {
    mutableStateOf(gameSettings?.houseTitle ?: season?.title ?: "Season 01 • Bigg Boss – The House")
  }
  var defaultDuration by remember(gameSettings?.defaultVotingDurationMinutes) {
    mutableLongStateOf(gameSettings?.defaultVotingDurationMinutes ?: 15L)
  }
  var defaultCapVip by remember(gameSettings?.defaultCaptainVipAccess) {
    mutableStateOf(gameSettings?.defaultCaptainVipAccess ?: true)
  }
  var allowNpcDefault by remember(gameSettings?.allowNpcVotesByDefault) {
    mutableStateOf(gameSettings?.allowNpcVotesByDefault ?: false)
  }

  var currentPwd by remember { mutableStateOf("") }
  var newPwd by remember { mutableStateOf("") }
  var confirmPwd by remember { mutableStateOf("") }

  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Admin Session & Quick Logout
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = TabCard,
      border = BorderStroke(1.dp, TabCyan.copy(alpha = 0.4f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.Security, contentDescription = null, tint = TabCyan, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("ADMIN SESSION & CONTROL ACCESS", color = TabCyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        val expText = adminSession?.expiresAtMillis?.takeIf { it > 0L }?.let {
          DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it))
        } ?: "Active Local Session"
        val authModeLabel = if (adminSession?.isLocalOnly != false) "Local APK Auth" else "Firebase Admin Auth"
        Text("Operator: ${adminSession?.username ?: "BIGGBOSS"} • $authModeLabel • Expires: $expText", color = Color.White, style = MaterialTheme.typography.bodySmall)
        if (adminSession?.mustChangeInitialPassword == true) {
          Text(
            text = "Notice: Using initial default Admin password. Consider changing it below.",
            color = TabGold,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
          )
        }
        Text(
          text = "Local-only protection: Admin credentials & lockout persist locally on this device (cannot prevent APK reverse-engineering).",
          color = TabMuted,
          style = MaterialTheme.typography.labelSmall,
        )
        Button(
          onClick = onAdminLogout,
          colors = ButtonDefaults.buttonColors(containerColor = TabDanger, contentColor = Color(0xFF1F0808)),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_logout_button"),
        ) {
          Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Admin Logout & End Session", fontWeight = FontWeight.ExtraBold)
        }
      }
    }

    // Change Admin Password (Verified & Persisted Locally)
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = TabCard,
      border = BorderStroke(1.dp, TabGold.copy(alpha = 0.35f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.Lock, contentDescription = null, tint = TabGold, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("CHANGE ADMIN PASSWORD", color = TabGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
          value = currentPwd,
          onValueChange = { if (it.length <= 128) currentPwd = it },
          label = { Text("Current Admin Password") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TabGold,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
          ),
          modifier = Modifier.fillMaxWidth().testTag("admin_current_password_input"),
        )
        OutlinedTextField(
          value = newPwd,
          onValueChange = { if (it.length <= 128) newPwd = it },
          label = { Text("New Admin Password (8+ chars)") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TabGold,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
          ),
          modifier = Modifier.fillMaxWidth().testTag("admin_new_password_input"),
        )
        OutlinedTextField(
          value = confirmPwd,
          onValueChange = { if (it.length <= 128) confirmPwd = it },
          label = { Text("Confirm New Password") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TabGold,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
          ),
          modifier = Modifier.fillMaxWidth().testTag("admin_confirm_password_input"),
        )
        Button(
          onClick = {
            onChangeAdminPassword(currentPwd, newPwd, confirmPwd) {
              currentPwd = ""
              newPwd = ""
              confirmPwd = ""
            }
          },
          enabled = !isBusy && currentPwd.isNotEmpty() && newPwd.isNotEmpty(),
          colors = ButtonDefaults.buttonColors(containerColor = TabGold, contentColor = Color(0xFF10141F)),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("change_admin_password_button"),
        ) {
          Text("Update Admin Password", fontWeight = FontWeight.ExtraBold)
        }
      }
    }

    // House Game Settings
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = TabCard,
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.Settings, contentDescription = null, tint = TabEmerald, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("HOUSE GAME SETTINGS", color = TabEmerald, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
          value = houseTitle,
          onValueChange = { if (it.length <= 80) houseTitle = it },
          label = { Text("Season / House Display Title") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TabEmerald,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
          ),
          modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(5L, 15L, 30L).forEach { mins ->
            val picked = defaultDuration == mins
            OutlinedButton(
              onClick = { defaultDuration = mins },
              border = BorderStroke(1.dp, if (picked) TabEmerald else Color.White.copy(alpha = 0.16f)),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
              Text("${mins}m Default", color = if (picked) TabEmerald else TabMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
        CaptainPowerSwitchRow("Default Captain VIP Room Access", defaultCapVip) { defaultCapVip = it }
        CaptainPowerSwitchRow("Allow Optional NPC Votes by Default", allowNpcDefault) { allowNpcDefault = it }
        Button(
          onClick = {
            onSaveGameSettings(
              GameSettings(
                houseTitle = houseTitle,
                defaultVotingDurationMinutes = defaultDuration,
                defaultCaptainVipAccess = defaultCapVip,
                allowNpcVotesByDefault = allowNpcDefault,
              )
            )
          },
          enabled = !isBusy,
          colors = ButtonDefaults.buttonColors(containerColor = TabEmerald, contentColor = Color(0xFF06281E)),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_game_settings_button"),
        ) {
          Text("Save Game Settings", fontWeight = FontWeight.ExtraBold)
        }
      }
    }

    // Recent Admin Audit Logs
    if (adminAuditLogs.isNotEmpty()) {
      Surface(shape = RoundedCornerShape(18.dp), color = TabCard) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("ADMIN SECURITY & ACTION AUDIT LOGS", color = TabCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          adminAuditLogs.take(12).forEach { entry ->
            Text("• [${entry.actionType}] ${entry.summary}", color = TabMuted, style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
  }
}
