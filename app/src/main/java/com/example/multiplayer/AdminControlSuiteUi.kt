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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val SuiteBg = Color(0xFF0C1222)
private val SuiteCard = Color(0xFF151E33)
private val SuiteElevated = Color(0xFF1C2742)
private val GoldAccent = Color(0xFFEAB954)
private val CyanAccent = Color(0xFF22D3EE)
private val EmeraldOk = Color(0xFF34D399)
private val Danger = Color(0xFFF87171)
private val Muted = Color(0xFF94A3B8)

@Composable
fun AdminControlSuiteDialog(
  currentAdminUid: String,
  isAdminVerified: Boolean,
  season: SeasonSession?,
  members: List<SeasonMember>,
  allUsers: List<UserAccount>,
  votingCycles: List<VotingCycle>,
  activeCycle: VotingCycle?,
  nominations: List<NominationBallot> = emptyList(),
  ballots: List<VotingBallot>,
  votingTallySummary: VotingTallyResult? = null,
  evictions: List<EvictionRecord>,
  announcements: List<AdminAnnouncement>,
  ttsStatus: TtsStatusSnapshot,
  tasks: List<HouseTask>,
  activeTask: HouseTask?,
  taskSubmissions: List<TaskSubmission>,
  taskProgressList: List<TaskPlayerProgress> = emptyList(),
  rations: List<RationItem> = emptyList(),
  cookingSessions: List<CookingSession> = emptyList(),
  leaderboard: List<PlayerLeaderboardEntry> = emptyList(),
  teams: List<HouseTeam>,
  captaincyHistory: List<CaptaincyRecord>,
  houseRules: List<HouseRule>,
  ruleViolations: List<RuleViolation>,
  dailySchedule: List<DailyScheduleSlot>,
  cctvRecordings: List<CctvRecordingRecord>,
  isUploadingCctv: Boolean,
  cctvUploadStatus: String?,
  aiConfig: AiControlConfig? = null,
  aiSchedules: List<AiScheduledEvent> = emptyList(),
  aiProposals: List<AiApprovalProposal> = emptyList(),
  aiLogs: List<AiActivityLog> = emptyList(),
  whileAwaySummary: AiWhileAwaySummary = AiWhileAwaySummary(),
  vipPrivileges: List<VipPrivilege> = emptyList(),
  privilegeHistory: List<PrivilegeHistoryRecord> = emptyList(),
  gameSettings: GameSettings? = null,
  adminAuditLogs: List<AdminAuditLog> = emptyList(),
  adminSession: AdminSessionSnapshot? = null,
  isBusy: Boolean,
  actionMessage: String?,
  actionError: String?,
  onDismiss: () -> Unit,
  onClearFeedback: () -> Unit,
  onOpenCctvWall: () -> Unit,
  onEnterControlRoom: () -> Unit = {},
  onDeleteCctvRecording: (CctvRecordingRecord) -> Unit,
  onSetSeasonStatus: (String) -> Unit,
  onModeratePlayer: (targetUid: String, action: String, reason: String) -> Unit,
  onSelectCycle: (String) -> Unit,
  onStartConfiguredCycle: (
    title: String,
    startStatus: String,
    nominees: List<NomineeSummary>,
    npcEnabled: Boolean,
    voteType: String,
    nominationsPerPlayer: Long,
    votingDurationMinutes: Long,
    resultVisibility: String,
    tieBreakerPolicy: String,
  ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
  onPromoteToOpenVoting: (VotingCycle, List<NomineeSummary>) -> Unit = { _, _ -> },
  onUpdateCycleState: (VotingCycle, String, Boolean) -> Unit,
  onCancelVotingCycle: (VotingCycle, String) -> Unit = { _, _ -> },
  onCastNpcVote: (String, String, String) -> Unit,
  onConfirmEviction: (VotingCycle, NomineeSummary, Long, Long, String, String, VoiceGender) -> Unit,
  onPreviewSpeech: (String, String, VoiceGender) -> Unit,
  onBroadcastAll: (String, String?, String, VoiceGender) -> Unit,
  onSelectTask: (String) -> Unit,
  onSaveOrStartTask: (
    taskId: String?,
    title: String,
    description: String,
    instructions: String,
    activityType: TaskActivityType,
    assignmentMode: String,
    isCaptaincy: Boolean,
    targetRoom: String,
    durationSeconds: Long,
    promptQuestion: String,
    options: List<String>,
    maxPoints: Long,
    expectedAnswer: String,
    startImmediately: Boolean,
    category: TaskCategory,
    difficulty: TaskDifficulty,
    assignedUids: List<String>,
    assignedTeamId: String?,
    requiredCheckpoints: List<String>,
    requiredRecipeId: String?,
    rewardAchievement: String?,
  ) -> Unit,
  onUpdateTaskStatus: (HouseTask, String) -> Unit,
  onDeclareTaskWinner: (HouseTask, TaskSubmission?) -> Unit,
  onSaveTeam: (
    teamId: String?,
    name: String,
    colorHex: String,
    memberUids: List<String>,
    score: Long,
    winsCount: Long,
  ) -> Unit,
  onAssignCaptain: (member: SeasonMember, title: String, scoreAwarded: Long) -> Unit,
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
  onSaveScheduleSlot: (DailyScheduleSlot) -> Unit,
  onPublishSchedule: (activeSlotId: String, broadcastUpdate: Boolean) -> Unit,
  onReplenishRation: (RationItem, Long) -> Unit = { _, _ -> },
  onReplenishAllRations: () -> Unit = {},
  onToggleRationPermission: (RationItem, Boolean) -> Unit = { _, _ -> },
  onManageCookingSession: (CookingSession, String) -> Unit = { _, _ -> },
  onSetAiEnabled: (Boolean) -> Unit = {},
  onSetAiMode: (AiControlMode) -> Unit = {},
  onSetAiModules: (Boolean, Boolean, Boolean, Boolean) -> Unit = { _, _, _, _ -> },
  onTriggerAiEmergencyStop: () -> Unit = {},
  onAcknowledgeAiWhileAway: () -> Unit = {},
  onCreateAiSchedule: (AiActionType, String, String, Long, Long) -> Unit = { _, _, _, _, _ -> },
  onCancelAiSchedule: (String) -> Unit = {},
  onRunAiTick: (String) -> Unit = {},
  onGenerateAiSuggestion: () -> Unit = {},
  onApproveAiProposal: (AiApprovalProposal) -> Unit = {},
  onRejectAiProposal: (AiApprovalProposal) -> Unit = {},
  onAssignOrReplaceCaptain: (SeasonMember, String, CaptainPowersConfig) -> Unit = { _, _, _ -> },
  onRemoveCaptain: () -> Unit = {},
  onUpdateCaptainPowers: (CaptainPowersConfig) -> Unit = {},
  onSetPlayerVip: (SeasonMember, Boolean, String) -> Unit = { _, _, _ -> },
  onSaveGameSettings: (GameSettings) -> Unit = {},
  onCalculateServerTally: (VotingCycle, Boolean) -> Unit = { _, _ -> },
  onChangeAdminPassword: (String, String, String, () -> Unit) -> Unit = { _, _, _, _ -> },
  onAdminLogout: () -> Unit = {},
  moderationFlags: Map<String, PlayerModerationFlags> = emptyMap(),
  onOpenAdminPhone: () -> Unit = {},
  onSetPlayerMuted: (String, Boolean, String) -> Unit = { _, _, _ -> },
  onSetPlayerTaskRestricted: (String, Boolean, String) -> Unit = { _, _, _ -> },
) {
  // Hard UI & guard check: never render suite for non-admins
  if (!isAdminVerified) return

  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf(
    "Overview",
    "Players",
    "Tasks",
    "Teams & Captain",
    "House Rules",
    "Daily Schedule",
    "Voting & Nominations",
    "Evictions",
    "Announcements",
    "CCTV Archive",
    "Kitchen & Rations",
    "Leaderboard",
    "AI Operator (Chair 1)",
    "Captain & VIP",
    "Settings & Security",
  )

  val activeMembers = remember(members) {
    members.filter { it.role != PlayerRole.ADMIN.name && it.status !in listOf("EVICTED", "BANNED", "REMOVED") }
  }
  val onlineRealCount = remember(activeMembers) { activeMembers.count { it.online } }
  val seasonStatus = season?.status ?: "LOBBY"

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = SuiteBg,
      border = BorderStroke(1.5.dp, CyanAccent.copy(alpha = 0.45f)),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.92f)
        .widthIn(max = 980.dp),
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Production Suite Top Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CyanAccent.copy(alpha = 0.16f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(Icons.Rounded.Shield, contentDescription = null, tint = CyanAccent)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                "BIGG BOSS – THE HOUSE • ADMIN CONTROL SUITE",
                color = CyanAccent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp,
              )
              Text(
                "${season?.title ?: "Season 01"} • $seasonStatus ($onlineRealCount Online / ${activeMembers.size} Active / 15 Max)",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
              onClick = onOpenAdminPhone,
              border = BorderStroke(1.dp, GoldAccent),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp).testTag("open_admin_phone_from_suite"),
            ) {
              Icon(Icons.Rounded.PhoneAndroid, contentDescription = "Admin Phone", tint = GoldAccent, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Phone", color = GoldAccent, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Button(
              onClick = onOpenCctvWall,
              colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF061622)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp).testTag("open_cctv_matrix_button"),
            ) {
              Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Live 3D CCTV", fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
            IconButton(
              onClick = onAdminLogout,
              modifier = Modifier.size(48.dp).testTag("admin_suite_logout_icon"),
            ) {
              Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Admin Logout", tint = Danger)
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(48.dp).testTag("close_admin_suite_button"),
            ) {
              Icon(Icons.Rounded.Close, contentDescription = "Close Admin Suite", tint = Color.White)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Strip
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          tabs.forEachIndexed { idx, title ->
            val selected = selectedTab == idx
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = if (selected) GoldAccent else SuiteElevated,
              border = BorderStroke(1.dp, if (selected) GoldAccent else Color.White.copy(alpha = 0.1f)),
              modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable {
                  onClearFeedback()
                  selectedTab = idx
                }
                .testTag("admin_tab_$idx"),
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                  text = title,
                  color = if (selected) Color(0xFF10141F) else Color.White,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }

        if (!actionError.isNullOrBlank() || !actionMessage.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(8.dp))
          val isErr = !actionError.isNullOrBlank()
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = (if (isErr) Danger else EmeraldOk).copy(alpha = 0.16f),
            border = BorderStroke(1.dp, if (isErr) Danger else EmeraldOk),
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

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          when (selectedTab) {
            0 -> AdminOverviewTab(
              seasonStatus = seasonStatus,
              onlineCount = onlineRealCount,
              activeCount = activeMembers.size,
              activeMembers = activeMembers,
              isBusy = isBusy,
              onSetSeasonStatus = onSetSeasonStatus,
              onOpenCctvWall = onOpenCctvWall,
              onEnterControlRoom = onEnterControlRoom,
            )
            1 -> AdminPlayerManagementTab(
              currentAdminUid = currentAdminUid,
              allUsers = allUsers,
              members = members,
              moderationFlags = moderationFlags,
              isLocalAdminSession = adminSession?.isLocalOnly == true,
              isBusy = isBusy,
              onModeratePlayer = onModeratePlayer,
              onSetPlayerMuted = onSetPlayerMuted,
              onSetPlayerTaskRestricted = onSetPlayerTaskRestricted,
            )
            2 -> AdminTasksManagementTab(
              tasks = tasks,
              activeTask = activeTask,
              submissions = taskSubmissions,
              members = members,
              teams = teams,
              taskProgressList = taskProgressList,
              isBusy = isBusy,
              onSelectTask = onSelectTask,
              onSaveOrStartTask = onSaveOrStartTask,
              onUpdateTaskStatus = onUpdateTaskStatus,
              onDeclareWinner = onDeclareTaskWinner,
            )
            3 -> AdminTeamsAndCaptainTab(
              season = season,
              members = members,
              teams = teams,
              captaincyHistory = captaincyHistory,
              isBusy = isBusy,
              onSaveTeam = onSaveTeam,
              onAssignCaptain = onAssignCaptain,
            )
            4 -> AdminHouseRulesTab(
              rules = houseRules,
              violations = ruleViolations,
              members = members,
              isBusy = isBusy,
              onSaveRule = onSaveRule,
              onBroadcastRule = onBroadcastRule,
              onIssueWarning = onIssueWarning,
            )
            5 -> AdminDailyScheduleTab(
              scheduleSlots = dailySchedule,
              isBusy = isBusy,
              onSaveSlot = onSaveScheduleSlot,
              onPublishSchedule = onPublishSchedule,
            )
            6 -> AdminVotingPhoneTab(
              allUsers = allUsers,
              members = members,
              season = season,
              votingCycles = votingCycles,
              activeCycle = activeCycle,
              nominations = nominations,
              ballots = ballots,
              isBusy = isBusy,
              onSelectCycle = onSelectCycle,
              onStartConfiguredCycle = onStartConfiguredCycle,
              onPromoteToOpenVoting = onPromoteToOpenVoting,
              onUpdateCycleState = onUpdateCycleState,
              onCalculateServerTally = onCalculateServerTally,
              onCancelCycle = onCancelVotingCycle,
              onCastNpcVote = onCastNpcVote,
            )
            7 -> AdminEvictionsTab(
              season = season,
              activeCycle = activeCycle,
              ballots = ballots,
              tallySummary = votingTallySummary,
              evictions = evictions,
              isBusy = isBusy,
              onCalculateServerTally = onCalculateServerTally,
              onConfirmEviction = onConfirmEviction,
              onCancelDueToTie = onCancelVotingCycle,
            )
            8 -> AdminAnnouncementsTab(
              ttsStatus = ttsStatus,
              announcements = announcements,
              isBusy = isBusy,
              onPreviewSpeech = onPreviewSpeech,
              onBroadcastAll = onBroadcastAll,
            )
            9 -> AdminCctvArchiveTab(
              recordings = cctvRecordings,
              isUploadingCctv = isUploadingCctv,
              cctvUploadStatus = cctvUploadStatus,
              isBusy = isBusy,
              onOpenCctvWall = onOpenCctvWall,
              onDeleteRecording = onDeleteCctvRecording,
            )
            10 -> AdminKitchenManagementTab(
              rations = rations,
              cookingSessions = cookingSessions,
              isBusy = isBusy,
              onReplenishRation = onReplenishRation,
              onReplenishAllRations = onReplenishAllRations,
              onToggleRationPermission = onToggleRationPermission,
              onManageCookingSession = onManageCookingSession,
            )
            11 -> AdminLeaderboardAndProgressTab(
              leaderboard = leaderboard,
              activeTask = activeTask,
              taskProgressList = taskProgressList,
            )
            12 -> AdminAiOperatorTab(
              config = aiConfig,
              schedules = aiSchedules,
              proposals = aiProposals,
              logs = aiLogs,
              whileAwaySummary = whileAwaySummary,
              isBusy = isBusy,
              onSetEnabled = onSetAiEnabled,
              onSetMode = onSetAiMode,
              onSetModules = onSetAiModules,
              onEmergencyStop = onTriggerAiEmergencyStop,
              onAcknowledgeWhileAway = onAcknowledgeAiWhileAway,
              onCreateSchedule = onCreateAiSchedule,
              onCancelSchedule = onCancelAiSchedule,
              onRunAiTick = onRunAiTick,
              onGenerateSuggestion = onGenerateAiSuggestion,
              onApproveProposal = onApproveAiProposal,
              onRejectProposal = onRejectAiProposal,
            )
            13 -> AdminCaptainAndVipTab(
              season = season,
              members = members,
              vipPrivileges = vipPrivileges,
              privilegeHistory = privilegeHistory,
              isBusy = isBusy,
              onAssignOrReplaceCaptain = onAssignOrReplaceCaptain,
              onRemoveCaptain = onRemoveCaptain,
              onUpdateCaptainPowers = onUpdateCaptainPowers,
              onSetPlayerVip = onSetPlayerVip,
            )
            14 -> AdminGameAndSecurityTab(
              season = season,
              gameSettings = gameSettings,
              adminSession = adminSession,
              adminAuditLogs = adminAuditLogs,
              isBusy = isBusy,
              onSaveGameSettings = onSaveGameSettings,
              onChangeAdminPassword = onChangeAdminPassword,
              onAdminLogout = onAdminLogout,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AdminOverviewTab(
  seasonStatus: String,
  onlineCount: Int,
  activeCount: Int,
  activeMembers: List<SeasonMember>,
  isBusy: Boolean,
  onSetSeasonStatus: (String) -> Unit,
  onOpenCctvWall: () -> Unit,
  onEnterControlRoom: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      AdminMetricTile("SEASON STATE", seasonStatus, GoldAccent, Modifier.weight(1f))
      AdminMetricTile("ONLINE REAL PLAYERS", "$onlineCount Online", EmeraldOk, Modifier.weight(1f))
      AdminMetricTile("HOUSE CAPACITY", "$activeCount / 15 Active", CyanAccent, Modifier.weight(1f))
    }

    Surface(
      shape = RoundedCornerShape(18.dp),
      color = SuiteCard,
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("SEASON LIFECYCLE CONTROLS", color = GoldAccent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = { onSetSeasonStatus("LIVE") },
            enabled = !isBusy && seasonStatus != "LIVE",
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldOk, contentColor = Color(0xFF06281E)),
            modifier = Modifier.weight(1f).height(48.dp).testTag("start_season_button"),
          ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Start Season", fontWeight = FontWeight.Bold)
          }
          OutlinedButton(
            onClick = { onSetSeasonStatus("LOBBY") },
            enabled = !isBusy && seasonStatus != "LOBBY",
            border = BorderStroke(1.dp, GoldAccent),
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            Text("Lobby Hold", color = GoldAccent, fontWeight = FontWeight.Bold)
          }
          Button(
            onClick = { onSetSeasonStatus("ENDED") },
            enabled = !isBusy && seasonStatus != "ENDED",
            colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color(0xFF1F0808)),
            modifier = Modifier.weight(1f).height(48.dp).testTag("end_season_button"),
          ) {
            Icon(Icons.Rounded.Stop, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("End Season", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    Surface(
      shape = RoundedCornerShape(18.dp),
      color = SuiteCard,
      border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f)),
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("PRIVATE 3D CONTROL ROOM & 24-CAMERA CCTV", color = CyanAccent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(
          "Private Control Room houses Chair 1 (ChatGPT AI Operator) and Chair 2 (Main Admin). Hidden from normal players.",
          color = Muted,
          style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = onEnterControlRoom,
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color(0xFF10141F)),
            modifier = Modifier.weight(1f).height(48.dp).testTag("enter_control_room_button"),
          ) {
            Text("Enter Control Room", fontWeight = FontWeight.ExtraBold)
          }
          Button(
            onClick = onOpenCctvWall,
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF061622)),
            modifier = Modifier.weight(1f).height(48.dp).testTag("launch_cctv_wall_button"),
          ) {
            Text("Launch CCTV", fontWeight = FontWeight.ExtraBold)
          }
        }
      }
    }

    Surface(shape = RoundedCornerShape(18.dp), color = SuiteCard) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ACTIVE SEASON ROSTER", color = GoldAccent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        if (activeMembers.isEmpty()) {
          Text("No active housemates joined yet.", color = Muted, style = MaterialTheme.typography.bodySmall)
        } else {
          activeMembers.forEach { m ->
            Row(
              modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SuiteElevated).padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(if (m.online) EmeraldOk else Muted))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text("${m.displayName} (Slot ${m.slotNumber})", color = Color.White, fontWeight = FontWeight.Bold)
                  Text("${m.playerId} • ${m.characterId} • ${m.role}", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
              }
              Text(if (m.online) m.status else "OFFLINE", color = if (m.online) EmeraldOk else Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminMetricTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = SuiteCard,
    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(label, color = accent, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(4.dp))
      Text(value, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
    }
  }
}

