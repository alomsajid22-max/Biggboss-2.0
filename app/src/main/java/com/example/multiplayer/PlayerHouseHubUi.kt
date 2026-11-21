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
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

private val HubBg = Color(0xFF0C1222)
private val HubCard = Color(0xFF151E33)
private val HubElevated = Color(0xFF1C2742)
private val Gold = Color(0xFFEAB954)
private val Cyan = Color(0xFF22D3EE)
private val Emerald = Color(0xFF34D399)
private val Danger = Color(0xFFF87171)
private val Muted = Color(0xFF94A3B8)

@Composable
fun RuleWarningBanner(
  warning: RealtimeWarningSnapshot,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = Color(0xFF2A0F14).copy(alpha = 0.96f),
    border = BorderStroke(1.5.dp, Danger),
    shadowElevation = 14.dp,
    modifier = modifier.widthIn(max = 560.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(Icons.Rounded.WarningAmber, contentDescription = "Rule Warning", tint = Danger, modifier = Modifier.size(24.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "OFFICIAL ${warning.warningLevel} • ${warning.ruleTitle.uppercase()}",
          color = Danger,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.ExtraBold,
        )
        Text(text = warning.note, color = Color.White, style = MaterialTheme.typography.bodySmall)
      }
      TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
        Text("Acknowledge", color = Gold, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun PlayerHouseHubDialog(
  currentUserId: String,
  isAdminVerified: Boolean = false,
  season: SeasonSession?,
  members: List<SeasonMember>,
  vipPrivileges: List<VipPrivilege> = emptyList(),
  privilegeHistory: List<PrivilegeHistoryRecord> = emptyList(),
  tasks: List<HouseTask>,
  activeTask: HouseTask?,
  submissions: List<TaskSubmission>,
  collected3dNodes: Set<String>,
  taskProgressList: List<TaskPlayerProgress> = emptyList(),
  leaderboard: List<PlayerLeaderboardEntry> = emptyList(),
  teams: List<HouseTeam>,
  rules: List<HouseRule>,
  violations: List<RuleViolation>,
  scheduleSlots: List<DailyScheduleSlot>,
  isBusy: Boolean,
  actionMessage: String?,
  actionError: String?,
  onSubmitAnswer: (HouseTask, String) -> Unit,
  onCompleteAndClaimTask: (HouseTask, String) -> Unit = { _, _ -> },
  onOpenKitchenFromTask: () -> Unit = {},
  onClearFeedback: () -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Active Task", "Captain & VIP", "House Rules", "Daily Schedule", "Leaderboard")

  var selectedOption by remember(activeTask?.id) { mutableStateOf("") }
  var freeAnswer by remember(activeTask?.id) { mutableStateOf("") }
  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

  val isTaskPlayable = activeTask != null && TaskLifecycleStatus.fromRaw(activeTask.status).isPlayable()
  LaunchedEffect(activeTask?.status, activeTask?.endTimeMillis) {
    while (activeTask != null && TaskLifecycleStatus.fromRaw(activeTask.status).isPlayable()) {
      nowMillis = System.currentTimeMillis()
      delay(1000L)
    }
  }

  val mySubmission = remember(submissions, currentUserId) {
    submissions.firstOrNull { it.userId == currentUserId }
  }
  val myTeam = remember(teams, currentUserId) {
    teams.firstOrNull { it.memberUids.contains(currentUserId) }
  }
  val myProgress = remember(taskProgressList, currentUserId) {
    taskProgressList.firstOrNull { it.userId == currentUserId }
  }
  val visibleTasks = remember(tasks, currentUserId, myTeam?.id) {
    tasks.filter { Phase8Logic.isTaskVisibleToPlayer(it, currentUserId, myTeam?.id, isAdmin = false) }
  }
  val displayTask = remember(activeTask, visibleTasks, currentUserId, myTeam?.id) {
    val picked = activeTask?.takeIf { Phase8Logic.isTaskVisibleToPlayer(it, currentUserId, myTeam?.id, false) }
    picked ?: visibleTasks.firstOrNull { TaskLifecycleStatus.fromRaw(it.status).isPlayable() } ?: visibleTasks.firstOrNull()
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = HubBg,
      border = BorderStroke(1.5.dp, Gold.copy(alpha = 0.55f)),
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .fillMaxHeight(0.88f)
        .widthIn(max = 880.dp),
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Assignment, contentDescription = null, tint = Gold, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Column {
              Text("BIGG BOSS – THE HOUSE • DAILY ACTIVITIES HUB", color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
              Text(
                if (!season?.captainDisplayName.isNullOrBlank()) {
                  "👑 Captain: ${season?.captainDisplayName} • ${myTeam?.name ?: "No Team Assigned"}"
                } else {
                  "Tasks, Teams, Captaincy, Rules & Schedule"
                },
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp).testTag("close_house_hub_btn")) {
            Icon(Icons.Rounded.Close, contentDescription = "Close Tasks Hub", tint = Color.White)
          }
        }

        Spacer(Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          tabs.forEachIndexed { idx, label ->
            val picked = selectedTab == idx
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = if (picked) Gold else HubElevated,
              modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable {
                  onClearFeedback()
                  selectedTab = idx
                }
                .testTag("hub_tab_$idx"),
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                  text = label,
                  color = if (picked) Color(0xFF10141F) else Color.White,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }

        if (!actionError.isNullOrBlank() || !actionMessage.isNullOrBlank()) {
          Spacer(Modifier.height(8.dp))
          val isErr = !actionError.isNullOrBlank()
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = (if (isErr) Danger else Emerald).copy(alpha = 0.16f),
            border = BorderStroke(1.dp, if (isErr) Danger else Emerald),
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

        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          when (selectedTab) {
            0 -> {
              Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                if (displayTask == null) {
                  Surface(shape = RoundedCornerShape(18.dp), color = HubCard, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Text("NO ACTIVE TASK RIGHT NOW", color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                      Text(
                        "Check the Daily Schedule tab or wait for the Admin Control Room to start the next house competition.",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                      )
                    }
                  }
                } else {
                  val remSec = Phase6Logic.remainingSeconds(displayTask.endTimeMillis, nowMillis)
                  val activityType = TaskActivityType.fromRaw(displayTask.activityType)
                  val category = TaskCategory.fromRaw(displayTask.category)
                  val difficulty = TaskDifficulty.fromRaw(displayTask.difficulty)
                  val playable = TaskLifecycleStatus.fromRaw(displayTask.status).isPlayable()
                  val rewardPreview = Phase8Logic.calculateRewardPoints(displayTask.maxPoints, difficulty)
                    .coerceAtMost(displayTask.maxPoints.coerceAtLeast(1L))
                  val isCheckpointOr3d = activityType in listOf(
                    TaskActivityType.TREASURE_HUNT,
                    TaskActivityType.OBSTACLE,
                    TaskActivityType.CHECKPOINT,
                  )
                  val requiredSteps = displayTask.requiredCheckpoints
                  val completedSteps = ((myProgress?.completedSteps.orEmpty()) + collected3dNodes).distinct()
                  val progressPct = if (requiredSteps.isNotEmpty()) {
                    Phase8Logic.computeProgressPercent(completedSteps, requiredSteps)
                  } else {
                    myProgress?.percent ?: if (mySubmission != null) 100L else 0L
                  }

                  Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = HubCard,
                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.55f)),
                  ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            Surface(shape = RoundedCornerShape(999.dp), color = Gold.copy(alpha = 0.2f)) {
                              Text(category.label.uppercase(), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Surface(shape = RoundedCornerShape(999.dp), color = Cyan.copy(alpha = 0.18f)) {
                              Text(difficulty.label.uppercase(), color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Surface(shape = RoundedCornerShape(999.dp), color = Emerald.copy(alpha = 0.16f)) {
                              Text(displayTask.assignmentMode, color = Emerald, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                          }
                          Text(displayTask.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                          Text(
                            "Zone: ${displayTask.targetRoom} • Reward: +$rewardPreview pts${if (displayTask.isCaptaincy) " • 👑 CAPTAINCY" else ""}",
                            color = Cyan,
                            style = MaterialTheme.typography.labelSmall,
                          )
                        }
                        Surface(
                          shape = RoundedCornerShape(12.dp),
                          color = if (playable) Emerald.copy(alpha = 0.16f) else HubElevated,
                          border = BorderStroke(1.dp, if (playable) Emerald else Muted),
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                          ) {
                            Icon(Icons.Rounded.Timer, contentDescription = null, tint = if (playable) Emerald else Muted, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                              text = if (playable) Phase6Logic.formatCountdown(remSec) else displayTask.status,
                              color = if (playable) Emerald else Color.White,
                              fontWeight = FontWeight.ExtraBold,
                            )
                          }
                        }
                      }

                      // Live Progress Tracker
                      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                          Text("Task Progress", color = Muted, style = MaterialTheme.typography.labelSmall)
                          Text("$progressPct%", color = Cyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                        }
                        LinearProgressIndicator(
                          progress = { progressPct / 100f },
                          color = if (progressPct >= 100L) Emerald else Cyan,
                          trackColor = HubElevated,
                          modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
                        )
                      }

                      Text(displayTask.description, color = Color.White, style = MaterialTheme.typography.bodySmall)
                      Surface(shape = RoundedCornerShape(10.dp), color = HubElevated) {
                        Text(
                          displayTask.instructions,
                          color = Color(0xFFE2E8F0),
                          style = MaterialTheme.typography.bodySmall,
                          modifier = Modifier.fillMaxWidth().padding(10.dp),
                        )
                      }

                      if (requiredSteps.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(12.dp), color = HubElevated) {
                          Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("CHECKPOINTS (${completedSteps.count { it in requiredSteps }}/${requiredSteps.size})", color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            requiredSteps.forEach { step ->
                              val done = step in completedSteps
                              Text(
                                text = "${if (done) "✓" else "○"} ${step.replace("_", " ").uppercase()}",
                                color = if (done) Emerald else Muted,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (done) FontWeight.Bold else FontWeight.Normal,
                              )
                            }
                          }
                        }
                      }

                      if (displayTask.promptQuestion.isNotBlank()) {
                        Text(displayTask.promptQuestion, color = Gold, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                      }

                      if (mySubmission != null) {
                        Surface(
                          shape = RoundedCornerShape(12.dp),
                          color = Emerald.copy(alpha = 0.14f),
                          border = BorderStroke(1.dp, Emerald),
                        ) {
                          Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                          ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Emerald)
                            Column(modifier = Modifier.weight(1f)) {
                              Text(
                                "Submitted • Status: ${mySubmission.verificationStatus} (${mySubmission.awardedScore} pts)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                              )
                              Text("Entry: ${mySubmission.answerText}", color = Muted, style = MaterialTheme.typography.labelSmall)
                            }
                          }
                        }
                      } else if (playable) {
                        if (activityType == TaskActivityType.COOKING) {
                          OutlinedButton(
                            onClick = onOpenKitchenFromTask,
                            border = BorderStroke(1.dp, Gold),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("task_open_kitchen_btn"),
                          ) {
                            Text("Open Kitchen & Cooking Studio", color = Gold, fontWeight = FontWeight.Bold)
                          }
                          Button(
                            onClick = { onCompleteAndClaimTask(displayTask, "COOKED_AND_SERVED") },
                            enabled = !isBusy && progressPct >= 100L && remSec > 0L,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color(0xFF06281E)),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("claim_cooking_task_btn"),
                          ) {
                            Text("Complete & Claim Reward (+$rewardPreview pts)", fontWeight = FontWeight.ExtraBold)
                          }
                        } else if (isCheckpointOr3d) {
                          val totalReq = requiredSteps.size.coerceAtLeast(3)
                          val doneCount = if (requiredSteps.isNotEmpty()) completedSteps.count { it in requiredSteps } else collected3dNodes.size
                          Button(
                            onClick = {
                              val code = when (activityType) {
                                TaskActivityType.TREASURE_HUNT -> "RELICS_${doneCount}_OF_$totalReq"
                                TaskActivityType.CHECKPOINT -> "CHECKPOINTS_${doneCount}_OF_$totalReq"
                                else -> "RINGS_${doneCount}_OF_$totalReq"
                              }
                              onCompleteAndClaimTask(displayTask, code)
                            },
                            enabled = !isBusy && doneCount >= totalReq && remSec > 0L,
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF10141F)),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_3d_task_btn"),
                          ) {
                            Text("Complete & Claim Reward ($doneCount/$totalReq)", fontWeight = FontWeight.ExtraBold)
                          }
                        } else if (displayTask.options.isNotEmpty()) {
                          displayTask.options.forEachIndexed { i, opt ->
                            val picked = selectedOption == opt
                            Surface(
                              shape = RoundedCornerShape(12.dp),
                              color = if (picked) Gold.copy(alpha = 0.18f) else HubElevated,
                              border = BorderStroke(1.dp, if (picked) Gold else Color.White.copy(alpha = 0.12f)),
                              modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { selectedOption = opt }
                                .testTag("task_option_$i"),
                            ) {
                              Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(opt, color = Color.White, fontWeight = if (picked) FontWeight.Bold else FontWeight.Medium)
                              }
                            }
                          }
                          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                              onClick = { onSubmitAnswer(displayTask, selectedOption) },
                              enabled = !isBusy && selectedOption.isNotBlank() && remSec > 0L,
                              border = BorderStroke(1.dp, Cyan),
                              modifier = Modifier.weight(1f).height(48.dp).testTag("submit_task_answer_btn"),
                            ) {
                              Text("Submit Answer", color = Cyan, fontWeight = FontWeight.Bold)
                            }
                            Button(
                              onClick = { onCompleteAndClaimTask(displayTask, selectedOption) },
                              enabled = !isBusy && selectedOption.isNotBlank() && remSec > 0L,
                              colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF10141F)),
                              modifier = Modifier.weight(1f).height(48.dp).testTag("claim_task_reward_btn"),
                            ) {
                              Text("Claim (+$rewardPreview)", fontWeight = FontWeight.ExtraBold)
                            }
                          }
                        } else {
                          OutlinedTextField(
                            value = freeAnswer,
                            onValueChange = { freeAnswer = it },
                            label = { Text("Enter Your Answer or Proof") },
                            colors = OutlinedTextFieldDefaults.colors(
                              focusedBorderColor = Gold,
                              unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                              focusedTextColor = Color.White,
                              unfocusedTextColor = Color.White,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                          )
                          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                              onClick = { onSubmitAnswer(displayTask, freeAnswer) },
                              enabled = !isBusy && freeAnswer.isNotBlank() && remSec > 0L,
                              border = BorderStroke(1.dp, Cyan),
                              modifier = Modifier.weight(1f).height(48.dp).testTag("submit_task_answer_btn"),
                            ) {
                              Text("Submit Answer", color = Cyan, fontWeight = FontWeight.Bold)
                            }
                            Button(
                              onClick = { onCompleteAndClaimTask(displayTask, freeAnswer.ifBlank { "COMPLETED" }) },
                              enabled = !isBusy && remSec > 0L,
                              colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF10141F)),
                              modifier = Modifier.weight(1f).height(48.dp).testTag("claim_task_reward_btn"),
                            ) {
                              Text("Complete & Claim", fontWeight = FontWeight.ExtraBold)
                            }
                          }
                        }
                      }

                      if (!displayTask.winnerSummary.isNullOrBlank()) {
                        Surface(shape = RoundedCornerShape(10.dp), color = Gold.copy(alpha = 0.16f)) {
                          Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Gold)
                            Spacer(Modifier.width(8.dp))
                            Text("Official Result: ${displayTask.winnerSummary}", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                          }
                        }
                      }
                    }
                  }
                }

                // Completed Results History
                val completed = remember(visibleTasks) { visibleTasks.filter { it.status == "COMPLETED" } }
                if (completed.isNotEmpty()) {
                  Surface(shape = RoundedCornerShape(18.dp), color = HubCard) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      Text("COMPLETED TASK RESULTS", color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                      completed.forEach { t ->
                        Text("• ${t.title}: ${t.winnerSummary ?: "Completed"}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                      }
                    }
                  }
                }
              }
            }

            1 -> {
              val memberByUid = remember(members) { members.associateBy { it.userId } }
              val activeVips = remember(vipPrivileges) { vipPrivileges.filter { it.effectiveVip } }
              val canOpenVipRoom = remember(currentUserId, isAdminVerified, season, vipPrivileges) {
                Phase11VipLogic.hasVipRoomAccess(currentUserId, isAdminVerified, season, vipPrivileges)
              }
              Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Surface(shape = RoundedCornerShape(18.dp), color = HubCard, border = BorderStroke(1.dp, Gold.copy(alpha = 0.55f))) {
                  Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Gold)
                      Spacer(Modifier.width(8.dp))
                      Text("CURRENT HOUSE CAPTAIN & POWERS", color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                      if (!season?.captainDisplayName.isNullOrBlank()) {
                        "👑 ${season?.captainDisplayName} (${season?.captainPlayerId}) — ${season?.captainTitle ?: "House Captain"}"
                      } else {
                        "No Captain declared yet for this round."
                      },
                      color = Color.White,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                    )
                    if (!season?.captainUid.isNullOrBlank()) {
                      Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                      ) {
                        val powerTags = listOf(
                          "VIP Room" to (season?.captainVipAccess ?: true),
                          "Immunity" to (season?.captainImmunity ?: false),
                          "2x Vote" to (season?.captainDoubleVote ?: false),
                          "Tie-Breaker" to (season?.captainTieBreaker ?: false),
                        )
                        powerTags.forEach { (pLabel, active) ->
                          Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (active) Emerald.copy(alpha = 0.18f) else HubElevated,
                            border = BorderStroke(1.dp, if (active) Emerald else Color.White.copy(alpha = 0.12f)),
                          ) {
                            Text(
                              text = "${if (active) "✓" else "✕"} $pLabel",
                              color = if (active) Emerald else Muted,
                              fontSize = 11.sp,
                              fontWeight = FontWeight.Bold,
                              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                          }
                        }
                      }
                    }
                  }
                }

                // VIP Room Access & Privilege Information Panel
                Surface(
                  shape = RoundedCornerShape(18.dp),
                  color = HubCard,
                  border = BorderStroke(1.dp, if (canOpenVipRoom) Emerald.copy(alpha = 0.65f) else Cyan.copy(alpha = 0.35f)),
                  modifier = Modifier.testTag("player_vip_info_panel"),
                ) {
                  Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = if (canOpenVipRoom) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = if (canOpenVipRoom) Emerald else Muted,
                      )
                      Spacer(Modifier.width(8.dp))
                      Text(
                        text = if (canOpenVipRoom) "CAPTAIN VIP ROOM ACCESS: GRANTED" else "CAPTAIN VIP ROOM ACCESS: LOCKED",
                        color = if (canOpenVipRoom) Emerald else Muted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                      )
                    }
                    Text(
                      text = if (canOpenVipRoom) {
                        "Your account can unlock the Captain VIP Suite door and use VIP Room furniture inside the 3D House."
                      } else {
                        "Only the House Captain (with VIP Access enabled), VIP Pass holders, or Admin can enter the Captain VIP Suite."
                      },
                      color = Color.White,
                      style = MaterialTheme.typography.bodySmall,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Rounded.Diamond, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(6.dp))
                      Text("VIP PASS HOLDERS (${activeVips.size})", color = Cyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    if (activeVips.isEmpty()) {
                      Text("No separate VIP passes active.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    } else {
                      activeVips.forEach { vip ->
                        Text("💎 ${vip.displayName} (${vip.vipLabel})", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                      }
                    }
                    if (privilegeHistory.isNotEmpty()) {
                      Text("RECENT PRIVILEGE UPDATES", color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                      privilegeHistory.take(5).forEach { rec ->
                        Text("• ${rec.displaySummary}", color = Muted, style = MaterialTheme.typography.labelSmall)
                      }
                    }
                  }
                }

                Surface(shape = RoundedCornerShape(18.dp), color = HubCard) {
                  Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Rounded.Groups, contentDescription = null, tint = Cyan)
                      Spacer(Modifier.width(8.dp))
                      Text("HOUSE TEAMS & SCORES (${teams.size})", color = Cyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    if (teams.isEmpty()) {
                      Text("Admin has not divided players into teams yet.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    } else {
                      teams.forEach { team ->
                        val swatch = runCatching { Color(android.graphics.Color.parseColor(team.colorHex)) }.getOrDefault(Gold)
                        Surface(shape = RoundedCornerShape(12.dp), color = HubElevated, border = BorderStroke(1.dp, swatch.copy(alpha = 0.55f))) {
                          Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                              Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(swatch))
                                Spacer(Modifier.width(8.dp))
                                Text(team.name, color = Color.White, fontWeight = FontWeight.Bold)
                              }
                              Text("${team.score} pts • ${team.winsCount} wins", color = Gold, fontWeight = FontWeight.ExtraBold)
                            }
                            val rosterNames = team.memberUids.mapNotNull { uid -> memberByUid[uid]?.displayName ?: uid.take(6) }
                            Text(
                              if (rosterNames.isEmpty()) "No members assigned" else "Members: ${rosterNames.joinToString(", ")}",
                              color = Muted,
                              style = MaterialTheme.typography.bodySmall,
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            2 -> {
              Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Rounded.Gavel, contentDescription = null, tint = Gold)
                  Spacer(Modifier.width(8.dp))
                  Text("OFFICIAL BIGG BOSS HOUSE RULES", color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                val activeRules = rules.filter { it.active }
                if (activeRules.isEmpty()) {
                  Text("House Rules will be published shortly by Admin.", color = Muted, style = MaterialTheme.typography.bodySmall)
                } else {
                  activeRules.forEach { r ->
                    Surface(shape = RoundedCornerShape(14.dp), color = HubCard) {
                      Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("#${r.ruleNumber} • ${r.title} (${r.severity})", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(r.description, color = Muted, style = MaterialTheme.typography.bodySmall)
                      }
                    }
                  }
                }

                val myViolations = violations.filter { it.targetUid == currentUserId }
                if (myViolations.isNotEmpty()) {
                  Surface(shape = RoundedCornerShape(14.dp), color = Danger.copy(alpha = 0.14f), border = BorderStroke(1.dp, Danger)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Text("YOUR RULE VIOLATION WARNINGS (${myViolations.size})", color = Danger, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                      myViolations.forEach { v ->
                        Text("[${v.warningLevel}] ${v.ruleTitle}: ${v.note}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                      }
                    }
                  }
                }
              }
            }

            3 -> {
              Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Rounded.Schedule, contentDescription = null, tint = Cyan)
                  Spacer(Modifier.width(8.dp))
                  Text("DAILY HOUSE ACTIVITIES SCHEDULE", color = Cyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                if (scheduleSlots.isEmpty()) {
                  Text("Daily routine not published yet.", color = Muted, style = MaterialTheme.typography.bodySmall)
                } else {
                  scheduleSlots.forEach { s ->
                    Surface(
                      shape = RoundedCornerShape(14.dp),
                      color = if (s.isCurrent) HubElevated else HubCard,
                      border = BorderStroke(1.dp, if (s.isCurrent) Emerald else Color.White.copy(alpha = 0.08f)),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text("${s.orderIndex}. ${s.title}", color = if (s.isCurrent) Emerald else Color.White, fontWeight = FontWeight.Bold)
                          Text("${s.timeLabel} • ${s.location}", color = Cyan, style = MaterialTheme.typography.labelSmall)
                          Text(s.description, color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        if (s.isCurrent) {
                          Surface(shape = RoundedCornerShape(999.dp), color = Emerald.copy(alpha = 0.2f)) {
                            Text("ACTIVE NOW", color = Emerald, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            4 -> {
              val totalHousePts = leaderboard.sumOf { it.housePointsContribution }
              val myEntry = leaderboard.firstOrNull { it.userId == currentUserId }
              Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Surface(shape = RoundedCornerShape(18.dp), color = HubCard, border = BorderStroke(1.dp, Gold.copy(alpha = 0.55f))) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column {
                      Text("HOUSE POINTS POOL", color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                      Text("$totalHousePts Season House Points", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("You: ${myEntry?.points ?: 0L} pts", color = Cyan, fontWeight = FontWeight.ExtraBold)
                  }
                }

                Surface(shape = RoundedCornerShape(18.dp), color = HubCard) {
                  Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("YOUR UNLOCKED ACHIEVEMENTS (${myEntry?.achievements?.size ?: 0}/${Phase8Catalog.achievementTitles.size})", color = Cyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Phase8Catalog.achievementTitles.entries.forEach { (code, achTitle) ->
                        val unlocked = myEntry?.achievements?.contains(code) == true
                        Surface(
                          shape = RoundedCornerShape(12.dp),
                          color = if (unlocked) Gold.copy(alpha = 0.2f) else HubElevated,
                          border = BorderStroke(1.dp, if (unlocked) Gold else Color.White.copy(alpha = 0.08f)),
                        ) {
                          Column(modifier = Modifier.padding(10.dp)) {
                            Text("${if (unlocked) "🏆" else "🔒"} $achTitle", color = if (unlocked) Gold else Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(if (unlocked) "Unlocked" else code.replace("_", " "), color = Muted, fontSize = 10.sp)
                          }
                        }
                      }
                    }
                  }
                }

                Surface(shape = RoundedCornerShape(18.dp), color = HubCard) {
                  Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TOP HOUSEMATE LEADERBOARD", color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    if (leaderboard.isEmpty()) {
                      Text("No points awarded yet. Complete tasks or cook a meal to earn House Points!", color = Muted, style = MaterialTheme.typography.bodySmall)
                    } else {
                      leaderboard.forEachIndexed { idx, entry ->
                        val isMe = entry.userId == currentUserId
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isMe) Gold.copy(alpha = 0.16f) else HubElevated)
                            .padding(10.dp),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically,
                        ) {
                          Column(modifier = Modifier.weight(1f)) {
                            Text("#${idx + 1} • ${entry.displayName}${if (isMe) " (You)" else ""}", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${entry.tasksCompleted} tasks • ${entry.mealsCooked} cooked meals", color = Muted, style = MaterialTheme.typography.labelSmall)
                          }
                          Text("${entry.points} pts", color = Gold, fontWeight = FontWeight.ExtraBold)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
