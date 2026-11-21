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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val PhoneBezel = Color(0xFF060913)
private val PhoneScreen = Color(0xFF0C1222)
private val PhoneCard = Color(0xFF151E33)
private val PhoneElevated = Color(0xFF1C2742)
private val PhoneGold = Color(0xFFEAB954)
private val PhoneCyan = Color(0xFF22D3EE)
private val PhoneEmerald = Color(0xFF34D399)
private val PhonePink = Color(0xFFF43F5E)
private val PhoneViolet = Color(0xFFA855F7)
private val PhoneMuted = Color(0xFF94A3B8)

enum class AdminPhoneApp(val title: String, val icon: ImageVector, val accent: Color) {
  HOME("Home", Icons.Rounded.PhoneAndroid, PhoneGold),
  BIGGGRAM("BiggGram", Icons.Rounded.PhotoCamera, PhonePink),
  NOMINATIONS("Nominations", Icons.Rounded.Groups, PhoneCyan),
  NPC_VOTING("NPC Results", Icons.Rounded.HowToVote, PhoneEmerald),
  PLAYERS("Players", Icons.Rounded.Groups, PhoneGold),
  CCTV("24-Cam CCTV", Icons.Rounded.Videocam, PhoneCyan),
  ANNOUNCEMENTS("Speakers & Alert", Icons.Rounded.Campaign, PhonePink),
  EVENTS_CLOCK("Clock & Finale", Icons.Rounded.Celebration, PhoneViolet),
  NOTIFICATIONS("Notifications", Icons.Rounded.Notifications, PhoneGold),
}

private fun parseColorSafe(hex: String, fallback: Color = PhoneGold): Color {
  return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
}

