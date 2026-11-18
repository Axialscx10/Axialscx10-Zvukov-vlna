package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TrackItem
import com.example.data.cleanAlbumName
import com.example.data.cleanArtistName
import com.example.data.formatMs
import com.example.ui.SoundWaveUiState
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.Primary
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.Secondary
import com.example.ui.theme.SecondaryFixed
import com.example.ui.theme.SecondaryFixedDim
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceContainerLowest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class TvVisualizerMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    SPECTRUM(
        id = "SPECTRUM",
        title = "Hi-Fi Spektrum",
        subtitle = "64-pásmový analyzátor + odraz",
        icon = Icons.Default.Equalizer
    ),
    VINYL_PULSE(
        id = "VINYL_PULSE",
        title = "Vinyl & Vlny",
        subtitle = "Otáčajúca sa LP platňa + basové prstence",
        icon = Icons.Default.Album
    ),
    SYNTHWAVE(
        id = "SYNTHWAVE",
        title = "Neon Synthwave",
        subtitle = "3D retro horizont + osciloskop",
        icon = Icons.Default.Waves
    ),
    AMBIENT_SPACE(
        id = "AMBIENT_SPACE",
        title = "Hviezdny Priestor",
        subtitle = "Častice, hmlovina a veľký text piesne",
        icon = Icons.Default.AutoAwesome
    );

    fun next(): TvVisualizerMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

/**
 * 60 FPS procedural & EQ-reactive audio visualizer canvas that requires zero microphone permissions
 * and works reliably across all Android TV boxes, HDMI outputs, and Navidrome streams.
 */
