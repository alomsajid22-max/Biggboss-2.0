package com.example.multiplayer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.credentials.CredentialManager
import com.example.R
import kotlinx.coroutines.launch

private val ObsidianBg = Color(0xFF0A0E19)
private val MidnightCard = Color(0xFF131A2C)
private val MidnightElevated = Color(0xFF1A233B)
private val GoldAccent = Color(0xFFEAB954)
private val EmeraldLive = Color(0xFF34D399)
private val CyanAdmin = Color(0xFF22D3EE)
private val MutedSlate = Color(0xFF94A3B8)
private val DangerRed = Color(0xFFF87171)

@Composable
private fun LuxuryBackdrop(content: @Composable () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.radialGradient(
          colors = listOf(Color(0xFF1D2847), ObsidianBg),
        )
      )
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .imePadding()
  ) {
    content()
  }
}

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  initialEntryStage: AuthEntryStage = AuthEntryStage.CHOOSER,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var entryStage by remember { mutableStateOf(initialEntryStage) }

  // 0 = Sign In, 1 = Create Account, 2 = Reset Password
  var authMode by remember { mutableIntStateOf(0) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }
  var infoMsg by remember { mutableStateOf<String?>(null) }

  BackHandler(enabled = entryStage != AuthEntryStage.CHOOSER) {
    errorMsg = null
    infoMsg = null
    entryStage = when (entryStage) {
      AuthEntryStage.ADMIN_AUTH -> AuthEntryStage.ADMIN_KEYWORD
      else -> AuthEntryStage.CHOOSER
    }
  }

  // Sign-In state
  var loginEmail by remember { mutableStateOf("") }
  var loginPassword by remember { mutableStateOf("") }
  var showLoginPass by remember { mutableStateOf(false) }

  // Register state (username stored as housemate displayName; login uses email + password)
  var regDisplayName by remember { mutableStateOf("") }
  var regEmail by remember { mutableStateOf("") }
  var regPassword by remember { mutableStateOf("") }
  var regConfirmPassword by remember { mutableStateOf("") }
  var showRegPass by remember { mutableStateOf(false) }
  var showRegConfirmPass by remember { mutableStateOf(false) }
  var regCharId by remember { mutableStateOf("Player 01") }

  // Forgot-Password state
  var resetEmail by remember { mutableStateOf("") }

  val fieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GoldAccent,
    unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = GoldAccent,
    unfocusedLabelColor = MutedSlate,
  )

  var adminLockoutStatus by remember { mutableStateOf(LocalAdminAuthManager.getLockoutStatus(context)) }

  LaunchedEffect(entryStage, adminLockoutStatus.isLocked) {
    if (entryStage == AuthEntryStage.ADMIN_AUTH) {
      adminLockoutStatus = LocalAdminAuthManager.getLockoutStatus(context)
      while (adminLockoutStatus.isLocked) {
        delay(1_000L)
        val refreshed = LocalAdminAuthManager.getLockoutStatus(context)
        adminLockoutStatus = refreshed
        if (!refreshed.isLocked) {
          errorMsg = null
          break
        }
      }
    }
  }

  val submitAdminLogin: (String, String) -> Unit = { adminUser, adminPass ->
    if (!isLoading) {
      errorMsg = null
      infoMsg = null
      when (val outcome = LocalAdminAuthManager.verifyAdminLogin(context, adminUser, adminPass)) {
        is LocalAdminAuthManager.LoginOutcome.Success -> {
          adminLockoutStatus = LocalAdminAuthManager.getLockoutStatus(context)
          onAuthSuccess()
        }
        is LocalAdminAuthManager.LoginOutcome.Failed -> {
          adminLockoutStatus = outcome.status
          errorMsg = outcome.message
        }
        is LocalAdminAuthManager.LoginOutcome.Locked -> {
          adminLockoutStatus = outcome.status
          errorMsg = null
        }
      }
    }
  }

  val submitLogin: () -> Unit = {
    if (!isLoading) {
      errorMsg = null
      infoMsg = null
      val eErr = AuthValidation.validateEmail(loginEmail)
      if (eErr != null) {
        errorMsg = eErr
      } else if (loginPassword.isEmpty()) {
        errorMsg = "Enter your password."
      } else {
        isLoading = true
        scope.launch {
          val res = loginWithEmailPassword(loginEmail, loginPassword)
          isLoading = false
          res.onSuccess {
            onAuthSuccess()
          }.onFailure { err ->
            errorMsg = err.message ?: "Sign-in failed."
          }
        }
      }
    }
  }

  val submitRegister: () -> Unit = {
    if (!isLoading) {
      errorMsg = null
      infoMsg = null
      val dErr = AuthValidation.validateDisplayName(regDisplayName)
      val eErr = AuthValidation.validateEmail(regEmail)
      val pErr = AuthValidation.validatePassword(regPassword)
      when {
        dErr != null -> errorMsg = dErr
        eErr != null -> errorMsg = eErr
        pErr != null -> errorMsg = pErr
        regPassword != regConfirmPassword -> errorMsg = "Passwords do not match."
        else -> {
          isLoading = true
          scope.launch {
            val res = registerWithEmailPassword(
              context = context,
              email = regEmail,
              password = regPassword,
              displayName = regDisplayName,
              characterId = regCharId,
            )
            isLoading = false
            res.onSuccess {
              onAuthSuccess()
            }.onFailure { err ->
              errorMsg = err.message ?: "Account creation failed."
            }
          }
        }
      }
    }
  }

  val submitReset: () -> Unit = {
    if (!isLoading) {
      errorMsg = null
      infoMsg = null
      val eErr = AuthValidation.validateEmail(resetEmail)
      if (eErr != null) {
        errorMsg = eErr
      } else {
        isLoading = true
        scope.launch {
          val res = sendFirebasePasswordResetEmail(resetEmail)
          isLoading = false
          res.onSuccess {
            val targetEmail = resetEmail.trim()
            loginEmail = targetEmail
            loginPassword = ""
            authMode = 0
            infoMsg = "Password reset link sent to $targetEmail. Check your inbox and spam folder."
          }.onFailure { err ->
            errorMsg = err.message ?: "Could not send password reset email."
          }
        }
      }
    }
  }

  val launchGoogleSignIn: () -> Unit = {
    if (!isLoading) {
      errorMsg = null
      infoMsg = null
      isLoading = true
      onGoogleSignInClicked(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {
          isLoading = false
          onAuthSuccess()
        },
        onAuthError = { msg ->
          isLoading = false
          errorMsg = msg
        },
        scope = scope,
        onAuthCancelled = {
          isLoading = false
          errorMsg = "Google Sign-In was cancelled."
        },
      )
    }
  }

  LuxuryBackdrop {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      contentAlignment = Alignment.Center,
    ) {
      val isWideLandscape = maxWidth >= 640.dp && entryStage == AuthEntryStage.PLAYER_AUTH

      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MidnightCard.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.34f)),
        shadowElevation = 18.dp,
        modifier = Modifier
          .widthIn(max = if (isWideLandscape) 940.dp else 540.dp)
          .let { if (isWideLandscape) it.fillMaxHeight() else it },
      ) {
        if (entryStage == AuthEntryStage.CHOOSER) {
          Column(
            modifier = Modifier
              .padding(horizontal = 20.dp, vertical = 18.dp)
              .verticalScroll(rememberScrollState()),
          ) {
            AuthEntryChooserPanel(
              onSelectPlayerLogin = {
                errorMsg = null
                infoMsg = null
                entryStage = AuthEntryStage.PLAYER_AUTH
              },
              onSelectAdminLogin = {
                errorMsg = null
                infoMsg = null
                entryStage = AuthEntryStage.ADMIN_KEYWORD
              },
            )
          }
        } else if (entryStage == AuthEntryStage.ADMIN_KEYWORD) {
          Column(
            modifier = Modifier
              .padding(horizontal = 20.dp, vertical = 18.dp)
              .verticalScroll(rememberScrollState()),
          ) {
            AdminKeywordEntrySection(
              fieldColors = fieldColors,
              onKeywordAccepted = {
                errorMsg = null
                infoMsg = null
                entryStage = AuthEntryStage.ADMIN_AUTH
              },
              onBack = {
                errorMsg = null
                entryStage = AuthEntryStage.CHOOSER
              },
            )
          }
        } else if (entryStage == AuthEntryStage.ADMIN_AUTH) {
          Column(
            modifier = Modifier
              .padding(horizontal = 20.dp, vertical = 18.dp)
              .verticalScroll(rememberScrollState()),
          ) {
            AdminAuthLoginSection(
              isLoading = isLoading,
              errorMsg = errorMsg,
              lockoutStatus = adminLockoutStatus,
              fieldColors = fieldColors,
              onSubmitAdminLogin = submitAdminLogin,
              onBack = {
                errorMsg = null
                entryStage = AuthEntryStage.ADMIN_KEYWORD
              },
            )
          }
        } else if (isWideLandscape) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 20.dp, vertical = 14.dp),
          ) {
            TextButton(
              onClick = {
                errorMsg = null
                infoMsg = null
                entryStage = AuthEntryStage.CHOOSER
              },
              modifier = Modifier.heightIn(min = 48.dp).testTag("player_auth_back_button"),
            ) {
              Text("← Login Options", color = GoldAccent, fontWeight = FontWeight.Bold)
            }
            // Compact Top Bar: Brand Identity + Mode Tabs
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(0.42f),
              ) {
                Box(
                  modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(GoldAccent.copy(alpha = 0.14f))
                    .border(1.5.dp, GoldAccent.copy(alpha = 0.6f), CircleShape),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Home,
                    contentDescription = stringResource(R.string.auth_hero_title),
                    tint = GoldAccent,
                    modifier = Modifier.size(22.dp),
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = stringResource(R.string.auth_brand_eyebrow),
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldAccent,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = stringResource(R.string.auth_hero_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                  )
                }
              }

              AuthModeTabs(
                authMode = authMode,
                isLoading = isLoading,
                onSelectMode = { idx ->
                  errorMsg = null
                  infoMsg = null
                  authMode = idx
                },
                modifier = Modifier.weight(0.58f),
              )
            }

            AuthStatusBanners(
              errorMsg = errorMsg,
              infoMsg = infoMsg,
              showSwitchToSignIn = authMode == 1 && AuthErrorMapper.isDuplicateEmailMessage(errorMsg),
              onSwitchToSignIn = {
                loginEmail = regEmail.trim()
                loginPassword = ""
                errorMsg = null
                infoMsg = "Switched to Sign In. Enter your password for ${regEmail.trim()}."
                authMode = 0
              },
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (authMode == 1) {
              // Create Account: Left = Credentials + Display Name, Right = 15-Cast Character Grid + Submit
              Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
              ) {
                Column(
                  modifier = Modifier
                    .weight(0.50f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                ) {
                  RegisterFieldsSection(
                    regDisplayName = regDisplayName,
                    onDisplayNameChange = { if (it.length <= 24) regDisplayName = it },
                    regEmail = regEmail,
                    onEmailChange = { if (it.length <= 160) regEmail = it },
                    regPassword = regPassword,
                    onPasswordChange = { if (it.length <= 128) regPassword = it },
                    regConfirmPassword = regConfirmPassword,
                    onConfirmPasswordChange = { if (it.length <= 128) regConfirmPassword = it },
                    showRegPass = showRegPass,
                    onToggleRegPass = { showRegPass = !showRegPass },
                    showRegConfirmPass = showRegConfirmPass,
                    onToggleRegConfirmPass = { showRegConfirmPass = !showRegConfirmPass },
                    isLoading = isLoading,
                    fieldColors = fieldColors,
                  )
                }

                Column(
                  modifier = Modifier
                    .weight(0.50f)
                    .fillMaxHeight(),
                ) {
                  val selectedChar = HousemateCatalog.findById(regCharId)
                  Text(
                    text = "3D HOUSEMATE CHARACTER • ${selectedChar.name.uppercase()} (${selectedChar.gender.uppercase()})",
                    color = GoldAccent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  CharacterSelectorGrid(
                    selectedCharId = regCharId,
                    onSelect = { regCharId = it },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Button(
                    onClick = submitRegister,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color(0xFF10141F)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("register_submit_button"),
                  ) {
                    if (isLoading) {
                      CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF10141F), strokeWidth = 2.5.dp)
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Creating Account...", fontWeight = FontWeight.Bold)
                    } else {
                      Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Create Account & Enter", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "All new registrations join with PLAYER role. Passwords are never saved in Firestore.",
                    color = MutedSlate,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                  )
                }
              }
            } else {
              // Sign In or Reset Password: Left = Overview + Optional Google Sign-In, Right = Email/Password Form
              Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
              ) {
                Column(
                  modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                  verticalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column {
                    Text(
                      text = stringResource(R.string.auth_hero_subtitle),
                      style = MaterialTheme.typography.bodySmall,
                      color = MutedSlate,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      InfoChip(
                        icon = Icons.Rounded.Groups,
                        label = "15 Players Max",
                        modifier = Modifier.weight(1f),
                      )
                      InfoChip(
                        icon = Icons.Rounded.Lock,
                        label = "Firebase Auth",
                        modifier = Modifier.weight(1f),
                      )
                    }
                  }

                  Column(modifier = Modifier.padding(top = 12.dp)) {
                    GoogleDividerRow()
                    Spacer(modifier = Modifier.height(8.dp))
                    GoogleSignInOutlinedButton(isLoading = isLoading, onClick = launchGoogleSignIn)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                      text = stringResource(R.string.auth_google_config_note),
                      style = MaterialTheme.typography.labelSmall,
                      color = MutedSlate.copy(alpha = 0.85f),
                    )
                  }
                }

                Column(
                  modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                  verticalArrangement = Arrangement.Center,
                ) {
                  if (authMode == 0) {
                    SignInFormSection(
                      loginEmail = loginEmail,
                      onEmailChange = { if (it.length <= 160) loginEmail = it },
                      loginPassword = loginPassword,
                      onPasswordChange = { if (it.length <= 128) loginPassword = it },
                      showLoginPass = showLoginPass,
                      onToggleLoginPass = { showLoginPass = !showLoginPass },
                      isLoading = isLoading,
                      fieldColors = fieldColors,
                      onForgotPasswordClick = {
                        errorMsg = null
                        infoMsg = null
                        resetEmail = loginEmail
                        authMode = 2
                      },
                      onSubmitLogin = submitLogin,
                    )
                  } else {
                    ResetPasswordFormSection(
                      resetEmail = resetEmail,
                      onEmailChange = { if (it.length <= 160) resetEmail = it },
                      isLoading = isLoading,
                      fieldColors = fieldColors,
                      onSubmitReset = submitReset,
                    )
                  }
                }
              }
            }
          }
        } else {
          // Compact / Portrait Single-Column Mobile Layout
          Column(
            modifier = Modifier
              .padding(horizontal = 20.dp, vertical = 18.dp)
              .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
              TextButton(
                onClick = {
                  errorMsg = null
                  infoMsg = null
                  entryStage = AuthEntryStage.CHOOSER
                },
                modifier = Modifier.heightIn(min = 48.dp).testTag("player_auth_back_button"),
              ) {
                Text("← Login Options", color = GoldAccent, fontWeight = FontWeight.Bold)
              }
            }
            Box(
              modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(GoldAccent.copy(alpha = 0.14f))
                .border(1.5.dp, GoldAccent.copy(alpha = 0.6f), CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.Home,
                contentDescription = stringResource(R.string.auth_hero_title),
                tint = GoldAccent,
                modifier = Modifier.size(26.dp),
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = stringResource(R.string.auth_brand_eyebrow),
              style = MaterialTheme.typography.labelSmall,
              color = GoldAccent,
              letterSpacing = 1.8.sp,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = stringResource(R.string.auth_hero_title),
              style = MaterialTheme.typography.headlineSmall,
              color = Color.White,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 2.5.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = stringResource(R.string.auth_hero_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MutedSlate,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))

            AuthModeTabs(
              authMode = authMode,
              isLoading = isLoading,
              onSelectMode = { idx ->
                errorMsg = null
                infoMsg = null
                authMode = idx
              },
              modifier = Modifier.fillMaxWidth(),
            )

            AuthStatusBanners(
              errorMsg = errorMsg,
              infoMsg = infoMsg,
              showSwitchToSignIn = authMode == 1 && AuthErrorMapper.isDuplicateEmailMessage(errorMsg),
              onSwitchToSignIn = {
                loginEmail = regEmail.trim()
                loginPassword = ""
                errorMsg = null
                infoMsg = "Switched to Sign In. Enter your password for ${regEmail.trim()}."
                authMode = 0
              },
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (authMode) {
              0 -> SignInFormSection(
                loginEmail = loginEmail,
                onEmailChange = { if (it.length <= 160) loginEmail = it },
                loginPassword = loginPassword,
                onPasswordChange = { if (it.length <= 128) loginPassword = it },
                showLoginPass = showLoginPass,
                onToggleLoginPass = { showLoginPass = !showLoginPass },
                isLoading = isLoading,
                fieldColors = fieldColors,
                onForgotPasswordClick = {
                  errorMsg = null
                  infoMsg = null
                  resetEmail = loginEmail
                  authMode = 2
                },
                onSubmitLogin = submitLogin,
              )

              1 -> {
                RegisterFieldsSection(
                  regDisplayName = regDisplayName,
                  onDisplayNameChange = { if (it.length <= 24) regDisplayName = it },
                  regEmail = regEmail,
                  onEmailChange = { if (it.length <= 160) regEmail = it },
                  regPassword = regPassword,
                  onPasswordChange = { if (it.length <= 128) regPassword = it },
                  regConfirmPassword = regConfirmPassword,
                  onConfirmPasswordChange = { if (it.length <= 128) regConfirmPassword = it },
                  showRegPass = showRegPass,
                  onToggleRegPass = { showRegPass = !showRegPass },
                  showRegConfirmPass = showRegConfirmPass,
                  onToggleRegConfirmPass = { showRegConfirmPass = !showRegConfirmPass },
                  isLoading = isLoading,
                  fieldColors = fieldColors,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = "SELECT 3D HOUSEMATE CHARACTER (15 CAST)",
                  color = GoldAccent,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.align(Alignment.Start),
                )
                Spacer(modifier = Modifier.height(6.dp))
                CharacterSelectorGrid(
                  selectedCharId = regCharId,
                  onSelect = { regCharId = it },
                  modifier = Modifier.fillMaxWidth().height(176.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                  onClick = submitRegister,
                  enabled = !isLoading,
                  colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color(0xFF10141F)),
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.fillMaxWidth().height(50.dp).testTag("register_submit_button"),
                ) {
                  if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF10141F), strokeWidth = 2.5.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creating Account...", fontWeight = FontWeight.Bold)
                  } else {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Account & Enter", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                  }
                }
              }

              2 -> ResetPasswordFormSection(
                resetEmail = resetEmail,
                onEmailChange = { if (it.length <= 160) resetEmail = it },
                isLoading = isLoading,
                fieldColors = fieldColors,
                onSubmitReset = submitReset,
              )
            }

            Spacer(modifier = Modifier.height(12.dp))
            GoogleDividerRow()
            Spacer(modifier = Modifier.height(10.dp))
            GoogleSignInOutlinedButton(isLoading = isLoading, onClick = launchGoogleSignIn)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = stringResource(R.string.auth_google_config_note),
              style = MaterialTheme.typography.labelSmall,
              color = MutedSlate.copy(alpha = 0.85f),
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AuthModeTabs(
  authMode: Int,
  isLoading: Boolean,
  onSelectMode: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    listOf(
      Triple(0, "Sign In", "auth_tab_login"),
      Triple(1, "Create Account", "auth_tab_register"),
      Triple(2, "Reset Password", "auth_tab_reset"),
    ).forEach { (idx, label, tag) ->
      val selected = authMode == idx
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) GoldAccent else MidnightElevated,
        border = BorderStroke(1.dp, if (selected) GoldAccent else Color.White.copy(alpha = 0.12f)),
        modifier = Modifier
          .weight(1f)
          .heightIn(min = 48.dp)
          .clickable(enabled = !isLoading) { onSelectMode(idx) }
          .testTag(tag),
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)) {
          Text(
            text = label,
            color = if (selected) Color(0xFF10141F) else Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun AuthStatusBanners(
  errorMsg: String?,
  infoMsg: String?,
  showSwitchToSignIn: Boolean = false,
  onSwitchToSignIn: (() -> Unit)? = null,
) {
  AnimatedVisibility(visible = !errorMsg.isNullOrBlank(), enter = fadeIn(), exit = fadeOut()) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = DangerRed.copy(alpha = 0.14f),
      border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.45f)),
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = errorMsg.orEmpty(),
          color = Color(0xFFFECACA),
          style = MaterialTheme.typography.bodySmall,
        )
        if (showSwitchToSignIn && onSwitchToSignIn != null) {
          OutlinedButton(
            onClick = onSwitchToSignIn,
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.65f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
            modifier = Modifier.heightIn(min = 48.dp).testTag("switch_to_signin_button"),
          ) {
            Text("Switch to Sign In", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }

  AnimatedVisibility(visible = !infoMsg.isNullOrBlank(), enter = fadeIn(), exit = fadeOut()) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = EmeraldLive.copy(alpha = 0.14f),
      border = BorderStroke(1.dp, EmeraldLive.copy(alpha = 0.45f)),
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
      Text(
        text = infoMsg.orEmpty(),
        color = Color(0xFFA7F3D0),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(10.dp),
      )
    }
  }
}

