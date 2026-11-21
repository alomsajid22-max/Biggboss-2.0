package com.example.multiplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

enum class AuthEntryStage {
  CHOOSER,
  PLAYER_AUTH,
  ADMIN_KEYWORD,
  ADMIN_AUTH,
}

private val EntryGold = Color(0xFFEAB954)
private val EntryCyan = Color(0xFF22D3EE)
private val EntryElevated = Color(0xFF1A233B)
private val EntryMuted = Color(0xFF94A3B8)
private val EntryDanger = Color(0xFFF87171)

@Composable
fun AuthEntryChooserPanel(
  onSelectPlayerLogin: () -> Unit,
  onSelectAdminLogin: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Box(
      modifier = Modifier
        .size(64.dp)
        .clip(CircleShape)
        .background(EntryGold.copy(alpha = 0.14f))
        .border(1.5.dp, EntryGold.copy(alpha = 0.65f), CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.Home,
        contentDescription = stringResource(R.string.auth_hero_title),
        tint = EntryGold,
        modifier = Modifier.size(32.dp),
      )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = stringResource(R.string.auth_brand_eyebrow),
        style = MaterialTheme.typography.labelSmall,
        color = EntryGold,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = stringResource(R.string.auth_hero_title),
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.5.sp,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Choose how you would like to enter BIGG BOSS – THE HOUSE",
        style = MaterialTheme.typography.bodySmall,
        color = EntryMuted,
        textAlign = TextAlign.Center,
      )
    }

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = EntryElevated,
      border = BorderStroke(1.dp, EntryGold.copy(alpha = 0.25f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Button(
          onClick = onSelectPlayerLogin,
          colors = ButtonDefaults.buttonColors(
            containerColor = EntryGold,
            contentColor = Color(0xFF10141F),
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("entry_player_login_button"),
        ) {
          Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Text("PLAYER LOGIN", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 1.sp)
        }

        OutlinedButton(
          onClick = onSelectAdminLogin,
          border = BorderStroke(1.5.dp, EntryCyan.copy(alpha = 0.75f)),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("entry_admin_login_button"),
        ) {
          Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = EntryCyan, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "ADMIN LOGIN",
            color = EntryCyan,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = 1.sp,
          )
        }
      }
    }

    Text(
      text = "Contestants sign in via Player Login. Admin access is verified locally inside the app.",
      color = EntryMuted.copy(alpha = 0.85f),
      style = MaterialTheme.typography.labelSmall,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
fun AdminKeywordEntrySection(
  fieldColors: TextFieldColors,
  onKeywordAccepted: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var keywordInput by remember { mutableStateOf("") }
  var keywordError by remember { mutableStateOf<String?>(null) }

  val verifyKeyword = {
    if (AdminAuthValidation.isValidAccessKeyword(keywordInput)) {
      keywordError = null
      onKeywordAccepted()
    } else {
      keywordError = "Invalid Admin Access Keyword"
    }
  }

  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      TextButton(
        onClick = onBack,
        modifier = Modifier.heightIn(min = 48.dp).testTag("admin_keyword_back_button"),
      ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = EntryGold)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Back", color = EntryGold, fontWeight = FontWeight.Bold)
      }
      Surface(shape = RoundedCornerShape(999.dp), color = EntryCyan.copy(alpha = 0.14f)) {
        Text(
          text = "ADMIN ENTRY GATE",
          color = EntryCyan,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.ExtraBold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Rounded.Key, contentDescription = null, tint = EntryCyan, modifier = Modifier.size(26.dp))
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "Admin Entry Verification",
          color = Color.White,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Keyword only unlocks the Admin sign-in screen. Admin username & password are still required.",
          color = EntryMuted,
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }

    AnimatedVisibility(visible = !keywordError.isNullOrBlank(), enter = fadeIn(), exit = fadeOut()) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = EntryDanger.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, EntryDanger.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().testTag("admin_keyword_error"),
      ) {
        Text(
          text = keywordError.orEmpty(),
          color = Color(0xFFFECACA),
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(10.dp),
        )
      }
    }

    OutlinedTextField(
      value = keywordInput,
      onValueChange = {
        if (it.length <= 64) {
          keywordInput = it
          if (keywordError != null) keywordError = null
        }
      },
      singleLine = true,
      label = { Text("Enter Admin Access Keyword") },
      placeholder = { Text("Enter access keyword") },
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
      ),
      keyboardActions = KeyboardActions(onDone = { verifyKeyword() }),
      colors = fieldColors,
      modifier = Modifier.fillMaxWidth().testTag("admin_keyword_input"),
    )

    Button(
      onClick = verifyKeyword,
      colors = ButtonDefaults.buttonColors(containerColor = EntryCyan, contentColor = Color(0xFF061622)),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("admin_keyword_submit_button"),
    ) {
      Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Continue to Admin Authentication", fontWeight = FontWeight.ExtraBold)
    }
  }
}

