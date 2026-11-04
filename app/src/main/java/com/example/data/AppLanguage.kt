package com.example.data

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    SYSTEM("SYSTEM", "Systém", "🌐"),
    SLOVAK("sk", "Slovenčina", "🇸🇰"),
    ENGLISH("en", "English", "🇬🇧"),
    CZECH("cs", "Čeština", "🇨🇿");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
    }
}

data class AppStringsBundle(
    // Bottom Navigation
    val navHome: String,
    val navExplore: String,
    val navLibrary: String,
    val navSettings: String,

    // Now Playing Screen
    val nowPlayingTitle: String,
    val playingFrom: String,
    val tabUpcoming: String,
    val tabLyrics: String,
    val tabSimilar: String,
    val emptyLyrics: String,
    val noTrackPlaying: String,

    // Settings Screen
    val settingsTitle: String,
    val sectionLanguage: String,
    val sectionTheme: String,
    val sectionStreaming: String,
    val sectionOffline: String,
    val sectionAbout: String,
    val simpleModeTitle: String,
    val simpleModeSubtitle: String,
    val wifiStreamingTitle: String,
    val cellularStreamingTitle: String,
    val serverLogoutButton: String,
    val serverConnected: String,
    val serverDisconnected: String,

    // General & Dialogs
    val searchPlaceholder: String,
    val quickPicks: String,
    val playAll: String,
    val favoriteTracks: String,
    val recentHistory: String,
    val offlineCacheTitle: String,
    val autoEqTitle: String,
    val equalizerTitle: String,
    val sleepTimerTitle: String
)

object AppStrings {
    private val slovakBundle = AppStringsBundle(
        navHome = "Domov",
        navExplore = "Objavuj",
        navLibrary = "Knižnica",
        navSettings = "Nastavenia",
        nowPlayingTitle = "Práve hrá",
        playingFrom = "Prehráva sa z: ",
        tabUpcoming = "NASLEDUJÚCE",
        tabLyrics = "TEXT",
        tabSimilar = "PODOBNÉ",
        emptyLyrics = "Pre túto skladbu nie je k dispozícii žiadny text.",
        noTrackPlaying = "Žiadna vybraná skladba",
        settingsTitle = "Nastavenia a kvalita",
        sectionLanguage = "Jazyk aplikácie",
        sectionTheme = "Motív aplikácie",
        sectionStreaming = "Kvalita streamovania",
        sectionOffline = "Lokálna vyrovnávacia pamäť a stiahnuté",
        sectionAbout = "O aplikácii SoundWave",
        simpleModeTitle = "Zjednodušený režim",
        simpleModeSubtitle = "Skryje technické detaily a zobrazí iba to najdôležitejšie",
        wifiStreamingTitle = "Streamovanie cez Wi-Fi",
        cellularStreamingTitle = "Mobilné dáta a transkódovanie",
        serverLogoutButton = "Odhlásiť sa zo servera",
        serverConnected = "Pripojené k serveru",
        serverDisconnected = "Nepripojené",
        searchPlaceholder = "Hľadaj skladby, albumy, interpretov…",
        quickPicks = "Rýchly výber",
        playAll = "Prehrať všetko",
        favoriteTracks = "Obľúbené skladby",
        recentHistory = "Nedávno prehrávané",
        offlineCacheTitle = "Smart Offline Cache",
        autoEqTitle = "AutoEQ Kalibrácia slúchadiel",
        equalizerTitle = "10-pásmový DSP Ekvalizér",
        sleepTimerTitle = "Časovač spánku"
    )

    private val englishBundle = AppStringsBundle(
        navHome = "Home",
        navExplore = "Explore",
        navLibrary = "Library",
        navSettings = "Settings",
        nowPlayingTitle = "Now Playing",
        playingFrom = "Playing from: ",
        tabUpcoming = "UP NEXT",
        tabLyrics = "LYRICS",
        tabSimilar = "SIMILAR",
        emptyLyrics = "No lyrics available for this track.",
        noTrackPlaying = "No track playing",
        settingsTitle = "Settings & Quality",
        sectionLanguage = "App Language",
        sectionTheme = "App Theme",
        sectionStreaming = "Streaming Quality",
        sectionOffline = "Local Cache & Downloads",
        sectionAbout = "About SoundWave",
        simpleModeTitle = "Simple Mode",
        simpleModeSubtitle = "Hide advanced technical options and keep it minimal",
        wifiStreamingTitle = "Wi-Fi Streaming",
        cellularStreamingTitle = "Cellular Data & Transcoding",
        serverLogoutButton = "Sign out of server",
        serverConnected = "Connected to server",
        serverDisconnected = "Disconnected",
        searchPlaceholder = "Search songs, albums, artists…",
        quickPicks = "Quick Picks",
        playAll = "Play all",
        favoriteTracks = "Liked Songs",
        recentHistory = "Recently Played",
        offlineCacheTitle = "Smart Offline Cache",
        autoEqTitle = "AutoEQ Headphone Calibration",
        equalizerTitle = "10-Band DSP Equalizer",
        sleepTimerTitle = "Sleep Timer"
    )

    private val czechBundle = AppStringsBundle(
        navHome = "Domů",
        navExplore = "Objevuj",
        navLibrary = "Knihovna",
        navSettings = "Nastavení",
        nowPlayingTitle = "Právě hraje",
        playingFrom = "Přehrává se z: ",
        tabUpcoming = "NÁSLEDUJÍCÍ",
        tabLyrics = "TEXT",
        tabSimilar = "PODOBNÉ",
        emptyLyrics = "Pro tuto skladbu není k dispozici žádný text.",
        noTrackPlaying = "Žádná vybraná skladba",
        settingsTitle = "Nastavení a kvalita",
        sectionLanguage = "Jazyk aplikace",
        sectionTheme = "Motiv aplikace",
        sectionStreaming = "Kvalita streamování",
        sectionOffline = "Místní mezipaměť a stažené",
        sectionAbout = "O aplikaci SoundWave",
        simpleModeTitle = "Zjednodušený režim",
        simpleModeSubtitle = "Skryje technické detaily a zobrazí jen to podstatné",
        wifiStreamingTitle = "Streamování přes Wi-Fi",
        cellularStreamingTitle = "Mobilní data a překódování",
        serverLogoutButton = "Odhlásit se ze serveru",
        serverConnected = "Připojeno k serveru",
        serverDisconnected = "Nepřipojeno",
        searchPlaceholder = "Hledej skladby, alba, interprety…",
        quickPicks = "Rychlý výběr",
        playAll = "Přehrát vše",
        favoriteTracks = "Oblíbené skladby",
        recentHistory = "Nedávno přehrávané",
        offlineCacheTitle = "Smart Offline Cache",
        autoEqTitle = "AutoEQ Kalibrace sluchátek",
        equalizerTitle = "10pásmový DSP Ekvalizér",
        sleepTimerTitle = "Časovač spánku"
    )

    fun forLanguage(langCode: String): AppStringsBundle {
        val effectiveCode = if (langCode == "SYSTEM") {
            java.util.Locale.getDefault().language.lowercase()
        } else {
            langCode.lowercase()
        }
        return when (effectiveCode) {
            "en" -> englishBundle
            "cs" -> czechBundle
            else -> slovakBundle
        }
    }
}
