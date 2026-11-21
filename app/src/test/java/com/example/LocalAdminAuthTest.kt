package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.multiplayer.AdminControlSuiteDialog
import com.example.multiplayer.AdminSessionSnapshot
import com.example.multiplayer.AdminSessionState
import com.example.multiplayer.AuthEntryStage
import com.example.multiplayer.AuthScreen
import com.example.multiplayer.LocalAdminAuthManager
import com.example.multiplayer.SeasonMember
import com.example.multiplayer.SeasonSession
import com.example.multiplayer.TtsStatusSnapshot
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalAdminAuthTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var context: Context

  private class MutableTestClock(
    var wallMs: Long = 1_760_000_000_000L,
    var elapsedMs: Long = 100_000L,
  ) : LocalAdminAuthManager.AdminClock {
    override fun currentWallTimeMs(): Long = wallMs
    override fun elapsedRealtimeMs(): Long = elapsedMs
  }

  private lateinit var fakeClock: MutableTestClock

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    fakeClock = MutableTestClock()
    LocalAdminAuthManager.clock = fakeClock
    LocalAdminAuthManager.resetAllForTests(context)
  }

  @After
  fun tearDown() {
    LocalAdminAuthManager.resetAllForTests(context)
    LocalAdminAuthManager.clock = LocalAdminAuthManager.SystemAdminClock
  }

  @Test
  fun test1And2_keywordValidationAndChooserNavigation() {
    composeTestRule.setContent {
      AuthScreen(
        onAuthSuccess = {},
        initialEntryStage = AuthEntryStage.CHOOSER,
      )
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("entry_player_login_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("entry_admin_login_button").assertIsDisplayed()

    composeTestRule.onNodeWithTag("entry_admin_login_button").performClick()
    composeTestRule.waitForIdle()

    // Incorrect keyword -> error, never grants Admin
    composeTestRule.onNodeWithTag("admin_keyword_input").performTextInput("MEHNAZ")
    composeTestRule.onNodeWithTag("admin_keyword_submit_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Invalid Admin Access Keyword").assertIsDisplayed()
    assertNull(AdminSessionState.session.value)

    // Correct keyword "mehnaz" -> opens Admin Authentication screen
    composeTestRule.onNodeWithTag("admin_keyword_input").performTextClearance()
    composeTestRule.onNodeWithTag("admin_keyword_input").performTextInput("mehnaz")
    composeTestRule.onNodeWithTag("admin_keyword_submit_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_username_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_password_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_login_submit_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_local_security_notice").assertIsDisplayed()
  }

  @Test
  fun test3_validAdminLoginOfflineAndLocalPasswordChange() {
    var authSuccessCount = 0
    composeTestRule.setContent {
      AuthScreen(
        onAuthSuccess = { authSuccessCount++ },
        initialEntryStage = AuthEntryStage.ADMIN_AUTH,
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("admin_username_input").performTextInput("BIGGBOSS")
    composeTestRule.onNodeWithTag("admin_password_input").performTextInput("mehnazsajid12")
    composeTestRule.onNodeWithTag("admin_login_submit_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, authSuccessCount)
    val session = AdminSessionState.session.value
    assertNotNull(session)
    assertEquals("BIGGBOSS", session!!.username)
    assertTrue(session.isLocalOnly)
    assertTrue(session.mustChangeInitialPassword)
    assertTrue(AdminSessionState.pendingAutoOpenControlPanel.value)

    // Local password change works offline and invalidates the initial password
    val changeRes = LocalAdminAuthManager.changeAdminPassword(
      context = context,
      currentPassword = "mehnazsajid12",
      newPassword = "NewBossPassword99",
      confirmPassword = "NewBossPassword99",
    )
    assertTrue(changeRes.isSuccess)
    assertFalse(changeRes.getOrThrow().mustChangeInitialPassword)

    // Old password now fails; new password succeeds
    LocalAdminAuthManager.logoutLocalAdmin()
    val oldOutcome = LocalAdminAuthManager.verifyAdminLogin(context, "BIGGBOSS", "mehnazsajid12")
    assertTrue(oldOutcome is LocalAdminAuthManager.LoginOutcome.Failed)

    val newOutcome = LocalAdminAuthManager.verifyAdminLogin(context, "BIGGBOSS", "NewBossPassword99")
    assertTrue(newOutcome is LocalAdminAuthManager.LoginOutcome.Success)
    assertEquals(0, LocalAdminAuthManager.getLockoutStatus(context).failedAttempts)
  }

  @Test
  fun test4_wrongCredentialsAndAttemptsBeforeLockoutTrackAccurately() {
    val wrongUser = LocalAdminAuthManager.verifyAdminLogin(context, "PLAYER01", "mehnazsajid12")
    assertTrue(wrongUser is LocalAdminAuthManager.LoginOutcome.Failed)
    assertEquals(1, (wrongUser as LocalAdminAuthManager.LoginOutcome.Failed).status.failedAttempts)
    assertFalse(wrongUser.status.isLocked)

    val wrongPass = LocalAdminAuthManager.verifyAdminLogin(context, "BIGGBOSS", "wrongpass")
    assertTrue(wrongPass is LocalAdminAuthManager.LoginOutcome.Failed)
    assertEquals(2, (wrongPass as LocalAdminAuthManager.LoginOutcome.Failed).status.failedAttempts)
    assertFalse(wrongPass.status.isLocked)

    // Succeeding on attempt 3 before lock resets failed attempts to 0
    val success = LocalAdminAuthManager.verifyAdminLogin(context, "BIGGBOSS", "mehnazsajid12")
    assertTrue(success is LocalAdminAuthManager.LoginOutcome.Success)
    assertEquals(0, LocalAdminAuthManager.getLockoutStatus(context).failedAttempts)
  }

  @Test
  fun test5_6_7_thirdFailureTriggers48HourLockoutPersistsAndResistsClockTampering() {
    val authCalls = AtomicInteger(0)
    composeTestRule.setContent {
      AuthScreen(
        onAuthSuccess = { authCalls.incrementAndGet() },
        initialEntryStage = AuthEntryStage.ADMIN_AUTH,
      )
    }
    composeTestRule.waitForIdle()

    // 1st failed attempt via UI
    composeTestRule.onNodeWithTag("admin_username_input").performScrollTo().performTextInput("BIGGBOSS")
    composeTestRule.onNodeWithTag("admin_password_input").performScrollTo().performTextInput("bad1")
    composeTestRule.onNodeWithTag("admin_login_submit_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_attempt_warning").performScrollTo().assertIsDisplayed()

    // 2nd failed attempt via UI
    composeTestRule.onNodeWithTag("admin_password_input").performScrollTo().performTextClearance()
    composeTestRule.onNodeWithTag("admin_password_input").performTextInput("bad2")
    composeTestRule.onNodeWithTag("admin_login_submit_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_attempt_warning").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Failed attempts: 2/2", substring = true).performScrollTo().assertIsDisplayed()

    // 3rd consecutive failure -> triggers 48h lockout immediately
    composeTestRule.onNodeWithTag("admin_password_input").performScrollTo().performTextClearance()
    composeTestRule.onNodeWithTag("admin_password_input").performTextInput("bad3")
    composeTestRule.onNodeWithTag("admin_login_submit_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertEquals(0, authCalls.get())
    composeTestRule.onNodeWithTag("admin_lockout_banner").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText(LocalAdminAuthManager.LOCKOUT_HEADLINE).performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_lockout_remaining").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_login_submit_button").performScrollTo().assertIsNotEnabled()

    // Even valid credentials cannot sign in while locked
    val blockedValid = LocalAdminAuthManager.verifyAdminLogin(context, "BIGGBOSS", "mehnazsajid12")
    assertTrue(blockedValid is LocalAdminAuthManager.LoginOutcome.Locked)

    // Clock tampering attempt 1: user sets device date +100 hours forward, same monotonic boot elapsed time (+10 seconds)
    fakeClock.wallMs += 100L * 60L * 60L * 1000L
    fakeClock.elapsedMs += 10_000L
    val afterForwardTamper = LocalAdminAuthManager.getLockoutStatus(context)
    assertTrue(afterForwardTamper.isLocked)
    assertTrue(afterForwardTamper.clockTamperingDetected)
    assertEquals(LocalAdminAuthManager.LOCKOUT_DURATION_MS - 10_000L, afterForwardTamper.remainingMillis)

    // Clock tampering attempt 2: user rolls device date backward 10 hours
    fakeClock.wallMs -= 10L * 60L * 60L * 1000L
    fakeClock.elapsedMs += 5_000L
    val afterBackwardTamper = LocalAdminAuthManager.getLockoutStatus(context)
    assertTrue(afterBackwardTamper.isLocked)
    assertEquals(LocalAdminAuthManager.LOCKOUT_DURATION_MS - 15_000L, afterBackwardTamper.remainingMillis)
  }

  @Test
  fun test8_lockoutAutoUnlocksAfter48HoursRealElapsedTime() {
    // Trigger 48h lockout
    repeat(3) {
      LocalAdminAuthManager.verifyAdminLogin(context, "BIGGBOSS", "wrong_$it")
    }
    val locked = LocalAdminAuthManager.getLockoutStatus(context)
    assertTrue(locked.isLocked)
    assertEquals(LocalAdminAuthManager.LOCKOUT_DURATION_MS, locked.remainingMillis)

    // Advance 47h 59m 59s -> still locked with 1s remaining
    val fortyEightHours = LocalAdminAuthManager.LOCKOUT_DURATION_MS
    fakeClock.wallMs += fortyEightHours - 1_000L
    fakeClock.elapsedMs += fortyEightHours - 1_000L
    val stillLocked = LocalAdminAuthManager.getLockoutStatus(context)
    assertTrue(stillLocked.isLocked)
    assertEquals(1_000L, stillLocked.remainingMillis)

    // Advance final 1 second -> automatically unlocked & attempts reset
    fakeClock.wallMs += 1_000L
    fakeClock.elapsedMs += 1_000L
    val unlocked = LocalAdminAuthManager.getLockoutStatus(context)
    assertFalse(unlocked.isLocked)
    assertEquals(0, unlocked.failedAttempts)
    assertEquals(0L, unlocked.remainingMillis)

    // Valid Admin credentials work again after unlock
    val success = LocalAdminAuthManager.verifyAdminLogin(context, "BIGGBOSS", "mehnazsajid12")
    assertTrue(success is LocalAdminAuthManager.LoginOutcome.Success)
  }

  @Test
  fun test9_playerLoginRemainsCompletelyAccessibleDuringAdmin48HourLockout() {
    repeat(3) {
      LocalAdminAuthManager.verifyAdminLogin(context, "BIGGBOSS", "bad_$it")
    }
    assertTrue(LocalAdminAuthManager.getLockoutStatus(context).isLocked)

    composeTestRule.setContent {
      AuthScreen(
        onAuthSuccess = {},
        initialEntryStage = AuthEntryStage.CHOOSER,
      )
    }
    composeTestRule.waitForIdle()

    // Player Login is still enabled & enters normal Sign-In / Create Account UI cleanly
    composeTestRule.onNodeWithTag("entry_player_login_button").assertIsEnabled().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("auth_tab_login").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_tab_register").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("login_email_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("login_password_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("login_submit_button").performScrollTo().assertIsDisplayed().assertIsEnabled()
    composeTestRule.onNodeWithTag("google_signin_button").performScrollTo().assertIsDisplayed().assertIsEnabled()
  }

  @Test
  fun test10_controlSuiteOpensForAdminContainsControlRoomAccessAndAssetsPreserved() {
    var enteredControlRoom = false
    composeTestRule.setContent {
      AdminControlSuiteDialog(
        currentAdminUid = LocalAdminAuthManager.ADMIN_UID,
        isAdminVerified = true,
        season = SeasonSession(id = "season_01", title = "BIGG BOSS – THE HOUSE", status = "LIVE"),
        members = listOf(
          SeasonMember(userId = "u_1", playerId = "GH-1001", displayName = "Aria", characterId = "Player 01", slotNumber = 1, online = true)
        ),
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
        adminSession = AdminSessionSnapshot(
          uid = LocalAdminAuthManager.ADMIN_UID,
          username = LocalAdminAuthManager.ADMIN_USERNAME,
          isLocalOnly = true,
        ),
        isBusy = false,
        actionMessage = null,
        actionError = null,
        onDismiss = {},
        onClearFeedback = {},
        onOpenCctvWall = {},
        onEnterControlRoom = { enteredControlRoom = true },
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
    composeTestRule.onNodeWithTag("enter_control_room_button").performScrollTo().assertIsDisplayed().performClick()
    assertTrue(enteredControlRoom)
    composeTestRule.onNodeWithText("Voting & Nominations").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Announcements").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("AI Operator (Chair 1)").performScrollTo().assertIsDisplayed()

    // Verify 3D Control Room assets: 2 operator chairs + hidden from players + zero mic permissions
    val furnitureJs = listOf(
      File("src/main/assets/js/furniture.js"),
      File("app/src/main/assets/js/furniture.js"),
    ).first { it.exists() }.readText()
    assertTrue(furnitureJs.contains("admin_chair_1_mesh"))
    assertTrue(furnitureJs.contains("ChatGPT AI Operator Chair"))
    assertTrue(furnitureJs.contains("admin_chair_2"))
    assertTrue(furnitureJs.contains("Main Admin (Owner) Chair"))
    assertTrue(furnitureJs.contains("adminGroup.visible = false"))

    val manifestXml = listOf(
      File("src/main/AndroidManifest.xml"),
      File("app/src/main/AndroidManifest.xml"),
    ).first { it.exists() }.readText()
    assertFalse(manifestXml.contains("RECORD_AUDIO"))
    assertFalse(manifestXml.contains("CALL_PHONE"))
  }
}
