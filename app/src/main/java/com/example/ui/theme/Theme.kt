package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LuggageTrackerLightColorScheme = lightColorScheme(
    primary = LimeChartreuse,
    onPrimary = TextOnLime,
    primaryContainer = SoftLimeTintBg,
    onPrimaryContainer = TextPrimary,
    secondary = ElectricIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = ElectricIndigo,
    tertiary = EmeraldSafe,
    onTertiary = Color.White,
    background = LightCreamBg,
    onBackground = TextPrimary,
    surface = PureWhiteCard,
    onSurface = TextPrimary,
    surfaceVariant = SoftGraySurface,
    onSurfaceVariant = TextSecondary,
    outline = LightBorderSubtle,
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
