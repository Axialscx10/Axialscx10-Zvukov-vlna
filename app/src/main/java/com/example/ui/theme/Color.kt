package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class SoundWaveColors(
    val surfaceVoid: Color,
    val surfaceDim: Color,
    val surface: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceBright: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val primaryFixed: Color,
    val primaryFixedDim: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val secondaryFixed: Color,
    val secondaryFixedDim: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val tertiaryFixed: Color,
    val errorColor: Color,
    val onErrorColor: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val amberStar: Color,
    val isDark: Boolean
)

val DarkColorTokens = SoundWaveColors(
    surfaceVoid = Color(0xFF030303),
    surfaceDim = Color(0xFF131313),
    surface = Color(0xFF131313),
    surfaceContainerLowest = Color(0xFF0E0E0E),
    surfaceContainerLow = Color(0xFF1C1B1B),
    surfaceContainer = Color(0xFF201F1F),
    surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Color(0xFF353534),
    surfaceBright = Color(0xFF393939),
    surfaceVariant = Color(0xFF353534),
    onSurface = Color(0xFFE5E2E1),
    onSurfaceVariant = Color(0xFFEABCB5),
    outline = Color(0xFFB08781),
    outlineVariant = Color(0xFF5F3E3A),
    primary = Color(0xFFFFB4A9),
    onPrimary = Color(0xFF690002),
    primaryContainer = Color(0xFFFF5545),
    onPrimaryContainer = Color(0xFF5C0002),
    primaryFixed = Color(0xFFFFDAD5),
    primaryFixedDim = Color(0xFFFFB4A9),
    secondary = Color(0xFFD3FBFF),
    onSecondary = Color(0xFF00363A),
    secondaryContainer = Color(0xFF00EEFC),
    onSecondaryContainer = Color(0xFF00686F),
    secondaryFixed = Color(0xFF7DF4FF),
    secondaryFixedDim = Color(0xFF00DBE9),
    tertiary = Color(0xFFDDB7FF),
    onTertiary = Color(0xFF490080),
    tertiaryContainer = Color(0xFFB76DFF),
    onTertiaryContainer = Color(0xFF400071),
    tertiaryFixed = Color(0xFFF0DBFF),
    errorColor = Color(0xFFFFB4AB),
    onErrorColor = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    amberStar = Color(0xFFFBBF24),
    isDark = true
)

val LightColorTokens = SoundWaveColors(
    surfaceVoid = Color(0xFFF3ECEB),
    surfaceDim = Color(0xFFEDE6E5),
    surface = Color(0xFFFBF8F8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5EFEF),
    surfaceContainer = Color(0xFFEFE9E8),
    surfaceContainerHigh = Color(0xFFE9E3E2),
    surfaceContainerHighest = Color(0xFFE2DCDB),
    surfaceBright = Color(0xFFFFF8F7),
    surfaceVariant = Color(0xFFEBE2E1),
    onSurface = Color(0xFF1E1B1B),
    onSurfaceVariant = Color(0xFF554442),
    outline = Color(0xFF887472),
    outlineVariant = Color(0xFFD5C4C2),
    primary = Color(0xFFC00018),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFF5545),
    onPrimaryContainer = Color(0xFFFFFFFF),
    primaryFixed = Color(0xFFFFDAD6),
    primaryFixedDim = Color(0xFFFFB4A9),
    secondary = Color(0xFF006874),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF00B4C4),
    onSecondaryContainer = Color(0xFF001F24),
    secondaryFixed = Color(0xFF006874),
    secondaryFixedDim = Color(0xFF004F58),
    tertiary = Color(0xFF6B4B93),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0DBFF),
    onTertiaryContainer = Color(0xFF26004E),
    tertiaryFixed = Color(0xFFF0DBFF),
    errorColor = Color(0xFFBA1A1A),
    onErrorColor = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    amberStar = Color(0xFFD97706),
    isDark = false
)

