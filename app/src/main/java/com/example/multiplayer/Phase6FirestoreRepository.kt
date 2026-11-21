package com.example.multiplayer

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

private const val P6_REPO_TAG = "Phase6FirestoreRepo"

class Phase6FirestoreRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  private fun seasonDoc(seasonId: String) = db.collection("seasons").document(seasonId)

  private fun requireUserId(): String =
    auth.currentUser?.uid ?: throw IllegalStateException("Authentication required.")

  fun observeTasks(seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID): Flow<List<HouseTask>> =
    callbackFlow {
      val reg = seasonDoc(seasonId).collection("tasks")
        .whereEqualTo("seasonId", seasonId)
        .addSnapshotListener { snap, err ->
          if (err != null) {
            Log.w(P6_REPO_TAG, "Error observing tasks: ${err.message}")
            return@addSnapshotListener
          }
          val list = snap?.documents
            ?.mapNotNull { it.toObject(HouseTask::class.java) }
            ?.sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
            .orEmpty()
          trySend(list)
        }
      awaitClose { reg.remove() }
    }

  fun observeTaskSubmissions(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    taskId: String,
    canReadAll: Boolean,
    currentUid: String,
  ): Flow<List<TaskSubmission>> = callbackFlow {
    val base = seasonDoc(seasonId).collection("tasks").document(taskId).collection("submissions")
      .whereEqualTo("seasonId", seasonId)
      .whereEqualTo("taskId", taskId)
    val query = if (canReadAll) base else base.whereEqualTo("userId", currentUid)
    val reg = query.addSnapshotListener { snap, err ->
      if (err != null) {
        Log.w(P6_REPO_TAG, "Error observing submissions ($taskId): ${err.message}")
        trySend(emptyList())
        return@addSnapshotListener
      }
      val list = snap?.documents
        ?.mapNotNull { it.toObject(TaskSubmission::class.java) }
        ?.sortedWith(compareByDescending<TaskSubmission> { it.awardedScore }.thenBy { it.elapsedSeconds })
        .orEmpty()
      trySend(list)
    }
    awaitClose { reg.remove() }
  }

  fun observeTeams(seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID): Flow<List<HouseTeam>> =
    callbackFlow {
      val reg = seasonDoc(seasonId).collection("teams")
        .whereEqualTo("seasonId", seasonId)
        .addSnapshotListener { snap, err ->
          if (err != null) {
            Log.w(P6_REPO_TAG, "Error observing teams: ${err.message}")
            return@addSnapshotListener
          }
          val list = snap?.documents
            ?.mapNotNull { it.toObject(HouseTeam::class.java) }
            ?.sortedWith(compareByDescending<HouseTeam> { it.score }.thenBy { it.name })
            .orEmpty()
          trySend(list)
        }
      awaitClose { reg.remove() }
    }

  fun observeCaptaincyHistory(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
  ): Flow<List<CaptaincyRecord>> = callbackFlow {
    val reg = seasonDoc(seasonId).collection("captaincyHistory")
      .whereEqualTo("seasonId", seasonId)
      .addSnapshotListener { snap, err ->
        if (err != null) {
          Log.w(P6_REPO_TAG, "Error observing captaincyHistory: ${err.message}")
          return@addSnapshotListener
        }
        val list = snap?.documents
          ?.mapNotNull { it.toObject(CaptaincyRecord::class.java) }
          ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
          .orEmpty()
        trySend(list)
      }
    awaitClose { reg.remove() }
  }

  fun observeHouseRules(seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID): Flow<List<HouseRule>> =
    callbackFlow {
      val reg = seasonDoc(seasonId).collection("houseRules")
        .whereEqualTo("seasonId", seasonId)
        .addSnapshotListener { snap, err ->
          if (err != null) {
            Log.w(P6_REPO_TAG, "Error observing houseRules: ${err.message}")
            return@addSnapshotListener
          }
          val list = snap?.documents
            ?.mapNotNull { it.toObject(HouseRule::class.java) }
            ?.sortedBy { it.ruleNumber }
            .orEmpty()
          trySend(list)
        }
      awaitClose { reg.remove() }
    }

  fun observeRuleViolations(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
  ): Flow<List<RuleViolation>> = callbackFlow {
    val reg = seasonDoc(seasonId).collection("ruleViolations")
      .whereEqualTo("seasonId", seasonId)
      .addSnapshotListener { snap, err ->
        if (err != null) {
          Log.w(P6_REPO_TAG, "Error observing ruleViolations: ${err.message}")
          return@addSnapshotListener
        }
        val list = snap?.documents
          ?.mapNotNull { it.toObject(RuleViolation::class.java) }
          ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
          .orEmpty()
        trySend(list)
      }
    awaitClose { reg.remove() }
  }

  fun observeDailySchedule(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
  ): Flow<List<DailyScheduleSlot>> = callbackFlow {
    val reg = seasonDoc(seasonId).collection("dailySchedule")
      .whereEqualTo("seasonId", seasonId)
      .addSnapshotListener { snap, err ->
        if (err != null) {
          Log.w(P6_REPO_TAG, "Error observing dailySchedule: ${err.message}")
          return@addSnapshotListener
        }
        val list = snap?.documents
          ?.mapNotNull { it.toObject(DailyScheduleSlot::class.java) }
          ?.sortedBy { it.orderIndex }
          .orEmpty()
        trySend(list)
      }
    awaitClose { reg.remove() }
  }

  suspend fun fetchAdminExpectedAnswer(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    taskId: String,
  ): String? {
    return try {
      val snap = seasonDoc(seasonId).collection("tasks").document(taskId)
        .collection("answerKey").document("config").get().await()
      snap.getString("expectedAnswer")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(P6_REPO_TAG, "Could not read admin expected answer: ${e.message}")
      null
    }
  }

  suspend fun upsertTask(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    taskId: String? = null,
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
    category: TaskCategory = TaskCategory.DAILY,
    difficulty: TaskDifficulty = TaskDifficulty.MEDIUM,
    assignedUids: List<String> = emptyList(),
    assignedTeamId: String? = null,
    requiredCheckpoints: List<String> = emptyList(),
    requiredRecipeId: String? = null,
    rewardAchievement: String? = "TASK_FINISHER",
  ): Result<HouseTask> {
    return try {
      val adminUid = requireUserId()
      val cleanTitle = title.trim().take(100)
      val cleanDesc = description.trim().take(400)
      val cleanInst = instructions.trim().take(800)
      require(cleanTitle.length >= 3) { "Task title must be at least 3 characters." }
      require(cleanDesc.length >= 3) { "Task description must be at least 3 characters." }
      require(cleanInst.length >= 3) { "Task instructions must be at least 3 characters." }

      val docId = taskId?.takeIf { it.isNotBlank() }
        ?: "task_${UUID.randomUUID().toString().replace("-", "").take(10)}"
      val taskRef = seasonDoc(seasonId).collection("tasks").document(docId)
      val existing = taskRef.get().await()

      val clampedDur = durationSeconds.coerceIn(30L, 7200L)
      val clampedPts = maxPoints.coerceIn(1L, 1000L)
      val mode = if (assignmentMode in listOf("INDIVIDUAL", "GROUP", "TEAM", "HOUSE")) assignmentMode else "INDIVIDUAL"
      val nowMs = System.currentTimeMillis()
      val nextStatus = when {
        startImmediately -> "ACTIVE"
        existing.exists() -> existing.getString("status") ?: "DRAFT"
        else -> "DRAFT"
      }
      val startMs = if (startImmediately) nowMs else (existing.getLong("startTimeMillis") ?: 0L)
      val endMs = if (startImmediately) nowMs + clampedDur * 1000L else (existing.getLong("endTimeMillis") ?: 0L)
      val cleanOptions = options.map { it.trim().take(120) }.filter { it.isNotBlank() }.take(6)
      val cleanAssignedUids = assignedUids.distinct().take(15)
      val cleanCheckpoints = requiredCheckpoints.distinct().take(12)
      val cleanTeamId = assignedTeamId?.takeIf { it.isNotBlank() }
      val cleanRecipeId = requiredRecipeId?.takeIf { it.isNotBlank() }
      val cleanAch = rewardAchievement?.takeIf { it.isNotBlank() } ?: "TASK_FINISHER"
      val cleanRoom = targetRoom.trim().ifBlank { activityType.defaultRoom }.take(60)
      val cleanPrompt = promptQuestion.trim().take(400)

      val batch = db.batch()
      if (existing.exists()) {
        batch.update(
          taskRef,
          mapOf(
            "title" to cleanTitle,
            "description" to cleanDesc,
            "instructions" to cleanInst,
            "activityType" to activityType.name,
            "assignmentMode" to mode,
            "category" to category.name,
            "difficulty" to difficulty.name,
            "isCaptaincy" to isCaptaincy,
            "status" to nextStatus,
            "targetRoom" to cleanRoom,
            "durationSeconds" to clampedDur,
            "startTimeMillis" to startMs,
            "endTimeMillis" to endMs,
            "promptQuestion" to cleanPrompt,
            "options" to cleanOptions,
            "assignedUids" to cleanAssignedUids,
            "assignedTeamId" to cleanTeamId,
            "requiredCheckpoints" to cleanCheckpoints,
            "requiredRecipeId" to cleanRecipeId,
            "rewardAchievement" to cleanAch,
            "maxPoints" to clampedPts,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      } else {
        batch.set(
          taskRef,
          mapOf(
            "seasonId" to seasonId,
            "title" to cleanTitle,
            "description" to cleanDesc,
            "instructions" to cleanInst,
            "activityType" to activityType.name,
            "assignmentMode" to mode,
            "category" to category.name,
            "difficulty" to difficulty.name,
            "isCaptaincy" to isCaptaincy,
            "status" to nextStatus,
            "targetRoom" to cleanRoom,
            "durationSeconds" to clampedDur,
            "startTimeMillis" to startMs,
            "endTimeMillis" to endMs,
            "promptQuestion" to cleanPrompt,
            "options" to cleanOptions,
            "assignedUids" to cleanAssignedUids,
            "assignedTeamId" to cleanTeamId,
            "requiredCheckpoints" to cleanCheckpoints,
            "requiredRecipeId" to cleanRecipeId,
            "progressPercent" to 0L,
            "rewardAchievement" to cleanAch,
            "maxPoints" to clampedPts,
            "winningUid" to null,
            "winningTeamId" to null,
            "winnerSummary" to null,
            "createdBy" to adminUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      val cleanExp = expectedAnswer.trim().take(240)
      if (cleanExp.isNotEmpty()) {
        batch.set(
          taskRef.collection("answerKey").document("config"),
          mapOf(
            "seasonId" to seasonId,
            "taskId" to docId,
            "expectedAnswer" to cleanExp,
            "updatedBy" to adminUid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      if (startImmediately) {
        batch.update(
          seasonDoc(seasonId),
          mapOf("activeTaskId" to docId, "updatedAt" to FieldValue.serverTimestamp()),
        )
      }

      batch.set(
        seasonDoc(seasonId).collection("adminControls").document(),
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "TASK_CONTROL",
          "payloadSummary" to "${if (startImmediately) "Started" else "Saved"} task: $cleanTitle (${category.name}/${difficulty.name})".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()

      Result.success(
        HouseTask(
          id = docId,
          seasonId = seasonId,
          title = cleanTitle,
          description = cleanDesc,
          instructions = cleanInst,
          activityType = activityType.name,
          assignmentMode = mode,
          category = category.name,
          difficulty = difficulty.name,
          isCaptaincy = isCaptaincy,
          status = nextStatus,
          targetRoom = cleanRoom,
          durationSeconds = clampedDur,
          startTimeMillis = startMs,
          endTimeMillis = endMs,
          promptQuestion = cleanPrompt,
          options = cleanOptions,
          assignedUids = cleanAssignedUids,
          assignedTeamId = cleanTeamId,
          requiredCheckpoints = cleanCheckpoints,
          requiredRecipeId = cleanRecipeId,
          progressPercent = 0L,
          rewardAchievement = cleanAch,
          maxPoints = clampedPts,
          createdBy = adminUid,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(P6_REPO_TAG, "upsertTask failed", e)
      Result.failure(e)
    }
  }

  suspend fun transitionTaskStatus(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    task: HouseTask,
    nextStatus: String,
  ): Result<Pair<Long, Long>> {
    return try {
      val adminUid = requireUserId()
      val isReset = nextStatus == "RESET"
      val effectiveStatus = if (isReset) "ACTIVE" else nextStatus
      val nowMs = System.currentTimeMillis()
      val startMs = if (effectiveStatus in listOf("ACTIVE", "IN_PROGRESS", "ASSIGNED") || isReset) nowMs else task.startTimeMillis
      val endMs = when (effectiveStatus) {
        "ACTIVE", "IN_PROGRESS", "ASSIGNED" -> nowMs + task.durationSeconds.coerceIn(30L, 7200L) * 1000L
        "COMPLETED", "CANCELLED", "FAILED", "EXPIRED" -> nowMs
        else -> task.endTimeMillis
      }

      val taskUpdate = mutableMapOf<String, Any?>(
        "status" to effectiveStatus,
        "startTimeMillis" to startMs,
        "endTimeMillis" to endMs,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      if (isReset) {
        taskUpdate["winningUid"] = null
        taskUpdate["winningTeamId"] = null
        taskUpdate["winnerSummary"] = null
        taskUpdate["progressPercent"] = 0L
      }

      val batch = db.batch()
      batch.update(seasonDoc(seasonId).collection("tasks").document(task.id), taskUpdate)
      batch.update(
        seasonDoc(seasonId),
        mapOf(
          "activeTaskId" to if (effectiveStatus in listOf("ACTIVE", "IN_PROGRESS", "ASSIGNED")) task.id else null,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("adminControls").document(),
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "TASK_CONTROL",
          "payloadSummary" to "Task ${task.title} -> $nextStatus".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(startMs to endMs)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(P6_REPO_TAG, "transitionTaskStatus failed", e)
      Result.failure(e)
    }
  }

  suspend fun submitPlayerTaskEntry(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    task: HouseTask,
    account: UserAccount,
    teamId: String?,
    answerText: String,
  ): Result<Unit> {
    return try {
      val uid = requireUserId()
      val cleanAns = answerText.trim().take(300)
      require(cleanAns.isNotEmpty()) { "Answer or challenge proof cannot be empty." }

      val taskRef = seasonDoc(seasonId).collection("tasks").document(task.id)
      val subRef = taskRef.collection("submissions").document(uid)
      val nowMs = System.currentTimeMillis()

      db.runTransaction { tx ->
        val taskSnap = tx.get(taskRef)
        val liveStatus = taskSnap.getString("status")
        if (!taskSnap.exists() || liveStatus !in listOf("ACTIVE", "IN_PROGRESS", "ASSIGNED")) {
          throw IllegalStateException("This task is not active right now.")
        }
        val endMs = taskSnap.getLong("endTimeMillis") ?: 0L
        if (endMs > 0L && nowMs > endMs + 2500L) {
          throw IllegalStateException("Time is up! Task timer has expired.")
        }
        if (tx.get(subRef).exists()) {
          throw IllegalStateException("Duplicate submission prevented: you already submitted.")
        }
        val startMs = taskSnap.getLong("startTimeMillis") ?: nowMs
        val elapsedSec = ((nowMs - startMs) / 1000L).coerceIn(0L, 7200L)

        tx.set(
          subRef,
          mapOf(
            "seasonId" to seasonId,
            "taskId" to task.id,
            "userId" to uid,
            "playerId" to account.playerId,
            "displayName" to account.displayName,
            "teamId" to teamId,
            "answerText" to cleanAns,
            "elapsedSeconds" to elapsedSec,
            "verificationStatus" to "SUBMITTED",
            "awardedScore" to 0L,
            "reviewedBy" to null,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()

      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(P6_REPO_TAG, "submitPlayerTaskEntry failed: ${e.message}")
      Result.failure(e)
    }
  }

  suspend fun gradeAndDeclareTaskWinner(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    task: HouseTask,
    submissions: List<TaskSubmission>,
    expectedAnswer: String?,
    overrideWinnerSub: TaskSubmission?,
    teams: List<HouseTeam>,
    members: List<SeasonMember>,
  ): Result<String> {
    return try {
      val adminUid = requireUserId()
      val batch = db.batch()
      val taskRef = seasonDoc(seasonId).collection("tasks").document(task.id)

      val graded = submissions.map { sub ->
        val matches = if (overrideWinnerSub != null) {
          sub.userId == overrideWinnerSub.userId || Phase6Logic.isAnswerMatch(sub.answerText, expectedAnswer)
        } else {
          Phase6Logic.isAnswerMatch(sub.answerText, expectedAnswer)
        }
        val nextStatus = if (matches) "VERIFIED" else "REJECTED"
        val pts = if (matches) task.maxPoints else 0L
        batch.update(
          taskRef.collection("submissions").document(sub.userId),
          mapOf(
            "verificationStatus" to nextStatus,
            "awardedScore" to pts,
            "reviewedBy" to adminUid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        sub.copy(verificationStatus = nextStatus, awardedScore = pts)
      }

      val winnerSub = overrideWinnerSub ?: Phase6Logic.pickAutoWinner(graded, expectedAnswer)
      val winningTeam = winnerSub?.teamId?.let { tid -> teams.firstOrNull { it.id == tid } }
        ?: if (task.assignmentMode == "TEAM") {
          winnerSub?.let { ws -> teams.firstOrNull { it.memberUids.contains(ws.userId) } }
        } else null

      val summaryText = when {
        winningTeam != null -> "${winningTeam.name} (+${task.maxPoints} pts) led by ${winnerSub?.displayName ?: "Team"}"
        winnerSub != null -> "${winnerSub.displayName} (${winnerSub.playerId}) — +${task.maxPoints} pts in ${winnerSub.elapsedSeconds}s"
        else -> "Completed — No verified winning entry"
      }.take(240)

      batch.update(
        taskRef,
        mapOf(
          "status" to "COMPLETED",
          "endTimeMillis" to System.currentTimeMillis(),
          "winningUid" to winnerSub?.userId,
          "winningTeamId" to winningTeam?.id,
          "winnerSummary" to summaryText,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )

      batch.update(
        seasonDoc(seasonId),
        mapOf("activeTaskId" to null, "updatedAt" to FieldValue.serverTimestamp()),
      )

      if (winningTeam != null) {
        batch.update(
          seasonDoc(seasonId).collection("teams").document(winningTeam.id),
          mapOf(
            "score" to (winningTeam.score + task.maxPoints).coerceAtMost(100000L),
            "winsCount" to (winningTeam.winsCount + 1L).coerceAtMost(1000L),
            "updatedBy" to adminUid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      if (task.isCaptaincy && winnerSub != null) {
        val mem = members.firstOrNull { it.userId == winnerSub.userId }
        val charId = mem?.characterId ?: "Player 01"
        batch.update(
          seasonDoc(seasonId),
          mapOf(
            "captainUid" to winnerSub.userId,
            "captainPlayerId" to winnerSub.playerId,
            "captainDisplayName" to winnerSub.displayName,
            "captainTitle" to "Task Captain (${task.title})".take(80),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        batch.set(
          seasonDoc(seasonId).collection("captaincyHistory").document(),
          mapOf(
            "seasonId" to seasonId,
            "taskId" to task.id,
            "captainUid" to winnerSub.userId,
            "captainPlayerId" to winnerSub.playerId,
            "captainDisplayName" to winnerSub.displayName,
            "captainCharacterId" to charId,
            "title" to "Task Captain (${task.title})".take(80),
            "scoreAwarded" to task.maxPoints,
            "assignedBy" to adminUid,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      if (winnerSub != null) {
        val grantId = Phase8Logic.grantIdForTask(task.id, winnerSub.userId)
        val grantRef = seasonDoc(seasonId).collection("rewardGrants").document(grantId)
        val lbRef = seasonDoc(seasonId).collection("leaderboard").document(winnerSub.userId)
        val existingGrant = grantRef.get().await()
        if (!existingGrant.exists()) {
          val lbSnap = lbRef.get().await()
          val lb = lbSnap.toObject(PlayerLeaderboardEntry::class.java)
          val ach = task.rewardAchievement?.takeIf { it.isNotBlank() } ?: "TASK_FINISHER"
          val pts = task.maxPoints.coerceIn(1L, 1000L)
          batch.set(
            grantRef,
            mapOf(
              "seasonId" to seasonId,
              "grantId" to grantId,
              "userId" to winnerSub.userId,
              "sourceType" to "TASK",
              "sourceId" to task.id,
              "awardedPoints" to pts,
              "achievementId" to ach,
              "grantedBy" to adminUid,
              "createdAt" to FieldValue.serverTimestamp(),
            ),
          )
          if (lb != null) {
            batch.update(
              lbRef,
              mapOf(
                "points" to (lb.points + pts).coerceAtMost(1000000L),
                "housePointsContribution" to (lb.housePointsContribution + pts).coerceAtMost(1000000L),
                "tasksCompleted" to (lb.tasksCompleted + 1L).coerceAtMost(10000L),
                "achievements" to (lb.achievements + ach).distinct().take(24),
                "lastGrantId" to grantId,
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            )
          } else {
            val mem = members.firstOrNull { it.userId == winnerSub.userId }
            batch.set(
              lbRef,
              mapOf(
                "seasonId" to seasonId,
                "userId" to winnerSub.userId,
                "playerId" to winnerSub.playerId,
                "displayName" to winnerSub.displayName.take(24),
                "characterId" to (mem?.characterId ?: "Player 01"),
                "teamId" to winnerSub.teamId,
                "points" to pts,
                "housePointsContribution" to pts,
                "tasksCompleted" to 1L,
                "mealsCooked" to 0L,
                "achievements" to listOf(ach),
                "lastGrantId" to grantId,
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            )
          }
        }
      }

      batch.set(
        seasonDoc(seasonId).collection("adminControls").document(),
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "TASK_CONTROL",
          "payloadSummary" to "Declared result: $summaryText".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )

      batch.commit().await()
      Result.success(summaryText)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(P6_REPO_TAG, "gradeAndDeclareTaskWinner failed", e)
      Result.failure(e)
    }
  }

  suspend fun upsertTeam(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    teamId: String? = null,
    name: String,
    colorHex: String,
    memberUids: List<String>,
    score: Long,
    winsCount: Long,
  ): Result<String> {
    return try {
      val adminUid = requireUserId()
      val cleanName = name.trim().take(40)
      require(cleanName.length >= 2) { "Team name must be at least 2 characters." }
      val cleanColor = if (Regex("^#[0-9a-fA-F]{6}$").matches(colorHex)) colorHex else "#EAB954"
      val cleanMembers = memberUids.distinct().take(15)
      val docId = teamId?.takeIf { it.isNotBlank() }
        ?: "team_${UUID.randomUUID().toString().replace("-", "").take(8)}"
      val ref = seasonDoc(seasonId).collection("teams").document(docId)
      val existing = ref.get().await()

      if (existing.exists()) {
        ref.update(
          mapOf(
            "name" to cleanName,
            "colorHex" to cleanColor,
            "memberUids" to cleanMembers,
            "score" to score.coerceIn(0L, 100000L),
            "winsCount" to winsCount.coerceIn(0L, 1000L),
            "updatedBy" to adminUid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      } else {
        ref.set(
          mapOf(
            "seasonId" to seasonId,
            "name" to cleanName,
            "colorHex" to cleanColor,
            "memberUids" to cleanMembers,
            "score" to score.coerceIn(0L, 100000L),
            "winsCount" to winsCount.coerceIn(0L, 1000L),
            "updatedBy" to adminUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      }

      seasonDoc(seasonId).collection("adminControls").document().set(
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "TEAM_ASSIGN",
          "payloadSummary" to "Saved Team $cleanName (${cleanMembers.size} players, $score pts)".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      ).await()

      Result.success(docId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun assignCaptain(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    targetUid: String,
    playerId: String,
    displayName: String,
    characterId: String,
    title: String,
    scoreAwarded: Long,
    taskId: String? = null,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val cleanTitle = title.trim().ifBlank { "House Captain" }.take(80)
      val batch = db.batch()
      batch.update(
        seasonDoc(seasonId),
        mapOf(
          "captainUid" to targetUid,
          "captainPlayerId" to playerId,
          "captainDisplayName" to displayName,
          "captainTitle" to cleanTitle,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("captaincyHistory").document(),
        mapOf(
          "seasonId" to seasonId,
          "taskId" to taskId,
          "captainUid" to targetUid,
          "captainPlayerId" to playerId,
          "captainDisplayName" to displayName,
          "captainCharacterId" to characterId,
          "title" to cleanTitle,
          "scoreAwarded" to scoreAwarded.coerceIn(0L, 1000L),
          "assignedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("adminControls").document(),
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "CAPTAINCY_UPDATE",
          "payloadSummary" to "Captain set to $displayName ($playerId)".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun seedDefaultRulesAndSchedule(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    seedRules: Boolean,
    seedSchedule: Boolean,
  ) {
    try {
      val adminUid = requireUserId()
      val batch = db.batch()
      var writes = 0
      if (seedRules) {
        Phase6Catalog.defaultHouseRules.forEach { r ->
          batch.set(
            seasonDoc(seasonId).collection("houseRules").document(r.id),
            mapOf(
              "seasonId" to seasonId,
              "ruleNumber" to r.ruleNumber,
              "title" to r.title,
              "description" to r.description,
              "severity" to r.severity,
              "active" to true,
              "updatedBy" to adminUid,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
          writes++
        }
      }
      if (seedSchedule) {
        Phase6Catalog.defaultDailySchedule.forEach { s ->
          batch.set(
            seasonDoc(seasonId).collection("dailySchedule").document(s.id),
            mapOf(
              "seasonId" to seasonId,
              "orderIndex" to s.orderIndex,
              "activityKey" to s.activityKey,
              "title" to s.title,
              "timeLabel" to s.timeLabel,
              "location" to s.location,
              "description" to s.description,
              "isCurrent" to (s.orderIndex == 1L),
              "updatedBy" to adminUid,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
          writes++
        }
      }
      if (writes > 0) batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(P6_REPO_TAG, "Seeding rules/schedule skipped: ${e.message}")
    }
  }

  suspend fun upsertHouseRule(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    ruleId: String? = null,
    ruleNumber: Long,
    title: String,
    description: String,
    severity: String,
    active: Boolean,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val cleanTitle = title.trim().take(80)
      val cleanDesc = description.trim().take(360)
      require(cleanTitle.length >= 3 && cleanDesc.length >= 3) { "Rule title and description required." }
      val docId = ruleId?.takeIf { it.isNotBlank() }
        ?: "rule_${UUID.randomUUID().toString().replace("-", "").take(8)}"
      val ref = seasonDoc(seasonId).collection("houseRules").document(docId)
      val existing = ref.get().await()

      if (existing.exists()) {
        ref.update(
          mapOf(
            "ruleNumber" to ruleNumber.coerceIn(1L, 100L),
            "title" to cleanTitle,
            "description" to cleanDesc,
            "severity" to if (severity in listOf("LOW", "MEDIUM", "HIGH", "CRITICAL")) severity else "MEDIUM",
            "active" to active,
            "updatedBy" to adminUid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      } else {
        ref.set(
          mapOf(
            "seasonId" to seasonId,
            "ruleNumber" to ruleNumber.coerceIn(1L, 100L),
            "title" to cleanTitle,
            "description" to cleanDesc,
            "severity" to if (severity in listOf("LOW", "MEDIUM", "HIGH", "CRITICAL")) severity else "MEDIUM",
            "active" to active,
            "updatedBy" to adminUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun issueRuleViolationWarning(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    ruleId: String,
    ruleTitle: String,
    targetUid: String,
    targetPlayerId: String,
    targetDisplayName: String,
    warningLevel: String,
    note: String,
  ): Result<String> {
    return try {
      val adminUid = requireUserId()
      val cleanNote = note.trim().ifBlank { "Follow House Rules immediately." }.take(240)
      val level = if (warningLevel in listOf("NOTICE", "WARNING", "FINAL_WARNING")) warningLevel else "WARNING"
      val violRef = seasonDoc(seasonId).collection("ruleViolations").document()
      val batch = db.batch()
      batch.set(
        violRef,
        mapOf(
          "seasonId" to seasonId,
          "ruleId" to ruleId.ifBlank { "rule_general" },
          "ruleTitle" to ruleTitle.trim().ifBlank { "House Rule" }.take(80),
          "targetUid" to targetUid,
          "targetPlayerId" to targetPlayerId,
          "targetDisplayName" to targetDisplayName,
          "warningLevel" to level,
          "note" to cleanNote,
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("adminControls").document(),
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "RULE_WARNING",
          "payloadSummary" to "$level -> $targetDisplayName: $ruleTitle".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(violRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun upsertScheduleSlot(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    slot: DailyScheduleSlot,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val docId = slot.id.ifBlank { "slot_${slot.orderIndex}" }
      val ref = seasonDoc(seasonId).collection("dailySchedule").document(docId)
      val existing = ref.get().await()
      if (existing.exists()) {
        ref.update(
          mapOf(
            "orderIndex" to slot.orderIndex.coerceIn(1L, 20L),
            "activityKey" to slot.activityKey.ifBlank { "ACTIVITY" }.take(40),
            "title" to slot.title.trim().take(60),
            "timeLabel" to slot.timeLabel.trim().take(30),
            "location" to slot.location.trim().take(60),
            "description" to slot.description.trim().take(240),
            "isCurrent" to slot.isCurrent,
            "updatedBy" to adminUid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      } else {
        ref.set(
          mapOf(
            "seasonId" to seasonId,
            "orderIndex" to slot.orderIndex.coerceIn(1L, 20L),
            "activityKey" to slot.activityKey.ifBlank { "ACTIVITY" }.take(40),
            "title" to slot.title.trim().take(60),
            "timeLabel" to slot.timeLabel.trim().take(30),
            "location" to slot.location.trim().take(60),
            "description" to slot.description.trim().take(240),
            "isCurrent" to slot.isCurrent,
            "updatedBy" to adminUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun publishScheduleAndMarkCurrent(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    slots: List<DailyScheduleSlot>,
    activeSlotId: String,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val batch = db.batch()
      slots.forEach { s ->
        val ref = seasonDoc(seasonId).collection("dailySchedule").document(s.id)
        batch.update(
          ref,
          mapOf(
            "isCurrent" to (s.id == activeSlotId),
            "updatedBy" to adminUid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      val activeSlot = slots.firstOrNull { it.id == activeSlotId }
      batch.set(
        seasonDoc(seasonId).collection("adminControls").document(),
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "SCHEDULE_PUBLISH",
          "payloadSummary" to "Schedule active: ${activeSlot?.title ?: activeSlotId} (${activeSlot?.timeLabel ?: ""})".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
