package com.example.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * High-visibility focus & press indication designed specifically so Android TV / Google TV D-pad
 * navigation is unmistakably clear in Dark Mode, AMOLED Mode, and Light Mode from across the room.
 */
object SoundWaveFocusIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return SoundWaveFocusIndicationNode(interactionSource)
    }

    override fun hashCode(): Int = -1049283741
    override fun equals(other: Any?): Boolean = other === this
}

private class SoundWaveFocusIndicationNode(
    private val interactionSource: InteractionSource
) : Modifier.Node(), DrawModifierNode {

    private var isPressed = false
    private var isFocused = false
    private var isHovered = false

    override fun onAttach() {
        coroutineScope.launch {
            val pressInteractions = mutableListOf<PressInteraction.Press>()
            val focusInteractions = mutableListOf<FocusInteraction.Focus>()
            val hoverInteractions = mutableListOf<HoverInteraction.Enter>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> pressInteractions.add(interaction)
                    is PressInteraction.Release -> pressInteractions.remove(interaction.press)
                    is PressInteraction.Cancel -> pressInteractions.remove(interaction.press)
                    is FocusInteraction.Focus -> focusInteractions.add(interaction)
                    is FocusInteraction.Unfocus -> focusInteractions.remove(interaction.focus)
                    is HoverInteraction.Enter -> hoverInteractions.add(interaction)
                    is HoverInteraction.Exit -> hoverInteractions.remove(interaction.enter)
                }
                val pressed = pressInteractions.isNotEmpty()
                val focused = focusInteractions.isNotEmpty()
                val hovered = hoverInteractions.isNotEmpty()
                if (isPressed != pressed || isFocused != focused || isHovered != hovered) {
                    isPressed = pressed
                    isFocused = focused
                    isHovered = hovered
                    invalidateDraw()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val minDim = minOf(size.width, size.height)
        val radiusPx = if (minDim <= 54.dp.toPx()) minDim / 2f else 12.dp.toPx()
        val cornerRadius = CornerRadius(radiusPx, radiusPx)

        // 1. Pre-content glow background so transparent rows light up immediately on TV
        if (isFocused || isHovered) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFFEA00).copy(alpha = 0.24f),
                        Color(0xFF00E5FF).copy(alpha = 0.22f)
                    )
                ),
                cornerRadius = cornerRadius
            )
        } else if (isPressed) {
            drawRoundRect(
                color = Color(0xFFFFEA00).copy(alpha = 0.32f),
                cornerRadius = cornerRadius
            )
        }

        // 2. Draw component content
        drawContent()

        // 3. Post-content high-contrast TV focus frame (visible on Dark, AMOLED, Light, and Album Art)
        if (isPressed) {
            drawRoundRect(
                color = Color(0xFFFFEA00).copy(alpha = 0.28f),
                cornerRadius = cornerRadius
            )
            val strokePx = 4.dp.toPx()
            drawRoundRect(
                color = Color(0xFFFFEA00),
                topLeft = Offset(strokePx / 2f, strokePx / 2f),
                size = Size(
                    (size.width - strokePx).coerceAtLeast(0f),
                    (size.height - strokePx).coerceAtLeast(0f)
                ),
                cornerRadius = cornerRadius,
                style = Stroke(width = strokePx)
            )
        } else if (isFocused || isHovered) {
            // Top glaze so opaque cards/buttons also visibly brighten when selected
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFFEA00).copy(alpha = 0.22f),
                        Color(0xFF00E5FF).copy(alpha = 0.18f)
                    )
                ),
                cornerRadius = cornerRadius
            )

            // Layer A: Outer dark contrast halo (ensures visibility even on white/bright backgrounds)
            val haloStroke = 6.dp.toPx()
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.85f),
                topLeft = Offset(haloStroke / 2f, haloStroke / 2f),
                size = Size(
                    (size.width - haloStroke).coerceAtLeast(0f),
                    (size.height - haloStroke).coerceAtLeast(0f)
                ),
                cornerRadius = cornerRadius,
                style = Stroke(width = haloStroke)
            )

            // Layer B: Vivid 4dp Electric Gold (#FFEA00) + Neon Cyan (#00E5FF) main TV frame
            val outerStroke = 4.dp.toPx()
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFEA00),
                        Color(0xFF00E5FF),
                        Color(0xFFFFEA00)
                    )
                ),
                topLeft = Offset(outerStroke / 2f, outerStroke / 2f),
                size = Size(
                    (size.width - outerStroke).coerceAtLeast(0f),
                    (size.height - outerStroke).coerceAtLeast(0f)
                ),
                cornerRadius = cornerRadius,
                style = Stroke(width = outerStroke)
            )

            // Layer C: Crisp inner pure white highlight rim
            val innerInset = outerStroke + 1.dp.toPx()
            val innerStroke = 1.5.dp.toPx()
            if (size.width > innerInset * 2 && size.height > innerInset * 2) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.95f),
                    topLeft = Offset(innerInset, innerInset),
                    size = Size(size.width - innerInset * 2, size.height - innerInset * 2),
                    cornerRadius = CornerRadius(
                        (radiusPx - innerInset).coerceAtLeast(2f),
                        (radiusPx - innerInset).coerceAtLeast(2f)
                    ),
                    style = Stroke(width = innerStroke)
                )
            }

            // Layer D: Left Electric Gold indicator bar on cards/rows/buttons wider than 72dp
            if (size.width >= 72.dp.toPx() && size.height >= 28.dp.toPx()) {
                val barW = 6.dp.toPx()
                val barH = (size.height * 0.58f).coerceAtLeast(14.dp.toPx())
                val barX = 4.dp.toPx()
                val barY = (size.height - barH) / 2f
                drawRoundRect(
                    color = Color(0xFFFFEA00),
                    topLeft = Offset(barX, barY),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                )
            }
        }
    }
}

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

    // CRITICAL: CompositionLocalProvider for LocalIndication MUST be INSIDE MaterialTheme,
    // because MaterialTheme provides its own default ripple() LocalIndication internally!
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        CompositionLocalProvider(
            LocalSoundWaveColors provides activeTokens,
            LocalIndication provides SoundWaveFocusIndication,
            content = content
        )
    }
}
