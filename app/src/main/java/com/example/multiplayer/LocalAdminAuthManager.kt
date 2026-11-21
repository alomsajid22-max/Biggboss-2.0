package com.example.multiplayer

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Local-only Admin Authentication & 48-Hour Lockout Manager.
 *
 * Operates completely offline inside the Android APK without Firebase Cloud Functions,
 * Cloudflare Workers, or any external/paid authentication service.
 *
 * Security Disclosure: Because verification runs inside the APK using local storage,
 * it protects normal gameplay flows offline, but cannot prevent someone who modifies
 * or reverse-engineers the APK binary.
 */
object LocalAdminAuthManager {

  const val ADMIN_USERNAME = "BIGGBOSS"
  const val ADMIN_UID = "biggboss_admin"
  const val MAX_FAILED_ATTEMPTS = 3
  const val LOCKOUT_DURATION_MS = 48L * 60L * 60L * 1000L // 172,800,000 ms (48 hours)
  private const val SESSION_TTL_MS = 2L * 60L * 60L * 1000L

  private const val PREFS_NAME = "biggboss_local_admin_auth_prefs"
  private const val KEY_SALT_HEX = "pwd_salt_hex"
  private const val KEY_HASH_HEX = "pwd_hash_hex"
  private const val KEY_IS_INITIAL_PWD = "is_initial_pwd"
  private const val KEY_PWD_VERSION = "pwd_version"

  private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
  private const val KEY_LOCKED_ACTIVE = "locked_active"
  private const val KEY_LOCK_WALL_START = "lock_wall_start"
  private const val KEY_LOCK_ELAPSED_START = "lock_elapsed_start"
  private const val KEY_LAST_WALL = "lock_last_wall"
  private const val KEY_LAST_ELAPSED = "lock_last_elapsed"
  private const val KEY_ACCUMULATED_MS = "lock_accumulated_ms"
  private const val KEY_CLOCK_TAMPERED = "lock_clock_tampered"
  private const val KEY_DEVICE_SALT = "lock_device_salt"
  private const val KEY_INTEGRITY_MAC = "lock_integrity_mac"

  private const val PBKDF2_ITERATIONS = 12_000
  private const val PBKDF2_KEY_BITS = 256

  // Salted PBKDF2-HMAC-SHA256 verification parameters for the initial default password.
  // Plaintext password is never written to SharedPreferences or application logs.
  private val INITIAL_SEED_SALT = byteArrayOf(
    0x4B, 0x19, 0x7A, 0x2C, 0x55, 0x0E, 0x88.toByte(), 0x3F,
    0x91.toByte(), 0x62, 0xD4.toByte(), 0x11, 0x70, 0xA8.toByte(), 0x35, 0xC6.toByte(),
  )
  private const val INITIAL_SEED_HASH_HEX =
    "01f9c8616b1a12651d857fa0a3fb5234bfae93f22a2aa870b959b9dfffafdcf5"

  interface AdminClock {
    fun currentWallTimeMs(): Long
    fun elapsedRealtimeMs(): Long
  }

  object SystemAdminClock : AdminClock {
    override fun currentWallTimeMs(): Long = System.currentTimeMillis()
    override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
  }

  data class LockoutStatus(
    val isLocked: Boolean = false,
    val failedAttempts: Int = 0,
    val remainingMillis: Long = 0L,
    val clockTamperingDetected: Boolean = false,
  ) {
    val remainingAttemptsBeforeLock: Int
      get() = (MAX_FAILED_ATTEMPTS - failedAttempts).coerceAtLeast(0)
  }

  sealed class LoginOutcome {
    data class Success(val snapshot: AdminSessionSnapshot) : LoginOutcome()
    data class Failed(
      val message: String,
      val status: LockoutStatus,
    ) : LoginOutcome()
    data class Locked(
      val message: String,
      val status: LockoutStatus,
    ) : LoginOutcome()
  }

  const val LOCKOUT_HEADLINE = "ADMIN ACCESS LOCKED – TRY AGAIN AFTER 48 HOURS."

  @Volatile
  var clock: AdminClock = SystemAdminClock

  private val secureRandom = SecureRandom()

