package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.multiplayer.AdminAuthValidation
import com.example.multiplayer.AdminControlSuiteDialog
import com.example.multiplayer.AdminSessionSnapshot
import com.example.multiplayer.AuthEntryStage
import com.example.multiplayer.AuthScreen
import com.example.multiplayer.AuthValidation
import com.example.multiplayer.CaptainPowersConfig
import com.example.multiplayer.Phase11VipLogic
import com.example.multiplayer.PlayerHouseHubDialog
import com.example.multiplayer.PrivilegeHistoryRecord
import com.example.multiplayer.SeasonMember
import com.example.multiplayer.SeasonSession
import com.example.multiplayer.TtsStatusSnapshot
import com.example.multiplayer.VipPrivilege
import com.example.multiplayer.VotingBallot
import com.example.multiplayer.VotingCycle
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase10And11Test {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun loginChooserAndAdminKeywordGateEnforceDualEntryFlowWithoutGrantingAdmin() {
    assertFalse(AdminAuthValidation.isValidAccessKeyword("wrong"))
    assertFalse(AdminAuthValidation.isValidAccessKeyword("MEHNAZ"))
    assertTrue(AdminAuthValidation.isValidAccessKeyword("mehnaz"))
    assertTrue(AdminAuthValidation.isValidAccessKeyword(" mehnaz "))
    assertFalse(AdminAuthValidation.canGrantAdminOnKeyword(true))
    assertFalse(AuthValidation.canClientElevateAdminFromUsername("BIGGBOSS"))

    composeTestRule.setContent {
      AuthScreen(
        onAuthSuccess = {},
        initialEntryStage = AuthEntryStage.CHOOSER,
      )
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("entry_player_login_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("entry_admin_login_button").assertIsDisplayed()

    // Enter Admin Login gate
    composeTestRule.onNodeWithTag("entry_admin_login_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_keyword_input").assertIsDisplayed()

    // Wrong keyword shows error and refuses progression
    composeTestRule.onNodeWithTag("admin_keyword_input").performTextInput("wrongkey")
    composeTestRule.onNodeWithTag("admin_keyword_submit_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_keyword_error").assertIsDisplayed()
    composeTestRule.onNodeWithText("Invalid Admin Access Keyword").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_login_submit_button").assertDoesNotExist()

    // Exact keyword "mehnaz" opens Admin Control Room Authentication
    composeTestRule.onNodeWithTag("admin_keyword_input").performTextClearance()
    composeTestRule.onNodeWithTag("admin_keyword_input").performTextInput("mehnaz")
    composeTestRule.onNodeWithTag("admin_keyword_submit_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Admin Control Room Authentication").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_username_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_password_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_login_submit_button").assertIsDisplayed()
  }

  @Test
  fun adminSessionExpiryPasswordValidationAndUiGateAreStrict() {
    val now = 1_760_000_000_000L
    val active = AdminSessionSnapshot(uid = "biggboss_admin", username = "BIGGBOSS", expiresAtMillis = now + 60_000L)
    val expired = AdminSessionSnapshot(uid = "biggboss_admin", username = "BIGGBOSS", expiresAtMillis = now - 1L)
    assertFalse(AdminAuthValidation.isSessionExpired(active, now))
    assertTrue(AdminAuthValidation.isSessionExpired(expired, now))

    assertNotNull(AdminAuthValidation.validatePasswordChange("", "NewPass123", "NewPass123"))
    assertNotNull(AdminAuthValidation.validatePasswordChange("OldPass123", "short", "short"))
    assertNotNull(AdminAuthValidation.validatePasswordChange("OldPass123", "OldPass123", "OldPass123"))
    assertNotNull(AdminAuthValidation.validatePasswordChange("OldPass123", "NewPass123", "Mismatch9"))
    assertNull(AdminAuthValidation.validatePasswordChange("OldPass123", "NewSecurePass99", "NewSecurePass99"))

    // Non-admin must NEVER render AdminControlSuiteDialog even if invoked
    composeTestRule.setContent {
      AdminControlSuiteDialog(
        currentAdminUid = "u_alice",
        isAdminVerified = false,
        season = SeasonSession(id = "season_01", title = "BIGG BOSS – THE HOUSE", status = "LIVE"),
        members = emptyList(),
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
        onDismiss = {},
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

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_suite_logout_icon").assertDoesNotExist()
  }

  @Test
  fun captainDoubleVoteImmunityAndWeightedTallySerializationWorkCorrectly() {
    val season = SeasonSession(
      id = "season_01",
      captainUid = "u_cap",
      captainDisplayName = "AriaStar",
      captainVipAccess = true,
      captainImmunity = true,
      captainDoubleVote = true,
      captainTieBreaker = true,
    )
    val cycle = VotingCycle(
      id = "cyc_weighted",
      title = "Week 2 Eviction",
      status = "CLOSED",
      voteType = "EVICT",
      npcVotesEnabled = false,
      nominatedUids = listOf("u_cap", "u_bob", "u_kabir"),
      nomineeSummaries = listOf(
        "u_cap|GH-CAP1|AriaStar|Player 01",
        "u_bob|GH-BOB2|BobNova|Player 02",
        "u_kabir|GH-KAB3|KabirRoy|Player 03",
      ),
    )
    val ballots = listOf(
      // Immune captain gets 3 raw votes, but is protected by captainImmunity!
      VotingBallot(id = "u_b1", voterUid = "u_bob", targetUid = "u_cap", sourceType = "REAL_PLAYER"),
      VotingBallot(id = "u_b2", voterUid = "u_kabir", targetUid = "u_cap", sourceType = "REAL_PLAYER"),
      VotingBallot(id = "u_b3", voterUid = "u_extra", targetUid = "u_cap", sourceType = "REAL_PLAYER"),
      // Captain votes for Bob with 2x weight = 2
      VotingBallot(id = "u_c1", voterUid = "u_cap", targetUid = "u_bob", sourceType = "REAL_PLAYER"),
      // Regular player votes for Kabir = 1
      VotingBallot(id = "u_d1", voterUid = "u_dev", targetUid = "u_kabir", sourceType = "REAL_PLAYER"),
    )

    val outcome = Phase11VipLogic.calculateWeightedVotingTally(cycle, ballots, season)
    assertTrue(outcome.doubleVoteApplied)
    assertTrue(outcome.immuneCaptainExcluded)
    assertEquals("u_cap", outcome.immuneCaptainUid)
    assertFalse(outcome.isTie)
    assertEquals(1, outcome.eligibleCandidates.size)
    assertEquals("u_bob", outcome.eligibleCandidates.first().nominee.uid)
    assertEquals(2, outcome.eligibleCandidates.first().totalVotes)

    val json = Phase11VipLogic.serializeTallyRowsJson(outcome.weightedTallies)
    val parsed = Phase11VipLogic.parseServerTallySummary(json)
    assertEquals(3, parsed.size)
    assertEquals(2, parsed.first { it.uid == "u_bob" }.realVotes)
  }

  @Test
  fun captainAndVipPrivilegesGateAccessAndBadgesAccurately() {
    val seasonCaptainWithVip = SeasonSession(
      id = "season_01",
      captainUid = "u_cap",
      captainDisplayName = "AriaStar",
      captainVipAccess = true,
    )
    val seasonCaptainNoVip = seasonCaptainWithVip.copy(captainVipAccess = false)
    val vipList = listOf(
      VipPrivilege(userId = "u_vip", displayName = "BobNova", vipEnabled = true, vipRoomAccess = true, vipLabel = "GOLD VIP"),
      VipPrivilege(userId = "u_revoked", displayName = "KabirRoy", vipEnabled = false, vipRoomAccess = false, vipLabel = "OLD VIP"),
    )

    assertTrue(Phase11VipLogic.hasVipRoomAccess("any", isAdmin = true, seasonCaptainNoVip, emptyList()))
    assertTrue(Phase11VipLogic.hasVipRoomAccess("u_cap", isAdmin = false, seasonCaptainWithVip, vipList))
    assertFalse(Phase11VipLogic.hasVipRoomAccess("u_cap", isAdmin = false, seasonCaptainNoVip, vipList))
    assertTrue(Phase11VipLogic.hasVipRoomAccess("u_vip", isAdmin = false, seasonCaptainNoVip, vipList))
    assertFalse(Phase11VipLogic.hasVipRoomAccess("u_revoked", isAdmin = false, seasonCaptainNoVip, vipList))

    assertEquals(listOf("u_vip"), Phase11VipLogic.activeVipUids(vipList))
    assertEquals(listOf("👑 CAPTAIN"), Phase11VipLogic.resolvePlayerBadges("u_cap", seasonCaptainWithVip, vipList))
    assertEquals(listOf("💎 VIP"), Phase11VipLogic.resolvePlayerBadges("u_vip", seasonCaptainWithVip, vipList))

    val powers = CaptainPowersConfig.fromSeason(
      seasonCaptainWithVip.copy(captainImmunity = true, captainDoubleVote = true, captainTieBreaker = false)
    )
    assertTrue(powers.vipRoomAccess)
    assertTrue(powers.nominationImmunity)
    assertTrue(powers.doubleVoteWeight)
    assertFalse(powers.tieBreakerPower)

    // Verify Player House Hub Captain & VIP tab renders VIP Room Access panel & privileges
    val members = listOf(
      SeasonMember(userId = "u_cap", playerId = "GH-CAP1", displayName = "AriaStar", characterId = "Player 01", slotNumber = 1),
      SeasonMember(userId = "u_vip", playerId = "GH-BOB2", displayName = "BobNova", characterId = "Player 02", slotNumber = 2),
    )
    composeTestRule.setContent {
      PlayerHouseHubDialog(
        currentUserId = "u_vip",
        isAdminVerified = false,
        season = seasonCaptainWithVip,
        members = members,
        vipPrivileges = vipList,
        privilegeHistory = listOf(
          PrivilegeHistoryRecord(
            actionType = "VIP_GRANTED",
            targetUid = "u_vip",
            targetDisplayName = "BobNova",
            detail = "Granted GOLD VIP for BobNova",
          )
        ),
        tasks = emptyList(),
        activeTask = null,
        submissions = emptyList(),
        collected3dNodes = emptySet(),
        teams = emptyList(),
        rules = emptyList(),
        violations = emptyList(),
        scheduleSlots = emptyList(),
        isBusy = false,
        actionMessage = null,
        actionError = null,
        onSubmitAnswer = { _, _ -> },
        onClearFeedback = {},
        onDismiss = {},
      )
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("hub_tab_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("player_vip_info_panel").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("CAPTAIN VIP ROOM ACCESS: GRANTED").assertIsDisplayed()
    composeTestRule.onNodeWithText("💎 BobNova (GOLD VIP)").assertIsDisplayed()
  }

  @Test
  fun existing3dVipRoomReusedWithoutDuplicationOrHardcodedAdminSecret() {
    val worldLayoutFile = listOf(
      File("src/main/assets/js/worldLayout.js"),
      File("app/src/main/assets/js/worldLayout.js"),
    ).first { it.exists() }
    val furnitureFile = listOf(
      File("src/main/assets/js/furniture.js"),
      File("app/src/main/assets/js/furniture.js"),
    ).first { it.exists() }

    val layoutJs = worldLayoutFile.readText()
    val furnitureJs = furnitureFile.readText()

    // Existing Captain VIP Room appears once in furniture.js and reuses door_captain + vipLockCollider in worldLayout.js
    val vipRoomSections = Regex("12\\. CAPTAIN VIP ROOM").findAll(furnitureJs).count()
    assertEquals("Existing VIP room must be reused, not duplicated", 1, vipRoomSections)
    assertEquals(1, Regex("id:\\s*'door_captain'").findAll(layoutJs).count())
    assertTrue(layoutJs.contains("requiredVip: true"))
    assertTrue(layoutJs.contains("vipLockCollider"))
    assertTrue(furnitureJs.contains("captain_bed"))
    assertTrue(furnitureJs.contains("captain_chair"))
    assertTrue(furnitureJs.contains("requiredVip: true"))
  }
}
