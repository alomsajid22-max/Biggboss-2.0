package com.example.multiplayer

import android.app.Activity
import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val AUTH_TAG = "Auth"

private val _isCompletingEmailRegistration = MutableStateFlow(false)
val isCompletingEmailRegistration: StateFlow<Boolean> = _isCompletingEmailRegistration.asStateFlow()

object AuthValidation {
  private val FALLBACK_EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

  fun validateEmail(raw: String): String? {
    val clean = raw.trim()
    if (clean.isEmpty()) {
      return "Email address is required."
    }
    if (clean.length > 160) {
      return "Email address must be 160 characters or fewer."
    }
    val matchesPattern = try {
      Patterns.EMAIL_ADDRESS.matcher(clean).matches()
    } catch (_: Throwable) {
      false
    }
    if (!matchesPattern && !FALLBACK_EMAIL_REGEX.matches(clean)) {
      return "Please enter a valid email address."
    }
    return null
  }

  fun validateDisplayName(raw: String): String? {
    val clean = raw.trim()
    if (clean.length !in 3..24) {
      return "Username / display name must be between 3 and 24 characters."
    }
    val alnumCount = clean.count { it.isLetterOrDigit() }
    if (alnumCount < 3) {
      return "Display name must include at least 3 letters or numbers."
    }
    return null
  }

  fun validatePassword(raw: String): String? {
    return if (raw.length !in 8..128) {
      "Password must be between 8 and 128 characters."
    } else null
  }

  /**
   * Defense-in-depth client invariant: username strings like "BIGGBOSS" never elevate role
   * on the client. Admin authority is granted exclusively by backend /admins/{uid}.
   */
  fun canClientElevateAdminFromUsername(@Suppress("UNUSED_PARAMETER") username: String): Boolean = false
}

object AuthErrorMapper {
  fun mapAuthCode(
    errorCode: String?,
    fallbackMessage: String?,
    isPasswordReset: Boolean = false,
  ): String {
    return when (errorCode) {
      "ERROR_EMAIL_ALREADY_IN_USE" ->
        "An account with this email already exists. Try signing in instead."
      "ERROR_INVALID_EMAIL" ->
        "Please enter a valid email address."
      "ERROR_WEAK_PASSWORD" ->
        "Password is too weak. Use at least 8 characters with letters and numbers."
      "ERROR_WRONG_PASSWORD",
      "ERROR_INVALID_CREDENTIAL" ->
        "Incorrect email or password. Please check your credentials and try again."
      "ERROR_USER_NOT_FOUND" ->
        if (isPasswordReset) "No account found with that email address."
        else "Incorrect email or password. Please check your credentials and try again."
      "ERROR_USER_DISABLED" ->
        "This account has been disabled by an administrator."
      "ERROR_TOO_MANY_REQUESTS" ->
        "Too many attempts. Please wait a moment and try again."
      "ERROR_OPERATION_NOT_ALLOWED" ->
        "Email/Password sign-in is not enabled in Firebase Console (Authentication → Sign-in method)."
      "ERROR_NETWORK_REQUEST_FAILED" ->
        "Network error: unable to connect to Firebase. Check your internet connection and retry."
      else -> fallbackMessage?.takeIf { it.isNotBlank() } ?: "Authentication failed. Please try again."
    }
  }

  fun mapException(error: Throwable, isPasswordReset: Boolean = false): String {
    val chain = generateSequence(error) { it.cause }.take(8).toList()
    val authErr = chain.filterIsInstance<FirebaseAuthException>().firstOrNull()
    val firestoreErr = chain.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    val netErr = chain.firstOrNull {
      it is FirebaseNetworkException || it is UnknownHostException || it is ConnectException || it is SocketTimeoutException
    }
    val tooManyErr = chain.filterIsInstance<FirebaseTooManyRequestsException>().firstOrNull()

    return when {
      authErr is FirebaseAuthWeakPasswordException ->
        mapAuthCode("ERROR_WEAK_PASSWORD", authErr.localizedMessage, isPasswordReset)
      authErr is FirebaseAuthUserCollisionException ->
        mapAuthCode("ERROR_EMAIL_ALREADY_IN_USE", authErr.localizedMessage, isPasswordReset)
      authErr is FirebaseAuthInvalidCredentialsException ->
        mapAuthCode(authErr.errorCode, authErr.localizedMessage, isPasswordReset)
      authErr is FirebaseAuthInvalidUserException ->
        mapAuthCode(authErr.errorCode, authErr.localizedMessage, isPasswordReset)
      authErr != null ->
        mapAuthCode(authErr.errorCode, authErr.localizedMessage, isPasswordReset)
      netErr != null ->
        mapAuthCode("ERROR_NETWORK_REQUEST_FAILED", netErr.localizedMessage, isPasswordReset)
      tooManyErr != null ->
        mapAuthCode("ERROR_TOO_MANY_REQUESTS", tooManyErr.localizedMessage, isPasswordReset)
      firestoreErr != null -> when (firestoreErr.code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
          "Network error: unable to save your profile to Firestore. Check your connection and retry."
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
          "Permission denied while saving profile. Ensure Firestore rules are deployed."
        else -> firestoreErr.localizedMessage ?: "Failed to save housemate profile."
      }
      else -> error.localizedMessage ?: "Authentication failed. Please try again."
    }
  }