  private fun prefs(context: Context): SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  @Synchronized
  fun getLockoutStatus(context: Context): LockoutStatus {
    val p = prefs(context)
    return evaluateAndPersistLockout(p)
  }

  @Synchronized
  fun verifyAdminLogin(
    context: Context,
    username: String,
    password: String,
  ): LoginOutcome {
    val p = prefs(context)
    val currentStatus = evaluateAndPersistLockout(p)
    if (currentStatus.isLocked) {
      return LoginOutcome.Locked(
        message = buildLockoutMessage(currentStatus.remainingMillis),
        status = currentStatus,
      )
    }

    ensureCredentialsSeeded(p)

    val cleanUser = username.trim()
    val userMatches = constantTimeEqualsUtf8(cleanUser, ADMIN_USERNAME)
    val pwdMatches = verifyStoredPassword(p, password)

    if (userMatches && pwdMatches) {
      val nowWall = clock.currentWallTimeMs()
      val isInitial = p.getBoolean(KEY_IS_INITIAL_PWD, true)
      clearFailedAndLockout(p)
      val session = AdminSessionSnapshot(
        uid = ADMIN_UID,
        username = ADMIN_USERNAME,
        sessionId = "local_${randomHex(16)}",
        expiresAtMillis = nowWall + SESSION_TTL_MS,
        mustChangeInitialPassword = isInitial,
        isLocalOnly = true,
      )
      AdminSessionState.setSession(session, autoOpenPanel = true)
      return LoginOutcome.Success(session)
    }

    val nextFailed = (currentStatus.failedAttempts + 1).coerceAtMost(MAX_FAILED_ATTEMPTS)
    if (nextFailed >= MAX_FAILED_ATTEMPTS) {
      val lockedStatus = trigger48HourLockout(p, nextFailed)
      return LoginOutcome.Locked(
        message = buildLockoutMessage(lockedStatus.remainingMillis),
        status = lockedStatus,
      )
    }

    persistFailedCount(p, nextFailed)
    val remainingTries = MAX_FAILED_ATTEMPTS - nextFailed
    val updatedStatus = LockoutStatus(
      isLocked = false,
      failedAttempts = nextFailed,
      remainingMillis = 0L,
      clockTamperingDetected = false,
    )
    return LoginOutcome.Failed(
      message = "Invalid Admin username or password ($nextFailed/2 consecutive failed attempts; $remainingTries attempt left before 48-hour lockout).",
      status = updatedStatus,
    )
  }

  @Synchronized
  fun changeAdminPassword(
    context: Context,
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
  ): Result<AdminSessionSnapshot> {
    val validationErr = AdminAuthValidation.validatePasswordChange(
      currentPassword = currentPassword,
      newPassword = newPassword,
      confirmPassword = confirmPassword,
    )
    if (validationErr != null) {
      return Result.failure(IllegalArgumentException(validationErr))
    }

    val p = prefs(context)
    ensureCredentialsSeeded(p)
    if (!verifyStoredPassword(p, currentPassword)) {
      return Result.failure(IllegalArgumentException("Current Admin password is incorrect."))
    }

    val newSalt = ByteArray(16).also { secureRandom.nextBytes(it) }
    val newHashHex = pbkdf2Hex(newPassword, newSalt)
    val nextVersion = p.getInt(KEY_PWD_VERSION, 1) + 1

    p.edit()
      .putString(KEY_SALT_HEX, newSalt.toHex())
      .putString(KEY_HASH_HEX, newHashHex)
      .putBoolean(KEY_IS_INITIAL_PWD, false)
      .putInt(KEY_PWD_VERSION, nextVersion)
      .commit()

    val existing = AdminSessionState.session.value
    val updated = AdminSessionSnapshot(
      uid = existing?.uid?.ifBlank { ADMIN_UID } ?: ADMIN_UID,
      username = ADMIN_USERNAME,
      sessionId = existing?.sessionId?.ifBlank { "local_${randomHex(16)}" } ?: "local_${randomHex(16)}",
      expiresAtMillis = clock.currentWallTimeMs() + SESSION_TTL_MS,
      mustChangeInitialPassword = false,
      isLocalOnly = true,
    )
    AdminSessionState.setSession(updated, autoOpenPanel = false)
    return Result.success(updated)
  }

