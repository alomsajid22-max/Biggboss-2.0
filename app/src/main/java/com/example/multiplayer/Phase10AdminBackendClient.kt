package com.example.multiplayer

import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AdminSessionState {
  private val _session = MutableStateFlow<AdminSessionSnapshot?>(null)
  val session: StateFlow<AdminSessionSnapshot?> = _session.asStateFlow()

  val pendingAutoOpenControlPanel = MutableStateFlow(false)

  fun setSession(snapshot: AdminSessionSnapshot?, autoOpenPanel: Boolean = false) {
    _session.value = snapshot
    if (autoOpenPanel) {
      pendingAutoOpenControlPanel.value = true
    }
  }

  fun clear() {
    _session.value = null
    pendingAutoOpenControlPanel.value = false
  }
}

class BackendHttpException(
  val statusCode: Int,
  override val message: String,
) : IllegalStateException(message)

object Phase10AdminBackendClient {
  private const val FALLBACK_PROJECT_ID = "gen-lang-client-0294875711"

  private fun functionUrl(functionName: String): String {
    val projectId = runCatching {
      FirebaseApp.getInstance().options.projectId?.takeIf { it.isNotBlank() }
    }.getOrNull() ?: FALLBACK_PROJECT_ID
    return "https://us-central1-$projectId.cloudfunctions.net/$functionName"
  }

  private suspend fun postJson(
    functionName: String,
    body: JSONObject,
    bearerToken: String? = null,
  ): JSONObject = withContext(Dispatchers.IO) {
    val conn = (URL(functionUrl(functionName)).openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      connectTimeout = 15_000
      readTimeout = 20_000
      doOutput = true
      setRequestProperty("Content-Type", "application/json; charset=UTF-8")
      if (!bearerToken.isNullOrBlank()) {
        setRequestProperty("Authorization", "Bearer $bearerToken")
      }
    }
    try {
      conn.outputStream.use { os ->
        os.write(body.toString().toByteArray(Charsets.UTF_8))
      }
      val code = conn.responseCode
      val stream = if (code in 200..299) conn.inputStream else conn.errorStream
      val rawText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
      val json = runCatching { JSONObject(rawText) }.getOrNull()
      if (code !in 200..299) {
        val serverMsg = json?.optString("error")?.takeIf { it.isNotBlank() }
          ?: json?.optString("message")?.takeIf { it.isNotBlank() }
        val errMsg = when (code) {
          404 -> "Admin authentication backend function '$functionName' is not deployed yet (HTTP 404). Deploy Cloud Functions with BIGGBOSS_ADMIN_INITIAL_PASSWORD secret."
          503 -> serverMsg ?: "Admin authentication secret is not configured on backend."
          429 -> serverMsg ?: "Too many failed Admin login attempts. Please wait and try again."
          401, 403 -> serverMsg ?: "Invalid Admin username or password."
          else -> serverMsg ?: "Admin backend error (HTTP $code)."
        }
        throw BackendHttpException(code, errMsg)
      }
      json ?: JSONObject()
    } finally {
      conn.disconnect()
    }
  }

