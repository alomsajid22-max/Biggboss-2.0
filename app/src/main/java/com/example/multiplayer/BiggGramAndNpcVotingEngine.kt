package com.example.multiplayer

import java.util.UUID

object BiggGramAndNpcVotingEngine {
  val FORBIDDEN_CCTV_ZONES = setOf("bathroom", "washroom", "changing", "smoking", "private")

  val ALL_24_CCTV_CAMERAS: List<CctvCameraMeta> = listOf(
    // Living Room (4 cameras)
    CctvCameraMeta("cam_living_1", "CAM 01", "Living Atrium NW", "Living Room", "living", true),
    CctvCameraMeta("cam_living_2", "CAM 02", "Living Atrium NE", "Living Room", "living", true),
    CctvCameraMeta("cam_living_3", "CAM 03", "Living Vine Tree Core", "Living Room", "living", true),
    CctvCameraMeta("cam_living_4", "CAM 04", "Living Main Sofa Lounge", "Living Room", "living", true),
    // Kitchen (4 cameras)
    CctvCameraMeta("cam_kitchen_1", "CAM 05", "Kitchen Island South", "Kitchen", "kitchen", true),
    CctvCameraMeta("cam_kitchen_2", "CAM 06", "Kitchen Stove & Prep", "Kitchen", "kitchen", true),
    CctvCameraMeta("cam_kitchen_3", "CAM 07", "Kitchen Pantry Door", "Kitchen", "kitchen", true),
    CctvCameraMeta("cam_kitchen_4", "CAM 08", "Kitchen Sink Corner", "Kitchen", "kitchen", true),
    // Dining Area (3 cameras)
    CctvCameraMeta("cam_dining_1", "CAM 09", "Dining Table Overview", "Dining", "dining", true),
    CctvCameraMeta("cam_dining_2", "CAM 10", "Dining Breakfast Bar", "Dining", "dining", true),
    CctvCameraMeta("cam_dining_3", "CAM 11", "Dining Sideboard View", "Dining", "dining", true),
    // Bedroom Common Floor (3 cameras — private bathroom/changing strictly excluded)
    CctvCameraMeta("cam_bedroom_1", "CAM 12", "Bedroom East Wing", "Bedroom", "bedroom", true),
    CctvCameraMeta("cam_bedroom_2", "CAM 13", "Bedroom West Wing", "Bedroom", "bedroom", true),
    CctvCameraMeta("cam_bedroom_3", "CAM 14", "Bedroom Center Aisle", "Bedroom", "bedroom", true),
    // Botanical Garden & Courtyard (4 cameras)
    CctvCameraMeta("cam_garden_1", "CAM 15", "Botanical Garden Entry", "Garden", "garden", true),
    CctvCameraMeta("cam_garden_2", "CAM 16", "Garden Pergola Deck", "Garden", "garden", true),
    CctvCameraMeta("cam_garden_3", "CAM 17", "Garden Fountain Path", "Garden", "garden", true),
    CctvCameraMeta("cam_garden_4", "CAM 18", "Poolside Walkway", "Garden", "pool", true),
    // Activity Room (3 cameras)
    CctvCameraMeta("cam_activity_1", "CAM 19", "Activity Arena Center", "Activity", "activity", true),
    CctvCameraMeta("cam_activity_2", "CAM 20", "Activity Podium Stage", "Activity", "activity", true),
    CctvCameraMeta("cam_activity_3", "CAM 21", "Activity North Entry", "Activity", "activity", true),
    // Hall / VIP Gallery Approach (3 cameras)
    CctvCameraMeta("cam_hall_1", "CAM 22", "Main Hall Corridor", "Hall", "gallery", true),
    CctvCameraMeta("cam_hall_2", "CAM 23", "East Wing Foyer", "Hall", "east_alcove", true),
    CctvCameraMeta("cam_hall_3", "CAM 24", "VIP Wing Exterior Gate", "Hall", "gallery", true),
  ).also { list ->
    require(list.size == 24) { "Phase 20 requires exactly 24 common-area CCTV cameras." }
    require(list.none { it.roomId.lowercase() in FORBIDDEN_CCTV_ZONES }) {
      "Privacy violation: private bathroom/smoking/changing zone present in CCTV list."
    }
  }