  fun logoutLocalAdmin() {
    AdminSessionState.clear()
  }

  /**
   * Formats remaining time clearly as e.g. "47h 59m 58s".
   */
  fun formatRemainingTime(remainingMillis: Long): String {
    val clampedSec = ((remainingMillis.coerceAtLeast(0L) + 999L) / 1000L)
    val hours = clampedSec / 3600L
    val minutes = (clampedSec % 3600L) / 60L
    val seconds = clampedSec % 60L
    return String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
  }

  fun buildLockoutMessage(remainingMillis: Long): String {
    return "$LOCKOUT_HEADLINE Remaining time: ${formatRemainingTime(remainingMillis)}"
  }

  @Synchronized
  internal fun resetAllForTests(context: Context) {
    prefs(context).edit().clear().commit()
    AdminSessionState.clear()
  }

  private fun getOrCreateDeviceSalt(p: SharedPreferences): String {
    val existing = p.getString(KEY_DEVICE_SALT, null)
    if (!existing.isNullOrBlank()) return existing
    val created = randomHex(16)
    p.edit().putString(KEY_DEVICE_SALT, created).commit()
    return created
  }

  private fun computeMac(
    deviceSalt: String,
    locked: Boolean,
    failedAttempts: Int,
    lockWallStart: Long,
    lockElapsedStart: Long,
    lastWall: Long,
    lastElapsed: Long,
    accumulatedMs: Long,
    tampered: Boolean,
  ): String {
    val raw = "$deviceSalt|$locked|$failedAttempts|$lockWallStart|$lockElapsedStart|$lastWall|$lastElapsed|$accumulatedMs|$tampered"
    val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
    return digest.toHex()
  }

  private fun persistFailedCount(p: SharedPreferences, failedAttempts: Int) {
    val salt = getOrCreateDeviceSalt(p)
    val mac = computeMac(salt, false, failedAttempts, 0L, 0L, 0L, 0L, 0L, false)
    p.edit()
      .putBoolean(KEY_LOCKED_ACTIVE, false)
      .putInt(KEY_FAILED_ATTEMPTS, failedAttempts)
      .putLong(KEY_LOCK_WALL_START, 0L)
      .putLong(KEY_LOCK_ELAPSED_START, 0L)
      .putLong(KEY_LAST_WALL, 0L)
      .putLong(KEY_LAST_ELAPSED, 0L)
      .putLong(KEY_ACCUMULATED_MS, 0L)
      .putBoolean(KEY_CLOCK_TAMPERED, false)
      .putString(KEY_INTEGRITY_MAC, mac)
      .commit()
  }

  private fun trigger48HourLockout(p: SharedPreferences, failedAttempts: Int): LockoutStatus {
    val salt = getOrCreateDeviceSalt(p)
    val nowWall = clock.currentWallTimeMs()
    val nowElapsed = clock.elapsedRealtimeMs()
    val mac = computeMac(
      deviceSalt = salt,
      locked = true,
      failedAttempts = failedAttempts,
      lockWallStart = nowWall,
      lockElapsedStart = nowElapsed,
      lastWall = nowWall,
      lastElapsed = nowElapsed,
      accumulatedMs = 0L,
      tampered = false,
    )
    p.edit()
      .putBoolean(KEY_LOCKED_ACTIVE, true)
      .putInt(KEY_FAILED_ATTEMPTS, failedAttempts)
      .putLong(KEY_LOCK_WALL_START, nowWall)
      .putLong(KEY_LOCK_ELAPSED_START, nowElapsed)
      .putLong(KEY_LAST_WALL, nowWall)
      .putLong(KEY_LAST_ELAPSED, nowElapsed)
      .putLong(KEY_ACCUMULATED_MS, 0L)
      .putBoolean(KEY_CLOCK_TAMPERED, false)
      .putString(KEY_INTEGRITY_MAC, mac)
      .commit()
    return LockoutStatus(
      isLocked = true,
      failedAttempts = failedAttempts,
      remainingMillis = LOCKOUT_DURATION_MS,
      clockTamperingDetected = false,
    )
  }