@Composable
fun AdminPersonalPhoneDialog(
  isAdminVerified: Boolean,
  members: List<SeasonMember>,
  allUsers: List<UserAccount>,
  activeCycle: VotingCycle?,
  nominations: List<NominationBallot>,
  npcProfiles: List<BiggGramNpcProfile>,
  biggGramPosts: List<BiggGramPost>,
  biggGramStories: List<BiggGramStory>,
  npcVotes: List<NpcVoteRecord>,
  notifications: List<AdminPhoneNotification>,
  clockState: HouseClockState,
  celebrations: List<HouseCelebrationEvent>,
  finaleState: FinaleCeremonyState,
  moderationFlags: Map<String, PlayerModerationFlags>,
  cctvOnlineMap: Map<String, Boolean>,
  actionMessage: String?,
  actionError: String?,
  onDismiss: () -> Unit,
  onClearFeedback: () -> Unit,
  onToggleLikePost: (String) -> Unit,
  onAddComment: (postId: String, commentText: String) -> Unit,
  onCreatePost: (caption: String) -> Unit,
  onMarkStoryViewed: (String) -> Unit,
  onPublishNomineesToBiggGram: (VotingCycle, List<NomineeSummary>) -> Unit,
  onRunAllNpcVotes: (VotingCycle) -> Unit,
  onCastSingleNpcVote: (VotingCycle, BiggGramNpcProfile, NomineeSummary) -> Unit,
  onToggleCctvOnline: (String) -> Unit,
  onOpen3dCctvMatrix: () -> Unit,
  onSendTtsAnnouncement: (String, String, VoiceGender) -> Unit,
  onTriggerEmergencySiren: (Boolean, String) -> Unit,
  onSetHouseClock: (Int, Int, String, Boolean) -> Unit,
  onSetWeather: (String) -> Unit,
  onTogglePartyMode: (Boolean) -> Unit,
  onTriggerCelebration: (CelebrationType, String, String, Boolean) -> Unit,
  onTriggerFinale: (SeasonMember, String, String) -> Unit,
  onAssignSecretTask: (SeasonMember, String, String, Long) -> Unit,
  onSetPlayerMuted: (String, Boolean, String) -> Unit,
  onSetPlayerTaskRestricted: (String, Boolean, String) -> Unit,
  onModeratePlayer: (String, String, String) -> Unit,
  onMarkNotificationsRead: () -> Unit,
) {
  if (!isAdminVerified) return

  var currentApp by remember { mutableStateOf(AdminPhoneApp.HOME) }
  var pendingConfirmAction by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(34.dp),
      color = PhoneBezel,
      border = BorderStroke(3.dp, PhoneGold.copy(alpha = 0.65f)),
      tonalElevation = 16.dp,
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .widthIn(max = 520.dp)
        .heightIn(max = 760.dp)
        .padding(8.dp)
        .testTag("admin_personal_phone_dialog"),
    ) {
      Column(
        modifier = Modifier
          .padding(10.dp)
          .clip(RoundedCornerShape(26.dp))
          .background(PhoneScreen),
      ) {
        // Smartphone Status Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080D1A))
            .padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (currentApp != AdminPhoneApp.HOME) {
              IconButton(
                onClick = { currentApp = AdminPhoneApp.HOME },
                modifier = Modifier.size(36.dp).testTag("admin_phone_back_home"),
              ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Phone Home", tint = Color.White)
              }
              Spacer(Modifier.width(4.dp))
            }
            Column {
              Text(
                text = "BOSS PHONE • ${currentApp.title.uppercase()}",
                color = PhoneGold,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
              )
              Text(
                text = "${clockState.formattedClock()} • Secret Control Room",
                color = PhoneMuted,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(40.dp).testTag("admin_phone_close"),
          ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close Admin Phone", tint = Color.White)
          }
        }

        if (actionMessage != null || actionError != null) {
          Surface(
            color = if (actionError != null) PhonePink.copy(alpha = 0.18f) else PhoneEmerald.copy(alpha = 0.16f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = actionError ?: actionMessage.orEmpty(),
                color = if (actionError != null) Color(0xFFFCA5A5) else PhoneEmerald,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
              )
              TextButton(onClick = onClearFeedback) { Text("OK", color = Color.White, fontSize = 11.sp) }
            }
          }
        }

        Column(
          modifier = Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          when (currentApp) {
            AdminPhoneApp.HOME -> PhoneHomeScreen(
              notificationsCount = notifications.count { !it.read },
              activeCycle = activeCycle,
              npcVotesCount = npcVotes.count { it.cycleId == activeCycle?.id },
              onlineCams = cctvOnlineMap.values.count { it },
              onOpenApp = { app ->
                if (app == AdminPhoneApp.NOTIFICATIONS) onMarkNotificationsRead()
                currentApp = app
              },
            )
            AdminPhoneApp.BIGGGRAM -> BiggGramAppScreen(
              posts = biggGramPosts,
              stories = biggGramStories,
              npcProfiles = npcProfiles,
              activeCycle = activeCycle,
              npcVotes = npcVotes,
              onToggleLike = onToggleLikePost,
              onAddComment = onAddComment,
              onCreatePost = onCreatePost,
              onMarkStoryViewed = onMarkStoryViewed,
              onRunAllNpcVotes = onRunAllNpcVotes,
              onCastSingleNpcVote = onCastSingleNpcVote,
            )
            AdminPhoneApp.NOMINATIONS -> PhoneNominationsAppScreen(
              activeCycle = activeCycle,
              nominations = nominations,
              members = members,
              onPublishNomineesToBiggGram = onPublishNomineesToBiggGram,
            )
            AdminPhoneApp.NPC_VOTING -> PhoneNpcResultsAppScreen(
              activeCycle = activeCycle,
              npcProfiles = npcProfiles,
              npcVotes = npcVotes,
              onRunAllNpcVotes = onRunAllNpcVotes,
              onCastSingleNpcVote = onCastSingleNpcVote,
            )
            AdminPhoneApp.PLAYERS -> PhonePlayersAppScreen(
              members = members,
              allUsers = allUsers,
              moderationFlags = moderationFlags,
              onSetMuted = onSetPlayerMuted,
              onSetTaskRestricted = onSetPlayerTaskRestricted,
              onAssignSecretTask = onAssignSecretTask,
              onRequestConfirmModeration = { label, block ->
                pendingConfirmAction = label to block
              },
              onModerate = onModeratePlayer,
            )
            AdminPhoneApp.CCTV -> PhoneCctvAppScreen(
              cctvOnlineMap = cctvOnlineMap,
              onToggleCamera = onToggleCctvOnline,
              onOpenMatrix = {
                onDismiss()
                onOpen3dCctvMatrix()
              },
            )
            AdminPhoneApp.ANNOUNCEMENTS -> PhoneAnnouncementsAppScreen(
              emergencyActive = clockState.emergencySirenActive,
              onSendTts = onSendTtsAnnouncement,
              onTriggerSiren = onTriggerEmergencySiren,
            )
            AdminPhoneApp.EVENTS_CLOCK -> PhoneEventsClockAppScreen(
              clockState = clockState,
              celebrations = celebrations,
              finaleState = finaleState,
              members = members,
              onSetHouseClock = onSetHouseClock,
              onSetWeather = onSetWeather,
              onTogglePartyMode = onTogglePartyMode,
              onTriggerCelebration = onTriggerCelebration,
              onTriggerFinale = { winner, runnerUp, title ->
                pendingConfirmAction = "Declare ${winner.displayName} Grand Finale Winner?" to {
                  onTriggerFinale(winner, runnerUp, title)
                }
              },
            )
            AdminPhoneApp.NOTIFICATIONS -> PhoneNotificationsAppScreen(notifications = notifications)
          }
        }
      }
    }
  }

  pendingConfirmAction?.let { (label, runAction) ->
    AlertDialog(
      onDismissRequest = { pendingConfirmAction = null },
      containerColor = PhoneCard,
      titleContentColor = Color.White,
      textContentColor = PhoneMuted,
      title = { Text("Confirm Admin Action", fontWeight = FontWeight.Bold) },
      text = { Text(label) },
      confirmButton = {
        Button(
          onClick = {
            runAction()
            pendingConfirmAction = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = PhoneGold, contentColor = Color(0xFF10141F)),
        ) { Text("Confirm", fontWeight = FontWeight.Bold) }
      },
      dismissButton = {
        TextButton(onClick = { pendingConfirmAction = null }) { Text("Cancel", color = PhoneMuted) }
      },
    )
  }
}

