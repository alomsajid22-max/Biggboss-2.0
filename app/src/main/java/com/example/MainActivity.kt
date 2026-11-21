package com.example

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.webkit.WebViewAssetLoader
import com.example.multiplayer.AdminAnnouncement
import com.example.multiplayer.AdminAuthValidation
import com.example.multiplayer.AdminControlSuiteDialog
import com.example.multiplayer.AdminPersonalPhoneDialog
import com.example.multiplayer.AdminSessionState
import com.example.multiplayer.AiControlConfig
import com.example.multiplayer.AiOperatorConsoleDialog
import com.example.multiplayer.AnnouncementPresets
import com.example.multiplayer.AnnouncementTtsManager
import com.example.multiplayer.AuthScreen
import com.example.multiplayer.BiggGramAndNpcVotingEngine
import com.example.multiplayer.CameraPreferences
import com.example.multiplayer.CelebrationType
import com.example.multiplayer.FinaleCeremonyState
import com.example.multiplayer.HouseChatAndCommsDialog
import com.example.multiplayer.HouseClockState
import com.example.multiplayer.HouseDestination
import com.example.multiplayer.HouseNominationDialog
import com.example.multiplayer.HouseTask
import com.example.multiplayer.HouseVotingBallotDialog
import com.example.multiplayer.KitchenAndCookingDialog
import com.example.multiplayer.LiveAnnouncementBanner
import com.example.multiplayer.LobbyScreen
import com.example.multiplayer.LocalAdminAuthManager
import com.example.multiplayer.MultiplayerFirestoreRepository
import com.example.multiplayer.MultiplayerViewModel
import com.example.multiplayer.Phase11CaptainVipRepository
import com.example.multiplayer.Phase11VipLogic
import com.example.multiplayer.Phase6FirestoreRepository
import com.example.multiplayer.Phase7And8FirestoreRepository
import com.example.multiplayer.Phase8Logic
import com.example.multiplayer.Phase9And10FirestoreRepository
import com.example.multiplayer.PlayerExtendedProfileDialog
import com.example.multiplayer.PlayerHouseHubDialog
import com.example.multiplayer.PlayerRole
import com.example.multiplayer.ProfileSetupScreen
import com.example.multiplayer.RealtimeMultiplayerRepository
import com.example.multiplayer.RemoteDoorState
import com.example.multiplayer.RemotePlayerState
import com.example.multiplayer.RemoteSeatState
import com.example.multiplayer.RuleWarningBanner
import com.example.multiplayer.SeasonMember
import com.example.multiplayer.UiState
import com.example.multiplayer.UserAccount
import com.example.multiplayer.VoiceGender
import com.example.multiplayer.WardrobeAppearance
import com.example.multiplayer.WardrobeStudioDialog
import com.example.multiplayer.attemptAutoSignIn
import com.example.multiplayer.authStateFlow
import com.example.multiplayer.isCompletingEmailRegistration
import com.example.multiplayer.signOutSession
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "BiggBossWebView"
private const val ENTRY_URL = "https://appassets.androidplatform.net/assets/index.html"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
    )

    setContent {
      MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        AppNavigation(onExit = { finish() })
      }
    }
  }
}

@Composable
private fun AppNavigation(onExit: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val auth = remember { Firebase.auth }
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val adminSession by AdminSessionState.session.collectAsStateWithLifecycle()
  val activeLocalAdmin = adminSession?.takeIf { it.isLocalOnly && !AdminAuthValidation.isSessionExpired(it) }
  val completingRegistration by isCompletingEmailRegistration.collectAsStateWithLifecycle()
  var checkingStartupAuth by remember { mutableStateOf(auth.currentUser == null && activeLocalAdmin == null) }

  LaunchedEffect(Unit) {
    if (auth.currentUser == null && activeLocalAdmin == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = { checkingStartupAuth = false },
        onUnauthenticated = { checkingStartupAuth = false },
        scope = scope,
      )
    } else {
      checkingStartupAuth = false
    }
  }

  val user = currentUser
  if (activeLocalAdmin != null) {
    key(activeLocalAdmin.uid, "local_admin") {
      AuthenticatedHouseHost(
        currentUserId = activeLocalAdmin.uid,
        isLocalAdminSession = true,
        defaultNameSuggestion = activeLocalAdmin.username,
        onExit = onExit,
      )
    }
  } else if (checkingStartupAuth && user == null) {
    FullScreenLoading("Checking signed-in session...")
  } else if (user == null || completingRegistration) {
    // Unauthenticated gate: house lobby & 3D scene cannot be rendered without completed Firebase Auth or Local Admin Auth.
    AuthScreen(
      onAuthSuccess = { checkingStartupAuth = false },
    )
  } else {
    key(user.uid, "firebase") {
      AuthenticatedHouseHost(
        currentUserId = user.uid,
        isLocalAdminSession = false,
        defaultNameSuggestion = user.displayName.orEmpty().take(24),
        onExit = onExit,
      )
    }
  }
}