  suspend fun verifyAdminLogin(
    username: String,
    password: String,
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
  ): Result<AdminSessionSnapshot> {
    return try {
      val cleanUser = username.trim()
      if (cleanUser.isEmpty() || password.isEmpty()) {
        return Result.failure(IllegalArgumentException("Enter both Admin username and password."))
      }
      val payload = JSONObject().apply {
        put("username", cleanUser)
        put("password", password)
        put("seasonId", seasonId)
      }
      val resJson = postJson("adminVerifyLogin", payload)
      val customToken = resJson.optString("customToken", "")
      if (customToken.isBlank()) {
        return Result.failure(IllegalStateException("Backend did not return a valid Admin session token."))
      }
      val authResult = Firebase.auth.signInWithCustomToken(customToken).await()
      val signedUid = authResult.user?.uid ?: resJson.optString("uid", "")
      val snapshot = AdminSessionSnapshot(
        uid = signedUid,
        username = resJson.optString("username", "BIGGBOSS"),
        sessionId = resJson.optString("sessionId", ""),
        expiresAtMillis = resJson.optLong("expiresAtMillis", System.currentTimeMillis() + 7_200_000L),
        mustChangeInitialPassword = resJson.optBoolean("mustChangeInitialPassword", false),
      )
      AdminSessionState.setSession(snapshot, autoOpenPanel = true)
      Result.success(snapshot)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(e.message ?: "Admin login failed.", e))
    }
  }

  suspend fun changeAdminPassword(
    currentPassword: String,
    newPassword: String,
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
  ): Result<AdminSessionSnapshot> {
    return try {
      val user = Firebase.auth.currentUser
        ?: return Result.failure(IllegalStateException("Admin must be signed in."))
      val idToken = user.getIdToken(true).await().token
        ?: return Result.failure(IllegalStateException("Unable to fetch Firebase ID token."))
      val payload = JSONObject().apply {
        put("currentPassword", currentPassword)
        put("newPassword", newPassword)
        put("seasonId", seasonId)
      }
      val resJson = postJson("adminChangePassword", payload, bearerToken = idToken)
      val customToken = resJson.optString("customToken", "")
      if (customToken.isNotBlank()) {
        Firebase.auth.signInWithCustomToken(customToken).await()
      }
      val updated = AdminSessionSnapshot(
        uid = user.uid,
        username = "BIGGBOSS",
        sessionId = resJson.optString("sessionId", AdminSessionState.session.value?.sessionId.orEmpty()),
        expiresAtMillis = resJson.optLong("expiresAtMillis", System.currentTimeMillis() + 7_200_000L),
        mustChangeInitialPassword = false,
      )
      AdminSessionState.setSession(updated, autoOpenPanel = false)
      Result.success(updated)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(e.message ?: "Could not change Admin password.", e))
    }
  }

  suspend fun logoutAdminSession(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
  ) {
    try {
      val user = Firebase.auth.currentUser
      val idToken = user?.getIdToken(false)?.await()?.token
      if (!idToken.isNullOrBlank()) {
        val payload = JSONObject().apply { put("seasonId", seasonId) }
        postJson("adminLogout", payload, bearerToken = idToken)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      // Best-effort remote session revoke; local Firebase sign-out still clears session.
    } finally {
      AdminSessionState.clear()
    }
  }

  suspend fun checkAdminSession(): Result<Boolean> {
    return try {
      val user = Firebase.auth.currentUser ?: return Result.success(false)
      val idToken = user.getIdToken(false).await().token ?: return Result.success(false)
      val resJson = postJson("adminCheckSession", JSONObject(), bearerToken = idToken)
      val active = resJson.optBoolean("active", resJson.optBoolean("ok", false))
      if (active) {
        val exp = resJson.optLong("expiresAtMillis", 0L)
        val sid = resJson.optString("sessionId", "")
        AdminSessionState.setSession(
          AdminSessionSnapshot(
            uid = user.uid,
            username = resJson.optString("username", "BIGGBOSS"),
            sessionId = sid,
            expiresAtMillis = exp,
          ),
          autoOpenPanel = false,
        )
      } else {
        AdminSessionState.clear()
      }
      Result.success(active)
    } catch (e: CancellationException) {
      throw e
    } catch (e: BackendHttpException) {
      if (e.statusCode == 401 || e.statusCode == 403) {
        AdminSessionState.clear()
        Result.success(false)
      } else {
        Result.failure(e)
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun calculateAndCloseVotingOnServer(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    cycleId: String,
    closeVoting: Boolean,
  ): Result<JSONObject> {
    return try {
      val user = Firebase.auth.currentUser
        ?: return Result.failure(IllegalStateException("Admin must be signed in."))
      val idToken = user.getIdToken(true).await().token
        ?: return Result.failure(IllegalStateException("Unable to fetch Firebase ID token."))
      val payload = JSONObject().apply {
        put("seasonId", seasonId)
        put("cycleId", cycleId)
        put("closeVoting", closeVoting)
      }
      val resJson = postJson("calculateAndCloseVoting", payload, bearerToken = idToken)
      Result.success(resJson)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