@Composable
private fun PhoneHomeScreen(
  notificationsCount: Int,
  activeCycle: VotingCycle?,
  npcVotesCount: Int,
  onlineCams: Int,
  onOpenApp: (AdminPhoneApp) -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = PhoneCard,
    border = BorderStroke(1.dp, PhoneGold.copy(alpha = 0.35f)),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text("OWNER EXCLUSIVE DEVICE", color = PhoneGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
      Text(
        "Real players have NO phone, NO BiggGram & NO BiggGram voting access. All house authority routes through this console.",
        color = Color.White,
        style = MaterialTheme.typography.bodySmall,
      )
      Text(
        "Active Cycle: ${activeCycle?.title ?: "None"} (${activeCycle?.status ?: "IDLE"}) • NPC Votes: $npcVotesCount • Cams Online: $onlineCams/24",
        color = PhoneCyan,
        style = MaterialTheme.typography.labelSmall,
      )
    }
  }

  val apps = AdminPhoneApp.entries.filter { it != AdminPhoneApp.HOME }
  apps.chunked(2).forEach { rowApps ->
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      rowApps.forEach { app ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = PhoneCard,
          border = BorderStroke(1.dp, app.accent.copy(alpha = 0.42f)),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 82.dp)
            .clickable { onOpenApp(app) }
            .testTag("phone_app_${app.name}"),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(app.icon, contentDescription = app.title, tint = app.accent, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            val label = if (app == AdminPhoneApp.NOTIFICATIONS && notificationsCount > 0) {
              "${app.title} ($notificationsCount)"
            } else app.title
            Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          }
        }
      }
      if (rowApps.size == 1) Spacer(Modifier.weight(1f))
    }
  }
}