@Composable
private fun AuthenticatedHouseHost(
  currentUserId: String,
  isLocalAdminSession: Boolean = false,
  defaultNameSuggestion: String,
  onExit: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val vm: MultiplayerViewModel = viewModel(
    key = "${currentUserId}_$isLocalAdminSession",
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        val auth = Firebase.auth
        MultiplayerViewModel(
          firestoreRepo = MultiplayerFirestoreRepository(db),
          phase6Repo = Phase6FirestoreRepository(db),
          phase7Repo = Phase7And8FirestoreRepository(db),
          phase9Repo = Phase9And10FirestoreRepository(auth, db),
          phase11Repo = Phase11CaptainVipRepository(db, auth),
          rtdbRepo = RealtimeMultiplayerRepository(),
          currentUserId = currentUserId,
          isLocalAdminSession = isLocalAdminSession,
          appContext = app.applicationContext,
        )
      }
    },
  )

  val accountState by vm.accountState.collectAsStateWithLifecycle()
  val isAdminVerified by vm.isAdminVerified.collectAsStateWithLifecycle()
  val seasonState by vm.seasonState.collectAsStateWithLifecycle()
  val membersState by vm.membersState.collectAsStateWithLifecycle()
  val allUsers by vm.allUsersState.collectAsStateWithLifecycle()
  val votingCycles by vm.votingCyclesState.collectAsStateWithLifecycle()
  val activeVotingCycle by vm.activeVotingCycle.collectAsStateWithLifecycle()
  val cycleNominations by vm.currentCycleNominationsState.collectAsStateWithLifecycle()
  val cycleBallots by vm.currentCycleVotesState.collectAsStateWithLifecycle()
  val votingTallySummary by vm.votingTallySummary.collectAsStateWithLifecycle()
  val evictions by vm.evictionsState.collectAsStateWithLifecycle()
  val announcements by vm.announcementsState.collectAsStateWithLifecycle()
  val tasks by vm.tasksState.collectAsStateWithLifecycle()
  val activeHouseTask by vm.activeHouseTask.collectAsStateWithLifecycle()
  val activeTaskSubmissions by vm.activeTaskSubmissions.collectAsStateWithLifecycle()
  val teams by vm.teamsState.collectAsStateWithLifecycle()
  val captaincyHistory by vm.captaincyHistoryState.collectAsStateWithLifecycle()
  val houseRules by vm.houseRulesState.collectAsStateWithLifecycle()
  val ruleViolations by vm.ruleViolationsState.collectAsStateWithLifecycle()
  val dailySchedule by vm.dailyScheduleState.collectAsStateWithLifecycle()
  val cctvRecordings by vm.cctvRecordingsState.collectAsStateWithLifecycle()
  val isUploadingCctv by vm.isUploadingCctv.collectAsStateWithLifecycle()
  val cctvUploadStatus by vm.cctvUploadStatus.collectAsStateWithLifecycle()
  val collected3dTaskNodes by vm.collected3dTaskNodes.collectAsStateWithLifecycle()
  val rations by vm.rationsState.collectAsStateWithLifecycle()
  val myKitchenProfile by vm.myKitchenProfileState.collectAsStateWithLifecycle()
  val cookingSessions by vm.cookingSessionsState.collectAsStateWithLifecycle()
  val activeTaskProgress by vm.activeTaskProgressState.collectAsStateWithLifecycle()
  val leaderboard by vm.leaderboardState.collectAsStateWithLifecycle()
  val kitchenAnimPulse by vm.kitchenAnimPulse.collectAsStateWithLifecycle()
  val aiConfig by vm.aiConfigState.collectAsStateWithLifecycle()
  val aiSchedules by vm.aiSchedulesState.collectAsStateWithLifecycle()
  val aiProposals by vm.aiProposalsState.collectAsStateWithLifecycle()
  val aiLogs by vm.aiLogsState.collectAsStateWithLifecycle()
  val whileAwaySummary by vm.whileAwaySummary.collectAsStateWithLifecycle()
  val vipPrivileges by vm.vipPrivilegesState.collectAsStateWithLifecycle()
  val privilegeHistory by vm.privilegeHistoryState.collectAsStateWithLifecycle()
  val gameSettings by vm.gameSettingsState.collectAsStateWithLifecycle()
  val adminAuditLogs by vm.adminAuditLogsState.collectAsStateWithLifecycle()
  val adminSession by AdminSessionState.session.collectAsStateWithLifecycle()
  val pendingAutoOpenAdminSuite by AdminSessionState.pendingAutoOpenControlPanel.collectAsStateWithLifecycle()
  val destination by vm.destination.collectAsStateWithLifecycle()
  val actionError by vm.actionError.collectAsStateWithLifecycle()
  val actionMessage by vm.actionMessage.collectAsStateWithLifecycle()
  val isBusy by vm.isBusy.collectAsStateWithLifecycle()
  val remotePlayers by vm.rtdbRepo.remotePlayers.collectAsStateWithLifecycle()
  val doorStates by vm.rtdbRepo.doorStates.collectAsStateWithLifecycle()
  val seatStates by vm.rtdbRepo.seatStates.collectAsStateWithLifecycle()
  val lightingMode by vm.rtdbRepo.lightingMode.collectAsStateWithLifecycle()
  val sessionSuperseded by vm.rtdbRepo.sessionSuperseded.collectAsStateWithLifecycle()
  val kickReason by vm.rtdbRepo.kickReason.collectAsStateWithLifecycle()
  val rtdbCaptain by vm.rtdbRepo.realtimeCaptain.collectAsStateWithLifecycle()
  val realtimeWarning by vm.rtdbRepo.realtimeWarning.collectAsStateWithLifecycle()

  // Phase 12–20 StateFlows
  val p12 = vm.phase12To20
  val wardrobeAppearance by p12.wardrobeState.collectAsStateWithLifecycle()
  val cameraPrefs by p12.cameraPrefsState.collectAsStateWithLifecycle()
  val ttsSettings by p12.ttsSettingsState.collectAsStateWithLifecycle()
  val houseClockState by p12.houseClockState.collectAsStateWithLifecycle()
  val celebrations by p12.celebrationsState.collectAsStateWithLifecycle()
  val finaleState by p12.finaleState.collectAsStateWithLifecycle()
  val publicChatMessages by p12.publicChatMessages.collectAsStateWithLifecycle()
  val privateDmMessages by p12.privateDmMessages.collectAsStateWithLifecycle()
  val moderationFlags by p12.moderationFlags.collectAsStateWithLifecycle()
  val secretTasks by p12.secretTasks.collectAsStateWithLifecycle()
  val biggGramNpcProfiles by p12.biggGramNpcProfiles.collectAsStateWithLifecycle()
  val biggGramPosts by p12.biggGramPosts.collectAsStateWithLifecycle()
  val biggGramStories by p12.biggGramStories.collectAsStateWithLifecycle()
  val npcVotes by p12.npcVotes.collectAsStateWithLifecycle()
  val adminPhoneNotifications by p12.adminPhoneNotifications.collectAsStateWithLifecycle()
  val cctvOnlineMap by p12.cctvOnlineMap.collectAsStateWithLifecycle()
  val speakerPulseNonce by p12.speakerPulseNonce.collectAsStateWithLifecycle()

  val ttsManager = remember(context) { AnnouncementTtsManager(context) }
  val ttsStatus by ttsManager.status.collectAsStateWithLifecycle()
  LaunchedEffect(ttsSettings) {
    ttsManager.updateSettings(ttsSettings)
  }
  DisposableEffect(ttsManager) {
    onDispose { ttsManager.shutdown() }
  }

  var isEditingProfile by remember { mutableStateOf(false) }
  var showAdminSuite by remember { mutableStateOf(false) }
  var showAiOperatorDialog by remember { mutableStateOf(false) }
  var showNominationDialog by remember { mutableStateOf(false) }
  var showBallotDialog by remember { mutableStateOf(false) }
  var showHouseHub by remember { mutableStateOf(false) }
  var showKitchenDialog by remember { mutableStateOf(false) }
  var showWardrobeDialog by remember { mutableStateOf(false) }
  var showProfileDialog by remember { mutableStateOf(false) }
  var showChatDialog by remember { mutableStateOf(false) }
  var showAdminPhoneDialog by remember { mutableStateOf(false) }
  var kitchenInitialTab by remember { mutableIntStateOf(0) }
  var kitchenInitialStation by remember { mutableStateOf("") }
  var pendingOpenCctv by remember { mutableStateOf(false) }
  var initializedAnnouncements by remember { mutableStateOf(false) }
  var lastHandledAnnId by remember { mutableStateOf<String?>(null) }
  var bannerAnnouncement by remember { mutableStateOf<AdminAnnouncement?>(null) }

  LaunchedEffect(isAdminVerified) {
    if (isAdminVerified) {
      vm.ensureDefaultRulesAndScheduleIfAdmin()
    }
  }

  // Trigger official one-time house welcome announcement on first entry into 3D House
  LaunchedEffect(destination) {
    if (destination == HouseDestination.IN_HOUSE) {
      if (vm.triggerHouseWelcomeIfFirstEntry()) {
        ttsManager.speakPublicAnnouncement(
          dedupKey = "welcome_house_first_entry",
          message = AnnouncementPresets.WELCOME_ANNOUNCEMENT,
          languageTag = "en-IN",
          voiceGender = VoiceGender.FEMALE,
        )
      }
    }
  }

  // Auto-open Admin Control Room Suite right after server-verified Admin login
  LaunchedEffect(isAdminVerified, pendingAutoOpenAdminSuite) {
    if (isAdminVerified && pendingAutoOpenAdminSuite) {
      showAdminSuite = true
      AdminSessionState.pendingAutoOpenControlPanel.value = false
    }
  }

  // Broadcast banner & queued Hindi/English TTS delivery for all connected housemates
  LaunchedEffect(announcements) {
    val latest = announcements.firstOrNull()
    if (!initializedAnnouncements) {
      lastHandledAnnId = latest?.id
      initializedAnnouncements = true
      return@LaunchedEffect
    }
    if (latest != null && latest.id.isNotBlank() && latest.id != lastHandledAnnId) {
      lastHandledAnnId = latest.id
      bannerAnnouncement = latest
      p12.triggerSpeakerPulse()
      val gender = if (latest.voiceGender.equals("MALE", ignoreCase = true)) VoiceGender.MALE else VoiceGender.FEMALE
      ttsManager.speakPublicAnnouncement(latest.id, latest.message, latest.languageTag, gender)
    }
  }

  // Real-time Kick / Ban / Eviction enforcement while inside 3D house
  val currentAccount = (accountState as? UiState.Success)?.data
  val currentMembers = (membersState as? UiState.Success)?.data.orEmpty()
  val myMemberStatus = currentMembers.firstOrNull { it.userId == currentUserId }?.status

  LaunchedEffect(currentAccount?.userId) {
    vm.syncPhase7And8Defaults(currentAccount)
  }

  LaunchedEffect(destination, currentAccount?.banned, myMemberStatus, kickReason) {
    if (destination == HouseDestination.IN_HOUSE) {
      when {
        currentAccount?.banned == true -> {
          vm.forceRemovalFromHouse(currentAccount.banReason ?: "Account banned by Admin Control Room.")
        }
        myMemberStatus == "EVICTED" || myMemberStatus == "BANNED" -> {
          vm.forceRemovalFromHouse("You have been evicted or removed from the active BIGG BOSS – THE HOUSE session.")
        }
        !kickReason.isNullOrBlank() -> {
          vm.forceRemovalFromHouse(kickReason.orEmpty())
        }
      }
    }
  }

  val currentSeason = (seasonState as? UiState.Success)?.data
  val currentCaptainUid = currentSeason?.captainUid?.ifBlank { null } ?: rtdbCaptain?.uid?.ifBlank { null }

  val myKitchenEnergy = myKitchenProfile?.energy ?: 75L
  val myKitchenHunger = myKitchenProfile?.hunger ?: 30L
  val nowMsSnapshot = remember(cookingSessions) { System.currentTimeMillis() }
  val liveCookingCount = cookingSessions.count { it.status == "COOKING" && it.readyAtMillis > nowMsSnapshot }
  val readyMealsCount = cookingSessions.count {
    it.servingsRemaining > 0L && (it.status == "READY" || (it.status == "COOKING" && it.readyAtMillis <= nowMsSnapshot))
  }
  val myTaskProg = activeTaskProgress.firstOrNull { it.userId == currentUserId }
  val myCollectedForTask = collected3dTaskNodes[activeHouseTask?.id].orEmpty()
  val myTaskProgressPercent = activeHouseTask?.let { t ->
    if (t.requiredCheckpoints.isNotEmpty()) {
      Phase8Logic.computeProgressPercent(
        (myTaskProg?.completedSteps.orEmpty() + myCollectedForTask).distinct(),
        t.requiredCheckpoints,
      )
    } else {
      myTaskProg?.percent ?: 0L
    }
  } ?: 0L

  val activeVipUids = remember(vipPrivileges) { Phase11VipLogic.activeVipUids(vipPrivileges) }
  val captainVipAccess = currentSeason?.captainVipAccess ?: true

  val handleSignOut: () -> Unit = {
    showAdminSuite = false
    showAiOperatorDialog = false
    showNominationDialog = false
    showBallotDialog = false
    showHouseHub = false
    showKitchenDialog = false
    showWardrobeDialog = false
    showProfileDialog = false
    showChatDialog = false
    showAdminPhoneDialog = false
    signOutSession(
      credentialManager = credentialManager,
      scope = scope,
      onBeforeFirebaseSignOut = {
        if (isLocalAdminSession || adminSession?.isLocalOnly == true) {
          LocalAdminAuthManager.logoutLocalAdmin()
        } else {
          AdminSessionState.clear()
        }
        vm.prepareSignOutCleanup()
      },
    )
  }

  // Enforce Admin Session Expiry (2h TTL)
  LaunchedEffect(adminSession) {
    val activeSnap = adminSession ?: return@LaunchedEffect
    while (true) {
      if (AdminAuthValidation.isSessionExpired(activeSnap)) {
        handleSignOut()
        break
      }
      delay(30_000L)
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    when (val state = accountState) {
      UiState.Loading -> FullScreenLoading("Loading your housemate account...")
      is UiState.Error -> {
        ErrorRecoveryOverlay(
          message = state.message,
          actionLabel = stringResource(R.string.lobby_logout),
          onRetry = handleSignOut,
        )
      }
      is UiState.Success -> {
        val account = state.data
        if (account == null || isEditingProfile) {
          if (isEditingProfile && account != null) {
            BackHandler {
              vm.clearError()
              isEditingProfile = false
            }
          }
          ProfileSetupScreen(
            initialDisplayName = account?.displayName ?: defaultNameSuggestion,
            initialCharacterId = account?.characterId ?: "Player 01",
            isEditingExisting = account != null,
            isBusy = isBusy,
            errorMessage = actionError,
            onSaveProfile = { name, charId ->
              vm.saveProfile(name, charId)
              if (account != null) {
                isEditingProfile = false
              }
            },
            onCancelEdit = if (account != null) {
              {
                vm.clearError()
                isEditingProfile = false
              }
            } else null,
            onSignOut = handleSignOut,
          )
        } else if (destination == HouseDestination.LOBBY) {
          BackHandler { onExit() }
          LobbyScreen(
            account = account,
            isAdminVerified = isAdminVerified,
            seasonState = seasonState,
            membersState = membersState,
            activeVotingCycle = activeVotingCycle,
            captainUid = currentCaptainUid,
            vipPrivileges = vipPrivileges,
            activeTask = activeHouseTask,
            isRtdbConfigured = vm.rtdbRepo.isConfigured,
            sessionSuperseded = sessionSuperseded,
            isBusy = isBusy,
            actionError = actionError,
            onJoinHouse = { vm.enterMultiplayerHouse() },
            onEditProfile = {
              vm.clearError()
              isEditingProfile = true
            },
            onOpenAdminSuite = {
              if (isAdminVerified) showAdminSuite = true
            },
            onOpenAiConsole = {
              if (isAdminVerified) showAiOperatorDialog = true
            },
            onOpenNominations = { showNominationDialog = true },
            onOpenVotingBallot = { showBallotDialog = true },
            onOpenHouseHub = { showHouseHub = true },
            onSignOut = handleSignOut,
            onClearError = { vm.clearError() },
          )
        } else {
          GlassHouseViewport(
            account = account,
            isAdminVerified = isAdminVerified,
            sessionId = vm.sessionId,
            isRtdbConfigured = vm.rtdbRepo.isConfigured,
            sessionSuperseded = sessionSuperseded,
            pendingOpenCctv = pendingOpenCctv,
            isAnyOverlayOpen = showHouseHub || showKitchenDialog || showAdminSuite || showAiOperatorDialog || showNominationDialog || showBallotDialog || showWardrobeDialog || showProfileDialog || showChatDialog || showAdminPhoneDialog,
            onConsumedOpenCctv = { pendingOpenCctv = false },
            activeVotingCycle = activeVotingCycle,
            hasNominatedInCycle = cycleNominations.any { it.voterUid == currentUserId },
            hasVotedInCycle = cycleBallots.any { it.voterUid == currentUserId },
            members = currentMembers,
            remotePlayers = remotePlayers,
            doorStates = doorStates,
            seatStates = seatStates,
            lightingMode = lightingMode,
            captainUid = currentCaptainUid,
            captainVipAccess = captainVipAccess,
            vipUids = activeVipUids,
            activeTask = activeHouseTask,
            collectedNodeIds = collected3dTaskNodes[activeHouseTask?.id].orEmpty(),
            kitchenEnergy = myKitchenEnergy,
            kitchenHunger = myKitchenHunger,
            cookingCount = liveCookingCount,
            readyMealsCount = readyMealsCount,
            myTaskProgressPercent = myTaskProgressPercent,
            kitchenAnimPulse = kitchenAnimPulse,
            aiConfig = aiConfig,
            wardrobeAppearance = wardrobeAppearance,
            cameraPrefs = cameraPrefs,
            houseClockState = houseClockState,
            celebrations = celebrations,
            finaleState = finaleState,
            speakerPulseNonce = speakerPulseNonce,
            cctvOnlineMap = cctvOnlineMap,
            unreadNotificationCount = adminPhoneNotifications.count { !it.read },
            onOpenNominations = { showNominationDialog = true },
            onOpenVotingBallot = { showBallotDialog = true },
            onOpenHouseHub = { showHouseHub = true },
            onOpenKitchenDialog = { tabIdx, stationId ->
              kitchenInitialTab = tabIdx.coerceIn(0, 2)
              kitchenInitialStation = stationId
              showKitchenDialog = true
            },
            onOpenWardrobe = { showWardrobeDialog = true },
            onOpenProfile = { showProfileDialog = true },
            onOpenChat = { showChatDialog = true },
            onOpenAdminPhone = {
              if (isAdminVerified) showAdminPhoneDialog = true
            },
            onCameraPrefsChanged = { mode, dist, sens ->
              p12.saveCameraPreferences(
                cameraPrefs.copy(
                  mode = mode,
                  distance = dist.toFloat().coerceIn(2.0f, 8.5f),
                  sensitivity = sens.toFloat().coerceIn(0.4f, 2.0f),
                )
              )
            },
            onCurrentRoomChanged = { roomName ->
              p12.updatePlayerRoom(currentUserId, roomName)
            },
            onTaskObjectCollected = { taskId, nodeId ->
              vm.record3dTaskNodeCollected(taskId, nodeId)
            },
            onOpenAdminConsole = {
              if (isAdminVerified) showAdminSuite = true
            },
            onOpenAiOperatorConsole = {
              if (isAdminVerified) showAiOperatorDialog = true
            },
            onPublishTransform = { x, y, z, rotY, pState, seatId, expr ->
              vm.rtdbRepo.publishLocalTransform(
                account = account,
                characterId = account.characterId,
                x = x,
                y = y,
                z = z,
                rotY = rotY,
                state = pState,
                seatId = seatId,
                expression = expr,
              )
            },
            onDoorToggled = { doorId, isOpen ->
              if (doorId != "door_admin" || isAdminVerified) {
                vm.rtdbRepo.publishDoorState(doorId, isOpen)
                if (doorId == "door_admin" && isAdminVerified) {
                  vm.reportAdminAction("DOOR_OVERRIDE", "Admin door set open=$isOpen")
                }
              }
            },
            onSeatOccupancyChanged = { seatId, occupied ->
              vm.rtdbRepo.publishSeatState(seatId, occupied, account.displayName)
            },
            onLightingModeChanged = { mode ->
              vm.rtdbRepo.publishLightingMode(mode)
            },
            onCharacterChanged = { charId ->
              vm.updateCharacterPreference(charId)
            },
            onAdminControlAction = { cmd, summary ->
              vm.reportAdminAction(cmd, summary)
            },
            onCctvRecordingCaptured = { camId, camLabel, startMs, durMs, mime, b64 ->
              vm.uploadCctvRecordingFromBridge(camId, camLabel, startMs, durMs, mime, b64)
            },
            onCctvRecordingError = { err ->
              vm.reportCctvRecordingError(err)
            },
            onReturnToLobby = { vm.exitHouseToLobby() },
            onRequestLogout = handleSignOut,
          )
        }
      }
    }

    bannerAnnouncement?.let { ann ->
      LiveAnnouncementBanner(
        announcement = ann,
        onReplayAudio = {
          val gender = if (ann.voiceGender.equals("MALE", ignoreCase = true)) VoiceGender.MALE else VoiceGender.FEMALE
          ttsManager.speak(ann.message, ann.languageTag, gender)
        },
        onDismiss = { bannerAnnouncement = null },
        modifier = Modifier
          .align(Alignment.TopCenter)
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 16.dp, vertical = 12.dp),
      )
    }

    realtimeWarning?.let { warning ->
      RuleWarningBanner(
        warning = warning,
        onDismiss = { vm.rtdbRepo.clearWarningBanner() },
        modifier = Modifier
          .align(Alignment.TopCenter)
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 16.dp, vertical = if (bannerAnnouncement != null) 96.dp else 12.dp),
      )
    }

    if (showHouseHub) {
      PlayerHouseHubDialog(
        currentUserId = currentUserId,
        isAdminVerified = isAdminVerified,
        season = currentSeason,
        members = currentMembers,
        vipPrivileges = vipPrivileges,
        privilegeHistory = privilegeHistory,
        tasks = tasks,
        activeTask = activeHouseTask,
        submissions = activeTaskSubmissions,
        collected3dNodes = collected3dTaskNodes[activeHouseTask?.id].orEmpty(),
        taskProgressList = activeTaskProgress,
        leaderboard = leaderboard,
        teams = teams,
        rules = houseRules,
        violations = ruleViolations,
        scheduleSlots = dailySchedule,
        isBusy = isBusy,
        actionMessage = actionMessage,
        actionError = actionError,
        onSubmitAnswer = { task, ans -> vm.submitMyTaskAnswer(task, ans) },
        onCompleteAndClaimTask = { task, proof -> vm.completeAndClaimTask(task, proof) },
        onOpenKitchenFromTask = {
          showHouseHub = false
          kitchenInitialTab = 1
          kitchenInitialStation = "kitchen_stove_1"
          showKitchenDialog = true
        },
        onClearFeedback = {
          vm.clearError()
          vm.clearMessage()
        },
        onDismiss = { showHouseHub = false },
      )
    }

    if (showKitchenDialog) {
      KitchenAndCookingDialog(
        currentUserId = currentUserId,
        initialTab = kitchenInitialTab,
        initialStationId = kitchenInitialStation,
        rations = rations,
        kitchenProfile = myKitchenProfile,
        cookingSessions = cookingSessions,
        isBusy = isBusy,
        actionMessage = actionMessage,
        actionError = actionError,
        onCollectRation = { itemId, amount -> vm.collectRation(itemId, amount) },
        onStartCooking = { stationId, recipe -> vm.startCooking(stationId, recipe) },
        onCompleteCooking = { session -> vm.completeCooking(session) },
        onClaimAndEatMeal = { session, seated -> vm.claimAndEatMeal(session, seated) },
        onClearFeedback = {
          vm.clearError()
          vm.clearMessage()
        },
        onDismiss = { showKitchenDialog = false },
      )
    }

    if (showNominationDialog && activeVotingCycle != null && activeVotingCycle?.status == "NOMINATIONS") {
      HouseNominationDialog(
        cycle = activeVotingCycle!!,
        currentUserId = currentUserId,
        members = currentMembers,
        nominations = cycleNominations,
        myNomination = cycleNominations.firstOrNull { it.voterUid == currentUserId },
        isBusy = isBusy,
        onSubmitNomination = { targets, reason ->
          vm.submitMyPeerNomination(activeVotingCycle!!.id, targets, reason)
        },
        onDismiss = { showNominationDialog = false },
      )
    }

    if (showBallotDialog && activeVotingCycle != null) {
      HouseVotingBallotDialog(
        cycle = activeVotingCycle!!,
        myBallot = cycleBallots.firstOrNull { it.voterUid == currentUserId },
        ballots = cycleBallots,
        isAdminVerified = isAdminVerified,
        isBusy = isBusy,
        onCastVote = { targetUid ->
          vm.castMyRealVote(activeVotingCycle!!.id, targetUid)
        },
        onDismiss = { showBallotDialog = false },
        npcOnlyVoting = true,
      )
    }

    if (showAiOperatorDialog && isAdminVerified) {
      AiOperatorConsoleDialog(
        isAdminVerified = isAdminVerified,
        config = aiConfig,
        schedules = aiSchedules,
        proposals = aiProposals,
        logs = aiLogs,
        whileAwaySummary = whileAwaySummary,
        isBusy = isBusy,
        actionMessage = actionMessage,
        actionError = actionError,
        onDismiss = { showAiOperatorDialog = false },
        onClearFeedback = {
          vm.clearError()
          vm.clearMessage()
        },
        onSetEnabled = { vm.setAiEnabled(it) },
        onSetMode = { vm.setAiMode(it) },
        onSetModules = { hi, ki, ta, ti -> vm.setAiAutomationModules(hi, ki, ta, ti) },
        onEmergencyStop = { vm.triggerAiEmergencyStop() },
        onAcknowledgeWhileAway = { vm.acknowledgeAiWhileAway() },
        onCreateSchedule = { act, ttl, pay, delayM, recM ->
          vm.createAiSchedule(act, ttl, pay, delayM, recM)
        },
        onCancelSchedule = { id -> vm.cancelAiSchedule(id) },
        onRunAiTick = { src -> vm.runAiAutomationTick(src) },
        onGenerateSuggestion = { vm.generateManualAiSuggestion() },
        onApproveProposal = { prop -> vm.approveAiProposal(prop) },
        onRejectProposal = { prop -> vm.rejectAiProposal(prop) },
      )
    }

    if (showAdminSuite && isAdminVerified) {
      AdminControlSuiteDialog(
        currentAdminUid = currentUserId,
        isAdminVerified = isAdminVerified,
        season = currentSeason,
        members = currentMembers,
        allUsers = allUsers,
        votingCycles = votingCycles,
        activeCycle = activeVotingCycle,
        nominations = cycleNominations,
        ballots = cycleBallots,
        votingTallySummary = votingTallySummary,
        evictions = evictions,
        announcements = announcements,
        ttsStatus = ttsStatus,
        tasks = tasks,
        activeTask = activeHouseTask,
        taskSubmissions = activeTaskSubmissions,
        taskProgressList = activeTaskProgress,
        rations = rations,
        cookingSessions = cookingSessions,
        leaderboard = leaderboard,
        teams = teams,
        captaincyHistory = captaincyHistory,
        houseRules = houseRules,
        ruleViolations = ruleViolations,
        dailySchedule = dailySchedule,
        cctvRecordings = cctvRecordings,
        isUploadingCctv = isUploadingCctv,
        cctvUploadStatus = cctvUploadStatus,
        aiConfig = aiConfig,
        aiSchedules = aiSchedules,
        aiProposals = aiProposals,
        aiLogs = aiLogs,
        whileAwaySummary = whileAwaySummary,
        vipPrivileges = vipPrivileges,
        privilegeHistory = privilegeHistory,
        gameSettings = gameSettings,
        adminAuditLogs = adminAuditLogs,
        adminSession = adminSession,
        isBusy = isBusy,
        actionMessage = actionMessage,
        actionError = actionError,
        onDismiss = { showAdminSuite = false },
        onClearFeedback = {
          vm.clearError()
          vm.clearMessage()
        },
        onOpenCctvWall = {
          showAdminSuite = false
          pendingOpenCctv = true
          if (destination == HouseDestination.LOBBY) {
            vm.enterMultiplayerHouse()
          }
        },
        onEnterControlRoom = {
          showAdminSuite = false
          if (destination == HouseDestination.LOBBY) {
            vm.enterMultiplayerHouse()
          }
        },
        onDeleteCctvRecording = { rec -> vm.deleteCctvRecording(rec) },
        onSetSeasonStatus = { status -> vm.setSeasonStatus(status) },
        onModeratePlayer = { targetUid, act, reason -> vm.moderatePlayer(targetUid, act, reason) },
        onSelectCycle = { cid -> vm.selectVotingCycle(cid) },
        onStartConfiguredCycle = { title, startStatus, nominees, npcEnabled, vType, nomPerP, durM, vis, tie ->
          vm.startConfiguredVotingOrNominationCycle(
            title = title,
            startStatus = startStatus,
            nominees = nominees,
            npcVotesEnabled = npcEnabled,
            voteType = vType,
            nominationsPerPlayer = nomPerP,
            votingDurationMinutes = durM,
            resultVisibility = vis,
            tieBreakerPolicy = tie,
          )
        },
        onPromoteToOpenVoting = { cycle, nominees ->
          vm.advanceCycleToOpenVoting(cycle, nominees)
        },
        onUpdateCycleState = { cycle, nextStatus, npcEnabled ->
          vm.updateVotingCycleState(cycle, nextStatus, npcEnabled)
        },
        onCancelVotingCycle = { cycle, reason ->
          vm.cancelVotingCycle(cycle, reason)
        },
        onCastNpcVote = { cid, label, targetUid ->
          vm.castSimulatedNpcVote(cid, label, targetUid)
        },
        onConfirmEviction = { cycle, nominee, rVotes, nVotes, msg, lang, gender ->
          vm.confirmEviction(cycle, nominee, rVotes, nVotes, msg, lang, gender)
        },
        onPreviewSpeech = { msg, lang, gender ->
          ttsManager.speak(msg, lang, gender)
        },
        onBroadcastAll = { msg, presetId, lang, gender ->
          vm.sendAnnouncement(msg, presetId, lang, gender)
        },
        onSelectTask = { tid -> vm.selectHouseTask(tid) },
        onSaveOrStartTask = { tid, title, desc, instr, actType, mode, isCap, room, dur, q, opts, pts, expAns, startNow, cat, diff, aUids, aTeamId, reqCkpts, reqRecId, rewAch ->
          vm.saveOrStartHouseTask(
            taskId = tid,
            title = title,
            description = desc,
            instructions = instr,
            activityType = actType,
            assignmentMode = mode,
            isCaptaincy = isCap,
            targetRoom = room,
            durationSeconds = dur,
            promptQuestion = q,
            options = opts,
            maxPoints = pts,
            expectedAnswer = expAns,
            startImmediately = startNow,
            category = cat,
            difficulty = diff,
            assignedUids = aUids,
            assignedTeamId = aTeamId,
            requiredCheckpoints = reqCkpts,
            requiredRecipeId = reqRecId,
            rewardAchievement = rewAch,
          )
        },
        onUpdateTaskStatus = { t, next -> vm.updateHouseTaskStatus(t, next) },
        onDeclareTaskWinner = { t, overrideSub -> vm.declareTaskWinner(t, overrideSub) },
        onSaveTeam = { tid, name, hex, mUids, sc, wins ->
          vm.saveHouseTeam(tid, name, hex, mUids, sc, wins)
        },
        onAssignCaptain = { mem, title, pts ->
          vm.assignHouseCaptain(mem, title, pts)
        },
        onSaveRule = { rid, num, title, desc, sev, active ->
          vm.saveHouseRule(rid, num, title, desc, sev, active)
        },
        onBroadcastRule = { msg ->
          vm.sendAnnouncement(msg, "house_rule_broadcast", "en-IN", VoiceGender.FEMALE)
        },
        onIssueWarning = { rule, target, lvl, note ->
          vm.issueRuleWarning(rule, target, lvl, note)
        },
        onSaveScheduleSlot = { slot -> vm.saveScheduleSlot(slot) },
        onPublishSchedule = { activeSlotId, broadcast -> vm.publishDailySchedule(activeSlotId, broadcast) },
        onReplenishRation = { item, add -> vm.adminReplenishRation(item, add) },
        onReplenishAllRations = { vm.adminReplenishAllRations() },
        onToggleRationPermission = { item, permitted -> vm.adminToggleRationPermission(item, permitted) },
        onManageCookingSession = { ses, next -> vm.adminManageCookingSession(ses, next) },
        onSetAiEnabled = { vm.setAiEnabled(it) },
        onSetAiMode = { vm.setAiMode(it) },
        onSetAiModules = { hi, ki, ta, ti -> vm.setAiAutomationModules(hi, ki, ta, ti) },
        onTriggerAiEmergencyStop = { vm.triggerAiEmergencyStop() },
        onAcknowledgeAiWhileAway = { vm.acknowledgeAiWhileAway() },
        onCreateAiSchedule = { act, ttl, pay, delayM, recM ->
          vm.createAiSchedule(act, ttl, pay, delayM, recM)
        },
        onCancelAiSchedule = { id -> vm.cancelAiSchedule(id) },
        onRunAiTick = { src -> vm.runAiAutomationTick(src) },
        onGenerateAiSuggestion = { vm.generateManualAiSuggestion() },
        onApproveAiProposal = { prop -> vm.approveAiProposal(prop) },
        onRejectAiProposal = { prop -> vm.rejectAiProposal(prop) },
        onAssignOrReplaceCaptain = { mem, title, powers -> vm.assignOrReplaceCaptainWithPowers(mem, title, powers) },
        onRemoveCaptain = { vm.removeHouseCaptain() },
        onUpdateCaptainPowers = { powers -> vm.updateCaptainPowers(powers) },
        onSetPlayerVip = { mem, enabled, label -> vm.setPlayerVipPrivilege(mem, enabled, label) },
        onSaveGameSettings = { settings -> vm.saveGameSettings(settings) },
        onCalculateServerTally = { cycle, closeVoting -> vm.calculateAndCloseVotingOnServer(cycle, closeVoting) },
        onChangeAdminPassword = { cur, nxt, conf, clearCb -> vm.changeAdminPassword(cur, nxt, conf, clearCb) },
        onAdminLogout = handleSignOut,
        moderationFlags = moderationFlags,
        onOpenAdminPhone = {
          showAdminSuite = false
          if (isAdminVerified) showAdminPhoneDialog = true
        },
        onSetPlayerMuted = { uid, muted, name -> p12.setPlayerMuted(uid, muted, name) },
        onSetPlayerTaskRestricted = { uid, restricted, name -> p12.setPlayerTaskRestricted(uid, restricted, name) },
      )
    }

    if (showWardrobeDialog) {
      WardrobeStudioDialog(
        currentAppearance = wardrobeAppearance,
        onApplyWardrobe = {
          p12.applyWardrobeAppearance(it)
          showWardrobeDialog = false
        },
        onDismiss = { showWardrobeDialog = false },
      )
    }

    if (showProfileDialog && currentAccount != null) {
      val extendedProfiles = remember(currentMembers, leaderboard, currentSeason, vipPrivileges, tasks, captaincyHistory) {
        BiggGramAndNpcVotingEngine.computeExtendedPlayerProfiles(
          members = currentMembers,
          leaderboard = leaderboard,
          season = currentSeason,
          vipPrivileges = vipPrivileges,
          tasks = tasks,
          captaincyHistory = captaincyHistory,
        )
      }
      PlayerExtendedProfileDialog(
        currentUserId = currentUserId,
        account = currentAccount,
        season = currentSeason,
        profiles = extendedProfiles,
        cameraPrefs = cameraPrefs,
        finaleState = finaleState,
        clockState = houseClockState,
        onSaveCameraPrefs = { p12.saveCameraPreferences(it) },
        onDismiss = { showProfileDialog = false },
      )
    }

    if (showChatDialog) {
      HouseChatAndCommsDialog(
        currentUserId = currentUserId,
        isAdminVerified = isAdminVerified,
        members = currentMembers,
        publicMessages = publicChatMessages,
        privateDms = privateDmMessages,
        announcements = announcements,
        ttsSettings = ttsSettings,
        secretTasks = secretTasks,
        isMutedInChat = moderationFlags[currentUserId]?.muted == true,
        onSendPublicMessage = { p12.sendPublicChatMessage(it) },
        onSendPrivateDm = { uid, name, txt -> p12.sendPrivateDirectMessage(uid, name, txt) },
        onUpdateTtsSettings = { p12.updateTtsSettings(it) },
        onReplayWelcome = {
          p12.triggerSpeakerPulse()
          ttsManager.speak(AnnouncementPresets.WELCOME_ANNOUNCEMENT, "en-IN", VoiceGender.FEMALE)
        },
        onCompleteSecretTask = { tid -> p12.completeSecretTask(tid) },
        onDismiss = { showChatDialog = false },
      )
    }

    // Main Admin Exclusive Personal Smartphone (Never shown to normal players)
    if (showAdminPhoneDialog && isAdminVerified) {
      AdminPersonalPhoneDialog(
        isAdminVerified = isAdminVerified,
        members = currentMembers,
        allUsers = allUsers,
        activeCycle = activeVotingCycle,
        nominations = cycleNominations,
        npcProfiles = biggGramNpcProfiles,
        biggGramPosts = biggGramPosts,
        biggGramStories = biggGramStories,
        npcVotes = npcVotes,
        notifications = adminPhoneNotifications,
        clockState = houseClockState,
        celebrations = celebrations,
        finaleState = finaleState,
        moderationFlags = moderationFlags,
        cctvOnlineMap = cctvOnlineMap,
        actionMessage = actionMessage,
        actionError = actionError,
        onDismiss = { showAdminPhoneDialog = false },
        onClearFeedback = {
          vm.clearError()
          vm.clearMessage()
        },
        onToggleLikePost = { p12.toggleLikeBiggGramPost(it) },
        onAddComment = { postId, text -> p12.addBiggGramPostComment(postId, "@biggboss.official", "BIGG BOSS", text) },
        onCreatePost = { p12.createAdminBiggGramPost(it) },
        onMarkStoryViewed = { p12.markStoryViewed(it) },
        onPublishNomineesToBiggGram = { cycle, nominees -> p12.publishNomineesToBiggGram(cycle, nominees) },
        onRunAllNpcVotes = { cycle -> p12.runAllRemainingNpcVotesOnBiggGram(cycle) },
        onCastSingleNpcVote = { cycle, npc, target -> p12.castNpcVoteOnBiggGram(cycle, npc, target) },
        onToggleCctvOnline = { camId -> p12.toggleCctvCameraOnline(camId) },
        onOpen3dCctvMatrix = {
          showAdminPhoneDialog = false
          pendingOpenCctv = true
          if (destination == HouseDestination.LOBBY) {
            vm.enterMultiplayerHouse()
          }
        },
        onSendTtsAnnouncement = { msg, lang, gender ->
          vm.sendAnnouncement(msg, "admin_phone_tts", lang, gender)
        },
        onTriggerEmergencySiren = { active, msg -> p12.triggerEmergencySiren(active, msg) },
        onSetHouseClock = { hr, min, mode, auto -> p12.setHouseTime(hr, min, mode, auto) },
        onSetWeather = { p12.setHouseWeather(it) },
        onTogglePartyMode = { p12.togglePartyMode(it) },
        onTriggerCelebration = { type, title, desc, now -> p12.createOrTriggerCelebration(type, title, desc, now) },
        onTriggerFinale = { winner, runnerUp, trophy -> p12.triggerGrandFinale(winner, runnerUp, trophy) },
        onAssignSecretTask = { target, title, instr, pts -> p12.assignSecretTask(target, title, instr, pts) },
        onSetPlayerMuted = { uid, muted, name -> p12.setPlayerMuted(uid, muted, name) },
        onSetPlayerTaskRestricted = { uid, restricted, name -> p12.setPlayerTaskRestricted(uid, restricted, name) },
        onModeratePlayer = { targetUid, act, reason -> vm.moderatePlayer(targetUid, act, reason) },
        onMarkNotificationsRead = { p12.markAllNotificationsRead() },
      )
    }
  }
}