@Composable
fun TvAudioVisualizerCanvas(
    isPlaying: Boolean,
    mode: TvVisualizerMode,
    accentColor: Color,
    secondaryColor: Color,
    eqBands: List<Float>,
    subBassBoostDb: Float,
    trackSeed: Int,
    modifier: Modifier = Modifier
) {
    var timeSec by remember { mutableFloatStateOf(0f) }
    val barCount = 56
    val smoothLevels = remember { FloatArray(barCount) { 0.12f } }
    val peakLevels = remember { FloatArray(barCount) { 0.16f } }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastFrameMs = -1L
        while (true) {
            withInfiniteAnimationFrameMillis { frameMs ->
                if (lastFrameMs < 0L) lastFrameMs = frameMs
                val dt = ((frameMs - lastFrameMs).coerceIn(1L, 80L)) / 1000f
                lastFrameMs = frameMs
                timeSec += dt
            }
            // Cap visualizer refresh to ~30fps on Android TV so 50%+ of Main Thread frame time
            // remains free for instant D-pad remote focus navigation!
            kotlinx.coroutines.delay(32L)
        }
    }

    // BPM / harmonic rhythm derived from track seed so each song has its own pulse character
    val bpmFactor = remember(trackSeed) {
        val raw = 96 + (abs(trackSeed) % 52) // 96..148 BPM feel
        raw / 60f
    }

    val primaryColor = PrimaryContainer
    val secondaryFixedColor = SecondaryFixed

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val t = timeSec

        // Calculate 56 frequency bins shaped by user's 10-band EQ & Sub-Bass settings
        val bassGain = (1f + (subBassBoostDb / 15f)).coerceIn(0.6f, 1.85f)
        val beatPulse = if (isPlaying) {
            val b1 = (sin(t * bpmFactor * PI.toFloat() * 2f) * 0.5f + 0.5f)
            val b2 = (sin(t * bpmFactor * PI.toFloat() * 4f + 0.8f) * 0.5f + 0.5f)
            (b1 * b1 * 0.7f + b2 * 0.3f)
        } else {
            0.08f
        }

        for (i in 0 until barCount) {
            val norm = i.toFloat() / (barCount - 1).toFloat()
            val eqIdx = (norm * (eqBands.size - 1)).toInt().coerceIn(0, (eqBands.size - 1).coerceAtLeast(0))
            val eqDb = if (eqBands.isNotEmpty()) eqBands[eqIdx] else 0f
            val eqMultiplier = (1f + eqDb / 14f).coerceIn(0.45f, 1.85f)

            val target = if (!isPlaying) {
                0.06f + 0.03f * sin(t * 1.5f + i * 0.25f)
            } else {
                val lowFreqEnvelope = (1f - norm * 0.65f) * bassGain
                val wave1 = sin(t * (4.2f + norm * 9.5f) + i * 0.37f) * 0.5f + 0.5f
                val wave2 = cos(t * (7.8f - norm * 3.1f) - i * 0.61f) * 0.5f + 0.5f
                val wave3 = sin(t * 13.5f + i * 1.15f) * 0.5f + 0.5f
                val kickBoost = if (norm < 0.22f) beatPulse * 0.48f * bassGain else 0f
                val vocalPresence = if (norm in 0.28f..0.68f) (wave1 * 0.45f + wave2 * 0.35f) else (wave2 * 0.3f + wave3 * 0.25f)
                val combined = (0.14f + vocalPresence * 0.58f + kickBoost) * lowFreqEnvelope * eqMultiplier
                combined.coerceIn(0.08f, 0.98f)
            }

            val prev = smoothLevels[i]
            val next = if (target > prev) {
                prev + (target - prev) * 0.38f
            } else {
                prev + (target - prev) * 0.16f
            }
            smoothLevels[i] = next

            if (next >= peakLevels[i]) {
                peakLevels[i] = next
            } else {
                peakLevels[i] = (peakLevels[i] - 0.008f).coerceAtLeast(next)
            }
        }

        when (mode) {
            TvVisualizerMode.SPECTRUM -> {
                // Ambient bass aura
                drawCircle(
                    color = accentColor.copy(alpha = (0.14f + beatPulse * 0.16f).coerceIn(0f, 0.36f)),
                    center = Offset(w * 0.5f, h * 0.62f),
                    radius = w * (0.34f + beatPulse * 0.06f)
                )

                val baselineY = h * 0.72f
                val maxBarHeight = h * 0.52f
                val reflectMaxHeight = h * 0.18f
                val horizontalPad = w * 0.04f
                val usableW = w - horizontalPad * 2f
                val slotW = usableW / barCount
                val barW = (slotW * 0.68f).coerceAtLeast(3f)
                val cornerR = CornerRadius(barW * 0.45f, barW * 0.45f)
                val peakCapColor = Color.White.copy(alpha = 0.9f)
                val reflectColor = primaryColor.copy(alpha = 0.24f)

                // Single shared vertical gradient across the spectrum band — zero per-bar shader allocations!
                val sharedBarBrush = Brush.verticalGradient(
                    colors = listOf(
                        secondaryFixedColor,
                        primaryColor,
                        accentColor
                    ),
                    startY = baselineY - maxBarHeight,
                    endY = baselineY
                )

                // Subtle horizon line
                drawLine(
                    color = Color.White.copy(alpha = 0.14f),
                    start = Offset(horizontalPad, baselineY),
                    end = Offset(w - horizontalPad, baselineY),
                    strokeWidth = 1.5f
                )

                for (i in 0 until barCount) {
                    val lvl = smoothLevels[i]
                    val peak = peakLevels[i]
                    val x = horizontalPad + i * slotW + (slotW - barW) * 0.5f
                    val barH = (lvl * maxBarHeight).coerceAtLeast(6f)
                    val topY = baselineY - barH

                    // Main spectrum bar
                    drawRoundRect(
                        brush = sharedBarBrush,
                        topLeft = Offset(x, topY),
                        size = Size(barW, barH),
                        cornerRadius = cornerR
                    )

                    // Floating Peak-Hold cap
                    val peakY = (baselineY - peak * maxBarHeight - 6f).coerceAtLeast(8f)
                    drawRoundRect(
                        color = peakCapColor,
                        topLeft = Offset(x, peakY),
                        size = Size(barW, 3.5f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )

                    // Water reflection below baseline
                    val reflectH = (lvl * reflectMaxHeight).coerceAtLeast(3f)
                    drawRoundRect(
                        color = reflectColor,
                        topLeft = Offset(x, baselineY + 3f),
                        size = Size(barW, reflectH),
                        cornerRadius = cornerR
                    )
                }
            }

            TvVisualizerMode.VINYL_PULSE -> {
                val cx = w * 0.5f
                val cy = h * 0.5f
                val minDim = minOf(w, h)
                val baseRadius = minDim * 0.22f

                // Concentric bass shockwaves
                for (ring in 1..4) {
                    val phase = ((t * 0.45f + ring * 0.25f) % 1f)
                    val ringR = baseRadius * (1.05f + phase * 1.45f)
                    val ringAlpha = (1f - phase) * (0.22f + beatPulse * 0.2f)
                    drawCircle(
                        color = if (ring % 2 == 0) secondaryFixedColor.copy(alpha = ringAlpha)
                        else primaryColor.copy(alpha = ringAlpha),
                        center = Offset(cx, cy),
                        radius = ringR,
                        style = Stroke(width = 2.5f + beatPulse * 3f)
                    )
                }

                // Radial equalizer rays around the vinyl disc
                val rayCount = barCount
                for (i in 0 until rayCount) {
                    val angleRad = (i.toFloat() / rayCount) * (2f * PI.toFloat()) + t * 0.22f
                    val lvl = smoothLevels[i]
                    val innerR = baseRadius * 1.06f + beatPulse * 8f
                    val outerR = innerR + lvl * (minDim * 0.24f)
                    val sx = cx + cos(angleRad) * innerR
                    val sy = cy + sin(angleRad) * innerR
                    val ex = cx + cos(angleRad) * outerR
                    val ey = cy + sin(angleRad) * outerR

                    drawLine(
                        color = if (i % 2 == 0) primaryColor else secondaryFixedColor,
                        start = Offset(sx, sy),
                        end = Offset(ex, ey),
                        strokeWidth = 4.5f,
                        cap = StrokeCap.Round
                    )
                }

                // Vinyl Disc Body
                drawCircle(
                    color = Color(0xFF111217),
                    center = Offset(cx, cy),
                    radius = baseRadius
                )
                // Vinyl grooves
                for (g in 1..6) {
                    val grooveR = baseRadius * (0.45f + g * 0.08f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.07f),
                        center = Offset(cx, cy),
                        radius = grooveR,
                        style = Stroke(width = 1.2f)
                    )
                }
                // Center vinyl label
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(primaryColor, secondaryFixedColor, accentColor, primaryColor),
                        center = Offset(cx, cy)
                    ),
                    center = Offset(cx, cy),
                    radius = baseRadius * 0.36f
                )
                drawCircle(
                    color = Color(0xFF0B0C10),
                    center = Offset(cx, cy),
                    radius = baseRadius * 0.08f
                )
            }

            TvVisualizerMode.SYNTHWAVE -> {
                val horizonY = h * 0.56f
                val sunRadius = minOf(w, h) * (0.22f + beatPulse * 0.025f)
                val sunCenter = Offset(w * 0.5f, horizonY - sunRadius * 0.25f)

                // Retro pulsing sun
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFD54F),
                            Color(0xFFFF5252),
                            Color(0xFFAA00FF)
                        ),
                        startY = sunCenter.y - sunRadius,
                        endY = sunCenter.y + sunRadius
                    ),
                    center = sunCenter,
                    radius = sunRadius
                )

                // Perspective 3D grid below horizon
                val gridLines = 14
                for (g in -gridLines..gridLines) {
                    val startX = w * 0.5f + g * (w * 0.035f)
                    val endX = w * 0.5f + g * (w * 0.16f)
                    drawLine(
                        color = primaryColor.copy(alpha = 0.26f),
                        start = Offset(startX, horizonY),
                        end = Offset(endX, h),
                        strokeWidth = 1.5f
                    )
                }
                val scrollOffset = (t * 0.65f) % 1f
                for (row in 0..8) {
                    val p = ((row + scrollOffset) / 9f)
                    val y = horizonY + (p * p) * (h - horizonY)
                    drawLine(
                        color = secondaryFixedColor.copy(alpha = (0.12f + p * 0.28f).coerceIn(0f, 0.45f)),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.5f
                    )
                }

                // Multi-layer Neon Oscilloscope Waves across the horizon
                for (layer in 0..2) {
                    val path = Path()
                    val stepCount = 90
                    val layerPhase = layer * 1.3f
                    val ampScale = (h * 0.18f) * (1f - layer * 0.22f)
                    for (s in 0..stepCount) {
                        val pct = s.toFloat() / stepCount
                        val x = pct * w
                        val binIdx = (pct * (barCount - 1)).toInt().coerceIn(0, barCount - 1)
                        val energy = smoothLevels[binIdx]
                        val wave = sin(pct * 14f + t * (5f + layer * 1.5f) + layerPhase) *
                            cos(pct * 6f - t * 3.2f)
                        val y = horizonY + wave * energy * ampScale
                        if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    val waveColor = when (layer) {
                        0 -> secondaryFixedColor
                        1 -> primaryColor
                        else -> Color.White.copy(alpha = 0.75f)
                    }
                    drawPath(
                        path = path,
                        color = waveColor,
                        style = Stroke(width = (5f - layer * 1.2f).coerceAtLeast(2f), cap = StrokeCap.Round)
                    )
                }
            }

            TvVisualizerMode.AMBIENT_SPACE -> {
                // Nebula clouds
                val cx1 = w * (0.35f + 0.12f * sin(t * 0.35f))
                val cy1 = h * (0.42f + 0.08f * cos(t * 0.28f))
                val cx2 = w * (0.68f + 0.10f * cos(t * 0.31f))
                val cy2 = h * (0.55f + 0.09f * sin(t * 0.24f))

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.26f + beatPulse * 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(cx1, cy1),
                        radius = w * 0.45f
                    ),
                    center = Offset(cx1, cy1),
                    radius = w * 0.45f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryFixedColor.copy(alpha = 0.22f + beatPulse * 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(cx2, cy2),
                        radius = w * 0.42f
                    ),
                    center = Offset(cx2, cy2),
                    radius = w * 0.42f
                )

                // Floating starlight particles reactive to highs/mids
                val starCount = 42
                for (s in 0 until starCount) {
                    val seed = (s * 7919 + trackSeed)
                    val baseX = (abs(seed) % 1000) / 1000f
                    val baseY = (abs(seed / 13) % 1000) / 1000f
                    val speed = 0.02f + (s % 5) * 0.008f
                    val px = ((baseX + t * speed) % 1f) * w
                    val py = ((baseY + sin(t * 0.5f + s) * 0.04f + 1f) % 1f) * h
                    val bin = s % barCount
                    val radius = 2f + smoothLevels[bin] * 6.5f
                    drawCircle(
                        color = if (s % 3 == 0) secondaryFixedColor.copy(alpha = 0.78f)
                        else Color.White.copy(alpha = 0.65f),
                        center = Offset(px, py),
                        radius = radius
                    )
                }

                // Gentle bottom ribbon wave
                val ribbonPath = Path()
                val baseRibbonY = h * 0.84f
                for (i in 0 until barCount) {
                    val pct = i.toFloat() / (barCount - 1)
                    val x = pct * w
                    val y = baseRibbonY - smoothLevels[i] * (h * 0.18f)
                    if (i == 0) ribbonPath.moveTo(x, y) else ribbonPath.lineTo(x, y)
                }
                drawPath(
                    path = ribbonPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(secondaryFixedColor, primaryColor, secondaryFixedColor)
                    ),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