  private fun clearFailedAndLockout(p: SharedPreferences) {
    val salt = getOrCreateDeviceSalt(p)
    val mac = computeMac(salt, false, 0, 0L, 0L, 0L, 0L, 0L, false)
    p.edit()
      .putBoolean(KEY_LOCKED_ACTIVE, false)
      .putInt(KEY_FAILED_ATTEMPTS, 0)
      .putLong(KEY_LOCK_WALL_START, 0L)
      .putLong(KEY_LOCK_ELAPSED_START, 0L)
      .putLong(KEY_LAST_WALL, 0L)
      .putLong(KEY_LAST_ELAPSED, 0L)
      .putLong(KEY_ACCUMULATED_MS, 0L)
      .putBoolean(KEY_CLOCK_TAMPERED, false)
      .putString(KEY_INTEGRITY_MAC, mac)
      .commit()
  }

  private fun evaluateAndPersistLockout(p: SharedPreferences): LockoutStatus {
    if (!p.contains(KEY_LOCKED_ACTIVE) && !p.contains(KEY_FAILED_ATTEMPTS)) {
      return LockoutStatus(isLocked = false, failedAttempts = 0, remainingMillis = 0L)
    }

    val salt = getOrCreateDeviceSalt(p)
    val locked = p.getBoolean(KEY_LOCKED_ACTIVE, false)
    val failed = p.getInt(KEY_FAILED_ATTEMPTS, 0)
    var lockWallStart = p.getLong(KEY_LOCK_WALL_START, 0L)
    var lockElapsedStart = p.getLong(KEY_LOCK_ELAPSED_START, 0L)
    var lastWall = p.getLong(KEY_LAST_WALL, 0L)
    var lastElapsed = p.getLong(KEY_LAST_ELAPSED, 0L)
    var accumulatedMs = p.getLong(KEY_ACCUMULATED_MS, 0L)
    var tampered = p.getBoolean(KEY_CLOCK_TAMPERED, false)
    val savedMac = p.getString(KEY_INTEGRITY_MAC, null)

    val expectedMac = computeMac(
      salt, locked, failed, lockWallStart, lockElapsedStart, lastWall, lastElapsed, accumulatedMs, tampered,
    )
    if (savedMac != expectedMac) {
      // Stored lockout data appears corrupted or tampered with: preserve safe lock rather than bypassing.
      return if (locked || failed >= MAX_FAILED_ATTEMPTS) {
        trigger48HourLockout(p, MAX_FAILED_ATTEMPTS).copy(clockTamperingDetected = true)
      } else {
        clearFailedAndLockout(p)
        LockoutStatus(isLocked = false, failedAttempts = 0, remainingMillis = 0L)
      }
    }

    if (!locked) {
      return LockoutStatus(isLocked = false, failedAttempts = failed, remainingMillis = 0L)
    }

    val nowWall = clock.currentWallTimeMs()
    val nowElapsed = clock.elapsedRealtimeMs()

    val sameBootSession = lockElapsedStart >= 0L && nowElapsed >= lastElapsed && nowElapsed >= lockElapsedStart
    val totalElapsedMs: Long

    if (sameBootSession) {
      // Within the same boot session (even across app force-stops & restarts),
      // SystemClock.elapsedRealtime() cannot be spoofed by Settings -> Date & Time.
      val monoDelta = (nowElapsed - lastElapsed).coerceAtLeast(0L)
      val wallDelta = nowWall - lastWall
      // Detect obvious wall clock manipulation (rolled backward OR jumped far ahead of monotonic clock)
      if (wallDelta < -5_000L || wallDelta > monoDelta + 120_000L) {
        tampered = true
      }
      accumulatedMs += monoDelta
      lastElapsed = nowElapsed
      // Advance lastWall only by actual monotonic delta if wall jumped/rolled anomalously,
      // otherwise track real wall time so genuine post-reboot time stays accurate.
      lastWall = if (wallDelta in -5_000L..(monoDelta + 120_000L) && nowWall >= lastWall) {
        nowWall
      } else {
        lastWall + monoDelta
      }
      totalElapsedMs = accumulatedMs
    } else {
      // Device rebooted (elapsedRealtime reset < lastElapsed).
      // Credit only monotonic time already accumulated up to lastWall; add wall delta if plausible & non-tampered.
      val rawWallDelta = nowWall - lastWall
      if (rawWallDelta < -5_000L) {
        tampered = true
      } else if (!tampered && rawWallDelta > 0L) {
        accumulatedMs += rawWallDelta
      }
      lockElapsedStart = nowElapsed
      lastElapsed = nowElapsed
      if (nowWall >= lastWall) {
        lastWall = nowWall
      }
      totalElapsedMs = accumulatedMs
    }

    val remaining = (LOCKOUT_DURATION_MS - totalElapsedMs).coerceAtLeast(0L)
    if (remaining <= 0L) {
      clearFailedAndLockout(p)
      return LockoutStatus(isLocked = false, failedAttempts = 0, remainingMillis = 0L)
    }

    val nextMac = computeMac(
      salt, true, failed, lockWallStart, lockElapsedStart, lastWall, lastElapsed, accumulatedMs, tampered,
    )
    p.edit()
      .putBoolean(KEY_LOCKED_ACTIVE, true)
      .putInt(KEY_FAILED_ATTEMPTS, failed)
      .putLong(KEY_LOCK_WALL_START, lockWallStart)
      .putLong(KEY_LOCK_ELAPSED_START, lockElapsedStart)
      .putLong(KEY_LAST_WALL, lastWall)
      .putLong(KEY_LAST_ELAPSED, lastElapsed)
      .putLong(KEY_ACCUMULATED_MS, accumulatedMs)
      .putBoolean(KEY_CLOCK_TAMPERED, tampered)
      .putString(KEY_INTEGRITY_MAC, nextMac)
      .commit()

    return LockoutStatus(
      isLocked = true,
      failedAttempts = failed,
      remainingMillis = remaining,
      clockTamperingDetected = tampered,
    )
  }

