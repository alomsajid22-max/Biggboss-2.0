package com.example.multiplayer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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

private val PanelCard = Color(0xFF151E33)
private val PanelElevated = Color(0xFF1C2742)
private val Gold = Color(0xFFEAB954)
private val Cyan = Color(0xFF22D3EE)
private val Emerald = Color(0xFF34D399)
private val Crimson = Color(0xFFF87171)
private val SlateMuted = Color(0xFF94A3B8)

private fun formatCountdownRemaining(endTimeMillis: Long, nowMillis: Long): String {
  if (endTimeMillis <= 0L) return "--:--"
  val remainingSec = ((endTimeMillis - nowMillis) / 1000L).coerceAtLeast(0L)
  val mins = remainingSec / 60L
  val secs = remainingSec % 60L
  return "%02d:%02d".format(mins, secs)
}

@Composable
fun AdminVotingPhoneTab(
  allUsers: List<UserAccount>,
  members: List<SeasonMember>,
  season: SeasonSession? = null,
  votingCycles: List<VotingCycle>,
  activeCycle: VotingCycle?,
  nominations: List<NominationBallot>,
  ballots: List<VotingBallot>,
  isBusy: Boolean,
  onSelectCycle: (String) -> Unit,
  onStartConfiguredCycle: (
    title: String,
    startStatus: String,
    nominees: List<NomineeSummary>,
    npcEnabled: Boolean,
    voteType: String,
    nominationsPerPlayer: Long,
    votingDurationMinutes: Long,
    resultVisibility: String,
    tieBreakerPolicy: String,
  ) -> Unit,
  onPromoteToOpenVoting: (cycle: VotingCycle, nominees: List<NomineeSummary>) -> Unit,
  onUpdateCycleState: (cycle: VotingCycle, nextStatus: String, npcEnabled: Boolean) -> Unit,
  onCalculateServerTally: (cycle: VotingCycle, closeVoting: Boolean) -> Unit = { _, _ -> },
  onCancelCycle: (cycle: VotingCycle, reason: String) -> Unit,
  onCastNpcVote: (cycleId: String, npcLabel: String, targetUid: String) -> Unit,
) {
  // Secret Admin Identity: Never allow ADMIN accounts into candidate/nomination rosters
  val candidates = remember(allUsers, members) {
    val byUid = LinkedHashMap<String, NomineeSummary>()
    members.filter {
      it.role != PlayerRole.ADMIN.name && it.status !in listOf("EVICTED", "BANNED", "REMOVED")
    }.forEach { m ->
      byUid[m.userId] = NomineeSummary(m.userId, m.playerId, m.displayName, m.characterId)
    }
    allUsers.filter { !it.banned && it.role != PlayerRole.ADMIN.name }.forEach { u ->
      if (!byUid.containsKey(u.userId)) {
        byUid[u.userId] = NomineeSummary(u.userId, u.playerId, u.displayName, u.characterId)
      }
    }
    byUid.values.toList()
  }

  val nominationTallies = remember(nominations, members) {
    Phase10VotingLogic.tallyNominations(nominations, members)
  }

  var cycleTitle by remember { mutableStateOf("Week ${votingCycles.size + 1} Elimination") }
  val pickedUids = remember { mutableStateListOf<String>() }
  var npcVotesEnabled by remember { mutableStateOf(false) }
  var voteType by remember { mutableStateOf("EVICT") }
  var nominationsPerPlayer by remember { mutableLongStateOf(2L) }
  var votingDurationMinutes by remember { mutableLongStateOf(15L) }
  var resultVisibility by remember { mutableStateOf("HIDDEN_UNTIL_CLOSED") }
  var tieBreakerPolicy by remember { mutableStateOf("ADMIN_DECIDES") }
  var cancelReason by remember { mutableStateOf("Voting round cancelled by Bigg Boss Control Room.") }

  var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(activeCycle?.id, activeCycle?.status, activeCycle?.votingEndTimeMillis) {
    while (true) {
      nowMs = System.currentTimeMillis()
      delay(1000L)
    }
  }

  val realBallots = remember(ballots) { ballots.filter { it.sourceType != "NPC" } }
  val npcBallots = remember(ballots) { ballots.filter { it.sourceType == "NPC" } }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Sleek Admin Voting Smartphone Frame
    Surface(
      shape = RoundedCornerShape(26.dp),
      color = Color(0xFF0A0F1C),
      border = BorderStroke(2.dp, Cyan.copy(alpha = 0.55f)),
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 580.dp)
        .align(Alignment.CenterHorizontally),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = Cyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "ADMIN VOTING PHONE • PHASE 10",
              color = Cyan,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 1.4.sp,
            )
          }
          Surface(shape = RoundedCornerShape(999.dp), color = PanelElevated) {
            Text(
              text = activeCycle?.status ?: "NO ACTIVE CYCLE",
              color = when (activeCycle?.status) {
                "NOMINATIONS" -> Cyan
                "OPEN" -> Emerald
                "CLOSED" -> Gold
                "EVICTED", "CANCELLED" -> Crimson
                else -> SlateMuted
              },
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }

        if (activeCycle != null) {
          Text(
            text = activeCycle.title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Rule: ${activeCycle.voteType} • Visibility: ${activeCycle.resultVisibility} • Tie: ${activeCycle.tieBreakerPolicy}",
            color = Cyan,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
          )
          if (activeCycle.status == "OPEN" && activeCycle.votingEndTimeMillis > 0L) {
            val timerStr = formatCountdownRemaining(activeCycle.votingEndTimeMillis, nowMs)
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.Timer, contentDescription = null, tint = Gold, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (nowMs >= activeCycle.votingEndTimeMillis) "Voting Timer Expired (00:00)"
                else "Voting Time Left: $timerStr",
                color = Gold,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("admin_voting_countdown"),
              )
            }
          }
          if (activeCycle.status == "CANCELLED" && !activeCycle.cancelledReason.isNullOrBlank()) {
            Text(
              text = "Cancelled Reason: ${activeCycle.cancelledReason}",
              color = Crimson,
              style = MaterialTheme.typography.bodySmall,
            )
          }

          Text(
            text = "Nominations Submitted: ${nominations.size} • Real Votes: ${realBallots.size} • NPC Votes: ${npcBallots.size}",
            color = SlateMuted,
            style = MaterialTheme.typography.bodySmall,
          )

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (activeCycle.status == "NOMINATIONS") {
              val topTallyCandidates = nominationTallies
                .filter { it.nominationCount > 0 }
                .take(5)
                .map { NomineeSummary(it.uid, it.playerId, it.displayName, it.characterId) }
              val fallbackPicked = candidates.filter { pickedUids.contains(it.uid) }
              val nomineesToPromote = if (fallbackPicked.isNotEmpty()) fallbackPicked else topTallyCandidates
              Button(
                onClick = { onPromoteToOpenVoting(activeCycle, nomineesToPromote) },
                enabled = !isBusy && nomineesToPromote.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color(0xFF06281E)),
                modifier = Modifier.height(48.dp).testTag("promote_nominations_open_btn"),
              ) {
                Icon(Icons.Rounded.HowToVote, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Promote (${nomineesToPromote.size}) & Open Vote", fontWeight = FontWeight.Bold)
              }
            } else if (activeCycle.status != "OPEN" && activeCycle.status != "EVICTED" && activeCycle.status != "CANCELLED") {
              Button(
                onClick = { onUpdateCycleState(activeCycle, "OPEN", activeCycle.npcVotesEnabled) },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color(0xFF06281E)),
                modifier = Modifier.height(48.dp).testTag("open_voting_button"),
              ) {
                Icon(Icons.Rounded.HowToVote, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Voting", fontWeight = FontWeight.Bold)
              }
            }
            if (activeCycle.status == "OPEN") {
              Button(
                onClick = { onUpdateCycleState(activeCycle, "CLOSED", activeCycle.npcVotesEnabled) },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF10141F)),
                modifier = Modifier.height(48.dp).testTag("close_voting_button"),
              ) {
                Text("Close Voting", fontWeight = FontWeight.Bold)
              }
              OutlinedButton(
                onClick = { onCalculateServerTally(activeCycle, true) },
                enabled = !isBusy,
                border = BorderStroke(1.dp, Cyan),
                modifier = Modifier.height(48.dp).testTag("calculate_server_tally_button"),
              ) {
                Text("Calculate & Close (Server)", color = Cyan, fontWeight = FontWeight.Bold)
              }
            } else if (activeCycle.status == "CLOSED") {
              OutlinedButton(
                onClick = { onCalculateServerTally(activeCycle, false) },
                enabled = !isBusy,
                border = BorderStroke(1.dp, Cyan),
                modifier = Modifier.height(48.dp).testTag("calculate_server_tally_button"),
              ) {
                Text("Recalculate Tally", color = Cyan, fontWeight = FontWeight.Bold)
              }
            }
            if (activeCycle.status in listOf("NOMINATIONS", "OPEN", "CLOSED")) {
              OutlinedButton(
                onClick = { onCancelCycle(activeCycle, cancelReason) },
                enabled = !isBusy,
                border = BorderStroke(1.dp, Crimson),
                modifier = Modifier.height(48.dp).testTag("cancel_voting_button"),
              ) {
                Icon(Icons.Rounded.Cancel, contentDescription = null, tint = Crimson, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cancel Round", color = Crimson, fontWeight = FontWeight.Bold)
              }
            }
          }

          if (activeCycle.status in listOf("NOMINATIONS", "OPEN", "CLOSED")) {
            OutlinedTextField(
              value = cancelReason,
              onValueChange = { if (it.length <= 200) cancelReason = it },
              label = { Text("Cancellation Reason (if cancelling)") },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Crimson,
                unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
              ),
              modifier = Modifier.fillMaxWidth().testTag("cancel_reason_input"),
            )
          }

          if (activeCycle.status == "NOMINATIONS") {
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Text("LIVE PEER NOMINATION COUNTS", color = Cyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            if (nominationTallies.isEmpty()) {
              Text("Waiting for eligible players to join.", color = SlateMuted, style = MaterialTheme.typography.bodySmall)
            } else {
              nominationTallies.forEach { entry ->
                Row(
                  modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PanelElevated).padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("${entry.displayName} (${entry.playerId})", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text(
                      if (entry.nominatedByNames.isEmpty()) "No nominations yet" else "By: ${entry.nominatedByNames.joinToString(", ")}",
                      color = SlateMuted,
                      style = MaterialTheme.typography.labelSmall,
                    )
                  }
                  Text("${entry.nominationCount} nom(s)", color = Cyan, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium)
                }
              }
            }
          }

          if (activeCycle.parsedNominees().isNotEmpty()) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            activeCycle.parsedNominees().forEach { nominee ->
              val realCount = realBallots.count { it.targetUid == nominee.uid }
              val npcCount = npcBallots.count { it.targetUid == nominee.uid }
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = PanelElevated,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    val isImmuneCap = Phase11VipLogic.isProtectedByCaptainImmunity(nominee.uid, season)
                    Text(
                      text = if (isImmuneCap) "${nominee.displayName} 🛡️ IMMUNE CAPTAIN" else nominee.displayName,
                      color = if (isImmuneCap) Emerald else Color.White,
                      fontWeight = FontWeight.Bold,
                    )
                    Text("${nominee.playerId} • ${nominee.characterId}", color = SlateMuted, style = MaterialTheme.typography.labelSmall)
                    Text(
                      "Real: $realCount  |  NPC: $npcCount  |  Total: ${realCount + npcCount}",
                      color = Gold,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold,
                    )
                  }
                  if (activeCycle.status == "OPEN" && activeCycle.npcVotesEnabled) {
                    OutlinedButton(
                      onClick = {
                        onCastNpcVote(activeCycle.id, "NPC-${npcBallots.size + 1}", nominee.uid)
                      },
                      enabled = !isBusy,
                      border = BorderStroke(1.dp, Cyan.copy(alpha = 0.5f)),
                      modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                      Text("+1 NPC Vote", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }

          if (ballots.isNotEmpty()) {
            Text("STORED BALLOT LEDGER", color = Cyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            ballots.take(8).forEach { b ->
              val targetName = activeCycle.parsedNominees().firstOrNull { it.uid == b.targetUid }?.displayName ?: b.targetUid
              Text(
                text = "• ${b.voterName ?: b.voterUid} -> $targetName [${b.sourceType}]",
                color = SlateMuted,
                style = MaterialTheme.typography.labelSmall,
              )
            }
          }
        }
      }
    }

    // Create / Configure Nominations & Voting Card
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = PanelCard,
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          "CONFIGURE NOMINATION & VOTING ROUND",
          color = Gold,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
          value = cycleTitle,
          onValueChange = { if (it.length <= 80) cycleTitle = it },
          label = { Text("Nomination / Voting Round Title") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
          ),
          modifier = Modifier.fillMaxWidth().testTag("voting_cycle_title_input"),
        )

        // Vote Rule: EVICT vs SAVE
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("EVICT" to "Vote to EVICT", "SAVE" to "Vote to SAVE").forEach { (rule, label) ->
            val picked = voteType == rule
            OutlinedButton(
              onClick = { voteType = rule },
              border = BorderStroke(1.dp, if (picked) Gold else Color.White.copy(alpha = 0.16f)),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("vote_type_$rule"),
            ) {
              Text(label, color = if (picked) Gold else SlateMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        // Result Visibility Policy
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(
            "HIDDEN_UNTIL_CLOSED" to "Hidden Until Closed",
            "LIVE_TOTALS" to "Live Totals",
            "ADMIN_ONLY" to "Admin Only",
          ).forEach { (vis, label) ->
            val picked = resultVisibility == vis
            OutlinedButton(
              onClick = { resultVisibility = vis },
              border = BorderStroke(1.dp, if (picked) Cyan else Color.White.copy(alpha = 0.16f)),
              modifier = Modifier.heightIn(min = 48.dp).testTag("visibility_$vis"),
            ) {
              Text(label, color = if (picked) Cyan else SlateMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        // Nominations per player & Duration
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(1L, 2L, 3L).forEach { cnt ->
            val picked = nominationsPerPlayer == cnt
            OutlinedButton(
              onClick = { nominationsPerPlayer = cnt },
              border = BorderStroke(1.dp, if (picked) Cyan else Color.White.copy(alpha = 0.16f)),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
              Text("$cnt Nom/Player", color = if (picked) Cyan else SlateMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(5L, 15L, 30L, 60L).forEach { mins ->
            val picked = votingDurationMinutes == mins
            OutlinedButton(
              onClick = { votingDurationMinutes = mins },
              border = BorderStroke(1.dp, if (picked) Gold else Color.White.copy(alpha = 0.16f)),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
              Text("${mins}m Timer", color = if (picked) Gold else SlateMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        // Tie breaker policy
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(
            "ADMIN_DECIDES" to "Tie: Admin Decides",
            "CAPTAIN_DECIDES" to "Tie: Captain Decides",
            "RANDOM_DRAW" to "Tie: Random Draw",
            "NO_EVICTION" to "Tie: No Eviction",
          ).forEach { (policy, label) ->
            val picked = tieBreakerPolicy == policy
            OutlinedButton(
              onClick = { tieBreakerPolicy = policy },
              border = BorderStroke(1.dp, if (picked) Emerald else Color.White.copy(alpha = 0.16f)),
              modifier = Modifier.heightIn(min = 48.dp),
            ) {
              Text(label, color = if (picked) Emerald else SlateMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            "Allow Optional NPC Votes (Strictly Labeled NPC)",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
          )
          Switch(checked = npcVotesEnabled, onCheckedChange = { npcVotesEnabled = it })
        }

        if (candidates.isEmpty()) {
          Text("No registered active housemates available to nominate yet.", color = SlateMuted, style = MaterialTheme.typography.bodySmall)
        } else {
          candidates.forEach { cand ->
            val checked = pickedUids.contains(cand.uid)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (checked) Gold.copy(alpha = 0.14f) else PanelElevated)
                .clickable {
                  if (checked) pickedUids.remove(cand.uid) else pickedUids.add(cand.uid)
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Checkbox(
                checked = checked,
                onCheckedChange = { isChecked ->
                  if (isChecked) {
                    if (!pickedUids.contains(cand.uid)) pickedUids.add(cand.uid)
                  } else {
                    pickedUids.remove(cand.uid)
                  }
                },
                colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = Color(0xFF10141F)),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "${cand.displayName} (${cand.playerId} • ${cand.characterId})",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }

        val chosenNominees = candidates.filter { pickedUids.contains(it.uid) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              onStartConfiguredCycle(
                cycleTitle,
                "NOMINATIONS",
                chosenNominees,
                npcVotesEnabled,
                voteType,
                nominationsPerPlayer,
                votingDurationMinutes,
                resultVisibility,
                tieBreakerPolicy,
              )
              pickedUids.clear()
            },
            enabled = cycleTitle.trim().length >= 3 && !isBusy,
            border = BorderStroke(1.dp, Cyan),
            modifier = Modifier.weight(1f).height(48.dp).testTag("start_nominations_round_btn"),
          ) {
            Text("Start Peer Nominations", color = Cyan, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          Button(
            onClick = {
              onStartConfiguredCycle(
                cycleTitle,
                "OPEN",
                chosenNominees,
                npcVotesEnabled,
                voteType,
                nominationsPerPlayer,
                votingDurationMinutes,
                resultVisibility,
                tieBreakerPolicy,
              )
              pickedUids.clear()
            },
            enabled = chosenNominees.isNotEmpty() && cycleTitle.trim().length >= 3 && !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF10141F)),
            modifier = Modifier.weight(1f).height(48.dp).testTag("publish_open_voting_btn"),
          ) {
            Text("Open Voting Now", fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }

    if (votingCycles.isNotEmpty()) {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = PanelCard,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.History, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("VOTING HISTORY", color = SlateMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          }
          votingCycles.forEach { cycle ->
            val selected = cycle.id == activeCycle?.id
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) Cyan.copy(alpha = 0.14f) else PanelElevated)
                .clickable { onSelectCycle(cycle.id) }
                .padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(cycle.title, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text("${cycle.nominatedUids.size} nominees • ${cycle.voteType} • ${cycle.status}", color = SlateMuted, style = MaterialTheme.typography.labelSmall)
              }
              if (cycle.evictedDisplayName != null) {
                Text("Evicted: ${cycle.evictedDisplayName}", color = Crimson, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun AdminEvictionsTab(
  season: SeasonSession? = null,
  activeCycle: VotingCycle?,
  ballots: List<VotingBallot>,
  tallySummary: VotingTallyResult?,
  evictions: List<EvictionRecord>,
  isBusy: Boolean,
  onCalculateServerTally: (cycle: VotingCycle, closeVoting: Boolean) -> Unit = { _, _ -> },
  onConfirmEviction: (
    cycle: VotingCycle,
    nominee: NomineeSummary,
    realVotes: Long,
    npcVotes: Long,
    announcement: String,
    languageTag: String,
    voiceGender: VoiceGender,
  ) -> Unit,
  onCancelDueToTie: (cycle: VotingCycle, reason: String) -> Unit,
) {
  val weightedOutcome = remember(activeCycle, ballots, season) {
    activeCycle?.let { Phase11VipLogic.calculateWeightedVotingTally(it, ballots, season) }
  }
  val effectiveTally = remember(weightedOutcome, tallySummary, activeCycle, ballots) {
    weightedOutcome?.baseResult ?: tallySummary ?: activeCycle?.let { Phase10VotingLogic.calculateVotingTally(it, ballots) }
  }
  val rankedTallies = effectiveTally?.rankedTallies.orEmpty()
  val nominees = remember(rankedTallies) { rankedTallies.map { it.nominee } }
  val suggestedEvicteeUid = effectiveTally?.leadingEvictionCandidates?.firstOrNull()?.nominee?.uid ?: nominees.firstOrNull()?.uid

  var selectedEvicteeUid by remember(activeCycle?.id, suggestedEvicteeUid) {
    mutableStateOf(suggestedEvicteeUid)
  }
  val chosenTally = rankedTallies.firstOrNull { it.nominee.uid == selectedEvicteeUid } ?: rankedTallies.firstOrNull()
  val chosenNominee = chosenTally?.nominee

  var evictionMessage by remember(chosenNominee?.uid, chosenTally?.totalVotes, activeCycle?.voteType) {
    mutableStateOf(
      if (chosenNominee != null && activeCycle != null && chosenTally != null) {
        Phase10VotingLogic.formatHindiEvictionAnnouncement(
          displayName = chosenNominee.displayName,
          voteType = activeCycle.voteType,
          totalVotes = chosenTally.totalVotes,
        )
      } else ""
    )
  }

  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = PanelCard,
      border = BorderStroke(1.dp, Crimson.copy(alpha = 0.35f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.Gavel, contentDescription = null, tint = Crimson, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("CONFIRM EVICTION • PHASE 10", color = Crimson, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
        }

        if (activeCycle == null || nominees.isEmpty()) {
          Text(
            "Nominate housemates in the Voting Phone tab before confirming an eviction.",
            color = SlateMuted,
            style = MaterialTheme.typography.bodySmall,
          )
        } else if (activeCycle.status == "EVICTED") {
          Text(
            "Cycle \"${activeCycle.title}\" already evicted ${activeCycle.evictedDisplayName ?: "a housemate"}.",
            color = Gold,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
          )
        } else if (activeCycle.status == "CANCELLED") {
          Text(
            "Cycle \"${activeCycle.title}\" was CANCELLED (${activeCycle.cancelledReason ?: "No eviction"}).",
            color = Crimson,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
          )
        } else {
          Text(
            "Cycle: ${activeCycle.title} (${activeCycle.status} • Rule: ${activeCycle.voteType} • Tie Policy: ${activeCycle.tieBreakerPolicy})",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
          )

          if (activeCycle.serverTalliedAtMillis > 0L) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Cyan.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, Cyan.copy(alpha = 0.5f)),
            ) {
              Text(
                text = "SERVER VERIFIED TALLY • Real: ${activeCycle.serverTotalRealVotes} • NPC: ${activeCycle.serverTotalNpcVotes}${if (activeCycle.serverIsTie) " • TIE" else ""}",
                color = Cyan,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          } else {
            OutlinedButton(
              onClick = { onCalculateServerTally(activeCycle, activeCycle.status == "OPEN") },
              enabled = !isBusy,
              border = BorderStroke(1.dp, Cyan),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("evictions_server_tally_btn"),
            ) {
              Text("Verify & Calculate Official Server Tally", color = Cyan, fontWeight = FontWeight.Bold)
            }
          }

          if (effectiveTally?.isTie == true) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Gold.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, Gold),
              modifier = Modifier.testTag("tie_detected_banner"),
            ) {
              Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("TIE DETECTED (${effectiveTally.leadingEvictionCandidates.size} Nominees Tied)", color = Gold, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium)
                }
                Text(
                  text = "Tied: ${effectiveTally.leadingEvictionCandidates.joinToString(", ") { it.nominee.displayName }}. Select one nominee below or cancel with No Eviction.",
                  color = Color.White,
                  style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(
                  onClick = { onCancelDueToTie(activeCycle, "Eviction round closed with tie — No Eviction applied.") },
                  enabled = !isBusy,
                  border = BorderStroke(1.dp, Gold),
                  modifier = Modifier.heightIn(min = 48.dp).testTag("cancel_tie_round_button"),
                ) {
                  Text("Cancel Round (No Eviction)", color = Gold, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          rankedTallies.forEach { tally ->
            val n = tally.nominee
            val isPicked = n.uid == chosenNominee?.uid
            val isImmuneCaptain = Phase11VipLogic.isProtectedByCaptainImmunity(n.uid, season)
            val isTiedCandidate = effectiveTally?.isTie == true &&
              effectiveTally.leadingEvictionCandidates.any { it.nominee.uid == n.uid }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isPicked) Crimson.copy(alpha = 0.16f) else PanelElevated)
                .border(1.dp, if (isImmuneCaptain) Emerald else if (isPicked) Crimson else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .clickable { selectedEvicteeUid = n.uid }
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(n.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                  if (isImmuneCaptain) {
                    Text("🛡️ CAPTAIN IMMUNE", color = Emerald, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                  }
                  if (isTiedCandidate) {
                    Text("TIED", color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                  }
                }
                Text("${n.playerId} • ${n.characterId}", color = SlateMuted, style = MaterialTheme.typography.labelSmall)
              }
              Text(
                "Real: ${tally.realVotes} • NPC: ${tally.npcVotes} • Total: ${tally.totalVotes}",
                color = Gold,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          OutlinedTextField(
            value = evictionMessage,
            onValueChange = { if (it.length <= 300) evictionMessage = it },
            label = { Text("Eviction Announcement (hi-IN Broadcast)") },
            minLines = 2,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Crimson,
              unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth().testTag("eviction_announcement_input"),
          )

          if (chosenNominee != null && chosenTally != null) {
            Button(
              onClick = {
                onConfirmEviction(
                  activeCycle,
                  chosenNominee,
                  chosenTally.realVotes.toLong(),
                  chosenTally.npcVotes.toLong(),
                  evictionMessage,
                  "hi-IN",
                  VoiceGender.FEMALE,
                )
              },
              enabled = !isBusy,
              colors = ButtonDefaults.buttonColors(containerColor = Crimson, contentColor = Color(0xFF1F0808)),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("confirm_eviction_button"),
            ) {
              Text("Confirm & Announce Eviction of ${chosenNominee.displayName}", fontWeight = FontWeight.ExtraBold)
            }
          }
        }
      }
    }

    Surface(
      shape = RoundedCornerShape(18.dp),
      color = PanelCard,
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("HISTORICAL EVICTION ARCHIVE", color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        if (evictions.isEmpty()) {
          Text("No evictions recorded in this season yet.", color = SlateMuted, style = MaterialTheme.typography.bodySmall)
        } else {
          evictions.forEach { rec ->
            Surface(shape = RoundedCornerShape(12.dp), color = PanelElevated) {
              Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Text("${rec.displayName} (${rec.playerId})", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Real Votes: ${rec.realVotes} • NPC Votes: ${rec.npcVotes}", color = Gold, style = MaterialTheme.typography.labelSmall)
                Text(rec.announcementText, color = SlateMuted, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun AdminAnnouncementsTab(
  ttsStatus: TtsStatusSnapshot,
  announcements: List<AdminAnnouncement>,
  isBusy: Boolean,
  onPreviewSpeech: (message: String, languageTag: String, gender: VoiceGender) -> Unit,
  onBroadcastAll: (message: String, presetId: String?, languageTag: String, gender: VoiceGender) -> Unit,
) {
  var selectedLang by remember { mutableStateOf("hi-IN") }
  var selectedGender by remember { mutableStateOf(VoiceGender.FEMALE) }
  var selectedPresetId by remember { mutableStateOf<String?>(AnnouncementPresets.all.first().id) }
  var message by remember { mutableStateOf(AnnouncementPresets.all.first().hindiText) }

  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = PanelCard,
      border = BorderStroke(1.dp, Gold.copy(alpha = 0.35f)),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.Campaign, contentDescription = null, tint = Gold, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text("BIGG BOSS ANNOUNCEMENT CONTROL", color = Gold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
            Text(ttsStatus.statusMessage, color = SlateMuted, style = MaterialTheme.typography.labelSmall)
          }
        }

        // Language & Gender selectors
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("hi-IN" to "Indian Hindi (hi-IN)", "en-IN" to "Indian English (en-IN)").forEach { (tag, label) ->
            val active = selectedLang == tag
            OutlinedButton(
              onClick = { selectedLang = tag },
              border = BorderStroke(1.dp, if (active) Cyan else Color.White.copy(alpha = 0.16f)),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
              Text(label, color = if (active) Cyan else SlateMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          VoiceGender.entries.forEach { gender ->
            val active = selectedGender == gender
            OutlinedButton(
              onClick = { selectedGender = gender },
              border = BorderStroke(1.dp, if (active) Gold else Color.White.copy(alpha = 0.16f)),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
              Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = if (active) Gold else SlateMuted, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("${gender.name} VOICE", color = if (active) Gold else SlateMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        Text("PRESET ANNOUNCEMENTS", color = Cyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        AnnouncementPresets.all.forEach { preset ->
          val picked = selectedPresetId == preset.id
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (picked) Cyan.copy(alpha = 0.14f) else PanelElevated,
            border = BorderStroke(1.dp, if (picked) Cyan else Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth().clickable {
              selectedPresetId = preset.id
              message = preset.hindiText
            },
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(preset.title, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
              Text(preset.hindiText, color = SlateMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
          }
        }

        OutlinedTextField(
          value = message,
          onValueChange = {
            if (it.length <= 320) {
              message = it
              selectedPresetId = null
            }
          },
          label = { Text("Custom Announcement Message") },
          minLines = 2,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
          ),
          modifier = Modifier.fillMaxWidth().testTag("announcement_input"),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = { onPreviewSpeech(message, selectedLang, selectedGender) },
            enabled = message.isNotBlank(),
            border = BorderStroke(1.dp, Cyan),
            modifier = Modifier.weight(0.42f).height(50.dp).testTag("preview_tts_button"),
          ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Cyan)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Preview", color = Cyan, fontWeight = FontWeight.Bold)
          }
          Button(
            onClick = { onBroadcastAll(message, selectedPresetId, selectedLang, selectedGender) },
            enabled = message.trim().length >= 2 && !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF10141F)),
            modifier = Modifier.weight(0.58f).height(50.dp).testTag("broadcast_all_button"),
          ) {
            Icon(Icons.Rounded.Campaign, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Announce to All Players", fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }

    if (announcements.isNotEmpty()) {
      Surface(shape = RoundedCornerShape(18.dp), color = PanelCard) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("RECENT BROADCASTS", color = SlateMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          announcements.take(6).forEach { ann ->
            Text("• [${ann.languageTag}/${ann.voiceGender}] ${ann.message}", color = Color.White, style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
  }
}

@Composable
fun HouseVotingBallotDialog(
  cycle: VotingCycle,
  myBallot: VotingBallot?,
  ballots: List<VotingBallot> = emptyList(),
  isAdminVerified: Boolean = false,
  isBusy: Boolean,
  onCastVote: (targetUid: String) -> Unit,
  onDismiss: () -> Unit,
  npcOnlyVoting: Boolean = false,
) {
  var pickedTargetUid by remember(cycle.id) { mutableStateOf(myBallot?.targetUid) }
  val nominees = remember(cycle) { cycle.parsedNominees() }
  val canShowTotals = remember(cycle, isAdminVerified) {
    Phase10VotingLogic.canPlayerViewTotals(cycle, isAdminVerified)
  }
  val tally = remember(cycle, ballots, canShowTotals) {
    if (canShowTotals) Phase10VotingLogic.calculateVotingTally(cycle, ballots) else null
  }
  var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(cycle.id, cycle.status, cycle.votingEndTimeMillis) {
    while (true) {
      nowMs = System.currentTimeMillis()
      delay(1000L)
    }
  }
  val isSaveRule = cycle.voteType.equals("SAVE", ignoreCase = true)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = PanelCard,
      border = BorderStroke(1.5.dp, Gold.copy(alpha = 0.5f)),
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .widthIn(max = 520.dp)
        .testTag("house_voting_ballot_dialog"),
    ) {
      Column(
        modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.HowToVote, contentDescription = null, tint = Gold)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "HOUSE VOTING • ${if (isSaveRule) "VOTE TO SAVE" else "VOTE TO EVICT"}",
                color = Gold,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(cycle.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = "Close Ballot", tint = SlateMuted)
          }
        }

        if (cycle.status == "OPEN" && cycle.votingEndTimeMillis > 0L) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (nowMs >= cycle.votingEndTimeMillis) "Voting window closing soon (00:00)"
              else "Time remaining: ${formatCountdownRemaining(cycle.votingEndTimeMillis, nowMs)}",
              color = Cyan,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag("ballot_countdown_label"),
            )
          }
        }

        if (cycle.status == "CANCELLED") {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Crimson.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, Crimson),
            modifier = Modifier.testTag("ballot_cancelled_banner"),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
              Text("Voting Round Cancelled", color = Crimson, fontWeight = FontWeight.ExtraBold)
              Text(
                text = cycle.cancelledReason ?: "No eviction occurred in this round.",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        } else if (npcOnlyVoting && !isAdminVerified) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Cyan.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, Cyan),
            modifier = Modifier.testTag("npc_only_voting_notice"),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("BIGGGRAM NPC VOTING ACTIVE", color = Cyan, fontWeight = FontWeight.ExtraBold)
              Text(
                text = "Final nominees have been published on BiggGram. External NPCs vote exclusively via BiggGram — housemates nominate peers but do not vote in public eliminations.",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
          nominees.forEach { n ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelElevated)
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(n.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${n.playerId} • Nominated on BiggGram", color = SlateMuted, style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        } else if (myBallot != null) {
          val votedFor = nominees.firstOrNull { it.uid == myBallot.targetUid }?.displayName ?: "Nominated Housemate"
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Emerald.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, Emerald),
            modifier = Modifier.testTag("ballot_recorded_banner"),
          ) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Emerald)
              Spacer(modifier = Modifier.width(10.dp))
              Text("Your 1-time verified ballot for $votedFor has been recorded.", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
          }
        } else if (cycle.status == "OPEN") {
          Text(
            text = if (isSaveRule) "Select one nominated housemate you want to SAVE:"
            else "Select one nominated housemate you want to EVICT:",
            color = SlateMuted,
            style = MaterialTheme.typography.bodySmall,
          )
          nominees.forEach { n ->
            val selected = pickedTargetUid == n.uid
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) Gold.copy(alpha = 0.16f) else PanelElevated)
                .border(1.dp, if (selected) Gold else Color.White.copy(alpha = 0.09f), RoundedCornerShape(12.dp))
                .clickable { pickedTargetUid = n.uid }
                .padding(12.dp)
                .testTag("ballot_nominee_${n.uid}"),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (selected) Gold else Color.White.copy(alpha = 0.25f)))
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(n.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${n.playerId} • ${n.characterId}", color = SlateMuted, style = MaterialTheme.typography.labelSmall)
              }
            }
          }
          Button(
            onClick = { pickedTargetUid?.let { onCastVote(it) } },
            enabled = pickedTargetUid != null && !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF10141F)),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("cast_ballot_button"),
          ) {
            Text("Submit Verified Ballot", fontWeight = FontWeight.ExtraBold)
          }
        }

        // Result Visibility Enforcement
        if (canShowTotals && tally != null) {
          HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
          Text("OFFICIAL VOTE TOTALS (${cycle.resultVisibility})", color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          tally.rankedTallies.forEach { row ->
            Row(
              modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PanelElevated).padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(row.nominee.displayName, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
              Text(
                text = "Real ${row.realVotes} • NPC ${row.npcVotes} • Total ${row.totalVotes}",
                color = Cyan,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        } else {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = PanelElevated,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.testTag("ballot_results_hidden_notice"),
          ) {
            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.Lock, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = when (cycle.resultVisibility) {
                  "ADMIN_ONLY" -> "Results are private to the Admin Control Room."
                  else -> "Vote totals stay hidden until Admin closes this voting round."
                },
                color = SlateMuted,
                style = MaterialTheme.typography.labelSmall,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun HouseNominationDialog(
  cycle: VotingCycle,
  currentUserId: String,
  members: List<SeasonMember>,
  nominations: List<NominationBallot>,
  myNomination: NominationBallot?,
  isBusy: Boolean,
  onSubmitNomination: (targets: List<NomineeSummary>, reason: String?) -> Unit,
  onDismiss: () -> Unit,
) {
  val maxPicks = cycle.nominationsPerPlayer.toInt().coerceIn(1, 3)
  val eligibleTargets = remember(members, currentUserId) {
    members.filter {
      it.role != PlayerRole.ADMIN.name &&
        it.userId != currentUserId &&
        it.status !in listOf("EVICTED", "BANNED", "REMOVED")
    }.map { NomineeSummary(it.userId, it.playerId, it.displayName, it.characterId) }
  }
  val pickedUids = remember(cycle.id) { mutableStateListOf<String>() }
  var reason by remember(cycle.id) { mutableStateOf("") }
  val liveTallies = remember(nominations, members) {
    Phase10VotingLogic.tallyNominations(nominations, members)
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = PanelCard,
      border = BorderStroke(1.5.dp, Cyan.copy(alpha = 0.55f)),
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .widthIn(max = 540.dp)
        .testTag("house_nomination_dialog"),
    ) {
      Column(
        modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = Cyan)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("CONFESSION NOMINATIONS • MAX $maxPicks", color = Cyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
              Text(cycle.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = "Close Nominations", tint = SlateMuted)
          }
        }

        if (myNomination != null) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Emerald.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, Emerald),
            modifier = Modifier.testTag("nomination_recorded_banner"),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
              Text("Nomination Recorded (Duplicate Blocked)", color = Emerald, fontWeight = FontWeight.ExtraBold)
              Text(
                text = "You nominated: ${myNomination.targetNames.joinToString(", ")}",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        } else {
          Text(
            text = "Select 1 to $maxPicks housemate(s) to nominate (Admin is never shown; you cannot self-nominate):",
            color = SlateMuted,
            style = MaterialTheme.typography.bodySmall,
          )
          if (eligibleTargets.isEmpty()) {
            Text("No other eligible active housemates available right now.", color = SlateMuted, style = MaterialTheme.typography.bodySmall)
          } else {
            eligibleTargets.forEach { target ->
              val picked = pickedUids.contains(target.uid)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(if (picked) Cyan.copy(alpha = 0.16f) else PanelElevated)
                  .border(1.dp, if (picked) Cyan else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                  .clickable {
                    if (picked) {
                      pickedUids.remove(target.uid)
                    } else if (pickedUids.size < maxPicks) {
                      pickedUids.add(target.uid)
                    }
                  }
                  .padding(10.dp)
                  .testTag("nominate_target_${target.uid}"),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Checkbox(
                  checked = picked,
                  onCheckedChange = { checked ->
                    if (checked) {
                      if (!pickedUids.contains(target.uid) && pickedUids.size < maxPicks) pickedUids.add(target.uid)
                    } else {
                      pickedUids.remove(target.uid)
                    }
                  },
                  colors = CheckboxDefaults.colors(checkedColor = Cyan, checkmarkColor = Color(0xFF061622)),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(target.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                  Text("${target.playerId} • ${target.characterId}", color = SlateMuted, style = MaterialTheme.typography.labelSmall)
                }
              }
            }
            OutlinedTextField(
              value = reason,
              onValueChange = { if (it.length <= 200) reason = it },
              label = { Text("Nomination Reason (Optional)") },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Cyan,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
              ),
              modifier = Modifier.fillMaxWidth().testTag("nomination_reason_input"),
            )
            val chosenTargets = eligibleTargets.filter { pickedUids.contains(it.uid) }
            Button(
              onClick = { onSubmitNomination(chosenTargets, reason.takeIf { it.isNotBlank() }) },
              enabled = chosenTargets.isNotEmpty() && !isBusy,
              colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color(0xFF061622)),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("submit_nomination_button"),
            ) {
              Text("Submit ${chosenTargets.size} Nomination(s)", fontWeight = FontWeight.ExtraBold)
            }
          }
        }

        if (liveTallies.isNotEmpty()) {
          HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
          Text("LIVE NOMINATION STATUS (${nominations.size} Cast)", color = SlateMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          liveTallies.take(8).forEach { row ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(row.displayName, color = Color.White, style = MaterialTheme.typography.bodySmall)
              Text("${row.nominationCount} nom(s)", color = Cyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
fun LiveAnnouncementBanner(
  announcement: AdminAnnouncement,
  onReplayAudio: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = Color(0xF011182C),
    border = BorderStroke(1.5.dp, Gold),
    shadowElevation = 14.dp,
    modifier = modifier.widthIn(max = 620.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(Icons.Rounded.Campaign, contentDescription = "Bigg Boss Announcement", tint = Gold, modifier = Modifier.size(24.dp))
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "BIGG BOSS ANNOUNCEMENT • ${announcement.languageTag}",
          color = Gold,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.ExtraBold,
        )
        Text(
          text = announcement.message,
          color = Color.White,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
        )
      }
      IconButton(onClick = onReplayAudio) {
        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = "Replay Announcement Audio", tint = Cyan)
      }
      TextButton(onClick = onDismiss) {
        Text("OK", color = Gold, fontWeight = FontWeight.Bold)
      }
    }
  }
}