private class GameBridge(
  private val mainHandler: Handler,
  private val onReady: () -> Unit,
  private val onError: (String) -> Unit,
  private val onPlayerTransformCb: (Double, Double, Double, Double, String, String?, String) -> Unit,
  private val onDoorToggledCb: (String, Boolean) -> Unit,
  private val onSeatOccupancyCb: (String, Boolean) -> Unit,
  private val onLightingModeCb: (String) -> Unit,
  private val onCharacterChangedCb: (String) -> Unit,
  private val onAdminActionCb: (String, String) -> Unit,
  private val onOpenAdminConsoleCb: () -> Unit,
  private val onOpenAiOperatorConsoleCb: () -> Unit,
  private val onOpenHouseHubCb: () -> Unit,
  private val onOpenKitchenDialogCb: (Int, String) -> Unit,
  private val onTaskObjectCollectedCb: (String, String) -> Unit,
  private val onCctvRecordingCapturedCb: (String, String, Long, Long, String, String) -> Unit,
  private val onCctvRecordingErrorCb: (String) -> Unit,
  private val onOpenWardrobeCb: () -> Unit,
  private val onOpenProfileCb: () -> Unit,
  private val onOpenChatCb: () -> Unit,
  private val onOpenAdminPhoneCb: () -> Unit,
  private val onCameraPrefsChangedCb: (String, Double, Double) -> Unit,
  private val onCurrentRoomChangedCb: (String) -> Unit,
  private val onReturnLobbyCb: () -> Unit,
  private val onLogoutCb: () -> Unit,
) {
  @JavascriptInterface
  fun onSceneReady() {
    mainHandler.post { onReady() }
  }

  @JavascriptInterface
  fun onSceneError(message: String) {
    mainHandler.post { onError(message) }
  }

  @JavascriptInterface
  fun onPlayerTransform(
    x: Double,
    y: Double,
    z: Double,
    rotY: Double,
    state: String,
    seatId: String,
    expression: String,
  ) {
    mainHandler.post {
      onPlayerTransformCb(x, y, z, rotY, state, seatId.ifBlank { null }, expression.ifBlank { "neutral" })
    }
  }

  @JavascriptInterface
  fun onDoorToggled(doorId: String, isOpen: Boolean) {
    mainHandler.post { onDoorToggledCb(doorId, isOpen) }
  }

  @JavascriptInterface
  fun onSeatOccupancyChanged(seatId: String, occupied: Boolean) {
    mainHandler.post { onSeatOccupancyCb(seatId, occupied) }
  }

  @JavascriptInterface
  fun onLightingModeChanged(mode: String) {
    mainHandler.post { onLightingModeCb(mode) }
  }

  @JavascriptInterface
  fun onCharacterChanged(characterId: String) {
    mainHandler.post { onCharacterChangedCb(characterId) }
  }

  @JavascriptInterface
  fun onAdminControlAction(commandType: String, payloadSummary: String) {
    mainHandler.post { onAdminActionCb(commandType, payloadSummary) }
  }

  @JavascriptInterface
  fun onOpenAdminConsole() {
    mainHandler.post { onOpenAdminConsoleCb() }
  }

  @JavascriptInterface
  fun onOpenAiOperatorConsole() {
    mainHandler.post { onOpenAiOperatorConsoleCb() }
  }

  @JavascriptInterface
  fun onOpenHouseHub() {
    mainHandler.post { onOpenHouseHubCb() }
  }

  @JavascriptInterface
  fun onOpenKitchenDialog(tabIndex: Int, stationId: String) {
    mainHandler.post { onOpenKitchenDialogCb(tabIndex, stationId) }
  }

  @JavascriptInterface
  fun onTaskObjectCollected(taskId: String, nodeId: String) {
    mainHandler.post { onTaskObjectCollectedCb(taskId, nodeId) }
  }

  @JavascriptInterface
  fun onOpenWardrobe() {
    mainHandler.post { onOpenWardrobeCb() }
  }

  @JavascriptInterface
  fun onOpenProfile() {
    mainHandler.post { onOpenProfileCb() }
  }

  @JavascriptInterface
  fun onOpenChat() {
    mainHandler.post { onOpenChatCb() }
  }

  @JavascriptInterface
  fun onOpenAdminPhone() {
    mainHandler.post { onOpenAdminPhoneCb() }
  }

  @JavascriptInterface
  fun onCameraPrefsChanged(mode: String, distance: Double, sensitivity: Double) {
    mainHandler.post { onCameraPrefsChangedCb(mode, distance, sensitivity) }
  }

  @JavascriptInterface
  fun onCurrentRoomChanged(roomName: String) {
    mainHandler.post { onCurrentRoomChangedCb(roomName) }
  }

  @JavascriptInterface
  fun onCctvRecordingCaptured(
    cameraId: String,
    cameraLabel: String,
    startedAtMillis: Double,
    durationMillis: Double,
    mimeType: String,
    base64Data: String,
  ) {
    mainHandler.post {
      onCctvRecordingCapturedCb(
        cameraId,
        cameraLabel,
        startedAtMillis.toLong(),
        durationMillis.toLong(),
        mimeType,
        base64Data,
      )
    }
  }

  @JavascriptInterface
  fun onCctvRecordingError(message: String) {
    mainHandler.post { onCctvRecordingErrorCb(message) }
  }

  @JavascriptInterface
  fun onRequestReturnToLobby() {
    mainHandler.post { onReturnLobbyCb() }
  }

  @JavascriptInterface
  fun onRequestLogout() {
    mainHandler.post { onLogoutCb() }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GlassHouseViewport(
  account: UserAccount,
  isAdminVerified: Boolean,
  sessionId: String,
  isRtdbConfigured: Boolean,
  sessionSuperseded: Boolean,
  pendingOpenCctv: Boolean,
  isAnyOverlayOpen: Boolean,
  onConsumedOpenCctv: () -> Unit,
  activeVotingCycle: com.example.multiplayer.VotingCycle?,
  hasNominatedInCycle: Boolean,
  hasVotedInCycle: Boolean,
  members: List<SeasonMember>,
  remotePlayers: Map<String, RemotePlayerState>,
  doorStates: Map<String, RemoteDoorState>,
  seatStates: Map<String, RemoteSeatState>,
  lightingMode: String?,
  captainUid: String?,
  captainVipAccess: Boolean = true,
  vipUids: List<String> = emptyList(),
  activeTask: HouseTask?,
  collectedNodeIds: Set<String>,
  kitchenEnergy: Long,
  kitchenHunger: Long,
  cookingCount: Int,
  readyMealsCount: Int,
  myTaskProgressPercent: Long,
  kitchenAnimPulse: Pair<String, Long>,
  aiConfig: AiControlConfig?,
  wardrobeAppearance: WardrobeAppearance = WardrobeAppearance(),
  cameraPrefs: CameraPreferences = CameraPreferences(),
  houseClockState: HouseClockState = HouseClockState(),
  celebrations: List<com.example.multiplayer.HouseCelebrationEvent> = emptyList(),
  finaleState: FinaleCeremonyState = FinaleCeremonyState(),
  speakerPulseNonce: Long = 0L,
  cctvOnlineMap: Map<String, Boolean> = emptyMap(),
  unreadNotificationCount: Int = 0,
  onOpenNominations: () -> Unit,
  onOpenVotingBallot: () -> Unit,
  onOpenHouseHub: () -> Unit,
  onOpenKitchenDialog: (Int, String) -> Unit,
  onOpenWardrobe: () -> Unit = {},
  onOpenProfile: () -> Unit = {},
  onOpenChat: () -> Unit = {},
  onOpenAdminPhone: () -> Unit = {},
  onCameraPrefsChanged: (String, Double, Double) -> Unit = { _, _, _ -> },
  onCurrentRoomChanged: (String) -> Unit = {},
  onTaskObjectCollected: (String, String) -> Unit,
  onOpenAdminConsole: () -> Unit,
  onOpenAiOperatorConsole: () -> Unit,
  onPublishTransform: (Double, Double, Double, Double, String, String?, String) -> Unit,
  onDoorToggled: (String, Boolean) -> Unit,
  onSeatOccupancyChanged: (String, Boolean) -> Unit,
  onLightingModeChanged: (String) -> Unit,
  onCharacterChanged: (String) -> Unit,
  onAdminControlAction: (String, String) -> Unit,
  onCctvRecordingCaptured: (String, String, Long, Long, String, String) -> Unit,
  onCctvRecordingError: (String) -> Unit,
  onReturnToLobby: () -> Unit,
  onRequestLogout: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val mainHandler = remember { Handler(Looper.getMainLooper()) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var sceneReady by remember { mutableStateOf(false) }
  var webViewRecreateKey by remember { mutableIntStateOf(0) }
  var crashRecoveryAttempts by remember { mutableIntStateOf(0) }

  val assetLoader = remember(context) {
    WebViewAssetLoader.Builder()
      .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
      .build()
  }

  BackHandler {
    val wv = webViewRef
    if (wv != null) {
      wv.evaluateJavascript("window.closeGameOverlays ? window.closeGameOverlays() : false") { result ->
        if (result != "true") {
          onReturnToLobby()
        }
      }
    } else {
      onReturnToLobby()
    }
  }

  // Sync verified session + roster + RTDB snapshots into Three.js whenever sceneReady or state changes
  LaunchedEffect(sceneReady, account, isAdminVerified, members, isRtdbConfigured, sessionSuperseded) {
    val wv = webViewRef ?: return@LaunchedEffect
    if (!sceneReady) return@LaunchedEffect
    val payload = buildSessionJson(account, isAdminVerified, sessionId, isRtdbConfigured, sessionSuperseded, members)
    wv.evaluateJavascript("window.MultiplayerBridge && window.MultiplayerBridge.setAuthenticatedSession($payload);", null)
  }

  LaunchedEffect(sceneReady, pendingOpenCctv, isAdminVerified) {
    val wv = webViewRef ?: return@LaunchedEffect
    if (!sceneReady || !pendingOpenCctv) return@LaunchedEffect
    if (isAdminVerified) {
      wv.evaluateJavascript("window.MultiplayerBridge && window.MultiplayerBridge.setCctvMode(true, null);", null)
    }
    onConsumedOpenCctv()
  }

  LaunchedEffect(sceneReady, remotePlayers) {
    val wv = webViewRef ?: return@LaunchedEffect
    if (!sceneReady) return@LaunchedEffect
    val payload = buildRemotePlayersJson(remotePlayers)
    wv.evaluateJavascript("window.MultiplayerBridge && window.MultiplayerBridge.onRemotePlayersUpdate($payload);", null)
  }

  LaunchedEffect(sceneReady, doorStates, seatStates, lightingMode) {
    val wv = webViewRef ?: return@LaunchedEffect
    if (!sceneReady) return@LaunchedEffect
    val payload = buildWorldStateJson(doorStates, seatStates, lightingMode)
    wv.evaluateJavascript("window.MultiplayerBridge && window.MultiplayerBridge.onWorldStateUpdate($payload);", null)
  }

  LaunchedEffect(sceneReady, captainUid, captainVipAccess, vipUids, activeTask, collectedNodeIds) {
    val wv = webViewRef ?: return@LaunchedEffect
    if (!sceneReady) return@LaunchedEffect
    val payload = buildPhase6Json(captainUid, captainVipAccess, vipUids, activeTask, collectedNodeIds)
    wv.evaluateJavascript("window.MultiplayerBridge && window.MultiplayerBridge.onPhase6StateUpdate($payload);", null)
  }

  LaunchedEffect(
    sceneReady,
    kitchenEnergy,
    kitchenHunger,
    cookingCount,
    readyMealsCount,
    myTaskProgressPercent,
    kitchenAnimPulse,
  ) {
    val wv = webViewRef ?: return@LaunchedEffect
    if (!sceneReady) return@LaunchedEffect
    val payload = buildPhase7And8Json(
      energy = kitchenEnergy,
      hunger = kitchenHunger,
      cookingCount = cookingCount,
      readyMealsCount = readyMealsCount,
      taskProgressPercent = myTaskProgressPercent,
      animPose = kitchenAnimPulse.first,
      animNonce = kitchenAnimPulse.second,
    )
    wv.evaluateJavascript("window.MultiplayerBridge && window.MultiplayerBridge.onPhase7And8StateUpdate($payload);", null)
  }

  LaunchedEffect(sceneReady, aiConfig, isAdminVerified) {
    val wv = webViewRef ?: return@LaunchedEffect
    if (!sceneReady || !isAdminVerified) return@LaunchedEffect
    val payload = buildPhase9AiJson(aiConfig)
    wv.evaluateJavascript("window.MultiplayerBridge && window.MultiplayerBridge.onPhase9AiStateUpdate($payload);", null)
  }

  LaunchedEffect(
    sceneReady,
    wardrobeAppearance,
    cameraPrefs,
    houseClockState,
    celebrations,
    finaleState,
    speakerPulseNonce,
    cctvOnlineMap,
    unreadNotificationCount,
  ) {
    val wv = webViewRef ?: return@LaunchedEffect
    if (!sceneReady) return@LaunchedEffect
    val payload = buildPhase12To20Json(
      wardrobe = wardrobeAppearance,
      cameraPrefs = cameraPrefs,
      clock = houseClockState,
      celebrations = celebrations,
      finale = finaleState,
      speakerPulseNonce = speakerPulseNonce,
      cctvOnlineMap = cctvOnlineMap,
      unreadNotificationCount = unreadNotificationCount,
    )
    wv.evaluateJavascript("window.MultiplayerBridge && window.MultiplayerBridge.onPhase12To20StateUpdate($payload);", null)
  }

  LaunchedEffect(sceneReady, isAnyOverlayOpen, webViewRecreateKey) {
    val wv = webViewRef ?: return@LaunchedEffect
    if (!sceneReady) return@LaunchedEffect
    wv.evaluateJavascript(
      "window.MultiplayerBridge && window.MultiplayerBridge.setPaused($isAnyOverlayOpen);",
      null,
    )
  }

  // Pause & resume WebView cleanly with Activity lifecycle to prevent GPU renderer termination
  DisposableEffect(lifecycleOwner, webViewRecreateKey) {
    val observer = LifecycleEventObserver { _, event ->
      val wv = webViewRef ?: return@LifecycleEventObserver
      when (event) {
        Lifecycle.Event.ON_PAUSE -> runCatching {
          wv.evaluateJavascript("window.MultiplayerBridge && window.MultiplayerBridge.setPaused(true);", null)
          wv.onPause()
        }
        Lifecycle.Event.ON_RESUME -> runCatching {
          wv.onResume()
          wv.evaluateJavascript("window.MultiplayerBridge && window.MultiplayerBridge.setPaused($isAnyOverlayOpen);", null)
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  DisposableEffect(webViewRecreateKey) {
    onDispose {
      webViewRef?.apply {
        runCatching {
          evaluateJavascript("window.disposeThreeScene && window.disposeThreeScene();", null)
          stopLoading()
          onPause()
          removeJavascriptInterface("AndroidBridge")
          (parent as? ViewGroup)?.removeView(this)
          destroy()
        }
      }
      webViewRef = null
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF0C101B)),
  ) {
    key(webViewRecreateKey) {
      AndroidView(
        modifier = Modifier
          .fillMaxSize()
          .testTag("threejs_webview"),
        factory = { ctx ->
          WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(AndroidColor.parseColor("#0C101B"))
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
            }
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
          }

          addJavascriptInterface(
            GameBridge(
              mainHandler = mainHandler,
              onReady = {
                errorMessage = null
                sceneReady = true
              },
              onError = { msg ->
                errorMessage = msg.ifBlank { context.getString(R.string.error_fallback_body) }
              },
              onPlayerTransformCb = onPublishTransform,
              onDoorToggledCb = onDoorToggled,
              onSeatOccupancyCb = onSeatOccupancyChanged,
              onLightingModeCb = onLightingModeChanged,
              onCharacterChangedCb = onCharacterChanged,
              onAdminActionCb = onAdminControlAction,
              onOpenAdminConsoleCb = onOpenAdminConsole,
              onOpenAiOperatorConsoleCb = onOpenAiOperatorConsole,
              onOpenHouseHubCb = onOpenHouseHub,
              onOpenKitchenDialogCb = onOpenKitchenDialog,
              onTaskObjectCollectedCb = onTaskObjectCollected,
              onCctvRecordingCapturedCb = onCctvRecordingCaptured,
              onCctvRecordingErrorCb = onCctvRecordingError,
              onOpenWardrobeCb = onOpenWardrobe,
              onOpenProfileCb = onOpenProfile,
              onOpenChatCb = onOpenChat,
              onOpenAdminPhoneCb = onOpenAdminPhone,
              onCameraPrefsChangedCb = onCameraPrefsChanged,
              onCurrentRoomChangedCb = onCurrentRoomChanged,
              onReturnLobbyCb = onReturnToLobby,
              onLogoutCb = onRequestLogout,
            ),
            "AndroidBridge",
          )

          webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
              Log.d(
                TAG,
                "[${consoleMessage.messageLevel()}] ${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})",
              )
              return true
            }
          }

          webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
              view: WebView,
              request: WebResourceRequest,
            ): WebResourceResponse? {
              return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun onReceivedError(
              view: WebView,
              request: WebResourceRequest,
              error: WebResourceError,
            ) {
              if (request.isForMainFrame) {
                errorMessage = error.description?.toString()
                  ?: context.getString(R.string.error_fallback_body)
              }
            }

            override fun onRenderProcessGone(
              view: WebView,
              detail: RenderProcessGoneDetail,
            ): Boolean {
              Log.e(TAG, "WebView render process gone (didCrash=${detail.didCrash()}, attempt=$crashRecoveryAttempts)")
              mainHandler.post {
                if (webViewRef === view) {
                  webViewRef = null
                }
                runCatching {
                  view.stopLoading()
                  view.onPause()
                  view.removeJavascriptInterface("AndroidBridge")
                  (view.parent as? ViewGroup)?.removeView(view)
                  view.destroy()
                }
                sceneReady = false
                if (crashRecoveryAttempts < 3) {
                  crashRecoveryAttempts += 1
                  errorMessage = null
                  webViewRecreateKey += 1
                } else {
                  errorMessage = context.getString(R.string.error_fallback_body)
                }
              }
              return true
            }
          }

          val targetUrl = if (crashRecoveryAttempts > 0) "$ENTRY_URL?eco=1" else ENTRY_URL
          loadUrl(targetUrl)
          webViewRef = this
        }
      },
      update = { webView ->
        webViewRef = webView
      },
      )
    }

    if (activeVotingCycle?.status == "NOMINATIONS" && !account.banned && !isAdminVerified) {
      Button(
        onClick = onOpenNominations,
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF22D3EE),
          contentColor = Color(0xFF061622),
        ),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(bottom = 14.dp)
          .height(48.dp)
          .testTag("in_house_nominate_pill"),
      ) {
        Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (hasNominatedInCycle) "Nominated" else "Nominate Open",
          fontWeight = FontWeight.ExtraBold,
        )
      }
    } else if (activeVotingCycle?.status in listOf("OPEN", "CANCELLED") && !account.banned) {
      Button(
        onClick = onOpenVotingBallot,
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFFEAB954),
          contentColor = Color(0xFF10141F),
        ),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(bottom = 14.dp)
          .height(48.dp)
          .testTag("in_house_vote_pill"),
      ) {
        Icon(Icons.Rounded.HowToVote, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = when {
            activeVotingCycle?.status == "CANCELLED" -> "Voting Cancelled"
            isAdminVerified -> "Admin Voting Phone"
            else -> "Nominated • BiggGram NPC Vote"
          },
          fontWeight = FontWeight.ExtraBold,
        )
      }
    }

    errorMessage?.let { msg ->
      ErrorRecoveryOverlay(
        message = msg,
        actionLabel = stringResource(R.string.action_retry),
        onRetry = {
          errorMessage = null
          sceneReady = false
          crashRecoveryAttempts = 0
          webViewRecreateKey += 1
        },
      )
    }
  }
}