@Composable
private fun SignInFormSection(
  loginEmail: String,
  onEmailChange: (String) -> Unit,
  loginPassword: String,
  onPasswordChange: (String) -> Unit,
  showLoginPass: Boolean,
  onToggleLoginPass: () -> Unit,
  isLoading: Boolean,
  fieldColors: androidx.compose.material3.TextFieldColors,
  onForgotPasswordClick: () -> Unit,
  onSubmitLogin: () -> Unit,
) {
  OutlinedTextField(
    value = loginEmail,
    onValueChange = onEmailChange,
    singleLine = true,
    enabled = !isLoading,
    label = { Text("Email Address") },
    placeholder = { Text("e.g., player@example.com") },
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.Email,
      imeAction = androidx.compose.ui.text.input.ImeAction.Next,
    ),
    colors = fieldColors,
    modifier = Modifier.fillMaxWidth().testTag("login_email_input"),
  )
  Spacer(modifier = Modifier.height(8.dp))
  OutlinedTextField(
    value = loginPassword,
    onValueChange = onPasswordChange,
    singleLine = true,
    enabled = !isLoading,
    label = { Text("Password") },
    visualTransformation = if (showLoginPass) VisualTransformation.None else PasswordVisualTransformation(),
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.Password,
      imeAction = androidx.compose.ui.text.input.ImeAction.Done,
    ),
    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
      onDone = { if (!isLoading) onSubmitLogin() },
    ),
    trailingIcon = {
      IconButton(
        onClick = onToggleLoginPass,
        modifier = Modifier.size(48.dp),
      ) {
        Icon(
          imageVector = if (showLoginPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
          contentDescription = if (showLoginPass) "Hide password" else "Show password",
          tint = MutedSlate,
        )
      }
    },
    colors = fieldColors,
    modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
  )
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
    TextButton(
      onClick = onForgotPasswordClick,
      enabled = !isLoading,
      modifier = Modifier.heightIn(min = 48.dp).testTag("forgot_password_button"),
    ) {
      Text("Forgot Password?", color = GoldAccent, fontWeight = FontWeight.SemiBold)
    }
  }
  Button(
    onClick = onSubmitLogin,
    enabled = !isLoading,
    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color(0xFF10141F)),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("login_submit_button"),
  ) {
    if (isLoading) {
      CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF10141F), strokeWidth = 2.5.dp)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Signing In...", fontWeight = FontWeight.Bold)
    } else {
      Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Sign In", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
  }
}

