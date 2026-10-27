package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LuggageTrackerLightColorScheme = lightColorScheme(
    primary = FacebookBlue,
    onPrimary = Color.White,
    primaryContainer = FacebookBlueSoftBg,
    onPrimaryContainer = FacebookBlue,
    secondary = FacebookBlueDark,
    onSecondary = Color.White,
    secondaryContainer = FacebookBlueSoftBg,
    onSecondaryContainer = FacebookBlue,
    tertiary = EmeraldSafe,
    onTertiary = Color.White,
    background = DashboardFeedGrayBg,
    onBackground = TextPrimary,
    surface = DashboardCardWhite,
    onSurface = TextPrimary,
    surfaceVariant = DashboardSurfaceLight,
    onSurfaceVariant = TextSecondary,
    outline = DashboardBorderLight,
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun BaggageGuardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuggageTrackerLightColorScheme,
        typography = Typography,
        content = content
    )
}