  val NPC_PROFILES: List<BiggGramNpcProfile> = listOf(
    BiggGramNpcProfile("npc_01", "Aria", "Player 01", "@aria.strategist", "Strategist", "Reading the house before making any move.", "#1F7A8C", "analytical"),
    BiggGramNpcProfile("npc_02", "Kai", "Player 02", "@kai.wildcard", "Wildcard", "Turning quiet nights into primetime.", "#B91C1C", "bold"),
    BiggGramNpcProfile("npc_03", "Zara", "Player 03", "@zara.architect", "Architect", "Precision builds, zero excuses.", "#D97706", "critical"),
    BiggGramNpcProfile("npc_04", "Mateo", "Player 04", "@chef.mateo", "Chef", "Feeding the house, watching the politics.", "#0EA5E9", "warm"),
    BiggGramNpcProfile("npc_05", "Nova", "Player 05", "@nova.showrunner", "Showrunner", "Lights, camera, confession time.", "#7C3AED", "dramatic"),
    BiggGramNpcProfile("npc_06", "Devon", "Player 06", "@devon.qm", "Quartermaster", "Every ration counted.", "#059669", "fair"),
    BiggGramNpcProfile("npc_07", "Sienna", "Player 07", "@sienna.analyst", "Analyst", "Tracking alliances & task stats.", "#BE185D", "sharp"),
    BiggGramNpcProfile("npc_08", "Rohan", "Player 08", "@rohan.athlete", "Athlete", "Ready for every physical arena task.", "#2563EB", "competitive"),
    BiggGramNpcProfile("npc_09", "Chloe", "Player 09", "@chloe.social", "Socialite", "Tea in the Living Atrium at 5 PM.", "#0D9488", "playful"),
    BiggGramNpcProfile("npc_10", "Liam", "Player 10", "@liam.tactician", "Tactician", "Always two steps ahead of eviction.", "#0284C7", "strategic"),
    BiggGramNpcProfile("npc_11", "Maya", "Player 11", "@maya.swimmer", "Swimmer", "Pool deck mornings hit different.", "#E11D48", "calm"),
    BiggGramNpcProfile("npc_12", "Tariq", "Player 12", "@tariq.botanist", "Botanist", "Peace under the Botanical Pergola.", "#15803D", "thoughtful"),
    BiggGramNpcProfile("npc_13", "Elena", "Player 13", "@elena.challenger", "Challenger", "Never backing down from a duel.", "#EA580C", "fierce"),
    BiggGramNpcProfile("npc_14", "Jude", "Player 14", "@jude.producer", "Producer", "Observing every whisper in East Hall.", "#334155", "observant"),
    BiggGramNpcProfile("npc_15", "Priya", "Player 15", "@priya.vip", "VIP Contender", "Golden Lounge standards only.", "#9333EA", "glam"),
  )

  fun defaultStories(now: Long = System.currentTimeMillis()): List<BiggGramStory> =
    NPC_PROFILES.take(8).mapIndexed { idx, npc ->
      val headline = when (idx) {
        0 -> "Morning Strategy Notes"
        1 -> "Wildcard Energy Check"
        2 -> "Architecture Inspection"
        3 -> "Dinner Menu Tease"
        4 -> "Showtime Countdown"
        5 -> "Store Room Audit"
        6 -> "Stats Breakdown"
        else -> "Arena Warmup"
      }
      BiggGramStory(
        id = "story_${npc.npcId}",
        npcId = npc.npcId,
        handle = npc.handle,
        displayName = npc.name,
        avatarHex = npc.avatarColorHex,
        headline = headline,
        caption = "${npc.name} (${npc.archetype}): \"${npc.bio}\"",
        timestampMillis = now - (idx * 600_000L),
        viewed = false,
      )
    }