val AmoledColorTokens = SoundWaveColors(
    surfaceVoid = Color(0xFF000000),
    surfaceDim = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF222222),
    surfaceBright = Color(0xFF2A2A2A),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFEABCB5),
    outline = Color(0xFFB08781),
    outlineVariant = Color(0xFF5F3E3A),
    primary = Color(0xFFFFB4A9),
    onPrimary = Color(0xFF690002),
    primaryContainer = Color(0xFFFF5545),
    onPrimaryContainer = Color(0xFF5C0002),
    primaryFixed = Color(0xFFFFDAD5),
    primaryFixedDim = Color(0xFFFFB4A9),
    secondary = Color(0xFFD3FBFF),
    onSecondary = Color(0xFF00363A),
    secondaryContainer = Color(0xFF00EEFC),
    onSecondaryContainer = Color(0xFF00686F),
    secondaryFixed = Color(0xFF7DF4FF),
    secondaryFixedDim = Color(0xFF00DBE9),
    tertiary = Color(0xFFDDB7FF),
    onTertiary = Color(0xFF490080),
    tertiaryContainer = Color(0xFFB76DFF),
    onTertiaryContainer = Color(0xFF400071),
    tertiaryFixed = Color(0xFFF0DBFF),
    errorColor = Color(0xFFFFB4AB),
    onErrorColor = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    amberStar = Color(0xFFFBBF24),
    isDark = true
)

val LocalSoundWaveColors = staticCompositionLocalOf { DarkColorTokens }

// Dynamic token accessors that match all existing imports across screens!
val SurfaceVoid: Color @Composable get() = LocalSoundWaveColors.current.surfaceVoid
val SurfaceDim: Color @Composable get() = LocalSoundWaveColors.current.surfaceDim
val Surface: Color @Composable get() = LocalSoundWaveColors.current.surface
val SurfaceContainerLowest: Color @Composable get() = LocalSoundWaveColors.current.surfaceContainerLowest
val SurfaceContainerLow: Color @Composable get() = LocalSoundWaveColors.current.surfaceContainerLow
val SurfaceContainer: Color @Composable get() = LocalSoundWaveColors.current.surfaceContainer
val SurfaceContainerHigh: Color @Composable get() = LocalSoundWaveColors.current.surfaceContainerHigh
val SurfaceContainerHighest: Color @Composable get() = LocalSoundWaveColors.current.surfaceContainerHighest
val SurfaceBright: Color @Composable get() = LocalSoundWaveColors.current.surfaceBright
val SurfaceVariant: Color @Composable get() = LocalSoundWaveColors.current.surfaceVariant

val OnSurface: Color @Composable get() = LocalSoundWaveColors.current.onSurface
val OnSurfaceVariant: Color @Composable get() = LocalSoundWaveColors.current.onSurfaceVariant
val Outline: Color @Composable get() = LocalSoundWaveColors.current.outline
val OutlineVariant: Color @Composable get() = LocalSoundWaveColors.current.outlineVariant

val Primary: Color @Composable get() = LocalSoundWaveColors.current.primary
val OnPrimary: Color @Composable get() = LocalSoundWaveColors.current.onPrimary
val PrimaryContainer: Color @Composable get() = LocalSoundWaveColors.current.primaryContainer
val OnPrimaryContainer: Color @Composable get() = LocalSoundWaveColors.current.onPrimaryContainer
val PrimaryFixed: Color @Composable get() = LocalSoundWaveColors.current.primaryFixed
val PrimaryFixedDim: Color @Composable get() = LocalSoundWaveColors.current.primaryFixedDim

val Secondary: Color @Composable get() = LocalSoundWaveColors.current.secondary
val OnSecondary: Color @Composable get() = LocalSoundWaveColors.current.onSecondary
val SecondaryContainer: Color @Composable get() = LocalSoundWaveColors.current.secondaryContainer
val OnSecondaryContainer: Color @Composable get() = LocalSoundWaveColors.current.onSecondaryContainer
val SecondaryFixed: Color @Composable get() = LocalSoundWaveColors.current.secondaryFixed
val SecondaryFixedDim: Color @Composable get() = LocalSoundWaveColors.current.secondaryFixedDim

val Tertiary: Color @Composable get() = LocalSoundWaveColors.current.tertiary
val OnTertiary: Color @Composable get() = LocalSoundWaveColors.current.onTertiary
val TertiaryContainer: Color @Composable get() = LocalSoundWaveColors.current.tertiaryContainer
val OnTertiaryContainer: Color @Composable get() = LocalSoundWaveColors.current.onTertiaryContainer
val TertiaryFixed: Color @Composable get() = LocalSoundWaveColors.current.tertiaryFixed

val ErrorColor: Color @Composable get() = LocalSoundWaveColors.current.errorColor
val OnErrorColor: Color @Composable get() = LocalSoundWaveColors.current.onErrorColor
val ErrorContainer: Color @Composable get() = LocalSoundWaveColors.current.errorContainer
val OnErrorContainer: Color @Composable get() = LocalSoundWaveColors.current.onErrorContainer
val AmberStar: Color @Composable get() = LocalSoundWaveColors.current.amberStar
