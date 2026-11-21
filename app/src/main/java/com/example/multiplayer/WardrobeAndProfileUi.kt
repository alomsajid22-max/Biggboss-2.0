package com.example.multiplayer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val StudioSurface = Color(0xFF0F1629)
private val StudioCard = Color(0xFF172038)
private val StudioGold = Color(0xFFEAB954)
private val StudioCyan = Color(0xFF22D3EE)
private val StudioEmerald = Color(0xFF34D399)
private val StudioMuted = Color(0xFF94A3B8)

private val COLOR_SWATCHES_TOP = listOf("#1F7A8C", "#B91C1C", "#D97706", "#7C3AED", "#059669", "#2563EB", "#E11D48", "#F8FAFC")
private val COLOR_SWATCHES_PANTS = listOf("#1E2433", "#18181B", "#334155", "#0F172A", "#292524", "#1E1B4B", "#F1F5F9")
private val COLOR_SWATCHES_HAIR = listOf("#261C17", "#111111", "#7C2D12", "#D97706", "#E2E8F0", "#451A03")
private val COLOR_SWATCHES_SKIN = listOf("#F1C29B", "#DCA47F", "#C68642", "#8D5524", "#6C3D22", "#F6D5B8")

private fun hexColorSafe(hex: String): Color =
  runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(StudioGold)

@Composable
fun WardrobeStudioDialog(
  currentAppearance: WardrobeAppearance,
  onApplyWardrobe: (WardrobeAppearance) -> Unit,
  onDismiss: () -> Unit,
) {
  var draft by remember(currentAppearance) { mutableStateOf(currentAppearance) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = StudioSurface,
      border = BorderStroke(1.dp, StudioGold.copy(alpha = 0.45f)),
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .widthIn(max = 560.dp)
        .heightIn(max = 720.dp)
        .padding(10.dp)
        .testTag("wardrobe_studio_dialog"),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Checkroom, contentDescription = null, tint = StudioGold)
            Spacer(Modifier.width(8.dp))
            Column {
              Text("CHARACTER WARDROBE", color = StudioGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
              Text("Live 3D Outfit Studio", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Close Wardrobe", tint = Color.White)
          }
        }

        Column(
          modifier = Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          // Live Preview Card
          Surface(shape = RoundedCornerShape(14.dp), color = StudioCard, border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.4f))) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column {
                Text("PREVIEW BEFORE APPLY", color = StudioCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("${draft.outfitStyle.uppercase()} • Hair: ${draft.hairStyle} • Acc: ${draft.accessory}", color = Color.White, fontWeight = FontWeight.Bold)
              }
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(draft.topHex, draft.pantsHex, draft.accentHex, draft.hairColorHex).forEach { hex ->
                  Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(hexColorSafe(hex)).border(1.dp, Color.White, CircleShape))
                }
              }
            }
          }

          // Presets
          Text("QUICK STUDIO PRESETS", color = StudioMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WardrobeAppearance.PRESETS.forEach { (label, preset) ->
              OutlinedButton(
                onClick = { draft = preset },
                border = BorderStroke(1.dp, StudioGold.copy(alpha = 0.5f)),
                modifier = Modifier.heightIn(min = 44.dp),
              ) { Text(label, color = StudioGold, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
          }

          ColorSwatchRow("Top Color", COLOR_SWATCHES_TOP, draft.topHex) { draft = draft.copy(topHex = it) }
          ColorSwatchRow("Pants Color", COLOR_SWATCHES_PANTS, draft.pantsHex) { draft = draft.copy(pantsHex = it) }
          ColorSwatchRow("Shoes Color", COLOR_SWATCHES_TOP, draft.shoesHex) { draft = draft.copy(shoesHex = it) }
          ColorSwatchRow("Hair Color", COLOR_SWATCHES_HAIR, draft.hairColorHex) { draft = draft.copy(hairColorHex = it) }
          ColorSwatchRow("Skin Tone", COLOR_SWATCHES_SKIN, draft.skinToneHex) { draft = draft.copy(skinToneHex = it) }

          Text("OUTFIT STYLE", color = StudioMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WardrobeAppearance.OUTFIT_STYLES.forEach { st ->
              val picked = draft.outfitStyle == st
              OutlinedButton(
                onClick = { draft = draft.copy(outfitStyle = st) },
                border = BorderStroke(1.dp, if (picked) StudioCyan else Color.White.copy(alpha = 0.16f)),
                modifier = Modifier.heightIn(min = 44.dp),
              ) { Text(st.uppercase(), color = if (picked) StudioCyan else StudioMuted, fontSize = 11.sp) }
            }
          }

          Text("HAIR STYLE", color = StudioMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WardrobeAppearance.HAIR_STYLES.forEach { hs ->
              val picked = draft.hairStyle == hs
              OutlinedButton(
                onClick = { draft = draft.copy(hairStyle = hs) },
                border = BorderStroke(1.dp, if (picked) StudioGold else Color.White.copy(alpha = 0.16f)),
                modifier = Modifier.heightIn(min = 44.dp),
              ) { Text(hs, color = if (picked) StudioGold else StudioMuted, fontSize = 11.sp) }
            }
          }

          Text("ACCESSORY", color = StudioMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WardrobeAppearance.ACCESSORIES.forEach { acc ->
              val picked = draft.accessory == acc
              OutlinedButton(
                onClick = { draft = draft.copy(accessory = acc, glasses = acc == "glasses" || draft.glasses) },
                border = BorderStroke(1.dp, if (picked) StudioEmerald else Color.White.copy(alpha = 0.16f)),
                modifier = Modifier.heightIn(min = 44.dp),
              ) { Text(acc.uppercase(), color = if (picked) StudioEmerald else StudioMuted, fontSize = 11.sp) }
            }
          }
        }

        Button(
          onClick = {
            onApplyWardrobe(draft)
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = StudioGold, contentColor = Color(0xFF10141F)),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("apply_wardrobe_button"),
        ) { Text("Apply Outfit & Save to 3D House", fontWeight = FontWeight.ExtraBold) }
      }
    }
  }
}