private fun buildSessionJson(
  account: UserAccount,
  isAdminVerified: Boolean,
  sessionId: String,
  isRtdbConfigured: Boolean,
  sessionSuperseded: Boolean,
  members: List<SeasonMember>,
): String {
  val membersArr = JSONArray()
  // Secret Admin Identity: Strip any ADMIN entries before sending house roster to WebView
  members.filter { it.role != PlayerRole.ADMIN.name }.forEach { m ->
    membersArr.put(
      JSONObject().apply {
        put("userId", m.userId)
        put("playerId", m.playerId)
        put("displayName", m.displayName)
        put("characterId", m.characterId)
        put("slotNumber", m.slotNumber)
        put("role", m.role)
        put("status", m.status)
        put("online", m.online)
      }
    )
  }
  return JSONObject().apply {
    put("verifiedByServer", true)
    put("uid", account.userId)
    put("playerId", account.playerId)
    put("displayName", account.displayName)
    put("characterId", account.characterId)
    put("role", if (isAdminVerified) "ADMIN" else "PLAYER")
    put("isAdminVerified", isAdminVerified)
    put("sessionId", sessionId)
    put("isRtdbConfigured", isRtdbConfigured)
    put("sessionSuperseded", sessionSuperseded)
    put("members", membersArr)
  }.toString()
}