  fun isDuplicateEmailMessage(message: String?): Boolean {
    if (message.isNullOrBlank()) return false
    val lower = message.lowercase()
    return lower.contains("already exists") || lower.contains("already in use")
  }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

/**
 * Registers a new player using Firebase Authentication's built-in Email/Password provider,
 * sets their Auth displayName, and atomically claims a unique housemate display name + PLAYER
 * profile in Firestore. Passwords are NEVER written to Firestore.
 */
suspend fun registerWithEmailPassword(
  context: Context,
  email: String,
  password: String,
  displayName: String,
  characterId: String,
): Result<UserAccount> {
  return try {
    AuthValidation.validateDisplayName(displayName)?.let { return Result.failure(IllegalArgumentException(it)) }
    AuthValidation.validateEmail(email)?.let { return Result.failure(IllegalArgumentException(it)) }
    AuthValidation.validatePassword(password)?.let { return Result.failure(IllegalArgumentException(it)) }
    if (HousemateCatalog.all.none { it.id == characterId }) {
      return Result.failure(IllegalArgumentException("Please choose a valid 3D housemate character."))
    }

    val cleanEmail = email.trim()
    val cleanDisplayName = displayName.trim()

    withContext(NonCancellable) {
      _isCompletingEmailRegistration.value = true
      try {
        val authResult = Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password).await()
        val createdUser = authResult.user
          ?: return@withContext Result.failure(IllegalStateException("Account creation failed: no user session returned."))

        try {
          createdUser.updateProfile(
            userProfileChangeRequest {
              this.displayName = cleanDisplayName
            }
          ).await()
        } catch (e: Exception) {
          Log.w(AUTH_TAG, "Non-fatal warning updating FirebaseAuth displayName: ${e.message}")
        }

        val db = FirebaseFirestore.getInstance(
          context.applicationContext.getString(R.string.firestore_database_id)
        )
        val repo = MultiplayerFirestoreRepository(db)
        val profileRes = repo.registerOrUpdateProfile(
          rawDisplayName = cleanDisplayName,
          characterId = characterId,
        )

        if (profileRes.isFailure) {
          val profileError = profileRes.exceptionOrNull()
            ?: IllegalStateException("Failed to create housemate profile.")
          try {
            createdUser.delete().await()
          } catch (cleanupErr: Exception) {
            Log.w(AUTH_TAG, "Auth rollback delete failed: ${cleanupErr.message}")
          }
          Firebase.auth.signOut()
          return@withContext Result.failure(IllegalStateException(AuthErrorMapper.mapException(profileError), profileError))
        }

        profileRes
      } finally {
        _isCompletingEmailRegistration.value = false
      }
    }
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Log.w(AUTH_TAG, "Email/Password registration failed: ${e.message}", e)
    Result.failure(IllegalStateException(AuthErrorMapper.mapException(e), e))
  }
}

/**
 * Signs in an existing player using Firebase Authentication's built-in Email/Password provider.
 */
suspend fun loginWithEmailPassword(
  email: String,
  password: String,
): Result<FirebaseUser> {
  return try {
    AuthValidation.validateEmail(email)?.let { return Result.failure(IllegalArgumentException(it)) }
    if (password.isEmpty()) {
      return Result.failure(IllegalArgumentException("Enter your password."))
    }

    val authResult = Firebase.auth.signInWithEmailAndPassword(email.trim(), password).await()
    val user = authResult.user
      ?: return Result.failure(IllegalStateException("Sign-in did not return an authenticated user."))
    Result.success(user)
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Log.w(AUTH_TAG, "Email/Password sign-in failed: ${e.message}", e)
    Result.failure(IllegalStateException(AuthErrorMapper.mapException(e), e))
  }
}

/**
 * Sends a Firebase Authentication password reset email to the user's registered email address.
 */
suspend fun sendFirebasePasswordResetEmail(email: String): Result<Unit> {
  return try {
    AuthValidation.validateEmail(email)?.let { return Result.failure(IllegalArgumentException(it)) }
    Firebase.auth.sendPasswordResetEmail(email.trim()).await()
    Result.success(Unit)
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Log.w(AUTH_TAG, "Password reset email failed: ${e.message}", e)
    Result.failure(IllegalStateException(AuthErrorMapper.mapException(e, isPasswordReset = true), e))
  }
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (_: Exception) {
    onUnauthenticated()
    return
  }
  if (clientId.isBlank()) {
    onUnauthenticated()
    return
  }
  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()
  val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()

  scope.launch {
    try {
      val cred = credentialManager.getCredential(context, request).credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (_: Exception) {
    onAuthError("Google Sign-In configuration missing: default_web_client_id not found.")
    return
  }
  if (clientId.isBlank()) {
    onAuthError("Google Sign-In configuration missing: default_web_client_id is empty.")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder()
    .addCredentialOption(signInOption)
    .build()

  scope.launch {
    try {
      val activity = context as? Activity
      if (activity == null) {
        onAuthError("Unable to launch sign-in without an active Activity.")
        return@launch
      }
      val cred = credentialManager.getCredential(activity, request).credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type returned by Credential Manager.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e(AUTH_TAG, "Google Sign-In failed", e)
      onAuthError(AuthErrorMapper.mapException(e))
    }
  }
}

fun signOutSession(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onBeforeFirebaseSignOut: suspend () -> Unit = {},
  onSignOutComplete: () -> Unit = {},
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        onBeforeFirebaseSignOut()
      } catch (e: Exception) {
        Log.w(AUTH_TAG, "Pre-signout cleanup warning: ${e.message}")
      }
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onSignOutComplete()
      }
    }
  }
}
