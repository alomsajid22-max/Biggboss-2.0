package com.example

import com.example.multiplayer.Phase6Catalog
import com.example.multiplayer.Phase6Logic
import com.example.multiplayer.TaskActivityType
import com.example.multiplayer.TaskSubmission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase6LogicTest {

  @Test
  fun countdownFormattingAndBounds() {
    assertEquals("00:00", Phase6Logic.formatCountdown(-5L))
    assertEquals("00:00", Phase6Logic.formatCountdown(0L))
    assertEquals("01:05", Phase6Logic.formatCountdown(65L))
    assertEquals("03:00", Phase6Logic.formatCountdown(180L))

    assertEquals(0L, Phase6Logic.remainingSeconds(0L, 10_000L))
    assertEquals(0L, Phase6Logic.remainingSeconds(5_000L, 10_000L))
    assertEquals(30L, Phase6Logic.remainingSeconds(40_000L, 10_000L))
  }

  @Test
  fun answerMatchingIgnoresCaseAndWhitespace() {
    assertTrue(Phase6Logic.isAnswerMatch("  tree-kitch-gard-pool ", "TREE-KITCH-GARD-POOL"))
    assertTrue(Phase6Logic.isAnswerMatch("anything", ""))
    assertTrue(Phase6Logic.isAnswerMatch("anything", null))
    assertFalse(Phase6Logic.isAnswerMatch("10", "13"))
  }

  @Test
  fun pickAutoWinnerSelectsHighestScoreThenFastestElapsed() {
    val wrong = TaskSubmission(
      id = "u1",
      userId = "u1",
      displayName = "WrongPlayer",
      answerText = "10",
      elapsedSeconds = 5L,
      verificationStatus = "SUBMITTED",
      awardedScore = 0L,
    )
    val slowerCorrect = TaskSubmission(
      id = "u2",
      userId = "u2",
      displayName = "SlowWinner",
      answerText = "13",
      elapsedSeconds = 42L,
      verificationStatus = "SUBMITTED",
      awardedScore = 0L,
    )
    val fasterCorrect = TaskSubmission(
      id = "u3",
      userId = "u3",
      displayName = "FastWinner",
      answerText = "13",
      elapsedSeconds = 11L,
      verificationStatus = "SUBMITTED",
      awardedScore = 0L,
    )
    val winner = Phase6Logic.pickAutoWinner(listOf(wrong, slowerCorrect, fasterCorrect), "13")
    assertNotNull(winner)
    assertEquals("u3", winner?.userId)

    val noWinner = Phase6Logic.pickAutoWinner(listOf(wrong), "13")
    assertNull(noWinner)
  }

  @Test
  fun catalogCoversAllNineActivitiesFiveRulesAndEightDailyScheduleSlots() {
    assertEquals(9, TaskActivityType.entries.size)
    TaskActivityType.entries.forEach { act ->
      val preset = Phase6Catalog.presetFor(act)
      assertEquals(act, preset.activityType)
      assertTrue(preset.title.isNotBlank())
      assertTrue(preset.expectedAnswer.isNotBlank())
    }
    assertEquals(5, Phase6Catalog.defaultHouseRules.size)
    assertEquals(8, Phase6Catalog.defaultDailySchedule.size)
    val keys = Phase6Catalog.defaultDailySchedule.map { it.activityKey }
    assertTrue(keys.containsAll(listOf("MORNING_WAKEUP", "MORNING_GATHERING", "BREAKFAST", "DAILY_TASK", "LUNCH", "EVENING_GATHERING", "DINNER", "NIGHT_SCHEDULE")))
  }
}
