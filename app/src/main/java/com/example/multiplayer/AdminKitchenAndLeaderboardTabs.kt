package com.example.multiplayer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SoupKitchen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardBg = Color(0xFF151E33)
private val ElevatedBg = Color(0xFF1C2742)
private val Amber = Color(0xFFFB923C)
private val Gold = Color(0xFFEAB954)
private val Cyan = Color(0xFF22D3EE)
private val Emerald = Color(0xFF34D399)
private val Danger = Color(0xFFF87171)
private val Muted = Color(0xFF94A3B8)

@Composable
fun AdminKitchenManagementTab(
  rations: List<RationItem>,
  cookingSessions: List<CookingSession>,
  isBusy: Boolean,
  onReplenishRation: (RationItem, Long) -> Unit,
  onReplenishAllRations: () -> Unit,
  onToggleRationPermission: (RationItem, Boolean) -> Unit,
  onManageCookingSession: (CookingSession, String) -> Unit,
) {
  val lowCount = rations.count { Phase7Logic.stockStatus(it) != RationStockStatus.AVAILABLE }
  val restrictedCount = rations.count { !it.permitted }
  val activeCookCount = cookingSessions.count { it.status == "COOKING" }
  val readyMealCount = cookingSessions.count { it.status == "READY" && it.servingsRemaining > 0L }

  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      AdminKitchenStat("LOW STOCK", "$lowCount Items", if (lowCount > 0) Amber else Emerald, Modifier.weight(1f))
      AdminKitchenStat("RESTRICTED", "$restrictedCount Locked", if (restrictedCount > 0) Danger else Cyan, Modifier.weight(1f))
      AdminKitchenStat("STOVES LIVE", "$activeCookCount Active", Amber, Modifier.weight(1f))
      AdminKitchenStat("READY MEALS", "$readyMealCount Served", Emerald, Modifier.weight(1f))
    }

    Button(
      onClick = onReplenishAllRations,
      enabled = !isBusy,
      colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color(0xFF06281E)),
      modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_replenish_all_rations_btn"),
    ) {
      Icon(Icons.Rounded.Refresh, contentDescription = null)
      Spacer(Modifier.width(6.dp))
      Text("Replenish Full House Pantry", fontWeight = FontWeight.ExtraBold)
    }

    // Ration Pantry Inventory & Permission Controls
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = CardBg,
      border = BorderStroke(1.dp, Cyan.copy(alpha = 0.25f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("DAILY RATION DISTRIBUTION & LOCKS", color = Cyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        val displayRations = if (rations.isNotEmpty()) rations else Phase7Catalog.defaultRations
        displayRations.forEach { item ->
          val status = Phase7Logic.stockStatus(item)
          val statusColor = when (status) {
            RationStockStatus.AVAILABLE -> Emerald
            RationStockStatus.LOW_STOCK -> Gold
            RationStockStatus.OUT_OF_STOCK, RationStockStatus.RESTRICTED -> Danger
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(ElevatedBg)
              .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("${item.name} (${item.category})", color = Color.White, fontWeight = FontWeight.Bold)
              Text(
                "Stock: ${item.quantity}/${item.maxQuantity} ${item.unit} • ${status.name}",
                color = statusColor,
                style = MaterialTheme.typography.labelSmall,
              )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { onReplenishRation(item, 10L) },
                enabled = !isBusy && item.quantity < item.maxQuantity,
                border = BorderStroke(1.dp, Emerald),
                modifier = Modifier.heightIn(min = 48.dp).testTag("admin_replenish_${item.itemId}"),
              ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add 10", tint = Emerald, modifier = Modifier.size(16.dp))
                Text("10", color = Emerald, fontWeight = FontWeight.Bold)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                  checked = item.permitted,
                  onCheckedChange = { onToggleRationPermission(item, it) },
                  enabled = !isBusy,
                  colors = SwitchDefaults.colors(checkedThumbColor = Emerald, uncheckedThumbColor = Danger),
                  modifier = Modifier.testTag("admin_permit_${item.itemId}"),
                )
                Text(if (item.permitted) "Open" else "Locked", color = if (item.permitted) Emerald else Danger, fontSize = 10.sp)
              }
            }
          }
        }
      }
    }

    // Active & Recent Cooking Sessions
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = CardBg,
      border = BorderStroke(1.dp, Amber.copy(alpha = 0.25f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.SoupKitchen, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("COOKING SESSIONS MONITOR (${cookingSessions.size})", color = Amber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        if (cookingSessions.isEmpty()) {
          Text("No cooking sessions recorded in this season yet.", color = Muted, style = MaterialTheme.typography.bodySmall)
        } else {
          cookingSessions.take(15).forEach { ses ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ElevatedBg)
                .padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${ses.recipeName} • ${ses.status}", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                  "Chef: ${ses.chefDisplayName} • Servings: ${ses.servingsRemaining}/${ses.servingsTotal}",
                  color = Muted,
                  style = MaterialTheme.typography.labelSmall,
                )
              }
              if (ses.status == "COOKING") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  OutlinedButton(
                    onClick = { onManageCookingSession(ses, "READY") },
                    enabled = !isBusy,
                    border = BorderStroke(1.dp, Emerald),
                    modifier = Modifier.heightIn(min = 48.dp),
                  ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Force Ready", tint = Emerald, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ready", color = Emerald, fontSize = 11.sp)
                  }
                  OutlinedButton(
                    onClick = { onManageCookingSession(ses, "CANCELLED") },
                    enabled = !isBusy,
                    border = BorderStroke(1.dp, Danger),
                    modifier = Modifier.heightIn(min = 48.dp),
                  ) {
                    Icon(Icons.Rounded.Cancel, contentDescription = "Cancel", tint = Danger, modifier = Modifier.size(15.dp))
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

@Composable
fun AdminLeaderboardAndProgressTab(
  leaderboard: List<PlayerLeaderboardEntry>,
  activeTask: HouseTask?,
  taskProgressList: List<TaskPlayerProgress>,
) {
  val totalHousePts = leaderboard.sumOf { it.housePointsContribution }
  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = CardBg,
      border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f)),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Gold, modifier = Modifier.size(28.dp))
          Spacer(Modifier.width(10.dp))
          Column {
            Text("SEASON HOUSE POINTS POOL", color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
            Text("$totalHousePts Total House Points", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
          }
        }
        Text("${leaderboard.size} Ranked", color = Cyan, fontWeight = FontWeight.Bold)
      }
    }

    if (activeTask != null) {
      Surface(shape = RoundedCornerShape(18.dp), color = CardBg) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "LIVE PROGRESS: ${activeTask.title} (${taskProgressList.size} active)",
            color = Cyan,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
          if (taskProgressList.isEmpty()) {
            Text("No player checkpoints recorded yet.", color = Muted, style = MaterialTheme.typography.bodySmall)
          } else {
            taskProgressList.forEach { prog ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(ElevatedBg)
                  .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("${prog.displayName} (${prog.status})", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("${prog.percent}% • ${prog.completedSteps.size} steps", color = Emerald, fontWeight = FontWeight.ExtraBold)
              }
            }
          }
        }
      }
    }

    Surface(shape = RoundedCornerShape(18.dp), color = CardBg) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("SEASON PLAYER LEADERBOARD", color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        if (leaderboard.isEmpty()) {
          Text("Leaderboard populates automatically as housemates cook meals and complete tasks.", color = Muted, style = MaterialTheme.typography.bodySmall)
        } else {
          leaderboard.forEachIndexed { idx, entry ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (idx == 0) Gold.copy(alpha = 0.14f) else ElevatedBg)
                .padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("#${idx + 1} • ${entry.displayName} (${entry.playerId})", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                  "${entry.tasksCompleted} tasks • ${entry.mealsCooked} meals • ${entry.achievements.size} badges",
                  color = Muted,
                  style = MaterialTheme.typography.labelSmall,
                )
              }
              Text("${entry.points} pts", color = Gold, fontWeight = FontWeight.ExtraBold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminKitchenStat(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = CardBg,
    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
      Spacer(Modifier.height(2.dp))
      Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
  }
}