@Composable
private fun BiggGramAppScreen(
  posts: List<BiggGramPost>,
  stories: List<BiggGramStory>,
  npcProfiles: List<BiggGramNpcProfile>,
  activeCycle: VotingCycle?,
  npcVotes: List<NpcVoteRecord>,
  onToggleLike: (String) -> Unit,
  onAddComment: (String, String) -> Unit,
  onCreatePost: (String) -> Unit,
  onMarkStoryViewed: (String) -> Unit,
  onRunAllNpcVotes: (VotingCycle) -> Unit,
  onCastSingleNpcVote: (VotingCycle, BiggGramNpcProfile, NomineeSummary) -> Unit,
) {
  var selectedStory by remember { mutableStateOf<BiggGramStory?>(null) }
  var newPostText by remember { mutableStateOf("") }
  var activeFilter by remember { mutableStateOf("ALL") }
  var commentDrafts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
  var pickedNpcIdx by remember { mutableIntStateOf(0) }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0xFF1A1025),
    border = BorderStroke(1.dp, PhonePink.copy(alpha = 0.55f)),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text("BIGGGRAM • NPC SOCIAL NETWORK", color = PhonePink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
      Text("${npcProfiles.size} AI Housemate Profiles Active • Exclusively Admin-Observed", color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
  }

  // Stories Ring Strip
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    stories.forEach { story ->
      val ringColor = if (story.viewed) PhoneMuted else PhonePink
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .width(64.dp)
          .clickable {
            selectedStory = story
            onMarkStoryViewed(story.id)
          }
          .testTag("bigggram_story_${story.npcId}"),
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(parseColorSafe(story.avatarHex))
            .border(2.dp, ringColor, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Text(story.displayName.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(3.dp))
        Text(story.displayName, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
  }

  selectedStory?.let { story ->
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = PhoneElevated,
      border = BorderStroke(1.dp, PhonePink),
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("${story.displayName} (${story.handle}) • STORY", color = PhonePink, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          Text("Close", color = PhoneCyan, modifier = Modifier.clickable { selectedStory = null }, fontSize = 11.sp)
        }
        Text(story.headline, color = Color.White, fontWeight = FontWeight.Bold)
        Text(story.caption, color = PhoneMuted, style = MaterialTheme.typography.bodySmall)
      }
    }
  }

  // Create official BiggGram post
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    OutlinedTextField(
      value = newPostText,
      onValueChange = { if (it.length <= 280) newPostText = it },
      placeholder = { Text("Post Bigg Boss update to BiggGram...") },
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PhonePink,
        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
      ),
      modifier = Modifier.weight(1f),
    )
    Button(
      onClick = {
        onCreatePost(newPostText)
        newPostText = ""
      },
      enabled = newPostText.isNotBlank(),
      colors = ButtonDefaults.buttonColors(containerColor = PhonePink, contentColor = Color.White),
      modifier = Modifier.height(48.dp),
    ) { Text("Post", fontWeight = FontWeight.Bold) }
  }

  // Quick Filter Chips
  Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    listOf("ALL", "NOMINATION_POLL", "DRAMA", "FINALE", "LIFESTYLE").forEach { cat ->
      val picked = activeFilter == cat
      OutlinedButton(
        onClick = { activeFilter = cat },
        border = BorderStroke(1.dp, if (picked) PhonePink else Color.White.copy(alpha = 0.16f)),
        modifier = Modifier.heightIn(min = 40.dp),
      ) {
        Text(cat.replace("_", " "), color = if (picked) PhonePink else PhoneMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
      }
    }
  }

  // Active Nomination Poll NPC Voting Controls
  if (activeCycle != null && activeCycle.status == "OPEN" && activeCycle.parsedNominees().isNotEmpty()) {
    val nominees = activeCycle.parsedNominees()
    val cycleNpcVotes = npcVotes.filter { it.cycleId == activeCycle.id }
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = Color(0xFF0E2920),
      border = BorderStroke(1.dp, PhoneEmerald.copy(alpha = 0.6f)),
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("LIVE NPC VOTING ON BIGGGRAM (${cycleNpcVotes.size}/${npcProfiles.size} VOTED)", color = PhoneEmerald, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
        val pickedNpc = npcProfiles[pickedNpcIdx.coerceIn(0, npcProfiles.lastIndex)]
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("Voter: ${pickedNpc.name} (${pickedNpc.handle})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          OutlinedButton(
            onClick = { pickedNpcIdx = (pickedNpcIdx + 1) % npcProfiles.size },
            modifier = Modifier.heightIn(min = 38.dp),
          ) { Text("Next NPC", color = PhoneCyan, fontSize = 11.sp) }
        }
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          nominees.forEach { nom ->
            Button(
              onClick = { onCastSingleNpcVote(activeCycle, pickedNpc, nom) },
              colors = ButtonDefaults.buttonColors(containerColor = PhoneEmerald, contentColor = Color(0xFF06281E)),
              modifier = Modifier.heightIn(min = 42.dp).testTag("bigggram_vote_${nom.uid}"),
            ) { Text("Vote ${nom.displayName}", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
          }
        }
        Button(
          onClick = { onRunAllNpcVotes(activeCycle) },
          colors = ButtonDefaults.buttonColors(containerColor = PhoneGold, contentColor = Color(0xFF10141F)),
          modifier = Modifier.fillMaxWidth().height(44.dp).testTag("bigggram_run_all_npc_votes"),
        ) { Text("Cast Single Vote For All Remaining NPCs", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }
      }
    }
  }

  val filtered = if (activeFilter == "ALL") posts else posts.filter { it.category == activeFilter }
  filtered.forEach { post ->
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = PhoneCard,
      border = BorderStroke(1.dp, if (post.category == "NOMINATION_POLL") PhoneCyan.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.1f)),
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(parseColorSafe(post.authorAvatarHex)),
            contentAlignment = Alignment.Center,
          ) { Text(post.authorName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold) }
          Spacer(Modifier.width(8.dp))
          Column {
            Text(post.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("${post.authorHandle} • ${post.category}", color = PhoneMuted, fontSize = 10.sp)
          }
        }
        Text(post.caption, color = Color.White, style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          IconButton(onClick = { onToggleLike(post.id) }, modifier = Modifier.size(38.dp)) {
            Icon(
              imageVector = if (post.likedByAdmin) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
              contentDescription = "Like Post",
              tint = if (post.likedByAdmin) PhonePink else PhoneMuted,
            )
          }
          Text("${post.likes} likes • ${post.comments.size} comments", color = PhoneMuted, fontSize = 11.sp)
        }
        post.comments.takeLast(4).forEach { c ->
          Text("💬 ${c.authorHandle}: ${c.text}", color = PhoneMuted, fontSize = 11.sp)
        }
        val draft = commentDrafts[post.id].orEmpty()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = draft,
            onValueChange = { txt -> commentDrafts = commentDrafts + (post.id to txt.take(160)) },
            placeholder = { Text("Comment as Bigg Boss...", fontSize = 11.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = PhoneCyan,
              unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
            ),
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = {
              if (draft.isNotBlank()) {
                onAddComment(post.id, draft)
                commentDrafts = commentDrafts - post.id
              }
            },
            modifier = Modifier.size(44.dp),
          ) {
            Icon(Icons.Rounded.Send, contentDescription = "Send Comment", tint = PhoneCyan)
          }
        }
      }
    }
  }
}