private fun buildRemotePlayersJson(remotePlayers: Map<String, RemotePlayerState>): String {
  val arr = JSONArray()
  remotePlayers.values.forEach { p ->
    arr.put(
      JSONObject().apply {
        put("uid", p.uid)
        put("playerId", p.playerId)
        put("displayName", p.displayName)
        put("characterId", p.characterId)
        put("x", p.x)
        put("y", p.y)
        put("z", p.z)
        put("rotY", p.rotY)
        put("state", p.state)
        put("expression", p.expression)
        put("seatId", p.seatId ?: JSONObject.NULL)
        put("online", p.online)
      }
    )
  }
  return arr.toString()
}

private fun buildWorldStateJson(
  doorStates: Map<String, RemoteDoorState>,
  seatStates: Map<String, RemoteSeatState>,
  lightingMode: String?,
): String {
  val doorsObj = JSONObject()
  doorStates.forEach { (id, d) ->
    doorsObj.put(id, d.open)
  }
  val seatsObj = JSONObject()
  seatStates.forEach { (id, s) ->
    if (!s.occupiedByUid.isNullOrBlank()) {
      seatsObj.put(
        id,
        JSONObject().apply {
          put("uid", s.occupiedByUid)
          put("name", s.occupiedByName ?: "Housemate")
        },
      )
    }
  }
  return JSONObject().apply {
    put("doors", doorsObj)
    put("seats", seatsObj)
    if (!lightingMode.isNullOrBlank()) put("lightingMode", lightingMode)
  }.toString()
}