@Composable
private fun RegisterFieldsSection(
  regDisplayName: String,
  onDisplayNameChange: (String) -> Unit,
  regEmail: String,
  onEmailChange: (String) -> Unit,
  regPassword: String,
  onPasswordChange: (String) -> Unit,
  regConfirmPassword: String,
  onConfirmPasswordChange: (String) -> Unit,
  showRegPass: Boolean,
  onToggleRegPass: () -> Unit,
  showRegConfirmPass: Boolean,
  onToggleRegConfirmPass: () -> Unit,
  isLoading: Boolean,
  fieldColors: androidx.compose.material3.TextFieldColors,
) {
  OutlinedTextField(
    value = regDisplayName,
    onValueChange = onDisplayNameChange,
    singleLine = true,
    enabled = !isLoading,
    label = { Text("Username / Display Name (3–24 chars)") },
    placeholder = { Text("e.g., AriaVIP") },
    keyboardOptions = KeyboardOptions(
      imeAction = androidx.compose.ui.text.input.ImeAction.Next,
    ),
    colors = fieldColors,
    modifier = Modifier.fillMaxWidth().testTag("register_display_name_input"),
  )
  Spacer(modifier = Modifier.height(6.dp))
  OutlinedTextField(
    value = regEmail,
    onValueChange = onEmailChange,
    singleLine = true,
    enabled = !isLoading,
    label = { Text("Email Address") },
    placeholder = { Text("e.g., player@example.com") },
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.Email,
      imeAction = androidx.compose.ui.text.input.ImeAction.Next,
    ),
    colors = fieldColors,
    modifier = Modifier.fillMaxWidth().testTag("register_email_input"),
  )
  Spacer(modifier = Modifier.height(6.dp))
  OutlinedTextField(
    value = regPassword,
    onValueChange = onPasswordChange,
    singleLine = true,
    enabled = !isLoading,
    label = { Text("Password (8+ characters)") },
    visualTransformation = if (showRegPass) VisualTransformation.None else PasswordVisualTransformation(),
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.Password,
      imeAction = androidx.compose.ui.text.input.ImeAction.Next,
    ),
    trailingIcon = {
      IconButton(
        onClick = onToggleRegPass,
        modifier = Modifier.size(48.dp),
      ) {
        Icon(
          imageVector = if (showRegPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
          contentDescription = if (showRegPass) "Hide password" else "Show password",
          tint = MutedSlate,
        )
      }
    },
    colors = fieldColors,
    modifier = Modifier.fillMaxWidth().testTag("register_password_input"),
  )
  Spacer(modifier = Modifier.height(6.dp))
  OutlinedTextField(
    value = regConfirmPassword,
    onValueChange = onConfirmPasswordChange,
    singleLine = true,
    enabled = !isLoading,
    label = { Text("Confirm Password") },
    visualTransformation = if (showRegConfirmPass) VisualTransformation.None else PasswordVisualTransformation(),
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.Password,
      imeAction = androidx.compose.ui.text.input.ImeAction.Done,
    ),
    trailingIcon = {
      IconButton(
        onClick = onToggleRegConfirmPass,
        modifier = Modifier.size(48.dp),
      ) {
        Icon(
          imageVector = if (showRegConfirmPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
          contentDescription = if (showRegConfirmPass) "Hide confirm password" else "Show confirm password",
          tint = MutedSlate,
        )
      }
    },
    colors = fieldColors,
    modifier = Modifier.fillMaxWidth().testTag("register_confirm_password_input"),
  )
}

