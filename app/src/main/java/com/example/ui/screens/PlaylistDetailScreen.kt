package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.example.ui.components.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AlbumItem
import com.example.data.TrackItem
import com.example.data.formatTrackSubtitle
import com.example.ui.SoundWaveUiState
import com.example.ui.components.AnimatedEqualizerBars
import com.example.ui.components.NetImage
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OnPrimaryContainer
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.Outline
import com.example.ui.theme.Primary
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.Secondary
import com.example.ui.theme.SecondaryFixed
import com.example.ui.theme.Surface
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.ui.graphics.SolidColor
import com.example.data.ArtistItem
import com.example.data.SmartGenreCatalog
import com.example.data.SmartGenreSpec
import com.example.data.cleanAlbumName
import com.example.data.cleanArtistName
import com.example.data.plural
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val DetailLosslessSuffixes = setOf("flac", "alac", "wav", "aiff", "aif", "ape", "wv", "dsf", "dff")

@Composable
fun PlaylistDetailScreen(
    uiState: SoundWaveUiState,
    onTrackClick: (TrackItem, List<TrackItem>) -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onTrackLikeToggle: (String) -> Unit,
    onOpenExplore: () -> Unit,
    onOpenStreamingSettings: () -> Unit,
    onOpenTrackMenu: (TrackItem) -> Unit = {},
    onAlbumClick: (AlbumItem) -> Unit = {},
    onArtistClick: (TrackItem) -> Unit = {}
) {
    val detail = uiState.detail
    if (detail == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Surface),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Nič na zobrazenie", color = OnSurfaceVariant, fontSize = 14.sp)
        }
        return
    }
    val allTracks = detail.tracks

    // Build complete list of albums / soundtracks / works off the Main UI Thread
    val artistAlbums by produceState(
        initialValue = detail.albums,
        key1 = detail.id,
        key2 = detail.albums,
        key3 = allTracks
    ) {
        value = withContext(Dispatchers.Default) {
            if (detail.albums.isNotEmpty()) {
                detail.albums.distinctBy { it.id }
            } else if (detail.kind == "Interpret" || detail.kind == "Žáner" || detail.kind == "Playlist" || detail.kind == "Zbierka") {
                allTracks
                    .groupBy { t ->
                        val cleanAlb = t.album.trim()
                        val hasValidAlbum = cleanAlb.isNotBlank() &&
                            !cleanAlb.equals("Unknown Album", ignoreCase = true) &&
                            !cleanAlb.equals("Neznámy album", ignoreCase = true)
                        when {
                            t.albumId.isNotBlank() && hasValidAlbum -> "al:${t.albumId}"
                            hasValidAlbum -> "al_group:$cleanAlb"
                            else -> {
                                val cleanArt = t.artist.trim().ifBlank { "Rôzne" }
                                "al_group:$cleanArt"
                            }
                        }
                    }
                    .map { (key, groupTracks) ->
                        val first = groupTracks.first()
                        val cleanAlb = first.album.trim()
                        val hasValidAlbum = cleanAlb.isNotBlank() &&
                            !cleanAlb.equals("Unknown Album", ignoreCase = true) &&
                            !cleanAlb.equals("Neznámy album", ignoreCase = true)
                        val title = if (hasValidAlbum) cleanAlb else first.artist.trim().ifBlank { "Nezaradené" }
                        AlbumItem(
                            id = key,
                            title = title,
                            subtitle = if (first.year > 0) "${first.artist.trim()} • ${first.year}" else first.artist.trim(),
                            techSpec = "${groupTracks.size} ${plural(groupTracks.size, "skladba", "skladby", "skladieb")}",
                            badge = first.badge,
                            imageUrl = first.imageUrl
                        )
                    }
                    .distinctBy { it.id }
                    .takeIf { it.size > 1 || detail.kind == "Interpret" || detail.kind == "Žáner" }
                    .orEmpty()
            } else {
                emptyList()
            }
        }
    }

    // Build list of artists/composers off the Main UI Thread
    val collectionArtists by produceState(
        initialValue = emptyList<Pair<ArtistItem, TrackItem>>(),
        key1 = detail.id,
        key2 = detail.kind,
        key3 = allTracks
    ) {
        value = withContext(Dispatchers.Default) {
            if (detail.kind == "Interpret" || allTracks.isEmpty()) {
                emptyList()
            } else {
                allTracks
                    .groupBy { t ->
                        t.artist.trim()
                            .ifBlank {
                                if (t.title.contains(" - ")) t.title.substringBefore(" - ").trim() else "Neznámy interpret"
                            }
                    }
                    .filter { (name, _) -> name.isNotBlank() }
                    .map { (name, groupTracks) ->
                        val first = groupTracks.first()
                        ArtistItem(
                            id = if (first.artistId.isNotBlank()) "ar:${first.artistId}" else "ar_name:$name",
                            name = name,
                            trackCount = "${groupTracks.size} ${plural(groupTracks.size, "skladba", "skladby", "skladieb")}",
                            imageUrl = first.imageUrl
                        ) to first
                    }
                    .distinctBy { it.first.id }
                    .sortedBy { it.first.name.lowercase() }
                    .takeIf { it.size > 1 }
                    .orEmpty()
            }
        }
    }

    var selectedAlbumFilterId by remember(detail.id) { mutableStateOf<String?>(null) }
    var selectedArtistFilterName by remember(detail.id) { mutableStateOf<String?>(null) }
    var selectedSubGenreId by remember(detail.id) { mutableStateOf<String?>(null) }
    var filterQuery by remember(detail.id) { mutableStateOf("") }
    // View modes: "SKLADBY" (overview + tracks), "ALBUMY" (full album/soundtrack grid), "INTERPRETI" (full artist grid)
    var viewMode by remember(detail.id) { mutableStateOf("SKLADBY") }

    // Compute available sub-genre slices on Dispatchers.Default so Main UI Thread never freezes!
    val subGenreChips by produceState(
        initialValue = emptyList<Pair<SmartGenreSpec, Int>>(),
        key1 = detail.id,
        key2 = detail.kind,
        key3 = allTracks
    ) {
        value = withContext(Dispatchers.Default) {
            if (detail.kind == "Žáner" && allTracks.size >= 30) {
                val sample = if (allTracks.size > 180) allTracks.take(180) else allTracks
                val currentSpec = SmartGenreCatalog.findSpec(detail.title)
                SmartGenreCatalog.specs
                    .filter { it.id != currentSpec?.id }
                    .mapNotNull { spec ->
                        val count = sample.count { SmartGenreCatalog.matchesTrack(it, spec) }
                        if (count >= 3) spec to count else null
                    }
                    .sortedByDescending { it.second }
            } else {
                emptyList()
            }
        }
    }

    val activeSubGenreSpec: SmartGenreSpec? = remember(selectedSubGenreId, subGenreChips) {
        subGenreChips.firstOrNull { it.first.id == selectedSubGenreId }?.first
    }

    val q = filterQuery.trim()

    val tracks = remember(allTracks, selectedAlbumFilterId, selectedArtistFilterName, activeSubGenreSpec, artistAlbums, q) {
        var result = allTracks
        if (activeSubGenreSpec != null) {
            result = result.filter { SmartGenreCatalog.matchesTrack(it, activeSubGenreSpec) }
        }
        selectedAlbumFilterId?.let { selId ->
            val rawAlbumId = selId.removePrefix("al:").removePrefix("al_group:")
            val selectedAlbumTitle = artistAlbums.find { it.id == selId }?.title ?: rawAlbumId
            result = result.filter { t ->
                val cleanAlb = t.album.trim()
                val cleanArt = t.artist.trim()
                (selId.startsWith("al:") && t.albumId == rawAlbumId) ||
                    cleanAlb.equals(selectedAlbumTitle, ignoreCase = true) ||
                    (cleanAlb.isBlank() && cleanArt.equals(selectedAlbumTitle, ignoreCase = true)) ||
                    (cleanAlb.isBlank() && t.title.startsWith("$selectedAlbumTitle -", ignoreCase = true))
            }
        }
        selectedArtistFilterName?.let { targetArtist ->
            result = result.filter { t ->
                val cleanArt = t.artist.trim()
                cleanArt.equals(targetArtist, ignoreCase = true) ||
                    t.artist.contains(targetArtist, ignoreCase = true) ||
                    t.title.startsWith("$targetArtist -", ignoreCase = true)
            }
        }
        if (q.isNotEmpty()) {
            result = result.filter { t ->
                t.title.contains(q, ignoreCase = true) ||
                    t.artist.contains(q, ignoreCase = true) ||
                    t.album.contains(q, ignoreCase = true)
            }
        }
        result
    }

    val filteredAlbums = remember(artistAlbums, activeSubGenreSpec, tracks, q) {
        var base = if (activeSubGenreSpec == null) {
            artistAlbums
        } else {
            val trackAlbumIds = tracks.mapNotNull { it.albumId.takeIf { id -> id.isNotBlank() } }.toSet()
            val trackAlbumTitles = tracks.map { it.album.trim().lowercase() }.toSet()
            artistAlbums.filter { alb ->
                val rawId = alb.id.removePrefix("al:").removePrefix("al_group:")
                rawId in trackAlbumIds ||
                    alb.title.trim().lowercase() in trackAlbumTitles ||
                    SmartGenreCatalog.matchesAlbum(alb, activeSubGenreSpec)
            }
        }
        if (q.isNotEmpty()) {
            base = base.filter {
                it.title.contains(q, ignoreCase = true) || it.subtitle.contains(q, ignoreCase = true)
            }
        }
        base
    }

    val filteredArtists = remember(collectionArtists, activeSubGenreSpec, tracks, q) {
        var base = if (activeSubGenreSpec == null) {
            collectionArtists
        } else {
            val matchingArtists = tracks.map { it.artist.trim().lowercase() }.toSet()
            collectionArtists.filter { (artist, sampleTrack) ->
                artist.name.trim().lowercase() in matchingArtists ||
                    SmartGenreCatalog.matchesTrack(sampleTrack, activeSubGenreSpec)
            }
        }
        if (q.isNotEmpty()) {
            base = base.filter { (artist, _) ->
                artist.name.contains(q, ignoreCase = true)
            }
        }
        base
    }

    val qualityLabel = remember(tracks) {
        when {
            tracks.isEmpty() -> null
            tracks.take(25).any { it.bitDepth >= 24 } -> "HI-RES LOSSLESS"
            tracks.take(25).all { it.suffix in DetailLosslessSuffixes } -> "LOSSLESS"
            else -> null
        }
    }

    val albumsSectionTitle = when {
        detail.title.contains("Soundtrack", ignoreCase = true) -> "Z čoho to je (Filmy / Hry / Albumy)"
        detail.kind == "Žáner" -> "Albumy a diela v žánri"
        detail.kind == "Interpret" -> "Albumy interpreta"
        else -> "Albumy vo výbere"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .testTag("playlist_detail_screen"),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // In-Page Utility Sub-bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = Secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = detail.kind.uppercase(),
                        color = Secondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UtilityCircleButton(icon = Icons.Default.Search, onClick = onOpenExplore)
                    UtilityCircleButton(icon = Icons.Default.Cast, onClick = onOpenStreamingSettings)
                }
            }
        }

        // Hero Section: cover + badges + actions
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(if (artistAlbums.isNotEmpty()) 156.dp else 208.dp)
                        .shadow(24.dp, RoundedCornerShape(16.dp), ambientColor = PrimaryContainer, spotColor = PrimaryContainer)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerHigh)
                ) {
                    NetImage(
                        url = detail.coverUrl,
                        contentDescription = detail.title,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (qualityLabel != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            SurfaceContainerLowest.copy(alpha = 0.75f)
                                        )
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceContainerLowest.copy(alpha = 0.9f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Secondary)
                            )
                            Text(
                                text = qualityLabel,
                                color = Secondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = detail.title,
                    color = OnSurface,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center
                )
                if (detail.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val canOpenAlbumArtist = detail.kind == "Album" && allTracks.isNotEmpty()
                    Row(
                        modifier = if (canOpenAlbumArtist) {
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SurfaceContainerHigh.copy(alpha = 0.7f))
                                .clickable { onArtistClick(allTracks.first()) }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        } else {
                            Modifier
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (canOpenAlbumArtist) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = SecondaryFixed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = detail.subtitle,
                            color = SecondaryFixed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        if (canOpenAlbumArtist) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Otvoriť interpreta",
                                tint = SecondaryFixed,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Server & format badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(SurfaceContainerHigh)
                            .clickable { onOpenStreamingSettings() }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${uiState.serverHost} (Navidrome)",
                            color = OnSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val firstTech = tracks.firstOrNull()?.techLine.orEmpty()
                    if (firstTech.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SurfaceContainerHigh)
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Secondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = firstTech,
                                color = Secondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = detail.metaLine,
                    color = OnSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Main Action Buttons
                val isFiltered = selectedAlbumFilterId != null || selectedArtistFilterName != null || selectedSubGenreId != null || q.isNotEmpty()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .shadow(14.dp, RoundedCornerShape(50), spotColor = PrimaryContainer)
                            .clip(RoundedCornerShape(50))
                            .background(PrimaryContainer)
                            .clickable {
                                if (isFiltered && tracks.isNotEmpty()) {
                                    onTrackClick(tracks.first(), tracks)
                                } else {
                                    onPlayAll(false)
                                }
                            }
                            .testTag("playlist_play_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = OnPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFiltered) "Prehrať výber (${tracks.size})" else "Prehrať (${allTracks.size})",
                            color = OnPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(50))
                            .background(SurfaceContainerHighest)
                            .clickable {
                                if (isFiltered && tracks.isNotEmpty()) {
                                    val shuffled = tracks.shuffled()
                                    onTrackClick(shuffled.first(), shuffled)
                                } else {
                                    onPlayAll(true)
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = Secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Náhodne",
                            color = OnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // View Mode Switcher + In-Collection Filter Search (for Genres, Soundtracks, Artists, Large Playlists)
        if (artistAlbums.isNotEmpty() || collectionArtists.isNotEmpty() || allTracks.size > 15) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Big TV D-pad & Touch friendly mode tabs
                    if (artistAlbums.isNotEmpty() || collectionArtists.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isTracksMode = viewMode == "SKLADBY"
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isTracksMode) PrimaryContainer else SurfaceContainerHigh)
                                    .border(
                                        1.dp,
                                        if (isTracksMode) Primary else Outline.copy(alpha = 0.25f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewMode = "SKLADBY" }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription = null,
                                    tint = if (isTracksMode) OnPrimaryContainer else OnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Skladby (${tracks.size})",
                                    color = if (isTracksMode) OnPrimaryContainer else OnSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (artistAlbums.isNotEmpty()) {
                                val isAlbumsMode = viewMode == "ALBUMY"
                                Row(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isAlbumsMode) PrimaryContainer else SurfaceContainerHigh)
                                        .border(
                                            1.dp,
                                            if (isAlbumsMode) Primary else SecondaryFixed.copy(alpha = 0.4f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewMode = "ALBUMY" }
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Album,
                                        contentDescription = null,
                                        tint = if (isAlbumsMode) OnPrimaryContainer else SecondaryFixed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (detail.kind == "Žáner") {
                                            "Z čoho to je / Albumy (${filteredAlbums.size})"
                                        } else {
                                            "Albumy (${filteredAlbums.size})"
                                        },
                                        color = if (isAlbumsMode) OnPrimaryContainer else OnSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (collectionArtists.isNotEmpty()) {
                                val isArtistsMode = viewMode == "INTERPRETI"
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isArtistsMode) PrimaryContainer else SurfaceContainerHigh)
                                        .border(
                                            1.dp,
                                            if (isArtistsMode) Primary else Outline.copy(alpha = 0.25f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewMode = "INTERPRETI" }
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isArtistsMode) OnPrimaryContainer else SecondaryFixed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Interpreti (${filteredArtists.size})",
                                        color = if (isArtistsMode) OnPrimaryContainer else OnSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Sub-genre / Style chips inside large genres (e.g., splitting "Rock" into Power Metal, Heavy Metal, SK & CZ, etc.)
                    if (subGenreChips.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            item {
                                val isAllSelected = selectedSubGenreId == null
                                Box(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isAllSelected) SecondaryFixed else SurfaceContainerHigh)
                                        .border(
                                            1.dp,
                                            if (isAllSelected) SecondaryFixed else Outline.copy(alpha = 0.3f),
                                            RoundedCornerShape(50)
                                        )
                                        .clickable { selectedSubGenreId = null }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Všetky štýly (${allTracks.size})",
                                        color = if (isAllSelected) SurfaceContainerLowest else OnSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            items(subGenreChips, key = { it.first.id }) { (spec, count) ->
                                val isSelected = selectedSubGenreId == spec.id
                                Box(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isSelected) SecondaryFixed else SurfaceContainerHigh)
                                        .border(
                                            1.dp,
                                            if (isSelected) SecondaryFixed else Outline.copy(alpha = 0.3f),
                                            RoundedCornerShape(50)
                                        )
                                        .clickable {
                                            selectedSubGenreId = if (isSelected) null else spec.id
                                        }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${spec.title} ($count)",
                                        color = if (isSelected) SurfaceContainerLowest else OnSurface,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // In-collection search/filter bar
                    var isFilterFocused by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(50))
                            .background(SurfaceContainerHigh)
                            .then(
                                if (isFilterFocused) {
                                    Modifier.border(3.dp, Color(0xFFFFEA00), RoundedCornerShape(50))
                                } else {
                                    Modifier
                                }
                            )
                            .onFocusChanged { isFilterFocused = it.hasFocus }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Filtrovať",
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = filterQuery,
                            onValueChange = { filterQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = OnSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(Primary),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (filterQuery.isEmpty()) {
                                    Text(
                                        text = "Filtrovať v „${detail.title}“ (film, hra, album, kapela, skladba)…",
                                        color = OnSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (filterQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { filterQuery = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Vyčistiť",
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Active filter banner (when user picked a specific Soundtrack/Album or Artist)
                    if (selectedAlbumFilterId != null || selectedArtistFilterName != null) {
                        val activeAlbumObj = artistAlbums.find { it.id == selectedAlbumFilterId }
                        val activeLabel = activeAlbumObj?.title ?: selectedArtistFilterName.orEmpty()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SecondaryFixed.copy(alpha = 0.16f))
                                .border(1.dp, SecondaryFixed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (selectedAlbumFilterId != null) Icons.Default.Album else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = SecondaryFixed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = if (selectedAlbumFilterId != null) "VYBRANÝ ALBUM / DIELO" else "VYBRANÝ INTERPRET",
                                        color = SecondaryFixed,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "$activeLabel (${tracks.size} ${plural(tracks.size, "skladba", "skladby", "skladieb")})",
                                        color = OnSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (activeAlbumObj != null && activeAlbumObj.id.startsWith("al:")) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(PrimaryContainer)
                                            .clickable { onAlbumClick(activeAlbumObj) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Celý album",
                                            color = OnPrimaryContainer,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = OnPrimaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(SurfaceContainerHighest)
                                        .clickable {
                                            selectedAlbumFilterId = null
                                            selectedArtistFilterName = null
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Zrušiť filter",
                                        tint = OnSurface,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Všetko",
                                        color = OnSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // MODE 1: Full Album / Soundtrack Grid ("ALBUMY")
        if (viewMode == "ALBUMY" && artistAlbums.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$albumsSectionTitle (${filteredAlbums.size})",
                        color = OnSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Klikni pre výber (OK)",
                        color = SecondaryFixed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            val rows = filteredAlbums.chunked(2)
            itemsIndexed(rows, key = { idx, row -> "album_row_${idx}_${row.first().id}" }) { _, rowAlbums ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowAlbums.forEach { album ->
                        ArtistAlbumRowCard(
                            album = album,
                            onClick = {
                                if (detail.kind == "Žáner" || album.id.startsWith("al_group:")) {
                                    // Filter right inside the genre/soundtrack or open album if no local tracks match
                                    val rawId = album.id.removePrefix("al:").removePrefix("al_group:")
                                    val hasLocal = allTracks.any {
                                        it.albumId == rawId ||
                                            it.album.equals(album.title, ignoreCase = true) ||
                                            it.artist.equals(album.title, ignoreCase = true)
                                    }
                                    if (hasLocal) {
                                        selectedAlbumFilterId = album.id
                                        viewMode = "SKLADBY"
                                    } else {
                                        onAlbumClick(album)
                                    }
                                } else {
                                    onAlbumClick(album)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowAlbums.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // MODE 2: Full Artist / Composer Grid ("INTERPRETI")
        if (viewMode == "INTERPRETI" && collectionArtists.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Interpreti / Autori v žánri (${filteredArtists.size})",
                        color = OnSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Klikni pre skladby alebo profil",
                        color = SecondaryFixed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            val artistRows = filteredArtists.chunked(2)
            itemsIndexed(artistRows, key = { idx, row -> "artist_row_${idx}_${row.first().first.id}" }) { _, rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { (artistItem, sampleTrack) ->
                        ArtistAlbumRowCard(
                            album = AlbumItem(
                                id = artistItem.id,
                                title = artistItem.name,
                                subtitle = "Interpret / Kapela",
                                techSpec = artistItem.trackCount,
                                badge = "",
                                imageUrl = artistItem.imageUrl
                            ),
                            onClick = {
                                selectedArtistFilterName = artistItem.name
                                viewMode = "SKLADBY"
                            },
                            onSecondaryAction = { onArtistClick(sampleTrack) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // MODE 3: Default Overview + All Tracks ("SKLADBY")
        if (viewMode == "SKLADBY") {
            if (filteredAlbums.isNotEmpty() && selectedAlbumFilterId == null && selectedArtistFilterName == null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    imageVector = Icons.Default.Album,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = albumsSectionTitle,
                                    color = OnSurface,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(SurfaceContainerHigh)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = filteredAlbums.size.toString(),
                                        color = SecondaryFixed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (filteredAlbums.size > 4) {
                                Text(
                                    text = "Všetky (${filteredAlbums.size}) ➔",
                                    color = SecondaryFixed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewMode = "ALBUMY" }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Show up to 4 albums (2 rows) in overview so Android TV composes instantaneously and D-pad reaches tracks in 2 clicks
                        val previewAlbums = filteredAlbums.take(4)
                        previewAlbums.chunked(2).forEach { rowAlbums ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowAlbums.forEach { album ->
                                    ArtistAlbumRowCard(
                                        album = album,
                                        onClick = {
                                            if (detail.kind == "Žáner" || album.id.startsWith("al_group:")) {
                                                val rawId = album.id.removePrefix("al:").removePrefix("al_group:")
                                                val hasLocal = allTracks.any {
                                                    it.albumId == rawId ||
                                                        cleanAlbumName(it.album).equals(album.title, ignoreCase = true) ||
                                                        cleanArtistName(it.artist).equals(album.title, ignoreCase = true)
                                                }
                                                if (hasLocal) {
                                                    selectedAlbumFilterId = album.id
                                                } else {
                                                    onAlbumClick(album)
                                                }
                                            } else {
                                                onAlbumClick(album)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowAlbums.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        if (filteredAlbums.size > previewAlbums.size) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceContainerHigh)
                                    .border(1.dp, SecondaryFixed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable { viewMode = "ALBUMY" }
                                    .padding(vertical = 10.dp, horizontal = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = null,
                                    tint = SecondaryFixed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Zobraziť všetky diela / albumy na výber (${filteredAlbums.size})",
                                    color = SecondaryFixed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = SecondaryFixed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Quick filter chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            item {
                                val isAllSelected = selectedAlbumFilterId == null
                                Box(
                                    modifier = Modifier
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isAllSelected) PrimaryContainer else SurfaceContainerHigh)
                                        .clickable { selectedAlbumFilterId = null }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Všetky skladby (${allTracks.size})",
                                        color = if (isAllSelected) OnPrimaryContainer else OnSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            itemsIndexed(filteredAlbums.take(15), key = { idx, alb -> "filter_${idx}_${alb.id}" }) { _, album ->
                                val isSelected = selectedAlbumFilterId == album.id
                                Box(
                                    modifier = Modifier
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isSelected) PrimaryContainer else SurfaceContainerHigh)
                                        .clickable {
                                            selectedAlbumFilterId = if (isSelected) null else album.id
                                        }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = album.title,
                                        color = if (isSelected) OnPrimaryContainer else OnSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tracklist Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, start = 16.dp, end = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val activeAlbumTitle = artistAlbums.find { it.id == selectedAlbumFilterId }?.title
                                ?: selectedArtistFilterName
                            Text(
                                text = if (activeAlbumTitle != null) "Skladby • $activeAlbumTitle" else "Všetky skladby",
                                color = OnSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(SurfaceContainerHigh)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tracks.size.toString(),
                                    color = SecondaryFixed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = SurfaceContainerHigh.copy(alpha = 0.45f))
                    Spacer(modifier = Modifier.height(6.dp))

                    if (detail.isLoading) {
                        Text(
                            text = "Načítavam všetky skladby a albumy zo servera…",
                            color = OnSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else if (detail.error != null) {
                        Text(
                            text = detail.error ?: "",
                            color = OnSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else if (tracks.isEmpty()) {
                        Text(
                            text = "Žiadne skladby",
                            color = OnSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }

            itemsIndexed(tracks, key = { index, t -> "$index:${t.id}" }) { index, track ->
                val isActive = uiState.currentTrack.id == track.id
                val isLiked = track.id in uiState.likedTrackIds
                val isDownloaded = track.id in uiState.downloadedTrackIds
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                    PlaylistTrackItemRow(
                        index = index + 1,
                        track = track,
                        isActive = isActive,
                        isPlaying = isActive && uiState.isPlaying,
                        isLiked = isLiked,
                        isDownloaded = isDownloaded,
                        showArtistButton = detail.kind != "Interpret",
                        onClick = { onTrackClick(track, tracks) },
                        onArtistClick = { onArtistClick(track) },
                        onLikeClick = { onTrackLikeToggle(track.id) },
                        onMenuClick = { onOpenTrackMenu(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistAlbumRowCard(
    album: AlbumItem,
    onClick: () -> Unit,
    onSecondaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, Outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
            .testTag("artist_album_${album.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceContainerHighest)
        ) {
            NetImage(
                url = album.imageUrl,
                contentDescription = album.title,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                color = OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (album.subtitle.isNotBlank()) {
                Text(
                    text = album.subtitle,
                    color = SecondaryFixed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (album.techSpec.isNotBlank()) {
                Text(
                    text = album.techSpec,
                    color = OnSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (onSecondaryAction != null) {
            IconButton(
                onClick = onSecondaryAction,
                modifier = Modifier
                    .size(32.dp)
                    .focusProperties { canFocus = false }
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Celý profil interpreta",
                    tint = SecondaryFixed,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Otvoriť album",
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun UtilityCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(SurfaceContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OnSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PlaylistTrackItemRow(
    index: Int,
    track: TrackItem,
    isActive: Boolean,
    isPlaying: Boolean,
    isLiked: Boolean,
    isDownloaded: Boolean,
    showArtistButton: Boolean = false,
    onClick: () -> Unit,
    onArtistClick: () -> Unit = {},
    onLikeClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) SurfaceContainerLow else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier.width(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isActive) {
                    AnimatedEqualizerBars(
                        isAnimating = isPlaying,
                        barColor = Primary,
                        secondaryBarColor = Primary,
                        barWidth = 3.dp,
                        maxHeight = 16.dp
                    )
                } else {
                    Text(
                        text = index.toString(),
                        color = OnSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainerHighest)
            ) {
                NetImage(
                    url = track.imageUrl,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isActive) Primary else OnSurface,
                    fontSize = 15.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (track.badge.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .wrapContentWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isActive) Primary.copy(alpha = 0.2f)
                                    else SurfaceContainerHighest
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = track.badge,
                                color = when {
                                    isActive -> Primary
                                    track.isHiResBadgeCyan -> Secondary
                                    else -> OnSurfaceVariant
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Text(
                        text = formatTrackSubtitle(track.artist, track.album, track.title),
                        color = if (showArtistButton) SecondaryFixed else OnSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .then(
                                if (showArtistButton) {
                                    Modifier
                                        .focusProperties { canFocus = false }
                                        .clickable { onArtistClick() }
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (isDownloaded) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = "Stiahnuté",
                    tint = Secondary,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(16.dp)
                )
            }
            Text(
                text = track.duration,
                color = if (isActive) Primary else OnSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
            if (showArtistButton) {
                IconButton(
                    onClick = onArtistClick,
                    modifier = Modifier
                        .size(32.dp)
                        .focusProperties { canFocus = false }
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Prejsť na interpreta",
                        tint = SecondaryFixed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            IconButton(
                onClick = onLikeClick,
                modifier = Modifier
                    .size(34.dp)
                    .focusProperties { canFocus = false }
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Obľúbené",
                    tint = if (isLiked) Primary else OnSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
            }
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(30.dp)
                    .focusProperties { canFocus = false }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Možnosti",
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}
