package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Preset Themes for Betium
val BetiumGold = Color(0xFFF59E0B)
val BetiumGoldDark = Color(0xFFD97706)
val BetiumGoldLight = Color(0xFFFDE68A)

val CyberCyan = Color(0xFF06B6D4)
val EmeraldMiner = Color(0xFF10B981)
val ElectricViolet = Color(0xFF8B5CF6)
val NeonRose = Color(0xFFF43F5E)
val SunsetOrange = Color(0xFFF97316)

// Dark Theme Surfaces
val DarkBgDefault = Color(0xFF0B0F19)
val DarkSurfaceDefault = Color(0xFF151D2F)
val DarkSurfaceElevated = Color(0xFF1E293B)
val DarkSurfaceBorder = Color(0xFF334155)

// OLED Pure Black Surfaces
val OledBg = Color(0xFF000000)
val OledSurface = Color(0xFF111111)
val OledSurfaceElevated = Color(0xFF1A1A1A)
val OledSurfaceBorder = Color(0xFF262626)

// Light Theme Surfaces
val LightBg = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF1F5F9)
val LightSurfaceBorder = Color(0xFFE2E8F0)

data class ColorPreset(
    val name: String,
    val hex: String,
    val color: Color
)

val ColorPresets = listOf(
    ColorPreset("Betium Gold", "#F59E0B", BetiumGold),
    ColorPreset("Cyber Cyan", "#06B6D4", CyberCyan),
    ColorPreset("Emerald Miner", "#10B981", EmeraldMiner),
    ColorPreset("Electric Violet", "#8B5CF6", ElectricViolet),
    ColorPreset("Neon Rose", "#F43F5E", NeonRose),
    ColorPreset("Sunset Orange", "#F97316", SunsetOrange)
)

fun parseHexColor(hexString: String, defaultColor: Color = BetiumGold): Color {
    val clean = hexString.trim().removePrefix("#")
    return try {
        when (clean.length) {
            6 -> Color(android.graphics.Color.parseColor("#$clean"))
            8 -> Color(android.graphics.Color.parseColor("#$clean"))
            else -> defaultColor
        }
    } catch (e: Exception) {
        defaultColor
    }
}
