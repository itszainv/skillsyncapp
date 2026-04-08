package com.example.skillsync.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SkillSyncRed,
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

private val LightColorScheme = lightColorScheme(
    primary = SkillSyncRed,
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

@Composable
fun SkillSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}