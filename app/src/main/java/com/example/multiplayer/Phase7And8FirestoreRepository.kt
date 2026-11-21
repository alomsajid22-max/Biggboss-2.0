package com.example.multiplayer

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

private const val P7_REPO_TAG = "Phase7And8Repo"

class Phase7And8FirestoreRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  private fun seasonDoc(seasonId: String) = db.collection("seasons").document(seasonId)

  private fun requireUserId(): String =
    auth.currentUser?.uid ?: throw IllegalStateException("Authentication required.")

  fun observeRations(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
  ): Flow<List<RationItem>> = callbackFlow {
    val reg = seasonDoc(seasonId).collection("kitchenRations")
      .whereEqualTo("seasonId", seasonId)
      .addSnapshotListener { snap, err ->
        if (err != null) {
          Log.w(P7_REPO_TAG, "Error observing kitchenRations: ${err.message}")
          return@addSnapshotListener
        }
        val list = snap?.documents
          ?.mapNotNull { it.toObject(RationItem::class.java) }
          ?.sortedBy { it.name }
          .orEmpty()
        trySend(list)
      }
    awaitClose { reg.remove() }
  }

  fun observeKitchenProfile(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    userId: String,
  ): Flow<KitchenPlayerState?> = callbackFlow {
    val reg = seasonDoc(seasonId).collection("kitchenProfiles").document(userId)
      .addSnapshotListener { snap, err ->
        if (err != null) {
          Log.w(P7_REPO_TAG, "Error observing kitchenProfile: ${err.message}")
          return@addSnapshotListener
        }
        trySend(snap?.toObject(KitchenPlayerState::class.java))
      }
    awaitClose { reg.remove() }
  }

  fun observeCookingSessions(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
  ): Flow<List<CookingSession>> = callbackFlow {
    val reg = seasonDoc(seasonId).collection("kitchenCooking")
      .whereEqualTo("seasonId", seasonId)
      .addSnapshotListener { snap, err ->
        if (err != null) {
          Log.w(P7_REPO_TAG, "Error observing kitchenCooking: ${err.message}")
          return@addSnapshotListener
        }
        val list = snap?.documents
          ?.mapNotNull { it.toObject(CookingSession::class.java) }
          ?.sortedByDescending { it.startedAtMillis }
          .orEmpty()
        trySend(list)
      }
    awaitClose { reg.remove() }
  }

  fun observeTaskProgressList(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    taskId: String,
  ): Flow<List<TaskPlayerProgress>> = callbackFlow {
    val reg = seasonDoc(seasonId).collection("tasks").document(taskId)
      .collection("progress")
      .whereEqualTo("seasonId", seasonId)
      .whereEqualTo("taskId", taskId)
      .addSnapshotListener { snap, err ->
        if (err != null) {
          Log.w(P7_REPO_TAG, "Error observing task progress ($taskId): ${err.message}")
          trySend(emptyList())
          return@addSnapshotListener
        }
        val list = snap?.documents
          ?.mapNotNull { it.toObject(TaskPlayerProgress::class.java) }
          ?.sortedByDescending { it.percent }
          .orEmpty()
        trySend(list)
      }
    awaitClose { reg.remove() }
  }

  fun observeLeaderboard(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
  ): Flow<List<PlayerLeaderboardEntry>> = callbackFlow {
    val reg = seasonDoc(seasonId).collection("leaderboard")
      .whereEqualTo("seasonId", seasonId)
      .addSnapshotListener { snap, err ->
        if (err != null) {
          Log.w(P7_REPO_TAG, "Error observing leaderboard: ${err.message}")
          return@addSnapshotListener
        }
        val list = snap?.documents
          ?.mapNotNull { it.toObject(PlayerLeaderboardEntry::class.java) }
          ?.sortedWith(compareByDescending<PlayerLeaderboardEntry> { it.points }.thenByDescending { it.tasksCompleted })
          .orEmpty()
        trySend(list)
      }
    awaitClose { reg.remove() }
  }

  suspend fun ensureDefaultRationsExist(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
  ) {
    try {
      val uid = auth.currentUser?.uid ?: return
      val col = seasonDoc(seasonId).collection("kitchenRations")
      val existing = col.whereEqualTo("seasonId", seasonId).get().await()
      val existingIds = existing.documents.map { it.id }.toSet()
      val missing = Phase7Catalog.defaultRations.filter { it.itemId !in existingIds }
      if (missing.isEmpty()) return

      val batch = db.batch()
      missing.forEach { item ->
        batch.set(
          col.document(item.itemId),
          mapOf(
            "seasonId" to seasonId,
            "itemId" to item.itemId,
            "name" to item.name,
            "category" to item.category,
            "unit" to item.unit,
            "quantity" to item.quantity,
            "maxQuantity" to item.maxQuantity,
            "minThreshold" to item.minThreshold,
            "permitted" to item.permitted,
            "energyValue" to item.energyValue,
            "lastUpdatedBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(P7_REPO_TAG, "Default ration initialization deferred: ${e.message}")
    }
  }

  suspend fun ensureKitchenProfileInitialized(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    account: UserAccount,
  ) {
    try {
      val uid = requireUserId()
      val ref = seasonDoc(seasonId).collection("kitchenProfiles").document(uid)
      val snap = ref.get().await()
      if (!snap.exists()) {
        ref.set(
          mapOf(
            "seasonId" to seasonId,
            "userId" to uid,
            "displayName" to account.displayName.take(24).ifBlank { "Housemate" },
            "energy" to 75L,
            "hunger" to 30L,
            "bagItems" to emptyMap<String, Long>(),
            "activeCookingId" to null,
            "lastCollectedAtMillis" to 0L,
            "lastCookedAtMillis" to 0L,
            "lastEatAtMillis" to 0L,
            "mealsCookedCount" to 0L,
            "mealsEatenCount" to 0L,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(P7_REPO_TAG, "Kitchen profile init deferred: ${e.message}")
    }
  }

  suspend fun ensureLeaderboardEntryInitialized(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    account: UserAccount,
    teamId: String?,
  ) {
    try {
      val uid = requireUserId()
      val ref = seasonDoc(seasonId).collection("leaderboard").document(uid)
      val snap = ref.get().await()
      if (!snap.exists()) {
        ref.set(
          mapOf(
            "seasonId" to seasonId,
            "userId" to uid,
            "playerId" to account.playerId.take(20),
            "displayName" to account.displayName.take(24).ifBlank { "Housemate" },
            "characterId" to account.characterId,
            "teamId" to teamId,
            "points" to 0L,
            "housePointsContribution" to 0L,
            "tasksCompleted" to 0L,
            "mealsCooked" to 0L,
            "achievements" to emptyList<String>(),
            "lastGrantId" to "init",
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(P7_REPO_TAG, "Leaderboard init deferred: ${e.message}")
    }
  }

  suspend fun collectRationIngredient(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    itemId: String,
    amount: Long,
    account: UserAccount,
  ): Result<String> {
    return try {
      val uid = requireUserId()
      val safeAmt = amount.coerceIn(1L, 5L)
      val rationRef = seasonDoc(seasonId).collection("kitchenRations").document(itemId)
      val profileRef = seasonDoc(seasonId).collection("kitchenProfiles").document(uid)
      val nowMs = System.currentTimeMillis()
      val cleanName = account.displayName.take(24).ifBlank { "Housemate" }

      val pickedName = db.runTransaction { tx ->
        val rationSnap = tx.get(rationRef)
        val item = rationSnap.toObject(RationItem::class.java)
          ?: throw IllegalStateException("Ration item not stocked yet.")
        val profSnap = tx.get(profileRef)
        val prof = profSnap.toObject(KitchenPlayerState::class.java)
          ?: KitchenPlayerState(seasonId = seasonId, userId = uid, displayName = cleanName)

        Phase7Logic.canCollectRation(item, safeAmt, prof.lastCollectedAtMillis, nowMs).getOrThrow()

        val nextQty = item.quantity - safeAmt
        val nextBag = prof.bagItems.toMutableMap()
        nextBag[itemId] = ((nextBag[itemId] ?: 0L) + safeAmt).coerceAtMost(20L)

        tx.update(
          rationRef,
          mapOf(
            "quantity" to nextQty,
            "lastUpdatedBy" to uid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          profileRef,
          mapOf(
            "seasonId" to seasonId,
            "userId" to uid,
            "displayName" to cleanName,
            "energy" to prof.energy.coerceIn(0L, 100L),
            "hunger" to prof.hunger.coerceIn(0L, 100L),
            "bagItems" to nextBag,
            "activeCookingId" to prof.activeCookingId,
            "lastCollectedAtMillis" to nowMs,
            "lastCookedAtMillis" to prof.lastCookedAtMillis,
            "lastEatAtMillis" to prof.lastEatAtMillis,
            "mealsCookedCount" to prof.mealsCookedCount,
            "mealsEatenCount" to prof.mealsEatenCount,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        item.name
      }.await()

      Result.success("Collected $safeAmt × $pickedName into your kitchen pouch.")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(P7_REPO_TAG, "collectRationIngredient failed: ${e.message}")
      Result.failure(e)
    }
  }

  suspend fun startVirtualCooking(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    stationId: String,
    recipe: VirtualRecipe,
    account: UserAccount,
  ): Result<CookingSession> {
    return try {
      val uid = requireUserId()
      val profileRef = seasonDoc(seasonId).collection("kitchenProfiles").document(uid)
      val sessionId = "cook_${UUID.randomUUID().toString().replace("-", "").take(10)}"
      val sessionRef = seasonDoc(seasonId).collection("kitchenCooking").document(sessionId)
      val nowMs = System.currentTimeMillis()
      val readyAtMs = nowMs + recipe.cookDurationSeconds * 1000L
      val cleanName = account.displayName.take(24).ifBlank { "Housemate" }

      db.runTransaction { tx ->
        val profSnap = tx.get(profileRef)
        val prof = profSnap.toObject(KitchenPlayerState::class.java)
          ?: throw IllegalStateException("Collect rations from the Pantry first!")

        val cd = Phase7Logic.remainingCooldownSeconds(prof.lastCookedAtMillis, recipe.cooldownSeconds, nowMs)
        if (cd > 0L) throw IllegalStateException("Cooking cooldown: wait ${cd}s.")

        val missing = Phase7Logic.missingIngredients(prof.bagItems, recipe)
        if (missing.isNotEmpty()) {
          val needText = missing.entries.joinToString(", ") { "${it.value}× ${it.key}" }
          throw IllegalStateException("Missing rations: $needText")
        }

        val updatedBag = Phase7Logic.deductIngredients(prof.bagItems, recipe)

        tx.set(
          sessionRef,
          mapOf(
            "seasonId" to seasonId,
            "stationId" to stationId,
            "recipeId" to recipe.recipeId,
            "recipeName" to recipe.name,
            "chefUid" to uid,
            "chefDisplayName" to cleanName,
            "status" to "COOKING",
            "ingredientsUsed" to recipe.ingredients,
            "startedAtMillis" to nowMs,
            "readyAtMillis" to readyAtMs,
            "energyRestore" to recipe.energyRestore,
            "servingsTotal" to recipe.servings,
            "servingsRemaining" to recipe.servings,
            "claimedByUids" to emptyList<String>(),
            "updatedBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          profileRef,
          mapOf(
            "seasonId" to seasonId,
            "userId" to uid,
            "displayName" to cleanName,
            "energy" to prof.energy.coerceIn(0L, 100L),
            "hunger" to prof.hunger.coerceIn(0L, 100L),
            "bagItems" to updatedBag,
            "activeCookingId" to sessionId,
            "lastCollectedAtMillis" to prof.lastCollectedAtMillis,
            "lastCookedAtMillis" to nowMs,
            "lastEatAtMillis" to prof.lastEatAtMillis,
            "mealsCookedCount" to prof.mealsCookedCount,
            "mealsEatenCount" to prof.mealsEatenCount,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()

      Result.success(
        CookingSession(
          id = sessionId,
          seasonId = seasonId,
          stationId = stationId,
          recipeId = recipe.recipeId,
          recipeName = recipe.name,
          chefUid = uid,
          chefDisplayName = cleanName,
          status = "COOKING",
          ingredientsUsed = recipe.ingredients,
          startedAtMillis = nowMs,
          readyAtMillis = readyAtMs,
          energyRestore = recipe.energyRestore,
          servingsTotal = recipe.servings,
          servingsRemaining = recipe.servings,
        ),
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(P7_REPO_TAG, "startVirtualCooking failed: ${e.message}")
      Result.failure(e)
    }
  }

  suspend fun completeVirtualCooking(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    session: CookingSession,
    account: UserAccount,
    teamId: String?,
  ): Result<String> {
    return try {
      val uid = requireUserId()
      val nowMs = System.currentTimeMillis()
      if (nowMs < session.readyAtMillis) {
        val waitSec = ((session.readyAtMillis - nowMs + 999L) / 1000L).coerceAtLeast(1L)
        return Result.failure(IllegalStateException("Still cooking — ready in ${waitSec}s."))
      }

      // Ensure leaderboard doc exists beforehand so rule `update` + getAfter succeeds atomically
      ensureLeaderboardEntryInitialized(seasonId, account, teamId)

      val sessionRef = seasonDoc(seasonId).collection("kitchenCooking").document(session.id)
      val profileRef = seasonDoc(seasonId).collection("kitchenProfiles").document(uid)
      val lbRef = seasonDoc(seasonId).collection("leaderboard").document(uid)
      val grantId = Phase8Logic.grantIdForCooking(session.id, uid)
      val grantRef = seasonDoc(seasonId).collection("rewardGrants").document(grantId)
      val cleanName = account.displayName.take(24).ifBlank { "Housemate" }

      val awardedPts = 25L

      db.runTransaction { tx ->
        val sesSnap = tx.get(sessionRef)
        if (!sesSnap.exists()) throw IllegalStateException("Cooking session no longer exists.")
        val status = sesSnap.getString("status") ?: ""
        if (status != "COOKING") throw IllegalStateException("Meal is already $status.")

        val profSnap = tx.get(profileRef)
        val prof = profSnap.toObject(KitchenPlayerState::class.java)
          ?: KitchenPlayerState(seasonId = seasonId, userId = uid, displayName = cleanName)

        val grantSnap = tx.get(grantRef)
        val lbSnap = tx.get(lbRef)
        val lb = lbSnap.toObject(PlayerLeaderboardEntry::class.java)

        val nextCooked = (prof.mealsCookedCount + 1L).coerceAtMost(10000L)
        val achievementCode = if (nextCooked >= 3L) "MASTER_CHEF" else "FIRST_CHEF"

        tx.update(
          sessionRef,
          mapOf(
            "status" to "READY",
            "updatedBy" to uid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          profileRef,
          mapOf(
            "seasonId" to seasonId,
            "userId" to uid,
            "displayName" to cleanName,
            "energy" to prof.energy.coerceIn(0L, 100L),
            "hunger" to prof.hunger.coerceIn(0L, 100L),
            "bagItems" to prof.bagItems,
            "activeCookingId" to null,
            "lastCollectedAtMillis" to prof.lastCollectedAtMillis,
            "lastCookedAtMillis" to prof.lastCookedAtMillis,
            "lastEatAtMillis" to prof.lastEatAtMillis,
            "mealsCookedCount" to nextCooked,
            "mealsEatenCount" to prof.mealsEatenCount,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        if (!grantSnap.exists() && lb != null) {
          val nextAch = (lb.achievements + achievementCode).distinct().take(24)
          tx.set(
            grantRef,
            mapOf(
              "seasonId" to seasonId,
              "grantId" to grantId,
              "userId" to uid,
              "sourceType" to "COOKING",
              "sourceId" to session.id,
              "awardedPoints" to awardedPts,
              "achievementId" to achievementCode,
              "grantedBy" to uid,
              "createdAt" to FieldValue.serverTimestamp(),
            ),
          )
          tx.update(
            lbRef,
            mapOf(
              "displayName" to cleanName,
              "characterId" to account.characterId,
              "teamId" to teamId,
              "points" to (lb.points + awardedPts).coerceAtMost(1000000L),
              "housePointsContribution" to (lb.housePointsContribution + awardedPts).coerceAtMost(1000000L),
              "mealsCooked" to (lb.mealsCooked + 1L).coerceAtMost(10000L),
              "achievements" to nextAch,
              "lastGrantId" to grantId,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
      }.await()

      Result.success("${session.recipeName} is plated for ${session.servingsTotal} servings! (+$awardedPts House Points)")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(P7_REPO_TAG, "completeVirtualCooking failed: ${e.message}")
      Result.failure(e)
    }
  }

  suspend fun claimAndEatMeal(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    session: CookingSession,
    account: UserAccount,
  ): Result<Pair<Long, Long>> {
    return try {
      val uid = requireUserId()
      val sessionRef = seasonDoc(seasonId).collection("kitchenCooking").document(session.id)
      val profileRef = seasonDoc(seasonId).collection("kitchenProfiles").document(uid)
      val nowMs = System.currentTimeMillis()
      val cleanName = account.displayName.take(24).ifBlank { "Housemate" }

      val resultStats = db.runTransaction { tx ->
        val sesSnap = tx.get(sessionRef)
        val liveSes = sesSnap.toObject(CookingSession::class.java)
          ?: throw IllegalStateException("Meal no longer exists.")
        if (liveSes.status !in listOf("COOKING", "READY")) {
          throw IllegalStateException("This meal is already ${liveSes.status.lowercase()}.")
        }
        if (nowMs < liveSes.readyAtMillis) {
          val waitSec = ((liveSes.readyAtMillis - nowMs + 999L) / 1000L).coerceAtLeast(1L)
          throw IllegalStateException("Meal finishes cooking in ${waitSec}s.")
        }
        if (liveSes.servingsRemaining <= 0L) {
          throw IllegalStateException("All servings have already been eaten.")
        }
        if (uid in liveSes.claimedByUids) {
          throw IllegalStateException("You already claimed a serving from this dish.")
        }

        val profSnap = tx.get(profileRef)
        val prof = profSnap.toObject(KitchenPlayerState::class.java)
          ?: KitchenPlayerState(seasonId = seasonId, userId = uid, displayName = cleanName)

        val eatCd = Phase7Logic.remainingCooldownSeconds(prof.lastEatAtMillis, Phase7Catalog.EAT_COOLDOWN_SEC, nowMs)
        if (eatCd > 0L) throw IllegalStateException("Wait ${eatCd}s before eating another meal.")

        val nextRem = (liveSes.servingsRemaining - 1L).coerceAtLeast(0L)
        val nextStatus = if (nextRem <= 0L) "CLAIMED" else "READY"
        val nextClaimed = liveSes.claimedByUids + uid
        val (newEnergy, newHunger) = Phase7Logic.applyMealNutrition(prof.energy, prof.hunger, liveSes.energyRestore)

        tx.update(
          sessionRef,
          mapOf(
            "servingsRemaining" to nextRem,
            "claimedByUids" to nextClaimed,
            "status" to nextStatus,
            "updatedBy" to uid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          profileRef,
          mapOf(
            "seasonId" to seasonId,
            "userId" to uid,
            "displayName" to cleanName,
            "energy" to newEnergy,
            "hunger" to newHunger,
            "bagItems" to prof.bagItems,
            "activeCookingId" to prof.activeCookingId,
            "lastCollectedAtMillis" to prof.lastCollectedAtMillis,
            "lastCookedAtMillis" to prof.lastCookedAtMillis,
            "lastEatAtMillis" to nowMs,
            "mealsCookedCount" to prof.mealsCookedCount,
            "mealsEatenCount" to (prof.mealsEatenCount + 1L).coerceAtMost(10000L),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        newEnergy to newHunger
      }.await()

      Result.success(resultStats)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(P7_REPO_TAG, "claimAndEatMeal failed: ${e.message}")
      Result.failure(e)
    }
  }

  // --- ADMIN KITCHEN CONTROLS ---

  suspend fun adminReplenishRation(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    item: RationItem,
    addAmount: Long = 10L,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val nextQty = (item.quantity + addAmount).coerceIn(0L, item.maxQuantity)
      val batch = db.batch()
      batch.update(
        seasonDoc(seasonId).collection("kitchenRations").document(item.itemId),
        mapOf(
          "quantity" to nextQty,
          "lastUpdatedBy" to adminUid,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("adminControls").document(),
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "RATION_REPLENISH",
          "payloadSummary" to "Replenished ${item.name} -> $nextQty/${item.maxQuantity}".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun adminReplenishAllRations(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    items: List<RationItem>,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val col = seasonDoc(seasonId).collection("kitchenRations")
      val batch = db.batch()
      val sourceList = items.ifEmpty { Phase7Catalog.defaultRations }
      sourceList.forEach { r ->
        batch.set(
          col.document(r.itemId),
          mapOf(
            "seasonId" to seasonId,
            "itemId" to r.itemId,
            "name" to r.name,
            "category" to r.category,
            "unit" to r.unit,
            "quantity" to r.maxQuantity,
            "maxQuantity" to r.maxQuantity,
            "minThreshold" to r.minThreshold,
            "permitted" to r.permitted,
            "energyValue" to r.energyValue,
            "lastUpdatedBy" to adminUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
          SetOptions.merge(),
        )
      }
      batch.set(
        seasonDoc(seasonId).collection("adminControls").document(),
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "RATION_REPLENISH",
          "payloadSummary" to "Admin replenished full house ration pantry (${sourceList.size} items).".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun adminToggleRationPermission(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    item: RationItem,
    permitted: Boolean,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val batch = db.batch()
      batch.update(
        seasonDoc(seasonId).collection("kitchenRations").document(item.itemId),
        mapOf(
          "permitted" to permitted,
          "lastUpdatedBy" to adminUid,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("adminControls").document(),
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "KITCHEN_MANAGE",
          "payloadSummary" to "${item.name} ${if (permitted) "permitted" else "restricted"} by Admin".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun adminManageCookingSession(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    session: CookingSession,
    nextStatus: String,
  ): Result<Unit> {
    return try {
      val adminUid = requireUserId()
      val cleanStatus = if (nextStatus in listOf("READY", "CANCELLED")) nextStatus else "CANCELLED"
      val batch = db.batch()
      batch.update(
        seasonDoc(seasonId).collection("kitchenCooking").document(session.id),
        mapOf(
          "status" to cleanStatus,
          "updatedBy" to adminUid,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        seasonDoc(seasonId).collection("adminControls").document(),
        mapOf(
          "seasonId" to seasonId,
          "commandType" to "KITCHEN_MANAGE",
          "payloadSummary" to "Admin set ${session.recipeName} (${session.chefDisplayName}) -> $cleanStatus".take(300),
          "issuedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // --- PHASE 8: TASK CHECKPOINTS & ATOMIC ANTI-DUPLICATE REWARD GRANTS ---

  suspend fun recordTaskCheckpointProgress(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    task: HouseTask,
    stepId: String,
    displayName: String,
  ): Result<TaskPlayerProgress> {
    return try {
      val uid = requireUserId()
      val progRef = seasonDoc(seasonId).collection("tasks").document(task.id)
        .collection("progress").document(uid)
      val nowMs = System.currentTimeMillis()
      val elapsedSec = if (task.startTimeMillis > 0L) {
        ((nowMs - task.startTimeMillis) / 1000L).coerceIn(0L, 7200L)
      } else 0L
      val cleanName = displayName.take(24).ifBlank { "Housemate" }

      val updated = db.runTransaction { tx ->
        val snap = tx.get(progRef)
        val existingSteps = snap.toObject(TaskPlayerProgress::class.java)?.completedSteps.orEmpty()
        val nextSteps = (existingSteps + stepId).distinct().take(12)
        val pct = Phase8Logic.computeProgressPercent(nextSteps, task.requiredCheckpoints)
        val nextStatus = if (pct >= 100L) "COMPLETED" else "IN_PROGRESS"

        tx.set(
          progRef,
          mapOf(
            "seasonId" to seasonId,
            "taskId" to task.id,
            "userId" to uid,
            "displayName" to cleanName,
            "completedSteps" to nextSteps,
            "percent" to pct,
            "status" to nextStatus,
            "elapsedSeconds" to elapsedSec,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        TaskPlayerProgress(
          id = uid,
          seasonId = seasonId,
          taskId = task.id,
          userId = uid,
          displayName = cleanName,
          completedSteps = nextSteps,
          percent = pct,
          status = nextStatus,
          elapsedSeconds = elapsedSec,
        )
      }.await()

      Result.success(updated)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(P7_REPO_TAG, "recordTaskCheckpointProgress failed: ${e.message}")
      Result.failure(e)
    }
  }

  /**
   * Atomically submits completion proof (if not yet submitted) and grants the one-time
   * deterministic `TASK_{taskId}_{uid}` reward into `/leaderboard/{uid}` + `/rewardGrants/{grantId}`.
   */
  suspend fun completeAndClaimTaskReward(
    seasonId: String = MultiplayerFirestoreRepository.DEFAULT_SEASON_ID,
    task: HouseTask,
    account: UserAccount,
    teamId: String?,
    proofText: String,
  ): Result<Long> {
    return try {
      val uid = requireUserId()
      ensureLeaderboardEntryInitialized(seasonId, account, teamId)

      val taskRef = seasonDoc(seasonId).collection("tasks").document(task.id)
      val subRef = taskRef.collection("submissions").document(uid)
      val progRef = taskRef.collection("progress").document(uid)
      val lbRef = seasonDoc(seasonId).collection("leaderboard").document(uid)
      val grantId = Phase8Logic.grantIdForTask(task.id, uid)
      val grantRef = seasonDoc(seasonId).collection("rewardGrants").document(grantId)

      val cleanName = account.displayName.take(24).ifBlank { "Housemate" }
      val nowMs = System.currentTimeMillis()
      val difficulty = TaskDifficulty.fromRaw(task.difficulty)
      val pointsToGrant = Phase8Logic.calculateRewardPoints(task.maxPoints, difficulty)
        .coerceAtMost(task.maxPoints.coerceIn(1L, 1000L))
      val achCode = task.rewardAchievement?.takeIf { it.isNotBlank() } ?: "TASK_FINISHER"

      val granted = db.runTransaction { tx ->
        val taskSnap = tx.get(taskRef)
        val liveTask = taskSnap.toObject(HouseTask::class.java)
          ?: throw IllegalStateException("Task not found.")
        if (!TaskLifecycleStatus.fromRaw(liveTask.status).isPlayable()) {
          throw IllegalStateException("Task is currently ${liveTask.status}.")
        }
        if (liveTask.endTimeMillis > 0L && nowMs > liveTask.endTimeMillis + 3000L) {
          throw IllegalStateException("Task deadline expired.")
        }

        val grantSnap = tx.get(grantRef)
        if (grantSnap.exists()) {
          throw IllegalStateException("Reward already claimed for this task.")
        }

        val subSnap = tx.get(subRef)
        val lbSnap = tx.get(lbRef)
        val lb = lbSnap.toObject(PlayerLeaderboardEntry::class.java)
          ?: throw IllegalStateException("Leaderboard entry missing.")

        val elapsedSec = if (liveTask.startTimeMillis > 0L) {
          ((nowMs - liveTask.startTimeMillis) / 1000L).coerceIn(0L, 7200L)
        } else 0L

        if (!subSnap.exists()) {
          tx.set(
            subRef,
            mapOf(
              "seasonId" to seasonId,
              "taskId" to task.id,
              "userId" to uid,
              "playerId" to account.playerId,
              "displayName" to cleanName,
              "teamId" to teamId,
              "answerText" to proofText.trim().take(300).ifBlank { "COMPLETED" },
              "elapsedSeconds" to elapsedSec,
              "verificationStatus" to "SUBMITTED",
              "awardedScore" to 0L,
              "reviewedBy" to null,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }

        tx.set(
          progRef,
          mapOf(
            "seasonId" to seasonId,
            "taskId" to task.id,
            "userId" to uid,
            "displayName" to cleanName,
            "completedSteps" to (liveTask.requiredCheckpoints.ifEmpty { listOf("completed") }).take(12),
            "percent" to 100L,
            "status" to "COMPLETED",
            "elapsedSeconds" to elapsedSec,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        tx.set(
          grantRef,
          mapOf(
            "seasonId" to seasonId,
            "grantId" to grantId,
            "userId" to uid,
            "sourceType" to "TASK",
            "sourceId" to task.id,
            "awardedPoints" to pointsToGrant,
            "achievementId" to achCode,
            "grantedBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )

        val nextAch = (lb.achievements + achCode).distinct().take(24)
        tx.update(
          lbRef,
          mapOf(
            "displayName" to cleanName,
            "characterId" to account.characterId,
            "teamId" to teamId,
            "points" to (lb.points + pointsToGrant).coerceAtMost(1000000L),
            "housePointsContribution" to (lb.housePointsContribution + pointsToGrant).coerceAtMost(1000000L),
            "tasksCompleted" to (lb.tasksCompleted + 1L).coerceAtMost(10000L),
            "achievements" to nextAch,
            "lastGrantId" to grantId,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        pointsToGrant
      }.await()

      Result.success(granted)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(P7_REPO_TAG, "completeAndClaimTaskReward failed: ${e.message}")
      Result.failure(e)
    }
  }
}