  fun defaultPosts(now: Long = System.currentTimeMillis()): List<BiggGramPost> = listOf(
    BiggGramPost(
      id = "bg_post_welcome",
      authorHandle = "@biggboss.official",
      authorName = "BIGG BOSS – THE HOUSE",
      authorAvatarHex = "#EAB954",
      caption = "Welcome to BiggGram! All official nomination polls and NPC public reactions live exclusively here for the Main Admin.",
      category = "LIFESTYLE",
      likes = 142,
      comments = listOf(
        BiggGramComment("c_w1", "@aria.strategist", "Aria", "All eyes on the weekend nominations!", now - 1_800_000L),
        BiggGramComment("c_w2", "@kai.wildcard", "Kai", "Let the real drama begin inside the house.", now - 1_200_000L),
      ),
      timestampMillis = now - 3_600_000L,
    ),
    BiggGramPost(
      id = "bg_post_kitchen",
      authorHandle = "@chef.mateo",
      authorName = "Mateo",
      authorAvatarHex = "#0EA5E9",
      caption = "Cooked a fresh Bigg Boss Power Bowl in the Gourmet Kitchen tonight. Who is helping clean the sink? 🍳",
      category = "LIFESTYLE",
      likes = 68,
      comments = listOf(
        BiggGramComment("c_k1", "@devon.qm", "Devon", "Leave some rations for morning breakfast!", now - 900_000L),
      ),
      timestampMillis = now - 2_400_000L,
    ),
    BiggGramPost(
      id = "bg_post_arena",
      authorHandle = "@rohan.athlete",
      authorName = "Rohan",
      authorAvatarHex = "#2563EB",
      caption = "Ready to defend Captaincy in the Activity Arena! Bring on the obstacle run. 💪",
      category = "DRAMA",
      likes = 91,
      comments = listOf(
        BiggGramComment("c_a1", "@elena.challenger", "Elena", "See you on the podium track, Rohan!", now - 400_000L),
      ),
      timestampMillis = now - 1_500_000L,
    ),
  )

  fun publishNominationPollPost(
    cycle: VotingCycle,
    nominees: List<NomineeSummary>,
    now: Long = System.currentTimeMillis(),
  ): BiggGramPost {
    val namesStr = nominees.joinToString(", ") { it.displayName }
    val postId = "bg_nom_${cycle.id}"
    val autoComments = listOf(
      BiggGramComment(
        id = "c_${cycle.id}_1",
        authorHandle = "@aria.strategist",
        authorName = "Aria",
        text = "Heavy nominations this week! Watching $namesStr closely.",
        timestampMillis = now + 1L,
      ),
      BiggGramComment(
        id = "c_${cycle.id}_2",
        authorHandle = "@nova.showrunner",
        authorName = "Nova",
        text = "BiggGram NPC audience polls are officially LIVE!",
        timestampMillis = now + 2L,
      ),
    )
    return BiggGramPost(
      id = postId,
      authorHandle = "@biggboss.official",
      authorName = "NOMINATION POLL • ${cycle.title}",
      authorAvatarHex = "#22D3EE",
      caption = "Official Nominated Housemates for ${cycle.title} (${cycle.voteType} rule): $namesStr. Every eligible NPC casts exactly ONE BiggGram vote!",
      category = "NOMINATION_POLL",
      cycleId = cycle.id,
      nomineeUids = nominees.map { it.uid },
      likes = 25,
      likedByAdmin = true,
      comments = autoComments,
      timestampMillis = now,
    )
  }

  fun castSingleNpcVote(
    cycleId: String,
    existingVotes: List<NpcVoteRecord>,
    npc: BiggGramNpcProfile,
    target: NomineeSummary,
    postId: String,
    now: Long = System.currentTimeMillis(),
  ): Result<NpcVoteRecord> {
    val alreadyVoted = existingVotes.any { it.cycleId == cycleId && it.npcId == npc.npcId }
    if (alreadyVoted) {
      return Result.failure(
        IllegalStateException("Duplicate vote blocked: ${npc.name} (${npc.handle}) already voted in this round."),
      )
    }
    return Result.success(
      NpcVoteRecord(
        id = "nv_${cycleId}_${npc.npcId}",
        cycleId = cycleId,
        npcId = npc.npcId,
        npcName = npc.name,
        npcHandle = npc.handle,
        targetUid = target.uid,
        targetName = target.displayName,
        postId = postId,
        castAtMillis = now,
      ),
    )
  }