@Composable
private fun PhoneNominationsAppScreen(
  activeCycle: VotingCycle?,
  nominations: List<NominationBallot>,
  members: List<SeasonMember>,
  onPublishNomineesToBiggGram: (VotingCycle, List<NomineeSummary>) -> Unit,
) {
  val tallies = remember(nominations, members) { Phase10VotingLogic.tallyNominations(nominations, members) }
  Surface(shape = RoundedCornerShape(14.dp), color = PhoneCard) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("NOMINATION COLLECTOR", color = PhoneCyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
      Text("Real players nominate housemates in-game. Main Admin reviews and publishes final nominees to BiggGram for NPC Voting.", color = PhoneMuted, style = MaterialTheme.typography.bodySmall)
      if (tallies.isEmpty()) {
        Text("No active housemates found yet.", color = PhoneMuted, style = MaterialTheme.typography.bodySmall)
      } else {
        tallies.forEach { t ->
          Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PhoneElevated).padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("${t.displayName} (${t.playerId})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("${t.nominationCount} peer noms", color = PhoneGold, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
          }
        }
      }
      if (activeCycle != null) {
        val suggested = tallies.filter { it.nominationCount > 0 }.take(5).ifEmpty { tallies.take(4) }
          .map { NomineeSummary(it.uid, it.playerId, it.displayName, it.characterId) }
        Button(
          onClick = { onPublishNomineesToBiggGram(activeCycle, suggested) },
          enabled = suggested.isNotEmpty(),
          colors = ButtonDefaults.buttonColors(containerColor = PhonePink, contentColor = Color.White),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("phone_publish_nominees_bigggram"),
        ) {
          Text("Publish (${suggested.size}) Nominees to BiggGram", fontWeight = FontWeight.ExtraBold)
        }
      }
    }
  }
}