@Composable
private fun ColorSwatchRow(
  label: String,
  swatches: List<String>,
  selectedHex: String,
  onPick: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(label.uppercase(), color = StudioMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      swatches.forEach { hex ->
        val picked = selectedHex.equals(hex, ignoreCase = true)
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(hexColorSafe(hex))
            .border(if (picked) 3.dp else 1.dp, if (picked) StudioCyan else Color.White.copy(alpha = 0.45f), CircleShape)
            .clickable { onPick(hex) },
        )
      }
    }
  }
}

@Composable
fun PlayerExtendedProfileDialog(
  currentUserId: String,
  account: UserAccount,
  season: SeasonSession?,
  profiles: List<PlayerExtendedProfile>,
  cameraPrefs: CameraPreferences,
  finaleState: FinaleCeremonyState,
  clockState: HouseClockState,
  onSaveCameraPrefs: (CameraPreferences) -> Unit,
  onDismiss: () -> Unit,
) {
  val myProfile = profiles.firstOrNull { it.userId == currentUserId }
  var camDraft by remember(cameraPrefs) { mutableStateOf(cameraPrefs) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = StudioSurface,
      border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.45f)),
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .widthIn(max = 580.dp)
        .heightIn(max = 730.dp)
        .padding(10.dp)
        .testTag("player_extended_profile_dialog"),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = StudioCyan)
            Spacer(Modifier.width(8.dp))
            Column {
              Text("HOUSE PROFILE • STATS & CAMERA", color = StudioCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
              Text("${account.displayName} (${account.playerId})", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Close Profile", tint = Color.White)
          }
        }

        Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          // House Info & Grand Finale Banner
          Surface(shape = RoundedCornerShape(14.dp), color = StudioCard) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("HOUSE INFO • BIGG BOSS – THE HOUSE", color = StudioGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              Text("Season: ${season?.title ?: "Season 01"} • Status: ${season?.status ?: "LIVE"}", color = Color.White, fontSize = 12.sp)
              Text("Clock: ${clockState.formattedClock()} • Weather: ${clockState.weather}", color = StudioCyan, fontSize = 12.sp)
              Text("Captain: ${season?.captainDisplayName ?: "Vacant"}", color = StudioEmerald, fontSize = 12.sp)
            }
          }

          if (finaleState.active) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF28200B), border = BorderStroke(1.dp, StudioGold)) {
              Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = StudioGold, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                  Text(finaleState.trophyTitle, color = StudioGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                  Text("Winner: ${finaleState.winnerName}", color = Color.White, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          // My Stats & Badges
          myProfile?.let { prof ->
            Surface(shape = RoundedCornerShape(14.dp), color = StudioCard) {
              Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("MY STATS & BADGES", color = StudioCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                Text("Rank #${prof.rank} • Points: ${prof.pointsTotal} • Tasks Won: ${prof.tasksWon} • Captaincies: ${prof.captainCount}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  prof.badges.forEach { badge ->
                    Surface(shape = RoundedCornerShape(99.dp), color = StudioGold.copy(alpha = 0.18f)) {
                      Text(badge, color = StudioGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                  }
                }
                prof.activityHistory.forEach { act -> Text("• $act", color = StudioMuted, fontSize = 11.sp) }
              }
            }
          }

          // Leaderboard
          Surface(shape = RoundedCornerShape(14.dp), color = StudioCard) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("SEASON LEADERBOARD (${profiles.size})", color = StudioGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
              if (profiles.isEmpty()) {
                Text("No ranked players active yet.", color = StudioMuted, fontSize = 12.sp)
              } else {
                profiles.take(10).forEach { p ->
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("#${p.rank} ${p.displayName}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("${p.pointsTotal} pts • ${p.tasksWon}W", color = StudioGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  }
                }
              }
            }
          }

          // Phase 20G Camera Controls & Saved Preferences
          Surface(shape = RoundedCornerShape(14.dp), color = StudioCard, border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.4f))) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = StudioCyan)
                Spacer(Modifier.width(6.dp))
                Text("3D CAMERA CONTROLS", color = StudioCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
              }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("THIRD_PERSON" to "3rd Person", "FIRST_PERSON" to "1st Person", "FREE" to "Free Cam").forEach { (m, lbl) ->
                  val picked = camDraft.mode == m
                  OutlinedButton(
                    onClick = { camDraft = camDraft.copy(mode = m) },
                    border = BorderStroke(1.dp, if (picked) StudioCyan else Color.White.copy(alpha = 0.16f)),
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp).testTag("cam_mode_$m"),
                  ) { Text(lbl, color = if (picked) StudioCyan else StudioMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
              }
              Text("Zoom Distance: ${"%.1f".format(camDraft.distance)}m", color = Color.White, fontSize = 12.sp)
              Slider(
                value = camDraft.distance,
                onValueChange = { camDraft = camDraft.copy(distance = it) },
                valueRange = 2.0f..8.5f,
                colors = SliderDefaults.colors(thumbColor = StudioCyan, activeTrackColor = StudioCyan),
              )
              Text("Look Sensitivity: ${"%.1f".format(camDraft.sensitivity)}x", color = Color.White, fontSize = 12.sp)
              Slider(
                value = camDraft.sensitivity,
                onValueChange = { camDraft = camDraft.copy(sensitivity = it) },
                valueRange = 0.4f..2.0f,
                colors = SliderDefaults.colors(thumbColor = StudioGold, activeTrackColor = StudioGold),
              )
              Button(
                onClick = { onSaveCameraPrefs(camDraft) },
                colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = Color(0xFF061622)),
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("save_camera_prefs_btn"),
              ) { Text("Save Camera Settings", fontWeight = FontWeight.ExtraBold) }
            }
          }
        }
      }
    }
  }
}