private fun buildPhase6Json(
  captainUid: String?,
  captainVipAccess: Boolean,
  vipUids: List<String>,
  activeTask: HouseTask?,
  collectedNodeIds: Set<String>,
): String {
  val taskObj = activeTask?.let { t ->
    JSONObject().apply {
      put("taskId", t.id)
      put("title", t.title)
      put("activityType", t.activityType)
      put("assignmentMode", t.assignmentMode)
      put("category", t.category)
      put("isCaptaincy", t.isCaptaincy)
      put("status", t.status)
      put("targetRoom", t.targetRoom)
      put("endTimeMillis", t.endTimeMillis)
    }
  }
  val collectedArr = JSONArray()
  collectedNodeIds.forEach { collectedArr.put(it) }
  val vipArr = JSONArray()
  vipUids.forEach { vipArr.put(it) }
  return JSONObject().apply {
    put("captainUid", captainUid ?: "")
    put("captainVipAccess", captainVipAccess)
    put("vipUids", vipArr)
    put("activeTask", taskObj ?: JSONObject.NULL)
    put("collectedNodeIds", collectedArr)
  }.toString()
}

private fun buildPhase7And8Json(
  energy: Long,
  hunger: Long,
  cookingCount: Int,
  readyMealsCount: Int,
  taskProgressPercent: Long,
  animPose: String,
  animNonce: Long,
): String {
  return JSONObject().apply {
    put("energy", energy)
    put("hunger", hunger)
    put("cookingCount", cookingCount)
    put("readyMealsCount", readyMealsCount)
    put("taskProgressPercent", taskProgressPercent)
    put("animPose", animPose)
    put("animNonce", animNonce)
  }.toString()
}

