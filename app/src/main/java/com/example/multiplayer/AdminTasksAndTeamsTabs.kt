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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

private val CardBg = Color(0xFF151E33)
private val ElevatedBg = Color(0xFF1C2742)
private val Gold = Color(0xFFEAB954)
private val Cyan = Color(0xFF22D3EE)
private val Emerald = Color(0xFF34D399)
private val Danger = Color(0xFFF87171)
private val Muted = Color(0xFF94A3B8)

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedBorderColor = Cyan,
  unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
  focusedTextColor = Color.White,
  unfocusedTextColor = Color.White,
  focusedLabelColor = Cyan,
  unfocusedLabelColor = Muted,
)

@Composable
fun AdminTasksManagementTab(
  tasks: List<HouseTask>,
  activeTask: HouseTask?,
  submissions: List<TaskSubmission>,
  members: List<SeasonMember> = emptyList(),
  teams: List<HouseTeam> = emptyList(),
  taskProgressList: List<TaskPlayerProgress> = emptyList(),
  isBusy: Boolean,
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
  onDeclareWinner: (HouseTask, TaskSubmission?) -> Unit,
) {
  val initialPreset = Phase6Catalog.presetFor(TaskActivityType.QUIZ)
  var editingTaskId by remember { mutableStateOf<String?>(null) }
  var selectedType by remember { mutableStateOf(initialPreset.activityType) }
  var selectedCategory by remember { mutableStateOf(initialPreset.defaultCategory) }
  var selectedDifficulty by remember { mutableStateOf(initialPreset.defaultDifficulty) }
  var title by remember { mutableStateOf(initialPreset.title) }
  var description by remember { mutableStateOf(initialPreset.description) }
  var instructions by remember { mutableStateOf(initialPreset.instructions) }
  var assignmentMode by remember { mutableStateOf(initialPreset.defaultMode) }
  var assignedUids by remember { mutableStateOf<Set<String>>(emptySet()) }
  var assignedTeamId by remember { mutableStateOf<String?>(null) }
  var requiredCheckpoints by remember { mutableStateOf(initialPreset.requiredCheckpoints) }
  var requiredRecipeId by remember { mutableStateOf(initialPreset.requiredRecipeId) }
  var rewardAchievement by remember { mutableStateOf(initialPreset.rewardAchievement) }
  var isCaptaincy by remember { mutableStateOf(false) }
  var targetRoom by remember { mutableStateOf(initialPreset.targetRoom) }
  var durationText by remember { mutableStateOf(initialPreset.defaultDurationSec.toString()) }
  var promptQuestion by remember { mutableStateOf(initialPreset.promptQuestion) }
  var optionsCsv by remember { mutableStateOf(initialPreset.options.joinToString(" | ")) }
  var expectedAnswer by remember { mutableStateOf(initialPreset.expectedAnswer) }
  var maxPointsText by remember { mutableStateOf("100") }

  val activeMembers = remember(members) {
    members.filter { it.status !in listOf("EVICTED", "BANNED", "REMOVED") }
  }

  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(activeTask?.status, activeTask?.endTimeMillis) {
    while (activeTask != null && TaskLifecycleStatus.fromRaw(activeTask.status).isPlayable()) {
      nowMillis = System.currentTimeMillis()
      delay(1000L)
    }
  }

  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Active/Selected Task Control Card
    if (activeTask != null) {
      val remSec = Phase6Logic.remainingSeconds(activeTask.endTimeMillis, nowMillis)
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "SELECTED TASK • ${activeTask.status} ${if (activeTask.isCaptaincy) "• 👑 CAPTAINCY" else ""}",
                color = Gold,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(activeTask.title, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Text(
                "${TaskActivityType.fromRaw(activeTask.activityType).label} • ${activeTask.assignmentMode} • ${activeTask.targetRoom}",
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
              )
            }
            if (activeTask.status == "ACTIVE") {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Emerald.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, Emerald),
              ) {
                Text(
                  text = Phase6Logic.formatCountdown(remSec),
                  color = Emerald,
                  fontWeight = FontWeight.ExtraBold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
              }
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
              onClick = { onUpdateTaskStatus(activeTask, "ACTIVE") },
              enabled = !isBusy && activeTask.status != "ACTIVE",
              colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color(0xFF06281E)),
              modifier = Modifier.weight(1f).height(48.dp).testTag("admin_start_task_btn"),
            ) {
              Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Start", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = { onUpdateTaskStatus(activeTask, "PAUSED") },
              enabled = !isBusy && TaskLifecycleStatus.fromRaw(activeTask.status).isPlayable(),
              border = BorderStroke(1.dp, Gold),
              modifier = Modifier.weight(1f).height(48.dp),
            ) {
              Icon(Icons.Rounded.Pause, contentDescription = null, tint = Gold, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Pause", color = Gold, fontSize = 11.sp)
            }
            OutlinedButton(
              onClick = { onUpdateTaskStatus(activeTask, "COMPLETED") },
              enabled = !isBusy && activeTask.status in listOf("ACTIVE", "IN_PROGRESS", "ASSIGNED", "PAUSED"),
              border = BorderStroke(1.dp, Cyan),
              modifier = Modifier.weight(1f).height(48.dp),
            ) {
              Icon(Icons.Rounded.Stop, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Stop", color = Cyan, fontSize = 11.sp)
            }
            OutlinedButton(
              onClick = { onUpdateTaskStatus(activeTask, "RESET") },
              enabled = !isBusy,
              border = BorderStroke(1.dp, Cyan),
              modifier = Modifier.weight(1f).height(48.dp).testTag("admin_reset_task_btn"),
            ) {
              Text("Reset", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = { onUpdateTaskStatus(activeTask, "CANCELLED") },
              enabled = !isBusy && activeTask.status !in listOf("COMPLETED", "CANCELLED"),
              border = BorderStroke(1.dp, Danger),
              modifier = Modifier.weight(1f).height(48.dp),
            ) {
              Icon(Icons.Rounded.Cancel, contentDescription = null, tint = Danger, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Cancel", color = Danger, fontSize = 11.sp)
            }
          }

          if (taskProgressList.isNotEmpty()) {
            Text("LIVE PLAYER PROGRESS (${taskProgressList.size})", color = Cyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            taskProgressList.forEach { prog ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(ElevatedBg)
                  .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("${prog.displayName} (${prog.status})", color = Color.White, style = MaterialTheme.typography.labelSmall)
                Text("${prog.percent}% • ${prog.completedSteps.size} steps", color = Emerald, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
            }
          }

          if (!activeTask.winnerSummary.isNullOrBlank()) {
            Surface(shape = RoundedCornerShape(10.dp), color = Emerald.copy(alpha = 0.14f)) {
              Text(
                text = "Result: ${activeTask.winnerSummary}",
                color = Color(0xFFA7F3D0),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(8.dp),
              )
            }
          }

          // Submissions & Result Declaration
          Text(
            "PLAYER SUBMISSIONS (${submissions.size})",
            color = Cyan,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
          )
          if (submissions.isEmpty()) {
            Text("Waiting for housemates to submit entries.", color = Muted, style = MaterialTheme.typography.bodySmall)
          } else {
            submissions.forEach { sub ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(ElevatedBg)
                  .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    "${sub.displayName} (${sub.playerId}) • ${sub.elapsedSeconds}s",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                  )
                  Text(
                    "Answer: ${sub.answerText} • ${sub.verificationStatus} (${sub.awardedScore} pts)",
                    color = if (sub.verificationStatus == "VERIFIED") Emerald else Muted,
                    style = MaterialTheme.typography.labelSmall,
                  )
                }
                OutlinedButton(
                  onClick = { onDeclareWinner(activeTask, sub) },
                  enabled = !isBusy,
                  border = BorderStroke(1.dp, Gold),
                  modifier = Modifier.heightIn(min = 48.dp),
                ) {
                  Icon(Icons.Rounded.EmojiEvents, contentDescription = "Declare Winner", tint = Gold, modifier = Modifier.size(15.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Winner", color = Gold, fontSize = 11.sp)
                }
              }
            }
          }

          Button(
            onClick = { onDeclareWinner(activeTask, null) },
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF10141F)),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auto_grade_task_btn"),
          ) {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Auto-Grade & Declare Result", fontWeight = FontWeight.ExtraBold)
          }
        }
      }
    }

    // Create / Edit Task Composer
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = CardBg,
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            if (editingTaskId == null) "CREATE NEW HOUSE TASK (PHASE 8)" else "EDIT TASK",
            color = Cyan,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
              onClick = {
                val (pickedPreset, pickedUids) = Phase8Logic.generateFairTaskAssignment(
                  presets = Phase6Catalog.taskPresets,
                  recentTasks = tasks,
                  eligibleMembers = activeMembers,
                  desiredCategory = selectedCategory,
                )
                selectedType = pickedPreset.activityType
                selectedCategory = pickedPreset.defaultCategory
                selectedDifficulty = pickedPreset.defaultDifficulty
                title = pickedPreset.title
                description = pickedPreset.description
                instructions = pickedPreset.instructions
                targetRoom = pickedPreset.targetRoom
                promptQuestion = pickedPreset.promptQuestion
                optionsCsv = pickedPreset.options.joinToString(" | ")
                expectedAnswer = pickedPreset.expectedAnswer
                durationText = pickedPreset.defaultDurationSec.toString()
                assignmentMode = pickedPreset.defaultMode
                assignedUids = pickedUids.toSet()
                assignedTeamId = if (pickedPreset.defaultMode == "TEAM") teams.firstOrNull()?.id else null
                requiredCheckpoints = pickedPreset.requiredCheckpoints
                requiredRecipeId = pickedPreset.requiredRecipeId
                rewardAchievement = pickedPreset.rewardAchievement
              },
              border = BorderStroke(1.dp, Gold),
              modifier = Modifier.heightIn(min = 48.dp).testTag("fair_randomize_task_btn"),
            ) {
              Text("Fair Randomize", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            if (editingTaskId != null) {
              OutlinedButton(
                onClick = { editingTaskId = null },
                modifier = Modifier.heightIn(min = 48.dp),
              ) {
                Text("New Blank", color = Cyan, fontSize = 11.sp)
              }
            }
          }
        }

        // 9 Activity Type Selector Chips
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          TaskActivityType.entries.forEach { act ->
            val selected = selectedType == act
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = if (selected) Cyan else ElevatedBg,
              modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable {
                  selectedType = act
                  val p = Phase6Catalog.presetFor(act)
                  title = p.title
                  description = p.description
                  instructions = p.instructions
                  targetRoom = p.targetRoom
                  promptQuestion = p.promptQuestion
                  optionsCsv = p.options.joinToString(" | ")
                  expectedAnswer = p.expectedAnswer
                  durationText = p.defaultDurationSec.toString()
                  assignmentMode = p.defaultMode
                  selectedCategory = p.defaultCategory
                  selectedDifficulty = p.defaultDifficulty
                  requiredCheckpoints = p.requiredCheckpoints
                  requiredRecipeId = p.requiredRecipeId
                  rewardAchievement = p.rewardAchievement
                }
                .testTag("task_preset_${act.name}"),
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                  text = act.label,
                  color = if (selected) Color(0xFF061622) else Color.White,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }

        // 8 Categories + 4 Difficulties
        Text("CATEGORY", color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          TaskCategory.entries.forEach { cat ->
            val picked = selectedCategory == cat
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = if (picked) Gold else ElevatedBg,
              modifier = Modifier.heightIn(min = 48.dp).clickable { selectedCategory = cat },
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(cat.label, color = if (picked) Color(0xFF10141F) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        Text("DIFFICULTY", color = Cyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
          TaskDifficulty.entries.forEach { diff ->
            val picked = selectedDifficulty == diff
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (picked) Emerald else ElevatedBg,
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).clickable { selectedDifficulty = diff },
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                Text(
                  "${diff.label} (${diff.scoreMultiplier}x)",
                  color = if (picked) Color(0xFF06281E) else Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Task Title") },
          singleLine = true,
          colors = darkFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Short Description") },
          colors = darkFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = instructions,
          onValueChange = { instructions = it },
          label = { Text("Detailed Instructions (Shown to Housemates)") },
          colors = darkFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )

        // Target Assignment Mode
        Text("ASSIGNMENT MODE", color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
          listOf("INDIVIDUAL", "GROUP", "TEAM", "HOUSE").forEach { mode ->
            val picked = assignmentMode == mode
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (picked) Cyan else ElevatedBg,
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).clickable { assignmentMode = mode },
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                Text(mode, color = if (picked) Color(0xFF061622) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        if (assignmentMode == "INDIVIDUAL" || assignmentMode == "GROUP") {
          Text("ASSIGN TARGET PLAYERS (${assignedUids.size} selected — empty = all players)", color = Muted, style = MaterialTheme.typography.labelSmall)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            activeMembers.forEach { m ->
              val picked = assignedUids.contains(m.userId)
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (picked) Gold.copy(alpha = 0.25f) else ElevatedBg,
                border = BorderStroke(1.dp, if (picked) Gold else Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.heightIn(min = 48.dp).clickable {
                  assignedUids = if (picked) assignedUids - m.userId else assignedUids + m.userId
                },
              ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                  Text(m.displayName, color = if (picked) Gold else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        } else if (assignmentMode == "TEAM" && teams.isNotEmpty()) {
          Text("ASSIGN TARGET TEAM", color = Muted, style = MaterialTheme.typography.labelSmall)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            teams.forEach { team ->
              val picked = assignedTeamId == team.id
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (picked) Cyan else ElevatedBg,
                modifier = Modifier.heightIn(min = 48.dp).clickable { assignedTeamId = if (picked) null else team.id },
              ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                  Text(team.name, color = if (picked) Color(0xFF061622) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ElevatedBg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Captaincy Task", color = Gold, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
          Switch(
            checked = isCaptaincy,
            onCheckedChange = { isCaptaincy = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Gold),
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = targetRoom,
            onValueChange = { targetRoom = it },
            label = { Text("Indoor Room") },
            singleLine = true,
            colors = darkFieldColors(),
            modifier = Modifier.weight(1.3f),
          )
          OutlinedTextField(
            value = durationText,
            onValueChange = { if (it.all(Char::isDigit)) durationText = it },
            label = { Text("Seconds") },
            singleLine = true,
            colors = darkFieldColors(),
            modifier = Modifier.weight(0.85f),
          )
          OutlinedTextField(
            value = maxPointsText,
            onValueChange = { if (it.all(Char::isDigit)) maxPointsText = it },
            label = { Text("Points") },
            singleLine = true,
            colors = darkFieldColors(),
            modifier = Modifier.weight(0.85f),
          )
        }

        OutlinedTextField(
          value = promptQuestion,
          onValueChange = { promptQuestion = it },
          label = { Text("Challenge Prompt / Question") },
          colors = darkFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = optionsCsv,
          onValueChange = { optionsCsv = it },
          label = { Text("Answer Options (separate with |, leave blank for open input/3D)") },
          colors = darkFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = expectedAnswer,
          onValueChange = { expectedAnswer = it },
          label = { Text("Secret Expected Answer (Admin-only Firestore vault)") },
          singleLine = true,
          colors = darkFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedButton(
            onClick = {
              onSaveOrStartTask(
                editingTaskId,
                title,
                description,
                instructions,
                selectedType,
                assignmentMode,
                isCaptaincy,
                targetRoom,
                durationText.toLongOrNull() ?: 180L,
                promptQuestion,
                optionsCsv.split("|").map { it.trim() }.filter { it.isNotBlank() },
                maxPointsText.toLongOrNull() ?: 100L,
                expectedAnswer,
                false,
                selectedCategory,
                selectedDifficulty,
                assignedUids.toList(),
                assignedTeamId,
                requiredCheckpoints,
                requiredRecipeId,
                rewardAchievement,
              )
              editingTaskId = null
            },
            enabled = !isBusy && title.trim().length >= 3,
            border = BorderStroke(1.dp, Cyan),
            modifier = Modifier.weight(1f).height(48.dp).testTag("save_draft_task_btn"),
          ) {
            Text("Save Draft", color = Cyan, fontWeight = FontWeight.Bold)
          }
          Button(
            onClick = {
              onSaveOrStartTask(
                editingTaskId,
                title,
                description,
                instructions,
                selectedType,
                assignmentMode,
                isCaptaincy,
                targetRoom,
                durationText.toLongOrNull() ?: 180L,
                promptQuestion,
                optionsCsv.split("|").map { it.trim() }.filter { it.isNotBlank() },
                maxPointsText.toLongOrNull() ?: 100L,
                expectedAnswer,
                true,
                selectedCategory,
                selectedDifficulty,
                assignedUids.toList(),
                assignedTeamId,
                requiredCheckpoints,
                requiredRecipeId,
                rewardAchievement,
              )
              editingTaskId = null
            },
            enabled = !isBusy && title.trim().length >= 3,
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF10141F)),
            modifier = Modifier.weight(1.3f).height(48.dp).testTag("publish_start_task_btn"),
          ) {
            Text("Publish & Start Live", fontWeight = FontWeight.ExtraBold)
          }
        }
      }
    }

    // Task History & Selector
    Surface(shape = RoundedCornerShape(18.dp), color = CardBg) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("TASK HISTORY & ARCHIVE (${tasks.size})", color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        if (tasks.isEmpty()) {
          Text("No tasks created in this season yet.", color = Muted, style = MaterialTheme.typography.bodySmall)
        } else {
          tasks.forEach { t ->
            val isSelected = t.id == activeTask?.id
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) Gold.copy(alpha = 0.14f) else ElevatedBg,
              border = BorderStroke(1.dp, if (isSelected) Gold else Color.White.copy(alpha = 0.08f)),
              modifier = Modifier.fillMaxWidth().clickable { onSelectTask(t.id) },
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(t.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Text(
                    "${t.category} • ${t.difficulty} • ${t.status} • ${t.winnerSummary ?: t.targetRoom}",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                  )
                }
                OutlinedButton(
                  onClick = {
                    editingTaskId = t.id
                    selectedType = TaskActivityType.fromRaw(t.activityType)
                    selectedCategory = TaskCategory.fromRaw(t.category)
                    selectedDifficulty = TaskDifficulty.fromRaw(t.difficulty)
                    title = t.title
                    description = t.description
                    instructions = t.instructions
                    assignmentMode = t.assignmentMode
                    assignedUids = t.assignedUids.toSet()
                    assignedTeamId = t.assignedTeamId
                    requiredCheckpoints = t.requiredCheckpoints
                    requiredRecipeId = t.requiredRecipeId
                    rewardAchievement = t.rewardAchievement ?: "TASK_FINISHER"
                    isCaptaincy = t.isCaptaincy
                    targetRoom = t.targetRoom
                    durationText = t.durationSeconds.toString()
                    promptQuestion = t.promptQuestion
                    optionsCsv = t.options.joinToString(" | ")
                    maxPointsText = t.maxPoints.toString()
                    onSelectTask(t.id)
                  },
                  modifier = Modifier.heightIn(min = 48.dp),
                ) {
                  Text("Edit", color = Cyan, fontSize = 11.sp)
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
fun AdminTeamsAndCaptainTab(
  season: SeasonSession?,
  members: List<SeasonMember>,
  teams: List<HouseTeam>,
  captaincyHistory: List<CaptaincyRecord>,
  isBusy: Boolean,
  onSaveTeam: (
    teamId: String?,
    name: String,
    colorHex: String,
    memberUids: List<String>,
    score: Long,
    winsCount: Long,
  ) -> Unit,
  onAssignCaptain: (member: SeasonMember, title: String, scoreAwarded: Long) -> Unit,
) {
  val activeMembers = remember(members) {
    members.filter { it.status !in listOf("EVICTED", "BANNED", "REMOVED") }
  }
  val palette = listOf("#EAB954", "#22D3EE", "#34D399", "#F87171", "#A78BFA", "#FB923C")

  var editingTeamId by remember { mutableStateOf<String?>(null) }
  var teamName by remember { mutableStateOf("Team Phoenix") }
  var teamColor by remember { mutableStateOf(palette.first()) }
  var teamScoreText by remember { mutableStateOf("0") }
  var teamWinsText by remember { mutableStateOf("0") }
  var selectedUids by remember { mutableStateOf<Set<String>>(emptySet()) }
  var captainTitle by remember { mutableStateOf("House Captain") }

  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Captaincy Panel
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = CardBg,
      border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("HOUSE CAPTAIN MANAGEMENT", color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(
          text = if (!season?.captainDisplayName.isNullOrBlank()) {
            "👑 Current Captain: ${season?.captainDisplayName} (${season?.captainPlayerId}) — ${season?.captainTitle ?: "House Captain"}"
          } else {
            "No House Captain assigned yet. Pick a contestant below or run a Captaincy Task."
          },
          color = Color.White,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
          value = captainTitle,
          onValueChange = { captainTitle = it },
          label = { Text("Captain Badge Title") },
          singleLine = true,
          colors = darkFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )
        if (activeMembers.isEmpty()) {
          Text("No active housemates joined yet.", color = Muted, style = MaterialTheme.typography.bodySmall)
        } else {
          activeMembers.forEach { m ->
            val isCurrent = season?.captainUid == m.userId
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isCurrent) Gold.copy(alpha = 0.16f) else ElevatedBg)
                .padding(horizontal = 10.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${if (isCurrent) "👑 " else ""}${m.displayName}", color = Color.White, fontWeight = FontWeight.Bold)
                Text("${m.playerId} • ${m.characterId}", color = Muted, style = MaterialTheme.typography.labelSmall)
              }
              Button(
                onClick = { onAssignCaptain(m, captainTitle, 100L) },
                enabled = !isBusy && !isCurrent,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF10141F)),
                modifier = Modifier.height(48.dp).testTag("assign_captain_${m.userId}"),
              ) {
                Text(if (isCurrent) "Current Captain" else "Make Captain", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // Team Builder
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = CardBg,
      border = BorderStroke(1.dp, Cyan.copy(alpha = 0.35f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          if (editingTeamId == null) "CREATE / DIVIDE HOUSE TEAM" else "EDIT TEAM",
          color = Cyan,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
          value = teamName,
          onValueChange = { teamName = it },
          label = { Text("Team Name") },
          singleLine = true,
          colors = darkFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("team_name_input"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Text("Color:", color = Muted, style = MaterialTheme.typography.bodySmall)
          palette.forEach { hex ->
            val c = Color(android.graphics.Color.parseColor(hex))
            val picked = teamColor.equals(hex, ignoreCase = true)
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(c)
                .border(if (picked) 3.dp else 1.dp, if (picked) Color.White else Color.Transparent, CircleShape)
                .clickable { teamColor = hex },
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = teamScoreText,
            onValueChange = { if (it.all(Char::isDigit)) teamScoreText = it },
            label = { Text("Team Score") },
            singleLine = true,
            colors = darkFieldColors(),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = teamWinsText,
            onValueChange = { if (it.all(Char::isDigit)) teamWinsText = it },
            label = { Text("Wins") },
            singleLine = true,
            colors = darkFieldColors(),
            modifier = Modifier.weight(1f),
          )
        }
        Text("ASSIGN TEAM MEMBERS (UP TO 15 PLAYERS)", color = Muted, style = MaterialTheme.typography.labelSmall)
        activeMembers.forEach { m ->
          val checked = selectedUids.contains(m.userId)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(ElevatedBg)
              .clickable {
                selectedUids = if (checked) selectedUids - m.userId else selectedUids + m.userId
              }
              .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Checkbox(
              checked = checked,
              onCheckedChange = { c ->
                selectedUids = if (c) selectedUids + m.userId else selectedUids - m.userId
              },
              colors = CheckboxDefaults.colors(checkedColor = Cyan),
            )
            Text("${m.displayName} (${m.playerId})", color = Color.White, style = MaterialTheme.typography.bodySmall)
          }
        }
        Button(
          onClick = {
            onSaveTeam(
              editingTeamId,
              teamName,
              teamColor,
              selectedUids.toList(),
              teamScoreText.toLongOrNull() ?: 0L,
              teamWinsText.toLongOrNull() ?: 0L,
            )
            editingTeamId = null
          },
          enabled = !isBusy && teamName.trim().length >= 2,
          colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color(0xFF061622)),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_team_button"),
        ) {
          Icon(Icons.Rounded.CheckCircle, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text("Save & Sync Team", fontWeight = FontWeight.ExtraBold)
        }
      }
    }

    // Existing Teams List
    Surface(shape = RoundedCornerShape(18.dp), color = CardBg) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("SYNCHRONIZED TEAMS (${teams.size})", color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        teams.forEach { team ->
          val swatch = runCatching { Color(android.graphics.Color.parseColor(team.colorHex)) }.getOrDefault(Gold)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(ElevatedBg)
              .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(swatch))
              Spacer(Modifier.width(8.dp))
              Column {
                Text("${team.name} • ${team.score} pts (${team.winsCount} wins)", color = Color.White, fontWeight = FontWeight.Bold)
                Text("${team.memberUids.size} members assigned", color = Muted, style = MaterialTheme.typography.labelSmall)
              }
            }
            OutlinedButton(
              onClick = {
                editingTeamId = team.id
                teamName = team.name
                teamColor = team.colorHex
                teamScoreText = team.score.toString()
                teamWinsText = team.winsCount.toString()
                selectedUids = team.memberUids.toSet()
              },
              modifier = Modifier.heightIn(min = 48.dp),
            ) {
              Text("Edit", color = Gold, fontSize = 11.sp)
            }
          }
        }
      }
    }

    if (captaincyHistory.isNotEmpty()) {
      Surface(shape = RoundedCornerShape(18.dp), color = CardBg) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("CAPTAINCY HISTORY", color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          captaincyHistory.forEach { rec ->
            Text(
              "👑 ${rec.captainDisplayName} (${rec.captainPlayerId}) — ${rec.title} (+${rec.scoreAwarded} pts)",
              color = Color.White,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      }
    }
  }
}
