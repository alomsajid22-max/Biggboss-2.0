package com.example.multiplayer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Phase12To20PersistenceStore(context: Context?) {
  private val prefs = context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun loadWardrobe(uid: String): WardrobeAppearance {
    val raw = prefs?.getString("wardrobe_$uid", null) ?: return WardrobeAppearance()
    return runCatching { WardrobeAppearance.fromJson(JSONObject(raw)) }.getOrDefault(WardrobeAppearance())
  }

  fun saveWardrobe(uid: String, app: WardrobeAppearance) {
    prefs?.edit()?.putString("wardrobe_$uid", app.toJsonObject().toString())?.apply()
  }

  fun loadCameraPrefs(uid: String): CameraPreferences {
    val raw = prefs?.getString("cam_prefs_$uid", null) ?: return CameraPreferences()
    return runCatching {
      val obj = JSONObject(raw)
      CameraPreferences(
        mode = obj.optString("mode", "THIRD_PERSON"),
        distance = obj.optDouble("distance", 4.6).toFloat().coerceIn(2.0f, 8.5f),
        sensitivity = obj.optDouble("sensitivity", 1.0).toFloat().coerceIn(0.4f, 2.0f),
        smoothness = obj.optDouble("smoothness", 0.72).toFloat().coerceIn(0.25f, 1.0f),
        freeCamAllowed = obj.optBoolean("freeCamAllowed", true),
      )
    }.getOrDefault(CameraPreferences())
  }

  fun saveCameraPrefs(uid: String, camPrefs: CameraPreferences) {
    prefs?.edit()?.putString("cam_prefs_$uid", camPrefs.toJsonObject().toString())?.apply()
  }

  fun loadTtsSettings(): TtsPlaybackSettings {
    val raw = prefs?.getString("tts_settings", null) ?: return TtsPlaybackSettings()
    return runCatching {
      val obj = JSONObject(raw)
      TtsPlaybackSettings(
        enabled = obj.optBoolean("enabled", true),
        languageTag = obj.optString("languageTag", "hi-IN"),
        volume = obj.optDouble("volume", 1.0).toFloat().coerceIn(0f, 1f),
        speechRate = obj.optDouble("speechRate", 0.95).toFloat().coerceIn(0.6f, 1.6f),
        welcomePlayed = obj.optBoolean("welcomePlayed", false),
      )
    }.getOrDefault(TtsPlaybackSettings())
  }

  fun saveTtsSettings(settings: TtsPlaybackSettings) {
    val obj = JSONObject().apply {
      put("enabled", settings.enabled)
      put("languageTag", settings.languageTag)
      put("volume", settings.volume.toDouble())
      put("speechRate", settings.speechRate.toDouble())
      put("welcomePlayed", settings.welcomePlayed)
    }
    prefs?.edit()?.putString("tts_settings", obj.toString())?.apply()
  }

  fun loadFinaleState(): FinaleCeremonyState {
    val raw = prefs?.getString("finale_state", null) ?: return FinaleCeremonyState()
    return runCatching {
      val obj = JSONObject(raw)
      FinaleCeremonyState(
        active = obj.optBoolean("active", false),
        winnerUid = obj.optString("winnerUid", ""),
        winnerName = obj.optString("winnerName", ""),
        runnerUpName = obj.optString("runnerUpName", ""),
        trophyTitle = obj.optString("trophyTitle", "BIGG BOSS – THE HOUSE SEASON 01 CHAMPION"),
        declaredAtMillis = obj.optLong("declaredAtMillis", 0L),
      )
    }.getOrDefault(FinaleCeremonyState())
  }

  fun saveFinaleState(state: FinaleCeremonyState) {
    val obj = JSONObject().apply {
      put("active", state.active)
      put("winnerUid", state.winnerUid)
      put("winnerName", state.winnerName)
      put("runnerUpName", state.runnerUpName)
      put("trophyTitle", state.trophyTitle)
      put("declaredAtMillis", state.declaredAtMillis)
    }
    prefs?.edit()?.putString("finale_state", obj.toString())?.apply()
  }

  fun loadCctvOnlineMap(): Map<String, Boolean> {
    val raw = prefs?.getString("cctv_online_map", null)
    val base = BiggGramAndNpcVotingEngine.ALL_24_CCTV_CAMERAS.associate { it.id to true }.toMutableMap()
    if (raw.isNullOrBlank()) return base
    return runCatching {
      val obj = JSONObject(raw)
      obj.keys().forEach { k -> base[k] = obj.optBoolean(k, true) }
      base.toMap()
    }.getOrDefault(base)
  }

  fun saveCctvOnlineMap(map: Map<String, Boolean>) {
    val obj = JSONObject()
    map.forEach { (k, v) -> obj.put(k, v) }
    prefs?.edit()?.putString("cctv_online_map", obj.toString())?.apply()
  }

  fun loadNpcVotes(): List<NpcVoteRecord> {
    val raw = prefs?.getString("npc_votes", null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      (0 until arr.length()).map { idx ->
        val o = arr.getJSONObject(idx)
        NpcVoteRecord(
          id = o.optString("id"),
          cycleId = o.optString("cycleId"),
          npcId = o.optString("npcId"),
          npcName = o.optString("npcName"),
          npcHandle = o.optString("npcHandle"),
          targetUid = o.optString("targetUid"),
          targetName = o.optString("targetName"),
          postId = o.optString("postId"),
          castAtMillis = o.optLong("castAtMillis"),
        )
      }
    }.getOrDefault(emptyList())
  }

  fun saveNpcVotes(votes: List<NpcVoteRecord>) {
    val arr = JSONArray()
    votes.forEach { v ->
      arr.put(
        JSONObject().apply {
          put("id", v.id)
          put("cycleId", v.cycleId)
          put("npcId", v.npcId)
          put("npcName", v.npcName)
          put("npcHandle", v.npcHandle)
          put("targetUid", v.targetUid)
          put("targetName", v.targetName)
          put("postId", v.postId)
          put("castAtMillis", v.castAtMillis)
        },
      )
    }
    prefs?.edit()?.putString("npc_votes", arr.toString())?.apply()
  }

  fun loadBiggGramPosts(): List<BiggGramPost> {
    val raw = prefs?.getString("bigg_gram_posts", null)
    if (raw.isNullOrBlank()) return BiggGramAndNpcVotingEngine.defaultPosts()
    return runCatching {
      val arr = JSONArray(raw)
      (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        val cArr = o.optJSONArray("comments") ?: JSONArray()
        val comments = (0 until cArr.length()).map { j ->
          val co = cArr.getJSONObject(j)
          BiggGramComment(
            id = co.optString("id"),
            authorHandle = co.optString("authorHandle"),
            authorName = co.optString("authorName"),
            text = co.optString("text"),
            timestampMillis = co.optLong("timestampMillis"),
          )
        }
        BiggGramPost(
          id = o.optString("id"),
          authorHandle = o.optString("authorHandle"),
          authorName = o.optString("authorName"),
          authorAvatarHex = o.optString("authorAvatarHex", "#EAB954"),
          caption = o.optString("caption"),
          category = o.optString("category", "LIFESTYLE"),
          cycleId = o.optString("cycleId").ifBlank { null },
          nomineeUids = Phase12To20JsonHelper.decodeStringList(o.optJSONArray("nomineeUids")),
          likes = o.optInt("likes", 0),
          likedByAdmin = o.optBoolean("likedByAdmin", false),
          comments = comments,
          timestampMillis = o.optLong("timestampMillis"),
        )
      }
    }.getOrDefault(BiggGramAndNpcVotingEngine.defaultPosts())
  }

  fun saveBiggGramPosts(posts: List<BiggGramPost>) {
    val arr = JSONArray()
    posts.take(40).forEach { p ->
      val cArr = JSONArray()
      p.comments.forEach { c ->
        cArr.put(
          JSONObject().apply {
            put("id", c.id)
            put("authorHandle", c.authorHandle)
            put("authorName", c.authorName)
            put("text", c.text)
            put("timestampMillis", c.timestampMillis)
          },
        )
      }
      arr.put(
        JSONObject().apply {
          put("id", p.id)
          put("authorHandle", p.authorHandle)
          put("authorName", p.authorName)
          put("authorAvatarHex", p.authorAvatarHex)
          put("caption", p.caption)
          put("category", p.category)
          put("cycleId", p.cycleId ?: "")
          put("nomineeUids", Phase12To20JsonHelper.encodeStringList(p.nomineeUids))
          put("likes", p.likes)
          put("likedByAdmin", p.likedByAdmin)
          put("comments", cArr)
          put("timestampMillis", p.timestampMillis)
        },
      )
    }
    prefs?.edit()?.putString("bigg_gram_posts", arr.toString())?.apply()
  }

  fun loadCelebrations(): List<HouseCelebrationEvent> {
    val raw = prefs?.getString("house_celebrations", null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        HouseCelebrationEvent(
          id = o.optString("id"),
          type = o.optString("type", CelebrationType.HOUSE_CELEBRATION.name),
          title = o.optString("title"),
          description = o.optString("description"),
          scheduledTimeMillis = o.optLong("scheduledTimeMillis"),
          status = o.optString("status", "SCHEDULED"),
          triggeredBy = o.optString("triggeredBy", "BIGGBOSS"),
          createdAtMillis = o.optLong("createdAtMillis"),
        )
      }
    }.getOrDefault(emptyList())
  }

  fun saveCelebrations(list: List<HouseCelebrationEvent>) {
    val arr = JSONArray()
    list.take(30).forEach { ev ->
      arr.put(
        JSONObject().apply {
          put("id", ev.id)
          put("type", ev.type)
          put("title", ev.title)
          put("description", ev.description)
          put("scheduledTimeMillis", ev.scheduledTimeMillis)
          put("status", ev.status)
          put("triggeredBy", ev.triggeredBy)
          put("createdAtMillis", ev.createdAtMillis)
        },
      )
    }
    prefs?.edit()?.putString("house_celebrations", arr.toString())?.apply()
  }

  companion object {
    private const val PREFS_NAME = "bigg_boss_phase12_20_store_v1"
  }
}
