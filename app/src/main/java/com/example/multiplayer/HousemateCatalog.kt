package com.example.multiplayer

import androidx.compose.ui.graphics.Color

data class HousemateOption(
  val id: String,
  val number: Int,
  val name: String,
  val gender: String,
  val archetype: String,
  val styleSummary: String,
  val spawnRoom: String,
  val primaryColor: Color,
  val accentColor: Color,
)

object HousemateCatalog {
  val all: List<HousemateOption> = listOf(
    HousemateOption("Player 01", 1, "Aria", "Female", "Strategist", "High Ponytail • Teal Bomber • Gold Earrings", "Living Atrium", Color(0xFF1F7A8C), Color(0xFFEAB954)),
    HousemateOption("Player 02", 2, "Kai", "Male", "Wildcard", "Spiky Hair • Crimson Hoodie • Headphones", "Living Atrium", Color(0xFFB91C1C), Color(0xFFFEF08A)),
    HousemateOption("Player 03", 3, "Zara", "Female", "Architect", "Curly Crown • Amber Blazer • Glasses", "Living Atrium", Color(0xFFD97706), Color(0xFFFFFFFF)),
    HousemateOption("Player 04", 4, "Mateo", "Male", "Chef", "Side-Part • Ivory Chef Vest", "Dining Area", Color(0xFF334155), Color(0xFFF8FAFC)),
    HousemateOption("Player 05", 5, "Nova", "Female", "Showrunner", "Platinum Bob • Violet Bomber • Earrings", "Gourmet Kitchen", Color(0xFF7C3AED), Color(0xFF67E8F9)),
    HousemateOption("Player 06", 6, "Devon", "Male", "Quartermaster", "Short Crop • Emerald Hoodie", "Store Room", Color(0xFF059669), Color(0xFFA7F3D0)),
    HousemateOption("Player 07", 7, "Sienna", "Female", "Analyst", "Top Bun • Rose Blazer • Glasses", "15-Player Bedroom", Color(0xFFBE185D), Color(0xFFFBCFE8)),
    HousemateOption("Player 08", 8, "Rohan", "Male", "Athlete", "Undercut • Cobalt Bomber • Headphones", "15-Player Bedroom", Color(0xFF2563EB), Color(0xFF93C5FD)),
    HousemateOption("Player 09", 9, "Chloe", "Female", "Socialite", "Golden Ponytail • Aqua Vest • Earrings", "Spa Bathroom", Color(0xFF0D9488), Color(0xFFFDE68A)),
    HousemateOption("Player 10", 10, "Liam", "Male", "Tactician", "Side-Part • Ocean Hoodie", "Swimming Pool", Color(0xFF0284C7), Color(0xFFFFFFFF)),
    HousemateOption("Player 11", 11, "Maya", "Female", "Swimmer", "Top Bun • Coral Bomber", "Swimming Pool", Color(0xFFE11D48), Color(0xFFFDE047)),
    HousemateOption("Player 12", 12, "Tariq", "Male", "Botanist", "Short Crop • Forest Vest • Glasses", "Botanical Garden", Color(0xFF15803D), Color(0xFFD9F99D)),
    HousemateOption("Player 13", 13, "Elena", "Female", "Challenger", "Chestnut Bob • Sunset Bomber • Earrings", "Activity Room", Color(0xFFEA580C), Color(0xFF1E293B)),
    HousemateOption("Player 14", 14, "Jude", "Male", "Producer", "Undercut • Charcoal Blazer • Glasses", "East Wing Foyer", Color(0xFF334155), Color(0xFF22D3EE)),
    HousemateOption("Player 15", 15, "Priya", "Female", "VIP Contender", "Curly Crown • Royal Violet Blazer", "VIP Gallery", Color(0xFF9333EA), Color(0xFFFACC15)),
  )

  fun findById(id: String): HousemateOption =
    all.firstOrNull { it.id == id } ?: all.first()
}
