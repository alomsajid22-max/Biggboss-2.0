package com.example.multiplayer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SoupKitchen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

private val KitchenBg = Color(0xFF0C1222)
private val KitchenCard = Color(0xFF151E33)
private val KitchenElevated = Color(0xFF1C2742)
private val AmberAccent = Color(0xFFFB923C)
private val Gold = Color(0xFFEAB954)
private val Cyan = Color(0xFF22D3EE)
private val Emerald = Color(0xFF34D399)
private val Danger = Color(0xFFF87171)
private val Muted = Color(0xFF94A3B8)

@Composable
fun KitchenAndCookingDialog(
  currentUserId: String,
  initialTab: Int = 0,
  initialStationId: String = "",
  rations: List<RationItem>,
  kitchenProfile: KitchenPlayerState?,
  cookingSessions: List<CookingSession>,
  isBusy: Boolean,
  actionMessage: String?,
  actionError: String?,
  onCollectRation: (itemId: String, amount: Long) -> Unit,
  onStartCooking: (stationId: String, recipe: VirtualRecipe) -> Unit,
  onCompleteCooking: (CookingSession) -> Unit,
  onClaimAndEatMeal: (session: CookingSession, seated: Boolean) -> Unit,
  onClearFeedback: () -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
  var selectedStationId by remember(initialStationId) {
    mutableStateOf(
      if (initialStationId in listOf("kitchen_stove_1", "kitchen_stove_2", "kitchen_prep")) {
        initialStationId
      } else {
        "kitchen_stove_1"
      }
    )
  }
  val isSeatedDining = initialStationId == "seated_dining"
  val tabs = listOf("Ration Pantry", "Cooking Studio", "Meals & Dining")

  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      nowMillis = System.currentTimeMillis()
      delay(1000L)
    }
  }

  val energy = (kitchenProfile?.energy ?: 75L).coerceIn(0L, 100L)
  val hunger = (kitchenProfile?.hunger ?: 30L).coerceIn(0L, 100L)
  val bag = kitchenProfile?.bagItems.orEmpty()
  val pouchCount = bag.values.sum()

  val collectCd = Phase7Logic.remainingCooldownSeconds(
    lastActionMillis = kitchenProfile?.lastCollectedAtMillis ?: 0L,
    cooldownSeconds = Phase7Catalog.COLLECT_COOLDOWN_SEC,
    nowMillis = nowMillis,
  )
  val eatCd = Phase7Logic.remainingCooldownSeconds(
    lastActionMillis = kitchenProfile?.lastEatAtMillis ?: 0L,
    cooldownSeconds = Phase7Catalog.EAT_COOLDOWN_SEC,
    nowMillis = nowMillis,
  )

  val activeCooking = remember(cookingSessions) {
    cookingSessions.filter { it.status == "COOKING" }
  }
  val readyMeals = remember(cookingSessions) {
    cookingSessions.filter {
      it.servingsRemaining > 0L && (it.status == "READY" || (it.status == "COOKING" && it.readyAtMillis <= nowMillis))
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = KitchenBg,
      border = BorderStroke(1.5.dp, AmberAccent.copy(alpha = 0.6f)),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.9f)
        .widthIn(max = 920.dp),
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AmberAccent.copy(alpha = 0.18f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(Icons.Rounded.SoupKitchen, contentDescription = null, tint = AmberAccent)
            }
            Spacer(Modifier.width(10.dp))
            Column {
              Text(
                "BIGG BOSS – THE HOUSE • VIRTUAL KITCHEN & RATIONS",
                color = AmberAccent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                "Pouch: $pouchCount items • ${readyMeals.size} meals served",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp).testTag("close_kitchen_dialog_btn"),
          ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close Kitchen", tint = Color.White)
          }
        }

        Spacer(Modifier.height(10.dp))

        // Live Energy & Hunger Status Bars
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = KitchenCard,
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Rounded.Bolt, contentDescription = "Energy", tint = Emerald, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Energy", color = Emerald, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Text("$energy%", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
              }
              LinearProgressIndicator(
                progress = { energy / 100f },
                color = Emerald,
                trackColor = KitchenElevated,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
              )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Rounded.Restaurant, contentDescription = "Hunger", tint = AmberAccent, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Hunger", color = AmberAccent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Text("$hunger%", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
              }
              LinearProgressIndicator(
                progress = { hunger / 100f },
                color = if (hunger > 70L) Danger else AmberAccent,
                trackColor = KitchenElevated,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
              )
            }
          }
        }

        Spacer(Modifier.height(10.dp))

        // Tab Row
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          tabs.forEachIndexed { idx, label ->
            val picked = selectedTab == idx
            val badge = when (idx) {
              1 -> if (activeCooking.isNotEmpty()) " (${activeCooking.size})" else ""
              2 -> if (readyMeals.isNotEmpty()) " (${readyMeals.size})" else ""
              else -> ""
            }
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = if (picked) AmberAccent else KitchenElevated,
              modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable {
                  onClearFeedback()
                  selectedTab = idx
                }
                .testTag("kitchen_tab_$idx"),
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                  text = "$label$badge",
                  color = if (picked) Color(0xFF1F0C02) else Color.White,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }

        if (!actionError.isNullOrBlank() || !actionMessage.isNullOrBlank()) {
          Spacer(Modifier.height(8.dp))
          val isErr = !actionError.isNullOrBlank()
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = (if (isErr) Danger else Emerald).copy(alpha = 0.16f),
            border = BorderStroke(1.dp, if (isErr) Danger else Emerald),
            modifier = Modifier.fillMaxWidth().clickable { onClearFeedback() },
          ) {
            Text(
              text = actionError ?: actionMessage.orEmpty(),
              color = Color.White,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(10.dp),
            )
          }
        }

        Spacer(Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          when (selectedTab) {
            0 -> {
              Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Kitchen, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("SHARED HOUSE RATION PANTRY", color = Cyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                  }
                  if (collectCd > 0L) {
                    Text("Cooldown: ${collectCd}s", color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                  }
                }

                val displayRations = if (rations.isNotEmpty()) rations else Phase7Catalog.defaultRations
                displayRations.forEach { item ->
                  val stockStatus = Phase7Logic.stockStatus(item)
                  val statusColor = when (stockStatus) {
                    RationStockStatus.AVAILABLE -> Emerald
                    RationStockStatus.LOW_STOCK -> Gold
                    RationStockStatus.OUT_OF_STOCK, RationStockStatus.RESTRICTED -> Danger
                  }
                  val inBag = bag[item.itemId] ?: 0L
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = KitchenCard,
                    border = BorderStroke(1.dp, if (!item.permitted) Danger.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(12.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                          Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                          Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.18f)) {
                            Text(
                              stockStatus.name,
                              color = statusColor,
                              fontSize = 10.sp,
                              fontWeight = FontWeight.ExtraBold,
                              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                          }
                          if (!item.permitted) {
                            Surface(shape = RoundedCornerShape(999.dp), color = Danger.copy(alpha = 0.22f)) {
                              Text(
                                "RESTRICTED",
                                color = Danger,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                              )
                            }
                          }
                        }
                        Text(
                          "Pantry: ${item.quantity}/${item.maxQuantity} ${item.unit} • Your Pouch: $inBag • +${item.energyValue}⚡",
                          color = Muted,
                          style = MaterialTheme.typography.labelSmall,
                        )
                      }
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                          onClick = { onCollectRation(item.itemId, 1L) },
                          enabled = !isBusy && item.permitted && item.quantity >= 1L && collectCd == 0L,
                          border = BorderStroke(1.dp, Cyan),
                          modifier = Modifier.heightIn(min = 48.dp).testTag("collect_1_${item.itemId}"),
                        ) {
                          Text("+1", color = Cyan, fontWeight = FontWeight.Bold)
                        }
                        Button(
                          onClick = { onCollectRation(item.itemId, 3L) },
                          enabled = !isBusy && item.permitted && item.quantity >= 3L && collectCd == 0L,
                          colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color(0xFF1F0C02)),
                          modifier = Modifier.heightIn(min = 48.dp).testTag("collect_3_${item.itemId}"),
                        ) {
                          Text("+3", fontWeight = FontWeight.ExtraBold)
                        }
                      }
                    }
                  }
                }
              }
            }

            1 -> {
              Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                // Active Cooking Timers
                if (activeCooking.isNotEmpty()) {
                  Text("LIVE STOVE TIMERS (${activeCooking.size})", color = AmberAccent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                  activeCooking.forEach { ses ->
                    val remSec = ((ses.readyAtMillis - nowMillis) / 1000L).coerceAtLeast(0L)
                    val isDone = remSec <= 0L
                    Surface(
                      shape = RoundedCornerShape(16.dp),
                      color = KitchenElevated,
                      border = BorderStroke(1.dp, if (isDone) Emerald else AmberAccent),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(ses.recipeName, color = Color.White, fontWeight = FontWeight.Bold)
                          Text(
                            "Chef: ${ses.chefDisplayName} • ${ses.stationId.replace("kitchen_", "").uppercase()}",
                            color = Muted,
                            style = MaterialTheme.typography.labelSmall,
                          )
                          Text(
                            if (isDone) "Ready to plate & serve (+25 House Pts)!" else "Simmering... ${remSec}s left",
                            color = if (isDone) Emerald else AmberAccent,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                          )
                        }
                        if (isDone && ses.chefUid == currentUserId) {
                          Button(
                            onClick = { onCompleteCooking(ses) },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color(0xFF06281E)),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("plate_meal_${ses.id}"),
                          ) {
                            Text("Plate & Serve", fontWeight = FontWeight.ExtraBold)
                          }
                        }
                      }
                    }
                  }
                }

                // Station Selector
                Text("SELECT COOKING STATION", color = Cyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                  listOf(
                    "kitchen_stove_1" to "Stove #1",
                    "kitchen_stove_2" to "Stove #2",
                    "kitchen_prep" to "Island Prep",
                  ).forEach { (stId, label) ->
                    val picked = selectedStationId == stId
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = if (picked) Cyan.copy(alpha = 0.22f) else KitchenCard,
                      border = BorderStroke(1.dp, if (picked) Cyan else Color.White.copy(alpha = 0.12f)),
                      modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clickable { selectedStationId = stId },
                    ) {
                      Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                        Text(label, color = if (picked) Cyan else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                      }
                    }
                  }
                }

                // Virtual Recipes Catalog
                Text("VIRTUAL HOUSE DISHES (IN-GAME MECHANIC)", color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Phase7Catalog.virtualRecipes.forEach { recipe ->
                  val missing = Phase7Logic.missingIngredients(bag, recipe)
                  val canCook = missing.isEmpty()
                  val cookCd = Phase7Logic.remainingCooldownSeconds(
                    lastActionMillis = kitchenProfile?.lastCookedAtMillis ?: 0L,
                    cooldownSeconds = recipe.cooldownSeconds,
                    nowMillis = nowMillis,
                  )
                  Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = KitchenCard,
                    border = BorderStroke(1.dp, if (canCook) Emerald.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f)),
                  ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(recipe.name, color = Color.White, fontWeight = FontWeight.Bold)
                          Text(recipe.tagline, color = Muted, style = MaterialTheme.typography.bodySmall)
                          Text(
                            "${recipe.cookDurationSeconds}s cook • ${recipe.servings} servings • +${recipe.energyRestore}⚡",
                            color = Gold,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                          )
                        }
                        Button(
                          onClick = { onStartCooking(selectedStationId, recipe) },
                          enabled = !isBusy && canCook && cookCd == 0L,
                          colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color(0xFF1F0C02)),
                          modifier = Modifier.heightIn(min = 48.dp).testTag("cook_recipe_${recipe.recipeId}"),
                        ) {
                          Text(if (cookCd > 0L) "Wait ${cookCd}s" else "Start Cooking", fontWeight = FontWeight.ExtraBold)
                        }
                      }

                      // Ingredient Readiness Pills
                      Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                      ) {
                        recipe.ingredients.forEach { (ingId, needQty) ->
                          val haveQty = bag[ingId] ?: 0L
                          val enough = haveQty >= needQty
                          Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = (if (enough) Emerald else AmberAccent).copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, if (enough) Emerald else AmberAccent),
                          ) {
                            Text(
                              text = "$ingId: $haveQty/$needQty",
                              color = if (enough) Emerald else AmberAccent,
                              fontSize = 11.sp,
                              fontWeight = FontWeight.Bold,
                              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            2 -> {
              Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocalDining, contentDescription = null, tint = Emerald, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("SERVED HOUSE MEALS (${readyMeals.size})", color = Emerald, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                  }
                  if (eatCd > 0L) {
                    Text("Eat Cooldown: ${eatCd}s", color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                  }
                }

                if (readyMeals.isEmpty()) {
                  Surface(shape = RoundedCornerShape(16.dp), color = KitchenCard, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Text("No meals on the counter or dining table yet.", color = Color.White, fontWeight = FontWeight.Bold)
                      Text("Collect rations in Pantry and cook a dish in Cooking Studio to serve the whole house!", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                  }
                } else {
                  readyMeals.forEach { meal ->
                    val alreadyClaimed = currentUserId in meal.claimedByUids
                    Surface(
                      shape = RoundedCornerShape(16.dp),
                      color = KitchenCard,
                      border = BorderStroke(1.dp, if (alreadyClaimed) Color.White.copy(alpha = 0.1f) else Emerald.copy(alpha = 0.55f)),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                          Text(meal.recipeName, color = Color.White, fontWeight = FontWeight.Bold)
                          Text("Prepared by ${meal.chefDisplayName}", color = Cyan, style = MaterialTheme.typography.labelSmall)
                          Text(
                            "Servings left: ${meal.servingsRemaining}/${meal.servingsTotal} • +${meal.energyRestore}⚡ Energy",
                            color = Emerald,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                          )
                        }
                        Button(
                          onClick = { onClaimAndEatMeal(meal, isSeatedDining) },
                          enabled = !isBusy && !alreadyClaimed && eatCd == 0L && meal.servingsRemaining > 0L,
                          colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color(0xFF06281E)),
                          modifier = Modifier.heightIn(min = 48.dp).testTag("claim_meal_${meal.id}"),
                        ) {
                          Text(if (alreadyClaimed) "Claimed" else "Claim & Eat", fontWeight = FontWeight.ExtraBold)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