  private fun ensureCredentialsSeeded(p: SharedPreferences) {
    if (p.contains(KEY_HASH_HEX) && p.contains(KEY_SALT_HEX)) return
    p.edit()
      .putString(KEY_SALT_HEX, INITIAL_SEED_SALT.toHex())
      .putString(KEY_HASH_HEX, INITIAL_SEED_HASH_HEX)
      .putBoolean(KEY_IS_INITIAL_PWD, true)
      .putInt(KEY_PWD_VERSION, 1)
      .commit()
  }

  private fun verifyStoredPassword(p: SharedPreferences, inputPassword: String): Boolean {
    if (inputPassword.isEmpty()) return false
    val saltHex = p.getString(KEY_SALT_HEX, null) ?: return false
    val expectedHex = p.getString(KEY_HASH_HEX, null) ?: return false
    val saltBytes = hexToBytes(saltHex) ?: return false
    val actualHex = pbkdf2Hex(inputPassword, saltBytes)
    return constantTimeEqualsUtf8(actualHex, expectedHex)
  }

  private fun pbkdf2Hex(secret: String, salt: ByteArray): String {
    return try {
      val spec = PBEKeySpec(secret.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
      val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
      factory.generateSecret(spec).encoded.toHex()
    } catch (_: Throwable) {
      val md = MessageDigest.getInstance("SHA-256")
      md.update(salt)
      md.digest(secret.toByteArray(Charsets.UTF_8)).toHex()
    }
  }

  private fun constantTimeEqualsUtf8(a: String, b: String): Boolean {
    val bytesA = a.toByteArray(Charsets.UTF_8)
    val bytesB = b.toByteArray(Charsets.UTF_8)
    return MessageDigest.isEqual(bytesA, bytesB)
  }

  private fun randomHex(byteCount: Int): String {
    val bytes = ByteArray(byteCount)
    secureRandom.nextBytes(bytes)
    return bytes.toHex()
  }

  private fun ByteArray.toHex(): String =
    joinToString("") { "%02x".format(Locale.US, it) }

  private fun hexToBytes(hex: String): ByteArray? {
    if (hex.length % 2 != 0) return null
    return try {
      ByteArray(hex.length / 2) { idx ->
        hex.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
      }
    } catch (_: Throwable) {
      null
    }
  }
}
