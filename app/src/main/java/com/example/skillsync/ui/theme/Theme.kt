package com.example.skillsync.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

fun isLightTheme(themeKey: String): Boolean = themeKey.startsWith("light_")

private fun accentForTheme(themeKey: String) = when {
    themeKey.endsWith("blue") -> SkillSyncBlue
    themeKey.endsWith("green") -> SkillSyncGreen
    else -> SkillSyncRed
}

private fun buildColorScheme(themeKey: String) = if (isLightTheme(themeKey)) {
    val accent = accentForTheme(themeKey)
    lightColorScheme(
        primary = accent,
        onPrimary = SkillSyncTextPrimary,
        secondary = SkillSyncGold,
        onSecondary = SkillSyncBlack,
        tertiary = SkillSyncGoldSoft,
        background = SkillSyncLightBackground,
        onBackground = SkillSyncLightTextPrimary,
        surface = SkillSyncLightSurface,
        onSurface = SkillSyncLightTextPrimary,
        surfaceVariant = SkillSyncLightSurfaceAlt,
        onSurfaceVariant = SkillSyncLightTextSecondary,
        outline = SkillSyncGold
    )
} else {
    val accent = accentForTheme(themeKey)
    darkColorScheme(
        primary = accent,
        onPrimary = SkillSyncTextPrimary,
        secondary = SkillSyncGold,
        onSecondary = SkillSyncBlack,
        tertiary = SkillSyncGoldSoft,
        background = SkillSyncBlack,
        onBackground = SkillSyncTextPrimary,
        surface = SkillSyncSurface,
        onSurface = SkillSyncTextPrimary,
        surfaceVariant = SkillSyncSurfaceAlt,
        onSurfaceVariant = SkillSyncTextSecondary,
        outline = SkillSyncGold
    )
}

@Composable
fun SkillSyncTheme(
    themeKey: String = "dark_red",
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = buildColorScheme(themeKey),
        typography = Typography,
        content = content
    )
}
