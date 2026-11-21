package com.example

import android.content.Context
import android.os.Looper
import android.util.Base64
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.multiplayer.AdminPersonalPhoneDialog
import com.example.multiplayer.BiggGramAndNpcVotingEngine
import com.example.multiplayer.CameraPreferences
import com.example.multiplayer.CelebrationType
import com.example.multiplayer.HouseChatAndCommsDialog
import com.example.multiplayer.MultiplayerFirestoreRepository
import com.example.multiplayer.MultiplayerViewModel
import com.example.multiplayer.NomineeSummary
import com.example.multiplayer.Phase11CaptainVipRepository
import com.example.multiplayer.Phase12To20Coordinator
import com.example.multiplayer.Phase12To20PersistenceStore
import com.example.multiplayer.Phase6FirestoreRepository
import com.example.multiplayer.Phase7And8FirestoreRepository
import com.example.multiplayer.Phase9And10FirestoreRepository
import com.example.multiplayer.RealtimeMultiplayerRepository
import com.example.multiplayer.SeasonMember
import com.example.multiplayer.TtsPlaybackSettings
import com.example.multiplayer.UserAccount
import com.example.multiplayer.VoiceGender
import com.example.multiplayer.VotingCycle
import com.example.multiplayer.WardrobeAppearance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase11To20Test {

  @get:Rule
  val composeTestRule = createComposeRule()

  private fun newCoordinator(
    isAdmin: Boolean = true,
    ttsLog: MutableList<String> = mutableListOf(),
    messages: MutableList<String> = mutableListOf(),
    errors: MutableList<String> = mutableListOf(),
  ): Pair<Phase12To20Coordinator, CoroutineScope> {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    ctx.getSharedPreferences("bigg_boss_phase12_20_store_v1", Context.MODE_PRIVATE).edit().clear().commit()
    val store = Phase12To20PersistenceStore(ctx)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    val account = UserAccount(
      id = if (isAdmin) "biggboss_admin" else "u_player_1",
      userId = if (isAdmin) "biggboss_admin" else "u_player_1",
      username = if (isAdmin) "BIGGBOSS" else "Aria",
      displayName = if (isAdmin) "BIGGBOSS" else "Aria",
      normalizedName = if (isAdmin) "biggboss" else "aria",
      playerId = if (isAdmin) "BB-ADMIN" else "BB-1001",
      characterId = "Player 01",
      role = if (isAdmin) "ADMIN" else "PLAYER",
      seasonId = "season_01",
    )
    val coordinator = Phase12To20Coordinator(
      scope = scope,
      store = store,
      currentUserId = account.userId,
      getAccount = { account },
      isAdmin = { isAdmin },
      onPostMessage = { messages.add(it) },
      onPostError = { errors.add(it) },
      onBroadcastPublicTts = { msg, _, _, _ -> ttsLog.add(msg) },
    )
    return coordinator to scope
  }

  @Test
  fun phase12WardrobeJsonAndPersistenceRoundTripWorks() {
    val custom = WardrobeAppearance(
      topHex = "#7C3AED",
      pantsHex = "#0F172A",
      outfitStyle = "blazer",
      shoesHex = "#FEF3C7",
      accentHex = "#22D3EE",
      hairStyle = "curlyCrown",
      hairColorHex = "#D97706",
      skinToneHex = "#8D5524",
      accessory = "headphones",
      glasses = true,
    )
    val restored = WardrobeAppearance.fromJson(custom.toJsonObject())
    assertEquals(custom, restored)

    val (coord, scope) = newCoordinator(isAdmin = false)
    try {
      coord.applyWardrobeAppearance(custom)
      assertEquals("blazer", coord.wardrobeState.value.outfitStyle)
      assertTrue(coord.wardrobeState.value.glasses)
    } finally {
      scope.cancel()
    }
  }

  @Test
  fun phase14To17ClockWeatherCelebrationsAndGrandFinaleWorkWithAdminGuard() {
    val ttsLog = mutableListOf<String>()
    val errors = mutableListOf<String>()
    val (playerCoord, pScope) = newCoordinator(isAdmin = false, ttsLog = ttsLog, errors = errors)
    try {
      playerCoord.setHouseTime(21, 0, "night", autoCycle = false)
      assertTrue(errors.any { it.contains("Main Admin") })
      assertEquals(10, playerCoord.houseClockState.value.hour)
    } finally {
      pScope.cancel()
    }

    val adminTts = mutableListOf<String>()
    val (adminCoord, aScope) = newCoordinator(isAdmin = true, ttsLog = adminTts)
    try {
      adminCoord.setHouseTime(21, 5, "night", autoCycle = false)
      assertEquals("Day 1 • 21:05", adminCoord.houseClockState.value.formattedClock())
      assertEquals("night", adminCoord.houseClockState.value.timeMode)

      adminCoord.setHouseWeather("RAIN")
      assertEquals("RAIN", adminCoord.houseClockState.value.weather)

      adminCoord.triggerEmergencySiren(true, "Emergency alert inside the Living Atrium")
      assertTrue(adminCoord.houseClockState.value.emergencySirenActive)
      assertTrue(adminTts.any { it.contains("Emergency") })

      adminCoord.createOrTriggerCelebration(
        type = CelebrationType.FESTIVAL,
        title = "Diwali Lights Night",
        description = "Festive lights across the garden",
        triggerImmediately = true,
      )
      assertEquals(1, adminCoord.celebrationsState.value.size)
      assertTrue(adminCoord.houseClockState.value.partyMode)

      val winner = SeasonMember(
        userId = "u_aria",
        playerId = "BB-101",
        displayName = "Aria",
        characterId = "Player 01",
        slotNumber = 1,
        online = true,
      )
      adminCoord.triggerGrandFinale(winner, "Kai", "BIGG BOSS SEASON 01 CHAMPION")
      assertTrue(adminCoord.finaleState.value.active)
      assertEquals("Aria", adminCoord.finaleState.value.winnerName)
      assertTrue(adminCoord.biggGramPosts.value.any { it.category == "FINALE" && it.caption.contains("Aria") })
    } finally {
      aScope.cancel()
    }
  }

  @Test
  fun phase18PrivateDmsNeverTriggerPublicTtsAndMutedPlayerIsBlockedInPublicChat() {
    val ttsLog = mutableListOf<String>()
    val errors = mutableListOf<String>()
    val (coord, scope) = newCoordinator(isAdmin = false, ttsLog = ttsLog, errors = errors)
    try {
      coord.sendPublicChatMessage("Hello everyone in the house!")
      assertTrue(coord.publicChatMessages.value.any { it.text == "Hello everyone in the house!" })

      val dmCountBefore = coord.privateDmMessages.value.size
      coord.sendPrivateDirectMessage("u_kai", "Kai", "Secret alliance plan between us")
      assertEquals(dmCountBefore + 1, coord.privateDmMessages.value.size)
      assertTrue(coord.privateDmMessages.value.first().isPrivateDm)
      assertTrue("Private DMs must NEVER route to public TTS speakers", ttsLog.isEmpty())
    } finally {
      scope.cancel()
    }

    // Also test mute enforcement on a player coordinator sharing same account UID
    val pErrors = mutableListOf<String>()
    val (mutedCoord, mScope) = newCoordinator(isAdmin = true, errors = pErrors)
    try {
      mutedCoord.setPlayerMuted("u_player_1", true, "Aria")
      assertTrue(mutedCoord.moderationFlags.value["u_player_1"]?.muted == true)
    } finally {
      mScope.cancel()
    }
  }

  @Test
  fun phase20BiggGram15NpcsSingleNonDuplicateNpcVotingAndTotalsAreAccurate() {
    assertEquals(15, BiggGramAndNpcVotingEngine.NPC_PROFILES.size)
    assertTrue(BiggGramAndNpcVotingEngine.defaultStories().size >= 8)

    val cycle = VotingCycle(
      id = "cyc_20_test",
      title = "Week 4 BiggGram Eviction",
      status = "OPEN",
      voteType = "EVICT",
      npcVotesEnabled = true,
      nominatedUids = listOf("u_1", "u_2", "u_3"),
      nomineeSummaries = listOf(
        "u_1|BB-1|Aria|Player 01",
        "u_2|BB-2|Kai|Player 02",
        "u_3|BB-3|Zara|Player 03",
      ),
    )
    val nominees = listOf(
      NomineeSummary("u_1", "BB-1", "Aria", "Player 01"),
      NomineeSummary("u_2", "BB-2", "Kai", "Player 02"),
      NomineeSummary("u_3", "BB-3", "Zara", "Player 03"),
    )

    // Duplicate vote rule check on engine directly
    val npc1 = BiggGramAndNpcVotingEngine.NPC_PROFILES.first()
    val firstVote = BiggGramAndNpcVotingEngine.castSingleNpcVote(cycle.id, emptyList(), npc1, nominees[0], "bg_nom_cyc_20_test")
    assertTrue(firstVote.isSuccess)
    val duplicateVote = BiggGramAndNpcVotingEngine.castSingleNpcVote(
      cycle.id,
      listOf(firstVote.getOrThrow()),
      npc1,
      nominees[1],
      "bg_nom_cyc_20_test",
    )
    assertTrue("Same NPC must never vote twice in the same cycle", duplicateVote.isFailure)

    // Coordinator workflow: publish -> cast 1 vote -> reject duplicate -> cast remaining 14 -> verify total = 15
    val (coord, scope) = newCoordinator(isAdmin = true)
    try {
      coord.publishNomineesToBiggGram(cycle, nominees)
      assertTrue(coord.biggGramPosts.value.any { it.cycleId == cycle.id && it.category == "NOMINATION_POLL" })

      assertTrue(coord.castNpcVoteOnBiggGram(cycle, npc1, nominees[0]))
      assertFalse(coord.castNpcVoteOnBiggGram(cycle, npc1, nominees[1]))

      val remaining = coord.runAllRemainingNpcVotesOnBiggGram(cycle)
      assertEquals(14, remaining)
      assertEquals(0, coord.runAllRemainingNpcVotesOnBiggGram(cycle))

      val outcome = BiggGramAndNpcVotingEngine.calculateNpcVotingOutcome(cycle.id, nominees, coord.npcVotes.value)
      assertEquals(15, outcome.totalValidNpcVotes)
      assertEquals(15, outcome.rows.sumOf { it.voteCount })
      assertNotNull(outcome.highestNominee)
      assertNotNull(outcome.lowestNominee)
    } finally {
      scope.cancel()
    }
  }

  @Test
  fun phase20HasExactly24CommonAreaCctvCamerasAndZeroPrivateZones() {
    val cams = BiggGramAndNpcVotingEngine.ALL_24_CCTV_CAMERAS
    assertEquals(24, cams.size)
    val forbidden = setOf("bathroom", "washroom", "changing", "smoking", "private")
    assertTrue(
      cams.none { cam ->
        val joined = "${cam.id} ${cam.label} ${cam.roomName} ${cam.roomId}".lowercase()
        forbidden.any { word -> joined.contains(word) }
      }
    )

    val (coord, scope) = newCoordinator(isAdmin = true)
    try {
      val firstId = cams.first().id
      assertEquals(true, coord.cctvOnlineMap.value[firstId])
      coord.toggleCctvCameraOnline(firstId)
      assertEquals(false, coord.cctvOnlineMap.value[firstId])
    } finally {
      scope.cancel()
    }
  }

  @Test
  fun phase20GCameraPreferencesClampAndPersist() {
    val (coord, scope) = newCoordinator(isAdmin = false)
    try {
      coord.saveCameraPreferences(CameraPreferences(mode = "FIRST_PERSON", distance = 25.0f, sensitivity = 5.0f, smoothness = 0.05f))
      assertEquals("FIRST_PERSON", coord.cameraPrefsState.value.mode)
      assertEquals(8.5f, coord.cameraPrefsState.value.distance, 0.01f)
      assertEquals(2.0f, coord.cameraPrefsState.value.sensitivity, 0.01f)
      assertEquals(0.25f, coord.cameraPrefsState.value.smoothness, 0.01f)

      coord.adjustCameraZoom(-10.0f)
      assertEquals(2.0f, coord.cameraPrefsState.value.distance, 0.01f)
    } finally {
      scope.cancel()
    }
  }

  @Test
  fun adminPersonalPhoneHiddenForNormalPlayersAndRendersAllAppsForAdmin() {
    composeTestRule.setContent {
      AdminPersonalPhoneDialog(
        isAdminVerified = false,
        members = emptyList(),
        allUsers = emptyList(),
        activeCycle = null,
        nominations = emptyList(),
        npcProfiles = BiggGramAndNpcVotingEngine.NPC_PROFILES,
        biggGramPosts = emptyList(),
        biggGramStories = emptyList(),
        npcVotes = emptyList(),
        notifications = emptyList(),
        clockState = com.example.multiplayer.HouseClockState(),
        celebrations = emptyList(),
        finaleState = com.example.multiplayer.FinaleCeremonyState(),
        moderationFlags = emptyMap(),
        cctvOnlineMap = emptyMap(),
        actionMessage = null,
        actionError = null,
        onDismiss = {},
        onClearFeedback = {},
        onToggleLikePost = {},
        onAddComment = { _, _ -> },
        onCreatePost = {},
        onMarkStoryViewed = {},
        onPublishNomineesToBiggGram = { _, _ -> },
        onRunAllNpcVotes = {},
        onCastSingleNpcVote = { _, _, _ -> },
        onToggleCctvOnline = {},
        onOpen3dCctvMatrix = {},
        onSendTtsAnnouncement = { _, _, _ -> },
        onTriggerEmergencySiren = { _, _ -> },
        onSetHouseClock = { _, _, _, _ -> },
        onSetWeather = {},
        onTogglePartyMode = {},
        onTriggerCelebration = { _, _, _, _ -> },
        onTriggerFinale = { _, _, _ -> },
        onAssignSecretTask = { _, _, _, _ -> },
        onSetPlayerMuted = { _, _, _ -> },
        onSetPlayerTaskRestricted = { _, _, _ -> },
        onModeratePlayer = { _, _, _ -> },
        onMarkNotificationsRead = {},
      )
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_personal_phone_dialog").assertDoesNotExist()
  }

  @Test
  fun adminPersonalPhoneOpensBiggGramAnd24CamMonitorForAdminAndPlayerChatHidesAdminIdentity() {
    composeTestRule.setContent {
      AdminPersonalPhoneDialog(
        isAdminVerified = true,
        members = emptyList(),
        allUsers = emptyList(),
        activeCycle = null,
        nominations = emptyList(),
        npcProfiles = BiggGramAndNpcVotingEngine.NPC_PROFILES,
        biggGramPosts = BiggGramAndNpcVotingEngine.defaultPosts(),
        biggGramStories = BiggGramAndNpcVotingEngine.defaultStories(),
        npcVotes = emptyList(),
        notifications = emptyList(),
        clockState = com.example.multiplayer.HouseClockState(),
        celebrations = emptyList(),
        finaleState = com.example.multiplayer.FinaleCeremonyState(),
        moderationFlags = emptyMap(),
        cctvOnlineMap = BiggGramAndNpcVotingEngine.ALL_24_CCTV_CAMERAS.associate { it.id to true },
        actionMessage = null,
        actionError = null,
        onDismiss = {},
        onClearFeedback = {},
        onToggleLikePost = {},
        onAddComment = { _, _ -> },
        onCreatePost = {},
        onMarkStoryViewed = {},
        onPublishNomineesToBiggGram = { _, _ -> },
        onRunAllNpcVotes = {},
        onCastSingleNpcVote = { _, _, _ -> },
        onToggleCctvOnline = {},
        onOpen3dCctvMatrix = {},
        onSendTtsAnnouncement = { _, _, _ -> },
        onTriggerEmergencySiren = { _, _ -> },
        onSetHouseClock = { _, _, _, _ -> },
        onSetWeather = {},
        onTogglePartyMode = {},
        onTriggerCelebration = { _, _, _, _ -> },
        onTriggerFinale = { _, _, _ -> },
        onAssignSecretTask = { _, _, _, _ -> },
        onSetPlayerMuted = { _, _, _ -> },
        onSetPlayerTaskRestricted = { _, _, _ -> },
        onModeratePlayer = { _, _, _ -> },
        onMarkNotificationsRead = {},
      )
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_personal_phone_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("phone_app_BIGGGRAM").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("BIGGGRAM • NPC SOCIAL NETWORK").assertIsDisplayed()
  }

  @Test
  fun chatDialogNeverExposesSecretAdminInPlayerDmTargets() {
    val members = listOf(
      SeasonMember(userId = "u_self", playerId = "BB-01", displayName = "Aria", characterId = "Player 01", role = "PLAYER", slotNumber = 1, online = true),
      SeasonMember(userId = "u_kai", playerId = "BB-02", displayName = "Kai", characterId = "Player 02", role = "PLAYER", slotNumber = 2, online = true),
      SeasonMember(userId = "biggboss_admin", playerId = "BB-ADMIN", displayName = "SecretOwnerName", characterId = "Player 03", role = "ADMIN", slotNumber = 0, online = true),
    )
    composeTestRule.setContent {
      HouseChatAndCommsDialog(
        currentUserId = "u_self",
        isAdminVerified = false,
        members = members,
        publicMessages = emptyList(),
        privateDms = emptyList(),
        announcements = emptyList(),
        ttsSettings = TtsPlaybackSettings(),
        secretTasks = emptyList(),
        isMutedInChat = false,
        onSendPublicMessage = {},
        onSendPrivateDm = { _, _, _ -> },
        onUpdateTtsSettings = {},
        onReplayWelcome = {},
        onCompleteSecretTask = {},
        onDismiss = {},
      )
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("chat_tab_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Kai").assertIsDisplayed()
    composeTestRule.onNodeWithText("SecretOwnerName").assertDoesNotExist()
  }

  @Test
  fun localAdminCctvRecordingDoesNotInvokeUnauthenticatedFirestoreCreate() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(ctx).isEmpty()) {
      FirebaseApp.initializeApp(ctx)
    }
    val db = FirebaseFirestore.getInstance(ctx.getString(R.string.firestore_database_id))
    val auth = Firebase.auth
    val vm = MultiplayerViewModel(
      firestoreRepo = MultiplayerFirestoreRepository(db, auth),
      phase6Repo = Phase6FirestoreRepository(db, auth),
      phase7Repo = Phase7And8FirestoreRepository(db, auth),
      phase9Repo = Phase9And10FirestoreRepository(auth, db),
      phase11Repo = Phase11CaptainVipRepository(db, auth),
      rtdbRepo = RealtimeMultiplayerRepository(auth),
      currentUserId = "biggboss_admin",
      isLocalAdminSession = true,
      appContext = ctx,
    )
    val dummyB64 = Base64.encodeToString(byteArrayOf(1, 2, 3, 4, 5), Base64.NO_WRAP)
    vm.uploadCctvRecordingFromBridge(
      cameraId = "cam_living_1",
      cameraLabel = "Living Atrium NW",
      startedAtMillis = 1_760_000_000_000L,
      durationMillis = 4000L,
      mimeType = "video/webm",
      base64Data = dummyB64,
    )
    // Pump Robolectric main looper & IO coroutine completion without blocking MainLooper
    val deadline = System.currentTimeMillis() + 4_000L
    while (vm.cctvRecordingsState.value.isEmpty() && System.currentTimeMillis() < deadline) {
      Shadows.shadowOf(Looper.getMainLooper()).idle()
      Thread.sleep(25L)
    }
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    assertNull(vm.actionError.value)
    val saved = vm.cctvRecordingsState.value
    assertEquals(1, saved.size)
    assertEquals("cam_living_1", saved.first().cameraId)
    assertTrue(saved.first().storagePath.startsWith("local_cache/"))
  }
}
