package com.example.multiplayer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class Phase12To20Coordinator(
  private val scope: CoroutineScope,
  private val store: Phase12To20PersistenceStore,
  private val currentUserId: String,
  private val getAccount: () -> UserAccount?,
  private val isAdmin: () -> Boolean,
  private val onPostMessage: (String) -> Unit,
  private val onPostError: (String) -> Unit,
  private val onBroadcastPublicTts: (message: String, presetId: String?, langTag: String, gender: VoiceGender) -> Unit,
) {
  private val _wardrobeState = MutableStateFlow(store.loadWardrobe(currentUserId))
  val wardrobeState: StateFlow<WardrobeAppearance> = _wardrobeState.asStateFlow()

  private val _cameraPrefsState = MutableStateFlow(store.loadCameraPrefs(currentUserId))
  val cameraPrefsState: StateFlow<CameraPreferences> = _cameraPrefsState.asStateFlow()

  private val _ttsSettingsState = MutableStateFlow(store.loadTtsSettings())
  val ttsSettingsState: StateFlow<TtsPlaybackSettings> = _ttsSettingsState.asStateFlow()

  private val _houseClockState = MutableStateFlow(HouseClockState())
  val houseClockState: StateFlow<HouseClockState> = _houseClockState.asStateFlow()

  private val _celebrationsState = MutableStateFlow(store.loadCelebrations())
  val celebrationsState: StateFlow<List<HouseCelebrationEvent>> = _celebrationsState.asStateFlow()

  private val _finaleState = MutableStateFlow(store.loadFinaleState())
  val finaleState: StateFlow<FinaleCeremonyState> = _finaleState.asStateFlow()

  private val _publicChatMessages = MutableStateFlow<List<HouseChatMessage>>(
    listOf(
      HouseChatMessage(
        id = "chat_welcome_1",
        senderUid = "system_biggboss",
        senderName = "BIGG BOSS",
        senderRole = "ADMIN",
        text = "Welcome housemates to BIGG BOSS – THE HOUSE! Be respectful in public house chat.",
        timestampMillis = System.currentTimeMillis() - 60_000L,
      )
    )
  )
  val publicChatMessages: StateFlow<List<HouseChatMessage>> = _publicChatMessages.asStateFlow()

  private val _privateDmMessages = MutableStateFlow<List<HouseChatMessage>>(emptyList())
  val privateDmMessages: StateFlow<List<HouseChatMessage>> = _privateDmMessages.asStateFlow()

  private val _moderationFlags = MutableStateFlow<Map<String, PlayerModerationFlags>>(emptyMap())
  val moderationFlags: StateFlow<Map<String, PlayerModerationFlags>> = _moderationFlags.asStateFlow()

  private val _secretTasks = MutableStateFlow<List<SecretTaskAssignment>>(emptyList())
  val secretTasks: StateFlow<List<SecretTaskAssignment>> = _secretTasks.asStateFlow()

  private val _biggGramNpcProfiles = MutableStateFlow(BiggGramAndNpcVotingEngine.NPC_PROFILES)
  val biggGramNpcProfiles: StateFlow<List<BiggGramNpcProfile>> = _biggGramNpcProfiles.asStateFlow()

  private val _biggGramPosts = MutableStateFlow(store.loadBiggGramPosts())
  val biggGramPosts: StateFlow<List<BiggGramPost>> = _biggGramPosts.asStateFlow()

  private val _biggGramStories = MutableStateFlow(BiggGramAndNpcVotingEngine.defaultStories())
  val biggGramStories: StateFlow<List<BiggGramStory>> = _biggGramStories.asStateFlow()

  private val _npcVotes = MutableStateFlow(store.loadNpcVotes())
  val npcVotes: StateFlow<List<NpcVoteRecord>> = _npcVotes.asStateFlow()

  private val _adminPhoneNotifications = MutableStateFlow<List<AdminPhoneNotification>>(
    listOf(
      AdminPhoneNotification(
        id = "notif_boot",
        title = "Admin Control Phone Ready",
        body = "Secret Control Room identity active. BiggGram & 24 CCTV feeds online.",
        category = "SYSTEM",
      )
    )
  )
  val adminPhoneNotifications: StateFlow<List<AdminPhoneNotification>> = _adminPhoneNotifications.asStateFlow()

  private val _cctvOnlineMap = MutableStateFlow(store.loadCctvOnlineMap())
  val cctvOnlineMap: StateFlow<Map<String, Boolean>> = _cctvOnlineMap.asStateFlow()

  private val _speakerPulseNonce = MutableStateFlow(0L)
  val speakerPulseNonce: StateFlow<Long> = _speakerPulseNonce.asStateFlow()

  init {
    // Phase 14: Automatic synced house clock & smooth Day/Dusk/Night cycle when enabled
    scope.launch {
      while (true) {
        delay(20_000L)
        val cur = _houseClockState.value
        if (cur.autoCycleEnabled) {
          var nextMin = cur.minute + 15
          var nextHour = cur.hour
          var nextDay = cur.dayNumber
          if (nextMin >= 60) {
            nextMin = 0
            nextHour += 1
          }
          if (nextHour >= 24) {
            nextHour = 0
            nextDay += 1
          }
          val nextMode = when (nextHour) {
            in 6..16 -> "day"
            in 17..19 -> "dusk"
            else -> "night"
          }
          _houseClockState.value = cur.copy(
            hour = nextHour,
            minute = nextMin,
            dayNumber = nextDay,
            timeMode = nextMode,
          )
        }
      }
    }
  }

  // --- PHASE 12: WARDROBE ---

  fun applyWardrobeAppearance(appearance: WardrobeAppearance) {
    _wardrobeState.value = appearance
    store.saveWardrobe(currentUserId, appearance)
    onPostMessage("Custom wardrobe outfit saved & synced to 3D character.")
  }

  // --- PHASE 20G: PLAYER CAMERA PREFERENCES ---

  fun saveCameraPreferences(prefs: CameraPreferences) {
    val clean = prefs.copy(
      distance = prefs.distance.coerceIn(2.0f, 8.5f),
      sensitivity = prefs.sensitivity.coerceIn(0.4f, 2.0f),
      smoothness = prefs.smoothness.coerceIn(0.25f, 1.0f),
    )
    _cameraPrefsState.value = clean
    store.saveCameraPrefs(currentUserId, clean)
    onPostMessage("Camera mode set to ${clean.mode.replace('_', ' ')}.")
  }

  fun setCameraMode(mode: String) {
    val cur = _cameraPrefsState.value
    if (mode == "FREE" && !cur.freeCamAllowed && !isAdmin()) {
      onPostError("Free Camera is currently locked by Admin.")
      return
    }
    saveCameraPreferences(cur.copy(mode = mode))
  }

  fun adjustCameraZoom(delta: Float) {
    val cur = _cameraPrefsState.value
    saveCameraPreferences(cur.copy(distance = (cur.distance + delta).coerceIn(2.0f, 8.5f)))
  }

  // --- PHASE 14 & 17: HOUSE CLOCK, WEATHER, PARTY MODE & EMERGENCY SIREN ---

  fun setHouseTime(hour: Int, minute: Int, timeMode: String, autoCycle: Boolean) {
    if (!isAdmin()) {
      onPostError("Only Main Admin can change the house clock.")
      return
    }
    val cur = _houseClockState.value
    _houseClockState.value = cur.copy(
      hour = hour.coerceIn(0, 23),
      minute = minute.coerceIn(0, 59),
      timeMode = timeMode,
      autoCycleEnabled = autoCycle,
    )
    pushAdminNotification("House Clock Updated", "${_houseClockState.value.formattedClock()} ($timeMode)", "CLOCK")
    onPostMessage("House clock synchronized to ${_houseClockState.value.formattedClock()}.")
  }

  fun setHouseWeather(weather: String) {
    if (!isAdmin()) {
      onPostError("Only Main Admin can change weather atmosphere.")
      return
    }
    _houseClockState.value = _houseClockState.value.copy(weather = weather)
    onPostMessage("House atmosphere set to $weather.")
  }

  fun togglePartyMode(enabled: Boolean) {
    if (!isAdmin()) {
      onPostError("Only Main Admin can toggle Party Mode.")
      return
    }
    _houseClockState.value = _houseClockState.value.copy(partyMode = enabled)
    if (enabled) {
      onBroadcastPublicTts("पार्टी मोड शुरू हो चुका है! सभी घरवाले लिविंग एरिया में जश्न मनाएं।", "party_mode", "hi-IN", VoiceGender.FEMALE)
      _speakerPulseNonce.value = System.currentTimeMillis()
    }
    onPostMessage(if (enabled) "Party & Dance lighting boost ACTIVATED." else "Party lighting turned off.")
  }

  fun triggerEmergencySiren(active: Boolean, alertMessage: String = "") {
    if (!isAdmin()) {
      onPostError("Only Main Admin can trigger Emergency Siren.")
      return
    }
    _houseClockState.value = _houseClockState.value.copy(emergencySirenActive = active)
    if (active) {
      val msg = alertMessage.trim().ifBlank {
        "सावधान! बिग बॉस की ओर से इमरजेंसी अलर्ट। सभी घरवाले तुरंत मुख्य हॉल में एकत्रित हों।"
      }
      onBroadcastPublicTts(msg, "emergency_siren", "hi-IN", VoiceGender.MALE)
      _speakerPulseNonce.value = System.currentTimeMillis()
      pushAdminNotification("EMERGENCY SIREN", msg, "ALERT")
    }
    onPostMessage(if (active) "Emergency House Siren ACTIVATED." else "Emergency Siren cleared.")
  }

  // --- PHASE 15: HOUSE CELEBRATIONS & SPECIAL EVENTS ---

  fun createOrTriggerCelebration(
    type: CelebrationType,
    title: String,
    description: String,
    triggerImmediately: Boolean,
  ) {
    if (!isAdmin()) {
      onPostError("Only Main Admin can trigger celebrations.")
      return
    }
    val cleanTitle = title.trim().ifBlank { type.label }.take(70)
    val cleanDesc = description.trim().ifBlank { "Special ${type.label} inside BIGG BOSS – THE HOUSE!" }.take(220)
    val now = System.currentTimeMillis()
    val ev = HouseCelebrationEvent(
      id = "cel_${UUID.randomUUID().toString().take(8)}",
      type = type.name,
      title = cleanTitle,
      description = cleanDesc,
      scheduledTimeMillis = now,
      status = if (triggerImmediately) "ACTIVE" else "SCHEDULED",
      triggeredBy = LocalAdminAuthManager.ADMIN_USERNAME,
      createdAtMillis = now,
    )
    val updated = listOf(ev) + _celebrationsState.value
    _celebrationsState.value = updated
    store.saveCelebrations(updated)
    if (triggerImmediately) {
      _houseClockState.value = _houseClockState.value.copy(partyMode = true)
      onBroadcastPublicTts("बधाई हो घरवालों! $cleanTitle — $cleanDesc", "celebration_${type.name}", "hi-IN", VoiceGender.FEMALE)
      _speakerPulseNonce.value = now
      pushAdminNotification("Celebration Active", cleanTitle, "EVENT")
    }
    onPostMessage(if (triggerImmediately) "Celebration \"$cleanTitle\" is LIVE in the House!" else "Scheduled \"$cleanTitle\".")
  }

  fun completeCelebration(eventId: String) {
    if (!isAdmin()) return
    val updated = _celebrationsState.value.map {
      if (it.id == eventId) it.copy(status = "COMPLETED") else it
    }
    _celebrationsState.value = updated
    store.saveCelebrations(updated)
    _houseClockState.value = _houseClockState.value.copy(partyMode = false)
    onPostMessage("Celebration marked completed.")
  }

  // --- PHASE 17: GRAND FINALE & SECRET TASKS ---

  fun triggerGrandFinale(winner: SeasonMember, runnerUpName: String, trophyTitle: String) {
    if (!isAdmin()) {
      onPostError("Only Main Admin can declare the Grand Finale winner.")
      return
    }
    val now = System.currentTimeMillis()
    val title = trophyTitle.trim().ifBlank { "BIGG BOSS – THE HOUSE SEASON 01 CHAMPION" }
    val state = FinaleCeremonyState(
      active = true,
      winnerUid = winner.userId,
      winnerName = winner.displayName,
      runnerUpName = runnerUpName.trim().ifBlank { "House Finalist" },
      trophyTitle = title,
      declaredAtMillis = now,
    )
    _finaleState.value = state
    store.saveFinaleState(state)
    _houseClockState.value = _houseClockState.value.copy(partyMode = true)

    val finalePost = BiggGramPost(
      id = "bg_finale_$now",
      authorHandle = "@biggboss.official",
      authorName = "GRAND FINALE • OFFICIAL RESULT",
      authorAvatarHex = "#EAB954",
      caption = "🏆 CONGRATULATIONS ${winner.displayName}! Winner of $title!",
      category = "FINALE",
      likes = 500,
      likedByAdmin = true,
      comments = listOf(
        BiggGramComment("cf_1", "@aria.strategist", "Aria", "What an unforgettable season! Well deserved! 🏆", now),
        BiggGramComment("cf_2", "@nova.showrunner", "Nova", "Fireworks across the Living Atrium!!", now + 1L),
      ),
      timestampMillis = now,
    )
    val nextPosts = listOf(finalePost) + _biggGramPosts.value
    _biggGramPosts.value = nextPosts
    store.saveBiggGramPosts(nextPosts)

    onBroadcastPublicTts(
      "ग्रैंड फिनाले परिणाम! BIGG BOSS – THE HOUSE के विजेता हैं ${winner.displayName}!",
      "grand_finale",
      "hi-IN",
      VoiceGender.FEMALE,
    )
    _speakerPulseNonce.value = now
    pushAdminNotification("Grand Finale Winner", "${winner.displayName} crowned Champion!", "FINALE")
    onPostMessage("${winner.displayName} crowned Grand Finale Champion!")
  }

  fun clearFinaleCeremony() {
    if (!isAdmin()) return
    val cleared = FinaleCeremonyState()
    _finaleState.value = cleared
    store.saveFinaleState(cleared)
    onPostMessage("Finale ceremony banner dismissed.")
  }

  fun assignSecretTask(target: SeasonMember, title: String, instructions: String, rewardPoints: Long) {
    if (!isAdmin()) {
      onPostError("Only Main Admin can assign Secret Tasks.")
      return
    }
    val task = SecretTaskAssignment(
      id = "sec_${UUID.randomUUID().toString().take(8)}",
      targetUid = target.userId,
      targetName = target.displayName,
      title = title.trim().ifBlank { "Secret House Mission" },
      secretInstructions = instructions.trim().ifBlank { "Complete quietly without alerting other housemates." },
      rewardPoints = rewardPoints.coerceIn(10L, 500L),
    )
    _secretTasks.value = listOf(task) + _secretTasks.value
    pushAdminNotification("Secret Task Sent", "Assigned \"${task.title}\" to ${target.displayName}", "SECRET")
    onPostMessage("Secret Task assigned privately to ${target.displayName}.")
  }

  fun completeSecretTask(taskId: String) {
    val cur = _secretTasks.value.firstOrNull { it.id == taskId } ?: return
    if (!isAdmin() && cur.targetUid != currentUserId) return
    _secretTasks.value = _secretTasks.value.map {
      if (it.id == taskId) it.copy(status = "COMPLETED") else it
    }
    onPostMessage("Secret Task \"${cur.title}\" completed! (+${cur.rewardPoints} pts)")
  }

  // --- PHASE 18: PUBLIC CHAT, PRIVATE 1:1 DMs & TTS PLAYBACK SETTINGS ---

  fun updateTtsSettings(next: TtsPlaybackSettings) {
    _ttsSettingsState.value = next
    store.saveTtsSettings(next)
  }

  fun markWelcomePlayed() {
    val cur = _ttsSettingsState.value
    if (!cur.welcomePlayed) {
      updateTtsSettings(cur.copy(welcomePlayed = true))
    }
    _speakerPulseNonce.value = System.currentTimeMillis()
  }

  fun triggerSpeakerPulse() {
    _speakerPulseNonce.value = System.currentTimeMillis()
  }

  fun sendPublicChatMessage(text: String) {
    val account = getAccount() ?: return
    val flags = _moderationFlags.value[account.userId]
    if (flags?.muted == true && !isAdmin()) {
      onPostError("You are muted by the Main Admin and cannot post in public chat.")
      return
    }
    val clean = text.trim().take(280)
    if (clean.isBlank()) return
    val msg = HouseChatMessage(
      id = "pub_${UUID.randomUUID().toString().take(8)}",
      senderUid = account.userId,
      senderName = if (isAdmin()) "BIGG BOSS" else account.displayName,
      senderRole = if (isAdmin()) "ADMIN" else account.role,
      recipientUid = null,
      text = clean,
      timestampMillis = System.currentTimeMillis(),
    )
    _publicChatMessages.value = (listOf(msg) + _publicChatMessages.value).take(120)
  }

  fun sendPrivateDirectMessage(recipientUid: String, recipientName: String, text: String) {
    val account = getAccount() ?: return
    val clean = text.trim().take(280)
    if (clean.isBlank() || recipientUid.isBlank()) return
    // Private DMs are stored strictly in privateDmMessages and NEVER routed to TTS speakers.
    val dm = HouseChatMessage(
      id = "dm_${UUID.randomUUID().toString().take(8)}",
      senderUid = account.userId,
      senderName = if (isAdmin()) "BIGG BOSS (Private)" else account.displayName,
      senderRole = if (isAdmin()) "ADMIN" else account.role,
      recipientUid = recipientUid,
      recipientName = recipientName,
      text = clean,
      timestampMillis = System.currentTimeMillis(),
    )
    _privateDmMessages.value = (listOf(dm) + _privateDmMessages.value).take(120)
  }

  // --- PLAYER MANAGEMENT FLAGS (MUTE / TASK RESTRICTION / CURRENT ROOM) ---

  fun setPlayerMuted(userId: String, muted: Boolean, displayName: String) {
    if (!isAdmin()) {
      onPostError("Only Main Admin can mute/unmute players.")
      return
    }
    val cur = _moderationFlags.value.toMutableMap()
    val existing = cur[userId] ?: PlayerModerationFlags(userId = userId)
    cur[userId] = existing.copy(muted = muted)
    _moderationFlags.value = cur
    pushAdminNotification("Moderation Update", "$displayName ${if (muted) "muted" else "unmuted"}", "MODERATION")
    onPostMessage("$displayName ${if (muted) "muted in chat" else "unmuted"}.")
  }

  fun setPlayerTaskRestricted(userId: String, restricted: Boolean, displayName: String) {
    if (!isAdmin()) {
      onPostError("Only Main Admin can restrict player tasks.")
      return
    }
    val cur = _moderationFlags.value.toMutableMap()
    val existing = cur[userId] ?: PlayerModerationFlags(userId = userId)
    cur[userId] = existing.copy(restrictedFromTasks = restricted)
    _moderationFlags.value = cur
    onPostMessage("$displayName task restriction set to $restricted.")
  }

  fun updatePlayerRoom(userId: String, room: String) {
    if (userId.isBlank() || room.isBlank()) return
    val cur = _moderationFlags.value.toMutableMap()
    val existing = cur[userId] ?: PlayerModerationFlags(userId = userId)
    if (existing.currentRoom != room) {
      cur[userId] = existing.copy(currentRoom = room)
      _moderationFlags.value = cur
    }
  }

  // --- PHASE 20: ADMIN PHONE, BIGGGRAM (NPC-ONLY), NPC VOTING & 24 CCTV CAMERAS ---

  fun publishNomineesToBiggGram(cycle: VotingCycle, nominees: List<NomineeSummary>) {
    if (!isAdmin()) {
      onPostError("Only Main Admin can publish nominees to BiggGram.")
      return
    }
    if (nominees.isEmpty()) {
      onPostError("Select at least one nominee to publish on BiggGram.")
      return
    }
    val post = BiggGramAndNpcVotingEngine.publishNominationPollPost(cycle, nominees)
    val nextPosts = listOf(post) + _biggGramPosts.value.filterNot { it.id == post.id }
    _biggGramPosts.value = nextPosts
    store.saveBiggGramPosts(nextPosts)
    pushAdminNotification("Published to BiggGram", "${nominees.size} nominees posted for ${cycle.title}", "BIGGGRAM")
    onPostMessage("Nomination list published to BiggGram (${nominees.size} nominees).")
  }

  fun toggleLikeBiggGramPost(postId: String) {
    if (!isAdmin()) return
    val next = _biggGramPosts.value.map { post ->
      if (post.id != postId) post else {
        val nextLiked = !post.likedByAdmin
        post.copy(
          likedByAdmin = nextLiked,
          likes = if (nextLiked) post.likes + 1 else (post.likes - 1).coerceAtLeast(0),
        )
      }
    }
    _biggGramPosts.value = next
    store.saveBiggGramPosts(next)
  }

  fun addBiggGramPostComment(postId: String, handle: String, authorName: String, text: String) {
    if (!isAdmin()) return
    val clean = text.trim().take(200)
    if (clean.isBlank()) return
    val comment = BiggGramComment(
      id = BiggGramAndNpcVotingEngine.newCommentId(),
      authorHandle = handle,
      authorName = authorName,
      text = clean,
    )
    val next = _biggGramPosts.value.map { post ->
      if (post.id == postId) post.copy(comments = post.comments + comment) else post
    }
    _biggGramPosts.value = next
    store.saveBiggGramPosts(next)
  }

  fun createAdminBiggGramPost(caption: String) {
    if (!isAdmin()) return
    val clean = caption.trim().take(280)
    if (clean.isBlank()) return
    val post = BiggGramPost(
      id = "bg_admin_${UUID.randomUUID().toString().take(8)}",
      authorHandle = "@biggboss.official",
      authorName = "BIGG BOSS – THE HOUSE",
      authorAvatarHex = "#EAB954",
      caption = clean,
      category = "LIFESTYLE",
      likes = 15,
      likedByAdmin = true,
    )
    val next = listOf(post) + _biggGramPosts.value
    _biggGramPosts.value = next
    store.saveBiggGramPosts(next)
    onPostMessage("Posted official update on BiggGram.")
  }

  fun markStoryViewed(storyId: String) {
    _biggGramStories.value = _biggGramStories.value.map {
      if (it.id == storyId) it.copy(viewed = true) else it
    }
  }

  fun castNpcVoteOnBiggGram(
    cycle: VotingCycle,
    npc: BiggGramNpcProfile,
    target: NomineeSummary,
  ): Boolean {
    if (!isAdmin()) {
      onPostError("Only Main Admin can supervise NPC voting on BiggGram.")
      return false
    }
    if (cycle.status != "OPEN") {
      onPostError("Voting must be OPEN before NPCs can cast votes on BiggGram.")
      return false
    }
    val postId = _biggGramPosts.value.firstOrNull { it.cycleId == cycle.id }?.id ?: "bg_nom_${cycle.id}"
    val res = BiggGramAndNpcVotingEngine.castSingleNpcVote(
      cycleId = cycle.id,
      existingVotes = _npcVotes.value,
      npc = npc,
      target = target,
      postId = postId,
    )
    return res.fold(
      onSuccess = { vote ->
        val nextVotes = listOf(vote) + _npcVotes.value
        _npcVotes.value = nextVotes
        store.saveNpcVotes(nextVotes)
        val commentText = BiggGramAndNpcVotingEngine.pickNpcReactionComment(npc, target.displayName, cycle.voteType)
        addBiggGramPostComment(postId, npc.handle, npc.name, commentText)
        onPostMessage("${npc.name} voted for ${target.displayName} on BiggGram.")
        true
      },
      onFailure = { err ->
        onPostError(err.message ?: "Duplicate NPC vote blocked.")
        false
      },
    )
  }

  fun runAllRemainingNpcVotesOnBiggGram(cycle: VotingCycle): Int {
    if (!isAdmin()) {
      onPostError("Only Main Admin can run NPC round voting.")
      return 0
    }
    if (cycle.status != "OPEN") {
      onPostError("Open voting first so eligible NPCs can vote on BiggGram.")
      return 0
    }
    val nominees = cycle.parsedNominees()
    if (nominees.isEmpty()) {
      onPostError("No nominated housemates in this cycle.")
      return 0
    }
    // Ensure a BiggGram post exists for this round
    if (_biggGramPosts.value.none { it.cycleId == cycle.id }) {
      publishNomineesToBiggGram(cycle, nominees)
    }
    val postId = _biggGramPosts.value.firstOrNull { it.cycleId == cycle.id }?.id ?: "bg_nom_${cycle.id}"
    val alreadyVotedIds = _npcVotes.value.filter { it.cycleId == cycle.id }.map { it.npcId }.toSet()
    val pendingNpcs = BiggGramAndNpcVotingEngine.NPC_PROFILES.filterNot { alreadyVotedIds.contains(it.npcId) }
    if (pendingNpcs.isEmpty()) {
      onPostMessage("All 15 BiggGram NPCs have already cast their single vote in ${cycle.title}.")
      return 0
    }
    val added = mutableListOf<NpcVoteRecord>()
    var rolling = _npcVotes.value
    pendingNpcs.forEachIndexed { idx, npc ->
      val target = nominees[(idx + npc.name.length) % nominees.size]
      val res = BiggGramAndNpcVotingEngine.castSingleNpcVote(cycle.id, rolling, npc, target, postId)
      res.onSuccess { v ->
        added.add(v)
        rolling = rolling + v
      }
    }
    if (added.isNotEmpty()) {
      _npcVotes.value = added.reversed() + _npcVotes.value
      store.saveNpcVotes(_npcVotes.value)
      added.take(4).forEach { v ->
        val npc = BiggGramAndNpcVotingEngine.NPC_PROFILES.first { it.npcId == v.npcId }
        addBiggGramPostComment(
          postId,
          npc.handle,
          npc.name,
          BiggGramAndNpcVotingEngine.pickNpcReactionComment(npc, v.targetName, cycle.voteType),
        )
      }
      pushAdminNotification("NPC Voting Update", "${added.size} NPC votes recorded on BiggGram for ${cycle.title}", "VOTING")
      onPostMessage("Recorded ${added.size} single non-duplicate NPC votes on BiggGram.")
    }
    return added.size
  }

  fun toggleCctvCameraOnline(cameraId: String) {
    if (!isAdmin()) return
    val next = _cctvOnlineMap.value.toMutableMap()
    val cur = next[cameraId] ?: true
    next[cameraId] = !cur
    _cctvOnlineMap.value = next
    store.saveCctvOnlineMap(next)
    onPostMessage("Camera $cameraId set ${if (!cur) "ONLINE" else "OFFLINE"}.")
  }

  fun pushAdminNotification(title: String, body: String, category: String = "SYSTEM") {
    val item = AdminPhoneNotification(
      id = "notif_${UUID.randomUUID().toString().take(8)}",
      title = title,
      body = body,
      category = category,
    )
    _adminPhoneNotifications.value = (listOf(item) + _adminPhoneNotifications.value).take(40)
  }

  fun markAllNotificationsRead() {
    _adminPhoneNotifications.value = _adminPhoneNotifications.value.map { it.copy(read = true) }
  }
}
