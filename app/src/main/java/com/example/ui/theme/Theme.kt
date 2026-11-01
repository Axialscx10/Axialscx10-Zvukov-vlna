package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private fun createColorScheme(tokens: SoundWaveColors) = if (tokens.isDark) {
    darkColorScheme(
        primary = tokens.primary,
        onPrimary = tokens.onPrimary,
        primaryContainer = tokens.primaryContainer,
        onPrimaryContainer = tokens.onPrimaryContainer,
        secondary = tokens.secondary,
        onSecondary = tokens.onSecondary,
        secondaryContainer = tokens.secondaryContainer,
        onSecondaryContainer = tokens.onSecondaryContainer,
        tertiary = tokens.tertiary,
        onTertiary = tokens.onTertiary,
        tertiaryContainer = tokens.tertiaryContainer,
        onTertiaryContainer = tokens.onTertiaryContainer,
        background = tokens.surface,
        onBackground = tokens.onSurface,
        surface = tokens.surface,
        onSurface = tokens.onSurface,
        surfaceVariant = tokens.surfaceVariant,
        onSurfaceVariant = tokens.onSurfaceVariant,
        surfaceDim = tokens.surfaceDim,
        surfaceBright = tokens.surfaceBright,
        surfaceContainerLowest = tokens.surfaceContainerLowest,
        surfaceContainerLow = tokens.surfaceContainerLow,
        surfaceContainer = tokens.surfaceContainer,
        surfaceContainerHigh = tokens.surfaceContainerHigh,
        surfaceContainerHighest = tokens.surfaceContainerHighest,
        outline = tokens.outline,
        outlineVariant = tokens.outlineVariant,
        error = tokens.errorColor,
        onError = tokens.onErrorColor,
        errorContainer = tokens.errorContainer,
        onErrorContainer = tokens.onErrorContainer
    )
} else {
    lightColorScheme(
        primary = tokens.primary,
        onPrimary = tokens.onPrimary,
        primaryContainer = tokens.primaryContainer,
        onPrimaryContainer = tokens.onPrimaryContainer,
        secondary = tokens.secondary,
        onSecondary = tokens.onSecondary,
        secondaryContainer = tokens.secondaryContainer,
        onSecondaryContainer = tokens.onSecondaryContainer,
        tertiary = tokens.tertiary,
        onTertiary = tokens.onTertiary,
        tertiaryContainer = tokens.tertiaryContainer,
        onTertiaryContainer = tokens.onTertiaryContainer,
        background = tokens.surface,
        onBackground = tokens.onSurface,
        surface = tokens.surface,
        onSurface = tokens.onSurface,
        surfaceVariant = tokens.surfaceVariant,
        onSurfaceVariant = tokens.onSurfaceVariant,
        surfaceDim = tokens.surfaceDim,
        surfaceBright = tokens.surfaceBright,
        surfaceContainerLowest = tokens.surfaceContainerLowest,
        surfaceContainerLow = tokens.surfaceContainerLow,
        surfaceContainer = tokens.surfaceContainer,
        surfaceContainerHigh = tokens.surfaceContainerHigh,
        surfaceContainerHighest = tokens.surfaceContainerHighest,
        outline = tokens.outline,
        outlineVariant = tokens.outlineVariant,
        error = tokens.errorColor,
        onError = tokens.onErrorColor,
        errorContainer = tokens.errorContainer,
        onErrorContainer = tokens.onErrorContainer
    )
}

@Composable
fun MyApplicationTheme(
    themeMode: String = "DARK",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val activeTokens = when (themeMode) {
        "LIGHT" -> LightColorTokens
        "AMOLED" -> AmoledColorTokens
        "SYSTEM" -> if (isSystemDark) DarkColorTokens else LightColorTokens
        else -> DarkColorTokens
    }

    val colorScheme = createColorScheme(activeTokens)

    CompositionLocalProvider(LocalSoundWaveColors provides activeTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
