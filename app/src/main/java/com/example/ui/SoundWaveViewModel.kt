package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.AlbumItem
import com.example.data.AppScreen
import com.example.data.ArtistItem
import com.example.data.CachedExploreSnapshot
import com.example.data.CachedHomeSnapshot
import com.example.data.CachedLibrarySnapshot
import com.example.data.CatalogCacheStore
import com.example.data.CollectionDetail
import com.example.data.GenreItem
import com.example.data.HeadphoneProfile
import com.example.data.LyricLine
import com.example.data.NoTrack
import com.example.data.OfflineManager
import com.example.data.PlaylistItem
import com.example.data.SavedCredentials
import com.example.data.SearchResult
import com.example.data.SettingsStore
import com.example.data.SleepTimerOption
import com.example.data.SoundWaveCatalog
import com.example.data.SubsonicApi
import com.example.data.SubsonicException
import com.example.data.TrackItem
import com.example.playback.AudioEffectsManager
import com.example.playback.MediaCacheManager
import com.example.playback.SleepTimerCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

import androidx.compose.runtime.Immutable

@Immutable
data class SoundWaveUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val backStack: List<AppScreen> = emptyList(),
    val showAutoEqModal: Boolean = false,
    val showScreenSwitcherSheet: Boolean = false,
    val showSleepTimerModal: Boolean = false,
    val sleepTimerRemainingSec: Int = 0,
    val sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    val downloadedTrackIds: Set<String> = emptySet(),
    val downloadingTrackIds: Set<String> = emptySet(),
    val downloadedTracks: List<TrackItem> = emptyList(),
    val downloadedSizeText: String = "0.0 MB",
    val trackMenuTarget: TrackItem? = null,
    val showOnboarding: Boolean = false,
    val themeMode: String = "DARK", // "DARK", "LIGHT", "SYSTEM", "AMOLED"
    val isSimpleMode: Boolean = false,
    val appLanguage: String = "SYSTEM",
    val strings: com.example.data.AppStringsBundle = com.example.data.AppStrings.forLanguage("SYSTEM"),
    /** One-shot text for a Toast (errors, confirmations). Cleared by consumeMessage(). */
    val message: String? = null,

    // Playback State
    val currentTrack: TrackItem = NoTrack,
    val isPlaying: Boolean = false,
    val isMiniPlayerDismissed: Boolean = false,
    val playbackProgress: Float = 0f,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isSeeking: Boolean = false,
    val queue: List<TrackItem> = emptyList(),
    val queueIndex: Int = -1,
    val playingFrom: String = "",
    val likedTrackIds: Set<String> = emptySet(),
    val isShuffleEnabled: Boolean = false,
    val isRepeatOne: Boolean = false,
    val nowPlayingTab: String = "TEXT", // "NASLEDUJÚCE", "TEXT", "PODOBNÉ"
    val lyrics: List<LyricLine> = emptyList(),
    val lyricsLoading: Boolean = false,
    val activeLyricIndex: Int = -1,
    val relatedTracks: List<TrackItem> = emptyList(),
    val relatedLoading: Boolean = false,
    val historyTracks: List<TrackItem> = emptyList(),

    // Home
    val homeFilter: String = "Všetko",
    val homeGenres: List<String> = com.example.data.SmartGenreCatalog.specs.map { it.title },
    val homeQuickPicks: List<TrackItem> = emptyList(),
    val homeListenAgain: List<AlbumItem> = emptyList(),
    val homeNewest: List<AlbumItem> = emptyList(),
    val homeLoading: Boolean = false,
    val homeError: String? = null,

    // Explore / Navidrome Search State
    val searchQuery: String = "",
    val exploreCategory: String = "Všetko",
    val activeAudiophileFilters: Set<String> = emptySet(),
    val exploreDefault: SearchResult = SearchResult(emptyList(), emptyList(), emptyList()),
    val searchResult: SearchResult = SearchResult(emptyList(), emptyList(), emptyList()),
    val exploreLoading: Boolean = false,
    val exploreError: String? = null,
    val genres: List<GenreItem> = com.example.data.SmartGenreCatalog.specs.map { GenreItem(it.title, 0) },
    val lastSyncTime: String = "Knižnica sa načíta zo servera",
    val isScanningServer: Boolean = false,

    // Detail page (album / playlist / artist / favourites / genre)
    val detail: CollectionDetail? = null,
    val detailStack: List<CollectionDetail> = emptyList(),

    // Library State
    val libraryFilterChip: String = "Playlisty",
    val libraryIsGridView: Boolean = false,
    val libraryPlaylists: List<PlaylistItem> = emptyList(),
    val libraryAlbums: List<AlbumItem> = emptyList(),
    val libraryArtists: List<ArtistItem> = emptyList(),
    val favoriteTracks: List<TrackItem> = emptyList(),
    val serverSongCount: Long = 0L,
    val libraryLoading: Boolean = false,
    val libraryError: String? = null,

    // Equalizer & DSP State
    val eqMasterEnabled: Boolean = true,
    val preampDb: Float = 0.0f,
    val eqBands: List<Float> = SoundWaveCatalog.headphoneProfiles[0].eqBands,
    val activePresetName: String = "Power Metal / Heavy Metal",
    val subBassBoostDb: Float = 2.0f,
    val crossfeedEnabled: Boolean = false,
    val autoGainEnabled: Boolean = true,
    val bluetoothVolumeStabilizerEnabled: Boolean = false,

    // AutoEQ Headphone Calibration State
    val autoEqSearchQuery: String = "",
    val autoEqTargetCurve: String = "Harman In-Ear 2019",
    val selectedHeadphone: HeadphoneProfile = SoundWaveCatalog.headphoneProfiles[0],
    val calibrationDryWetPct: Float = 100f,
    val autoPreampEnabled: Boolean = true,
    val subBassCompensationEnabled: Boolean = true,

    // Streaming Quality & ffmpeg Server Settings State
    val cellularTranscodingEnabled: Boolean = false,
    val opusBitrateTarget: String = "320k", // "64k", "128k", "192k", "256k", "320k", "Auto"
    val backendCodec: String = "FLAC Stream",
    val maxSampleRate: String = "48 kHz",
    val hardwareAccelVaapi: Boolean = true,
    val bitPerfectExclusiveMode: Boolean = true,
    val replayGainEnabled: Boolean = false,
    val replayGainMode: String = "Album Gain (Zachová dynamiku)",
    val replayGainPreampDb: Float = 0.0f,
    val gaplessEnabled: Boolean = true,
    val crossfadeSeconds: Int = 0,
    val cacheSizeText: String = "Počítam veľkosť pamäte…",
    val isTestingDiagnostics: Boolean = false,
    val diagnosticResultVisible: Boolean = false,
    val diagnosticResultText: String = "",

    // Navidrome Server Login State
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val showTokenHint: Boolean = false,
    val showAdvancedAudioAccordion: Boolean = false,
    val loginOfflineCacheEnabled: Boolean = true,
    val loginPreferFlacEnabled: Boolean = true,
    val loginTranscodeOption: String = "Pôvodná kvalita (Direct Stream)",
    val isConnectingServer: Boolean = false,
    val isServerConnectedSuccess: Boolean = false,
    val isLoggedIn: Boolean = false,
    val connectionError: String? = null,
    val serverInfoText: String = "",
    val pingMs: Int = -1
) {
    /** "192.168.1.5:4533" – the server address without the scheme. */
    val serverHost: String
        get() = serverUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
}

/** What the Explore screen should show: server search results, or the default "discover" content. */
fun SoundWaveUiState.exploreContent(): SearchResult =
    if (searchQuery.isBlank()) exploreDefault else searchResult

private data class StreamProfile(val format: String?, val maxBitRate: Int?, val label: String)

class SoundWaveViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsStore(app)
    private val offlineManager = OfflineManager(app)
    private val catalogCache = CatalogCacheStore(app)
    private var api: SubsonicApi? = null

    private val _uiState = MutableStateFlow(buildState(settings.loadCredentials()))
    val uiState: StateFlow<SoundWaveUiState> = _uiState.asStateFlow()

    private var controller: MediaController? = null
    private val pendingForController = mutableListOf<(MediaController) -> Unit>()

    private var homeJob: Job? = null
    private var exploreJob: Job? = null
    private var libraryJob: Job? = null
    private var detailJob: Job? = null
    private var exploreLoaded = false
    private var libraryLoaded = false
    private var searchJob: Job? = null
    private var seekJob: Job? = null
    private var lyricsJob: Job? = null
    private var relatedJob: Job? = null
    private var relatedLoadedFor: String = ""
    private var scrobbled = false
    private var suppressInitialRestoreTransition = false

    init {
        connectController()
        startProgressLoop()
        observeSleepTimer()
        refreshCacheSize()
        refreshDownloadedTracks()
        val initial = _uiState.value
        AudioEffectsManager.setBluetoothVolumeStabilizerEnabled(initial.bluetoothVolumeStabilizerEnabled)
        AudioEffectsManager.setAutoGainEnabled(initial.autoGainEnabled)
        AudioEffectsManager.setSpatialEnabled(initial.crossfeedEnabled)
        AudioEffectsManager.setSubBassBoost(initial.subBassBoostDb)
        AudioEffectsManager.setBands(initial.eqBands, initial.preampDb)
        AudioEffectsManager.setReplayGain(initial.replayGainEnabled, initial.replayGainPreampDb, initial.replayGainMode)
        AudioEffectsManager.setMasterEnabled(initial.eqMasterEnabled)
        settings.loadCredentials()?.let { c ->
            val createdApi = SubsonicApi(c.url, c.username, c.token, c.salt)
            api = createdApi
            rehydratePersistedTracks(createdApi)
            viewModelScope.launch {
                hydrateCatalogFromDiskCache(createdApi)
                verifyAndLoad()
            }
        }
    }

    private fun observeSleepTimer() {
        viewModelScope.launch {
            SleepTimerCoordinator.state.collect { st ->
                _uiState.update {
                    it.copy(sleepTimerOption = st.option, sleepTimerRemainingSec = st.remainingSec)
                }
                st.expiredMessage?.let { msg ->
                    showMessage(msg)
                    SleepTimerCoordinator.consumeExpiredMessage()
                }
            }
        }
    }

    private fun rehydrateTrack(a: SubsonicApi, t: TrackItem): TrackItem =
        if (t.id.isBlank()) t else t.copy(imageUrl = a.resolveCoverUrl(t.coverArtId, t.imageUrl, 360))

    private fun rehydratePersistedTracks(a: SubsonicApi) {
        _uiState.update { st ->
            st.copy(
                currentTrack = rehydrateTrack(a, st.currentTrack),
                queue = st.queue.map { rehydrateTrack(a, it) },
                historyTracks = st.historyTracks.map { rehydrateTrack(a, it) }
            )
        }
    }

    private suspend fun hydrateCatalogFromDiskCache(a: SubsonicApi) = withContext(Dispatchers.IO) {
        val cachedHome = catalogCache.loadHome(a)
        val cachedLib = catalogCache.loadLibrary(a)
        val cachedExp = catalogCache.loadExplore(a)
        if (cachedHome != null || cachedLib != null || cachedExp != null) {
            _uiState.update { st ->
                val mergedGenres = when {
                    cachedHome != null && cachedHome.genres.isNotEmpty() -> cachedHome.genres
                    cachedExp != null && cachedExp.genres.isNotEmpty() -> cachedExp.genres
                    else -> st.genres
                }
                val mergedHomeGenres = (mergedGenres.map { it.name } + com.example.data.SmartGenreCatalog.specs.map { it.title })
                    .distinct()
                    .take(15)
                st.copy(
                    homeQuickPicks = if (st.homeQuickPicks.isEmpty() && cachedHome != null) cachedHome.quickPicks else st.homeQuickPicks,
                    homeListenAgain = if (st.homeListenAgain.isEmpty() && cachedHome != null) cachedHome.listenAgain else st.homeListenAgain,
                    homeNewest = if (st.homeNewest.isEmpty() && cachedHome != null) cachedHome.newest else st.homeNewest,
                    genres = mergedGenres,
                    homeGenres = mergedHomeGenres,
                    libraryPlaylists = when {
                        st.libraryPlaylists.isEmpty() && cachedLib != null -> cachedLib.playlists
                        st.libraryPlaylists.isEmpty() && cachedHome != null -> cachedHome.playlists
                        else -> st.libraryPlaylists
                    },
                    libraryArtists = if (st.libraryArtists.isEmpty() && cachedLib != null) cachedLib.artists else st.libraryArtists,
                    libraryAlbums = if (st.libraryAlbums.isEmpty() && cachedLib != null) cachedLib.albums else st.libraryAlbums,
                    favoriteTracks = if (st.favoriteTracks.isEmpty() && cachedLib != null) cachedLib.favorites else st.favoriteTracks,
                    serverSongCount = if (st.serverSongCount == 0L && cachedLib != null) cachedLib.serverSongCount else st.serverSongCount,
                    exploreDefault = if (st.exploreDefault.tracks.isEmpty() && cachedExp != null) {
                        SearchResult(cachedExp.artists, cachedExp.albums, cachedExp.tracks)
                    } else st.exploreDefault,
                    lastSyncTime = "Načítané z rýchlej pamäte • Obnovujem…"
                )
            }
        }
    }

    private fun buildState(creds: SavedCredentials?): SoundWaveUiState {
        val history = settings.loadHistory()
        val lastTrack = settings.loadLastTrack()
        val lastPos = settings.loadLastPositionMs()
        val lastQueue = settings.loadLastQueue()
        val lastQueueIdx = settings.loadLastQueueIndex()
        val restoredTrack = lastTrack?.takeIf { it.id.isNotBlank() } ?: NoTrack
        val restoredQueue = when {
            lastQueue.isNotEmpty() -> lastQueue
            restoredTrack !== NoTrack -> listOf(restoredTrack)
            else -> emptyList()
        }
        val restoredDur = if (restoredTrack !== NoTrack) restoredTrack.durationSec * 1000L else 0L
        val restoredProgress = if (restoredDur > 0L) (lastPos.toFloat() / restoredDur).coerceIn(0f, 1f) else 0f

        val savedPreset = settings.getString(SettingsStore.KEY_EQ_PRESET, "Power Metal / Heavy Metal")
        val defaultBands = SoundWaveCatalog.eqPresets[savedPreset] ?: SoundWaveCatalog.headphoneProfiles[0].eqBands
        val savedBands = settings.loadEqBands(defaultBands)

        return SoundWaveUiState(
            currentScreen = if (creds == null) AppScreen.SERVER_LOGIN else AppScreen.HOME,
            serverUrl = creds?.url ?: settings.lastServerUrl(),
            username = creds?.username ?: settings.lastUsername(),
            isLoggedIn = creds != null,
            isServerConnectedSuccess = creds != null,
            serverInfoText = if (creds != null) "Pripájam sa…" else "",
            cellularTranscodingEnabled = settings.getBoolean(SettingsStore.KEY_CELLULAR_TRANSCODING, false),
            opusBitrateTarget = settings.getString(SettingsStore.KEY_OPUS_BITRATE, "320k"),
            backendCodec = settings.getString(SettingsStore.KEY_BACKEND_CODEC, "FLAC Stream"),
            loginTranscodeOption = settings.getString(
                SettingsStore.KEY_LOGIN_TRANSCODE,
                "Pôvodná kvalita (Direct Stream)"
            ),
            eqMasterEnabled = settings.getBoolean(SettingsStore.KEY_EQ_MASTER, true),
            activePresetName = savedPreset,
            eqBands = savedBands,
            preampDb = settings.getFloat(SettingsStore.KEY_EQ_PREAMP, 0.0f),
            subBassBoostDb = settings.getFloat(SettingsStore.KEY_SUB_BASS, 2.0f),
            crossfeedEnabled = settings.getBoolean(SettingsStore.KEY_SPATIAL, false),
            autoGainEnabled = settings.getBoolean(SettingsStore.KEY_AUTO_GAIN, true),
            bluetoothVolumeStabilizerEnabled = settings.getBoolean(SettingsStore.KEY_BT_STABILIZER, false),
            replayGainEnabled = settings.getBoolean(SettingsStore.KEY_REPLAY_GAIN_ENABLED, false),
            replayGainMode = settings.getString(SettingsStore.KEY_REPLAY_GAIN_MODE, "Album Gain (Zachová dynamiku)"),
            replayGainPreampDb = settings.getFloat(SettingsStore.KEY_REPLAY_GAIN_PREAMP, 0.0f),
            currentTrack = restoredTrack,
            queue = restoredQueue,
            queueIndex = if (restoredQueue.isNotEmpty()) lastQueueIdx.coerceIn(0, restoredQueue.lastIndex) else -1,
            positionMs = lastPos,
            durationMs = restoredDur,
            playbackProgress = restoredProgress,
            historyTracks = history,
            showOnboarding = !settings.isOnboardingCompleted(),
            themeMode = settings.getThemeMode(),
            isSimpleMode = settings.isSimpleMode(),
            appLanguage = settings.getAppLanguage(),
            strings = com.example.data.AppStrings.forLanguage(settings.getAppLanguage())
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    fun setAppLanguage(lang: String) {
        settings.setAppLanguage(lang)
        _uiState.update {
            it.copy(
                appLanguage = lang,
                strings = com.example.data.AppStrings.forLanguage(lang)
            )
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val localeManager = getApplication<Application>().getSystemService(android.app.LocaleManager::class.java)
                localeManager?.applicationLocales = if (lang == "SYSTEM" || lang.isBlank()) {
                    android.os.LocaleList.getEmptyLocaleList()
                } else {
                    android.os.LocaleList.forLanguageTags(lang)
                }
            }
        } catch (_: Throwable) {}
    }

    fun setThemeMode(mode: String) {
        settings.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun toggleSimpleMode() {
        val next = !_uiState.value.isSimpleMode
        settings.setSimpleMode(next)
        _uiState.update { it.copy(isSimpleMode = next) }
    }

    fun showMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun launchApi(
        onError: (String) -> Unit = { showMessage(it) },
        block: suspend (SubsonicApi) -> Unit
    ): Job {
        val a = api
        if (a == null) {
            onError("Najprv sa prihlás k serveru.")
            return viewModelScope.launch { }
        }
        return viewModelScope.launch(Dispatchers.IO) {
            try {
                block(a)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SubsonicException) {
                onError(e.message ?: "Chyba servera.")
            } catch (e: Exception) {
                onError("Neočakávaná chyba: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    private suspend fun <T> safe(default: T, block: suspend () -> T): T =
        try {
            block()
        } catch (e: SubsonicException) {
            default
        }

    /** Keeps the heart icons in sync with what the server says about the loaded tracks. */
    private fun syncLikes(tracks: List<TrackItem>) {
        if (tracks.isEmpty()) return
        _uiState.update { state ->
            val ids = tracks.map { it.id }.toSet()
            val liked = tracks.filter { it.isLiked }.map { it.id }
            state.copy(likedTrackIds = (state.likedTrackIds - ids) + liked)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------------------------------------

    fun navigateTo(screen: AppScreen) {
        _uiState.update { state ->
            if (state.currentScreen == screen) {
                state.copy(showScreenSwitcherSheet = false)
            } else {
                state.copy(
                    backStack = state.backStack + state.currentScreen,
                    currentScreen = screen,
                    showScreenSwitcherSheet = false,
                    isMiniPlayerDismissed = if (screen == AppScreen.NOW_PLAYING) false else state.isMiniPlayerDismissed
                )
            }
        }
        when (screen) {
            AppScreen.EXPLORE -> if (!exploreLoaded && api != null) loadExplore()
            AppScreen.LIBRARY -> if (!libraryLoaded && api != null) loadLibrary()
            else -> {}
        }
    }

    fun navigateBack(): Boolean {
        val current = _uiState.value
        if (current.showAutoEqModal) {
            _uiState.update { it.copy(showAutoEqModal = false) }
            return true
        }
        if (current.showScreenSwitcherSheet) {
            _uiState.update { it.copy(showScreenSwitcherSheet = false) }
            return true
        }
        if (current.currentScreen == AppScreen.PLAYLIST_DETAIL && current.detailStack.isNotEmpty()) {
            val prevDetail = current.detailStack.last()
            _uiState.update {
                it.copy(
                    detail = prevDetail,
                    detailStack = it.detailStack.dropLast(1)
                )
            }
            return true
        }
        if (current.backStack.isNotEmpty()) {
            val prev = current.backStack.last()
            _uiState.update {
                it.copy(
                    currentScreen = prev,
                    backStack = it.backStack.dropLast(1)
                )
            }
            return true
        }
        if (current.currentScreen != AppScreen.HOME && current.isLoggedIn) {
            _uiState.update { it.copy(currentScreen = AppScreen.HOME) }
            return true
        }
        return false
    }

    fun setShowAutoEqModal(show: Boolean) {
        _uiState.update { it.copy(showAutoEqModal = show) }
    }

    fun setShowScreenSwitcherSheet(show: Boolean) {
        _uiState.update { it.copy(showScreenSwitcherSheet = show) }
    }

    // ---------------------------------------------------------------------------------------------
    // Server login / session
    // ---------------------------------------------------------------------------------------------

    private suspend fun verifyAndLoad() {
        val a = api ?: return
        // Start loading Home screen content immediately in parallel with ping() so TV doesn't wait!
        loadAll()
        try {
            val t0 = SystemClock.elapsedRealtime()
            val info = a.ping()
            val ms = (SystemClock.elapsedRealtime() - t0).toInt()
            _uiState.update {
                it.copy(serverInfoText = describeServer(info.serverType, info.serverVersion, info.apiVersion, info.openSubsonic), pingMs = ms)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SubsonicException) {
            _uiState.update { it.copy(serverInfoText = "Server nedostupný", pingMs = -1) }
            if (e.code == 40) {
                // The saved token no longer works (password changed on the server).
                settings.clearCredentials()
                api = null
                _uiState.update {
                    it.copy(
                        currentScreen = AppScreen.SERVER_LOGIN,
                        isLoggedIn = false,
                        isServerConnectedSuccess = false,
                        connectionError = "Prihlásenie vypršalo, zadaj heslo znova."
                    )
                }
                return
            }
            showMessage(e.message ?: "Server nedostupný.")
        }
    }

    private fun describeServer(type: String, version: String, apiVersion: String, openSubsonic: Boolean): String {
        val name = type.replaceFirstChar { c -> c.titlecase(Locale.ROOT) }
        return buildString {
            append(name)
            if (version.isNotBlank()) append(" ").append(version.substringBefore(" "))
            if (apiVersion.isNotBlank()) append(" • Subsonic API ").append(apiVersion)
            if (openSubsonic) append(" • OpenSubsonic")
        }
    }

    private fun loadAll() {
        exploreLoaded = false
        libraryLoaded = false
        loadHome()
    }

    fun connectToServer() {
        val s = _uiState.value
        if (s.isConnectingServer) return
        val url = SubsonicApi.normalizeUrl(s.serverUrl)
        val user = s.username.trim()
        if (url.isBlank() || user.isBlank()) {
            _uiState.update { it.copy(connectionError = "Zadaj adresu servera a používateľské meno.") }
            return
        }
        _uiState.update {
            it.copy(isConnectingServer = true, isServerConnectedSuccess = false, connectionError = null)
        }
        viewModelScope.launch {
            try {
                val saved = settings.loadCredentials()
                val token: String
                val salt: String
                if (s.password.isNotEmpty()) {
                    salt = SubsonicApi.newSalt()
                    token = SubsonicApi.tokenFor(s.password, salt)
                } else if (saved != null && saved.url == url && saved.username == user) {
                    token = saved.token
                    salt = saved.salt
                } else {
                    throw SubsonicException("Zadaj heslo.")
                }
                val candidate = SubsonicApi(url, user, token, salt)
                val t0 = SystemClock.elapsedRealtime()
                val info = candidate.ping()
                val ms = (SystemClock.elapsedRealtime() - t0).toInt()

                settings.saveCredentials(SavedCredentials(url, user, token, salt))
                api = candidate
                _uiState.update {
                    it.copy(
                        isConnectingServer = false,
                        isServerConnectedSuccess = true,
                        isLoggedIn = true,
                        serverUrl = url,
                        username = user,
                        password = "",
                        connectionError = null,
                        pingMs = ms,
                        serverInfoText = describeServer(info.serverType, info.serverVersion, info.apiVersion, info.openSubsonic)
                    )
                }
                loadAll()
                delay(700)
                _uiState.update {
                    if (it.currentScreen == AppScreen.SERVER_LOGIN) {
                        it.copy(currentScreen = AppScreen.HOME, backStack = emptyList())
                    } else it
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SubsonicException) {
                _uiState.update {
                    it.copy(isConnectingServer = false, isServerConnectedSuccess = false, connectionError = e.message)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isConnectingServer = false,
                        isServerConnectedSuccess = false,
                        connectionError = "Neočakávaná chyba: ${e.message ?: e.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    fun logout() {
        settings.clearCredentials()
        viewModelScope.launch(Dispatchers.IO) { catalogCache.clearAll() }
        exploreLoaded = false
        libraryLoaded = false
        api = null
        withController {
            it.stop()
            it.clearMediaItems()
        }
        val prev = _uiState.value
        _uiState.value = buildState(null).copy(serverUrl = prev.serverUrl, username = prev.username)
    }

    // ---------------------------------------------------------------------------------------------
    // Loading content
    // ---------------------------------------------------------------------------------------------

    fun loadHome() {
        homeJob?.cancel()
        val genre = _uiState.value.homeFilter.takeIf { it != "Všetko" && it != "All" }
        _uiState.update { it.copy(homeLoading = it.homeQuickPicks.isEmpty(), homeError = null) }
        homeJob = launchApi(onError = { msg ->
            _uiState.update { st ->
                st.copy(homeLoading = false, homeError = if (st.homeQuickPicks.isEmpty()) msg else null)
            }
        }) { a ->
            coroutineScope {
                val picks = async { a.randomSongs(10, genre) }
                val again = async {
                    if (genre == null) a.albumList("recent", 10) else a.albumList("byGenre", 10, genre = genre)
                }
                val fresh = async { a.albumList("newest", 10) }
                val genreList = async { safe(emptyList<GenreItem>()) { a.genres() } }
                val pls = async { safe<List<PlaylistItem>?>(null) { a.playlists() } }

                val pickList = picks.await()
                val againList = again.await().ifEmpty { safe(emptyList<AlbumItem>()) { a.albumList("random", 10) } }
                val freshList = fresh.await()
                val genresLoaded = genreList.await()
                val plsLoaded = pls.await()
                if (genre == null) {
                    catalogCache.saveHome(
                        a.baseUrl,
                        a.username,
                        CachedHomeSnapshot(pickList, againList, freshList, genresLoaded, plsLoaded.orEmpty())
                    )
                }
                _uiState.update { st ->
                    val mergedGenres = if (genresLoaded.isNotEmpty()) genresLoaded else st.genres
                    val mergedHomeGenres = (mergedGenres.map { it.name } + com.example.data.SmartGenreCatalog.specs.map { it.title })
                        .distinct()
                        .take(15)
                    st.copy(
                        homeLoading = false,
                        homeError = null,
                        homeQuickPicks = pickList,
                        homeListenAgain = againList,
                        homeNewest = freshList,
                        genres = mergedGenres,
                        homeGenres = mergedHomeGenres,
                        libraryPlaylists = plsLoaded ?: st.libraryPlaylists,
                        lastSyncTime = "Synchronizované zo servera"
                    )
                }
                syncLikes(pickList)
            }
        }
    }

    fun loadLibrary() {
        libraryJob?.cancel()
        libraryLoaded = true
        _uiState.update {
            it.copy(
                libraryLoading = it.libraryPlaylists.isEmpty() && it.libraryAlbums.isEmpty() && it.libraryArtists.isEmpty(),
                libraryError = null
            )
        }
        libraryJob = launchApi(onError = { msg ->
            libraryLoaded = false
            _uiState.update { st ->
                val hasCached = st.libraryPlaylists.isNotEmpty() || st.libraryAlbums.isNotEmpty()
                st.copy(libraryLoading = false, libraryError = if (hasCached) null else msg)
            }
        }) { a ->
            coroutineScope {
                val pls = async { a.playlists() }
                val artists = async { a.artists() }
                val fav = async { safe(emptyList<TrackItem>()) { a.starredTracks() } }
                val scan = async { safe(com.example.data.ScanStatus(false, 0L)) { a.scanStatus() } }
                val firstAlbums = async { safe(emptyList<AlbumItem>()) { a.albumList("alphabeticalByName", 150, 0) } }

                val plsLoaded = pls.await()
                val artistsLoaded = artists.await()
                val favLoaded = fav.await()
                val firstAlbumsLoaded = firstAlbums.await()
                val scanStatus = scan.await()

                catalogCache.saveLibrary(
                    a.baseUrl,
                    a.username,
                    CachedLibrarySnapshot(
                        plsLoaded,
                        artistsLoaded,
                        firstAlbumsLoaded,
                        favLoaded,
                        scanStatus.count
                    )
                )

                _uiState.update {
                    it.copy(
                        libraryLoading = false,
                        libraryError = null,
                        libraryPlaylists = plsLoaded,
                        libraryArtists = artistsLoaded,
                        libraryAlbums = firstAlbumsLoaded,
                        favoriteTracks = favLoaded,
                        serverSongCount = if (scanStatus.count > 0L) scanStatus.count else it.serverSongCount,
                        lastSyncTime = "Synchronizované zo servera"
                    )
                }
                syncLikes(favLoaded)
            }
        }
    }

    fun loadExplore() {
        exploreJob?.cancel()
        exploreLoaded = true
        exploreJob = launchApi(onError = { msg ->
            exploreLoaded = false
            _uiState.update { st ->
                st.copy(exploreError = if (st.exploreDefault.tracks.isEmpty()) msg else null)
            }
        }) { a ->
            coroutineScope {
                val tracks = async { a.randomSongs(20) }
                val albums = async { a.albumList("newest", 10) }
                val artists = async { safe(emptyList<ArtistItem>()) { a.artists() } }
                val genreList = async { safe(emptyList<GenreItem>()) { a.genres() } }
                val t = tracks.await()
                val al = albums.await()
                val ar = artists.await()
                val ge = genreList.await()
                val artistSlice = ar.shuffled().take(12)
                catalogCache.saveExplore(
                    a.baseUrl,
                    a.username,
                    CachedExploreSnapshot(artistSlice, al, t, ge)
                )
                _uiState.update { st ->
                    st.copy(
                        exploreError = null,
                        exploreDefault = SearchResult(artistSlice, al, t),
                        genres = if (ge.isNotEmpty()) ge else st.genres
                    )
                }
                syncLikes(t)
            }
        }
    }

    fun openCollection(id: String, fallbackName: String = "") {
        detailJob?.cancel()
        val kind = when {
            id.startsWith("pl:") -> "Playlist"
            id.startsWith("al:") || id.startsWith("al_name:") -> "Album"
            id.startsWith("ar:") || id.startsWith("ar_name:") -> "Interpret"
            id.startsWith("ge:") -> "Žáner"
            else -> "Zbierka"
        }
        val loadingTitle = fallbackName.takeIf { it.isNotBlank() } ?: "Načítavam…"
        _uiState.update { st ->
            val isAlreadyOnDetail = st.currentScreen == AppScreen.PLAYLIST_DETAIL
            val stack = if (isAlreadyOnDetail) st.backStack else st.backStack + st.currentScreen
            val nextDetailStack = if (isAlreadyOnDetail && st.detail != null && st.detail.id != id && !st.detail.isLoading) {
                st.detailStack + st.detail
            } else if (!isAlreadyOnDetail) {
                emptyList()
            } else {
                st.detailStack
            }
            st.copy(
                detail = CollectionDetail(
                    id = id, kind = kind, title = loadingTitle, subtitle = "Načítavam zo servera…", coverUrl = "",
                    metaLine = "", tracks = emptyList(), isLoading = true
                ),
                detailStack = nextDetailStack,
                currentScreen = AppScreen.PLAYLIST_DETAIL,
                backStack = stack,
                showScreenSwitcherSheet = false
            )
        }
        detailJob = launchApi(onError = { msg ->
            _uiState.update { st ->
                st.copy(detail = st.detail?.copy(isLoading = false, title = "Nepodarilo sa načítať", error = msg))
            }
        }) { a ->
            val result = when {
                id.startsWith("pl:") -> a.playlistDetail(id.removePrefix("pl:"))
                id.startsWith("al:") -> {
                    val rawId = id.removePrefix("al:")
                    try {
                        a.albumDetail(rawId)
                    } catch (e: Exception) {
                        if (fallbackName.isNotBlank()) a.albumDetailByName(fallbackName) else throw e
                    }
                }
                id.startsWith("al_name:") -> a.albumDetailByName(id.removePrefix("al_name:"))
                id.startsWith("ar:") -> {
                    val rawId = id.removePrefix("ar:")
                    try {
                        val det = a.artistDetail(rawId)
                        if (det.tracks.isEmpty() && det.albums.isEmpty() && fallbackName.isNotBlank()) {
                            a.artistDetailByName(fallbackName)
                        } else {
                            det
                        }
                    } catch (e: Exception) {
                        if (fallbackName.isNotBlank()) a.artistDetailByName(fallbackName) else throw e
                    }
                }
                id.startsWith("ar_name:") -> a.artistDetailByName(id.removePrefix("ar_name:"))
                id.startsWith("ge:") -> a.genreDetail(id.removePrefix("ge:"))
                else -> a.favoritesDetail()
            }
            _uiState.update { st ->
                // Ignore a late answer if the user already opened something else.
                if (st.detail?.id == id) st.copy(detail = result) else st
            }
            syncLikes(result.tracks)
        }
    }

    fun openFavorites() = openCollection("fav")

    /**
     * Opens the full artist/band page (with all their albums and tracks) for any track,
     * even when playing a random mix or folder where track.artistId is empty.
     */
    fun openArtistForTrack(track: TrackItem) {
        val cleanedArtist = com.example.data.cleanArtistName(track.artist).trim()
            .ifBlank {
                if (track.title.contains(" - ")) track.title.substringBefore(" - ").trim() else ""
            }
        val artistId = track.artistId.trim()
        when {
            artistId.isNotBlank() -> openCollection("ar:$artistId", fallbackName = cleanedArtist)
            cleanedArtist.isNotBlank() && !cleanedArtist.equals("Neznámy interpret", ignoreCase = true) -> {
                openCollection("ar_name:$cleanedArtist", fallbackName = cleanedArtist)
            }
            else -> showMessage("Pre túto skladbu nie je uvedený interpret.")
        }
    }

    /**
     * Opens the full album page for any track, resolving by albumId or searching by album title.
     */
    fun openAlbumForTrack(track: TrackItem) {
        val cleanedAlbum = com.example.data.cleanAlbumName(track.album).trim()
        val albumId = track.albumId.trim()
        when {
            albumId.isNotBlank() -> openCollection("al:$albumId", fallbackName = cleanedAlbum)
            cleanedAlbum.isNotBlank() -> openCollection("al_name:$cleanedAlbum", fallbackName = cleanedAlbum)
            else -> openArtistForTrack(track)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Playback
    // ---------------------------------------------------------------------------------------------

    private fun connectController() {
        val ctx = getApplication<Application>()
        val token = SessionToken(ctx, ComponentName(ctx, com.example.playback.PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(playerListener)
                if (c.mediaItemCount > 0) {
                    syncFromController(c)
                } else {
                    // Controller is empty (e.g. freshly started after phone/TV restart).
                    // Slice around queueIndex and suppress transition side-effects so we preserve exact saved position!
                    val state = _uiState.value
                    if (state.currentTrack.id.isNotBlank() && state.queue.isNotEmpty()) {
                        val profile = currentStreamProfile()
                        val fullQueue = state.queue
                        val windowStart = (state.queueIndex - 10).coerceAtLeast(0).coerceAtMost((fullQueue.size - 1).coerceAtLeast(0))
                        val safeQueue = if (fullQueue.size > 60) fullQueue.drop(windowStart).take(60) else fullQueue
                        val localIdx = if (fullQueue.size > 60) {
                            (state.queueIndex - windowStart).coerceIn(0, (safeQueue.size - 1).coerceAtLeast(0))
                        } else {
                            state.queueIndex.coerceIn(0, (safeQueue.size - 1).coerceAtLeast(0))
                        }
                        val items = safeQueue.map { mediaItemFor(api, it, profile) }
                        val pos = state.positionMs
                        suppressInitialRestoreTransition = true
                        _uiState.update { it.copy(queue = safeQueue, queueIndex = localIdx, currentTrack = safeQueue[localIdx]) }
                        c.setMediaItems(items, localIdx, pos)
                        c.pause()
                    }
                }
                val pending = pendingForController.toList()
                pendingForController.clear()
                pending.forEach { it(c) }
            } catch (e: Exception) {
                showMessage("Prehrávač sa nepodarilo spustiť.")
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun withController(action: (MediaController) -> Unit) {
        val c = controller
        if (c != null) action(c) else pendingForController += action
    }

    /** The service can outlive the Activity; pick up whatever it is playing. */
    private fun syncFromController(c: MediaController) {
        if (c.mediaItemCount == 0) return
        val items = (0 until c.mediaItemCount).map { c.getMediaItemAt(it) }
        val queue = items.map { trackFromMediaItem(it) }
        val idx = c.currentMediaItemIndex.coerceIn(0, queue.lastIndex)
        _uiState.update {
            it.copy(
                queue = queue,
                queueIndex = idx,
                currentTrack = queue[idx],
                isPlaying = c.isPlaying,
                isShuffleEnabled = c.shuffleModeEnabled,
                isRepeatOne = c.repeatMode == Player.REPEAT_MODE_ONE,
                durationMs = queue[idx].durationSec * 1000L
            )
        }
        loadLyrics(queue[idx])
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (!isPlaying) {
                // When paused, immediately save position to disk so phone shutdown/reboot retains it
                controller?.let { c ->
                    val pos = c.currentPosition
                    val s = _uiState.value
                    if (s.currentTrack.id.isNotBlank()) {
                        settings.savePlaybackState(s.currentTrack, pos, s.queue, s.queueIndex)
                    }
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (suppressInitialRestoreTransition) {
                suppressInitialRestoreTransition = false
                mediaItem?.let { item ->
                    loadLyrics(_uiState.value.currentTrack)
                    if (item.mediaId.isNotBlank() && _uiState.value.nowPlayingTab in setOf("PODOBNÉ", "RELATED")) {
                        loadRelated(_uiState.value.currentTrack)
                    }
                }
                return
            }
            onCurrentItemChanged()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val c = controller ?: return
            if (playbackState == Player.STATE_READY && c.duration != C.TIME_UNSET && c.duration > 0) {
                _uiState.update { it.copy(durationMs = c.duration) }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.update { it.copy(isShuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _uiState.update { it.copy(isRepeatOne = repeatMode == Player.REPEAT_MODE_ONE) }
        }

        override fun onPlayerError(error: PlaybackException) {
            val isNetworkErr = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                error.errorCode == PlaybackException.ERROR_CODE_TIMEOUT ||
                error.errorCodeName.contains("NETWORK", ignoreCase = true)
            if (isNetworkErr) {
                showMessage("Výpadok siete – automaticky obnovujem pripojenie…")
            } else {
                showMessage("Chyba prehrávania (${error.errorCodeName}). Skontroluj spojenie so serverom.")
            }
        }
    }

    private fun onCurrentItemChanged() {
        val c = controller ?: return
        val item = c.currentMediaItem
        if (item == null) {
            _uiState.update { it.copy(currentTrack = NoTrack, queueIndex = -1, isPlaying = false) }
            return
        }
        val idx = c.currentMediaItemIndex
        val track = _uiState.value.queue.getOrNull(idx)?.takeIf { it.id == item.mediaId }
            ?: trackFromMediaItem(item)
        scrobbled = false
        if (track.id.isNotBlank()) {
            val currentQueue = _uiState.value.queue
            _uiState.update {
                val updatedHistory = (listOf(track) + it.historyTracks.filterNot { h -> h.id == track.id }).take(30)
                it.copy(
                    currentTrack = track,
                    queueIndex = idx,
                    historyTracks = updatedHistory,
                    playbackProgress = 0f,
                    positionMs = 0L,
                    durationMs = track.durationSec * 1000L,
                    lyrics = emptyList(),
                    activeLyricIndex = -1,
                    relatedTracks = emptyList()
                )
            }
            viewModelScope.launch(Dispatchers.IO) {
                settings.addTrackToHistory(track)
                settings.savePlaybackState(track, 0L, currentQueue, idx)
            }
            launchApi(onError = { }) { it.scrobble(track.id, submission = false) }
        } else {
            _uiState.update {
                it.copy(
                    currentTrack = track,
                    queueIndex = idx,
                    playbackProgress = 0f,
                    positionMs = 0L,
                    durationMs = track.durationSec * 1000L,
                    lyrics = emptyList(),
                    activeLyricIndex = -1,
                    relatedTracks = emptyList()
                )
            }
        }
        loadLyrics(track)
        if (_uiState.value.nowPlayingTab in setOf("PODOBNÉ", "RELATED")) loadRelated(track)
    }

    private fun startProgressLoop() {
        viewModelScope.launch {
            var loopTicks = 0
            while (isActive) {
                val c = controller
                val isPlaying = c?.isPlaying == true
                val onNowPlaying = _uiState.value.currentScreen == AppScreen.NOW_PLAYING
                // On TV/Mobile browsing screens (Home, Explore, Library, Detail), only tick every 2000ms
                // so D-pad scrolling and list loading never stutter from 500ms state updates!
                val sleepMs = when {
                    !isPlaying -> 1500L
                    onNowPlaying -> 500L
                    else -> 2000L
                }
                delay(sleepMs)
                if (c == null || c.mediaItemCount == 0 || !isPlaying) continue
                val s = _uiState.value
                if (s.isSeeking) continue
                val pos = c.currentPosition
                val dur = if (c.duration != C.TIME_UNSET && c.duration > 0) c.duration else s.currentTrack.durationSec * 1000L
                val progress = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
                if (onNowPlaying || kotlin.math.abs(progress - s.playbackProgress) >= 0.008f) {
                    _uiState.update {
                        it.copy(
                            positionMs = pos,
                            durationMs = dur,
                            playbackProgress = progress,
                            activeLyricIndex = if (onNowPlaying) lyricIndexAt(it.lyrics, pos) else it.activeLyricIndex
                        )
                    }
                }
                maybeScrobble(s.currentTrack, pos, dur)

                loopTicks++
                if (loopTicks % 6 == 0) {
                    settings.savePositionOnly(pos)
                }
            }
        }
    }

    private fun lyricIndexAt(lines: List<LyricLine>, posMs: Long): Int {
        if (lines.isEmpty() || lines.first().startMs < 0) return -1
        return lines.indexOfLast { it.startMs <= posMs }
    }

    private fun maybeScrobble(track: TrackItem, pos: Long, dur: Long) {
        if (scrobbled || track.id.isBlank() || dur < 30_000L) return
        if (pos >= minOf(dur / 2, 240_000L)) {
            scrobbled = true
            launchApi(onError = { }) { it.scrobble(track.id, submission = true) }
        }
    }

    private fun loadLyrics(track: TrackItem) {
        lyricsJob?.cancel()
        if (track.id.isBlank()) return
        _uiState.update { it.copy(lyricsLoading = true) }
        lyricsJob = launchApi(onError = { _uiState.update { s -> s.copy(lyricsLoading = false) } }) { a ->
            val lines = a.lyrics(track)
            _uiState.update { st ->
                if (st.currentTrack.id == track.id) {
                    st.copy(lyrics = lines, lyricsLoading = false, activeLyricIndex = lyricIndexAt(lines, st.positionMs))
                } else st
            }
        }
    }

    private fun loadRelated(track: TrackItem) {
        if (track.id.isBlank() || relatedLoadedFor == track.id) return
        relatedLoadedFor = track.id
        relatedJob?.cancel()
        _uiState.update { it.copy(relatedLoading = true) }
        relatedJob = launchApi(onError = { _uiState.update { s -> s.copy(relatedLoading = false) } }) { a ->
            val similar = a.similarSongs(track.id, 20)
            _uiState.update { st ->
                if (st.currentTrack.id == track.id) st.copy(relatedTracks = similar, relatedLoading = false) else st
            }
            syncLikes(similar)
        }
    }

    private fun isOnCellular(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /**
     * Determines the exact format and bitrate to request from the Navidrome/Subsonic server.
     * Works consistently on Wi-Fi, Ethernet (TV), and Mobile Data!
     */
    private fun currentStreamProfile(): StreamProfile {
        val s = _uiState.value
        val rate = when (s.opusBitrateTarget) {
            "64k" -> 64
            "128k" -> 128
            "192k" -> 192
            "256k" -> 256
            "320k" -> 320
            else -> 192
        }

        // 1. If user explicitly selected a transcoding codec (MP3, Opus, Ogg Vorbis), apply it everywhere (Wi-Fi, TV, Mobile)
        when (s.backendCodec) {
            "MP3 (LAME)" -> return StreamProfile("mp3", minOf(rate, 320), "Prekódovanie • MP3 ${minOf(rate, 320)} kbps")
            "Opus (libopus)" -> return StreamProfile("opus", rate, "Prekódovanie • Opus $rate kbps")
            "Ogg Vorbis" -> return StreamProfile("ogg", rate, "Prekódovanie • Ogg Vorbis $rate kbps")
        }

        // 2. If user is on "FLAC Stream" (Originál bez kompresie), check if Mobile Data Saver should kick in on cellular
        if (isOnCellular() && s.cellularTranscodingEnabled) {
            val mobileRate = minOf(rate, 320)
            return StreamProfile("mp3", mobileRate, "Úspora mobilných dát • MP3 $mobileRate kbps")
        }

        // 3. Otherwise check if loginTranscodeOption was explicitly set to a compressed mode
        return when (s.loginTranscodeOption) {
            "Vysoká (MP3 320 kbps)" -> StreamProfile("mp3", 320, "Prekódovanie • MP3 320 kbps")
            "Mobilné dáta (Opus 192 kbps)" -> StreamProfile("opus", 192, "Prekódovanie • Opus 192 kbps")
            "Úspora dát (Opus 128 kbps)" -> StreamProfile("opus", 128, "Prekódovanie • Opus 128 kbps")
            else -> StreamProfile("raw", null, "Originál bez kompresie (Direct Play)")
        }
    }

    /**
     * Immediately reloads the currently playing track & queue with the newly selected streaming quality
     * while preserving the exact playback position.
     */
    private fun reloadActiveStreamForQualityChange() {
        val a = api ?: return
        val s = _uiState.value
        val profile = currentStreamProfile()
        if (s.queue.isEmpty()) {
            showMessage("Nastavená kvalita: ${profile.label}")
            return
        }
        withController { c ->
            if (c.mediaItemCount == 0) {
                showMessage("Nastavená kvalita: ${profile.label}")
                return@withController
            }
            val safeQueue = s.queue.take(350)
            val idx = c.currentMediaItemIndex.coerceIn(0, safeQueue.lastIndex)
            val pos = c.currentPosition.coerceAtLeast(0L)
            val wasPlaying = c.isPlaying
            val items = safeQueue.map { mediaItemFor(a, it, profile) }
            c.setMediaItems(items, idx, pos)
            c.prepare()
            if (wasPlaying) {
                c.play()
            }
            showMessage("Prehrávanie prepnuté na: ${profile.label}")
        }
    }

    private fun mediaItemFor(a: SubsonicApi?, t: TrackItem, profile: StreamProfile): MediaItem {
        val activeBadge = when (profile.format) {
            "mp3" -> "MP3 ${profile.maxBitRate ?: 320}K"
            "opus" -> "OPUS ${profile.maxBitRate ?: 192}K"
            "ogg" -> "OGG ${profile.maxBitRate ?: 192}K"
            else -> t.badge
        }
        val activeTechLine = when (profile.format) {
            "mp3", "opus", "ogg" -> "${profile.label} (Zdroj: ${t.techLine.ifBlank { t.suffix.uppercase() }})"
            else -> if (t.techLine.isNotBlank()) "${t.techLine} • Originál" else profile.label
        }
        val extras = Bundle().apply {
            putString("badge", activeBadge)
            putString("techLine", activeTechLine)
            putString("albumId", t.albumId)
            putString("artistId", t.artistId)
            putString("coverArtId", t.coverArtId)
            putString("suffix", t.suffix)
            putInt("durationSec", t.durationSec)
            putBoolean("hiRes", profile.format == "raw" && t.isHiResBadgeCyan)
            if (t.replayGainTrackDb != null) putFloat("rgTrack", t.replayGainTrackDb)
            if (t.replayGainAlbumDb != null) putFloat("rgAlbum", t.replayGainAlbumDb)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(t.title)
            .setArtist(t.artist)
            .setAlbumTitle(t.album)
            .setArtworkUri(if (t.imageUrl.isBlank()) null else Uri.parse(t.imageUrl))
            .setExtras(extras)
            .build()

        val isDownloaded = t.id in _uiState.value.downloadedTrackIds
        val localPath = if (isDownloaded && profile.format == "raw") offlineManager.getLocalFileUri(t.id) else null
        val uri = if (localPath != null) {
            Uri.fromFile(File(localPath))
        } else if (a != null) {
            Uri.parse(a.streamUrl(t.id, profile.format, profile.maxBitRate))
        } else {
            Uri.EMPTY
        }

        return MediaItem.Builder()
            .setMediaId(t.id)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun trackFromMediaItem(item: MediaItem): TrackItem {
        val md = item.mediaMetadata
        val ex = md.extras
        val durationSec = ex?.getInt("durationSec") ?: 0
        return TrackItem(
            id = item.mediaId,
            title = md.title?.toString().orEmpty(),
            artist = md.artist?.toString().orEmpty(),
            album = md.albumTitle?.toString().orEmpty(),
            duration = com.example.data.formatDuration(durationSec),
            badge = ex?.getString("badge").orEmpty(),
            imageUrl = md.artworkUri?.toString().orEmpty(),
            isHiResBadgeCyan = ex?.getBoolean("hiRes") ?: true,
            albumId = ex?.getString("albumId").orEmpty(),
            artistId = ex?.getString("artistId").orEmpty(),
            durationSec = durationSec,
            suffix = ex?.getString("suffix").orEmpty(),
            coverArtId = ex?.getString("coverArtId").orEmpty(),
            techLine = ex?.getString("techLine").orEmpty(),
            replayGainTrackDb = if (ex != null && ex.containsKey("rgTrack")) ex.getFloat("rgTrack") else null,
            replayGainAlbumDb = if (ex != null && ex.containsKey("rgAlbum")) ex.getFloat("rgAlbum") else null
        )
    }

    private fun startQueue(list: List<TrackItem>, startIndex: Int, from: String, shuffle: Boolean?) {
        val a = api
        val downloadedIds = _uiState.value.downloadedTrackIds
        val isOfflinePlayback = list.all { it.id in downloadedIds }
        if (a == null && !isOfflinePlayback) {
            showMessage("Najprv sa prihlás k serveru alebo prehraj stiahnuté skladby.")
            return
        }
        if (list.isEmpty() || startIndex !in list.indices) return
        // Cap queue sent over Binder IPC to 100 tracks to keep Android TV IPC & memory instant
        val window = list.drop(startIndex).take(100)
        val profile = currentStreamProfile()
        val items = window.map { mediaItemFor(a, it, profile) }
        scrobbled = false
        relatedLoadedFor = ""
        val track = window[0]
        _uiState.update {
            val updatedHistory = (listOf(track) + it.historyTracks.filterNot { h -> h.id == track.id }).take(30)
            it.copy(
                queue = window,
                queueIndex = 0,
                currentTrack = track,
                historyTracks = updatedHistory,
                playingFrom = from,
                isPlaying = true,
                isMiniPlayerDismissed = false,
                playbackProgress = 0f,
                positionMs = 0L,
                durationMs = track.durationSec * 1000L,
                lyrics = emptyList(),
                activeLyricIndex = -1,
                relatedTracks = emptyList()
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            settings.addTrackToHistory(track)
            settings.savePlaybackState(track, 0L, window, 0)
        }
        withController { c ->
            if (shuffle != null) c.shuffleModeEnabled = shuffle
            c.setMediaItems(items, 0, 0L)
            c.prepare()
            c.play()
        }
    }

    /**
     * Plays [track]. When [context] (the list the user tapped it in) contains the track, the whole
     * list becomes the queue so next/previous work as expected.
     */
    fun playTrack(
        track: TrackItem,
        openFullPlayer: Boolean = false,
        context: List<TrackItem> = emptyList(),
        from: String = ""
    ) {
        val list = if (context.any { it.id == track.id }) context else listOf(track)
        val idx = list.indexOfFirst { it.id == track.id }
        startQueue(list, idx, from, null)
        if (openFullPlayer) navigateTo(AppScreen.NOW_PLAYING)
    }

    fun playAll(list: List<TrackItem>, shuffle: Boolean = false, from: String = "") {
        if (list.isEmpty()) return
        if (shuffle) {
            startQueue(list.shuffled(), 0, from, true)
        } else {
            startQueue(list, 0, from, false)
        }
    }

    fun playQueueIndex(index: Int) {
        withController { c ->
            if (index in 0 until c.mediaItemCount) {
                c.seekToDefaultPosition(index)
                c.play()
            }
        }
    }

    fun togglePlayPause() {
        withController { c ->
            if (c.mediaItemCount == 0) return@withController
            if (c.isPlaying) {
                c.pause()
                val pos = c.currentPosition
                settings.savePositionOnly(pos)
            } else {
                if (c.playbackState == Player.STATE_IDLE) c.prepare()
                c.play()
                _uiState.update { it.copy(isMiniPlayerDismissed = false) }
            }
        }
    }

    fun dismissMiniPlayer() {
        withController { c ->
            c.pause()
        }
        _uiState.update { it.copy(isMiniPlayerDismissed = true, isPlaying = false) }
    }

    fun skipToNextTrack() {
        withController { c -> if (c.hasNextMediaItem()) c.seekToNextMediaItem() }
    }

    fun skipToPreviousTrack() {
        withController { c -> c.seekToPrevious() }
    }

    /** Called while the user drags the progress bar; the actual seek happens when they pause for a moment. */
    fun setPlaybackProgress(progress: Float) {
        val p = progress.coerceIn(0f, 1f)
        val dur = _uiState.value.durationMs
        val pos = (dur * p).toLong()
        _uiState.update {
            it.copy(isSeeking = true, playbackProgress = p, positionMs = pos, activeLyricIndex = lyricIndexAt(it.lyrics, pos))
        }
        seekJob?.cancel()
        seekJob = viewModelScope.launch {
            delay(250)
            withController { c ->
                c.seekTo(pos)
                settings.savePositionOnly(pos)
            }
            _uiState.update { it.copy(isSeeking = false) }
        }
    }

    fun toggleTrackLike(trackId: String) {
        if (trackId.isBlank()) return
        val wasLiked = trackId in _uiState.value.likedTrackIds
        _uiState.update {
            it.copy(likedTrackIds = if (wasLiked) it.likedTrackIds - trackId else it.likedTrackIds + trackId)
        }
        launchApi(onError = { msg ->
            _uiState.update {
                it.copy(likedTrackIds = if (wasLiked) it.likedTrackIds + trackId else it.likedTrackIds - trackId)
            }
            showMessage(msg)
        }) { a ->
            if (wasLiked) a.unstar(trackId) else a.star(trackId)
            val fav = safe<List<TrackItem>?>(null) { a.starredTracks() }
            if (fav != null) _uiState.update { it.copy(favoriteTracks = fav) }
        }
    }

    fun toggleShuffle() {
        withController { c -> c.shuffleModeEnabled = !c.shuffleModeEnabled }
    }

    fun toggleRepeatOne() {
        withController { c ->
            c.repeatMode = if (c.repeatMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
        }
    }

    fun setNowPlayingTab(tab: String) {
        _uiState.update { it.copy(nowPlayingTab = tab) }
        if (tab in setOf("PODOBNÉ", "RELATED")) loadRelated(_uiState.value.currentTrack)
    }

    /** Tapping a synced lyric line jumps to that line. */
    fun setActiveLyricIndex(index: Int) {
        val line = _uiState.value.lyrics.getOrNull(index) ?: return
        if (line.startMs < 0) return
        withController { it.seekTo(line.startMs) }
        _uiState.update { it.copy(activeLyricIndex = index, positionMs = line.startMs) }
    }

    /** "Náhodný mix" on the Library screen: random songs from the whole server library. */
    fun playRandomMix() {
        launchApi { a ->
            val songs = a.randomSongs(50)
            if (songs.isEmpty()) {
                showMessage("Na serveri sa nenašli žiadne skladby.")
            } else {
                syncLikes(songs)
                playAll(songs, shuffle = false, from = "Náhodný mix")
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Home / Explore / Library UI actions
    // ---------------------------------------------------------------------------------------------

    fun setHomeFilter(filter: String) {
        _uiState.update { it.copy(homeFilter = filter) }
        loadHome()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, exploreError = null) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(exploreLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            val a = api ?: return@launch
            _uiState.update { it.copy(exploreLoading = true) }
            try {
                val result = a.search(query.trim())
                _uiState.update { it.copy(searchResult = result, exploreLoading = false, exploreError = null) }
                syncLikes(result.tracks)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SubsonicException) {
                _uiState.update { it.copy(exploreLoading = false, exploreError = e.message) }
            }
        }
    }

    fun setExploreCategory(category: String) {
        _uiState.update { it.copy(exploreCategory = category) }
        if (category == "Power Metal") {
            setSearchQuery("Power Metal")
        }
    }

    fun toggleAudiophileFilter(filter: String) {
        _uiState.update { state ->
            val updated = if (filter in state.activeAudiophileFilters) {
                state.activeAudiophileFilters - filter
            } else {
                state.activeAudiophileFilters + filter
            }
            state.copy(activeAudiophileFilters = updated)
        }
    }

    fun resetAudiophileFilters() {
        _uiState.update { it.copy(activeAudiophileFilters = emptySet()) }
    }

    fun triggerServerScan() {
        if (_uiState.value.isScanningServer) return
        _uiState.update { it.copy(isScanningServer = true, lastSyncTime = "Spúšťam skenovanie knižnice…") }
        launchApi(onError = { msg ->
            _uiState.update { it.copy(isScanningServer = false, lastSyncTime = "Skenovanie zlyhalo: $msg") }
        }) { a ->
            var st = a.startScan()
            var guard = 0
            while (st.scanning && guard < 300) {
                _uiState.update { it.copy(lastSyncTime = "Skenujem knižnicu… ${st.count} súborov") }
                delay(1000)
                st = a.scanStatus()
                guard++
            }
            _uiState.update {
                it.copy(isScanningServer = false, lastSyncTime = "Skenovanie dokončené · ${st.count} súborov")
            }
            loadAll()
        }
    }

    fun setLibraryFilterChip(chip: String) {
        _uiState.update { it.copy(libraryFilterChip = chip) }
    }

    fun toggleLibraryGridView() {
        _uiState.update { it.copy(libraryIsGridView = !it.libraryIsGridView) }
    }

    fun createNewPlaylist(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) {
            showMessage("Zadaj názov playlistu.")
            return
        }
        launchApi { a ->
            a.createPlaylist(clean)
            val pls = a.playlists()
            _uiState.update { it.copy(libraryPlaylists = pls) }
            showMessage("Playlist „$clean“ bol vytvorený.")
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Equalizer & DSP (Hardware AudioEffects)
    // ---------------------------------------------------------------------------------------------

    fun toggleEqMaster(enabled: Boolean) {
        settings.putBoolean(SettingsStore.KEY_EQ_MASTER, enabled)
        _uiState.update { it.copy(eqMasterEnabled = enabled) }
        AudioEffectsManager.setMasterEnabled(enabled)
    }

    fun setPreampDb(value: Float) {
        val rounded = (Math.round(value * 2f) / 2f).coerceIn(-12f, 12f)
        settings.putFloat(SettingsStore.KEY_EQ_PREAMP, rounded)
        _uiState.update { it.copy(preampDb = rounded) }
        AudioEffectsManager.setBands(_uiState.value.eqBands, rounded)
    }

    fun setEqBand(index: Int, value: Float) {
        val rounded = (Math.round(value * 2f) / 2f).coerceIn(-12f, 12f)
        _uiState.update { state ->
            val updated = state.eqBands.toMutableList()
            if (index in updated.indices) {
                updated[index] = rounded
            }
            settings.saveEqBands(updated)
            settings.putString(SettingsStore.KEY_EQ_PRESET, "Vlastný (Custom)")
            state.copy(eqBands = updated, activePresetName = "Vlastný (Custom)")
        }
        AudioEffectsManager.setBands(_uiState.value.eqBands, _uiState.value.preampDb)
    }

    fun resetEqFlat() {
        val flat = List(10) { 0f }
        settings.saveEqBands(flat)
        settings.putString(SettingsStore.KEY_EQ_PRESET, "Audiofil Flat (Referenčný)")
        settings.putFloat(SettingsStore.KEY_EQ_PREAMP, 0.0f)
        _uiState.update {
            it.copy(
                eqBands = flat,
                preampDb = 0.0f,
                activePresetName = "Audiofil Flat (Referenčný)"
            )
        }
        AudioEffectsManager.setBands(flat, 0.0f)
    }

    fun invertEqBands() {
        val inverted = _uiState.value.eqBands.map { (-it).coerceIn(-12f, 12f) }
        settings.saveEqBands(inverted)
        settings.putString(SettingsStore.KEY_EQ_PRESET, "Invertovaný")
        _uiState.update { state ->
            state.copy(
                eqBands = inverted,
                activePresetName = "Invertovaný"
            )
        }
        AudioEffectsManager.setBands(inverted, _uiState.value.preampDb)
    }

    fun selectEqPreset(presetName: String) {
        val values = SoundWaveCatalog.eqPresets[presetName] ?: return
        settings.saveEqBands(values)
        settings.putString(SettingsStore.KEY_EQ_PRESET, presetName)
        _uiState.update {
            it.copy(
                eqBands = values,
                activePresetName = presetName
            )
        }
        AudioEffectsManager.setBands(values, _uiState.value.preampDb)
    }

    fun setSubBassBoostDb(value: Float) {
        val rounded = (Math.round(value * 2f) / 2f).coerceIn(0f, 10f)
        settings.putFloat(SettingsStore.KEY_SUB_BASS, rounded)
        _uiState.update { it.copy(subBassBoostDb = rounded) }
        AudioEffectsManager.setSubBassBoost(rounded)
    }

    fun toggleCrossfeed(enabled: Boolean) {
        settings.putBoolean(SettingsStore.KEY_SPATIAL, enabled)
        _uiState.update { it.copy(crossfeedEnabled = enabled) }
        AudioEffectsManager.setSpatialEnabled(enabled)
        showMessage(if (enabled) "Priestorový 3D zvuk (Virtualizer) zapnutý." else "Priestorový 3D zvuk vypnutý.")
    }

    fun toggleAutoGain(enabled: Boolean) {
        settings.putBoolean(SettingsStore.KEY_AUTO_GAIN, enabled)
        _uiState.update { it.copy(autoGainEnabled = enabled) }
        AudioEffectsManager.setAutoGainEnabled(enabled)
        showMessage(if (enabled) "Ochrana proti chrčaniu (Auto-Gain) zapnutá." else "Ochrana proti chrčaniu vypnutá.")
    }

    fun toggleBluetoothVolumeStabilizer(enabled: Boolean) {
        settings.putBoolean(SettingsStore.KEY_BT_STABILIZER, enabled)
        _uiState.update { it.copy(bluetoothVolumeStabilizerEnabled = enabled) }
        AudioEffectsManager.setBluetoothVolumeStabilizerEnabled(enabled)
        showMessage(
            if (enabled) {
                "Núdzový BT bypass zapnutý – EQ je obídený a hlasitosť zamknutá na 100 %."
            } else {
                "Núdzový BT bypass vypnutý – ekvalizér a basy sú aktívne aj cez Bluetooth."
            }
        )
    }

    // AutoEQ Modal
    fun setAutoEqSearchQuery(query: String) {
        val q = query.trim()
        val matched = if (q.length >= 2) {
            SoundWaveCatalog.headphoneProfiles.firstOrNull {
                it.name.contains(q, ignoreCase = true) ||
                    it.targetInfo.contains(q, ignoreCase = true) ||
                    it.id.contains(q, ignoreCase = true)
            }
        } else null
        _uiState.update { st ->
            if (matched != null) {
                val targetCurve = if (matched.targetInfo.contains("In-Ear", ignoreCase = true)) {
                    "Harman In-Ear 2019"
                } else {
                    "Harman Target 2019v2"
                }
                st.copy(
                    autoEqSearchQuery = query,
                    selectedHeadphone = matched,
                    autoEqTargetCurve = targetCurve
                )
            } else {
                st.copy(autoEqSearchQuery = query)
            }
        }
    }

    fun setAutoEqTargetCurve(target: String) {
        _uiState.update { it.copy(autoEqTargetCurve = target) }
    }

    fun selectHeadphoneProfile(profile: HeadphoneProfile) {
        val targetCurve = if (profile.targetInfo.contains("In-Ear", ignoreCase = true)) {
            "Harman In-Ear 2019"
        } else {
            "Harman Target 2019v2"
        }
        val preampVal = profile.preampDb.replace("dB", "").trim().toFloatOrNull() ?: -5.5f
        val state = _uiState.value
        val scale = (state.calibrationDryWetPct / 100f).coerceIn(0f, 1f)
        val calibratedBands = profile.eqBands.map { db ->
            val v = db * scale
            (Math.round(v * 2f) / 2f).coerceIn(-12f, 12f)
        }
        val finalPreamp = if (state.autoPreampEnabled) preampVal else state.preampDb
        val presetTitle = "AutoEQ: ${profile.name}"
        settings.saveEqBands(calibratedBands)
        settings.putFloat(SettingsStore.KEY_EQ_PREAMP, finalPreamp)
        settings.putString(SettingsStore.KEY_EQ_PRESET, presetTitle)
        _uiState.update {
            it.copy(
                selectedHeadphone = profile,
                autoEqTargetCurve = targetCurve,
                eqBands = calibratedBands,
                preampDb = finalPreamp,
                activePresetName = presetTitle
            )
        }
        AudioEffectsManager.setBands(calibratedBands, finalPreamp)
    }

    fun setCalibrationDryWet(pct: Float) {
        _uiState.update { it.copy(calibrationDryWetPct = pct.coerceIn(0f, 100f)) }
    }

    fun toggleAutoPreamp(enabled: Boolean) {
        _uiState.update { it.copy(autoPreampEnabled = enabled) }
    }

    fun toggleSubBassCompensation(enabled: Boolean) {
        _uiState.update { it.copy(subBassCompensationEnabled = enabled) }
    }

    fun applyAutoEqProfileToDsp() {
        val state = _uiState.value
        val profile = state.selectedHeadphone
        val preampVal = profile.preampDb.replace("dB", "").trim().toFloatOrNull() ?: -5.5f
        val scale = (state.calibrationDryWetPct / 100f).coerceIn(0f, 1f)
        val calibratedBands = profile.eqBands.map { db ->
            val v = db * scale
            (Math.round(v * 2f) / 2f).coerceIn(-12f, 12f)
        }
        val finalPreamp = if (state.autoPreampEnabled) preampVal else state.preampDb
        val presetTitle = "AutoEQ: ${profile.name}"
        settings.saveEqBands(calibratedBands)
        settings.putFloat(SettingsStore.KEY_EQ_PREAMP, finalPreamp)
        settings.putString(SettingsStore.KEY_EQ_PRESET, presetTitle)
        _uiState.update {
            it.copy(
                eqBands = calibratedBands,
                preampDb = finalPreamp,
                activePresetName = presetTitle,
                showAutoEqModal = false
            )
        }
        AudioEffectsManager.setBands(calibratedBands, finalPreamp)
        showMessage("Kalibračný profil „${profile.name}“ (${profile.preampDb}) bol aplikovaný do zvuku.")
    }

    // ---------------------------------------------------------------------------------------------
    // Sleep Timer (Service-owned so it survives Activity closing)
    // ---------------------------------------------------------------------------------------------

    fun setShowSleepTimerModal(show: Boolean) {
        _uiState.update { it.copy(showSleepTimerModal = show) }
    }

    fun setSleepTimer(option: SleepTimerOption) {
        _uiState.update { it.copy(showSleepTimerModal = false) }
        SleepTimerCoordinator.setTimer(option)
        when (option) {
            SleepTimerOption.OFF -> showMessage("Časovač spánku bol vypnutý.")
            SleepTimerOption.END_OF_TRACK -> showMessage("Časovač spánku: Prehrávanie sa zastaví na konci tejto skladby.")
            else -> showMessage("Časovač spánku nastavený na ${option.label}.")
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Queue Management
    // ---------------------------------------------------------------------------------------------

    fun playNext(track: TrackItem) {
        val a = api
        val profile = currentStreamProfile()
        val mediaItem = mediaItemFor(a, track, profile)
        withController { c ->
            val insertIdx = if (c.mediaItemCount == 0) 0 else (c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount)
            c.addMediaItem(insertIdx, mediaItem)
            _uiState.update { state ->
                val q = state.queue.toMutableList()
                if (insertIdx in 0..q.size) {
                    q.add(insertIdx, track)
                } else {
                    q.add(track)
                }
                state.copy(queue = q)
            }
            showMessage("Skladba „${track.title}“ bude prehraná ako nasledujúca.")
        }
    }

    fun addToQueue(track: TrackItem) {
        val a = api
        val profile = currentStreamProfile()
        val mediaItem = mediaItemFor(a, track, profile)
        withController { c ->
            c.addMediaItem(mediaItem)
            _uiState.update { state ->
                state.copy(queue = state.queue + track)
            }
            showMessage("Skladba „${track.title}“ bola pridaná do poradia.")
        }
    }

    fun removeFromQueue(index: Int) {
        withController { c ->
            if (index in 0 until c.mediaItemCount) {
                c.removeMediaItem(index)
                _uiState.update { state ->
                    val q = state.queue.toMutableList()
                    if (index in q.indices) {
                        q.removeAt(index)
                    }
                    val newIdx = c.currentMediaItemIndex
                    state.copy(queue = q, queueIndex = newIdx)
                }
            }
        }
    }

    fun clearUpcomingQueue() {
        withController { c ->
            val curr = c.currentMediaItemIndex
            val count = c.mediaItemCount
            if (count > curr + 1) {
                for (i in (count - 1) downTo (curr + 1)) {
                    c.removeMediaItem(i)
                }
                _uiState.update { state ->
                    state.copy(queue = state.queue.take(curr + 1))
                }
                showMessage("Nasledujúce skladby v poradí boli vymazané.")
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Track Actions Sheet
    // ---------------------------------------------------------------------------------------------

    fun setTrackMenuTarget(track: TrackItem?) {
        _uiState.update { it.copy(trackMenuTarget = track) }
    }

    // ---------------------------------------------------------------------------------------------
    // Offline Storage & Downloads
    // ---------------------------------------------------------------------------------------------

    fun refreshDownloadedTracks() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { offlineManager.getAllDownloadedTracks() }
            val bytes = withContext(Dispatchers.IO) { offlineManager.getDownloadedTotalSizeBytes() }
            val formatted = formatMb(bytes)
            _uiState.update {
                it.copy(
                    downloadedTracks = list,
                    downloadedTrackIds = list.map { t -> t.id }.toSet(),
                    downloadedSizeText = formatted
                )
            }
        }
    }

    fun downloadTrack(track: TrackItem) {
        if (offlineManager.isDownloaded(track.id)) {
            showMessage("Skladba „${track.title}“ už je stiahnutá.")
            return
        }
        val a = api
        if (a == null) {
            showMessage("Pre stiahnutie skladby sa najprv prihlás k serveru.")
            return
        }
        _uiState.update { it.copy(downloadingTrackIds = it.downloadingTrackIds + track.id) }
        viewModelScope.launch {
            val url = a.downloadUrl(track.id)
            val success = offlineManager.downloadTrack(url, track)
            _uiState.update { it.copy(downloadingTrackIds = it.downloadingTrackIds - track.id) }
            if (success) {
                showMessage("Skladba „${track.title}“ bola stiahnutá na offline počúvanie.")
                refreshDownloadedTracks()
            } else {
                showMessage("Sťahovanie skladby „${track.title}“ zlyhalo. Skontroluj pripojenie.")
            }
        }
    }

    fun deleteDownloadedTrack(trackId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { offlineManager.deleteDownloadedTrack(trackId) }
            refreshDownloadedTracks()
            showMessage("Stiahnutá skladba bola vymazaná z úložiska.")
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { offlineManager.clearAllDownloads() }
            refreshDownloadedTracks()
            showMessage("Všetky stiahnuté skladby boli vymazané z úložiska.")
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Streaming Quality & Server Settings
    // ---------------------------------------------------------------------------------------------

    fun toggleCellularTranscoding() {
        val next = !_uiState.value.cellularTranscodingEnabled
        settings.putBoolean(SettingsStore.KEY_CELLULAR_TRANSCODING, next)
        _uiState.update { it.copy(cellularTranscodingEnabled = next) }
        reloadActiveStreamForQualityChange()
    }

    fun setOpusBitrateTarget(target: String) {
        settings.putString(SettingsStore.KEY_OPUS_BITRATE, target)
        // If user was on "FLAC Stream" (Originál) and tapped a specific kbps bitrate button,
        // automatically switch codec to MP3 (LAME) so the chosen bitrate takes effect immediately!
        val currentCodec = _uiState.value.backendCodec
        val nextCodec = if (currentCodec == "FLAC Stream" && target != "Originál") "MP3 (LAME)" else currentCodec
        if (nextCodec != currentCodec) {
            settings.putString(SettingsStore.KEY_BACKEND_CODEC, nextCodec)
        }
        _uiState.update { it.copy(opusBitrateTarget = target, backendCodec = nextCodec) }
        reloadActiveStreamForQualityChange()
    }

    fun setBackendCodec(codec: String) {
        settings.putString(SettingsStore.KEY_BACKEND_CODEC, codec)
        // Reset loginTranscodeOption override if user picks a codec directly
        if (codec == "FLAC Stream") {
            settings.putString(SettingsStore.KEY_LOGIN_TRANSCODE, "Pôvodná kvalita (Direct Stream)")
            _uiState.update {
                it.copy(
                    backendCodec = codec,
                    loginTranscodeOption = "Pôvodná kvalita (Direct Stream)"
                )
            }
        } else {
            _uiState.update { it.copy(backendCodec = codec) }
        }
        reloadActiveStreamForQualityChange()
    }

    fun cycleMaxSampleRate() {
        val rates = listOf("48 kHz", "96 kHz", "192 kHz", "44.1 kHz")
        val next = rates[(rates.indexOf(_uiState.value.maxSampleRate) + 1) % rates.size]
        _uiState.update { it.copy(maxSampleRate = next) }
    }

    fun toggleHardwareAccelVaapi() {
        _uiState.update { it.copy(hardwareAccelVaapi = !it.hardwareAccelVaapi) }
    }

    fun toggleBitPerfectExclusive() {
        val next = !_uiState.value.bitPerfectExclusiveMode
        _uiState.update { it.copy(bitPerfectExclusiveMode = next) }
        if (next) {
            setBackendCodec("FLAC Stream")
        }
    }

    fun toggleReplayGain() {
        val next = !_uiState.value.replayGainEnabled
        settings.putBoolean(SettingsStore.KEY_REPLAY_GAIN_ENABLED, next)
        _uiState.update { it.copy(replayGainEnabled = next) }
        AudioEffectsManager.setReplayGain(next, _uiState.value.replayGainPreampDb, _uiState.value.replayGainMode)
        showMessage(if (next) "Vyrovnávanie hlasitosti (ReplayGain) zapnuté." else "Vyrovnávanie hlasitosti vypnuté.")
    }

    fun setReplayGainMode(mode: String) {
        settings.putString(SettingsStore.KEY_REPLAY_GAIN_MODE, mode)
        _uiState.update { it.copy(replayGainMode = mode) }
        AudioEffectsManager.setReplayGain(_uiState.value.replayGainEnabled, _uiState.value.replayGainPreampDb, mode)
    }

    fun setReplayGainPreamp(db: Float) {
        val rounded = (Math.round(db * 2f) / 2f).coerceIn(-6f, 6f)
        settings.putFloat(SettingsStore.KEY_REPLAY_GAIN_PREAMP, rounded)
        _uiState.update { it.copy(replayGainPreampDb = rounded) }
        AudioEffectsManager.setReplayGain(_uiState.value.replayGainEnabled, rounded, _uiState.value.replayGainMode)
    }

    fun toggleGapless() {
        _uiState.update { it.copy(gaplessEnabled = !it.gaplessEnabled) }
    }

    fun setCrossfadeSeconds(sec: Int) {
        _uiState.update { it.copy(crossfadeSeconds = sec.coerceIn(0, 12)) }
    }

    private fun dirSize(f: File): Long =
        if (f.isFile) f.length() else (f.listFiles()?.sumOf { dirSize(it) } ?: 0L)

    private fun formatMb(bytes: Long): String =
        String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))

    private fun refreshCacheSize() {
        viewModelScope.launch {
            val size = withContext(Dispatchers.IO) { dirSize(getApplication<Application>().cacheDir) }
            _uiState.update { it.copy(cacheSizeText = "Vyrovnávacia pamäť (obaly + audio cache) zaberá ${formatMb(size)}") }
        }
    }

    fun clearStreamCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                MediaCacheManager.clearAllCaches(getApplication())
            }
            refreshCacheSize()
            showMessage("Vyrovnávacia pamäť bola vymazaná.")
        }
    }

    fun runTranscodingDiagnostics() {
        if (_uiState.value.isTestingDiagnostics) return
        _uiState.update { it.copy(isTestingDiagnostics = true, diagnosticResultVisible = false) }
        launchApi(onError = { msg ->
            _uiState.update {
                it.copy(isTestingDiagnostics = false, diagnosticResultVisible = true, diagnosticResultText = "Test zlyhal: $msg")
            }
        }) { a ->
            val latencies = (1..3).map {
                val t0 = SystemClock.elapsedRealtime()
                a.ping()
                (SystemClock.elapsedRealtime() - t0).toInt()
            }
            val avg = latencies.average().toInt()
            val s = _uiState.value
            val profile = currentStreamProfile()
            val sample = s.currentTrack.takeIf { it.id.isNotBlank() }
                ?: s.homeQuickPicks.firstOrNull()
                ?: a.randomSongs(1).firstOrNull()

            val text = StringBuilder("✓ Odozva Navidrome servera: $avg ms")
            if (sample != null) {
                val probe = a.probeStream(a.streamUrl(sample.id, profile.format, profile.maxBitRate))
                val kbps = if (probe.totalMs > 0) probe.bytes * 8L / probe.totalMs else 0L
                val sourceFormat = sample.suffix.uppercase(Locale.ROOT).ifBlank { "NEZNÁMY" }
                val respType = probe.contentType.lowercase(Locale.ROOT).ifBlank { "neznámy" }

                text.append("\n• Testovaná skladba: „${sample.title}“")
                text.append("\n• Pôvodný súbor na serveri: $sourceFormat (${sample.techLine.ifBlank { "štandard" }})")
                text.append("\n• Zvolený režim v aplikácii: ${profile.label}")
                text.append("\n• Skutočná odpoveď servera: $respType (~$kbps kbps, prvý bajt za ${probe.firstByteMs} ms)")

                if (sourceFormat == "MP3" && profile.format == "raw") {
                    text.append("\n\nℹ️ Poznámka: Táto skladba je už na tvojom serveri uložená ako MP3. Prepnutie na FLAC nemôže zvýšiť kvalitu súboru, ktorý bol uložený ako MP3.")
                } else if (profile.format != null && profile.format != "raw" && (respType.contains("flac") || (sourceFormat == "FLAC" && !respType.contains(profile.format) && !respType.contains("mpeg") && !respType.contains("ogg")))) {
                    text.append("\n\n⚠️ Navidrome poslal pôvodný súbor bez prekódovania! Ak chceš na serveri povoliť prekódovanie do MP3/Opus, otvor Navidrome cez web -> Prehrávače (Players) -> pri „SoundWave“ zapni Prekódovanie (Transcoding).")
                } else {
                    text.append("\n\n✓ Streamovanie a kvalita fungujú správne!")
                }
            } else {
                text.append("\nKnižnica je prázdna, test streamu preskočený.")
            }
            _uiState.update {
                it.copy(
                    isTestingDiagnostics = false,
                    diagnosticResultVisible = true,
                    diagnosticResultText = text.toString(),
                    pingMs = avg
                )
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Server Login form
    // ---------------------------------------------------------------------------------------------

    fun setServerUrl(url: String) {
        _uiState.update { it.copy(serverUrl = url, isServerConnectedSuccess = false, connectionError = null) }
    }

    fun setUsername(user: String) {
        _uiState.update { it.copy(username = user, isServerConnectedSuccess = false, connectionError = null) }
    }

    fun setPassword(pwd: String) {
        _uiState.update { it.copy(password = pwd, isServerConnectedSuccess = false, connectionError = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleTokenHint() {
        _uiState.update { it.copy(showTokenHint = !it.showTokenHint) }
    }

    fun toggleAdvancedAudioAccordion() {
        _uiState.update { it.copy(showAdvancedAudioAccordion = !it.showAdvancedAudioAccordion) }
    }

    fun toggleLoginOfflineCache() {
        _uiState.update { it.copy(loginOfflineCacheEnabled = !it.loginOfflineCacheEnabled) }
    }

    fun toggleLoginPreferFlac() {
        _uiState.update { it.copy(loginPreferFlacEnabled = !it.loginPreferFlacEnabled) }
    }

    fun cycleLoginTranscodeOption() {
        val options = listOf(
            "Pôvodná kvalita (Direct Stream)",
            "Vysoká (MP3 320 kbps)",
            "Mobilné dáta (Opus 192 kbps)",
            "Úspora dát (Opus 128 kbps)"
        )
        val next = options[(options.indexOf(_uiState.value.loginTranscodeOption) + 1) % options.size]
        settings.putString(SettingsStore.KEY_LOGIN_TRANSCODE, next)
        _uiState.update { it.copy(loginTranscodeOption = next) }
    }

    /** Public Navidrome demo server – handy for trying the app without your own server. */
    fun fillDemoServerData() {
        _uiState.update {
            it.copy(
                serverUrl = "https://demo.navidrome.org",
                username = "demo",
                password = "demo",
                isServerConnectedSuccess = false,
                connectionError = null
            )
        }
    }

    fun dismissOnboarding() {
        settings.setOnboardingCompleted(true)
        _uiState.update { it.copy(showOnboarding = false) }
    }

    fun showOnboardingGuide() {
        _uiState.update { it.copy(showOnboarding = true, showScreenSwitcherSheet = false) }
    }

    fun openServerLoginFromOnboarding() {
        settings.setOnboardingCompleted(true)
        _uiState.update {
            it.copy(
                showOnboarding = false,
                currentScreen = AppScreen.SERVER_LOGIN
            )
        }
    }

    fun startDemoFromOnboarding() {
        settings.setOnboardingCompleted(true)
        _uiState.update {
            it.copy(
                showOnboarding = false,
                serverUrl = "https://demo.navidrome.org",
                username = "demo",
                password = "demo",
                connectionError = null
            )
        }
        connectToServer()
    }

    fun clearHistory() {
        settings.clearHistory()
        _uiState.update { it.copy(historyTracks = emptyList()) }
        showMessage("História počúvania bola vymazaná.")
    }

    override fun onCleared() {
        controller?.let { c ->
            if (c.mediaItemCount > 0) {
                val pos = c.currentPosition
                val s = _uiState.value
                if (s.currentTrack.id.isNotBlank()) {
                    settings.savePlaybackState(s.currentTrack, pos, s.queue, s.queueIndex)
                }
            }
        }
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        super.onCleared()
    }
}
