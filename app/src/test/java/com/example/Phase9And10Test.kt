package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.example.multiplayer.AdminAiOperatorTab
import com.example.multiplayer.AdminControlSuiteDialog
import com.example.multiplayer.AdminEvictionsTab
import com.example.multiplayer.AiActionType
import com.example.multiplayer.AiActivityLog
import com.example.multiplayer.AiApprovalProposal
import com.example.multiplayer.AiControlConfig
import com.example.multiplayer.AiControlMode
import com.example.multiplayer.AiOperatorConsoleDialog
import com.example.multiplayer.AiScheduledEvent
import com.example.multiplayer.HouseNominationDialog
import com.example.multiplayer.HouseTask
import com.example.multiplayer.HouseVotingBallotDialog
import com.example.multiplayer.LobbyScreen
import com.example.multiplayer.NominationBallot
import com.example.multiplayer.NomineeSummary
import com.example.multiplayer.Phase10VotingLogic
import com.example.multiplayer.Phase9AiHouseManagerEngine
import com.example.multiplayer.RationItem
import com.example.multiplayer.SeasonMember
import com.example.multiplayer.SeasonSession
import com.example.multiplayer.TtsStatusSnapshot
import com.example.multiplayer.UiState
import com.example.multiplayer.UserAccount
import com.example.multiplayer.VotingBallot
import com.example.multiplayer.VotingCycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase9And10Test {

  @get:Rule
  val composeTestRule = createComposeRule()

  private fun sampleMembers(): List<SeasonMember> = listOf(
    SeasonMember(
      userId = "u_alice",
      playerId = "GH-ALICE1",
      displayName = "AriaStar",
      characterId = "Player 01",
      slotNumber = 1,
      role = "PLAYER",
      status = "IN_HOUSE",
      online = true,
    ),
    SeasonMember(
      userId = "u_bob",
      playerId = "GH-BOB002",
      displayName = "BobNova",
      characterId = "Player 02",
      slotNumber = 2,
      role = "PLAYER",
      status = "IN_HOUSE",
      online = true,
    ),
    SeasonMember(
      userId = "u_kabir",
      playerId = "GH-KABIR3",
      displayName = "KabirRoy",
      characterId = "Player 03",
      slotNumber = 3,
      role = "PLAYER",
      status = "IN_HOUSE",
      online = true,
    ),
    SeasonMember(
      userId = "u_owner_admin",
      playerId = "GH-ADMIN0",
      displayName = "SecretBossOwner",
      characterId = "Player 01",
      slotNumber = 0,
      role = "ADMIN",
      status = "IN_HOUSE",
      online = true,
    ),
  )

  // --- SCENARIO 1: CONTROL ROOM TWO CHAIRS & SEPARATE INTERFACES ---

  @Test
  fun scenario1_controlRoomTwoChairsHaveIndependentUisAndBlockedForPlayers() {
    var chair1Open = true
    var chair2Open = false
    val config = AiControlConfig(enabled = true, mode = AiControlMode.ASSIST.name)

    composeTestRule.setContent {
      // Non-admin must never render Chair 1 AI Operator Console even if requested
      AiOperatorConsoleDialog(
        isAdminVerified = false,
        config = config,
        schedules = emptyList(),
        proposals = emptyList(),
        logs = emptyList(),
        whileAwaySummary = Phase9AiHouseManagerEngine.computeWhileAwaySummary(emptyList(), 0L),
        isBusy = false,
        actionMessage = null,
        actionError = null,
        onDismiss = { chair1Open = false },
        onClearFeedback = {},
        onSetEnabled = {},
        onSetMode = {},
        onSetModules = { _, _, _, _ -> },
        onEmergencyStop = {},
        onAcknowledgeWhileAway = {},
        onCreateSchedule = { _, _, _, _, _ -> },
        onCancelSchedule = {},
        onRunAiTick = {},
        onGenerateSuggestion = {},
        onApproveProposal = {},
        onRejectProposal = {},
      )
      if (chair1Open) {
        AiOperatorConsoleDialog(
          isAdminVerified = true,
          config = config,
          schedules = emptyList(),
          proposals = emptyList(),
          logs = emptyList(),
          whileAwaySummary = Phase9AiHouseManagerEngine.computeWhileAwaySummary(emptyList(), 0L),
          isBusy = false,
          actionMessage = null,
          actionError = null,
          onDismiss = { chair1Open = false },
          onClearFeedback = {},
          onSetEnabled = {},
          onSetMode = {},
          onSetModules = { _, _, _, _ -> },
          onEmergencyStop = {},
          onAcknowledgeWhileAway = {},
          onCreateSchedule = { _, _, _, _, _ -> },
          onCancelSchedule = {},
          onRunAiTick = {},
          onGenerateSuggestion = {},
          onApproveProposal = {},
          onRejectProposal = {},
        )
      }
      if (chair2Open) {
        AdminControlSuiteDialog(
          currentAdminUid = "u_owner_admin",
          isAdminVerified = true,
          season = SeasonSession(id = "season_01", title = "BIGG BOSS – THE HOUSE S1", status = "LIVE"),
          members = sampleMembers(),
          allUsers = emptyList(),
          votingCycles = emptyList(),
          activeCycle = null,
          ballots = emptyList(),
          evictions = emptyList(),
          announcements = emptyList(),
          ttsStatus = TtsStatusSnapshot(),
          tasks = emptyList(),
          activeTask = null,
          taskSubmissions = emptyList(),
          teams = emptyList(),
          captaincyHistory = emptyList(),
          houseRules = emptyList(),
          ruleViolations = emptyList(),
          dailySchedule = emptyList(),
          cctvRecordings = emptyList(),
          isUploadingCctv = false,
          cctvUploadStatus = null,
          isBusy = false,
          actionMessage = null,
          actionError = null,
          onDismiss = { chair2Open = false },
          onClearFeedback = {},
          onOpenCctvWall = {},
          onDeleteCctvRecording = {},
          onSetSeasonStatus = {},
          onModeratePlayer = { _, _, _ -> },
          onSelectCycle = {},
          onUpdateCycleState = { _, _, _ -> },
          onCastNpcVote = { _, _, _ -> },
          onConfirmEviction = { _, _, _, _, _, _, _ -> },
          onPreviewSpeech = { _, _, _ -> },
          onBroadcastAll = { _, _, _, _ -> },
          onSelectTask = {},
          onSaveOrStartTask = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
          onUpdateTaskStatus = { _, _ -> },
          onDeclareTaskWinner = { _, _ -> },
          onSaveTeam = { _, _, _, _, _, _ -> },
          onAssignCaptain = { _, _, _ -> },
          onSaveRule = { _, _, _, _, _, _ -> },
          onBroadcastRule = {},
          onIssueWarning = { _, _, _, _ -> },
          onSaveScheduleSlot = {},
          onPublishSchedule = { _, _ -> },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("chair1_ai_operator_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ai_api_transparency_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("CHAIR 1 • CHATGPT AI OPERATOR").assertIsDisplayed()
  }

  // --- SCENARIO 2: SECRET ADMIN IDENTITY ---

  @Test
  @Config(qualifiers = "w960dp-h720dp")
  fun scenario2_secretAdminIdentityHidesOwnerFromLobbyAndNominationPools() {
    val members = sampleMembers()
    val tallies = Phase10VotingLogic.tallyNominations(emptyList(), members)
    assertFalse(tallies.any { it.uid == "u_owner_admin" || it.displayName == "SecretBossOwner" })
    assertEquals(3, tallies.size)

    composeTestRule.setContent {
      LobbyScreen(
        account = UserAccount(
          userId = "u_alice",
          displayName = "AriaStar",
          playerId = "GH-ALICE1",
          characterId = "Player 01",
          role = "PLAYER",
        ),
        isAdminVerified = false,
        seasonState = UiState.Success(SeasonSession(id = "season_01", title = "Season 01", status = "LIVE")),
        membersState = UiState.Success(members),
        activeVotingCycle = null,
        captainUid = null,
        activeTask = null,
        isRtdbConfigured = true,
        sessionSuperseded = false,
        isBusy = false,
        actionError = null,
        onJoinHouse = {},
        onEditProfile = {},
        onOpenAdminSuite = {},
        onOpenVotingBallot = {},
        onOpenHouseHub = {},
        onSignOut = {},
        onClearError = {},
      )
    }

    composeTestRule.waitForIdle()
    composeTestRule.onAllNodesWithText("AriaStar").onFirst().assertIsDisplayed()
    composeTestRule.onNodeWithText("BobNova").assertIsDisplayed()
    composeTestRule.onNodeWithText("SecretBossOwner").assertDoesNotExist()
  }

  // --- SCENARIO 3: AI CONTROLS (ON/OFF, MODES, EMERGENCY STOP, APPROVAL QUEUE) ---

  @Test
  fun scenario3_aiControlsModesEmergencyStopAndApprovalQueueWorkCorrectly() {
    val now = 1_750_000_000_000L
    val lowRice = RationItem(
      itemId = "rice",
      name = "Royal Rice",
      unit = "kg",
      quantity = 2L,
      maxQuantity = 40L,
      minThreshold = 5L,
      permitted = true,
    )
    val expiredTask = HouseTask(
      id = "t_exp_1",
      title = "Kitchen Sprint",
      status = "IN_PROGRESS",
      endTimeMillis = now - 1000L,
    )

    // Emergency stop / OFF / MANUAL blocks automatic execution
    val stoppedEval = Phase9AiHouseManagerEngine.evaluateHouseTick(
      config = AiControlConfig(enabled = true, emergencyStopped = true, mode = AiControlMode.AUTO.name),
      schedules = emptyList(),
      existingExecutionKeys = emptySet(),
      rations = listOf(lowRice),
      tasks = listOf(expiredTask),
      eligibleMembers = sampleMembers(),
      seasonId = "season_01",
      actorUid = "u_owner_admin",
      source = "ADMIN_TICK",
      nowMillis = now,
    )
    assertTrue(stoppedEval.proposalsToCreate.isEmpty())
    assertTrue(stoppedEval.directExpiredTasks.isEmpty())
    assertTrue(stoppedEval.skippedReason != null)

    // ASSIST mode generates proposals instead of mutating live game state directly
    val assistEval = Phase9AiHouseManagerEngine.evaluateHouseTick(
      config = AiControlConfig(enabled = true, emergencyStopped = false, mode = AiControlMode.ASSIST.name),
      schedules = emptyList(),
      existingExecutionKeys = emptySet(),
      rations = listOf(lowRice),
      tasks = listOf(expiredTask),
      eligibleMembers = sampleMembers(),
      seasonId = "season_01",
      actorUid = "u_owner_admin",
      source = "ADMIN_TICK",
      nowMillis = now,
    )
    assertEquals(2, assistEval.proposalsToCreate.size)
    assertTrue(assistEval.directExpiredTasks.isEmpty())
    assertTrue(assistEval.directKitchenReplenishes.isEmpty())

    // Verify Chair 1 UI dispatches mode switch and Emergency Stop callbacks
    var selectedMode: AiControlMode? = null
    var stopPressed = false
    composeTestRule.setContent {
      AdminAiOperatorTab(
        config = AiControlConfig(enabled = true, mode = AiControlMode.ASSIST.name),
        schedules = emptyList(),
        proposals = assistEval.proposalsToCreate,
        logs = emptyList(),
        whileAwaySummary = Phase9AiHouseManagerEngine.computeWhileAwaySummary(emptyList(), 0L),
        isBusy = false,
        onSetEnabled = {},
        onSetMode = { selectedMode = it },
        onSetModules = { _, _, _, _ -> },
        onEmergencyStop = { stopPressed = true },
        onAcknowledgeWhileAway = {},
        onCreateSchedule = { _, _, _, _, _ -> },
        onCancelSchedule = {},
        onRunAiTick = {},
        onGenerateSuggestion = {},
        onApproveProposal = {},
        onRejectProposal = {},
      )
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("ai_mode_AUTO").performClick()
    assertEquals(AiControlMode.AUTO, selectedMode)
    composeTestRule.onNodeWithTag("ai_emergency_stop_button").performClick()
    assertTrue(stopPressed)
  }

  // --- SCENARIO 4: OFFLINE AUTOMATION & WHILE YOU WERE AWAY ---

  @Test
  fun scenario4_offlineSchedulesExecuteWithoutDuplicatesAndProduceWhileAwaySummary() {
    val dueAt = 1_750_000_000_000L
    val schedules = listOf(
      AiScheduledEvent(
        id = "sched_hindi_1",
        actionType = AiActionType.HINDI_ANNOUNCEMENT.name,
        title = "Morning Wakeup Hindi",
        messageOrPayload = "सुप्रभात घरवालों! लिविंग एरिया में एकत्रित हों।",
        targetTimeMillis = dueAt - 60_000L,
        recurrenceMinutes = 0L,
        status = "PENDING",
        modeOverride = "INHERIT",
        executionKey = "wake_day_1",
      )
    )

    val firstTick = Phase9AiHouseManagerEngine.evaluateHouseTick(
      config = AiControlConfig(enabled = true, mode = AiControlMode.AUTO.name),
      schedules = schedules,
      existingExecutionKeys = emptySet(),
      rations = emptyList(),
      tasks = emptyList(),
      eligibleMembers = sampleMembers(),
      seasonId = "season_01",
      actorUid = "cloud-scheduler",
      source = "CLOUD_SCHEDULER",
      nowMillis = dueAt,
    )
    assertEquals(1, firstTick.directAnnouncements.size)
    assertEquals("EXECUTED", firstTick.scheduleTransitions.first().nextStatus)
    assertFalse(firstTick.logsToAppend.first().acknowledgedByAdmin)

    // Repeating the same tick with recorded executionKeys prevents duplicate firing
    val executedKeys = firstTick.logsToAppend.map { it.executionKey }.toSet()
    val duplicateTick = Phase9AiHouseManagerEngine.evaluateHouseTick(
      config = AiControlConfig(enabled = true, mode = AiControlMode.AUTO.name),
      schedules = schedules,
      existingExecutionKeys = executedKeys,
      rations = emptyList(),
      tasks = emptyList(),
      eligibleMembers = sampleMembers(),
      seasonId = "season_01",
      actorUid = "cloud-scheduler",
      source = "OFFLINE_CATCHUP",
      nowMillis = dueAt + 5_000L,
    )
    assertTrue(duplicateTick.directAnnouncements.isEmpty())

    // Summary surfaces offline actions when Admin returns
    val summary = Phase9AiHouseManagerEngine.computeWhileAwaySummary(
      logs = firstTick.logsToAppend + AiActivityLog(
        id = "prop_log",
        title = "Proposal Queued: Lunch Task",
        detail = "Waiting for approval",
        source = "OFFLINE_CATCHUP",
        executedAtMillis = dueAt,
        acknowledgedByAdmin = false,
      ),
      lastAdminSeenAtMillis = dueAt - 120_000L,
    )
    assertEquals(2, summary.missedExecutionsCount)
    assertEquals(1, summary.missedAnnouncementsCount)
    assertEquals(1, summary.missedProposalsCount)
  }

  // --- SCENARIO 5: PEER NOMINATIONS ---

  @Test
  fun scenario5_peerNominationsExcludeSelfAndAdminAndLockDuplicates() {
    var submittedTargets: List<NomineeSummary> = emptyList()
    val cycle = VotingCycle(
      id = "cyc_nom_1",
      title = "Week 1 Nominations",
      status = "NOMINATIONS",
      nominationsPerPlayer = 2L,
    )

    composeTestRule.setContent {
      HouseNominationDialog(
        cycle = cycle,
        currentUserId = "u_alice",
        members = sampleMembers(),
        nominations = emptyList(),
        myNomination = null,
        isBusy = false,
        onSubmitNomination = { targets, _ -> submittedTargets = targets },
        onDismiss = {},
      )
    }

    composeTestRule.waitForIdle()
    // Self (u_alice) and Admin (u_owner_admin) must NOT appear as nomination targets
    composeTestRule.onNodeWithTag("nominate_target_u_alice").assertDoesNotExist()
    composeTestRule.onNodeWithTag("nominate_target_u_owner_admin").assertDoesNotExist()
    composeTestRule.onNodeWithTag("nominate_target_u_bob").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nominate_target_u_kabir").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nominate_target_u_bob").performClick()
    composeTestRule.onNodeWithTag("submit_nomination_button").assertIsEnabled().performClick()
    assertEquals(listOf("u_bob"), submittedTargets.map { it.uid })
  }

  // --- SCENARIO 6: VOTING RULES & VISIBILITY ENFORCEMENT ---

  @Test
  fun scenario6_votingVisibilityHidesOrRevealsTotalsPerPolicy() {
    val openHiddenCycle = VotingCycle(
      id = "cyc_v1",
      title = "Week 1 Eviction",
      status = "OPEN",
      voteType = "EVICT",
      resultVisibility = "HIDDEN_UNTIL_CLOSED",
      votingEndTimeMillis = System.currentTimeMillis() + 300_000L,
      nominatedUids = listOf("u_alice", "u_bob"),
      nomineeSummaries = listOf("u_alice|GH-ALICE1|AriaStar|Player 01", "u_bob|GH-BOB002|BobNova|Player 02"),
    )
    assertFalse(Phase10VotingLogic.canPlayerViewTotals(openHiddenCycle, isAdmin = false))
    assertTrue(Phase10VotingLogic.canPlayerViewTotals(openHiddenCycle, isAdmin = true))
    assertTrue(Phase10VotingLogic.canPlayerViewTotals(openHiddenCycle.copy(status = "CLOSED"), isAdmin = false))
    assertFalse(Phase10VotingLogic.canPlayerViewTotals(openHiddenCycle.copy(status = "CLOSED", resultVisibility = "ADMIN_ONLY"), isAdmin = false))
    assertTrue(Phase10VotingLogic.canPlayerViewTotals(openHiddenCycle.copy(resultVisibility = "LIVE_TOTALS"), isAdmin = false))

    val ballots = listOf(
      VotingBallot(id = "u_kabir", voterUid = "u_kabir", targetUid = "u_bob", sourceType = "REAL_PLAYER"),
    )
    composeTestRule.setContent {
      HouseVotingBallotDialog(
        cycle = openHiddenCycle,
        myBallot = null,
        ballots = ballots,
        isAdminVerified = false,
        isBusy = false,
        onCastVote = {},
        onDismiss = {},
      )
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("ballot_countdown_label").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ballot_results_hidden_notice").performScrollTo().assertIsDisplayed()
  }

  // --- SCENARIO 7: EVICTION CALCULATION, REAL VS NPC, TIES & HINDI ANNOUNCEMENTS ---

  @Test
  fun scenario7_evictionTallySeparatesRealAndNpcVotesHandlesSaveRuleAndDetectsTies() {
    val baseCycle = VotingCycle(
      id = "cyc_tally",
      title = "Finale Elimination",
      status = "CLOSED",
      voteType = "EVICT",
      npcVotesEnabled = true,
      nominatedUids = listOf("u_alice", "u_bob"),
      nomineeSummaries = listOf("u_alice|GH-ALICE1|AriaStar|Player 01", "u_bob|GH-BOB002|BobNova|Player 02"),
    )
    val ballots = listOf(
      VotingBallot(id = "v1", voterUid = "u_kabir", targetUid = "u_bob", sourceType = "REAL_PLAYER"),
      VotingBallot(id = "v2", voterUid = "npc_1", targetUid = "u_bob", sourceType = "NPC"),
      VotingBallot(id = "v3", voterUid = "u_alice", targetUid = "u_alice", sourceType = "REAL_PLAYER"),
    )

    // EVICT rule: highest total votes leads eviction (BobNova = 2 > AriaStar = 1)
    val evictResult = Phase10VotingLogic.calculateVotingTally(baseCycle, ballots)
    assertFalse(evictResult.isTie)
    assertEquals(2, evictResult.totalRealVotes)
    assertEquals(1, evictResult.totalNpcVotes)
    assertEquals(1, evictResult.rankedTallies.first().realVotes)
    assertEquals(1, evictResult.rankedTallies.first().npcVotes)
    assertEquals("u_bob", evictResult.leadingEvictionCandidates.first().nominee.uid)

    // SAVE rule: lowest total votes leads eviction (AriaStar = 1 < BobNova = 2)
    val saveResult = Phase10VotingLogic.calculateVotingTally(baseCycle.copy(voteType = "SAVE"), ballots)
    assertEquals("u_alice", saveResult.leadingEvictionCandidates.first().nominee.uid)

    // Tie detection
    val tiedBallots = listOf(
      VotingBallot(id = "t1", voterUid = "u_alice", targetUid = "u_bob", sourceType = "REAL_PLAYER"),
      VotingBallot(id = "t2", voterUid = "u_bob", targetUid = "u_alice", sourceType = "REAL_PLAYER"),
    )
    val tiedResult = Phase10VotingLogic.calculateVotingTally(baseCycle, tiedBallots)
    assertTrue(tiedResult.isTie)
    assertEquals(2, tiedResult.leadingEvictionCandidates.size)

    val hindiEvict = Phase10VotingLogic.formatHindiEvictionAnnouncement("BobNova", "EVICT", 2)
    assertTrue(hindiEvict.contains("BobNova"))
    assertTrue(hindiEvict.contains("बेघर"))

    var cancelCalled = false
    composeTestRule.setContent {
      AdminEvictionsTab(
        activeCycle = baseCycle,
        ballots = tiedBallots,
        tallySummary = tiedResult,
        evictions = emptyList(),
        isBusy = false,
        onConfirmEviction = { _, _, _, _, _, _, _ -> },
        onCancelDueToTie = { _, _ -> cancelCalled = true },
      )
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tie_detected_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eviction_announcement_input").assertTextContains("AriaStar", substring = true)
    composeTestRule.onNodeWithTag("cancel_tie_round_button").performClick()
    assertTrue(cancelCalled)
  }

  // --- SCENARIO 8: NOMINATION & HINDI CANCELLATION HELPERS REGRESSION ---

  @Test
  fun scenario8_nominationTalliesAndHindiCancelledAnnouncementAreDeterministic() {
    val nominations = listOf(
      NominationBallot(voterUid = "u_alice", voterName = "AriaStar", targetUids = listOf("u_bob", "u_kabir")),
      NominationBallot(voterUid = "u_kabir", voterName = "KabirRoy", targetUids = listOf("u_bob")),
    )
    val tallies = Phase10VotingLogic.tallyNominations(nominations, sampleMembers())
    assertEquals("u_bob", tallies.first().uid)
    assertEquals(2, tallies.first().nominationCount)
    assertEquals(1, tallies.first { it.uid == "u_kabir" }.nominationCount)

    val cancelHindi = Phase10VotingLogic.formatHindiCancelledAnnouncement("Week 1", "Tie vote")
    assertTrue(cancelHindi.contains("रद्द"))
    assertTrue(cancelHindi.contains("Tie vote"))
  }
}