@Composable
private fun PhoneNpcResultsAppScreen(
  activeCycle: VotingCycle?,
  npcProfiles: List<BiggGramNpcProfile>,
  npcVotes: List<NpcVoteRecord>,
  onRunAllNpcVotes: (VotingCycle) -> Unit,
  onCastSingleNpcVote: (VotingCycle, BiggGramNpcProfile, NomineeSummary) -> Unit,
) {
  if (activeCycle == null || activeCycle.parsedNominees().isEmpty()) {
    Surface(shape = RoundedCornerShape(14.dp), color = PhoneCard) {
      Text("Start or open a nomination/voting round first to supervise NPC votes on BiggGram.", color = PhoneMuted, modifier = Modifier.padding(14.dp))
    }
    return
  }
  val nominees = activeCycle.parsedNominees()
  val outcome = remember(activeCycle.id, nominees, npcVotes) {
    BiggGramAndNpcVotingEngine.calculateNpcVotingOutcome(activeCycle.id, nominees, npcVotes)
  }

  Surface(shape = RoundedCornerShape(14.dp), color = PhoneCard, border = BorderStroke(1.dp, PhoneEmerald.copy(alpha = 0.5f))) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("NPC-ONLY VOTING & LIVE RESULTS", color = PhoneEmerald, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
      Text("Cycle: ${activeCycle.title} (${activeCycle.status}) • Total Valid NPC Votes: ${outcome.totalValidNpcVotes}/${npcProfiles.size}", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
      outcome.highestNominee?.let { hi ->
        Text("Highest Voted: ${hi.nominee.displayName} (${hi.voteCount} votes • ${"%.1f".format(hi.percentage)}%)", color = PhoneGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
      }
      outcome.lowestNominee?.let { lo ->
        Text("Lowest Voted: ${lo.nominee.displayName} (${lo.voteCount} votes • ${"%.1f".format(lo.percentage)}%)", color = PhoneCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
      }
      if (outcome.isTieAtTop) {
        Text("⚠️ TIE DETECTED AT TOP!", color = PhonePink, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
      }
      outcome.rows.forEach { row ->
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PhoneElevated).padding(10.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(row.nominee.displayName, color = Color.White, fontWeight = FontWeight.Bold)
            Text("${row.voteCount} votes (${"%.1f".format(row.percentage)}%)", color = PhoneGold, fontWeight = FontWeight.Bold)
          }
          Spacer(Modifier.height(4.dp))
          LinearProgressIndicator(
            progress = { (row.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
            color = PhoneEmerald,
            trackColor = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)),
          )
        }
      }
      if (activeCycle.status == "OPEN") {
        Button(
          onClick = { onRunAllNpcVotes(activeCycle) },
          colors = ButtonDefaults.buttonColors(containerColor = PhoneEmerald, contentColor = Color(0xFF06281E)),
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Run All Remaining NPC Votes (1 Each Max)", fontWeight = FontWeight.ExtraBold) }
      }
    }
  }
}

@Composable
private fun PhonePlayersAppScreen(
  members: List<SeasonMember>,
  allUsers: List<UserAccount>,
  moderationFlags: Map<String, PlayerModerationFlags>,
  onSetMuted: (String, Boolean, String) -> Unit,
  onSetTaskRestricted: (String, Boolean, String) -> Unit,
  onAssignSecretTask: (SeasonMember, String, String, Long) -> Unit,
  onRequestConfirmModeration: (String, () -> Unit) -> Unit,
  onModerate: (String, String, String) -> Unit,
) {
  val combined = remember(members, allUsers) {
    val map = LinkedHashMap<String, SeasonMember>()
    members.filter { it.role != PlayerRole.ADMIN.name }.forEach { map[it.userId] = it }
    allUsers.filter { it.role != PlayerRole.ADMIN.name }.forEach { u ->
      if (!map.containsKey(u.userId)) {
        map[u.userId] = SeasonMember(
          userId = u.userId,
          playerId = u.playerId,
          displayName = u.displayName,
          characterId = u.characterId,
          role = u.role,
          status = if (u.banned) "BANNED" else "REGISTERED",
          online = false,
        )
      }
    }
    map.values.toList()
  }
  Text("ALL PLAYERS (${combined.size})", color = PhoneGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
  if (combined.isEmpty()) {
    Text("No registered players yet.", color = PhoneMuted)
  } else {
    combined.forEach { m ->
      val flags = moderationFlags[m.userId] ?: PlayerModerationFlags(m.userId)
      Surface(shape = RoundedCornerShape(12.dp), color = PhoneCard) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
              Text("${m.displayName} (${m.playerId})", color = Color.White, fontWeight = FontWeight.Bold)
              Text("${if (m.online) "🟢 Online" else "⚪ Offline"} • ${m.status} • Room: ${flags.currentRoom}", color = PhoneMuted, fontSize = 11.sp)
            }
          }
          Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
              onClick = { onSetMuted(m.userId, !flags.muted, m.displayName) },
              modifier = Modifier.heightIn(min = 40.dp),
            ) { Text(if (flags.muted) "Unmute" else "Mute Chat", color = PhoneCyan, fontSize = 11.sp) }
            OutlinedButton(
              onClick = { onSetTaskRestricted(m.userId, !flags.restrictedFromTasks, m.displayName) },
              modifier = Modifier.heightIn(min = 40.dp),
            ) { Text(if (flags.restrictedFromTasks) "Allow Tasks" else "Restrict Tasks", color = PhoneGold, fontSize = 11.sp) }
            OutlinedButton(
              onClick = {
                onAssignSecretTask(m, "Secret Observation Mission", "Gather a hidden clue inside the Activity Room quietly.", 80L)
              },
              modifier = Modifier.heightIn(min = 40.dp),
            ) { Text("Secret Task", color = PhoneViolet, fontSize = 11.sp) }
            OutlinedButton(
              onClick = {
                onRequestConfirmModeration("Kick ${m.displayName} from active house?") {
                  onModerate(m.userId, "KICK", "Removed via Admin Phone")
                }
              },
              modifier = Modifier.heightIn(min = 40.dp),
            ) { Text("Kick", color = PhonePink, fontSize = 11.sp) }
          }
        }
      }
    }
  }
}