@Composable
fun AdminAuthLoginSection(
  isLoading: Boolean,
  errorMsg: String?,
  lockoutStatus: LocalAdminAuthManager.LockoutStatus = LocalAdminAuthManager.LockoutStatus(),
  fieldColors: TextFieldColors,
  onSubmitAdminLogin: (username: String, password: String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }
  val isLocked = lockoutStatus.isLocked

  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      TextButton(
        onClick = onBack,
        enabled = !isLoading,
        modifier = Modifier.heightIn(min = 48.dp).testTag("admin_auth_back_button"),
      ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = EntryGold)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Back", color = EntryGold, fontWeight = FontWeight.Bold)
      }
      Surface(shape = RoundedCornerShape(999.dp), color = EntryCyan.copy(alpha = 0.16f)) {
        Text(
          text = "LOCAL APK ADMIN AUTH",
          color = EntryCyan,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.ExtraBold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Rounded.Lock, contentDescription = null, tint = EntryCyan, modifier = Modifier.size(24.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(
          text = "Admin Control Room Authentication",
          color = Color.White,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Verified locally inside the Android APK (no Cloud Functions or external server).",
          color = EntryMuted,
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }

    AnimatedVisibility(visible = isLocked, enter = fadeIn(), exit = fadeOut()) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = EntryDanger.copy(alpha = 0.2f),
        border = BorderStroke(1.5.dp, EntryDanger),
        modifier = Modifier.fillMaxWidth().testTag("admin_lockout_banner"),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = LocalAdminAuthManager.LOCKOUT_HEADLINE,
            color = Color(0xFFFECACA),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
          )
          Text(
            text = "Remaining time: ${LocalAdminAuthManager.formatRemainingTime(lockoutStatus.remainingMillis)}",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("admin_lockout_remaining"),
          )
          if (lockoutStatus.clockTamperingDetected) {
            Text(
              text = "Device clock change detected — monotonic 48-hour lockout timer enforced.",
              color = EntryGold,
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
      }
    }

    AnimatedVisibility(visible = !isLocked && !errorMsg.isNullOrBlank(), enter = fadeIn(), exit = fadeOut()) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = EntryDanger.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, EntryDanger.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().testTag("admin_login_error"),
      ) {
        Text(
          text = errorMsg.orEmpty(),
          color = Color(0xFFFECACA),
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(10.dp),
        )
      }
    }

    if (!isLocked && lockoutStatus.failedAttempts > 0) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = EntryGold.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, EntryGold.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().testTag("admin_attempt_warning"),
      ) {
        Text(
          text = "Failed attempts: ${lockoutStatus.failedAttempts}/2 • ${lockoutStatus.remainingAttemptsBeforeLock} attempt(s) remaining before 48-hour lockout.",
          color = EntryGold,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
      }
    }

    OutlinedTextField(
      value = username,
      onValueChange = { if (it.length <= 40) username = it },
      singleLine = true,
      enabled = !isLoading && !isLocked,
      label = { Text("Admin Username") },
      placeholder = { Text("Enter Admin username") },
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
      colors = fieldColors,
      modifier = Modifier.fillMaxWidth().testTag("admin_username_input"),
    )

    OutlinedTextField(
      value = password,
      onValueChange = { if (it.length <= 128) password = it },
      singleLine = true,
      enabled = !isLoading && !isLocked,
      label = { Text("Admin Password") },
      visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
      ),
      keyboardActions = KeyboardActions(
        onDone = {
          if (!isLoading && !isLocked && username.trim().isNotEmpty() && password.isNotEmpty()) {
            onSubmitAdminLogin(username, password)
          }
        },
      ),
      trailingIcon = {
        IconButton(
          onClick = { showPassword = !showPassword },
          enabled = !isLocked,
          modifier = Modifier.size(48.dp),
        ) {
          Icon(
            imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            contentDescription = if (showPassword) "Hide password" else "Show password",
            tint = EntryMuted,
          )
        }
      },
      colors = fieldColors,
      modifier = Modifier.fillMaxWidth().testTag("admin_password_input"),
    )

    Button(
      onClick = { onSubmitAdminLogin(username, password) },
      enabled = !isLoading && !isLocked && username.trim().isNotEmpty() && password.isNotEmpty(),
      colors = ButtonDefaults.buttonColors(containerColor = EntryCyan, contentColor = Color(0xFF061622)),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("admin_login_submit_button"),
    ) {
      when {
        isLocked -> {
          Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Admin Login Locked (48h)", fontWeight = FontWeight.ExtraBold)
        }
        isLoading -> {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF061622), strokeWidth = 2.5.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Verifying Locally...", fontWeight = FontWeight.Bold)
        }
        else -> {
          Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Sign In to Control Room", fontWeight = FontWeight.ExtraBold)
        }
      }
    }

    Surface(
      shape = RoundedCornerShape(10.dp),
      color = EntryElevated,
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
      modifier = Modifier.fillMaxWidth().testTag("admin_local_security_notice"),
    ) {
      Text(
        text = "Local-Only Notice: Credentials & lockout state verify offline inside the APK. This protects normal gameplay offline, but is not strong protection against someone modifying or reverse-engineering the APK.",
        color = EntryMuted,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(10.dp),
      )
    }
  }
}
