package com.example

import com.example.multiplayer.HouseTask
import com.example.multiplayer.Phase6Catalog
import com.example.multiplayer.Phase7Catalog
import com.example.multiplayer.Phase7Logic
import com.example.multiplayer.Phase8Logic
import com.example.multiplayer.RationItem
import com.example.multiplayer.RationStockStatus
import com.example.multiplayer.SeasonMember
import com.example.multiplayer.TaskCategory
import com.example.multiplayer.TaskDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase7And8LogicTest {

  @Test
  fun rationStockStatusCoversAvailableLowEmptyAndRestricted() {
    val base = RationItem(itemId = "rice", name = "Royal Rice", quantity = 20L, maxQuantity = 40L, minThreshold = 5L, permitted = true)
    assertEquals(RationStockStatus.AVAILABLE, Phase7Logic.stockStatus(base))
    assertEquals(RationStockStatus.LOW_STOCK, Phase7Logic.stockStatus(base.copy(quantity = 5L)))
    assertEquals(RationStockStatus.OUT_OF_STOCK, Phase7Logic.stockStatus(base.copy(quantity = 0L)))
    assertEquals(RationStockStatus.RESTRICTED, Phase7Logic.stockStatus(base.copy(permitted = false)))
  }

  @Test
  fun rationCollectionValidationChecksStockAmountPermissionAndCooldown() {
    val item = RationItem(itemId = "rice", name = "Royal Rice", quantity = 4L, maxQuantity = 40L, minThreshold = 5L, permitted = true)
    assertTrue(Phase7Logic.canCollectRation(item, 2L, 0L, 10_000L).isSuccess)
    assertTrue(Phase7Logic.canCollectRation(item, 6L, 0L, 10_000L).isFailure)
    assertTrue(Phase7Logic.canCollectRation(item, 5L, 0L, 10_000L).isFailure)
    assertTrue(Phase7Logic.canCollectRation(item.copy(permitted = false), 1L, 0L, 10_000L).isFailure)
    assertTrue(Phase7Logic.canCollectRation(item, 1L, 9_000L, 10_000L).isFailure)
  }

  @Test
  fun virtualRecipeIngredientCheckDeductionAndNutritionBounds() {
    val recipe = Phase7Catalog.findRecipe("golden_atrium_platter")
    val insufficientBag = mapOf("rice" to 1L, "vegetables" to 2L)
    val missing = Phase7Logic.missingIngredients(insufficientBag, recipe)
    assertEquals(1L, missing["rice"])
    assertEquals(1L, missing["spices"])
    assertFalse(Phase7Logic.hasIngredients(insufficientBag, recipe))

    val fullBag = mapOf("rice" to 3L, "vegetables" to 2L, "spices" to 1L)
    assertTrue(Phase7Logic.hasIngredients(fullBag, recipe))
    val afterDeduct = Phase7Logic.deductIngredients(fullBag, recipe)
    assertEquals(1L, afterDeduct["rice"])
    assertFalse(afterDeduct.containsKey("vegetables"))
    assertFalse(afterDeduct.containsKey("spices"))

    val (e1, h1) = Phase7Logic.applyMealNutrition(currentEnergy = 85L, currentHunger = 15L, energyRestore = 34L)
    assertEquals(100L, e1)
    assertEquals(0L, h1)
  }

  @Test
  fun taskDifficultyProgressSecretVisibilityAndGrantIds() {
    assertEquals(100L, Phase8Logic.calculateRewardPoints(100L, TaskDifficulty.EASY))
    assertEquals(150L, Phase8Logic.calculateRewardPoints(100L, TaskDifficulty.HARD))
    assertEquals(200L, Phase8Logic.calculateRewardPoints(100L, TaskDifficulty.EXTREME))

    val required = listOf("zone_bedroom", "zone_kitchen", "zone_garden", "zone_lounge")
    assertEquals(50L, Phase8Logic.computeProgressPercent(listOf("zone_bedroom", "zone_kitchen"), required))
    assertEquals(100L, Phase8Logic.computeProgressPercent(required, required))

    val secretTask = HouseTask(
      id = "t_secret",
      category = TaskCategory.SECRET.name,
      assignedUids = listOf("u_alice"),
      assignedTeamId = "team_red",
    )
    assertTrue(Phase8Logic.isTaskVisibleToPlayer(secretTask, "u_alice", null, isAdmin = false))
    assertTrue(Phase8Logic.isTaskVisibleToPlayer(secretTask, "u_bob", "team_red", isAdmin = false))
    assertFalse(Phase8Logic.isTaskVisibleToPlayer(secretTask, "u_bob", "team_blue", isAdmin = false))
    assertTrue(Phase8Logic.isTaskVisibleToPlayer(secretTask, "u_admin", null, isAdmin = true))

    assertEquals("TASK_t1_u1", Phase8Logic.grantIdForTask("t1", "u1"))
    assertEquals("COOK_c1_u1", Phase8Logic.grantIdForCooking("c1", "u1"))
  }

  @Test
  fun fairTaskAssignmentAvoidsImmediateRepeatAssignees() {
    val members = listOf(
      SeasonMember(userId = "u1", displayName = "Aarav", status = "IN_HOUSE"),
      SeasonMember(userId = "u2", displayName = "Diya", status = "IN_HOUSE"),
      SeasonMember(userId = "u3", displayName = "Kabir", status = "IN_HOUSE"),
    )
    val recent = listOf(
      HouseTask(id = "old1", activityType = "QUIZ", assignedUids = listOf("u1")),
      HouseTask(id = "old2", activityType = "MEMORY", assignedUids = listOf("u2")),
    )
    val (preset, assignees) = Phase8Logic.generateFairTaskAssignment(
      presets = Phase6Catalog.taskPresets,
      recentTasks = recent,
      eligibleMembers = members,
      desiredCategory = TaskCategory.INDIVIDUAL,
      randomIndex = { 0 },
    )
    assertTrue(preset.activityType.name !in setOf("QUIZ", "MEMORY"))
    assertEquals(listOf("u3"), assignees)
  }
}
