package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import com.example.ui.components.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SoundWaveUiState
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OnPrimaryContainer
import com.example.ui.theme.OnSecondaryContainer
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.Outline
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.Primary
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryFixed
import com.example.ui.theme.PrimaryFixedDim
import com.example.ui.theme.Secondary
import com.example.ui.theme.SecondaryContainer
import com.example.ui.theme.SecondaryFixed
import com.example.ui.theme.SecondaryFixedDim
import com.example.ui.theme.Surface
import com.example.ui.theme.SurfaceBright
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import java.util.Locale

@Composable
fun StreamingSettingsScreen(
    uiState: SoundWaveUiState,
    onOpenServerLogin: () -> Unit,
    onToggleCellularTranscoding: () -> Unit,
    onSelectOpusBitrate: (String) -> Unit,
    onSelectBackendCodec: (String) -> Unit,
    onCycleSampleRate: () -> Unit,
    onToggleHardwareAccel: () -> Unit,
    onToggleBitPerfect: () -> Unit,
    onToggleReplayGain: () -> Unit,
    onSelectReplayGainMode: (String) -> Unit,
    onReplayGainPreampChange: (Float) -> Unit,
    onToggleGapless: () -> Unit,
    onCrossfadeChange: (Int) -> Unit,
    onClearCache: () -> Unit,
    onRunDiagnostics: () -> Unit,
    onClearDownloads: () -> Unit = {},
    onThemeModeChange: (String) -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
    onToggleSimpleMode: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onOpenGuide: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .testTag("streaming_settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Server Connection Status Ribbon (Click to open Server Login)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLow)
                    .clickable { onOpenServerLogin() }
                    .padding(16.dp)
                    .testTag("open_server_login_ribbon"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = Secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Secondary)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (uiState.isLoggedIn) uiState.serverHost else "Nepripojené",
                                color = OnSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceContainerHighest)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when {
                                        !uiState.isLoggedIn -> "PRIHLÁSIŤ"
                                        uiState.pingMs >= 0 -> "ONLINE"
                                        else -> "NEDOSTUPNÝ"
                                    },
                                    color = Secondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = uiState.serverInfoText.ifBlank { "Klikni a pripoj sa k Navidrome serveru" },
                            color = Outline,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = SecondaryFixed,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (uiState.pingMs >= 0) "${uiState.pingMs} ms" else "— ms",
                            color = SecondaryFixed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (uiState.serverUrl.startsWith("https://")) "HTTPS" else "HTTP",
                        color = Outline,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Section: VZHĽAD A MOTÍV (Svetlý / Tmavý / Amoled / Systémový + Zjednodušený režim)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLow)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Vzhľad a motív aplikácie",
                        color = OnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 4 Theme Buttons: Tmavý, Svetlý, Amoled, Systém
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf(
                        Triple("DARK", "Tmavý", Icons.Default.DarkMode),
                        Triple("LIGHT", "Svetlý", Icons.Default.LightMode),
                        Triple("AMOLED", "Amoled", Icons.Default.Brightness4),
                        Triple("SYSTEM", "Systém", Icons.Default.BrightnessAuto)
                    )

                    themes.forEach { (mode, label, icon) ->
                        val isSelected = uiState.themeMode == mode
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Primary.copy(alpha = 0.2f) else SurfaceContainerHigh)
                                .clickable { onThemeModeChange(mode) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) Primary else OnSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = label,
                                color = if (isSelected) Primary else OnSurface,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SurfaceContainerHighest)
                )

                // Jazyk aplikácie / Language selector
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Jazyk aplikácie / Language",
                                color = OnSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 4 Language Buttons: Systém, Slovenčina, English, Čeština
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val languages = listOf(
                            Triple("SYSTEM", "Systém", "🌐"),
                            Triple("sk", "Slovenčina", "🇸🇰"),
                            Triple("en", "English", "🇬🇧"),
                            Triple("cs", "Čeština", "🇨🇿")
                        )

                        languages.forEach { (code, label, flag) ->
                            val isSelected = uiState.appLanguage.equals(code, ignoreCase = true)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Primary.copy(alpha = 0.2f) else SurfaceContainerHigh)
                                    .clickable { onLanguageChange(code) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = flag,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = label,
                                    color = if (isSelected) Primary else OnSurface,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SurfaceContainerHighest)
                )

                // Simple Mode Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleSimpleMode() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (uiState.isSimpleMode) SecondaryFixed else OnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Zjednodušené rozhranie",
                                color = OnSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.isSimpleMode)
                                "Zapnuté: Skryté zložité audiofilské parametre pre čistý zážitok"
                            else
                                "Vypnuté: Zobrazujú sa detailné technické audio parametre a FFmpeg",
                            color = OnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = uiState.isSimpleMode,
                        onCheckedChange = { onToggleSimpleMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Primary,
                            checkedTrackColor = PrimaryContainer.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        // Section: RÝCHLE NÁSTROJE (Ekvalizér a Sprievodca)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ekvalizér Card
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerLow)
                        .clickable { onOpenEqualizer() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Ekvalizér & DSP",
                            color = OnSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "10 pásiem & AutoEQ",
                            color = OnSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                // Manuál Card
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerLow)
                        .clickable { onOpenGuide() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = SecondaryFixed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Sprievodca",
                            color = OnSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Klikací manuál",
                            color = OnSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Section 1: Kvalita zvuku a streamovania (Jednoduchý a okamžitý výber)
        item {
            val isRawOriginal = uiState.backendCodec == "FLAC Stream"
            val currentRate = uiState.opusBitrateTarget
            val activeQualitySummary = when {
                isRawOriginal -> "Originál / FLAC (Bez straty kvality – Direct Play)"
                uiState.backendCodec == "MP3 (LAME)" -> "Prekódovanie: MP3 • ${currentRate.replace("k", "")} kbps"
                uiState.backendCodec == "Opus (libopus)" -> "Prekódovanie: Opus • ${currentRate.replace("k", "")} kbps"
                else -> "Prekódovanie: Ogg Vorbis • ${currentRate.replace("k", "")} kbps"
            }
            val hasPlayingTrack = uiState.currentTrack.id.isNotBlank()
            val sourceSuffix = uiState.currentTrack.suffix.uppercase(Locale.ROOT).ifBlank { "NEZNÁMY" }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Kvalita zvuku (Prehrávanie zo servera)",
                            color = OnSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "PREPÍNA NAŽIVO",
                        color = SecondaryFixed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp
                    )
                }

                // Live Stream Monitor Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerHigh)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AKTÍVNY REŽIM STREAMU",
                            color = SecondaryFixed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(PrimaryContainer)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isRawOriginal) "ORIGINÁL (FLAC / RAW)" else "${uiState.backendCodec.substringBefore(" ")} ${currentRate.uppercase()}",
                                color = OnPrimaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Text(
                        text = activeQualitySummary,
                        color = OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (hasPlayingTrack) {
                        Text(
                            text = "Práve hrá: „${uiState.currentTrack.title}“ • Súbor na serveri: $sourceSuffix (${uiState.currentTrack.techLine.ifBlank { "štandard" }})",
                            color = OnSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        if (sourceSuffix == "MP3" && isRawOriginal) {
                            Text(
                                text = "ℹ️ Táto skladba je na tvojom serveri uložená ako MP3. Prepnutie na FLAC nemôže zvýšiť kvalitu súboru, ktorý už je v MP3.",
                                color = SecondaryFixed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 15.sp
                            )
                        }
                    } else {
                        Text(
                            text = "Keď pustíš skladbu a klikneš na ľubovoľnú kvalitu nižšie, zvuk sa okamžite prepne počas hrania.",
                            color = OnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                // 4 Simple One-Click Quality Presets
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainer)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Vyber si kvalitu jedným kliknutím:",
                        color = OnSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val presets = listOf(
                        Triple(
                            "ORIGINÁL / FLAC (Bez straty kvality)",
                            "Odporúčané pre TV a Wi-Fi • Posiela presne pôvodný súbor zo servera bez kompresie",
                            isRawOriginal
                        ) to {
                            onSelectBackendCodec("FLAC Stream")
                        },
                        Triple(
                            "Vysoká kvalita (MP3 • 320 kbps)",
                            "Čistý zvuk a plná kompatibilita • Menšia veľkosť dát ako pri FLAC",
                            !isRawOriginal && uiState.backendCodec == "MP3 (LAME)" && currentRate == "320k"
                        ) to {
                            onSelectBackendCodec("MP3 (LAME)")
                            onSelectOpusBitrate("320k")
                        },
                        Triple(
                            "Štandardná kvalita (MP3 • 128 kbps)",
                            "Ideálne na mobilné dáta alebo bežné Bluetooth reproduktory",
                            !isRawOriginal && uiState.backendCodec == "MP3 (LAME)" && currentRate == "128k"
                        ) to {
                            onSelectBackendCodec("MP3 (LAME)")
                            onSelectOpusBitrate("128k")
                        },
                        Triple(
                            "Nízka kvalita / Test rozdielu (MP3 • 64 kbps)",
                            "Maximálna úspora dát • Ideálne ak chceš hneď ušami počuť rozdiel oproti Originálu",
                            !isRawOriginal && currentRate == "64k"
                        ) to {
                            onSelectBackendCodec("MP3 (LAME)")
                            onSelectOpusBitrate("64k")
                        }
                    )

                    presets.forEach { (info, action) ->
                        val (title, subtitle, selected) = info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Primary.copy(alpha = 0.18f) else SurfaceContainerLow)
                                .clickable { action() }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    color = if (selected) Primary else OnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                imageVector = if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = title,
                                tint = if (selected) Primary else Outline,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Mobile Data Saver Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLow)
                            .clickable { onToggleCellularTranscoding() }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SignalCellularAlt,
                                contentDescription = null,
                                tint = Secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Automaticky šetriť mobilné dáta mimo Wi-Fi",
                                    color = OnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Keď nie si na Wi-Fi, automaticky zníži tok na MP3, aby sa nemíňali dáta",
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = uiState.cellularTranscodingEnabled,
                            onCheckedChange = { onToggleCellularTranscoding() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnPrimary,
                                checkedTrackColor = PrimaryContainer,
                                uncheckedThumbColor = OnSurfaceVariant,
                                uncheckedTrackColor = SurfaceContainerHighest
                            )
                        )
                    }
                }
            }
        }

        // Section 2: Manuálny výber formátu a dátového toku (ak si chceš nastaviť vlastný kodek)
        if (!uiState.isSimpleMode) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainer)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Ručné nastavenie formátu a dátového toku (kbps)",
                            color = OnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CodecRadioCard(
                            title = "Originál (FLAC)",
                            subtitle = "Bez kompresie",
                            selected = uiState.backendCodec == "FLAC Stream",
                            onClick = { onSelectBackendCodec("FLAC Stream") },
                            modifier = Modifier.weight(1f)
                        )
                        CodecRadioCard(
                            title = "MP3 (LAME)",
                            subtitle = "Najlepšia podpora",
                            selected = uiState.backendCodec == "MP3 (LAME)",
                            onClick = { onSelectBackendCodec("MP3 (LAME)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CodecRadioCard(
                            title = "Opus",
                            subtitle = "Moderná kompresia",
                            selected = uiState.backendCodec == "Opus (libopus)",
                            onClick = { onSelectBackendCodec("Opus (libopus)") },
                            modifier = Modifier.weight(1f)
                        )
                        CodecRadioCard(
                            title = "Ogg Vorbis",
                            subtitle = "Alternatívny formát",
                            selected = uiState.backendCodec == "Ogg Vorbis",
                            onClick = { onSelectBackendCodec("Ogg Vorbis") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "Dátový tok pri prekódovaní (MP3 / Opus / Ogg):",
                        color = OnSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerLow)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("64k", "128k", "192k", "256k", "320k").forEach { rate ->
                            val selected = uiState.opusBitrateTarget == rate && uiState.backendCodec != "FLAC Stream"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Primary else Color.Transparent)
                                    .clickable { onSelectOpusBitrate(rate) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rate.replace("k", " kbps"),
                                    color = if (selected) OnPrimary else OnSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Čo nastaviť v Navidrome a prečo niekedy nepočuť rozdiel (Praktické vysvetlenie)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLow)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = SecondaryFixed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Čo nastaviť v Navidrome & prečo nepočuť rozdiel?",
                        color = OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "1. Záleží na tom, v čom je skladba uložená na serveri:\n" +
                        "Ak je pesnička na tvojom disku uložená ako MP3 (napr. 192 alebo 320 kbps), prepnutie na FLAC z nej neurobí štúdiové FLAC. Hore v rámčeku vždy vidíš, či je súbor na serveri skutočne FLAC alebo MP3.",
                    color = OnSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Text(
                    text = "2. Rozdiel medzi FLAC a MP3 320 kbps:\n" +
                        "Na reproduktoroch TV alebo cez Bluetooth slúchadlá (ktoré zvuk samé komprimujú) je rozdiel medzi FLAC a MP3 320 kbps takmer nepočuteľný. Ak si chceš overiť, že prepínanie kvality funguje, prepni vyššie na „Nízka kvalita (64 kbps)“ – rozdiel vo výškach a čineloch budeš počuť okamžite.",
                    color = OnSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Text(
                    text = "3. Treba niečo nastaviť v Navidrome?\n" +
                        "• Ak počúvaš v režime „Originál / FLAC“, v Navidrome netreba nastavovať vôbec nič.\n" +
                        "• Ak chceš, aby Navidrome naozaj prekódoval FLAC súbory na menšie MP3/Opus, otvor svoj Navidrome cez webový prehliadač -> klikni vpravo hore na ikonu používateľa -> Prehrávače (Players) -> nájdi „SoundWave“ a nastav Prekódovanie (Transcoding) na „mp3“.",
                    color = OnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 17.sp
                )
            }
        }

        // Section 4: Vyrovnávanie hlasitosti (ReplayGain)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vyrovnávanie hlasitosti (ReplayGain)",
                            color = OnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Automaticky vyrovná hlasitosť medzi staršími (tichšími) a novšími (hlasnejšími) skladbami",
                            color = OnSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = uiState.replayGainEnabled,
                        onCheckedChange = { onToggleReplayGain() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnPrimary,
                            checkedTrackColor = PrimaryContainer,
                            uncheckedThumbColor = OnSurfaceVariant,
                            uncheckedTrackColor = SurfaceContainerHighest
                        )
                    )
                }

                if (uiState.replayGainEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Doladenie hlasitosti (Pre-amp)",
                            color = OnSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(
                            text = String.format(Locale.US, "%s%.1f dB", if (uiState.replayGainPreampDb > 0f) "+" else "", uiState.replayGainPreampDb),
                            color = Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = uiState.replayGainPreampDb,
                        onValueChange = onReplayGainPreampChange,
                        valueRange = -6f..6f,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryFixed,
                            activeTrackColor = Primary,
                            inactiveTrackColor = SurfaceContainerHighest
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        // Section 5: Vyrovnávacia pamäť & Stiahnuté skladby
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Úložisko a vyrovnávacia pamäť",
                    color = OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceContainerHigh)
                        .clickable { onClearCache() }
                        .padding(12.dp)
                        .testTag("clear_cache_btn"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = Outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Vymazať dočasnú vyrovnávaciu pamäť",
                                color = OnSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = uiState.cacheSizeText,
                                color = Outline,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Outline,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceContainerHigh)
                        .clickable { onClearDownloads() }
                        .padding(12.dp)
                        .testTag("clear_downloads_btn"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Vymazať stiahnuté offline skladby",
                                color = OnSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${uiState.downloadedTracks.size} skladieb zaberá ${uiState.downloadedSizeText}",
                                color = Outline,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Section 6: Testovacie tlačidlo (Diagnostika streamu a servera)
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(14.dp, RoundedCornerShape(50), spotColor = PrimaryContainer)
                        .clip(RoundedCornerShape(50))
                        .background(Primary)
                        .clickable { onRunDiagnostics() }
                        .testTag("test_diagnostics_btn"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isTestingDiagnostics) Icons.Default.Sync else Icons.Default.NetworkCheck,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isTestingDiagnostics) "Testujem spojenie a formát..." else "Otestovať čo presne posiela server (Test kvality)",
                        color = OnPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                AnimatedVisibility(visible = uiState.diagnosticResultVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerHigh)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = uiState.diagnosticResultText,
                            color = OnSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Section: O aplikácii SoundWave
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLow)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "O aplikácii SoundWave",
                        color = OnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Verzia 25.0 (Build 25) • Pripravené pre Google TV a smartfóny",
                    color = OnSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = "Podpora Navidrome / Subsonic API v1.16.1 • Bezstratové FLAC & živé prepínanie kvality • 10-pásmový DSP ekvalizér & AutoEQ profily",
                    color = Outline,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun CodecRadioCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SurfaceContainerHigh else SurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = OnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Outline,
                fontSize = 10.sp
            )
        }
        Icon(
            imageVector = if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) Primary else Outline,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ServerLoginScreen(
    uiState: SoundWaveUiState,
    onServerUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleTokenHint: () -> Unit,
    onToggleAdvancedAccordion: () -> Unit,
    onToggleOfflineCache: () -> Unit,
    onTogglePreferFlac: () -> Unit,
    onCycleTranscodeOption: () -> Unit,
    onConnectServer: () -> Unit,
    onFillDemoServer: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .testTag("server_login_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Visual Hero & Vinyl Emblem
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .shadow(20.dp, CircleShape, ambientColor = PrimaryContainer, spotColor = PrimaryContainer)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerLowest),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainer.copy(alpha = 0.5f))
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerLowest)
                        )
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = PrimaryFixed,
                            modifier = Modifier.size(28.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Pripojenie k Navidrome",
                    color = OnSurface,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Zadajte údaje vášho osobného hudobného servera pre streamovanie bezstratového zvuku (FLAC/MP3).",
                    color = OnSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }

        // Server Address Input Field
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "URL Servera (Server Address)",
                        color = OnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "SUBSONIC V1.16+",
                        color = Primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerLow)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = uiState.serverUrl,
                        onValueChange = onServerUrlChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = PlusJakartaSans,
                            color = OnSurface,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(Primary),
                        decorationBox = { innerTextField ->
                            if (uiState.serverUrl.isEmpty()) {
                                Text(
                                    text = "https://vas-server.sk:4533",
                                    color = OnSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 14.sp,
                                    fontFamily = PlusJakartaSans
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("login_server_url_input")
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = SecondaryFixedDim,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Podporuje SSL (HTTPS), Tailscale aj lokálnu sieť",
                        color = OnSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Username Input Field
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Používateľské meno",
                    color = OnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerLow)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = uiState.username,
                        onValueChange = onUsernameChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = PlusJakartaSans,
                            color = OnSurface,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(Primary),
                        decorationBox = { innerTextField ->
                            if (uiState.username.isEmpty()) {
                                Text(
                                    text = "Používateľské meno",
                                    color = OnSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 14.sp,
                                    fontFamily = PlusJakartaSans
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("login_username_input")
                    )
                }
            }
        }

        // Password / App Token Field
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Heslo alebo API Token",
                        color = OnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Čo je App Token?",
                        color = SecondaryFixed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onToggleTokenHint() }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerLow)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        singleLine = true,
                        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        textStyle = TextStyle(
                            fontFamily = PlusJakartaSans,
                            color = OnSurface,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(Primary),
                        decorationBox = { innerTextField ->
                            if (uiState.password.isEmpty()) {
                                Text(
                                    text = "Heslo k vášmu účtu",
                                    color = OnSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 14.sp,
                                    fontFamily = PlusJakartaSans
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("login_password_input")
                    )
                    IconButton(
                        onClick = onTogglePasswordVisibility,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Prepnúť viditeľnosť hesla",
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = uiState.showTokenHint) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SecondaryContainer.copy(alpha = 0.12f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Zadaj heslo svojho Navidrome účtu. Aplikácia heslo neukladá – po úspešnom prihlásení si zapamätá len bezpečnostný token (md5 hash s náhodnou soľou).",
                            color = Secondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Advanced Settings Collapsible Accordion
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainerLow.copy(alpha = 0.75f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleAdvancedAccordion() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Pokročilé nastavenia zvuku",
                            color = OnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = if (uiState.showAdvancedAudioAccordion) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = uiState.showAdvancedAudioAccordion) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Offline vyrovnávacia pamäť",
                                    color = OnSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Ukladať práve počúvané skladby",
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = uiState.loginOfflineCacheEnabled,
                                onCheckedChange = { onToggleOfflineCache() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OnPrimaryContainer,
                                    checkedTrackColor = PrimaryContainer
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Preferovať bezstratové FLAC",
                                    color = OnSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Streamovať 24-bit/96kHz bez kompresie",
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = uiState.loginPreferFlacEnabled,
                                onCheckedChange = { onTogglePreferFlac() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OnPrimaryContainer,
                                    checkedTrackColor = PrimaryContainer
                                )
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Maximálny dátový tok (Transcoding)",
                                color = OnSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainer)
                                    .clickable { onCycleTranscodeOption() }
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.loginTranscodeOption,
                                    color = OnSurface,
                                    fontSize = 12.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.UnfoldMore,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Primary Connect Button + Demo Server Link
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val btnBg = if (uiState.isServerConnectedSuccess) SecondaryContainer else PrimaryContainer
                val btnFg = if (uiState.isServerConnectedSuccess) OnSecondaryContainer else OnPrimaryContainer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(18.dp, RoundedCornerShape(50), spotColor = PrimaryContainer)
                        .clip(RoundedCornerShape(50))
                        .background(btnBg)
                        .clickable { onConnectServer() }
                        .testTag("login_connect_btn"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isServerConnectedSuccess) Icons.Default.CheckCircle else Icons.Default.Bolt,
                        contentDescription = null,
                        tint = btnFg,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            uiState.isConnectingServer -> "Pripájam..."
                            uiState.isServerConnectedSuccess -> "Pripojené úspešne!"
                            else -> "Pripojiť k serveru"
                        },
                        color = btnFg,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Vyskúšať demo server",
                        color = OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onFillDemoServer() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("login_demo_server_btn")
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHighest)
                    )
                    Text(
                        text = "Pomocník",
                        color = OnSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleTokenHint() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (uiState.connectionError != null) {
                    Text(
                        text = uiState.connectionError ?: "",
                        color = PrimaryFixedDim,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_error_text")
                    )
                }

                // Live connection status pill
                AnimatedVisibility(visible = uiState.isConnectingServer || uiState.isServerConnectedSuccess) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceContainer)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isServerConnectedSuccess) Icons.Default.CloudDone else Icons.Default.Sync,
                            contentDescription = null,
                            tint = SecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (uiState.isServerConnectedSuccess) {
                                    "Prihlásenie overené"
                                } else {
                                    "Overujem prihlásenie na serveri..."
                                },
                                color = OnSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.isServerConnectedSuccess) {
                                    buildString {
                                        if (uiState.pingMs >= 0) append("Ping: ${uiState.pingMs} ms")
                                        if (uiState.serverInfoText.isNotBlank()) {
                                            if (isNotEmpty()) append(" • ")
                                            append(uiState.serverInfoText)
                                        }
                                    }
                                } else {
                                    "Čakám na odpoveď servera…"
                                },
                                color = OnSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Security End-to-End Footnote
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainerLowest.copy(alpha = 0.65f))
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PrimaryFixedDim,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(18.dp)
                )
                Text(
                    text = "Heslo sa neukladá. Aplikácia si zapamätá len prihlasovací token (md5 hash s náhodnou soľou) v súkromnom úložisku aplikácie, ktoré je vylúčené zo zálohovania. Komunikuje výhradne s adresou tvojho servera. Poznámka: http:// spojenie nie je šifrované – v domácej sieti alebo cez Tailscale je to bežné, mimo nej používaj https://.",
                    color = OnSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        if (uiState.isLoggedIn) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(50))
                        .background(SurfaceContainerHigh)
                        .clickable { onLogout() }
                        .testTag("login_logout_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Odhlásiť sa a zabudnúť server",
                        color = OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