private fun buildPhase9AiJson(aiConfig: AiControlConfig?): String {
  val cfg = aiConfig ?: AiControlConfig()
  return JSONObject().apply {
    put("enabled", cfg.enabled)
    put("mode", cfg.mode)
    put("emergencyStopped", cfg.emergencyStopped)
  }.toString()
}

private fun buildPhase12To20Json(
  wardrobe: WardrobeAppearance,
  cameraPrefs: CameraPreferences,
  clock: HouseClockState,
  celebrations: List<com.example.multiplayer.HouseCelebrationEvent>,
  finale: FinaleCeremonyState,
  speakerPulseNonce: Long,
  cctvOnlineMap: Map<String, Boolean>,
  unreadNotificationCount: Int,
): String {
  val onlineObj = JSONObject()
  cctvOnlineMap.forEach { (k, v) -> onlineObj.put(k, v) }
  val activeCelebration = celebrations.firstOrNull { it.status == "ACTIVE" }
  return JSONObject().apply {
    put("wardrobe", wardrobe.toJsonObject())
    put(
      "cameraPrefs",
      JSONObject().apply {
        put("mode", cameraPrefs.mode)
        put("distance", cameraPrefs.distance.toDouble())
        put("sensitivity", cameraPrefs.sensitivity.toDouble())
        put("smoothness", cameraPrefs.smoothness.toDouble())
        put("freeCamAllowed", cameraPrefs.freeCamAllowed)
      },
    )
    put(
      "clock",
      JSONObject().apply {
        put("formatted", clock.formattedClock())
        put("dayNumber", clock.dayNumber)
        put("timeMode", clock.timeMode)
        put("weather", clock.weather)
        put("partyMode", clock.partyMode)
        put("emergencySirenActive", clock.emergencySirenActive)
      },
    )
    put("activeCelebration", activeCelebration?.title ?: JSONObject.NULL)
    put(
      "finale",
      JSONObject().apply {
        put("active", finale.active)
        put("winnerName", finale.winnerName)
        put("trophyTitle", finale.trophyTitle)
      },
    )
    put("speakerPulseNonce", speakerPulseNonce)
    put("cctvOnlineMap", onlineObj)
    put("unreadNotificationCount", unreadNotificationCount)
  }.toString()
}

@Composable
private fun FullScreenLoading(message: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF0A0E19))
      .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      CircularProgressIndicator(color = Color(0xFFEAB954), strokeWidth = 3.dp)
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = message,
        color = Color(0xFFCBD5E1),
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

@Composable
private fun ErrorRecoveryOverlay(
  message: String,
  actionLabel: String,
  onRetry: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xEE0C101B))
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = Color(0xFF141B2D),
      tonalElevation = 8.dp,
      modifier = Modifier.widthIn(max = 380.dp),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.WarningAmber,
          contentDescription = stringResource(R.string.error_title),
          tint = Color(0xFFEAB954),
          modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = stringResource(R.string.error_title),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFF94A3B8),
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
          onClick = onRetry,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFEAB954),
            contentColor = Color(0xFF10141F),
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .height(48.dp)
            .testTag("retry_scene_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.size(8.dp))
          Text(
            text = actionLabel,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}
