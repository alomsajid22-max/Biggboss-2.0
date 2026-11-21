package com.example.multiplayer

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class RationStockStatus(val label: String) {
  AVAILABLE("Available"),
  LOW_STOCK("Low Stock"),
  OUT_OF_STOCK("Out of Stock"),
  RESTRICTED("Restricted"),
}

@Keep
@IgnoreExtraProperties
data class RationItem(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val itemId: String = "",
  val name: String = "",
  val category: String = "Staple",
  val unit: String = "units",
  val quantity: Long = 0L,
  val maxQuantity: Long = 40L,
  val minThreshold: Long = 5L,
  val permitted: Boolean = true,
  val energyValue: Long = 15L,
  val lastUpdatedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class KitchenPlayerState(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val userId: String = "",
  val displayName: String = "Housemate",
  val energy: Long = 75L,
  val hunger: Long = 30L,
  val bagItems: Map<String, Long> = emptyMap(),
  val activeCookingId: String? = null,
  val lastCollectedAtMillis: Long = 0L,
  val lastCookedAtMillis: Long = 0L,
  val lastEatAtMillis: Long = 0L,
  val mealsCookedCount: Long = 0L,
  val mealsEatenCount: Long = 0L,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CookingSession(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val stationId: String = "kitchen_stove_1",
  val recipeId: String = "",
  val recipeName: String = "",
  val chefUid: String = "",
  val chefDisplayName: String = "",
  val status: String = "COOKING", // COOKING | READY | CLAIMED | CANCELLED
  val ingredientsUsed: Map<String, Long> = emptyMap(),
  val startedAtMillis: Long = 0L,
  val readyAtMillis: Long = 0L,
  val energyRestore: Long = 25L,
  val servingsTotal: Long = 2L,
  val servingsRemaining: Long = 2L,
  val claimedByUids: List<String> = emptyList(),
  val updatedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

/**
 * Strictly fictional in-game virtual recipe (no real-world recipes or cooking instructions).
 */
data class VirtualRecipe(
  val recipeId: String,
  val name: String,
  val tagline: String,
  val stationType: String,
  val cookDurationSeconds: Long,
  val cooldownSeconds: Long,
  val energyRestore: Long,
  val servings: Long,
  val ingredients: Map<String, Long>,
  val mealColorHex: String,
)

object Phase7Catalog {
  const val COLLECT_COOLDOWN_SEC = 4L
  const val COOK_COOLDOWN_SEC = 10L
  const val EAT_COOLDOWN_SEC = 6L

  val defaultRations: List<RationItem> = listOf(
    RationItem(itemId = "rice", name = "Royal Rice", category = "Grains", unit = "scoops", quantity = 24L, maxQuantity = 40L, minThreshold = 6L, permitted = true, energyValue = 16L),
    RationItem(itemId = "bread", name = "Atrium Bread", category = "Bakery", unit = "loaves", quantity = 18L, maxQuantity = 30L, minThreshold = 4L, permitted = true, energyValue = 14L),
    RationItem(itemId = "vegetables", name = "Garden Veggies", category = "Produce", unit = "crates", quantity = 22L, maxQuantity = 40L, minThreshold = 6L, permitted = true, energyValue = 15L),
    RationItem(itemId = "fruits", name = "Orchard Fruits", category = "Produce", unit = "baskets", quantity = 20L, maxQuantity = 36L, minThreshold = 5L, permitted = true, energyValue = 14L),
    RationItem(itemId = "milk", name = "Chilled Milk", category = "Dairy", unit = "cartons", quantity = 16L, maxQuantity = 30L, minThreshold = 4L, permitted = true, energyValue = 12L),
    RationItem(itemId = "eggs", name = "Farm Eggs", category = "Protein", unit = "trays", quantity = 18L, maxQuantity = 32L, minThreshold = 5L, permitted = true, energyValue = 16L),
    RationItem(itemId = "water", name = "Pure Mineral Water", category = "Hydration", unit = "bottles", quantity = 30L, maxQuantity = 50L, minThreshold = 8L, permitted = true, energyValue = 10L),
    RationItem(itemId = "spices", name = "House Spice Mix", category = "Pantry", unit = "jars", quantity = 14L, maxQuantity = 24L, minThreshold = 3L, permitted = true, energyValue = 10L),
    RationItem(itemId = "lentils", name = "Golden Lentils", category = "Protein", unit = "packs", quantity = 18L, maxQuantity = 32L, minThreshold = 5L, permitted = true, energyValue = 18L),
    RationItem(itemId = "butter", name = "Cream Butter", category = "Dairy", unit = "blocks", quantity = 12L, maxQuantity = 24L, minThreshold = 3L, permitted = true, energyValue = 15L),
  )

  val virtualRecipes: List<VirtualRecipe> = listOf(
    VirtualRecipe(
      recipeId = "golden_atrium_platter",
      name = "Golden Atrium Platter",
      tagline = "Signature house feast restoring high stamina.",
      stationType = "Main Range",
      cookDurationSeconds = 8L,
      cooldownSeconds = COOK_COOLDOWN_SEC,
      energyRestore = 34L,
      servings = 3L,
      ingredients = mapOf("rice" to 2L, "vegetables" to 2L, "spices" to 1L),
      mealColorHex = "#EAB954",
    ),
    VirtualRecipe(
      recipeId = "royal_sunrise_breakfast",
      name = "Royal Sunrise Breakfast",
      tagline = "Quick morning booster platter for 2 housemates.",
      stationType = "Prep Counter",
      cookDurationSeconds = 6L,
      cooldownSeconds = COOK_COOLDOWN_SEC,
      energyRestore = 28L,
      servings = 2L,
      ingredients = mapOf("bread" to 2L, "eggs" to 2L, "butter" to 1L),
      mealColorHex = "#F59E0B",
    ),
    VirtualRecipe(
      recipeId = "glass_house_power_bowl",
      name = "Bigg Boss Power Bowl",
      tagline = "Task endurance bowl crafted for heavy obstacle runs.",
      stationType = "Main Range",
      cookDurationSeconds = 9L,
      cooldownSeconds = COOK_COOLDOWN_SEC,
      energyRestore = 38L,
      servings = 3L,
      ingredients = mapOf("lentils" to 2L, "rice" to 2L, "water" to 1L),
      mealColorHex = "#34D399",
    ),
    VirtualRecipe(
      recipeId = "hydration_berry_refresher",
      name = "Hydration Berry Refresher",
      tagline = "Instant cooling drink crafted at the sink & chiller.",
      stationType = "Hydration Station",
      cookDurationSeconds = 5L,
      cooldownSeconds = COOK_COOLDOWN_SEC,
      energyRestore = 24L,
      servings = 2L,
      ingredients = mapOf("fruits" to 2L, "water" to 2L, "milk" to 1L),
      mealColorHex = "#38BDF8",
    ),
    VirtualRecipe(
      recipeId = "midnight_spice_broth",
      name = "Midnight Spice Broth",
      tagline = "Warm late-night elixir shared after nominations.",
      stationType = "Main Range",
      cookDurationSeconds = 7L,
      cooldownSeconds = COOK_COOLDOWN_SEC,
      energyRestore = 30L,
      servings = 3L,
      ingredients = mapOf("vegetables" to 2L, "lentils" to 1L, "spices" to 1L, "water" to 1L),
      mealColorHex = "#C084FC",
    ),
  )

  fun findRecipe(recipeId: String): VirtualRecipe =
    virtualRecipes.firstOrNull { it.recipeId == recipeId } ?: virtualRecipes.first()
}

object Phase7Logic {
  fun stockStatus(item: RationItem): RationStockStatus = when {
    !item.permitted -> RationStockStatus.RESTRICTED
    item.quantity <= 0L -> RationStockStatus.OUT_OF_STOCK
    item.quantity <= item.minThreshold -> RationStockStatus.LOW_STOCK
    else -> RationStockStatus.AVAILABLE
  }

  fun remainingCooldownSeconds(lastActionMillis: Long, cooldownSeconds: Long, nowMillis: Long = System.currentTimeMillis()): Long {
    if (lastActionMillis <= 0L || cooldownSeconds <= 0L) return 0L
    val readyAt = lastActionMillis + cooldownSeconds * 1000L
    return if (readyAt <= nowMillis) 0L else ((readyAt - nowMillis + 999L) / 1000L)
  }

  fun canCollectRation(
    item: RationItem,
    amount: Long,
    lastCollectedAtMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
  ): Result<Unit> {
    if (!item.permitted) return Result.failure(IllegalStateException("${item.name} is restricted by Admin."))
    if (amount !in 1L..5L) return Result.failure(IllegalArgumentException("Select 1 to 5 ${item.unit}."))
    if (item.quantity < amount) return Result.failure(IllegalStateException("Insufficient stock for ${item.name}."))
    val cd = remainingCooldownSeconds(lastCollectedAtMillis, Phase7Catalog.COLLECT_COOLDOWN_SEC, nowMillis)
    if (cd > 0L) return Result.failure(IllegalStateException("Ration cooldown: wait ${cd}s."))
    return Result.success(Unit)
  }

  fun missingIngredients(bag: Map<String, Long>, recipe: VirtualRecipe): Map<String, Long> {
    val missing = linkedMapOf<String, Long>()
    for ((itemId, needed) in recipe.ingredients) {
      val have = bag[itemId] ?: 0L
      if (have < needed) missing[itemId] = needed - have
    }
    return missing
  }

  fun hasIngredients(bag: Map<String, Long>, recipe: VirtualRecipe): Boolean =
    missingIngredients(bag, recipe).isEmpty()

  fun deductIngredients(bag: Map<String, Long>, recipe: VirtualRecipe): Map<String, Long> {
    val next = bag.toMutableMap()
    for ((itemId, needed) in recipe.ingredients) {
      val left = (next[itemId] ?: 0L) - needed
      if (left > 0L) next[itemId] = left else next.remove(itemId)
    }
    return next
  }

  fun applyMealNutrition(currentEnergy: Long, currentHunger: Long, energyRestore: Long): Pair<Long, Long> {
    val newEnergy = (currentEnergy + energyRestore.coerceAtLeast(1L)).coerceIn(0L, 100L)
    val hungerReduction = (energyRestore * 9L / 10L).coerceAtLeast(10L)
    val newHunger = (currentHunger - hungerReduction).coerceIn(0L, 100L)
    return newEnergy to newHunger
  }
}

// =====================================================================
// PHASE 8: ADVANCED TASK MANAGEMENT, PROGRESS, LEADERBOARD & REWARDS
// =====================================================================

enum class TaskCategory(val label: String, val defaultAssignmentMode: String) {
  INDIVIDUAL("Individual", "INDIVIDUAL"),
  GROUP("Group", "GROUP"),
  TEAM("Team", "TEAM"),
  DAILY("Daily", "HOUSE"),
  WEEKLY("Weekly", "HOUSE"),
  HOUSE("House", "HOUSE"),
  CAPTAIN("Captain", "INDIVIDUAL"),
  SECRET("Secret", "INDIVIDUAL");

  companion object {
    fun fromRaw(raw: String?): TaskCategory =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DAILY
  }
}

enum class TaskDifficulty(val label: String, val scoreMultiplier: Double, val colorHex: String) {
  EASY("Easy", 1.0, "#34D399"),
  MEDIUM("Medium", 1.25, "#38BDF8"),
  HARD("Hard", 1.5, "#F59E0B"),
  EXTREME("Extreme", 2.0, "#F87171");

  companion object {
    fun fromRaw(raw: String?): TaskDifficulty =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MEDIUM
  }
}

enum class TaskLifecycleStatus(val label: String) {
  AVAILABLE("Available"),
  ASSIGNED("Assigned"),
  IN_PROGRESS("In Progress"),
  ACTIVE("Active"),
  PAUSED("Paused"),
  COMPLETED("Completed"),
  FAILED("Failed"),
  EXPIRED("Expired"),
  CANCELLED("Cancelled"),
  DRAFT("Draft");

  fun isPlayable(): Boolean =
    this == AVAILABLE || this == ASSIGNED || this == IN_PROGRESS || this == ACTIVE

  companion object {
    fun fromRaw(raw: String?): TaskLifecycleStatus =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DRAFT
  }
}

@Keep
@IgnoreExtraProperties
data class TaskPlayerProgress(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val taskId: String = "",
  val userId: String = "",
  val displayName: String = "",
  val completedSteps: List<String> = emptyList(),
  val percent: Long = 0L,
  val status: String = "IN_PROGRESS", // ASSIGNED | IN_PROGRESS | COMPLETED | FAILED | EXPIRED
  val elapsedSeconds: Long = 0L,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PlayerLeaderboardEntry(
  @DocumentId val id: String = "",
  val seasonId: String = "season_01",
  val userId: String = "",
  val playerId: String = "",
  val displayName: String = "",
  val characterId: String = "Player 01",
  val teamId: String? = null,
  val points: Long = 0L,
  val housePointsContribution: Long = 0L,
  val tasksCompleted: Long = 0L,
  val mealsCooked: Long = 0L,
  val achievements: List<String> = emptyList(),
  val lastGrantId: String = "init",
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

object Phase8Catalog {
  val achievementTitles: Map<String, String> = mapOf(
    "FIRST_CHEF" to "Kitchen Apprentice",
    "MASTER_CHEF" to "Gourmet Chef",
    "RATION_KEEPER" to "Pantry Steward",
    "TASK_FINISHER" to "Task Champion",
    "SPEED_RUNNER" to "Atrium Sprinter",
    "CAPTAIN_CROWN" to "House Captain",
    "TEAM_MVP" to "Squad Strategist",
  )

  fun achievementTitle(code: String): String = achievementTitles[code] ?: code
}

object Phase8Logic {
  fun computeProgressPercent(completedSteps: Collection<String>, requiredSteps: List<String>): Long {
    if (requiredSteps.isEmpty()) return if (completedSteps.isNotEmpty()) 100L else 0L
    val done = requiredSteps.count { it in completedSteps }
    return ((done * 100L) / requiredSteps.size.toLong()).coerceIn(0L, 100L)
  }

  fun calculateRewardPoints(basePoints: Long, difficulty: TaskDifficulty): Long {
    val safe = basePoints.coerceIn(1L, 1000L)
    return kotlin.math.round(safe * difficulty.scoreMultiplier).toLong().coerceIn(1L, 1000L)
  }

  fun grantIdForTask(taskId: String, uid: String): String = "TASK_${taskId}_$uid"

  fun grantIdForCooking(sessionId: String, uid: String): String = "COOK_${sessionId}_$uid"

  fun isTaskVisibleToPlayer(
    task: HouseTask,
    currentUid: String,
    myTeamId: String?,
    isAdmin: Boolean,
  ): Boolean {
    if (isAdmin) return true
    if (task.category == TaskCategory.SECRET.name) {
      return task.assignedUids.contains(currentUid) ||
        (!task.assignedTeamId.isNullOrBlank() && task.assignedTeamId == myTeamId)
    }
    return true
  }

  fun isPlayerEligibleForTask(
    task: HouseTask,
    currentUid: String,
    myTeamId: String?,
  ): Boolean {
    if (!TaskLifecycleStatus.fromRaw(task.status).isPlayable()) return false
    if (task.assignedUids.isNotEmpty() && !task.assignedUids.contains(currentUid)) return false
    if (!task.assignedTeamId.isNullOrBlank() && task.assignedTeamId != myTeamId) return false
    return true
  }

  /**
   * Picks a random task preset + fair target assignees while avoiding immediate repeat assignments.
   */
  fun generateFairTaskAssignment(
    presets: List<TaskPresetTemplate>,
    recentTasks: List<HouseTask>,
    eligibleMembers: List<SeasonMember>,
    desiredCategory: TaskCategory,
    groupSize: Int = 3,
    randomIndex: (Int) -> Int = { bound -> if (bound <= 1) 0 else kotlin.random.Random.nextInt(bound) },
  ): Pair<TaskPresetTemplate, List<String>> {
    val recentTypes = recentTasks.take(3).map { it.activityType }.toSet()
    val candidatePresets = presets.filter { it.activityType.name !in recentTypes }.ifEmpty { presets }
    val pickedPreset = candidatePresets[randomIndex(candidatePresets.size)]

    val recentAssigneeCounts = mutableMapOf<String, Int>()
    recentTasks.take(5).forEach { t ->
      t.assignedUids.forEach { uid ->
        recentAssigneeCounts[uid] = (recentAssigneeCounts[uid] ?: 0) + 1
      }
    }

    val sortedMembers = eligibleMembers
      .filter { it.status == "IN_HOUSE" || it.status == "LOBBY" }
      .sortedBy { recentAssigneeCounts[it.userId] ?: 0 }

    val assignees = when (desiredCategory) {
      TaskCategory.INDIVIDUAL, TaskCategory.SECRET, TaskCategory.CAPTAIN ->
        sortedMembers.take(1).map { it.userId }
      TaskCategory.GROUP ->
        sortedMembers.take(groupSize.coerceIn(2, 6)).map { it.userId }
      TaskCategory.TEAM, TaskCategory.DAILY, TaskCategory.WEEKLY, TaskCategory.HOUSE ->
        emptyList()
    }
    return pickedPreset to assignees
  }
}