/**
 * Widescreen 16:9 Stage Card displayed right inside NowPlayingScreen when running on Android TV
 * (or when toggled on by the user). Combines the live visualizer, compact album cover, track metadata,
 * synced lyric preview, and one-click Fullscreen & Style buttons.
 */
@Composable
fun TvNowPlayingVisualizerStage(
    uiState: SoundWaveUiState,
    visualizerMode: TvVisualizerMode,
    accentColor: Color,
    onCycleMode: () -> Unit,
    onSelectMode: (TvVisualizerMode) -> Unit,
    onOpenFullscreen: () -> Unit,
    onOpenArtistDetail: () -> Unit,
    onOpenAlbumDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = uiState.currentTrack
    val artistDisplay = cleanArtistName(track.artist).ifBlank { "Neznámy interpret" }
    val albumDisplay = cleanAlbumName(track.album)
    val trackSeed = remember(track.id, track.title, track.artist) {
        (track.id + track.title + track.artist).hashCode()
    }
    val currentLyricText = remember(uiState.lyrics, uiState.activeLyricIndex) {
        val idx = uiState.activeLyricIndex
        if (idx in uiState.lyrics.indices) uiState.lyrics[idx].text else null
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Main 16:9 TV Visualizer Stage Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .shadow(24.dp, RoundedCornerShape(22.dp), ambientColor = accentColor, spotColor = accentColor)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF08090E))
                .border(1.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
        ) {
            // Live 60fps Audio Visualizer Canvas
            TvAudioVisualizerCanvas(
                isPlaying = uiState.isPlaying,
                mode = visualizerMode,
                accentColor = accentColor,
                secondaryColor = SecondaryFixed,
                eqBands = uiState.eqBands,
                subBassBoostDb = uiState.subBassBoostDb,
                trackSeed = trackSeed,
                modifier = Modifier.fillMaxSize()
            )

            // Subtle dark gradient overlay so text & artwork are always crisp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF06070B).copy(alpha = 0.82f),
                                Color(0xFF06070B).copy(alpha = 0.38f),
                                Color(0xFF06070B).copy(alpha = 0.65f)
                            )
                        )
                    )
            )

            // Foreground Content: Compact Album Cover on Left + Track Info & Live Lyric on Right
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Compact Album Cover with Format Badge
                Box(
                    modifier = Modifier
                        .size(168.dp)
                        .shadow(18.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerLowest)
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                ) {
                    NetImage(
                        url = track.imageUrl,
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (track.badge.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.82f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (uiState.isPlaying) {
                                AnimatedEqualizerBars(
                                    barColor = Primary,
                                    secondaryBarColor = SecondaryFixedDim,
                                    barWidth = 2.dp,
                                    maxHeight = 10.dp
                                )
                            }
                            Text(
                                text = track.badge,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Right Column: Visualizer Status Pill, Track Title, Artist, Live Lyric & Fullscreen Trigger
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Row: Mode badge + Fullscreen button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SurfaceContainerHigh.copy(alpha = 0.85f))
                                .border(1.dp, SecondaryFixed.copy(alpha = 0.45f), RoundedCornerShape(50))
                                .clickable { onCycleMode() }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = visualizerMode.icon,
                                contentDescription = null,
                                tint = SecondaryFixed,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "TV VIZUALIZÁCIA: ${visualizerMode.title.uppercase()}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(PrimaryContainer)
                                .clickable { onOpenFullscreen() }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Na celú obrazovku",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Celá obrazovka",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Middle: Track Title & Clickable Artist/Album
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = track.title.ifBlank { "Neprehráva sa žiadna skladba" },
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = artistDisplay,
                                color = SecondaryFixed,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { onOpenArtistDetail() }
                            )
                            if (albumDisplay.isNotBlank()) {
                                Text(
                                    text = "• $albumDisplay",
                                    color = OnSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { onOpenAlbumDetail() }
                                )
                            }
                        }
                    }

                    // Bottom: Live synced lyric line or visualizer subtitle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.52f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (!currentLyricText.isNullOrBlank()) {
                                "♪  $currentLyricText"
                            } else {
                                "${visualizerMode.title} — ${visualizerMode.subtitle}"
                            },
                            color = if (!currentLyricText.isNullOrBlank()) Color.White else OnSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = if (!currentLyricText.isNullOrBlank()) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 4 Visualizer Style Selector Pills (easy to navigate with TV remote arrows)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvVisualizerMode.entries.forEach { modeItem ->
                val isSelected = modeItem == visualizerMode
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) PrimaryContainer.copy(alpha = 0.9f)
                            else SurfaceContainerHigh.copy(alpha = 0.8f)
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectMode(modeItem) }
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = modeItem.icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else SecondaryFixed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = modeItem.title,
                        color = if (isSelected) Color.White else OnSurface,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Fullscreen Cinema TV Visualizer Overlay.
 * Fills the entire TV screen with 60fps audio reactive visuals, floating album art,
 * synchronized karaoke lyrics, upcoming track preview, and remote-friendly transport controls.
 */