  fun pickNpcReactionComment(npc: BiggGramNpcProfile, targetName: String, voteType: String): String =
    when (npc.personalityStyle) {
      "analytical" -> "Evaluated the week's gameplay — my BiggGram $voteType vote goes to $targetName."
      "bold" -> "No hesitation! Voting for $targetName right here on BiggGram."
      "critical" -> "After tracking task performance, $targetName gets my single vote."
      "dramatic" -> "Plot twist incoming! Locked my vote for $targetName! 🔥"
      "warm" -> "Tough choice for the house, locked my vote for $targetName."
      else -> "Submitted my single official BiggGram vote for $targetName."
    }

  fun calculateNpcVotingOutcome(
    cycleId: String,
    nominees: List<NomineeSummary>,
    votes: List<NpcVoteRecord>,
  ): NpcVotingOutcome {
    // Deduplicate defensively by (cycleId, npcId) so invalid duplicates never corrupt totals
    val validVotes = votes
      .filter { it.cycleId == cycleId }
      .distinctBy { it.npcId }
    val countsByUid = validVotes.groupingBy { it.targetUid }.eachCount()
    val total = validVotes.size
    val rows = nominees.map { nom ->
      val cnt = countsByUid[nom.uid] ?: 0
      val pct = if (total > 0) (cnt * 100.0) / total else 0.0
      NpcVotingSummaryRow(nominee = nom, voteCount = cnt, percentage = pct)
    }.sortedByDescending { it.voteCount }

    val highest = rows.maxByOrNull { it.voteCount }
    val lowest = rows.minByOrNull { it.voteCount }
    val maxVotes = highest?.voteCount ?: 0
    val isTie = rows.count { it.voteCount == maxVotes && maxVotes > 0 } > 1

    return NpcVotingOutcome(
      cycleId = cycleId,
      totalValidNpcVotes = total,
      rows = rows,
      highestNominee = highest,
      lowestNominee = lowest,
      isTieAtTop = isTie,
    )
  }

  fun computeExtendedPlayerProfiles(
    members: List<SeasonMember>,
    leaderboard: List<PlayerLeaderboardEntry>,
    season: SeasonSession?,
    vipPrivileges: List<VipPrivilege>,
    tasks: List<HouseTask>,
    captaincyHistory: List<CaptaincyRecord>,
  ): List<PlayerExtendedProfile> {
    val activeMembers = members.filter { it.role != PlayerRole.ADMIN.name }
    val lbByUid = leaderboard.associateBy { it.userId }
    val raw = activeMembers.map { m ->
      val lb = lbByUid[m.userId]
      val points = lb?.points ?: (if (season?.captainUid == m.userId) 120L else 40L)
      val wonTasks = tasks.count { it.winningUid == m.userId }
      val capCount = captaincyHistory.count { it.captainUid == m.userId }
        .coerceAtLeast(if (season?.captainUid == m.userId) 1 else 0)
      val badges = buildList {
        if (season?.captainUid == m.userId) add("👑 Current Captain")
        if (vipPrivileges.any { it.userId == m.userId && it.effectiveVip }) add("💎 VIP Lounge")
        if (wonTasks > 0) add("🏆 Task Winner ($wonTasks)")
        if (points >= 150L) add("⭐ Top Contender")
        if (isEmpty()) add("🏠 Active Housemate")
      }
      val history = buildList {
        if (season?.captainUid == m.userId) add("Serving as House Captain")
        tasks.filter { it.winningUid == m.userId }.take(4).forEach { t ->
          add("Won task: ${t.title} (+${t.maxPoints} pts)")
        }
        if (isEmpty()) add("Joined Season 01 • Bigg Boss – The House")
      }
      PlayerExtendedProfile(
        userId = m.userId,
        displayName = m.displayName,
        playerId = m.playerId,
        characterId = m.characterId,
        rank = 0,
        pointsTotal = points,
        tasksCompleted = lb?.tasksCompleted?.toInt() ?: wonTasks,
        tasksWon = wonTasks,
        captainCount = capCount,
        badges = badges,
        activityHistory = history,
      )
    }.sortedByDescending { it.pointsTotal }

    return raw.mapIndexed { idx, profile -> profile.copy(rank = idx + 1) }
  }

  fun newCommentId(): String = "bgc_${UUID.randomUUID().toString().take(8)}"
}