@Composable
private fun PhoneCctvAppScreen(
  cctvOnlineMap: Map<String, Boolean>,
  onToggleCamera: (String) -> Unit,
  onOpenMatrix: () -> Unit,
) {
  Button(
    onClick = onOpenMatrix,
    colors = ButtonDefaults.buttonColors(containerColor = PhoneCyan, contentColor = Color(0xFF061622)),
    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("phone_open_3d_cctv_btn"),
  ) {
    Icon(Icons.Rounded.Videocam, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text("Launch Control Room 24-Cam 3D Monitor", fontWeight = FontWeight.ExtraBold)
  }
  Text("PRIVACY LOCKED: Bathrooms, Washrooms, Changing & Smoking zones have ZERO cameras.", color = PhoneEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
  val grouped = remember { BiggGramAndNpcVotingEngine.ALL_24_CCTV_CAMERAS.groupBy { it.roomName } }
  grouped.forEach { (roomName, cams) ->
    Surface(shape = RoundedCornerShape(12.dp), color = PhoneCard) {
      Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$roomName (${cams.size} Cams)", color = PhoneGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        cams.forEach { cam ->
          val isOn = cctvOnlineMap[cam.id] ?: true
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${cam.code} • ${cam.label}", color = if (isOn) Color.White else PhoneMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Switch(checked = isOn, onCheckedChange = { onToggleCamera(cam.id) })
          }
        }
      }
    }
  }
}

@Composable
private fun PhoneAnnouncementsAppScreen(
  emergencyActive: Boolean,
  onSendTts: (String, String, VoiceGender) -> Unit,
  onTriggerSiren: (Boolean, String) -> Unit,
) {
  var msg by remember { mutableStateOf("") }
  var lang by remember { mutableStateOf("hi-IN") }
  var emergencyNote by remember { mutableStateOf("सावधान! इमरजेंसी अलर्ट! सभी तुरंत लिविंग रूम में आएं।") }

  Surface(shape = RoundedCornerShape(12.dp), color = PhoneCard) {
    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("HOUSE SPEAKER BROADCAST", color = PhoneGold, fontWeight = FontWeight.Bold)
      OutlinedTextField(
        value = msg,
        onValueChange = { if (it.length <= 280) msg = it },
        label = { Text("Announcement Text") },
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("hi-IN" to "Hindi", "en-IN" to "English").forEach { (code, lbl) ->
          OutlinedButton(onClick = { lang = code }, modifier = Modifier.weight(1f).heightIn(min = 44.dp)) {
            Text(lbl, color = if (lang == code) PhoneGold else PhoneMuted)
          }
        }
      }
      Button(
        onClick = { onSendTts(msg, lang, VoiceGender.FEMALE); msg = "" },
        enabled = msg.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = PhoneGold, contentColor = Color(0xFF10141F)),
        modifier = Modifier.fillMaxWidth().height(48.dp),
      ) { Text("Broadcast Over House Speakers", fontWeight = FontWeight.Bold) }
      OutlinedButton(
        onClick = { onSendTts(AnnouncementPresets.WELCOME_ANNOUNCEMENT, "en-IN", VoiceGender.FEMALE) },
        modifier = Modifier.fillMaxWidth().height(44.dp),
      ) { Text("Replay Show Welcome", color = PhoneCyan) }
    }
  }

  Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF281016), border = BorderStroke(1.dp, PhonePink)) {
    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = PhonePink)
        Spacer(Modifier.width(6.dp))
        Text("EMERGENCY SIREN SYSTEM", color = PhonePink, fontWeight = FontWeight.ExtraBold)
      }
      OutlinedTextField(
        value = emergencyNote,
        onValueChange = { emergencyNote = it },
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
      )
      Button(
        onClick = { onTriggerSiren(!emergencyActive, emergencyNote) },
        colors = ButtonDefaults.buttonColors(containerColor = PhonePink, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("phone_emergency_siren_btn"),
      ) { Text(if (emergencyActive) "Stop Emergency Siren" else "Activate Emergency Siren", fontWeight = FontWeight.ExtraBold) }
    }
  }
}

