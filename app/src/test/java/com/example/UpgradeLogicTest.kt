package com.example

import com.example.multiplayer.AuthErrorMapper
import com.example.multiplayer.AuthValidation
import com.example.multiplayer.CctvPlaybackMath
import com.example.multiplayer.HousemateCatalog
import com.example.multiplayer.MultiplayerFirestoreRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class UpgradeLogicTest {

  @Test
  fun emailDisplayNameAndPasswordValidationEnforceStrictFormat() {
    assertNotNull(AuthValidation.validateEmail(""))
    assertNotNull(AuthValidation.validateEmail("not-an-email"))
    assertNull(AuthValidation.validateEmail("aria.player@example.com"))

    assertNotNull(AuthValidation.validateDisplayName("ab"))
    assertNotNull(AuthValidation.validateDisplayName("!!!"))
    assertNull(AuthValidation.validateDisplayName("aria_01"))
    assertNull(AuthValidation.validateDisplayName("Aria VIP"))

    assertNotNull(AuthValidation.validatePassword("short7"))
    assertNull(AuthValidation.validatePassword("Str0ngPass!"))
  }

  @Test
  fun clientNeverElevatesAdminRoleFromUsernameStringAlone() {
    assertFalse(AuthValidation.canClientElevateAdminFromUsername("BIGGBOSS"))
    assertFalse(AuthValidation.canClientElevateAdminFromUsername("biggboss"))
    assertFalse(AuthValidation.canClientElevateAdminFromUsername("admin"))
  }

  @Test
  fun authErrorMapperProducesClearMessagesForDuplicateWeakIncorrectAndNetworkErrors() {
    assertTrue(
      AuthErrorMapper.mapAuthCode("ERROR_EMAIL_ALREADY_IN_USE", null)
        .contains("already exists", ignoreCase = true)
    )
    assertTrue(
      AuthErrorMapper.mapAuthCode("ERROR_WEAK_PASSWORD", null)
        .contains("weak", ignoreCase = true)
    )
    assertTrue(
      AuthErrorMapper.mapAuthCode("ERROR_INVALID_EMAIL", null)
        .contains("valid email", ignoreCase = true)
    )
    assertTrue(
      AuthErrorMapper.mapAuthCode("ERROR_WRONG_PASSWORD", null)
        .contains("Incorrect email or password", ignoreCase = true)
    )
    assertTrue(
      AuthErrorMapper.mapAuthCode("ERROR_INVALID_CREDENTIAL", null)
        .contains("Incorrect email or password", ignoreCase = true)
    )
    assertTrue(
      AuthErrorMapper.mapAuthCode("ERROR_USER_NOT_FOUND", null, isPasswordReset = true)
        .contains("No account found", ignoreCase = true)
    )
    assertTrue(
      AuthErrorMapper.mapAuthCode("ERROR_OPERATION_NOT_ALLOWED", null)
        .contains("Firebase Console", ignoreCase = true)
    )
    assertTrue(
      AuthErrorMapper.mapAuthCode("ERROR_NETWORK_REQUEST_FAILED", null)
        .contains("Network error", ignoreCase = true)
    )
    assertTrue(
      AuthErrorMapper.mapException(UnknownHostException("offline"))
        .contains("Network error", ignoreCase = true)
    )
  }

  @Test
  fun housemateCatalogContains15DistinctMaleAndFemaleCharacters() {
    val all = HousemateCatalog.all
    assertEquals(15, all.size)
    assertEquals((1..15).toList(), all.map { it.number })
    assertTrue(all.any { it.gender == "Female" })
    assertTrue(all.any { it.gender == "Male" })
    assertTrue(all.all { it.styleSummary.isNotBlank() && it.spawnRoom.isNotBlank() })
  }

  @Test
  fun forbiddenCctvKeywordsAlwaysExcludePrivateRoomsAndSmokingLounge() {
    val keywords = MultiplayerFirestoreRepository.FORBIDDEN_CCTV_KEYWORDS
    assertTrue(keywords.containsAll(listOf("bathroom", "washroom", "changing", "private", "smoking")))
  }

  @Test
  fun cctvPlaybackMathHandlesSeekRewindSpeedsAndDurationFormatting() {
    assertEquals(0L, CctvPlaybackMath.clampSeekMs(-500L, 12_000L))
    assertEquals(12_000L, CctvPlaybackMath.clampSeekMs(20_000L, 12_000L))
    assertEquals(7_000L, CctvPlaybackMath.rewind5s(12_000L, 20_000L))
    assertEquals(0L, CctvPlaybackMath.rewind5s(3_000L, 20_000L))

    assertEquals(listOf(0.5f, 1.0f, 1.5f, 2.0f), CctvPlaybackMath.SUPPORTED_SPEEDS)
    assertEquals("00:00", CctvPlaybackMath.formatDurationMs(0L))
    assertEquals("01:05", CctvPlaybackMath.formatDurationMs(65_000L))
  }
}