@Composable
private fun ResetPasswordFormSection(
  resetEmail: String,
  onEmailChange: (String) -> Unit,
  isLoading: Boolean,
  fieldColors: androidx.compose.material3.TextFieldColors,
  onSubmitReset: () -> Unit,
) {
  Text(
    text = "Enter your registered email address to receive a Firebase password reset link.",
    color = MutedSlate,
    style = MaterialTheme.typography.bodySmall,
  )
  Spacer(modifier = Modifier.height(10.dp))
  OutlinedTextField(
    value = resetEmail,
    onValueChange = onEmailChange,
    singleLine = true,
    enabled = !isLoading,
    label = { Text("Registered Email Address") },
    placeholder = { Text("e.g., player@example.com") },
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.Email,
      imeAction = androidx.compose.ui.text.input.ImeAction.Done,
    ),
    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
      onDone = { if (!isLoading) onSubmitReset() },
    ),
    colors = fieldColors,
    modifier = Modifier.fillMaxWidth().testTag("reset_email_input"),
  )
  Spacer(modifier = Modifier.height(12.dp))
  Button(
    onClick = onSubmitReset,
    enabled = !isLoading,
    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color(0xFF10141F)),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("reset_submit_button"),
  ) {
    if (isLoading) {
      CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF10141F), strokeWidth = 2.5.dp)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Sending Reset Email...", fontWeight = FontWeight.Bold)
    } else {
      Text("Send Password Reset Email", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
  }
}

