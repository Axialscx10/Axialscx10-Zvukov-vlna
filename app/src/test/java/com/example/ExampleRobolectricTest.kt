package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SoundWave", appName)
  }

  @Test
  fun `playback position and queue are saved and restored properly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = com.example.data.SettingsStore(context)
    val track = com.example.data.TrackItem(
      id = "track_123",
      title = "Midnight City",
      artist = "M83",
      album = "Hurry Up, We're Dreaming",
      duration = "4:03",
      badge = "FLAC 24-bit",
      imageUrl = "https://example.com/cover.jpg",
      durationSec = 243
    )

    store.savePlaybackState(track, 125000L, listOf(track), 0)

    val restoredTrack = store.loadLastTrack()
    assertEquals("track_123", restoredTrack?.id)
    assertEquals("Midnight City", restoredTrack?.title)
    assertEquals(125000L, store.loadLastPositionMs())

    store.savePositionOnly(130000L)
    assertEquals(130000L, store.loadLastPositionMs())
  }

  @Test
  fun `history persistence tracks played songs and allows clearing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = com.example.data.SettingsStore(context)
    store.clearHistory()

    val t1 = com.example.data.TrackItem(id = "1", title = "Song 1", artist = "Artist 1", album = "A1", duration = "3:00", badge = "MP3", imageUrl = "")
    val t2 = com.example.data.TrackItem(id = "2", title = "Song 2", artist = "Artist 2", album = "A2", duration = "3:30", badge = "FLAC", imageUrl = "")

    store.addTrackToHistory(t1)
    store.addTrackToHistory(t2)

    val history = store.loadHistory()
    assertEquals(2, history.size)
    assertEquals("2", history[0].id)
    assertEquals("1", history[1].id)

    store.clearHistory()
    assertEquals(0, store.loadHistory().size)
  }

  @Test
  fun `widget state update works properly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.widget.SoundWaveWidgetProvider.updateWidgetState(context, "Midnight City", "M83", true)
  }

  @Test
  fun `multi-language localization bundles provide valid translations`() {
    val sk = com.example.data.AppStrings.forLanguage("sk")
    val en = com.example.data.AppStrings.forLanguage("en")
    val cs = com.example.data.AppStrings.forLanguage("cs")

    assertEquals("Domov", sk.navHome)
    assertEquals("Home", en.navHome)
    assertEquals("Domů", cs.navHome)

    assertEquals("Nastavení", cs.navSettings)
  }

  @Test
  fun `viewModel initializes without throwing exception`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.SoundWaveViewModel(app)
    org.junit.Assert.assertNotNull(vm.uiState.value)
    assertFalse(vm.uiState.value.bluetoothVolumeStabilizerEnabled)
    assertFalse(vm.uiState.value.cellularTranscodingEnabled)
  }

  @Test
  fun `normalizeUrl defaults public hosts to https and LAN IPs to http`() {
    assertEquals("https://hudba.beamvid.com", com.example.data.SubsonicApi.normalizeUrl("hudba.beamvid.com/"))
    assertEquals("http://192.168.68.66:4533", com.example.data.SubsonicApi.normalizeUrl("192.168.68.66:4533"))
    assertEquals("http://100.67.160.126:4533", com.example.data.SubsonicApi.normalizeUrl("100.67.160.126:4533"))
    assertEquals("http://hudba.beamvid.com", com.example.data.SubsonicApi.normalizeUrl("http://hudba.beamvid.com"))
  }

  @Test
  fun `token urls are stripped on persist and cleared on logout`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = com.example.data.SettingsStore(context)
    val tokenizedCover = "https://hudba.beamvid.com/rest/getCoverArt.view?u=vlado&t=secret123&s=salt99&id=al-1&size=360"
    val track = com.example.data.TrackItem(
      id = "t1",
      title = "Song",
      artist = "Band",
      album = "Album",
      duration = "3:00",
      badge = "FLAC",
      imageUrl = tokenizedCover,
      coverArtId = "al-1",
      durationSec = 180
    )
    store.saveCredentials(com.example.data.SavedCredentials("https://hudba.beamvid.com", "vlado", "secret123", "salt99"))
    store.savePlaybackState(track, 5000L, listOf(track), 0)
    store.addTrackToHistory(track)

    val loaded = store.loadLastTrack()
    assertEquals("", loaded?.imageUrl)
    assertEquals("al-1", loaded?.coverArtId)

    store.clearCredentials()
    assertNull(store.loadCredentials())
    assertNull(store.loadLastTrack())
    assertTrue(store.loadHistory().isEmpty())
    assertTrue(store.loadLastQueue().isEmpty())
  }

  @Test
  fun `audio effects manager keeps eq active, applies preamp once, and interpolates 10 ui bands`() {
    val fx = com.example.playback.AudioEffectsManager
    fx.resetForTest()
    try {
      assertFalse(fx.shouldBypassHardwareDsp())

      // -6 dB preamp -> 10^(-6/20) ~= 0.5012 (single attenuation, NOT -12 dB = 0.25)
      fx.setBands(List(10) { 0f }, -6f)
      val vol = fx.computePlayerVolume()
      assertTrue("Expected ~0.501 but was $vol", abs(vol - 0.5012f) < 0.02f)

      // All 10 UI bands (including 31 Hz & 500 Hz) contribute into a 5-band HW EQ
      val hwFreqs = floatArrayOf(60f, 230f, 910f, 3600f, 14000f)
      val uiBands = listOf(8f, 0f, 0f, 0f, 6f, 0f, 0f, 0f, 0f, 0f)
      val mapped = fx.mapUiBandsToHwBands(uiBands, hwFreqs, subBassDb = 0f, spatial = false)
      assertTrue("Expected 31Hz to boost first HW band, got ${mapped[0]}", mapped[0] > 2.0f)
      assertTrue("Expected 500Hz to lift 910Hz HW band, got ${mapped[2]}", mapped[2] > 1.0f)
    } finally {
      fx.resetForTest()
    }
  }

  @Test
  fun `catalog cache persists home snapshot without tokens and rehydrates with current api`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cache = com.example.data.CatalogCacheStore(context)
    cache.clearAll()
    val api = com.example.data.SubsonicApi("https://hudba.beamvid.com", "vlado", "tok1", "sal1")
    val track = com.example.data.TrackItem(
      id = "tr-1",
      title = "Track",
      artist = "HammerFall",
      album = "Legacy",
      duration = "4:00",
      badge = "FLAC",
      imageUrl = api.coverUrl("al-77"),
      coverArtId = "al-77",
      durationSec = 240
    )
    cache.saveHome(
      api.baseUrl,
      api.username,
      com.example.data.CachedHomeSnapshot(
        quickPicks = listOf(track),
        listenAgain = emptyList(),
        newest = emptyList(),
        genres = listOf(com.example.data.GenreItem("Power Metal", 50)),
        playlists = emptyList()
      )
    )
    val loaded = cache.loadHome(api)
    org.junit.Assert.assertNotNull(loaded)
    assertEquals(1, loaded!!.quickPicks.size)
    assertEquals("al-77", loaded.quickPicks[0].coverArtId)
    assertTrue(loaded.quickPicks[0].imageUrl.contains("t=tok1"))
    cache.clearAll()
  }
}