@Composable
fun TvFullscreenVisualizerOverlay(
    uiState: SoundWaveUiState,
    visualizerMode: TvVisualizerMode,
    accentColor: Color,
    onSelectMode: (TvVisualizerMode) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onCloseFullscreen: () -> Unit
) {
    BackHandler(onBack = onCloseFullscreen)

    var showHud by remember { mutableStateOf(true) }
    val track = uiState.currentTrack
    val artistDisplay = cleanArtistName(track.artist).ifBlank { "Neznámy interpret" }
    val albumDisplay = cleanAlbumName(track.album)
    val trackSeed = remember(track.id, track.title, track.artist) {
        (track.id + track.title + track.artist).hashCode()
    }

    val nextTrack = remember(uiState.queue, uiState.queueIndex) {
        val nextIdx = uiState.queueIndex + 1
        if (nextIdx in uiState.queue.indices) uiState.queue[nextIdx] else null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05060A))
    ) {
        // Full-bleed 60fps Visualizer
        TvAudioVisualizerCanvas(
            isPlaying = uiState.isPlaying,
            mode = visualizerMode,
            accentColor = accentColor,
            secondaryColor = SecondaryFixed,
            eqBands = uiState.eqBands,
            subBassBoostDb = uiState.subBassBoostDb,
            trackSeed = trackSeed,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle vignette so text remains legible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Top Bar: Mode Switcher + Clean Canvas toggle + Exit Fullscreen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 28.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showHud) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvVisualizerMode.entries.forEach { modeItem ->
                        val isSelected = modeItem == visualizerMode
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSelected) PrimaryContainer
                                    else Color.Black.copy(alpha = 0.6f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { onSelectMode(modeItem) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = modeItem.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else SecondaryFixed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = modeItem.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hide / Show HUD button ("Čisté plátno")
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                        .clickable { showHud = !showHud }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (showHud) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = SecondaryFixed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (showHud) "Čistý obraz" else "Zobraziť info",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Exit Fullscreen button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SurfaceContainerHigh.copy(alpha = 0.9f))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(50))
                        .clickable { onCloseFullscreen() }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Ukončiť celú obrazovku",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Späť na prehrávač",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Center Live Synced Lyrics Overlay (if song has lyrics and HUD is visible)
        if (showHud && uiState.lyrics.isNotEmpty()) {
            val activeIdx = uiState.activeLyricIndex
            val currentLine = if (activeIdx in uiState.lyrics.indices) {
                uiState.lyrics[activeIdx].text
            } else {
                uiState.lyrics.firstOrNull()?.text.orEmpty()
            }
            val nextLine = if (activeIdx + 1 in uiState.lyrics.indices) {
                uiState.lyrics[activeIdx + 1].text
            } else {
                ""
            }

            if (currentLine.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 48.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.42f))
                        .padding(horizontal = 28.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = currentLine,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    if (nextLine.isNotBlank()) {
                        Text(
                            text = nextLine,
                            color = OnSurfaceVariant.copy(alpha = 0.78f),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Bottom Cinema HUD: Album Art, Track Info, Transport Controls & Next Up
        if (showHud) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Thin Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(uiState.playbackProgress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(SecondaryFixed, PrimaryContainer)
                                    )
                                )
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMs(uiState.positionMs),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatMs(uiState.durationMs),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Bottom HUD Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Artwork + Title/Artist/Quality
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .shadow(16.dp, RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceContainerLowest)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        ) {
                            NetImage(
                                url = track.imageUrl,
                                contentDescription = track.title,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = track.title,
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (track.badge.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SecondaryFixed.copy(alpha = 0.2f))
                                            .border(1.dp, SecondaryFixed.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = track.badge,
                                            color = SecondaryFixed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = buildString {
                                    append(artistDisplay)
                                    if (albumDisplay.isNotBlank()) append(" • $albumDisplay")
                                },
                                color = SecondaryFixed,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (nextTrack != null) {
                                Text(
                                    text = "Nasleduje: ${nextTrack.title} (${cleanArtistName(nextTrack.artist)})",
                                    color = OnSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Right: Transport Controls (Prev / Play-Pause / Next)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .clickable { onPreviousClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Predchádzajúca",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .shadow(18.dp, CircleShape, spotColor = PrimaryContainer)
                                .clip(CircleShape)
                                .background(PrimaryContainer)
                                .clickable { onTogglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pozastaviť" else "Prehrať",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .clickable { onNextClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Ďalšia",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
