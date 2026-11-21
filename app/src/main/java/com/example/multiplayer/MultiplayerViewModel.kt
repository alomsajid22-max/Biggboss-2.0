package com.example.multiplayer

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private const val VM_TAG = "MultiplayerVM"

enum class HouseDestination {
  LOBBY,
  IN_HOUSE,
}

@OptIn(ExperimentalCoroutinesApi::class)
class MultiplayerViewModel(
  private val firestoreRepo: MultiplayerFirestoreRepository,
  private val phase6Repo: Phase6FirestoreRepository,
  private val phase7Repo: Phase7And8FirestoreRepository,
  private val phase9Repo: Phase9And10FirestoreRepository,
  private val phase11Repo: Phase11CaptainVipRepository,
  val rtdbRepo: RealtimeMultiplayerRepository,
  private val currentUserId: String,
  private val isLocalAdminSession: Boolean = false,
  private val appContext: Context? = null,
) : ViewModel() {

  val sessionId: String = "sess_${UUID.randomUUID().toString().replace("-", "").take(12)}"

  private val defaultAdminAccount = UserAccount(
    id = currentUserId,
    userId = currentUserId,
    username = LocalAdminAuthManager.ADMIN_USERNAME,
    displayName = LocalAdminAuthManager.ADMIN_USERNAME,
    normalizedName = LocalAdminAuthManager.ADMIN_USERNAME.lowercase(),
    playerId = "BB-ADMIN",
    characterId = "Player 01",
    role = PlayerRole.ADMIN.name,
    seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
  )

  private val localAccountFlow = MutableStateFlow<UserAccount?>(defaultAdminAccount)
  private val localAdminFlow = MutableStateFlow(isLocalAdminSession)
  private val localSeasonFlow = MutableStateFlow<SeasonSession?>(
    SeasonSession(
      id = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
      title = "Season 01 • Bigg Boss – The House",
      status = "LIVE",
      maxPlayers = 15L,
      activeCount = 0L,
      dayNightMode = "day",
    )
  )
  private val localMembersFlow = MutableStateFlow<List<SeasonMember>>(emptyList())
  private val localAllUsersFlow = MutableStateFlow<List<UserAccount>>(listOf(defaultAdminAccount))
  private val localVotingCyclesFlow = MutableStateFlow<List<VotingCycle>>(emptyList())
  private val localBallotsFlow = MutableStateFlow<List<VotingBallot>>(emptyList())
  private val localNominationsFlow = MutableStateFlow<List<NominationBallot>>(emptyList())
  private val localEvictionsFlow = MutableStateFlow<List<EvictionRecord>>(emptyList())
  private val localAnnouncementsFlow = MutableStateFlow<List<AdminAnnouncement>>(emptyList())
  private val localTasksFlow = MutableStateFlow<List<HouseTask>>(emptyList())
  private val localTaskSubmissionsFlow = MutableStateFlow<List<TaskSubmission>>(emptyList())
  private val localTeamsFlow = MutableStateFlow<List<HouseTeam>>(emptyList())
  private val localCaptaincyHistoryFlow = MutableStateFlow<List<CaptaincyRecord>>(emptyList())
  private val localHouseRulesFlow = MutableStateFlow(
    Phase6Catalog.defaultHouseRules.map {
      HouseRule(
        id = it.id,
        seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
        ruleNumber = it.ruleNumber,
        title = it.title,
        description = it.description,
        severity = it.severity,
        active = true,
        updatedBy = currentUserId,
      )
    }
  )
  private val localRuleViolationsFlow = MutableStateFlow<List<RuleViolation>>(emptyList())
  private val localDailyScheduleFlow = MutableStateFlow(
    Phase6Catalog.defaultDailySchedule.mapIndexed { idx, it ->
      DailyScheduleSlot(
        id = it.id,
        seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
        orderIndex = it.orderIndex,
        activityKey = it.activityKey,
        title = it.title,
        timeLabel = it.timeLabel,
        location = it.location,
        description = it.description,
        isCurrent = idx == 0,
        updatedBy = currentUserId,
      )
    }
  )
  private val localCctvRecordingsFlow = MutableStateFlow<List<CctvRecordingRecord>>(emptyList())
  private val localRationsFlow = MutableStateFlow(
    Phase7Catalog.defaultRations.map {
      it.copy(id = it.itemId, seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID)
    }
  )
  private val localKitchenProfileFlow = MutableStateFlow<KitchenPlayerState?>(
    KitchenPlayerState(
      id = currentUserId,
      seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
      userId = currentUserId,
      displayName = LocalAdminAuthManager.ADMIN_USERNAME,
    )
  )
  private val localCookingSessionsFlow = MutableStateFlow<List<CookingSession>>(emptyList())
  private val localTaskProgressFlow = MutableStateFlow<List<TaskPlayerProgress>>(emptyList())
  private val localLeaderboardFlow = MutableStateFlow<List<PlayerLeaderboardEntry>>(emptyList())
  private val localAiConfigFlow = MutableStateFlow<AiControlConfig?>(
    AiControlConfig(
      id = Phase9AiCatalog.DEFAULT_CONFIG_ID,
      seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
      enabled = true,
      mode = AiControlMode.ASSIST.name,
      apiProvider = AiApiProviderStatus.RULE_ENGINE_NO_API_KEY.key,
    )
  )
  private val localAiSchedulesFlow = MutableStateFlow<List<AiScheduledEvent>>(emptyList())
  private val localAiProposalsFlow = MutableStateFlow<List<AiApprovalProposal>>(emptyList())
  private val localAiLogsFlow = MutableStateFlow<List<AiActivityLog>>(emptyList())
  private val localVipPrivilegesFlow = MutableStateFlow<List<VipPrivilege>>(emptyList())
  private val localPrivilegeHistoryFlow = MutableStateFlow<List<PrivilegeHistoryRecord>>(emptyList())
  private val localGameSettingsFlow = MutableStateFlow<GameSettings?>(
    GameSettings(
      id = "config",
      seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    )
  )
  private val localAdminAuditLogsFlow = MutableStateFlow<List<AdminAuditLog>>(
    listOf(
      AdminAuditLog(
        id = "local_login_init",
        seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
        actionType = "LOCAL_ADMIN_LOGIN",
        summary = "Signed in locally on device as ${LocalAdminAuthManager.ADMIN_USERNAME}.",
        actorUid = currentUserId,
      )
    )
  )

  val accountState: StateFlow<UiState<UserAccount?>> = if (isLocalAdminSession) {
    localAccountFlow.map<UserAccount?, UiState<UserAccount?>> { UiState.Success(it) }
      .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Success(localAccountFlow.value))
  } else {
    firestoreRepo.observeUserAccount(currentUserId)
      .map<UserAccount?, UiState<UserAccount?>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(VM_TAG, "Error observing account", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load player profile."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )
  }

  val isAdminVerified: StateFlow<Boolean> = if (isLocalAdminSession) {
    localAdminFlow.asStateFlow()
  } else {
    firestoreRepo.observeAdminGrant(currentUserId)
      .catch { error ->
        if (error is CancellationException) throw error
        emit(false)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = false,
      )
  }

  val seasonState: StateFlow<UiState<SeasonSession?>> = if (isLocalAdminSession) {
    localSeasonFlow.map<SeasonSession?, UiState<SeasonSession?>> { UiState.Success(it) }
      .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Success(localSeasonFlow.value))
  } else {
    firestoreRepo.observeActiveSeason()
      .map<SeasonSession?, UiState<SeasonSession?>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(VM_TAG, "Error observing season", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load season status."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )
  }

  val membersState: StateFlow<UiState<List<SeasonMember>>> = if (isLocalAdminSession) {
    localMembersFlow.map<List<SeasonMember>, UiState<List<SeasonMember>>> { UiState.Success(it) }
      .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Success(localMembersFlow.value))
  } else {
    firestoreRepo.observeSeasonMembers()
      .map<List<SeasonMember>, UiState<List<SeasonMember>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(VM_TAG, "Error observing members", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load online housemates."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )
  }

  // Registered user directory is queried ONLY when ADMIN role is verified.
  val allUsersState: StateFlow<List<UserAccount>> = if (isLocalAdminSession) {
    localAllUsersFlow.asStateFlow()
  } else {
    isAdminVerified
      .flatMapLatest { isAdmin ->
        if (!isAdmin) flowOf(emptyList())
        else firestoreRepo.observeAllRegisteredUsers().catch { emit(emptyList()) }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val cctvRecordingsState: StateFlow<List<CctvRecordingRecord>> = if (isLocalAdminSession) {
    localCctvRecordingsFlow.asStateFlow()
  } else {
    isAdminVerified
      .flatMapLatest { isAdmin ->
        if (!isAdmin) flowOf(emptyList())
        else firestoreRepo.observeCctvRecordings().catch { emit(emptyList()) }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val votingCyclesState: StateFlow<List<VotingCycle>> = if (isLocalAdminSession) {
    localVotingCyclesFlow.asStateFlow()
  } else {
    firestoreRepo.observeVotingCycles()
      .catch { emit(emptyList()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  private val _selectedCycleId = MutableStateFlow<String?>(null)

  val activeVotingCycle: StateFlow<VotingCycle?> = combine(votingCyclesState, _selectedCycleId) { cycles, pickedId ->
    cycles.firstOrNull { it.id == pickedId }
      ?: cycles.firstOrNull { it.status == "OPEN" }
      ?: cycles.firstOrNull { it.status == "NOMINATIONS" }
      ?: cycles.firstOrNull { it.status == "CLOSED" }
      ?: cycles.firstOrNull()
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = null,
  )

  val currentCycleVotesState: StateFlow<List<VotingBallot>> = if (isLocalAdminSession) {
    combine(activeVotingCycle, localBallotsFlow) { cycle, ballots ->
      val cid = cycle?.id
      if (cid.isNullOrBlank()) emptyList() else ballots.filter { it.cycleId == cid }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
  } else {
    combine(
      activeVotingCycle,
      isAdminVerified,
    ) { cycle, isAdmin ->
      val id = cycle?.id
      val canReadAll = cycle != null && Phase10VotingLogic.canPlayerViewTotals(cycle, isAdmin)
      Triple(id, canReadAll, cycle?.status)
    }
      .distinctUntilChanged()
      .flatMapLatest { (cycleId, canReadAll, _) ->
        if (cycleId.isNullOrBlank()) flowOf(emptyList())
        else firestoreRepo.observeVotesForCycle(
          cycleId = cycleId,
          canReadAllVotes = canReadAll,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val currentCycleNominationsState: StateFlow<List<NominationBallot>> = if (isLocalAdminSession) {
    combine(activeVotingCycle, localNominationsFlow) { cycle, noms ->
      val cid = cycle?.id
      if (cid.isNullOrBlank()) emptyList() else noms.filter { it.cycleId == cid }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
  } else {
    activeVotingCycle
      .map { it?.id }
      .distinctUntilChanged()
      .flatMapLatest { cycleId ->
        if (cycleId.isNullOrBlank()) flowOf(emptyList())
        else phase9Repo.observeNominationsForCycle(cycleId = cycleId).catch { emit(emptyList()) }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val votingTallySummary: StateFlow<VotingTallyResult?> = combine(
    activeVotingCycle,
    currentCycleVotesState,
    seasonState,
  ) { cycle, ballots, sState ->
    val season = (sState as? UiState.Success)?.data
    cycle?.let { Phase11VipLogic.calculateWeightedVotingTally(it, ballots, season).baseResult }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = null,
  )

  val evictionsState: StateFlow<List<EvictionRecord>> = if (isLocalAdminSession) {
    localEvictionsFlow.asStateFlow()
  } else {
    firestoreRepo.observeEvictions()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val announcementsState: StateFlow<List<AdminAnnouncement>> = if (isLocalAdminSession) {
    localAnnouncementsFlow.asStateFlow()
  } else {
    firestoreRepo.observeAnnouncements()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val tasksState: StateFlow<List<HouseTask>> = if (isLocalAdminSession) {
    localTasksFlow.asStateFlow()
  } else {
    phase6Repo.observeTasks()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  private val _selectedTaskId = MutableStateFlow<String?>(null)

  val activeHouseTask: StateFlow<HouseTask?> = combine(tasksState, _selectedTaskId) { tasks, pickedId ->
    tasks.firstOrNull { it.id == pickedId }
      ?: tasks.firstOrNull { it.status in listOf("ACTIVE", "IN_PROGRESS", "ASSIGNED") }
      ?: tasks.firstOrNull { it.status == "PAUSED" }
      ?: tasks.firstOrNull { it.status == "DRAFT" }
      ?: tasks.firstOrNull()
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = null,
  )

  val activeTaskSubmissions: StateFlow<List<TaskSubmission>> = if (isLocalAdminSession) {
    combine(activeHouseTask, localTaskSubmissionsFlow) { task, subs ->
      val tid = task?.id
      if (tid.isNullOrBlank()) emptyList() else subs.filter { it.taskId == tid }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
  } else {
    combine(
      activeHouseTask,
      isAdminVerified,
    ) { task, isAdmin ->
      Triple(task?.id, isAdmin || task?.status == "COMPLETED", task?.status)
    }
      .distinctUntilChanged()
      .flatMapLatest { (taskId, canReadAll, _) ->
        if (taskId.isNullOrBlank()) flowOf(emptyList())
        else phase6Repo.observeTaskSubmissions(
          taskId = taskId,
          canReadAll = canReadAll,
          currentUid = currentUserId,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val teamsState: StateFlow<List<HouseTeam>> = if (isLocalAdminSession) {
    localTeamsFlow.asStateFlow()
  } else {
    phase6Repo.observeTeams()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val captaincyHistoryState: StateFlow<List<CaptaincyRecord>> = if (isLocalAdminSession) {
    localCaptaincyHistoryFlow.asStateFlow()
  } else {
    phase6Repo.observeCaptaincyHistory()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val houseRulesState: StateFlow<List<HouseRule>> = if (isLocalAdminSession) {
    localHouseRulesFlow.asStateFlow()
  } else {
    phase6Repo.observeHouseRules()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val ruleViolationsState: StateFlow<List<RuleViolation>> = if (isLocalAdminSession) {
    localRuleViolationsFlow.asStateFlow()
  } else {
    phase6Repo.observeRuleViolations()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val dailyScheduleState: StateFlow<List<DailyScheduleSlot>> = if (isLocalAdminSession) {
    localDailyScheduleFlow.asStateFlow()
  } else {
    phase6Repo.observeDailySchedule()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  private val _collected3dTaskNodes = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
  val collected3dTaskNodes: StateFlow<Map<String, Set<String>>> = _collected3dTaskNodes.asStateFlow()

  // --- PHASE 7 & 8 STATEFLOWS ---

  val rationsState: StateFlow<List<RationItem>> = if (isLocalAdminSession) {
    localRationsFlow.asStateFlow()
  } else {
    phase7Repo.observeRations()
      .catch { emit(emptyList()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val myKitchenProfileState: StateFlow<KitchenPlayerState?> = if (isLocalAdminSession) {
    localKitchenProfileFlow.asStateFlow()
  } else {
    phase7Repo.observeKitchenProfile(userId = currentUserId)
      .catch { emit(null) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null,
      )
  }

  val cookingSessionsState: StateFlow<List<CookingSession>> = if (isLocalAdminSession) {
    localCookingSessionsFlow.asStateFlow()
  } else {
    phase7Repo.observeCookingSessions()
      .catch { emit(emptyList()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val activeTaskProgressState: StateFlow<List<TaskPlayerProgress>> = if (isLocalAdminSession) {
    combine(activeHouseTask, localTaskProgressFlow) { task, list ->
      val tid = task?.id
      if (tid.isNullOrBlank()) emptyList() else list.filter { it.taskId == tid }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
  } else {
    activeHouseTask
      .map { it?.id }
      .distinctUntilChanged()
      .flatMapLatest { taskId ->
        if (taskId.isNullOrBlank()) flowOf(emptyList())
        else phase7Repo.observeTaskProgressList(taskId = taskId)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val leaderboardState: StateFlow<List<PlayerLeaderboardEntry>> = if (isLocalAdminSession) {
    localLeaderboardFlow.asStateFlow()
  } else {
    phase7Repo.observeLeaderboard()
      .catch { emit(emptyList()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  // --- PHASE 9 CHATGPT AI CONTROL ROOM STATEFLOWS (ADMIN-ONLY) ---

  val aiConfigState: StateFlow<AiControlConfig?> = if (isLocalAdminSession) {
    localAiConfigFlow.asStateFlow()
  } else {
    isAdminVerified
      .flatMapLatest { isAdmin ->
        if (!isAdmin) flowOf(null)
        else phase9Repo.observeAiConfig().catch { emit(null) }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null,
      )
  }

  val aiSchedulesState: StateFlow<List<AiScheduledEvent>> = if (isLocalAdminSession) {
    localAiSchedulesFlow.asStateFlow()
  } else {
    isAdminVerified
      .flatMapLatest { isAdmin ->
        if (!isAdmin) flowOf(emptyList())
        else phase9Repo.observeAiSchedules().catch { emit(emptyList()) }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val aiProposalsState: StateFlow<List<AiApprovalProposal>> = if (isLocalAdminSession) {
    localAiProposalsFlow.asStateFlow()
  } else {
    isAdminVerified
      .flatMapLatest { isAdmin ->
        if (!isAdmin) flowOf(emptyList())
        else phase9Repo.observeAiProposals().catch { emit(emptyList()) }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val aiLogsState: StateFlow<List<AiActivityLog>> = if (isLocalAdminSession) {
    localAiLogsFlow.asStateFlow()
  } else {
    isAdminVerified
      .flatMapLatest { isAdmin ->
        if (!isAdmin) flowOf(emptyList())
        else phase9Repo.observeAiLogs().catch { emit(emptyList()) }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val whileAwaySummary: StateFlow<AiWhileAwaySummary> = combine(
    aiLogsState,
    aiConfigState,
  ) { logs, config ->
    Phase9AiHouseManagerEngine.computeWhileAwaySummary(
      logs = logs,
      lastAdminSeenAtMillis = config?.lastAdminSeenAtMillis ?: 0L,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = AiWhileAwaySummary(),
  )

  // --- PHASE 10 & 11 CAPTAINCY, VIP PRIVILEGES, SETTINGS & AUDIT LOGS ---

  val vipPrivilegesState: StateFlow<List<VipPrivilege>> = if (isLocalAdminSession) {
    localVipPrivilegesFlow.asStateFlow()
  } else {
    phase11Repo.observeVipPrivileges()
      .catch { emit(emptyList()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val privilegeHistoryState: StateFlow<List<PrivilegeHistoryRecord>> = if (isLocalAdminSession) {
    localPrivilegeHistoryFlow.asStateFlow()
  } else {
    phase11Repo.observePrivilegeHistory()
      .catch { emit(emptyList()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  val gameSettingsState: StateFlow<GameSettings?> = if (isLocalAdminSession) {
    localGameSettingsFlow.asStateFlow()
  } else {
    phase11Repo.observeGameSettings()
      .catch { emit(null) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null,
      )
  }

  val adminAuditLogsState: StateFlow<List<AdminAuditLog>> = if (isLocalAdminSession) {
    localAdminAuditLogsFlow.asStateFlow()
  } else {
    isAdminVerified
      .flatMapLatest { isAdmin ->
        if (!isAdmin) flowOf(emptyList())
        else phase11Repo.observeAdminAuditLogs().catch { emit(emptyList()) }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )
  }

  private val _kitchenAnimPulse = MutableStateFlow(Pair("", 0L))
  val kitchenAnimPulse: StateFlow<Pair<String, Long>> = _kitchenAnimPulse.asStateFlow()

  private val _destination = MutableStateFlow(HouseDestination.LOBBY)
  val destination: StateFlow<HouseDestination> = _destination.asStateFlow()

  private val _actionError = MutableStateFlow<String?>(null)
  val actionError: StateFlow<String?> = _actionError.asStateFlow()

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private val _isUploadingCctv = MutableStateFlow(false)
  val isUploadingCctv: StateFlow<Boolean> = _isUploadingCctv.asStateFlow()

  private val _cctvUploadStatus = MutableStateFlow<String?>(null)
  val cctvUploadStatus: StateFlow<String?> = _cctvUploadStatus.asStateFlow()

  val phase12To20: Phase12To20Coordinator by lazy {
    Phase12To20Coordinator(
      scope = viewModelScope,
      store = Phase12To20PersistenceStore(appContext),
      currentUserId = currentUserId,
      getAccount = { (accountState.value as? UiState.Success)?.data },
      isAdmin = { isAdminVerified.value },
      onPostMessage = { msg -> _actionMessage.value = msg },
      onPostError = { err -> _actionError.value = err },
      onBroadcastPublicTts = { msg, preset, lang, gender ->
        if (isAdminVerified.value) {
          sendAnnouncement(msg, preset, lang, gender)
        } else {
          _actionMessage.value = msg
        }
      },
    )
  }

  fun triggerHouseWelcomeIfFirstEntry(): Boolean {
    val ttsSettings = phase12To20.ttsSettingsState.value
    if (ttsSettings.welcomePlayed) return false
    phase12To20.markWelcomePlayed()
    if (isAdminVerified.value) {
      sendAnnouncement(
        message = AnnouncementPresets.WELCOME_ANNOUNCEMENT,
        presetId = "welcome_house",
        languageTag = "en-IN",
        voiceGender = VoiceGender.FEMALE,
      )
    }
    return true
  }

  private var didInitialOfflineCatchUp = false

  init {
    if (!isLocalAdminSession) {
      viewModelScope.launch {
        firestoreRepo.ensureDefaultSeasonExists()
        phase7Repo.ensureDefaultRationsExist()
      }
      viewModelScope.launch {
        isAdminVerified.collect { isAdmin ->
          if (isAdmin && !didInitialOfflineCatchUp) {
            didInitialOfflineCatchUp = true
            phase9Repo.ensureAiConfigInitialized()
            delay(1200L)
            runAiAutomationTickInternal(source = "OFFLINE_CATCHUP", silent = true)
          }
        }
      }
      viewModelScope.launch {
        while (true) {
          delay(30_000L)
          val cfg = aiConfigState.value
          if (isAdminVerified.value && cfg != null && cfg.isOperational() && cfg.parsedMode() != AiControlMode.MANUAL) {
            runAiAutomationTickInternal(source = "ADMIN_TICK", silent = true)
          }
        }
      }
    }
  }

  private fun appendLocalAudit(actionType: String, summary: String) {
    val entry = AdminAuditLog(
      id = "audit_${UUID.randomUUID().toString().take(8)}",
      seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
      actionType = actionType,
      summary = summary,
      actorUid = currentUserId,
    )
    localAdminAuditLogsFlow.value = listOf(entry) + localAdminAuditLogsFlow.value
  }

  fun syncPhase7And8Defaults(account: UserAccount?) {
    if (account == null || isLocalAdminSession) return
    val myTeamId = teamsState.value.firstOrNull { it.memberUids.contains(currentUserId) }?.id
    viewModelScope.launch {
      phase7Repo.ensureDefaultRationsExist()
      phase7Repo.ensureKitchenProfileInitialized(account = account)
      phase7Repo.ensureLeaderboardEntryInitialized(account = account, teamId = myTeamId)
    }
  }

  fun triggerKitchenPose(mode: String) {
    if (mode.isBlank()) return
    _kitchenAnimPulse.value = mode to System.currentTimeMillis()
  }

  fun clearError() {
    _actionError.value = null
  }

  fun clearMessage() {
    _actionMessage.value = null
  }

  fun selectVotingCycle(cycleId: String?) {
    _selectedCycleId.value = cycleId
  }

  fun selectHouseTask(taskId: String?) {
    _selectedTaskId.value = taskId
  }

  fun record3dTaskNodeCollected(taskId: String, nodeId: String) {
    if (taskId.isBlank() || nodeId.isBlank()) return
    val current = _collected3dTaskNodes.value.toMutableMap()
    val set = (current[taskId] ?: emptySet()) + nodeId
    current[taskId] = set
    _collected3dTaskNodes.value = current

    val account = (accountState.value as? UiState.Success)?.data ?: return
    val task = tasksState.value.firstOrNull { it.id == taskId } ?: activeHouseTask.value?.takeIf { it.id == taskId } ?: return
    if (isLocalAdminSession) {
      val existing = localTaskProgressFlow.value.firstOrNull { it.taskId == taskId && it.userId == currentUserId }
      val steps = ((existing?.completedSteps ?: emptyList()) + nodeId).distinct()
      val totalSteps = task.requiredCheckpoints.size.toLong().coerceAtLeast(1L)
      val pct = ((steps.size.toLong() * 100L) / totalSteps).coerceIn(0L, 100L)
      val updated = (existing ?: TaskPlayerProgress(
        id = "${taskId}_$currentUserId",
        taskId = taskId,
        userId = currentUserId,
        displayName = account.displayName,
      )).copy(completedSteps = steps, percent = pct)
      localTaskProgressFlow.value = (localTaskProgressFlow.value.filterNot { it.id == updated.id }) + updated
      return
    }
    viewModelScope.launch {
      val res = phase7Repo.recordTaskCheckpointProgress(
        task = task,
        stepId = nodeId,
        displayName = account.displayName,
      )
      res.onSuccess { prog ->
        rtdbRepo.publishTaskProgressLive(taskId, prog.percent)
      }
    }
  }

  private fun requireAdminAction(): Boolean {
    if (!isAdminVerified.value) {
      _actionError.value = "Access denied: ADMIN role required."
      return false
    }
    return true
  }

  fun saveProfile(displayName: String, characterId: String) {
    if (isLocalAdminSession) {
      val cleanName = displayName.trim().ifBlank { LocalAdminAuthManager.ADMIN_USERNAME }
      val current = localAccountFlow.value ?: defaultAdminAccount
      localAccountFlow.value = current.copy(
        displayName = cleanName,
        normalizedName = cleanName.lowercase(),
        characterId = characterId,
      )
      _actionMessage.value = "Local Admin profile updated."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = firestoreRepo.registerOrUpdateProfile(displayName, characterId)
      _isBusy.value = false
      res.onFailure { err ->
        _actionError.value = err.message ?: "Could not save profile."
      }
    }
  }

  fun updateCharacterPreference(characterId: String) {
    val account = (accountState.value as? UiState.Success)?.data ?: return
    if (isLocalAdminSession) {
      localAccountFlow.value = account.copy(characterId = characterId)
      _actionMessage.value = "Switched avatar to $characterId."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = firestoreRepo.registerOrUpdateProfile(account.displayName, characterId)
      if (res.isSuccess && _destination.value == HouseDestination.IN_HOUSE) {
        firestoreRepo.updateMemberStatus(
          status = "IN_HOUSE",
          online = true,
          characterId = characterId,
          sessionId = sessionId,
        )
      }
      _isBusy.value = false
      res.onFailure { err ->
        _actionError.value = err.message ?: "Could not switch character."
      }
    }
  }

  fun enterMultiplayerHouse(characterIdOverride: String? = null) {
    val account = (accountState.value as? UiState.Success)?.data ?: return
    if (account.banned) {
      _actionError.value = account.banReason ?: "This account has been banned by Admin."
      return
    }
    val chosenChar = characterIdOverride ?: account.characterId
    if (isLocalAdminSession) {
      localAccountFlow.value = account.copy(characterId = chosenChar)
      _actionError.value = null
      _destination.value = HouseDestination.IN_HOUSE
      appendLocalAudit("ENTER_CONTROL_ROOM", "Admin entered 3D House & Private Control Room locally.")
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      rtdbRepo.clearKickBanner()
      val joinRes = firestoreRepo.joinSeasonHouse(
        account = account,
        isAdminVerified = isAdminVerified.value,
        characterId = chosenChar,
        sessionId = sessionId,
      )
      joinRes.onSuccess {
        rtdbRepo.attachSeasonSync(
          seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
          uid = currentUserId,
          account = account,
          characterId = chosenChar,
          sessionId = sessionId,
          isAdmin = isAdminVerified.value,
        )
        _destination.value = HouseDestination.IN_HOUSE
      }.onFailure { err ->
        _actionError.value = err.message ?: "Unable to join house season."
      }
      _isBusy.value = false
    }
  }

  fun exitHouseToLobby() {
    if (isLocalAdminSession) {
      _destination.value = HouseDestination.LOBBY
      viewModelScope.launch { rtdbRepo.detachSync() }
      return
    }
    viewModelScope.launch {
      _destination.value = HouseDestination.LOBBY
      rtdbRepo.detachSync()
      firestoreRepo.updateMemberStatus(status = "LOBBY", online = true)
    }
  }

  fun forceRemovalFromHouse(reason: String) {
    viewModelScope.launch {
      _destination.value = HouseDestination.LOBBY
      rtdbRepo.detachSync()
      _actionError.value = reason
    }
  }

  suspend fun prepareSignOutCleanup() {
    rtdbRepo.detachSync()
    if (!isLocalAdminSession) {
      firestoreRepo.releaseSeasonSlot()
    }
  }

  fun reportAdminAction(commandType: String, payloadSummary: String) {
    if (!requireAdminAction()) return
    if (isLocalAdminSession) {
      appendLocalAudit(commandType, payloadSummary)
      return
    }
    viewModelScope.launch {
      val res = firestoreRepo.recordAdminControlAction(
        commandType = commandType,
        payloadSummary = payloadSummary,
      )
      res.onFailure { err ->
        _actionError.value = err.message ?: "Admin control action denied."
      }
    }
  }

  fun setSeasonStatus(nextStatus: String) {
    if (!requireAdminAction()) return
    if (isLocalAdminSession) {
      localSeasonFlow.value = (localSeasonFlow.value ?: SeasonSession(id = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID))
        .copy(status = nextStatus)
      appendLocalAudit("SEASON_STATUS", "Season status updated locally to $nextStatus.")
      _actionMessage.value = "Season status updated to $nextStatus."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = firestoreRepo.setSeasonStatus(nextStatus = nextStatus)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "Season status updated to $nextStatus."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Failed to update season status."
      }
    }
  }

  fun moderatePlayer(targetUid: String, action: String, reason: String = "") {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    if (isLocalAdminSession) {
      localMembersFlow.value = localMembersFlow.value.map { mem ->
        if (mem.userId != targetUid) mem
        else when (action) {
          "KICK" -> mem.copy(status = "REMOVED", online = false)
          "BAN" -> mem.copy(status = "BANNED", online = false)
          "UNBAN" -> mem.copy(status = "LOBBY")
          else -> mem
        }
      }
      localAllUsersFlow.value = localAllUsersFlow.value.map { user ->
        if (user.userId != targetUid) user
        else when (action) {
          "BAN" -> user.copy(banned = true, banReason = reason.ifBlank { "Banned by Admin Control Room." })
          "UNBAN" -> user.copy(banned = false, banReason = null)
          else -> user
        }
      }
      appendLocalAudit("MODERATE_PLAYER", "$action applied locally to $targetUid")
      _actionMessage.value = "Player action $action applied locally."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = firestoreRepo.moderateHousemate(
        seasonId = seasonId,
        targetUid = targetUid,
        action = action,
        reason = reason,
      )
      if (res.isSuccess) {
        when (action) {
          "KICK" -> rtdbRepo.publishKick(seasonId, targetUid, reason.ifBlank { "Removed from session by Admin." })
          "BAN" -> rtdbRepo.publishKick(seasonId, targetUid, reason.ifBlank { "Banned by Admin Control Room." })
          "UNBAN" -> rtdbRepo.clearKick(seasonId, targetUid)
        }
        _actionMessage.value = "Player action $action applied."
      } else {
        _actionError.value = res.exceptionOrNull()?.message ?: "Moderation action failed."
      }
      _isBusy.value = false
    }
  }

  fun publishNominations(
    title: String,
    nominees: List<NomineeSummary>,
    openVotingImmediately: Boolean,
    npcVotesEnabled: Boolean,
  ) {
    if (!requireAdminAction()) return
    val status = if (openVotingImmediately) "OPEN" else "NOMINATIONS"
    if (isLocalAdminSession) {
      val cycleId = "cyc_${UUID.randomUUID().toString().take(8)}"
      val cycle = VotingCycle(
        id = cycleId,
        seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
        title = title.trim().ifBlank { "Weekly Eviction Nomination" },
        status = status,
        nominatedUids = nominees.map { it.uid },
        nomineeSummaries = nominees.map { it.encode() },
        npcVotesEnabled = npcVotesEnabled,
        createdBy = currentUserId,
      )
      localVotingCyclesFlow.value = listOf(cycle) + localVotingCyclesFlow.value
      _selectedCycleId.value = cycleId
      _actionMessage.value = "Nomination list published locally ($status)."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = firestoreRepo.publishOrUpdateVotingCycle(
        title = title,
        status = status,
        nominees = nominees,
        npcVotesEnabled = npcVotesEnabled,
      )
      _isBusy.value = false
      res.onSuccess { cycleId ->
        _selectedCycleId.value = cycleId
        _actionMessage.value = "Nomination list published ($status)."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Failed to publish nominations."
      }
    }
  }

  fun updateVotingCycleState(
    cycle: VotingCycle,
    nextStatus: String,
    npcVotesEnabled: Boolean = cycle.npcVotesEnabled,
  ) {
    if (!requireAdminAction()) return
    val nominees = cycle.parsedNominees()
    if (nominees.isEmpty()) {
      _actionError.value = "Cannot update cycle without nominees."
      return
    }
    if (isLocalAdminSession) {
      localVotingCyclesFlow.value = localVotingCyclesFlow.value.map {
        if (it.id == cycle.id) it.copy(status = nextStatus, npcVotesEnabled = npcVotesEnabled) else it
      }
      _actionMessage.value = "Voting is now $nextStatus."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = firestoreRepo.publishOrUpdateVotingCycle(
        cycleId = cycle.id,
        title = cycle.title,
        status = nextStatus,
        nominees = nominees,
        npcVotesEnabled = npcVotesEnabled,
      )
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "Voting is now $nextStatus."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Failed to update voting state."
      }
    }
  }

  fun castMyRealVote(cycleId: String, targetUid: String) {
    val account = (accountState.value as? UiState.Success)?.data ?: return
    if (isLocalAdminSession) {
      val ballot = VotingBallot(
        id = "vote_${currentUserId}_$cycleId",
        seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
        cycleId = cycleId,
        voterUid = currentUserId,
        voterName = account.displayName,
        targetUid = targetUid,
        sourceType = "REAL_PLAYER",
      )
      localBallotsFlow.value = localBallotsFlow.value.filterNot {
        it.cycleId == cycleId && it.voterUid == currentUserId && it.sourceType == "REAL_PLAYER"
      } + ballot
      _actionMessage.value = "Your vote has been recorded locally."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = firestoreRepo.castRealVote(
        cycleId = cycleId,
        targetUid = targetUid,
        voterDisplayName = account.displayName,
      )
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "Your vote has been recorded."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not cast vote."
      }
    }
  }

  fun castSimulatedNpcVote(cycleId: String, npcLabel: String, targetUid: String) {
    if (!requireAdminAction()) return
    if (isLocalAdminSession) {
      val cyc = localVotingCyclesFlow.value.firstOrNull { it.id == cycleId }
      if (cyc?.npcVotesEnabled != true) {
        _actionError.value = "Enable NPC voting on this cycle first."
        return
      }
      val ballot = VotingBallot(
        id = "npc_${UUID.randomUUID().toString().take(8)}",
        seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
        cycleId = cycleId,
        voterUid = "npc_${npcLabel.lowercase().replace(" ", "_")}",
        voterName = npcLabel,
        targetUid = targetUid,
        sourceType = "NPC",
      )
      localBallotsFlow.value = localBallotsFlow.value + ballot
      _actionMessage.value = "Recorded 1 optional NPC vote ($npcLabel)."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = firestoreRepo.castNpcVote(
        cycleId = cycleId,
        npcLabel = npcLabel,
        targetUid = targetUid,
      )
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "Recorded 1 optional NPC vote ($npcLabel)."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Enable NPC voting on this cycle first."
      }
    }
  }

  fun confirmEviction(
    cycle: VotingCycle,
    nominee: NomineeSummary,
    realVotes: Long,
    npcVotes: Long,
    customAnnouncement: String,
    languageTag: String,
    voiceGender: VoiceGender,
  ) {
    if (!requireAdminAction()) return
    val currentSeason = (seasonState.value as? UiState.Success)?.data
    if (Phase11VipLogic.isProtectedByCaptainImmunity(nominee.uid, currentSeason)) {
      _actionError.value = "${nominee.displayName} is the House Captain with active Nomination Immunity!"
      return
    }
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    if (isLocalAdminSession) {
      localVotingCyclesFlow.value = localVotingCyclesFlow.value.map {
        if (it.id == cycle.id) it.copy(status = "EVICTED", evictedUid = nominee.uid, evictedDisplayName = nominee.displayName) else it
      }
      localMembersFlow.value = localMembersFlow.value.map {
        if (it.userId == nominee.uid) it.copy(status = "EVICTED", online = false) else it
      }
      localEvictionsFlow.value = listOf(
        EvictionRecord(
          id = "evict_${UUID.randomUUID().toString().take(8)}",
          seasonId = seasonId,
          cycleId = cycle.id,
          userId = nominee.uid,
          playerId = nominee.playerId,
          displayName = nominee.displayName,
          characterId = nominee.characterId,
          realVotes = realVotes,
          npcVotes = npcVotes,
          announcementText = customAnnouncement,
          evictedBy = currentUserId,
        )
      ) + localEvictionsFlow.value
      localAnnouncementsFlow.value = listOf(
        AdminAnnouncement(
          id = "ann_${UUID.randomUUID().toString().take(8)}",
          seasonId = seasonId,
          message = customAnnouncement,
          presetId = "eviction_confirmed",
          languageTag = languageTag,
          voiceGender = voiceGender.name,
          issuedBy = currentUserId,
        )
      ) + localAnnouncementsFlow.value
      appendLocalAudit("EVICTION_CONFIRMED", "Evicted ${nominee.displayName} locally.")
      _actionMessage.value = "${nominee.displayName} evicted & announced locally."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = firestoreRepo.confirmEviction(
        seasonId = seasonId,
        cycleId = cycle.id,
        nominee = nominee,
        realVotes = realVotes,
        npcVotes = npcVotes,
        announcementText = customAnnouncement,
      )
      if (res.isSuccess) {
        rtdbRepo.publishKick(seasonId, nominee.uid, customAnnouncement)
        val annRes = firestoreRepo.publishAnnouncement(
          seasonId = seasonId,
          message = customAnnouncement,
          presetId = "eviction_confirmed",
          languageTag = languageTag,
          voiceGender = voiceGender,
        )
        annRes.onSuccess { annId ->
          rtdbRepo.publishRtdbAnnouncement(seasonId, annId, customAnnouncement, languageTag, voiceGender.name)
        }
        _actionMessage.value = "${nominee.displayName} evicted & announced to all housemates."
      } else {
        _actionError.value = res.exceptionOrNull()?.message ?: "Eviction confirmation failed."
      }
      _isBusy.value = false
    }
  }

  fun sendAnnouncement(
    message: String,
    presetId: String?,
    languageTag: String,
    voiceGender: VoiceGender,
  ) {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    if (isLocalAdminSession) {
      localAnnouncementsFlow.value = listOf(
        AdminAnnouncement(
          id = "ann_${UUID.randomUUID().toString().take(8)}",
          seasonId = seasonId,
          message = message,
          presetId = presetId,
          languageTag = languageTag,
          voiceGender = voiceGender.name,
          issuedBy = currentUserId,
        )
      ) + localAnnouncementsFlow.value
      phase12To20.triggerSpeakerPulse()
      appendLocalAudit("ANNOUNCEMENT", message.take(80))
      _actionMessage.value = "Announcement queued locally."
      return
    }
    phase12To20.triggerSpeakerPulse()
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = firestoreRepo.publishAnnouncement(
        seasonId = seasonId,
        message = message,
        presetId = presetId,
        languageTag = languageTag,
        voiceGender = voiceGender,
      )
      _isBusy.value = false
      res.onSuccess { annId ->
        rtdbRepo.publishRtdbAnnouncement(seasonId, annId, message, languageTag, voiceGender.name)
        _actionMessage.value = "Announcement broadcast to all connected players."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Failed to broadcast announcement."
      }
    }
  }

  // --- PHASE 6: TASKS, TEAMS, CAPTAINCY, HOUSE RULES & DAILY SCHEDULE ---

  fun ensureDefaultRulesAndScheduleIfAdmin() {
    if (!isAdminVerified.value || isLocalAdminSession) return
    val needRules = houseRulesState.value.isEmpty()
    val needSchedule = dailyScheduleState.value.isEmpty()
    if (!needRules && !needSchedule) return
    viewModelScope.launch {
      phase6Repo.seedDefaultRulesAndSchedule(
        seedRules = needRules,
        seedSchedule = needSchedule,
      )
    }
  }

  fun saveOrStartHouseTask(
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
  ) {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    if (isLocalAdminSession) {
      val now = System.currentTimeMillis()
      val id = taskId ?: "task_${UUID.randomUUID().toString().take(8)}"
      val saved = HouseTask(
        id = id,
        seasonId = seasonId,
        title = title.trim(),
        description = description.trim(),
        instructions = instructions.trim(),
        activityType = activityType.name,
        assignmentMode = assignmentMode,
        category = category.name,
        difficulty = difficulty.name,
        isCaptaincy = isCaptaincy,
        status = if (startImmediately) "ACTIVE" else "DRAFT",
        targetRoom = targetRoom,
        durationSeconds = durationSeconds,
        startTimeMillis = if (startImmediately) now else 0L,
        endTimeMillis = if (startImmediately) now + durationSeconds * 1000L else 0L,
        promptQuestion = promptQuestion,
        options = options,
        assignedUids = assignedUids,
        assignedTeamId = assignedTeamId,
        requiredCheckpoints = requiredCheckpoints,
        requiredRecipeId = requiredRecipeId,
        rewardAchievement = rewardAchievement,
        maxPoints = maxPoints,
        createdBy = currentUserId,
      )
      localTasksFlow.value = listOf(saved) + localTasksFlow.value.filterNot { it.id == id }
      _selectedTaskId.value = id
      _actionMessage.value = if (startImmediately) "Task '${saved.title}' is now LIVE!" else "Task '${saved.title}' saved."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase6Repo.upsertTask(
        seasonId = seasonId,
        taskId = taskId,
        title = title,
        description = description,
        instructions = instructions,
        activityType = activityType,
        assignmentMode = assignmentMode,
        isCaptaincy = isCaptaincy,
        targetRoom = targetRoom,
        durationSeconds = durationSeconds,
        promptQuestion = promptQuestion,
        options = options,
        maxPoints = maxPoints,
        expectedAnswer = expectedAnswer,
        startImmediately = startImmediately,
        category = category,
        difficulty = difficulty,
        assignedUids = assignedUids,
        assignedTeamId = assignedTeamId,
        requiredCheckpoints = requiredCheckpoints,
        requiredRecipeId = requiredRecipeId,
        rewardAchievement = rewardAchievement,
      )
      _isBusy.value = false
      res.onSuccess { saved ->
        _selectedTaskId.value = saved.id
        rtdbRepo.publishActiveTaskSnapshot(seasonId, saved)
        _actionMessage.value = if (startImmediately) "Task '${saved.title}' is now LIVE!" else "Task '${saved.title}' saved."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not save task."
      }
    }
  }

  fun updateHouseTaskStatus(task: HouseTask, nextStatus: String) {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    if (isLocalAdminSession) {
      val now = System.currentTimeMillis()
      val effectiveStatus = if (nextStatus == "RESET") "ACTIVE" else nextStatus
      val startMs = if (effectiveStatus == "ACTIVE") now else task.startTimeMillis
      val endMs = if (effectiveStatus == "ACTIVE") now + task.durationSeconds * 1000L else task.endTimeMillis
      localTasksFlow.value = localTasksFlow.value.map {
        if (it.id == task.id) it.copy(status = effectiveStatus, startTimeMillis = startMs, endTimeMillis = endMs) else it
      }
      _actionMessage.value = "Task '${task.title}' set to $effectiveStatus."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase6Repo.transitionTaskStatus(seasonId, task, nextStatus)
      _isBusy.value = false
      res.onSuccess { (startMs, endMs) ->
        val effectiveStatus = if (nextStatus == "RESET") "ACTIVE" else nextStatus
        val cleaned = if (nextStatus == "RESET") {
          val copy = _collected3dTaskNodes.value.toMutableMap()
          copy.remove(task.id)
          _collected3dTaskNodes.value = copy
          task.copy(status = effectiveStatus, startTimeMillis = startMs, endTimeMillis = endMs, winnerSummary = null)
        } else {
          task.copy(status = effectiveStatus, startTimeMillis = startMs, endTimeMillis = endMs)
        }
        rtdbRepo.publishActiveTaskSnapshot(seasonId, cleaned)
        _actionMessage.value = if (nextStatus == "RESET") {
          "Task '${task.title}' reset and restarted."
        } else {
          "Task '${task.title}' set to $effectiveStatus."
        }
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not update task status."
      }
    }
  }

  fun declareTaskWinner(task: HouseTask, overrideWinner: TaskSubmission? = null) {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    val members = (membersState.value as? UiState.Success)?.data.orEmpty()
    val submissions = activeTaskSubmissions.value
    if (isLocalAdminSession) {
      val summary = overrideWinner?.let { "Winner: ${it.displayName} (+${task.maxPoints} pts)" } ?: "Completed locally"
      localTasksFlow.value = localTasksFlow.value.map {
        if (it.id == task.id) it.copy(status = "COMPLETED", winnerSummary = summary) else it
      }
      _actionMessage.value = "Result declared: $summary"
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val expected = phase6Repo.fetchAdminExpectedAnswer(seasonId, task.id)
      val res = phase6Repo.gradeAndDeclareTaskWinner(
        seasonId = seasonId,
        task = task,
        submissions = submissions,
        expectedAnswer = expected,
        overrideWinnerSub = overrideWinner,
        teams = teamsState.value,
        members = members,
      )
      _isBusy.value = false
      res.onSuccess { summary ->
        rtdbRepo.publishActiveTaskSnapshot(
          seasonId,
          task.copy(status = "COMPLETED", winnerSummary = summary),
        )
        _actionMessage.value = "Result declared: $summary"
      }.onFailure { err ->
        _actionError.value = err.message ?: "Failed to declare task winner."
      }
    }
  }

  fun submitMyTaskAnswer(task: HouseTask, answerText: String) {
    val account = (accountState.value as? UiState.Success)?.data ?: return
    if (account.banned) {
      _actionError.value = "Banned accounts cannot participate in house tasks."
      return
    }
    if (isLocalAdminSession) {
      val sub = TaskSubmission(
        id = "sub_${UUID.randomUUID().toString().take(8)}",
        seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
        taskId = task.id,
        userId = currentUserId,
        playerId = account.playerId,
        displayName = account.displayName,
        answerText = answerText,
      )
      localTaskSubmissionsFlow.value = listOf(sub) + localTaskSubmissionsFlow.value
      _actionMessage.value = "Submitted entry locally for '${task.title}'."
      return
    }
    val myTeamId = teamsState.value.firstOrNull { it.memberUids.contains(currentUserId) }?.id
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase6Repo.submitPlayerTaskEntry(
        task = task,
        account = account,
        teamId = myTeamId,
        answerText = answerText,
      )
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "Submitted entry for '${task.title}'. Waiting for Admin verification."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Submission rejected."
      }
    }
  }

  fun saveHouseTeam(
    teamId: String? = null,
    name: String,
    colorHex: String,
    memberUids: List<String>,
    score: Long,
    winsCount: Long,
  ) {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    if (isLocalAdminSession) {
      val id = teamId ?: "team_${UUID.randomUUID().toString().take(8)}"
      val saved = HouseTeam(
        id = id,
        seasonId = seasonId,
        name = name,
        colorHex = colorHex,
        memberUids = memberUids,
        score = score,
        winsCount = winsCount,
        updatedBy = currentUserId,
      )
      localTeamsFlow.value = listOf(saved) + localTeamsFlow.value.filterNot { it.id == id }
      _actionMessage.value = "Team '$name' updated locally (${memberUids.size} players)."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase6Repo.upsertTeam(
        seasonId = seasonId,
        teamId = teamId,
        name = name,
        colorHex = colorHex,
        memberUids = memberUids,
        score = score,
        winsCount = winsCount,
      )
      _isBusy.value = false
      res.onSuccess { savedId ->
        rtdbRepo.publishTeamSnapshot(
          seasonId,
          HouseTeam(
            id = savedId,
            seasonId = seasonId,
            name = name,
            colorHex = colorHex,
            memberUids = memberUids,
            score = score,
            winsCount = winsCount,
          ),
        )
        _actionMessage.value = "Team '$name' synchronized (${memberUids.size} players)."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not save team."
      }
    }
  }

  fun assignHouseCaptain(
    member: SeasonMember,
    title: String,
    scoreAwarded: Long = 100L,
    taskId: String? = null,
  ) {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    val currentSeason = (seasonState.value as? UiState.Success)?.data
    val powers = CaptainPowersConfig.fromSeason(currentSeason)
    if (isLocalAdminSession) {
      assignOrReplaceCaptainWithPowers(member, title, powers)
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = if (taskId == null) {
        phase11Repo.assignOrReplaceCaptain(
          seasonId = seasonId,
          member = member,
          title = title,
          powers = powers,
          scoreAwarded = scoreAwarded,
        )
      } else {
        phase6Repo.assignCaptain(
          seasonId = seasonId,
          targetUid = member.userId,
          playerId = member.playerId,
          displayName = member.displayName,
          characterId = member.characterId,
          title = title,
          scoreAwarded = scoreAwarded,
          taskId = taskId,
        )
      }
      _isBusy.value = false
      res.onSuccess {
        rtdbRepo.publishCaptainSnapshot(
          seasonId = seasonId,
          uid = member.userId,
          playerId = member.playerId,
          displayName = member.displayName,
          title = title,
          vipAccess = powers.vipRoomAccess,
        )
        _actionMessage.value = "${member.displayName} is now House Captain!"
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not assign House Captain."
      }
    }
  }

  fun saveHouseRule(
    ruleId: String? = null,
    ruleNumber: Long,
    title: String,
    description: String,
    severity: String,
    active: Boolean,
  ) {
    if (!requireAdminAction()) return
    if (isLocalAdminSession) {
      val id = ruleId ?: "rule_${UUID.randomUUID().toString().take(8)}"
      val saved = HouseRule(
        id = id,
        seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
        ruleNumber = ruleNumber,
        title = title,
        description = description,
        severity = severity,
        active = active,
        updatedBy = currentUserId,
      )
      localHouseRulesFlow.value = (localHouseRulesFlow.value.filterNot { it.id == id } + saved).sortedBy { it.ruleNumber }
      _actionMessage.value = "House Rule saved locally."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase6Repo.upsertHouseRule(
        ruleId = ruleId,
        ruleNumber = ruleNumber,
        title = title,
        description = description,
        severity = severity,
        active = active,
      )
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "House Rule saved."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Failed to save house rule."
      }
    }
  }

  fun issueRuleWarning(
    rule: HouseRule,
    targetMember: SeasonMember,
    warningLevel: String,
    note: String,
  ) {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    if (isLocalAdminSession) {
      val viol = RuleViolation(
        id = "viol_${UUID.randomUUID().toString().take(8)}",
        seasonId = seasonId,
        ruleId = rule.id.ifBlank { "rule_general" },
        ruleTitle = rule.title.ifBlank { "House Rule" },
        targetUid = targetMember.userId,
        targetPlayerId = targetMember.playerId,
        targetDisplayName = targetMember.displayName,
        warningLevel = warningLevel,
        note = note,
        issuedBy = currentUserId,
      )
      localRuleViolationsFlow.value = listOf(viol) + localRuleViolationsFlow.value
      _actionMessage.value = "$warningLevel issued locally to ${targetMember.displayName}."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase6Repo.issueRuleViolationWarning(
        seasonId = seasonId,
        ruleId = rule.id.ifBlank { "rule_general" },
        ruleTitle = rule.title.ifBlank { "House Rule" },
        targetUid = targetMember.userId,
        targetPlayerId = targetMember.playerId,
        targetDisplayName = targetMember.displayName,
        warningLevel = warningLevel,
        note = note,
      )
      _isBusy.value = false
      res.onSuccess { violId ->
        rtdbRepo.publishRuleWarning(
          seasonId = seasonId,
          targetUid = targetMember.userId,
          violationId = violId,
          ruleTitle = rule.title,
          warningLevel = warningLevel,
          note = note,
        )
        _actionMessage.value = "$warningLevel issued to ${targetMember.displayName}."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not issue rule warning."
      }
    }
  }

  fun saveScheduleSlot(slot: DailyScheduleSlot) {
    if (!requireAdminAction()) return
    if (isLocalAdminSession) {
      localDailyScheduleFlow.value = localDailyScheduleFlow.value.map {
        if (it.id == slot.id) slot else it
      }
      _actionMessage.value = "Updated '${slot.title}' locally."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase6Repo.upsertScheduleSlot(slot = slot)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "Updated '${slot.title}'."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not update schedule slot."
      }
    }
  }

  fun publishDailySchedule(activeSlotId: String, broadcastUpdate: Boolean) {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    val slots = dailyScheduleState.value
    if (isLocalAdminSession) {
      localDailyScheduleFlow.value = slots.map { it.copy(isCurrent = it.id == activeSlotId) }
      val activeSlot = slots.firstOrNull { it.id == activeSlotId }
      if (broadcastUpdate && activeSlot != null) {
        sendAnnouncement(
          message = "House Schedule Update: ${activeSlot.title} at ${activeSlot.timeLabel} in ${activeSlot.location}.",
          presetId = "schedule_update",
          languageTag = "en-IN",
          voiceGender = VoiceGender.FEMALE,
        )
      }
      _actionMessage.value = "Daily house schedule published locally."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase6Repo.publishScheduleAndMarkCurrent(seasonId, slots, activeSlotId)
      _isBusy.value = false
      res.onSuccess {
        val activeSlot = slots.firstOrNull { it.id == activeSlotId }
        rtdbRepo.publishScheduleSnapshot(seasonId, activeSlot)
        if (broadcastUpdate && activeSlot != null) {
          sendAnnouncement(
            message = "House Schedule Update: ${activeSlot.title} at ${activeSlot.timeLabel} in ${activeSlot.location}.",
            presetId = "schedule_update",
            languageTag = "en-IN",
            voiceGender = VoiceGender.FEMALE,
          )
        }
        _actionMessage.value = "Daily house schedule published."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Failed to publish daily schedule."
      }
    }
  }

  fun uploadCctvRecordingFromBridge(
    cameraId: String,
    cameraLabel: String,
    startedAtMillis: Long,
    durationMillis: Long,
    mimeType: String,
    base64Data: String,
  ) {
    if (!requireAdminAction()) return
    if (base64Data.isBlank()) {
      _actionError.value = "Captured video stream was empty."
      return
    }
    viewModelScope.launch {
      _isUploadingCctv.value = true
      val isLocalOnly = isLocalAdminSession || Firebase.auth.currentUser == null
      _cctvUploadStatus.value = if (isLocalOnly) {
        "Saving $cameraLabel clip to Local Control Room Archive..."
      } else {
        "Uploading $cameraLabel to Firebase Storage..."
      }
      val res = try {
        val bytes = withContext(Dispatchers.Default) {
          Base64.decode(base64Data, Base64.DEFAULT)
        }
        if (isLocalOnly) {
          val recId = "local_cctv_${UUID.randomUUID().toString().take(8)}"
          val safeCam = cameraId.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "cam" }
          val savedUri = withContext(Dispatchers.IO) {
            val ctx = appContext
            if (ctx != null) {
              val outFile = File(ctx.cacheDir, "cctv_${safeCam}_${startedAtMillis}.webm")
              outFile.writeBytes(bytes)
              outFile.toURI().toString()
            } else ""
          }
          val rec = CctvRecordingRecord(
            id = recId,
            seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
            cameraId = cameraId,
            cameraLabel = cameraLabel,
            startedAtMillis = startedAtMillis,
            durationMillis = durationMillis,
            mimeType = mimeType,
            downloadUrl = savedUri,
            storagePath = "local_cache/$safeCam",
            sizeBytes = bytes.size.toLong(),
            recordedBy = currentUserId,
          )
          localCctvRecordingsFlow.value = listOf(rec) + localCctvRecordingsFlow.value
          Result.success(rec)
        } else {
          firestoreRepo.uploadCctvRecording(
            cameraId = cameraId,
            cameraLabel = cameraLabel,
            startedAtMillis = startedAtMillis,
            durationMillis = durationMillis,
            mimeType = mimeType,
            videoBytes = bytes,
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Result.failure(e)
      }
      _isUploadingCctv.value = false
      res.onSuccess { rec ->
        val label = if (isLocalOnly) "Local Control Room Archive" else "Cloud Archive"
        _cctvUploadStatus.value = "Saved ${rec.cameraLabel} (${rec.durationMillis / 1000}s) to $label."
        _actionMessage.value = _cctvUploadStatus.value
      }.onFailure { err ->
        val msg = err.message ?: "Failed to save CCTV recording."
        _cctvUploadStatus.value = "Save failed: $msg"
        _actionError.value = msg
      }
    }
  }

  fun reportCctvRecordingError(message: String) {
    _cctvUploadStatus.value = message
    _actionError.value = message
  }

  fun deleteCctvRecording(record: CctvRecordingRecord) {
    if (!requireAdminAction()) return
    if (isLocalAdminSession) {
      localCctvRecordingsFlow.value = localCctvRecordingsFlow.value.filterNot { it.id == record.id }
      _actionMessage.value = "Deleted local recording ${record.cameraLabel}."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      val res = firestoreRepo.deleteCctvRecording(record = record)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "Deleted recording ${record.cameraLabel}."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not delete CCTV recording."
      }
    }
  }

  // --- PHASE 7: KITCHEN, RATIONS & VIRTUAL COOKING ---

  fun collectRation(itemId: String, amount: Long) {
    val account = (accountState.value as? UiState.Success)?.data ?: return
    if (account.banned) {
      _actionError.value = "Banned accounts cannot collect house rations."
      return
    }
    if (isLocalAdminSession) {
      val target = localRationsFlow.value.firstOrNull { it.itemId == itemId } ?: return
      val collectQty = amount.coerceIn(1L, target.quantity.coerceAtLeast(0L))
      if (!target.permitted || collectQty <= 0L) {
        _actionError.value = "${target.name} is unavailable right now."
        return
      }
      localRationsFlow.value = localRationsFlow.value.map {
        if (it.itemId == itemId) it.copy(quantity = (it.quantity - collectQty).coerceAtLeast(0L)) else it
      }
      val profile = localKitchenProfileFlow.value ?: KitchenPlayerState(id = currentUserId, userId = currentUserId, displayName = account.displayName)
      val bag = profile.bagItems.toMutableMap()
      bag[itemId] = (bag[itemId] ?: 0L) + collectQty
      localKitchenProfileFlow.value = profile.copy(bagItems = bag, lastCollectedAtMillis = System.currentTimeMillis())
      _actionMessage.value = "Collected $collectQty x ${target.name} (local)."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase7Repo.collectRationIngredient(
        itemId = itemId,
        amount = amount,
        account = account,
      )
      _isBusy.value = false
      res.onSuccess { msg ->
        _actionMessage.value = msg
        val activeTask = activeHouseTask.value
        if (activeTask != null && activeTask.activityType == TaskActivityType.COOKING.name) {
          record3dTaskNodeCollected(activeTask.id, "kitchen_collect")
        }
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not collect ration item."
      }
    }
  }

  fun startCooking(stationId: String, recipe: VirtualRecipe) {
    val account = (accountState.value as? UiState.Success)?.data ?: return
    if (account.banned) {
      _actionError.value = "Banned accounts cannot cook in the house kitchen."
      return
    }
    if (isLocalAdminSession) {
      val now = System.currentTimeMillis()
      val session = CookingSession(
        id = "local_cook_${now}",
        stationId = stationId,
        recipeId = recipe.recipeId,
        recipeName = recipe.name,
        chefUid = currentUserId,
        chefDisplayName = account.displayName,
        status = "COOKING",
        ingredientsUsed = recipe.ingredients,
        startedAtMillis = now,
        readyAtMillis = now + recipe.cookDurationSeconds * 1000L,
        energyRestore = recipe.energyRestore,
        servingsTotal = recipe.servings,
        servingsRemaining = recipe.servings,
        updatedBy = currentUserId,
      )
      localCookingSessionsFlow.value = listOf(session) + localCookingSessionsFlow.value
      triggerKitchenPose("cook")
      _actionMessage.value = "Cooking ${recipe.name} locally (${recipe.cookDurationSeconds}s)..."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase7Repo.startVirtualCooking(
        stationId = stationId,
        recipe = recipe,
        account = account,
      )
      _isBusy.value = false
      res.onSuccess { session ->
        rtdbRepo.publishKitchenStationLive(stationId, "COOKING", account.displayName, recipe.name)
        triggerKitchenPose("cook")
        _actionMessage.value = "Cooking ${recipe.name} at station (${recipe.cookDurationSeconds}s)..."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not start virtual cooking."
      }
    }
  }

  fun completeCooking(session: CookingSession) {
    val account = (accountState.value as? UiState.Success)?.data ?: return
    if (isLocalAdminSession) {
      localCookingSessionsFlow.value = localCookingSessionsFlow.value.map {
        if (it.id == session.id) it.copy(status = "READY") else it
      }
      _actionMessage.value = "${session.recipeName} is ready to serve (local)!"
      return
    }
    val myTeamId = teamsState.value.firstOrNull { it.memberUids.contains(currentUserId) }?.id
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase7Repo.completeVirtualCooking(
        session = session,
        account = account,
        teamId = myTeamId,
      )
      _isBusy.value = false
      res.onSuccess { msg ->
        rtdbRepo.publishKitchenStationLive(session.stationId, "READY", account.displayName, session.recipeName)
        val activeTask = activeHouseTask.value
        if (activeTask != null && activeTask.activityType == TaskActivityType.COOKING.name) {
          record3dTaskNodeCollected(activeTask.id, "kitchen_cook")
        }
        _actionMessage.value = msg
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not finish cooking."
      }
    }
  }

  fun claimAndEatMeal(session: CookingSession, seated: Boolean = false) {
    val account = (accountState.value as? UiState.Success)?.data ?: return
    if (account.banned) {
      _actionError.value = "Banned accounts cannot claim house meals."
      return
    }
    if (isLocalAdminSession) {
      val remaining = (session.servingsRemaining - 1L).coerceAtLeast(0L)
      localCookingSessionsFlow.value = localCookingSessionsFlow.value.map {
        if (it.id == session.id) {
          it.copy(
            servingsRemaining = remaining,
            status = if (remaining <= 0L) "CLAIMED" else it.status,
            claimedByUids = (it.claimedByUids + currentUserId).distinct(),
          )
        } else it
      }
      val profile = localKitchenProfileFlow.value ?: KitchenPlayerState(id = currentUserId, userId = currentUserId, displayName = account.displayName)
      val newEnergy = (profile.energy + session.energyRestore).coerceIn(0L, 100L)
      val newHunger = (profile.hunger - session.energyRestore).coerceIn(0L, 100L)
      localKitchenProfileFlow.value = profile.copy(energy = newEnergy, hunger = newHunger, lastEatAtMillis = System.currentTimeMillis())
      triggerKitchenPose(if (seated) "sit_eat" else "eat")
      _actionMessage.value = "Enjoyed ${session.recipeName}! Energy $newEnergy% • Hunger $newHunger%"
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase7Repo.claimAndEatMeal(
        session = session,
        account = account,
      )
      _isBusy.value = false
      res.onSuccess { (energy, hunger) ->
        rtdbRepo.publishKitchenStationLive(session.stationId, "SERVED", account.displayName, session.recipeName)
        triggerKitchenPose(if (seated) "sit_eat" else "eat")
        val activeTask = activeHouseTask.value
        if (activeTask != null && activeTask.activityType == TaskActivityType.COOKING.name) {
          record3dTaskNodeCollected(activeTask.id, "kitchen_serve")
        }
        _actionMessage.value = "Enjoyed ${session.recipeName}! Energy ${energy}% • Hunger ${hunger}%"
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not claim meal."
      }
    }
  }

  fun adminReplenishRation(item: RationItem, addAmount: Long = 10L) {
    if (!requireAdminAction()) return
    if (isLocalAdminSession) {
      localRationsFlow.value = localRationsFlow.value.map {
        if (it.itemId == item.itemId) it.copy(quantity = (it.quantity + addAmount).coerceAtMost(it.maxQuantity)) else it
      }
      _actionMessage.value = "Replenished ${item.name} (+$addAmount)."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase7Repo.adminReplenishRation(item = item, addAmount = addAmount)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "Replenished ${item.name} (+$addAmount)."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not replenish ration."
      }
    }
  }

  fun adminReplenishAllRations() {
    if (!requireAdminAction()) return
    if (isLocalAdminSession) {
      localRationsFlow.value = localRationsFlow.value.map { it.copy(quantity = it.maxQuantity) }
      _actionMessage.value = "Full house pantry replenished!"
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase7Repo.adminReplenishAllRations(items = rationsState.value)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "Full house pantry replenished!"
      }.onFailure { err ->
        _actionError.value = err.message ?: "Failed to replenish full pantry."
      }
    }
  }

  fun adminToggleRationPermission(item: RationItem, permitted: Boolean) {
    if (!requireAdminAction()) return
    if (isLocalAdminSession) {
      localRationsFlow.value = localRationsFlow.value.map {
        if (it.itemId == item.itemId) it.copy(permitted = permitted) else it
      }
      _actionMessage.value = "${item.name} is now ${if (permitted) "Permitted" else "Restricted"}."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase7Repo.adminToggleRationPermission(item = item, permitted = permitted)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "${item.name} is now ${if (permitted) "Permitted" else "Restricted"}."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Failed to toggle ration permission."
      }
    }
  }

  fun adminManageCookingSession(session: CookingSession, nextStatus: String) {
    if (!requireAdminAction()) return
    if (isLocalAdminSession) {
      localCookingSessionsFlow.value = localCookingSessionsFlow.value.map {
        if (it.id == session.id) it.copy(status = nextStatus) else it
      }
      _actionMessage.value = "Session '${session.recipeName}' marked $nextStatus."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase7Repo.adminManageCookingSession(session = session, nextStatus = nextStatus)
      _isBusy.value = false
      res.onSuccess {
        rtdbRepo.publishKitchenStationLive(session.stationId, nextStatus, session.chefDisplayName, session.recipeName)
        _actionMessage.value = "Session '${session.recipeName}' marked $nextStatus."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not manage cooking session."
      }
    }
  }

  // --- PHASE 8: ATOMIC TASK REWARD COMPLETION ---

  fun completeAndClaimTask(task: HouseTask, proofText: String) {
    val account = (accountState.value as? UiState.Success)?.data ?: return
    if (account.banned) {
      _actionError.value = "Banned accounts cannot claim task rewards."
      return
    }
    if (isLocalAdminSession) {
      _actionMessage.value = "Completed '${task.title}' locally."
      return
    }
    val myTeamId = teamsState.value.firstOrNull { it.memberUids.contains(currentUserId) }?.id
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase7Repo.completeAndClaimTaskReward(
        task = task,
        account = account,
        teamId = myTeamId,
        proofText = proofText,
      )
      _isBusy.value = false
      res.onSuccess { pts ->
        rtdbRepo.publishTaskProgressLive(task.id, 100L)
        _actionMessage.value = "Completed '${task.title}' and earned +$pts House Points!"
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not complete and claim task reward."
      }
    }
  }

  // --- PHASE 9: CHATGPT AI CONTROL ROOM ACTIONS ---

  fun setAiEnabled(enabled: Boolean) {
    if (!requireAdminAction()) return
    if (isLocalAdminSession) {
      val cur = localAiConfigFlow.value ?: AiControlConfig()
      localAiConfigFlow.value = cur.copy(
        enabled = enabled,
        emergencyStopped = if (enabled) false else cur.emergencyStopped,
        updatedBy = currentUserId,
      )
      _actionMessage.value = if (enabled) "ChatGPT AI Operator turned ON." else "ChatGPT AI Operator turned OFF."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase9Repo.updateAiConfig(
        enabled = enabled,
        clearEmergencyStop = enabled,
      )
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = if (enabled) "ChatGPT AI Operator turned ON." else "ChatGPT AI Operator turned OFF."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not toggle AI state."
      }
    }
  }

  fun setAiMode(mode: AiControlMode) {
    if (!requireAdminAction()) return
    if (isLocalAdminSession) {
      val cur = localAiConfigFlow.value ?: AiControlConfig()
      localAiConfigFlow.value = cur.copy(
        mode = mode.name,
        emergencyStopped = if (mode != AiControlMode.MANUAL) false else cur.emergencyStopped,
        updatedBy = currentUserId,
      )
      _actionMessage.value = "AI Operating Mode switched to ${mode.label}."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase9Repo.updateAiConfig(
        mode = mode,
        clearEmergencyStop = mode != AiControlMode.MANUAL,
      )
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "AI Operating Mode switched to ${mode.label}."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not switch AI mode."
      }
    }
  }

  fun setAiAutomationModules(
    autoHindi: Boolean,
    autoKitchen: Boolean,
    autoTasks: Boolean,
    autoTimer: Boolean,
  ) {
    if (!requireAdminAction()) return
    if (isLocalAdminSession) {
      val cur = localAiConfigFlow.value ?: AiControlConfig()
      localAiConfigFlow.value = cur.copy(
        autoHindiAnnouncements = autoHindi,
        autoKitchenMonitor = autoKitchen,
        autoTaskCoordinator = autoTasks,
        autoTimerMonitor = autoTimer,
        updatedBy = currentUserId,
      )
      _actionMessage.value = "AI module preferences updated."
      return
    }
    viewModelScope.launch {
      phase9Repo.updateAiConfig(
        autoHindiAnnouncements = autoHindi,
        autoKitchenMonitor = autoKitchen,
        autoTaskCoordinator = autoTasks,
        autoTimerMonitor = autoTimer,
      )
      _actionMessage.value = "AI module preferences updated."
    }
  }

  fun triggerAiEmergencyStop() {
    if (!requireAdminAction()) return
    val pendingIds = aiProposalsState.value.filter { it.status == "PENDING" }.map { it.id }
    if (isLocalAdminSession) {
      val cur = localAiConfigFlow.value ?: AiControlConfig()
      localAiConfigFlow.value = cur.copy(enabled = false, emergencyStopped = true, mode = AiControlMode.MANUAL.name)
      localAiProposalsFlow.value = localAiProposalsFlow.value.map {
        if (it.id in pendingIds) it.copy(status = "CANCELLED") else it
      }
      _actionMessage.value = "EMERGENCY STOP activated: AI disabled & automation halted."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase9Repo.triggerEmergencyStop(pendingProposalIds = pendingIds)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "EMERGENCY STOP activated: AI disabled & automation halted."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Emergency stop failed."
      }
    }
  }

  fun acknowledgeAiWhileAway() {
    if (!requireAdminAction()) return
    val unseenIds = aiLogsState.value.filter { !it.acknowledgedByAdmin }.map { it.id }
    if (isLocalAdminSession) {
      localAiLogsFlow.value = localAiLogsFlow.value.map { it.copy(acknowledgedByAdmin = true) }
      _actionMessage.value = "Offline AI activity summary acknowledged."
      return
    }
    viewModelScope.launch {
      phase9Repo.acknowledgeWhileAway(unseenLogIds = unseenIds)
      _actionMessage.value = "Offline AI activity summary acknowledged."
    }
  }

  fun createAiSchedule(
    actionType: AiActionType,
    title: String,
    messageOrPayload: String,
    delayMinutes: Long,
    recurrenceMinutes: Long = 0L,
  ) {
    if (!requireAdminAction()) return
    val targetTime = System.currentTimeMillis() + (delayMinutes.coerceAtLeast(0L) * 60_000L)
    if (isLocalAdminSession) {
      val ev = AiScheduledEvent(
        id = "sched_${UUID.randomUUID().toString().take(8)}",
        seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
        actionType = actionType.name,
        title = title.trim(),
        messageOrPayload = messageOrPayload.trim(),
        targetTimeMillis = targetTime,
        recurrenceMinutes = recurrenceMinutes,
        status = "PENDING",
        createdBy = currentUserId,
      )
      localAiSchedulesFlow.value = listOf(ev) + localAiSchedulesFlow.value
      _actionMessage.value = "AI schedule created locally."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase9Repo.createAiSchedule(
        actionType = actionType,
        title = title,
        messageOrPayload = messageOrPayload,
        targetTimeMillis = targetTime,
        recurrenceMinutes = recurrenceMinutes,
      )
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = if (delayMinutes == 0L) {
          "AI schedule queued as due immediately."
        } else {
          "Scheduled '${title.trim()}' in $delayMinutes min."
        }
      }.onFailure { err ->
        _actionError.value = err.message ?: "Failed to create AI schedule."
      }
    }
  }

  fun cancelAiSchedule(scheduleId: String) {
    if (!requireAdminAction()) return
    if (isLocalAdminSession) {
      localAiSchedulesFlow.value = localAiSchedulesFlow.value.map {
        if (it.id == scheduleId) it.copy(status = "CANCELLED") else it
      }
      _actionMessage.value = "AI schedule cancelled."
      return
    }
    viewModelScope.launch {
      phase9Repo.cancelAiSchedule(scheduleId = scheduleId)
      _actionMessage.value = "AI schedule cancelled."
    }
  }

  fun runAiAutomationTick(source: String = "ADMIN_TICK") {
    if (!requireAdminAction()) return
    viewModelScope.launch {
      _isBusy.value = true
      runAiAutomationTickInternal(source = source, silent = false)
      _isBusy.value = false
    }
  }

  private suspend fun runAiAutomationTickInternal(source: String, silent: Boolean) {
    if (!isAdminVerified.value) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    val cfg = aiConfigState.value ?: AiControlConfig(seasonId = seasonId)
    val members = (membersState.value as? UiState.Success)?.data.orEmpty()
    val existingKeys = aiLogsState.value.mapNotNull { it.executionKey.takeIf(String::isNotBlank) }.toSet()

    val eval = Phase9AiHouseManagerEngine.evaluateHouseTick(
      config = cfg,
      schedules = aiSchedulesState.value,
      existingExecutionKeys = existingKeys,
      rations = rationsState.value,
      tasks = tasksState.value,
      eligibleMembers = members,
      seasonId = seasonId,
      actorUid = currentUserId,
      source = source,
      nowMillis = System.currentTimeMillis(),
    )

    if (eval.skippedReason != null) {
      if (!silent) _actionMessage.value = eval.skippedReason
      return
    }

    if (isLocalAdminSession) {
      localAiLogsFlow.value = eval.logsToAppend + localAiLogsFlow.value
      localAiProposalsFlow.value = eval.proposalsToCreate + localAiProposalsFlow.value
      eval.directAnnouncements.forEach { (_, hindiMsg) ->
        sendAnnouncement(hindiMsg, "ai_house_manager", "hi-IN", VoiceGender.MALE)
      }
      if (!silent) {
        _actionMessage.value = "Local AI Tick: ${eval.logsToAppend.size} action(s), ${eval.proposalsToCreate.size} proposal(s)."
      }
      return
    }

    var executedCount = 0
    val isOffline = source == "OFFLINE_CATCHUP"

    eval.logsToAppend.forEach { logEntry ->
      val trans = eval.scheduleTransitions.firstOrNull {
        logEntry.executionKey.contains(it.scheduleId)
      }
      val claimed = phase9Repo.claimIdempotentLogAndTransitionSchedule(
        seasonId = seasonId,
        log = logEntry,
        scheduleTransition = trans,
        isOfflineCatchUp = isOffline,
      )
      if (!claimed) return@forEach

      executedCount += 1
      eval.directAnnouncements.filter {
        it.first == logEntry.executionKey || it.first == "${logEntry.executionKey}_ann"
      }.forEach { (_, hindiMsg) ->
        firestoreRepo.publishAnnouncement(
          seasonId = seasonId,
          message = hindiMsg,
          presetId = "ai_house_manager",
          languageTag = "hi-IN",
          voiceGender = VoiceGender.MALE,
        ).onSuccess { annId ->
          rtdbRepo.publishRtdbAnnouncement(seasonId, annId, hindiMsg, "hi-IN", VoiceGender.MALE.name)
        }
      }
    }

    // Also run transition-only updates if not already updated by a log
    eval.proposalsToCreate.forEach { prop ->
      phase9Repo.createAiProposal(seasonId = seasonId, proposal = prop)
    }

    eval.directTaskSuggestions.forEach { plan ->
      phase6Repo.upsertTask(
        seasonId = seasonId,
        title = plan.title,
        description = plan.description,
        instructions = plan.instructions,
        activityType = TaskActivityType.entries.firstOrNull { it.name == plan.activityType } ?: TaskActivityType.CHECKPOINT,
        assignmentMode = "HOUSE",
        isCaptaincy = false,
        targetRoom = plan.targetRoom,
        durationSeconds = plan.durationSeconds,
        promptQuestion = "",
        options = emptyList(),
        maxPoints = plan.maxPoints,
        expectedAnswer = "",
        startImmediately = true,
        category = TaskCategory.DAILY,
        difficulty = TaskDifficulty.entries.firstOrNull { it.name == plan.difficulty } ?: TaskDifficulty.MEDIUM,
        assignedUids = plan.assignedUids,
        requiredCheckpoints = plan.requiredCheckpoints,
      ).onSuccess { saved ->
        rtdbRepo.publishActiveTaskSnapshot(seasonId, saved)
      }
    }

    eval.directExpiredTasks.forEach { exp ->
      val task = tasksState.value.firstOrNull { it.id == exp.taskId }
      if (task != null) {
        phase6Repo.transitionTaskStatus(seasonId, task, "COMPLETED")
        rtdbRepo.publishActiveTaskSnapshot(seasonId, task.copy(status = "COMPLETED"))
      }
    }

    eval.directKitchenReplenishes.forEach { kit ->
      val ration = rationsState.value.firstOrNull { it.itemId == kit.itemId }
      if (ration != null) {
        phase7Repo.adminReplenishRation(seasonId, ration, kit.addQuantity)
      }
    }

    if (!silent) {
      _actionMessage.value = "AI Tick ($source): $executedCount action(s), ${eval.proposalsToCreate.size} proposal(s)."
    }
  }

  fun generateManualAiSuggestion() {
    if (!requireAdminAction()) return
    val members = (membersState.value as? UiState.Success)?.data.orEmpty()
    val proposal = Phase9AiHouseManagerEngine.buildHouseSuggestion(
      rations = rationsState.value,
      tasks = tasksState.value,
      eligibleMembers = members,
      nowMillis = System.currentTimeMillis(),
    )
    if (isLocalAdminSession) {
      val localProp = proposal.copy(id = "prop_${UUID.randomUUID().toString().take(8)}")
      localAiProposalsFlow.value = listOf(localProp) + localAiProposalsFlow.value
      _actionMessage.value = "ChatGPT AI created proposal: ${proposal.title}"
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      val res = phase9Repo.createAiProposal(proposal = proposal)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "ChatGPT AI created proposal: ${proposal.title}"
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not generate AI suggestion."
      }
    }
  }

  fun approveAiProposal(proposal: AiApprovalProposal) {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    if (isLocalAdminSession) {
      localAiProposalsFlow.value = localAiProposalsFlow.value.map {
        if (it.id == proposal.id) it.copy(status = "APPROVED", reviewedBy = currentUserId) else it
      }
      if (!proposal.hindiAnnouncement.isNullOrBlank()) {
        sendAnnouncement(proposal.hindiAnnouncement, "ai_approved", "hi-IN", VoiceGender.MALE)
      }
      _actionMessage.value = "Approved locally: ${proposal.title}"
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      val markRes = phase9Repo.markProposalReviewed(seasonId, proposal.id, approved = true)
      if (markRes.isSuccess) {
        if (!proposal.hindiAnnouncement.isNullOrBlank()) {
          firestoreRepo.publishAnnouncement(
            seasonId = seasonId,
            message = proposal.hindiAnnouncement,
            presetId = "ai_approved",
            languageTag = "hi-IN",
            voiceGender = VoiceGender.MALE,
          ).onSuccess { annId ->
            rtdbRepo.publishRtdbAnnouncement(seasonId, annId, proposal.hindiAnnouncement, "hi-IN", VoiceGender.MALE.name)
          }
        }
        val payload = proposal.payloadJson.orEmpty()
        if (payload.contains("REPLENISH_RATION")) {
          val lowItem = rationsState.value.firstOrNull { it.permitted && it.quantity <= it.minThreshold }
          if (lowItem != null) phase7Repo.adminReplenishRation(seasonId, lowItem, 8L)
        } else if (payload.contains("EXPIRE_TASK")) {
          val expTask = tasksState.value.firstOrNull { it.status in listOf("ACTIVE", "IN_PROGRESS", "ASSIGNED") && it.endTimeMillis > 0 && System.currentTimeMillis() >= it.endTimeMillis }
          if (expTask != null) {
            phase6Repo.transitionTaskStatus(seasonId, expTask, "COMPLETED")
            rtdbRepo.publishActiveTaskSnapshot(seasonId, expTask.copy(status = "COMPLETED"))
          }
        } else if (proposal.category == AiActionType.DAILY_TASK.name) {
          val members = (membersState.value as? UiState.Success)?.data.orEmpty()
          val preset = Phase6Catalog.taskPresets.first()
          phase6Repo.upsertTask(
            seasonId = seasonId,
            title = preset.title,
            description = preset.description,
            instructions = preset.instructions,
            activityType = preset.activityType,
            assignmentMode = preset.defaultMode,
            isCaptaincy = false,
            targetRoom = preset.targetRoom,
            durationSeconds = preset.defaultDurationSec,
            promptQuestion = preset.promptQuestion,
            options = preset.options,
            maxPoints = Phase8Logic.calculateRewardPoints(100L, preset.defaultDifficulty),
            expectedAnswer = preset.expectedAnswer,
            startImmediately = true,
            category = preset.defaultCategory,
            difficulty = preset.defaultDifficulty,
            assignedUids = members.filter { it.role != PlayerRole.ADMIN.name }.take(5).map { it.userId },
            requiredCheckpoints = preset.requiredCheckpoints,
          ).onSuccess { saved ->
            rtdbRepo.publishActiveTaskSnapshot(seasonId, saved)
          }
        }
        _actionMessage.value = "Approved & executed: ${proposal.title}"
      } else {
        _actionError.value = markRes.exceptionOrNull()?.message ?: "Could not approve proposal."
      }
      _isBusy.value = false
    }
  }

  fun rejectAiProposal(proposal: AiApprovalProposal) {
    if (!requireAdminAction()) return
    if (isLocalAdminSession) {
      localAiProposalsFlow.value = localAiProposalsFlow.value.map {
        if (it.id == proposal.id) it.copy(status = "REJECTED", reviewedBy = currentUserId) else it
      }
      _actionMessage.value = "Proposal rejected."
      return
    }
    viewModelScope.launch {
      phase9Repo.markProposalReviewed(proposalId = proposal.id, approved = false)
      _actionMessage.value = "Proposal rejected."
    }
  }

  // --- PHASE 10: PEER NOMINATIONS, CONFIGURABLE VOTING & CANCELLATION ---

  fun startConfiguredVotingOrNominationCycle(
    title: String,
    startStatus: String, // NOMINATIONS | OPEN
    nominees: List<NomineeSummary>,
    npcVotesEnabled: Boolean,
    voteType: String,
    nominationsPerPlayer: Long,
    votingDurationMinutes: Long,
    resultVisibility: String,
    tieBreakerPolicy: String,
  ) {
    if (!requireAdminAction()) return
    val now = System.currentTimeMillis()
    val endTime = if (startStatus == "OPEN") now + votingDurationMinutes.coerceIn(1L, 1440L) * 60_000L else 0L
    val nomEnd = if (startStatus == "NOMINATIONS") now + votingDurationMinutes.coerceIn(1L, 1440L) * 60_000L else 0L
    if (isLocalAdminSession) {
      val cycleId = "cyc_${UUID.randomUUID().toString().take(8)}"
      val cyc = VotingCycle(
        id = cycleId,
        seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
        title = title.trim().ifBlank { "Weekly Eviction" },
        status = startStatus,
        nominatedUids = nominees.map { it.uid },
        nomineeSummaries = nominees.map { it.encode() },
        npcVotesEnabled = npcVotesEnabled,
        voteType = voteType,
        nominationsPerPlayer = nominationsPerPlayer,
        nominationEndTimeMillis = nomEnd,
        votingDurationMinutes = votingDurationMinutes,
        votingEndTimeMillis = endTime,
        resultVisibility = resultVisibility,
        tieBreakerPolicy = tieBreakerPolicy,
        createdBy = currentUserId,
      )
      localVotingCyclesFlow.value = listOf(cyc) + localVotingCyclesFlow.value
      _selectedCycleId.value = cycleId
      if (startStatus == "OPEN" && nominees.isNotEmpty()) {
        phase12To20.publishNomineesToBiggGram(cyc, nominees)
      }
      _actionMessage.value = "Cycle '${cyc.title}' started locally ($startStatus • $voteType)."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = firestoreRepo.publishOrUpdateVotingCycle(
        title = title,
        status = startStatus,
        nominees = nominees,
        npcVotesEnabled = npcVotesEnabled,
        voteType = voteType,
        nominationsPerPlayer = nominationsPerPlayer,
        nominationEndTimeMillis = nomEnd,
        votingDurationMinutes = votingDurationMinutes,
        votingEndTimeMillis = endTime,
        resultVisibility = resultVisibility,
        tieBreakerPolicy = tieBreakerPolicy,
      )
      _isBusy.value = false
      res.onSuccess { cycleId ->
        _selectedCycleId.value = cycleId
        if (startStatus == "OPEN" && nominees.isNotEmpty()) {
          phase12To20.publishNomineesToBiggGram(
            VotingCycle(id = cycleId, title = title, status = startStatus, voteType = voteType),
            nominees,
          )
        }
        _actionMessage.value = "Cycle '${title.trim()}' started ($startStatus • $voteType)."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not start cycle."
      }
    }
  }

  fun advanceCycleToOpenVoting(
    cycle: VotingCycle,
    selectedNominees: List<NomineeSummary>,
  ) {
    if (!requireAdminAction()) return
    if (selectedNominees.isEmpty()) {
      _actionError.value = "Select at least 1 nominee before opening voting."
      return
    }
    val now = System.currentTimeMillis()
    val durationMins = cycle.votingDurationMinutes.coerceIn(1L, 1440L)
    if (isLocalAdminSession) {
      localVotingCyclesFlow.value = localVotingCyclesFlow.value.map {
        if (it.id != cycle.id) it else it.copy(
          status = "OPEN",
          nominatedUids = selectedNominees.map { n -> n.uid },
          nomineeSummaries = selectedNominees.map { n -> n.encode() },
          votingEndTimeMillis = now + durationMins * 60_000L,
        )
      }
      phase12To20.publishNomineesToBiggGram(cycle.copy(status = "OPEN"), selectedNominees)
      _actionMessage.value = "Voting is NOW OPEN for ${selectedNominees.size} nominee(s) & published to BiggGram."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = firestoreRepo.publishOrUpdateVotingCycle(
        cycleId = cycle.id,
        title = cycle.title,
        status = "OPEN",
        nominees = selectedNominees,
        npcVotesEnabled = cycle.npcVotesEnabled,
        voteType = cycle.voteType,
        nominationsPerPlayer = cycle.nominationsPerPlayer,
        nominationEndTimeMillis = cycle.nominationEndTimeMillis,
        votingDurationMinutes = durationMins,
        votingEndTimeMillis = now + durationMins * 60_000L,
        resultVisibility = cycle.resultVisibility,
        tieBreakerPolicy = cycle.tieBreakerPolicy,
      )
      _isBusy.value = false
      res.onSuccess {
        phase12To20.publishNomineesToBiggGram(cycle.copy(status = "OPEN"), selectedNominees)
        _actionMessage.value = "Voting is NOW OPEN for ${selectedNominees.size} nominee(s) ($durationMins min timer)."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Failed to open voting."
      }
    }
  }

  fun cancelVotingCycle(cycle: VotingCycle, reason: String) {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    val cleanReason = reason.trim().ifBlank { "Cancelled by Admin Control Room." }
    if (isLocalAdminSession) {
      localVotingCyclesFlow.value = localVotingCyclesFlow.value.map {
        if (it.id == cycle.id) it.copy(status = "CANCELLED", cancelledReason = cleanReason, votingEndTimeMillis = 0L) else it
      }
      val hindiMsg = Phase10VotingLogic.formatHindiCancelledAnnouncement(cycle.title, cleanReason)
      sendAnnouncement(hindiMsg, "voting_cancelled", "hi-IN", VoiceGender.FEMALE)
      _actionMessage.value = "Voting cycle cancelled & announced locally."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = firestoreRepo.publishOrUpdateVotingCycle(
        cycleId = cycle.id,
        title = cycle.title,
        status = "CANCELLED",
        nominees = cycle.parsedNominees(),
        npcVotesEnabled = cycle.npcVotesEnabled,
        voteType = cycle.voteType,
        nominationsPerPlayer = cycle.nominationsPerPlayer,
        nominationEndTimeMillis = cycle.nominationEndTimeMillis,
        votingDurationMinutes = cycle.votingDurationMinutes,
        votingEndTimeMillis = 0L,
        resultVisibility = cycle.resultVisibility,
        tieBreakerPolicy = cycle.tieBreakerPolicy,
        cancelledReason = cleanReason,
      )
      _isBusy.value = false
      res.onSuccess {
        val hindiMsg = Phase10VotingLogic.formatHindiCancelledAnnouncement(cycle.title, cleanReason)
        firestoreRepo.publishAnnouncement(
          seasonId = seasonId,
          message = hindiMsg,
          presetId = "voting_cancelled",
          languageTag = "hi-IN",
          voiceGender = VoiceGender.FEMALE,
        ).onSuccess { annId ->
          rtdbRepo.publishRtdbAnnouncement(seasonId, annId, hindiMsg, "hi-IN", VoiceGender.FEMALE.name)
        }
        _actionMessage.value = "Voting cycle cancelled & announced."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not cancel cycle."
      }
    }
  }

  fun submitMyPeerNomination(
    cycleId: String,
    targets: List<NomineeSummary>,
    reason: String?,
  ) {
    val account = (accountState.value as? UiState.Success)?.data ?: return
    if (account.banned) {
      _actionError.value = "Banned accounts cannot submit nominations."
      return
    }
    if (isLocalAdminSession) {
      _actionMessage.value = "Submitted ${targets.size} nomination(s) locally."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase9Repo.submitPeerNomination(
        cycleId = cycleId,
        voterDisplayName = account.displayName,
        targets = targets,
        reason = reason,
      )
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "Submitted ${targets.size} nomination(s)."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Nomination rejected."
      }
    }
  }

  // --- PHASE 10 & 11: SERVER VOTE TALLY, CAPTAINCY POWERS, VIP & ADMIN SECURITY ---

  fun assignOrReplaceCaptainWithPowers(
    member: SeasonMember,
    title: String,
    powers: CaptainPowersConfig,
  ) {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    if (isLocalAdminSession) {
      val cur = localSeasonFlow.value ?: SeasonSession(id = seasonId)
      localSeasonFlow.value = cur.copy(
        captainUid = member.userId,
        captainPlayerId = member.playerId,
        captainDisplayName = member.displayName,
        captainTitle = title,
        captainVipAccess = powers.vipRoomAccess,
        captainImmunity = powers.nominationImmunity,
        captainDoubleVote = powers.doubleVoteWeight,
        captainTieBreaker = powers.tieBreakerPower,
      )
      _actionMessage.value = "${member.displayName} assigned as House Captain locally."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase11Repo.assignOrReplaceCaptain(
        seasonId = seasonId,
        member = member,
        title = title,
        powers = powers,
      )
      _isBusy.value = false
      res.onSuccess {
        rtdbRepo.publishCaptainSnapshot(
          seasonId = seasonId,
          uid = member.userId,
          playerId = member.playerId,
          displayName = member.displayName,
          title = title,
          vipAccess = powers.vipRoomAccess,
        )
        _actionMessage.value = "${member.displayName} assigned as House Captain with custom powers."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not assign House Captain."
      }
    }
  }

  fun removeHouseCaptain() {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    val currentSeason = (seasonState.value as? UiState.Success)?.data
    if (isLocalAdminSession) {
      localSeasonFlow.value = (currentSeason ?: SeasonSession(id = seasonId)).copy(
        captainUid = null,
        captainPlayerId = null,
        captainDisplayName = null,
        captainTitle = null,
      )
      _actionMessage.value = "House Captain cleared locally."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase11Repo.removeCaptain(
        seasonId = seasonId,
        previousCaptainUid = currentSeason?.captainUid,
        previousCaptainName = currentSeason?.captainDisplayName,
      )
      _isBusy.value = false
      res.onSuccess {
        rtdbRepo.publishCaptainSnapshot(
          seasonId = seasonId,
          uid = "",
          playerId = "",
          displayName = "",
          title = "",
          vipAccess = false,
        )
        _actionMessage.value = "House Captain cleared and VIP Captain Room locked."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not remove House Captain."
      }
    }
  }

  fun updateCaptainPowers(powers: CaptainPowersConfig) {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    val currentSeason = (seasonState.value as? UiState.Success)?.data
    if (isLocalAdminSession) {
      localSeasonFlow.value = (currentSeason ?: SeasonSession(id = seasonId)).copy(
        captainVipAccess = powers.vipRoomAccess,
        captainImmunity = powers.nominationImmunity,
        captainDoubleVote = powers.doubleVoteWeight,
        captainTieBreaker = powers.tieBreakerPower,
      )
      _actionMessage.value = "Captain powers updated locally."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase11Repo.updateCaptainPowers(
        seasonId = seasonId,
        powers = powers,
        captainDisplayName = currentSeason?.captainDisplayName,
      )
      _isBusy.value = false
      res.onSuccess {
        if (!currentSeason?.captainUid.isNullOrBlank()) {
          rtdbRepo.publishCaptainSnapshot(
            seasonId = seasonId,
            uid = currentSeason?.captainUid.orEmpty(),
            playerId = currentSeason?.captainPlayerId.orEmpty(),
            displayName = currentSeason?.captainDisplayName.orEmpty(),
            title = currentSeason?.captainTitle ?: "House Captain",
            vipAccess = powers.vipRoomAccess,
          )
        }
        _actionMessage.value = "Captain powers updated."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not update Captain powers."
      }
    }
  }

  fun setPlayerVipPrivilege(
    member: SeasonMember,
    enabled: Boolean,
    vipLabel: String = "VIP PASS",
  ) {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    if (isLocalAdminSession) {
      val updated = VipPrivilege(
        id = member.userId,
        seasonId = seasonId,
        userId = member.userId,
        playerId = member.playerId,
        displayName = member.displayName,
        vipActive = enabled,
        vipEnabled = enabled,
        vipRoomAccess = enabled,
        vipLabel = vipLabel,
        grantedBy = currentUserId,
        updatedBy = currentUserId,
      )
      localVipPrivilegesFlow.value = listOf(updated) + localVipPrivilegesFlow.value.filterNot { it.userId == member.userId }
      _actionMessage.value = if (enabled) "Granted VIP Room Pass locally." else "Revoked VIP Room Pass locally."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase11Repo.setVipPrivilege(
        seasonId = seasonId,
        targetUid = member.userId,
        playerId = member.playerId,
        displayName = member.displayName,
        enabled = enabled,
        vipLabel = vipLabel,
      )
      _isBusy.value = false
      res.onSuccess {
        val currentVipUids = vipPrivilegesState.value
          .filter { it.userId != member.userId && it.effectiveVip }
          .map { it.userId }
          .let { list -> if (enabled) (list + member.userId).distinct() else list }
        rtdbRepo.publishVipPlayersMap(seasonId, currentVipUids)
        _actionMessage.value = if (enabled) {
          "Granted VIP Room Pass to ${member.displayName}."
        } else {
          "Revoked VIP Room Pass from ${member.displayName}."
        }
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not update VIP status."
      }
    }
  }

  fun saveGameSettings(settings: GameSettings) {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    if (isLocalAdminSession) {
      localGameSettingsFlow.value = settings.copy(seasonId = seasonId, updatedBy = currentUserId)
      localSeasonFlow.value = (localSeasonFlow.value ?: SeasonSession(id = seasonId)).copy(title = settings.houseTitle)
      _actionMessage.value = "House game settings saved locally."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = phase11Repo.saveGameSettings(seasonId = seasonId, settings = settings)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "House game settings saved."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Could not save game settings."
      }
    }
  }

  fun calculateAndCloseVotingOnServer(cycle: VotingCycle, closeVoting: Boolean) {
    if (!requireAdminAction()) return
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    val currentSeason = (seasonState.value as? UiState.Success)?.data
    val ballots = currentCycleVotesState.value
    val weighted = Phase11VipLogic.calculateWeightedVotingTally(cycle, ballots, currentSeason)
    if (isLocalAdminSession) {
      val tallyJson = Phase11VipLogic.serializeTallyRowsJson(weighted.weightedTallies)
      localVotingCyclesFlow.value = localVotingCyclesFlow.value.map {
        if (it.id != cycle.id) it else it.copy(
          status = if (closeVoting) "CLOSED" else it.status,
          serverTallyJson = tallyJson,
          serverTotalRealVotes = weighted.baseResult.totalRealVotes.toLong(),
          serverTotalNpcVotes = weighted.baseResult.totalNpcVotes.toLong(),
          serverIsTie = weighted.isTie,
          serverTalliedAtMillis = System.currentTimeMillis(),
        )
      }
      _actionMessage.value = if (closeVoting) {
        "Weighted tally calculated locally & voting closed."
      } else {
        "Weighted tally calculated locally (${weighted.baseResult.totalRealVotes} real, ${weighted.baseResult.totalNpcVotes} NPC)."
      }
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val fbRes = phase11Repo.writeTallyToVotingCycleFallback(
        seasonId = seasonId,
        cycle = cycle,
        weightedOutcome = weighted,
        closeVoting = closeVoting,
      )
      _isBusy.value = false
      fbRes.onSuccess {
        _actionMessage.value = "Weighted tally synced (${weighted.baseResult.totalRealVotes} real, ${weighted.baseResult.totalNpcVotes} NPC)."
      }.onFailure { fbErr ->
        _actionError.value = fbErr.message ?: "Vote tally failed."
      }
    }
  }

  fun changeAdminPassword(
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    onSuccessClear: () -> Unit = {},
  ) {
    if (!requireAdminAction()) return
    val ctx = appContext
    if (ctx == null) {
      _actionError.value = "Application context unavailable for local password update."
      return
    }
    val valErr = AdminAuthValidation.validatePasswordChange(currentPassword, newPassword, confirmPassword)
    if (valErr != null) {
      _actionError.value = valErr
      return
    }
    val seasonId = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val res = withContext(Dispatchers.Default) {
        LocalAdminAuthManager.changeAdminPassword(
          context = ctx,
          currentPassword = currentPassword,
          newPassword = newPassword,
          confirmPassword = confirmPassword,
        )
      }
      _isBusy.value = false
      res.onSuccess {
        if (isLocalAdminSession) {
          appendLocalAudit("ADMIN_PASSWORD_CHANGED", "Local Admin password updated inside APK.")
        } else {
          phase11Repo.appendAdminAuditLog(seasonId, "ADMIN_PASSWORD_CHANGED", "Local Admin password updated via Security Panel.")
        }
        onSuccessClear()
        _actionMessage.value = "Local Admin password updated on this device."
      }.onFailure { err ->
        _actionError.value = err.message ?: "Failed to update Admin password."
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
  }
}