@Composable
private fun PhoneEventsClockAppScreen(
  clockState: HouseClockState,
  celebrations: List<HouseCelebrationEvent>,
  finaleState: FinaleCeremonyState,
  members: List<SeasonMember>,
  onSetHouseClock: (Int, Int, String, Boolean) -> Unit,
  onSetWeather: (String) -> Unit,
  onTogglePartyMode: (Boolean) -> Unit,
  onTriggerCelebration: (CelebrationType, String, String, Boolean) -> Unit,
  onTriggerFinale: (SeasonMember, String, String) -> Unit,
) {
  var celTitle by remember { mutableStateOf("House Diwali & Dance Night") }
  Surface(shape = RoundedCornerShape(12.dp), color = PhoneCard) {
    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = PhoneCyan)
        Spacer(Modifier.width(6.dp))
        Text("CLOCK: ${clockState.formattedClock()} (${clockState.timeMode.uppercase()})", color = Color.White, fontWeight = FontWeight.Bold)
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
          Triple(10, 0, "day") to "Day",
          Triple(17, 30, "dusk") to "Dusk",
          Triple(22, 30, "night") to "Night",
        ).forEach { (cfg, lbl) ->
          OutlinedButton(
            onClick = { onSetHouseClock(cfg.first, cfg.second, cfg.third, clockState.autoCycleEnabled) },
            modifier = Modifier.weight(1f).heightIn(min = 44.dp),
          ) { Text(lbl, color = if (clockState.timeMode == cfg.third) PhoneGold else PhoneMuted, fontSize = 11.sp) }
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Party / Dance Mode Lights", color = Color.White, fontSize = 12.sp)
        Switch(checked = clockState.partyMode, onCheckedChange = onTogglePartyMode)
      }
      Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("CLEAR", "RAIN", "GOLDEN", "NIGHT_MIST").forEach { w ->
          OutlinedButton(onClick = { onSetWeather(w) }, modifier = Modifier.heightIn(min = 40.dp)) {
            Text(w, color = if (clockState.weather == w) PhoneCyan else PhoneMuted, fontSize = 10.sp)
          }
        }
      }
    }
  }

  Surface(shape = RoundedCornerShape(12.dp), color = PhoneCard) {
    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("CELEBRATIONS & FESTIVALS", color = PhoneViolet, fontWeight = FontWeight.Bold)
      OutlinedTextField(
        value = celTitle,
        onValueChange = { celTitle = it },
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CelebrationType.entries.take(3).forEach { type ->
          Button(
            onClick = { onTriggerCelebration(type, celTitle, "Admin launched ${type.label}", true) },
            colors = ButtonDefaults.buttonColors(containerColor = PhoneViolet, contentColor = Color.White),
            modifier = Modifier.weight(1f).heightIn(min = 44.dp),
          ) { Text(type.label.split(" ").first(), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        }
      }
      if (celebrations.isNotEmpty()) {
        Text("Recent: ${celebrations.first().title} (${celebrations.first().status})", color = PhoneMuted, fontSize = 11.sp)
      }
    }
  }

  val eligibleWinner = members.firstOrNull { it.role != PlayerRole.ADMIN.name && it.status != "EVICTED" }
  Surface(shape = RoundedCornerShape(12.dp), color = PhoneCard, border = BorderStroke(1.dp, PhoneGold)) {
    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = PhoneGold)
        Spacer(Modifier.width(6.dp))
        Text("GRAND FINALE & TROPHY", color = PhoneGold, fontWeight = FontWeight.ExtraBold)
      }
      if (finaleState.active) {
        Text("🏆 Crowned: ${finaleState.winnerName}", color = PhoneEmerald, fontWeight = FontWeight.Bold)
      }
      Button(
        onClick = {
          val target = eligibleWinner ?: SeasonMember(userId = "u_finalist", displayName = "House Champion", playerId = "BB-01")
          onTriggerFinale(target, "Runner-Up Finalist", "BIGG BOSS – THE HOUSE SEASON 01 CHAMPION")
        },
        colors = ButtonDefaults.buttonColors(containerColor = PhoneGold, contentColor = Color(0xFF10141F)),
        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("phone_trigger_finale_btn"),
      ) { Text("Crown Grand Finale Winner & Present Trophy", fontWeight = FontWeight.ExtraBold) }
    }
  }
}

@Composable
private fun PhoneNotificationsAppScreen(notifications: List<AdminPhoneNotification>) {
  Text("PHONE NOTIFICATIONS (${notifications.size})", color = PhoneGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
  notifications.forEach { n ->
    Surface(shape = RoundedCornerShape(12.dp), color = PhoneCard) {
      Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
        Text("[${n.category}] ${n.title}", color = PhoneCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(n.body, color = Color.White, fontSize = 11.sp)
      }
    }
  }
}