@Composable
private fun AdminPlayerManagementTab(
  currentAdminUid: String,
  allUsers: List<UserAccount>,
  members: List<SeasonMember>,
  moderationFlags: Map<String, PlayerModerationFlags>,
  isLocalAdminSession: Boolean,
  isBusy: Boolean,
  onModeratePlayer: (targetUid: String, action: String, reason: String) -> Unit,
  onSetPlayerMuted: (String, Boolean, String) -> Unit,
  onSetPlayerTaskRestricted: (String, Boolean, String) -> Unit,
) {
  val memberByUid = remember(members) { members.associateBy { it.userId } }
  var inspectedUser by remember { mutableStateOf<UserAccount?>(null) }
  var pendingKickOrBan by remember { mutableStateOf<Triple<UserAccount, String, String>?>(null) }

  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(
      "FULL HOUSE ROSTER (${allUsers.size} ACCOUNTS • ${members.count { it.online }} ONLINE)",
      color = CyanAccent,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
    )
    if (isLocalAdminSession) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = GoldAccent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
      ) {
        Text(
          text = "Local APK Admin active: Mute & Task Restrict apply immediately. Cloud Kick/Ban writes also require Firebase /admins/{uid} auth.",
          color = GoldAccent,
          style = MaterialTheme.typography.labelSmall,
          modifier = Modifier.padding(8.dp),
        )
      }
    }

    if (allUsers.isEmpty()) {
      Text("No registered users found yet.", color = Muted, style = MaterialTheme.typography.bodySmall)
    } else {
      allUsers.forEach { user ->
        val mem = memberByUid[user.userId]
        val isOnline = mem?.online == true
        val isSelf = user.userId == currentAdminUid
        val flags = moderationFlags[user.userId]
        val isMuted = flags?.muted == true
        val isTaskRestricted = flags?.restrictedFromTasks == true
        val roomLoc = flags?.currentRoom?.takeIf { it.isNotBlank() } ?: "Living Atrium"

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = SuiteCard,
          border = BorderStroke(1.dp, if (user.banned) Danger.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)),
          modifier = Modifier.fillMaxWidth().clickable { inspectedUser = user },
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(if (isOnline) EmeraldOk else Muted))
                  Text(user.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                  if (user.banned) {
                    Text("BANNED", color = Danger, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                  }
                  if (mem?.status == "EVICTED") {
                    Text("EVICTED", color = GoldAccent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                  }
                  if (isMuted) {
                    Text("MUTED", color = Danger, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                  }
                }
                Text(
                  "${user.playerId} • ${user.characterId} • ${if (isOnline) "Online ($roomLoc)" else "Offline"}",
                  color = Muted,
                  style = MaterialTheme.typography.labelSmall,
                )
              }
            }

            if (!isSelf) {
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                OutlinedButton(
                  onClick = { onSetPlayerMuted(user.userId, !isMuted, user.displayName) },
                  enabled = !isBusy,
                  border = BorderStroke(1.dp, if (isMuted) EmeraldOk else CyanAccent),
                  modifier = Modifier.heightIn(min = 44.dp),
                ) {
                  Text(if (isMuted) "Unmute Chat" else "Mute Chat", color = if (isMuted) EmeraldOk else CyanAccent, fontSize = 11.sp)
                }
                OutlinedButton(
                  onClick = { onSetPlayerTaskRestricted(user.userId, !isTaskRestricted, user.displayName) },
                  enabled = !isBusy,
                  border = BorderStroke(1.dp, if (isTaskRestricted) EmeraldOk else GoldAccent),
                  modifier = Modifier.heightIn(min = 44.dp),
                ) {
                  Text(if (isTaskRestricted) "Allow Tasks" else "Restrict Tasks", color = if (isTaskRestricted) EmeraldOk else GoldAccent, fontSize = 11.sp)
                }
                if (mem != null && mem.status !in listOf("EVICTED", "BANNED")) {
                  OutlinedButton(
                    onClick = { pendingKickOrBan = Triple(user, "KICK", "Removed from session by Admin.") },
                    enabled = !isBusy,
                    border = BorderStroke(1.dp, GoldAccent),
                    modifier = Modifier.heightIn(min = 44.dp),
                  ) {
                    Icon(Icons.Rounded.PersonRemove, contentDescription = "Kick Player", tint = GoldAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kick", color = GoldAccent, fontSize = 11.sp)
                  }
                }
                OutlinedButton(
                  onClick = {
                    val act = if (user.banned) "UNBAN" else "BAN"
                    val rsn = if (user.banned) "" else "Account banned by Admin Control Room."
                    if (act == "BAN") {
                      pendingKickOrBan = Triple(user, act, rsn)
                    } else {
                      onModeratePlayer(user.userId, act, rsn)
                    }
                  },
                  enabled = !isBusy,
                  border = BorderStroke(1.dp, if (user.banned) EmeraldOk else Danger),
                  modifier = Modifier.heightIn(min = 44.dp).testTag("ban_toggle_${user.userId}"),
                ) {
                  Icon(
                    imageVector = if (user.banned) Icons.Rounded.Security else Icons.Rounded.Block,
                    contentDescription = if (user.banned) "Unban" else "Ban",
                    tint = if (user.banned) EmeraldOk else Danger,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(if (user.banned) "Unban" else "Ban", color = if (user.banned) EmeraldOk else Danger, fontSize = 11.sp)
                }
              }
            }
          }
        }
      }
    }
  }

  pendingKickOrBan?.let { (target, act, reason) ->
    AlertDialog(
      onDismissRequest = { pendingKickOrBan = null },
      containerColor = SuiteCard,
      title = { Text("Confirm $act: ${target.displayName}", color = Danger, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "Proceed with $act for ${target.displayName} (${target.playerId})? Reason: $reason",
          color = Color.White,
          style = MaterialTheme.typography.bodySmall,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onModeratePlayer(target.userId, act, reason)
            pendingKickOrBan = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color(0xFF1F0808)),
        ) { Text("Confirm $act", fontWeight = FontWeight.Bold) }
      },
      dismissButton = {
        TextButton(onClick = { pendingKickOrBan = null }) { Text("Cancel", color = Muted) }
      },
    )
  }

  inspectedUser?.let { profile ->
    AlertDialog(
      onDismissRequest = { inspectedUser = null },
      containerColor = SuiteCard,
      title = { Text("${profile.displayName} • Profile", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Player ID: ${profile.playerId}", color = GoldAccent)
          Text("UID: ${profile.userId}", color = Muted, style = MaterialTheme.typography.bodySmall)
          Text("Email: ${profile.email ?: "Hidden / Not Provided"}", color = Muted, style = MaterialTheme.typography.bodySmall)
          Text("Avatar: ${profile.characterId}", color = Color.White)
          Text("Role: ${profile.role}", color = CyanAccent)
          Text("Evictions Count: ${profile.evictedSeasons}", color = Color.White)
          Text("Access Status: ${if (profile.banned) "BANNED (${profile.banReason ?: "Policy"})" else "Active"}", color = if (profile.banned) Danger else EmeraldOk)
        }
      },
      confirmButton = {
        TextButton(onClick = { inspectedUser = null }) {
          Text("Close", color = GoldAccent)
        }
      },
    )
  }
}
