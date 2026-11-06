package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun BetiumTheme(
    themeMode: String = "dark", // "system", "dark", "light"
    accentColorHex: String = "#F59E0B",
    isOledDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemInDark
    }

    val primaryColor = parseHexColor(accentColorHex, BetiumGold)

    val colorScheme = if (isDark) {
        val bg = if (isOledDark) OledBg else DarkBgDefault
        val surface = if (isOledDark) OledSurface else DarkSurfaceDefault
        val surfaceVariant = if (isOledDark) OledSurfaceElevated else DarkSurfaceElevated

        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color(0xFF0F172A),
            primaryContainer = primaryColor.copy(alpha = 0.2f),
            onPrimaryContainer = primaryColor,
            secondary = primaryColor.copy(alpha = 0.85f),
            onSecondary = Color.White,
            background = bg,
            onBackground = Color(0xFFF1F5F9),
            surface = surface,
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = Color(0xFFCBD5E1),
            outline = if (isOledDark) OledSurfaceBorder else DarkSurfaceBorder
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.15f),
            onPrimaryContainer = primaryColor,
            secondary = primaryColor.copy(alpha = 0.85f),
            onSecondary = Color.White,
            background = LightBg,
            onBackground = Color(0xFF0F172A),
            surface = LightSurface,
            onSurface = Color(0xFF1E293B),
            surfaceVariant = LightSurfaceElevated,
            onSurfaceVariant = Color(0xFF475569),
            outline = LightSurfaceBorder
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    BetiumTheme(
        themeMode = if (darkTheme) "dark" else "light",
        content = content
    )
}
