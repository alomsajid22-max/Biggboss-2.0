package com.example.multiplayer

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

private const val RTDB_TAG = "GlassHouseRTDB"

class RealtimeMultiplayerRepository(
  private val auth: FirebaseAuth = Firebase.auth,
) {
  private val databaseUrl: String? = try {
    FirebaseApp.getInstance().options.databaseUrl
  } catch (_: Exception) {
    null
  }

  val isConfigured: Boolean
    get() = !databaseUrl.isNullOrBlank()

  private val database: FirebaseDatabase? by lazy {
    if (!isConfigured) null
    else try {
      FirebaseDatabase.getInstance()
    } catch (e: Exception) {
      Log.w(RTDB_TAG, "Realtime Database initialization skipped: ${e.message}")
      null
    }
  }

  private val _remotePlayers = MutableStateFlow<Map<String, RemotePlayerState>>(emptyMap())
  val remotePlayers: StateFlow<Map<String, RemotePlayerState>> = _remotePlayers.asStateFlow()

  private val _doorStates = MutableStateFlow<Map<String, RemoteDoorState>>(emptyMap())
  val doorStates: StateFlow<Map<String, RemoteDoorState>> = _doorStates.asStateFlow()

  private val _seatStates = MutableStateFlow<Map<String, RemoteSeatState>>(emptyMap())
  val seatStates: StateFlow<Map<String, RemoteSeatState>> = _seatStates.asStateFlow()

  private val _lightingMode = MutableStateFlow<String?>(null)
  val lightingMode: StateFlow<String?> = _lightingMode.asStateFlow()

  private val _sessionSuperseded = MutableStateFlow(false)
  val sessionSuperseded: StateFlow<Boolean> = _sessionSuperseded.asStateFlow()

  private val _kickReason = MutableStateFlow<String?>(null)
  val kickReason: StateFlow<String?> = _kickReason.asStateFlow()

  private val _syncError = MutableStateFlow<String?>(null)
  val syncError: StateFlow<String?> = _syncError.asStateFlow()

  private val _realtimeActiveTask = MutableStateFlow<RealtimeTaskSnapshot?>(null)
  val realtimeActiveTask: StateFlow<RealtimeTaskSnapshot?> = _realtimeActiveTask.asStateFlow()

  private val _realtimeCaptain = MutableStateFlow<RealtimeCaptainSnapshot?>(null)
  val realtimeCaptain: StateFlow<RealtimeCaptainSnapshot?> = _realtimeCaptain.asStateFlow()

  private val _realtimeWarning = MutableStateFlow<RealtimeWarningSnapshot?>(null)
  val realtimeWarning: StateFlow<RealtimeWarningSnapshot?> = _realtimeWarning.asStateFlow()

  private val _kitchenLive = MutableStateFlow<Map<String, String>>(emptyMap())
  val kitchenLive: StateFlow<Map<String, String>> = _kitchenLive.asStateFlow()

  private val _taskProgressLive = MutableStateFlow<Map<String, Long>>(emptyMap())
  val taskProgressLive: StateFlow<Map<String, Long>> = _taskProgressLive.asStateFlow()

  private var attachedSeasonId: String? = null
  private var attachedUid: String? = null
  private var attachedSessionId: String? = null
  private var attachedIsAdmin: Boolean = false
  private var playersListener: ValueEventListener? = null
  private var doorsListener: ValueEventListener? = null
  private var seatsListener: ValueEventListener? = null
  private var lightingListener: ValueEventListener? = null
  private var sessionLockListener: ValueEventListener? = null
  private var kickListener: ValueEventListener? = null
  private var playersRef: DatabaseReference? = null
  private var doorsRef: DatabaseReference? = null
  private var seatsRef: DatabaseReference? = null
  private var lightingRef: DatabaseReference? = null
  private var sessionLockRef: DatabaseReference? = null
  private var kickRef: DatabaseReference? = null
  private var activeTaskListener: ValueEventListener? = null
  private var captainListener: ValueEventListener? = null
  private var warningListener: ValueEventListener? = null
  private var kitchenLiveListener: ValueEventListener? = null
  private var taskProgressLiveListener: ValueEventListener? = null
  private var activeTaskRef: DatabaseReference? = null
  private var captainRef: DatabaseReference? = null
  private var warningRef: DatabaseReference? = null
  private var kitchenLiveRef: DatabaseReference? = null
  private var taskProgressLiveRef: DatabaseReference? = null
  private var lastPublishMs: Long = 0L
  private var lastPublishedState: String = ""
  private var lastPublishedExpression: String = "neutral"

  suspend fun attachSeasonSync(
    seasonId: String,
    uid: String,
    account: UserAccount,
    characterId: String,
    sessionId: String,
    isAdmin: Boolean = false,
  ) {
    val db = database ?: return
    if (attachedUid == uid && attachedSeasonId == seasonId && attachedSessionId == sessionId && attachedIsAdmin == isAdmin) {
      return
    }
    detachSync()

    attachedSeasonId = seasonId
    attachedUid = uid
    attachedSessionId = sessionId
    attachedIsAdmin = isAdmin
    _sessionSuperseded.value = false
    _syncError.value = null

    val base = db.reference.child("seasons").child(seasonId)
    val myPlayerRef = base.child("players").child(uid)
    val myAdminPresenceRef = base.child("adminPresence").child(uid)
    val myLockRef = base.child("sessionLocks").child(uid)

    try {
      // Single-active-session lock + automatic disconnect cleanup
      myLockRef.setValue(
        mapOf(
          "sessionId" to sessionId,
          "updatedAt" to ServerValue.TIMESTAMP,
        )
      ).await()
      myLockRef.onDisconnect().removeValue()

      if (isAdmin) {
        // Secret Admin Identity: NEVER publish Admin into public players/ node
        runCatching { myPlayerRef.removeValue().await() }
        myAdminPresenceRef.onDisconnect().updateChildren(
          mapOf(
            "online" to false,
            "state" to "offline",
            "updatedAt" to ServerValue.TIMESTAMP,
          )
        )
        myAdminPresenceRef.setValue(
          mapOf(
            "uid" to uid,
            "x" to 19.4,
            "y" to 0.0,
            "z" to -2.8,
            "rotY" to 0.0,
            "state" to "idle",
            "online" to true,
            "sessionId" to sessionId,
            "updatedAt" to ServerValue.TIMESTAMP,
          )
        ).await()
      } else {
        myPlayerRef.onDisconnect().updateChildren(
          mapOf(
            "online" to false,
            "state" to "offline",
            "seatId" to null,
            "updatedAt" to ServerValue.TIMESTAMP,
          )
        )

        // Publish initial spawn snapshot
        myPlayerRef.setValue(
          mapOf(
            "uid" to uid,
            "playerId" to account.playerId,
            "displayName" to account.displayName,
            "characterId" to characterId,
            "x" to 0.0,
            "y" to 0.0,
            "z" to 6.0,
            "rotY" to 0.0,
            "state" to "idle",
            "expression" to "neutral",
            "online" to true,
            "sessionId" to sessionId,
            "updatedAt" to ServerValue.TIMESTAMP,
          )
        ).await()
      }
    } catch (e: Exception) {
      Log.w(RTDB_TAG, "Failed to register initial RTDB presence: ${e.message}")
      _syncError.value = e.localizedMessage
    }

    playersRef = base.child("players").also { ref ->
      val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val next = mutableMapOf<String, RemotePlayerState>()
          for (child in snapshot.children) {
            val pUid = child.key ?: continue
            if (pUid == uid) continue
            val online = child.child("online").getValue(Boolean::class.java) ?: false
            val stateStr = child.child("state").getValue(String::class.java) ?: "idle"
            if (!online || stateStr == "offline") continue
            next[pUid] = RemotePlayerState(
              uid = pUid,
              playerId = child.child("playerId").getValue(String::class.java) ?: pUid.take(6),
              displayName = child.child("displayName").getValue(String::class.java) ?: "Housemate",
              characterId = child.child("characterId").getValue(String::class.java) ?: "Player 01",
              x = (child.child("x").value as? Number)?.toDouble() ?: 0.0,
              y = (child.child("y").value as? Number)?.toDouble() ?: 0.0,
              z = (child.child("z").value as? Number)?.toDouble() ?: 6.0,
              rotY = (child.child("rotY").value as? Number)?.toDouble() ?: 0.0,
              state = stateStr,
              expression = child.child("expression").getValue(String::class.java) ?: "neutral",
              seatId = child.child("seatId").getValue(String::class.java),
              online = online,
              sessionId = child.child("sessionId").getValue(String::class.java) ?: "",
              updatedAt = (child.child("updatedAt").value as? Number)?.toLong() ?: 0L,
            )
          }
          _remotePlayers.value = next
        }

        override fun onCancelled(error: DatabaseError) {
          Log.w(RTDB_TAG, "RTDB players listener cancelled: ${error.message}")
          _syncError.value = error.message
        }
      }
      ref.addValueEventListener(listener)
      playersListener = listener
    }

    doorsRef = base.child("worldState").child("doors").also { ref ->
      val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val next = mutableMapOf<String, RemoteDoorState>()
          for (child in snapshot.children) {
            val doorId = child.key ?: continue
            next[doorId] = RemoteDoorState(
              doorId = doorId,
              open = child.child("isOpen").getValue(Boolean::class.java) ?: false,
              updatedBy = child.child("updatedBy").getValue(String::class.java) ?: "",
              updatedAt = (child.child("updatedAt").value as? Number)?.toLong() ?: 0L,
            )
          }
          _doorStates.value = next
        }

        override fun onCancelled(error: DatabaseError) {
          Log.w(RTDB_TAG, "RTDB doors listener cancelled: ${error.message}")
        }
      }
      ref.addValueEventListener(listener)
      doorsListener = listener
    }

    seatsRef = base.child("worldState").child("seats").also { ref ->
      val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val next = mutableMapOf<String, RemoteSeatState>()
          for (child in snapshot.children) {
            val seatId = child.key ?: continue
            next[seatId] = RemoteSeatState(
              seatId = seatId,
              occupiedByUid = child.child("occupiedByUid").getValue(String::class.java),
              occupiedByName = child.child("occupiedByName").getValue(String::class.java),
              updatedAt = (child.child("updatedAt").value as? Number)?.toLong() ?: 0L,
            )
          }
          _seatStates.value = next
        }

        override fun onCancelled(error: DatabaseError) {
          Log.w(RTDB_TAG, "RTDB seats listener cancelled: ${error.message}")
        }
      }
      ref.addValueEventListener(listener)
      seatsListener = listener
    }

    lightingRef = base.child("worldState").child("lighting").child("dayNightMode").also { ref ->
      val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val mode = snapshot.getValue(String::class.java)
          if (!mode.isNullOrBlank()) {
            _lightingMode.value = mode
          }
        }

        override fun onCancelled(error: DatabaseError) {}
      }
      ref.addValueEventListener(listener)
      lightingListener = listener
    }

    sessionLockRef = myLockRef.also { ref ->
      val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val currentLock = snapshot.child("sessionId").getValue(String::class.java)
          if (!currentLock.isNullOrBlank() && currentLock != sessionId) {
            _sessionSuperseded.value = true
          }
        }

        override fun onCancelled(error: DatabaseError) {}
      }
      ref.addValueEventListener(listener)
      sessionLockListener = listener
    }

    kickRef = base.child("kicks").child(uid).also { ref ->
      val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val reason = snapshot.child("reason").getValue(String::class.java)
          _kickReason.value = reason
        }

        override fun onCancelled(error: DatabaseError) {}
      }
      ref.addValueEventListener(listener)
      kickListener = listener
    }

    activeTaskRef = base.child("activeTask").also { ref ->
      val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          if (!snapshot.exists()) {
            _realtimeActiveTask.value = null
            return
          }
          _realtimeActiveTask.value = RealtimeTaskSnapshot(
            taskId = snapshot.child("taskId").getValue(String::class.java) ?: "",
            title = snapshot.child("title").getValue(String::class.java) ?: "",
            activityType = snapshot.child("activityType").getValue(String::class.java) ?: "QUIZ",
            assignmentMode = snapshot.child("assignmentMode").getValue(String::class.java) ?: "INDIVIDUAL",
            isCaptaincy = snapshot.child("isCaptaincy").getValue(Boolean::class.java) ?: false,
            status = snapshot.child("status").getValue(String::class.java) ?: "DRAFT",
            targetRoom = snapshot.child("targetRoom").getValue(String::class.java) ?: "Living Atrium",
            startTimeMillis = (snapshot.child("startTimeMillis").value as? Number)?.toLong() ?: 0L,
            endTimeMillis = (snapshot.child("endTimeMillis").value as? Number)?.toLong() ?: 0L,
            winnerSummary = snapshot.child("winnerSummary").getValue(String::class.java),
            updatedAt = (snapshot.child("updatedAt").value as? Number)?.toLong() ?: 0L,
          )
        }

        override fun onCancelled(error: DatabaseError) {}
      }
      ref.addValueEventListener(listener)
      activeTaskListener = listener
    }

    captainRef = base.child("captain").also { ref ->
      val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          if (!snapshot.exists()) {
            _realtimeCaptain.value = null
            return
          }
          _realtimeCaptain.value = RealtimeCaptainSnapshot(
            uid = snapshot.child("uid").getValue(String::class.java) ?: "",
            playerId = snapshot.child("playerId").getValue(String::class.java) ?: "",
            displayName = snapshot.child("displayName").getValue(String::class.java) ?: "",
            title = snapshot.child("title").getValue(String::class.java) ?: "House Captain",
            updatedAt = (snapshot.child("updatedAt").value as? Number)?.toLong() ?: 0L,
          )
        }

        override fun onCancelled(error: DatabaseError) {}
      }
      ref.addValueEventListener(listener)
      captainListener = listener
    }

    warningRef = base.child("ruleWarnings").child(uid).also { ref ->
      val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          if (!snapshot.exists()) {
            _realtimeWarning.value = null
            return
          }
          _realtimeWarning.value = RealtimeWarningSnapshot(
            id = snapshot.child("id").getValue(String::class.java) ?: "",
            ruleTitle = snapshot.child("ruleTitle").getValue(String::class.java) ?: "",
            warningLevel = snapshot.child("warningLevel").getValue(String::class.java) ?: "WARNING",
            note = snapshot.child("note").getValue(String::class.java) ?: "",
            updatedAt = (snapshot.child("updatedAt").value as? Number)?.toLong() ?: 0L,
          )
        }

        override fun onCancelled(error: DatabaseError) {}
      }
      ref.addValueEventListener(listener)
      warningListener = listener
    }

    kitchenLiveRef = base.child("kitchenLive").also { ref ->
      val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val next = mutableMapOf<String, String>()
          for (child in snapshot.children) {
            val stationId = child.key ?: continue
            val status = child.child("status").getValue(String::class.java) ?: "IDLE"
            next[stationId] = status
          }
          _kitchenLive.value = next
        }

        override fun onCancelled(error: DatabaseError) {}
      }
      ref.addValueEventListener(listener)
      kitchenLiveListener = listener
    }

    taskProgressLiveRef = base.child("taskProgressLive").also { ref ->
      val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val next = mutableMapOf<String, Long>()
          for (child in snapshot.children) {
            val playerUid = child.key ?: continue
            val pct = (child.child("percent").value as? Number)?.toLong() ?: 0L
            next[playerUid] = pct.coerceIn(0L, 100L)
          }
          _taskProgressLive.value = next
        }

        override fun onCancelled(error: DatabaseError) {}
      }
      ref.addValueEventListener(listener)
      taskProgressLiveListener = listener
    }
  }

  fun publishLocalTransform(
    account: UserAccount,
    characterId: String,
    x: Double,
    y: Double,
    z: Double,
    rotY: Double,
    state: String,
    seatId: String?,
    expression: String = "neutral",
  ) {
    val db = database ?: return
    val uid = auth.currentUser?.uid ?: return
    val seasonId = attachedSeasonId ?: return
    val sessionId = attachedSessionId ?: return
    if (_sessionSuperseded.value) return

    val safeExpr = if (expression in VALID_EXPRESSIONS) expression else "neutral"
    val now = System.currentTimeMillis()
    val stateChanged = state != lastPublishedState || safeExpr != lastPublishedExpression
    if (!stateChanged && (now - lastPublishMs) < 75L) return
    lastPublishMs = now
    lastPublishedState = state
    lastPublishedExpression = safeExpr

    val clampedX = x.coerceIn(-27.5, 27.5)
    val clampedY = y.coerceIn(-1.5, 4.5)
    val clampedZ = z.coerceIn(-25.5, 25.5)

    if (attachedIsAdmin) {
      val adminPayload = mapOf<String, Any?>(
        "uid" to uid,
        "x" to clampedX,
        "y" to clampedY,
        "z" to clampedZ,
        "rotY" to rotY,
        "state" to state,
        "online" to true,
        "sessionId" to sessionId,
        "updatedAt" to ServerValue.TIMESTAMP,
      )
      db.reference.child("seasons").child(seasonId).child("adminPresence").child(uid)
        .updateChildren(adminPayload)
      return
    }

    val payload = mutableMapOf<String, Any?>(
      "uid" to uid,
      "playerId" to account.playerId,
      "displayName" to account.displayName,
      "characterId" to characterId,
      "x" to clampedX,
      "y" to clampedY,
      "z" to clampedZ,
      "rotY" to rotY,
      "state" to state,
      "expression" to safeExpr,
      "seatId" to seatId,
      "online" to true,
      "sessionId" to sessionId,
      "updatedAt" to ServerValue.TIMESTAMP,
    )

    db.reference.child("seasons").child(seasonId).child("players").child(uid)
      .updateChildren(payload)
  }

  fun publishDoorState(doorId: String, isOpen: Boolean) {
    val db = database ?: return
    val uid = auth.currentUser?.uid ?: return
    val seasonId = attachedSeasonId ?: return
    db.reference.child("seasons").child(seasonId).child("worldState").child("doors").child(doorId)
      .setValue(
        mapOf(
          "isOpen" to isOpen,
          "updatedBy" to uid,
          "updatedAt" to ServerValue.TIMESTAMP,
        )
      )
  }

  fun publishSeatState(seatId: String, occupied: Boolean, displayName: String) {
    if (attachedIsAdmin || seatId.startsWith("admin_")) return
    val db = database ?: return
    val uid = auth.currentUser?.uid ?: return
    val seasonId = attachedSeasonId ?: return
    val seatRef = db.reference.child("seasons").child(seasonId).child("worldState").child("seats").child(seatId)
    if (occupied) {
      seatRef.setValue(
        mapOf(
          "occupiedByUid" to uid,
          "occupiedByName" to displayName,
          "updatedAt" to ServerValue.TIMESTAMP,
        )
      )
    } else {
      seatRef.removeValue()
    }
  }

  fun publishLightingMode(mode: String) {
    val db = database ?: return
    auth.currentUser?.uid ?: return
    val seasonId = attachedSeasonId ?: return
    db.reference.child("seasons").child(seasonId).child("worldState").child("lighting")
      .setValue(
        mapOf(
          "dayNightMode" to mode,
          "updatedAt" to ServerValue.TIMESTAMP,
        )
      )
  }

  fun publishKick(seasonId: String, targetUid: String, reason: String) {
    val db = database ?: return
    val adminUid = auth.currentUser?.uid ?: return
    val base = db.reference.child("seasons").child(seasonId)
    base.child("kicks").child(targetUid).setValue(
      mapOf(
        "reason" to reason.take(200),
        "issuedBy" to adminUid,
        "updatedAt" to ServerValue.TIMESTAMP,
      )
    )
    base.child("players").child(targetUid).updateChildren(
      mapOf(
        "online" to false,
        "state" to "offline",
        "seatId" to null,
        "updatedAt" to ServerValue.TIMESTAMP,
      )
    )
  }

  fun clearKick(seasonId: String, targetUid: String) {
    val db = database ?: return
    db.reference.child("seasons").child(seasonId).child("kicks").child(targetUid).removeValue()
  }

  fun publishRtdbAnnouncement(
    seasonId: String,
    announcementId: String,
    message: String,
    languageTag: String,
    voiceGender: String,
  ) {
    val db = database ?: return
    val adminUid = auth.currentUser?.uid ?: return
    db.reference.child("seasons").child(seasonId).child("announcements").child("latest").setValue(
      mapOf(
        "id" to announcementId,
        "message" to message.take(320),
        "languageTag" to languageTag,
        "voiceGender" to voiceGender,
        "issuedBy" to adminUid,
        "updatedAt" to ServerValue.TIMESTAMP,
      )
    )
  }

  fun publishActiveTaskSnapshot(seasonId: String, task: HouseTask) {
    val db = database ?: return
    db.reference.child("seasons").child(seasonId).child("activeTask").setValue(
      mapOf(
        "taskId" to task.id,
        "title" to task.title,
        "activityType" to task.activityType,
        "assignmentMode" to task.assignmentMode,
        "isCaptaincy" to task.isCaptaincy,
        "status" to task.status,
        "targetRoom" to task.targetRoom,
        "startTimeMillis" to task.startTimeMillis,
        "endTimeMillis" to task.endTimeMillis,
        "winnerSummary" to task.winnerSummary,
        "updatedAt" to ServerValue.TIMESTAMP,
      )
    )
  }

  fun publishTeamSnapshot(seasonId: String, team: HouseTeam) {
    val db = database ?: return
    db.reference.child("seasons").child(seasonId).child("teams").child(team.id).setValue(
      mapOf(
        "name" to team.name,
        "colorHex" to team.colorHex,
        "score" to team.score,
        "winsCount" to team.winsCount,
        "memberCount" to team.memberUids.size,
        "updatedAt" to ServerValue.TIMESTAMP,
      )
    )
  }

  fun publishCaptainSnapshot(
    seasonId: String,
    uid: String,
    playerId: String,
    displayName: String,
    title: String,
    vipAccess: Boolean = true,
  ) {
    val db = database ?: return
    db.reference.child("seasons").child(seasonId).child("captain").setValue(
      mapOf(
        "uid" to uid,
        "playerId" to playerId,
        "displayName" to displayName,
        "title" to title,
        "vipAccess" to vipAccess,
        "updatedAt" to ServerValue.TIMESTAMP,
      )
    )
  }

  fun publishVipPlayersMap(seasonId: String, vipUids: List<String>) {
    val db = database ?: return
    val map = vipUids.filter { it.isNotBlank() }.associateWith { true }
    db.reference.child("seasons").child(seasonId).child("vipPlayers").setValue(map)
  }

  fun publishRuleWarning(
    seasonId: String,
    targetUid: String,
    violationId: String,
    ruleTitle: String,
    warningLevel: String,
    note: String,
  ) {
    val db = database ?: return
    val adminUid = auth.currentUser?.uid ?: return
    db.reference.child("seasons").child(seasonId).child("ruleWarnings").child(targetUid).setValue(
      mapOf(
        "id" to violationId,
        "ruleTitle" to ruleTitle,
        "warningLevel" to warningLevel,
        "note" to note.take(240),
        "issuedBy" to adminUid,
        "updatedAt" to ServerValue.TIMESTAMP,
      )
    )
  }

  fun publishScheduleSnapshot(seasonId: String, activeSlot: DailyScheduleSlot?) {
    val db = database ?: return
    db.reference.child("seasons").child(seasonId).child("dailySchedule").setValue(
      mapOf(
        "currentSlotId" to (activeSlot?.id ?: ""),
        "currentTitle" to (activeSlot?.title ?: ""),
        "currentTimeLabel" to (activeSlot?.timeLabel ?: ""),
        "updatedAt" to ServerValue.TIMESTAMP,
      )
    )
  }

  fun publishKitchenStationLive(
    stationId: String,
    status: String,
    chefName: String,
    recipeName: String,
  ) {
    val db = database ?: return
    val uid = auth.currentUser?.uid ?: return
    val seasonId = attachedSeasonId ?: return
    db.reference.child("seasons").child(seasonId).child("kitchenLive").child(stationId).setValue(
      mapOf(
        "stationId" to stationId,
        "status" to status,
        "chefUid" to uid,
        "chefName" to chefName.take(24),
        "recipeName" to recipeName.take(60),
        "updatedAt" to ServerValue.TIMESTAMP,
      )
    )
  }

  fun publishTaskProgressLive(taskId: String, percent: Long) {
    val db = database ?: return
    val uid = auth.currentUser?.uid ?: return
    val seasonId = attachedSeasonId ?: return
    db.reference.child("seasons").child(seasonId).child("taskProgressLive").child(uid).setValue(
      mapOf(
        "taskId" to taskId,
        "percent" to percent.coerceIn(0L, 100L),
        "updatedAt" to ServerValue.TIMESTAMP,
      )
    )
  }

  fun clearKickBanner() {
    _kickReason.value = null
  }

  fun clearWarningBanner() {
    _realtimeWarning.value = null
  }

  suspend fun detachSync() {
    playersListener?.let { l -> playersRef?.removeEventListener(l) }
    doorsListener?.let { l -> doorsRef?.removeEventListener(l) }
    seatsListener?.let { l -> seatsRef?.removeEventListener(l) }
    lightingListener?.let { l -> lightingRef?.removeEventListener(l) }
    sessionLockListener?.let { l -> sessionLockRef?.removeEventListener(l) }
    kickListener?.let { l -> kickRef?.removeEventListener(l) }
    activeTaskListener?.let { l -> activeTaskRef?.removeEventListener(l) }
    captainListener?.let { l -> captainRef?.removeEventListener(l) }
    warningListener?.let { l -> warningRef?.removeEventListener(l) }
    kitchenLiveListener?.let { l -> kitchenLiveRef?.removeEventListener(l) }
    taskProgressLiveListener?.let { l -> taskProgressLiveRef?.removeEventListener(l) }

    playersListener = null
    doorsListener = null
    seatsListener = null
    lightingListener = null
    sessionLockListener = null
    kickListener = null
    activeTaskListener = null
    captainListener = null
    warningListener = null
    kitchenLiveListener = null
    taskProgressLiveListener = null

    val db = database
    val uid = attachedUid
    val seasonId = attachedSeasonId
    val wasAdmin = attachedIsAdmin
    if (db != null && !uid.isNullOrBlank() && !seasonId.isNullOrBlank()) {
      try {
        val presenceNode = if (wasAdmin) "adminPresence" else "players"
        db.reference.child("seasons").child(seasonId).child(presenceNode).child(uid)
          .updateChildren(
            mapOf(
              "online" to false,
              "state" to "offline",
              "seatId" to null,
              "updatedAt" to ServerValue.TIMESTAMP,
            )
          ).await()
        db.reference.child("seasons").child(seasonId).child("sessionLocks").child(uid)
          .removeValue().await()
      } catch (e: Exception) {
        Log.w(RTDB_TAG, "Failed to update offline RTDB status on detach: ${e.message}")
      }
    }

    attachedSeasonId = null
    attachedUid = null
    attachedSessionId = null
    attachedIsAdmin = false
    _remotePlayers.value = emptyMap()
    _kitchenLive.value = emptyMap()
    _taskProgressLive.value = emptyMap()
  }

  companion object {
    val VALID_EXPRESSIONS = setOf("neutral", "happy", "angry", "sad", "surprised")
  }
}