@Composable
private fun GoogleDividerRow() {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.14f))
    Text("OR", color = MutedSlate, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.14f))
  }
}

@Composable
private fun GoogleSignInOutlinedButton(
  isLoading: Boolean,
  onClick: () -> Unit,
) {
  OutlinedButton(
    onClick = onClick,
    enabled = !isLoading,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .height(48.dp)
      .testTag("google_signin_button"),
  ) {
    Icon(
      imageVector = Icons.AutoMirrored.Rounded.Login,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(18.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = stringResource(R.string.auth_google_cta),
      color = Color.White,
      fontWeight = FontWeight.Bold,
      fontSize = 14.sp,
    )
  }
}

@Composable
private fun InfoChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MidnightElevated,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    modifier = modifier,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
      Icon(icon, contentDescription = null, tint = EmeraldLive, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun ProfileSetupScreen(
  initialDisplayName: String,
  initialCharacterId: String,
  isEditingExisting: Boolean,
  isBusy: Boolean,
  errorMessage: String?,
  onSaveProfile: (displayName: String, characterId: String) -> Unit,
  onCancelEdit: (() -> Unit)? = null,
  onSignOut: () -> Unit,
) {
  var displayName by remember(initialDisplayName) { mutableStateOf(initialDisplayName) }
  var selectedCharId by remember(initialCharacterId) { mutableStateOf(initialCharacterId.ifBlank { "Player 01" }) }

  val trimmed = displayName.trim()
  val isNameValid = trimmed.length in 3..24 &&
    trimmed.lowercase().replace(Regex("[^a-z0-9_-]"), "_").length >= 3

  LuxuryBackdrop {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      contentAlignment = Alignment.Center,
    ) {
      val wideLayout = maxWidth > 700.dp

      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MidnightCard.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.28f)),
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 920.dp)
          .heightIn(max = 640.dp),
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(24.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = if (isEditingExisting) "Update Housemate Profile" else stringResource(R.string.reg_title),
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                )
                Text(
                  text = stringResource(R.string.reg_subtitle),
                  style = MaterialTheme.typography.bodySmall,
                  color = MutedSlate,
                )
              }
            }
            TextButton(
              onClick = onCancelEdit ?: onSignOut,
              modifier = Modifier.heightIn(min = 48.dp),
            ) {
              Text(
                text = if (onCancelEdit != null) "Back to Lobby" else stringResource(R.string.lobby_logout),
                color = GoldAccent,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          if (wideLayout) {
            Row(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
              Column(modifier = Modifier.weight(0.42f)) {
                ProfileNamePanel(
                  displayName = displayName,
                  onDisplayNameChange = { displayName = it },
                  selectedCharId = selectedCharId,
                  isNameValid = isNameValid,
                  errorMessage = errorMessage,
                )
              }
              Column(modifier = Modifier.weight(0.58f)) {
                Text(
                  text = "CHOOSE 3D HOUSEMATE AVATAR (15 CAST)",
                  style = MaterialTheme.typography.labelSmall,
                  color = GoldAccent,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.2.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                CharacterSelectorGrid(
                  selectedCharId = selectedCharId,
                  onSelect = { selectedCharId = it },
                  modifier = Modifier.weight(1f),
                )
              }
            }
          } else {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
              ProfileNamePanel(
                displayName = displayName,
                onDisplayNameChange = { displayName = it },
                selectedCharId = selectedCharId,
                isNameValid = isNameValid,
                errorMessage = errorMessage,
              )
              Spacer(modifier = Modifier.height(12.dp))
              CharacterSelectorGrid(
                selectedCharId = selectedCharId,
                onSelect = { selectedCharId = it },
                modifier = Modifier.height(220.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = { onSaveProfile(trimmed, selectedCharId) },
            enabled = isNameValid && !isBusy,
            colors = ButtonDefaults.buttonColors(
              containerColor = GoldAccent,
              contentColor = Color(0xFF10141F),
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("save_profile_button"),
          ) {
            if (isBusy) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF10141F), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Reserving Unique Profile...", fontWeight = FontWeight.Bold)
            } else {
              Text(
                text = if (isEditingExisting) "Save Changes" else stringResource(R.string.reg_save_profile),
                fontWeight = FontWeight.ExtraBold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ProfileNamePanel(
  displayName: String,
  onDisplayNameChange: (String) -> Unit,
  selectedCharId: String,
  isNameValid: Boolean,
  errorMessage: String?,
) {
  val selectedOption = HousemateCatalog.findById(selectedCharId)
  OutlinedTextField(
    value = displayName,
    onValueChange = { if (it.length <= 24) onDisplayNameChange(it) },
    singleLine = true,
    label = { Text(stringResource(R.string.reg_display_name_label)) },
    placeholder = { Text("e.g., AriaVIP") },
    isError = displayName.isNotEmpty() && !isNameValid,
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = GoldAccent,
      unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
      focusedTextColor = Color.White,
      unfocusedTextColor = Color.White,
      focusedLabelColor = GoldAccent,
      unfocusedLabelColor = MutedSlate,
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("display_name_input"),
  )

  Spacer(modifier = Modifier.height(10.dp))

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MidnightElevated,
    border = BorderStroke(1.dp, selectedOption.primaryColor.copy(alpha = 0.5f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(selectedOption.primaryColor)
          .border(2.dp, selectedOption.accentColor, RoundedCornerShape(12.dp)),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = "${selectedOption.id} • ${selectedOption.name} (${selectedOption.gender})",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.titleSmall,
        )
        Text(
          text = "${selectedOption.styleSummary} • ${selectedOption.archetype}",
          color = MutedSlate,
          style = MaterialTheme.typography.bodySmall,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }

  if (!errorMessage.isNullOrBlank()) {
    Spacer(modifier = Modifier.height(10.dp))
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = DangerRed.copy(alpha = 0.14f),
      border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = errorMessage,
        color = Color(0xFFFECACA),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(10.dp),
      )
    }
  }
}

@Composable
private fun CharacterSelectorGrid(
  selectedCharId: String,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 138.dp),
    contentPadding = PaddingValues(2.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier,
  ) {
    items(HousemateCatalog.all, key = { it.id }) { option ->
      val selected = option.id == selectedCharId
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) GoldAccent.copy(alpha = 0.18f) else MidnightElevated,
        border = BorderStroke(
          width = if (selected) 1.5.dp else 1.dp,
          color = if (selected) GoldAccent else Color.White.copy(alpha = 0.1f),
        ),
        modifier = Modifier
          .heightIn(min = 56.dp)
          .clickable { onSelect(option.id) }
          .testTag("char_option_${option.number}"),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(10.dp),
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(option.primaryColor)
              .border(1.5.dp, option.accentColor, RoundedCornerShape(8.dp)),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = option.name,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.bodySmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${option.id} • ${option.gender}",
              color = if (selected) GoldAccent else MutedSlate,
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
fun LobbyScreen(
  account: UserAccount,
  isAdminVerified: Boolean,
  seasonState: UiState<SeasonSession?>,
  membersState: UiState<List<SeasonMember>>,
  activeVotingCycle: VotingCycle?,
  captainUid: String?,
  vipPrivileges: List<VipPrivilege> = emptyList(),
  activeTask: HouseTask?,
  isRtdbConfigured: Boolean,
  sessionSuperseded: Boolean,
  isBusy: Boolean,
  actionError: String?,
  onJoinHouse: () -> Unit,
  onEditProfile: () -> Unit,
  onOpenAdminSuite: () -> Unit,
  onOpenAiConsole: () -> Unit = {},
  onOpenNominations: () -> Unit = {},
  onOpenVotingBallot: () -> Unit,
  onOpenHouseHub: () -> Unit,
  onSignOut: () -> Unit,
  onClearError: () -> Unit,
) {
  val season = (seasonState as? UiState.Success)?.data
  val members = (membersState as? UiState.Success)?.data.orEmpty()
  val activeMembers = remember(members) {
    members.filter { it.role != PlayerRole.ADMIN.name && it.status !in listOf("EVICTED", "BANNED", "REMOVED") }
  }
  val myMembership = remember(members, account.userId) {
    members.firstOrNull { it.userId == account.userId }
  }
  val isEvicted = myMembership?.status == "EVICTED"
  val isBanned = account.banned || myMembership?.status == "BANNED"
  val onlineCount = activeMembers.count { it.online }
  val occupiedCount = activeMembers.size.coerceAtMost(15)
  val availableSlots = (15 - occupiedCount).coerceAtLeast(0)
  val seasonStatus = season?.status ?: "LOBBY"
  val selectedCharacter = HousemateCatalog.findById(account.characterId)
  val canJoinHouse = !isBusy && !isBanned && !isEvicted && seasonStatus != "ENDED"

  LuxuryBackdrop {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(14.dp),
    ) {
      val wideLayout = maxWidth > 680.dp

      Column(modifier = Modifier.fillMaxSize()) {
        // Header Bar
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MidnightCard.copy(alpha = 0.92f),
          border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text(
                text = "BIGG BOSS – THE HOUSE • MULTIPLAYER LOBBY",
                color = GoldAccent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.6.sp,
              )
              Text(
                text = season?.title ?: "Season 01 • Bigg Boss – The House",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }

            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              StatusBadge(
                label = "$seasonStatus • $occupiedCount/15 Slots",
                dotColor = EmeraldLive,
              )
              StatusBadge(
                label = if (isRtdbConfigured) "RTDB Live" else "Firestore Sync",
                dotColor = if (isRtdbConfigured) EmeraldLive else GoldAccent,
              )
              OutlinedButton(
                onClick = onSignOut,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .height(48.dp)
                  .testTag("logout_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.Logout,
                  contentDescription = stringResource(R.string.lobby_logout),
                  tint = Color.White,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.lobby_logout), color = Color.White)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (sessionSuperseded) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = DangerRed.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
          ) {
            Text(
              text = "Session paused: your account opened the house on another device. Tap Join House to reclaim.",
              color = Color(0xFFFECACA),
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(10.dp),
            )
          }
        }

        if (!actionError.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = DangerRed.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 10.dp)
              .clickable { onClearError() },
          ) {
            Text(
              text = actionError,
              color = Color(0xFFFECACA),
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(10.dp),
            )
          }
        }

        if (wideLayout) {
          Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            PlayerProfileLobbyCard(
              account = account,
              selectedCharacter = selectedCharacter,
              isAdminVerified = isAdminVerified,
              isBanned = isBanned,
              isEvicted = isEvicted,
              seasonEnded = seasonStatus == "ENDED",
              canJoinHouse = canJoinHouse,
              activeVotingCycle = activeVotingCycle,
              activeTask = activeTask,
              isCaptain = !captainUid.isNullOrBlank() && captainUid == account.userId,
              isVip = vipPrivileges.any { it.userId == account.userId && it.effectiveVip },
              onlineCount = onlineCount,
              availableSlots = availableSlots,
              isRtdbConfigured = isRtdbConfigured,
              isBusy = isBusy,
              onJoinHouse = onJoinHouse,
              onEditProfile = onEditProfile,
              onOpenAdminSuite = onOpenAdminSuite,
              onOpenAiConsole = onOpenAiConsole,
              onOpenNominations = onOpenNominations,
              onOpenVotingBallot = onOpenVotingBallot,
              onOpenHouseHub = onOpenHouseHub,
              modifier = Modifier.weight(0.38f).fillMaxHeight(),
            )
            SeasonSlotsCard(
              members = activeMembers,
              currentUserId = account.userId,
              captainUid = captainUid,
              vipPrivileges = vipPrivileges,
              membersState = membersState,
              modifier = Modifier.weight(0.62f).fillMaxHeight(),
            )
          }
        } else {
          Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            PlayerProfileLobbyCard(
              account = account,
              selectedCharacter = selectedCharacter,
              isAdminVerified = isAdminVerified,
              isBanned = isBanned,
              isEvicted = isEvicted,
              seasonEnded = seasonStatus == "ENDED",
              canJoinHouse = canJoinHouse,
              activeVotingCycle = activeVotingCycle,
              activeTask = activeTask,
              isCaptain = !captainUid.isNullOrBlank() && captainUid == account.userId,
              isVip = vipPrivileges.any { it.userId == account.userId && it.effectiveVip },
              onlineCount = onlineCount,
              availableSlots = availableSlots,
              isRtdbConfigured = isRtdbConfigured,
              isBusy = isBusy,
              onJoinHouse = onJoinHouse,
              onEditProfile = onEditProfile,
              onOpenAdminSuite = onOpenAdminSuite,
              onOpenAiConsole = onOpenAiConsole,
              onOpenNominations = onOpenNominations,
              onOpenVotingBallot = onOpenVotingBallot,
              onOpenHouseHub = onOpenHouseHub,
              scrollable = false,
              modifier = Modifier.fillMaxWidth(),
            )
            SeasonSlotsCard(
              members = activeMembers,
              currentUserId = account.userId,
              captainUid = captainUid,
              vipPrivileges = vipPrivileges,
              membersState = membersState,
              modifier = Modifier.fillMaxWidth().height(340.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StatusBadge(label: String, dotColor: Color) {
  Surface(
    shape = RoundedCornerShape(999.dp),
    color = MidnightElevated,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(dotColor),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun PlayerProfileLobbyCard(
  account: UserAccount,
  selectedCharacter: HousemateOption,
  isAdminVerified: Boolean,
  isBanned: Boolean,
  isEvicted: Boolean,
  seasonEnded: Boolean,
  canJoinHouse: Boolean,
  activeVotingCycle: VotingCycle?,
  activeTask: HouseTask?,
  isCaptain: Boolean,
  isVip: Boolean = false,
  onlineCount: Int,
  availableSlots: Int,
  isRtdbConfigured: Boolean,
  isBusy: Boolean,
  onJoinHouse: () -> Unit,
  onEditProfile: () -> Unit,
  onOpenAdminSuite: () -> Unit,
  onOpenAiConsole: () -> Unit,
  onOpenNominations: () -> Unit,
  onOpenVotingBallot: () -> Unit,
  onOpenHouseHub: () -> Unit,
  scrollable: Boolean = true,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MidnightCard.copy(alpha = 0.94f),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    modifier = modifier,
  ) {
    val contentModifier = if (scrollable) {
      Modifier.padding(16.dp).verticalScroll(rememberScrollState())
    } else {
      Modifier.padding(16.dp)
    }
    Column(
      modifier = contentModifier,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              val prefix = buildString {
                if (isCaptain) append("👑 ")
                if (isVip) append("💎 ")
              }
              Text(
                text = "$prefix${account.displayName}",
                color = if (isCaptain) GoldAccent else Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Badge, contentDescription = null, tint = MutedSlate, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = account.playerId,
                  color = GoldAccent,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isAdminVerified) CyanAdmin.copy(alpha = 0.16f) else GoldAccent.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, if (isAdminVerified) CyanAdmin else GoldAccent.copy(alpha = 0.5f)),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
              Icon(
                imageVector = if (isAdminVerified) Icons.Rounded.Shield else Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = if (isAdminVerified) CyanAdmin else GoldAccent,
                modifier = Modifier.size(13.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isAdminVerified) "ADMIN" else "PLAYER",
                color = if (isAdminVerified) CyanAdmin else GoldAccent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MidnightElevated,
          border = BorderStroke(1.dp, selectedCharacter.primaryColor.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(selectedCharacter.primaryColor)
                  .border(1.5.dp, selectedCharacter.accentColor, RoundedCornerShape(10.dp)),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "${selectedCharacter.id} • ${selectedCharacter.name} (${selectedCharacter.gender})",
                  color = Color.White,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Spawns in ${selectedCharacter.spawnRoom}",
                  color = MutedSlate,
                  style = MaterialTheme.typography.labelSmall,
                )
              }
            }
            TextButton(
              onClick = onEditProfile,
              modifier = Modifier.heightIn(min = 48.dp),
            ) {
              Icon(Icons.Rounded.Edit, contentDescription = "Edit Profile", tint = GoldAccent, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Edit", color = GoldAccent)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          LobbyStatBox(
            label = "ONLINE NOW",
            value = "$onlineCount Real",
            modifier = Modifier.weight(1f),
          )
          LobbyStatBox(
            label = "OPEN SLOTS",
            value = "$availableSlots / 15",
            modifier = Modifier.weight(1f),
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mandatory common-area CCTV monitoring privacy notice
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = EmeraldLive.copy(alpha = 0.1f),
          border = BorderStroke(1.dp, EmeraldLive.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Rounded.Videocam, contentDescription = null, tint = EmeraldLive, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Common areas are monitored by CCTV • Bathrooms remain private.",
              color = Color(0xFFA7F3D0),
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }

        if (!isRtdbConfigured) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Sync, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Enable Realtime Database in Firebase Console for live cross-device 3D movement sync.",
              color = MutedSlate,
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (isBanned) {
        Text(
          text = account.banReason ?: "This account has been banned by the Admin Control Room.",
          color = Color(0xFFFECACA),
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = 8.dp),
        )
      } else if (isEvicted) {
        Text(
          text = "You have been evicted from this season. Historical stats remain preserved.",
          color = GoldAccent,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = 8.dp),
        )
      } else if (seasonEnded) {
        Text(
          text = "Season has ended by Admin Control.",
          color = GoldAccent,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = 8.dp),
        )
      }

      if (isAdminVerified) {
        OutlinedButton(
          onClick = onOpenAdminSuite,
          border = BorderStroke(1.dp, CyanAdmin),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(bottom = 8.dp)
            .testTag("lobby_admin_suite_button"),
        ) {
          Icon(Icons.Rounded.Shield, contentDescription = null, tint = CyanAdmin, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Open Admin Control Suite (Chair 2)", color = CyanAdmin, fontWeight = FontWeight.ExtraBold)
        }
        OutlinedButton(
          onClick = onOpenAiConsole,
          border = BorderStroke(1.dp, GoldAccent),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(bottom = 8.dp)
            .testTag("lobby_ai_operator_button"),
        ) {
          Icon(Icons.Rounded.Shield, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Open ChatGPT AI Operator (Chair 1)", color = GoldAccent, fontWeight = FontWeight.ExtraBold)
        }
      }

      if (!isBanned && !isEvicted) {
        val isTaskLive = activeTask?.status == "ACTIVE"
        OutlinedButton(
          onClick = onOpenHouseHub,
          border = BorderStroke(1.dp, if (isTaskLive) EmeraldLive else GoldAccent.copy(alpha = 0.65f)),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(bottom = 8.dp)
            .testTag("lobby_house_hub_button"),
        ) {
          Icon(
            Icons.Rounded.Assignment,
            contentDescription = null,
            tint = if (isTaskLive) EmeraldLive else GoldAccent,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isTaskLive) "Live Task: ${activeTask?.title}" else "Tasks, Teams, Rules & Schedule",
            color = if (isTaskLive) EmeraldLive else GoldAccent,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      if (activeVotingCycle?.status == "NOMINATIONS" && !isBanned && !isEvicted && !isAdminVerified) {
        OutlinedButton(
          onClick = onOpenNominations,
          border = BorderStroke(1.dp, CyanAdmin),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(bottom = 8.dp)
            .testTag("open_nominations_button"),
        ) {
          Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = CyanAdmin, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Nominate Now: ${activeVotingCycle.title}", color = CyanAdmin, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }

      if (activeVotingCycle?.status in listOf("OPEN", "CLOSED", "CANCELLED") && !isBanned && !isEvicted) {
        OutlinedButton(
          onClick = onOpenVotingBallot,
          border = BorderStroke(1.dp, GoldAccent),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(bottom = 8.dp)
            .testTag("open_ballot_button"),
        ) {
          Icon(Icons.Rounded.HowToVote, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          val voteCta = when (activeVotingCycle?.status) {
            "OPEN" -> "Vote Now: ${activeVotingCycle.title}"
            "CANCELLED" -> "Voting Cancelled: ${activeVotingCycle.title}"
            else -> "Voting Closed: ${activeVotingCycle?.title}"
          }
          Text(voteCta, color = GoldAccent, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }

      Button(
        onClick = onJoinHouse,
        enabled = canJoinHouse,
        colors = ButtonDefaults.buttonColors(
          containerColor = GoldAccent,
          contentColor = Color(0xFF10141F),
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("join_house_button"),
      ) {
        if (isBusy) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF10141F), strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Entering House...", fontWeight = FontWeight.Bold)
        } else {
          Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.lobby_join_house), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
      }
    }
  }
}

@Composable
private fun LobbyStatBox(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MidnightElevated,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(label, color = MutedSlate, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
      Text(value, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun SeasonSlotsCard(
  members: List<SeasonMember>,
  currentUserId: String,
  captainUid: String?,
  vipPrivileges: List<VipPrivilege> = emptyList(),
  membersState: UiState<List<SeasonMember>>,
  modifier: Modifier = Modifier,
) {
  val slotMap = remember(members) { members.associateBy { it.slotNumber.toInt() } }
  val activeVipUids = remember(vipPrivileges) { vipPrivileges.filter { it.effectiveVip }.map { it.userId }.toSet() }

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MidnightCard.copy(alpha = 0.94f),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "SHARED SEASON ROSTER (15 REAL PLAYER SLOTS)",
            color = GoldAccent,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
          )
          Text(
            text = "Authenticated contestants occupy live house slots; NPCs are separate.",
            color = MutedSlate,
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (membersState is UiState.Error) {
        Text(
          text = membersState.message,
          color = Color(0xFFFECACA),
          style = MaterialTheme.typography.bodySmall,
        )
      }

      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 152.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f),
      ) {
        items((1..15).toList(), key = { it }) { slot ->
          val occupant = slotMap[slot]
          val isMe = occupant?.userId == currentUserId
          val isSlotCaptain = !captainUid.isNullOrBlank() && occupant?.userId == captainUid
          val isSlotVip = occupant != null && activeVipUids.contains(occupant.userId)
          val charOpt = occupant?.let { HousemateCatalog.findById(it.characterId) }

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = when {
              isMe -> GoldAccent.copy(alpha = 0.16f)
              isSlotCaptain -> GoldAccent.copy(alpha = 0.10f)
              occupant != null -> MidnightElevated
              else -> Color(0xFF0E1424)
            },
            border = BorderStroke(
              width = if (isMe || isSlotCaptain) 1.5.dp else 1.dp,
              color = when {
                isMe || isSlotCaptain -> GoldAccent
                occupant != null -> Color.White.copy(alpha = 0.14f)
                else -> Color.White.copy(alpha = 0.06f)
              },
            ),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(
                    when {
                      occupant == null -> Color.White.copy(alpha = 0.15f)
                      occupant.online -> EmeraldLive
                      else -> MutedSlate
                    }
                  ),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (occupant != null) {
                    "${if (isSlotCaptain) "👑 " else ""}${if (isSlotVip) "💎 " else ""}${occupant.displayName}"
                  } else {
                    "Slot ${slot.toString().padStart(2, '0')} • Open"
                  },
                  color = if (isSlotCaptain) GoldAccent else if (occupant != null) Color.White else MutedSlate,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = if (occupant != null) FontWeight.Bold else FontWeight.Medium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = if (occupant != null) {
                    val badges = buildList {
                      if (isSlotCaptain) add("CAPTAIN")
                      if (isSlotVip) add("VIP")
                    }.joinToString(" • ")
                    val badgePrefix = if (badges.isNotEmpty()) "$badges • " else ""
                    "$badgePrefix${occupant.playerId} • ${charOpt?.name ?: occupant.characterId}"
                  } else {
                    "Available for player"
                  },
                  color = if (isMe || isSlotCaptain) GoldAccent else MutedSlate,
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    }
  }
}
