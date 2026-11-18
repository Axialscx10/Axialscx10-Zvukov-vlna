package com.example.data

import android.content.Context

import org.json.JSONArray
import org.json.JSONObject

/**
 * Saved server login. The password itself is never stored: Subsonic authentication only needs
 * the token = md5(password + salt) together with the salt, which is what we keep.
 */
data class SavedCredentials(
    val url: String,
    val username: String,
    val token: String,
    val salt: String
)

/** Small SharedPreferences wrapper (excluded from cloud backup, see res/xml/backup_rules.xml). */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("soundwave_prefs", Context.MODE_PRIVATE)

    fun loadCredentials(): SavedCredentials? {
        val url = prefs.getString(KEY_URL, null)
        val user = prefs.getString(KEY_USER, null)
        val token = prefs.getString(KEY_TOKEN, null)
        val salt = prefs.getString(KEY_SALT, null)
        if (url.isNullOrBlank() || user.isNullOrBlank() || token.isNullOrBlank() || salt.isNullOrBlank()) {
            return null
        }
        return SavedCredentials(url, user, token, salt)
    }

    fun saveCredentials(credentials: SavedCredentials) {
        prefs.edit()
            .putString(KEY_URL, credentials.url)
            .putString(KEY_USER, credentials.username)
            .putString(KEY_TOKEN, credentials.token)
            .putString(KEY_SALT, credentials.salt)
            .apply()
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_SALT)
            .remove(KEY_LAST_TRACK)
            .remove(KEY_LAST_POSITION_MS)
            .remove(KEY_LAST_QUEUE)
            .remove(KEY_LAST_QUEUE_INDEX)
            .remove(KEY_HISTORY)
            .apply()
    }

    /** Last typed server URL / user name, so the login form is pre-filled. */
    fun lastServerUrl(): String = prefs.getString(KEY_URL, "") ?: ""

    fun lastUsername(): String = prefs.getString(KEY_USER, "") ?: ""

    fun getString(key: String, default: String): String = prefs.getString(key, default) ?: default

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun isOnboardingCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, "DARK") ?: "DARK"

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun getAppLanguage(): String = prefs.getString(KEY_APP_LANGUAGE, "SYSTEM") ?: "SYSTEM"

    fun setAppLanguage(lang: String) {
        prefs.edit().putString(KEY_APP_LANGUAGE, lang).apply()
    }

    fun isSimpleMode(): Boolean = prefs.getBoolean(KEY_SIMPLE_MODE, false)

    fun setSimpleMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SIMPLE_MODE, enabled).apply()
    }

    // ---------------------------------------------------------------------------------------------
    // Playback state & History persistence (survives phone reboot & app restarts)
    // ---------------------------------------------------------------------------------------------

    private fun trackToJson(t: TrackItem): JSONObject = JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("artist", t.artist)
        put("album", t.album)
        put("duration", t.duration)
        put("badge", t.badge)
        // Never persist Subsonic query tokens (t/s) to SharedPreferences; rebuild from coverArtId at runtime
        put("imageUrl", SubsonicApi.sanitizeStoredUrl(t.imageUrl))
        put("albumId", t.albumId)
        put("artistId", t.artistId)
        put("durationSec", t.durationSec)
        put("suffix", t.suffix)
        put("bitRate", t.bitRate)
        put("bitDepth", t.bitDepth)
        put("samplingRate", t.samplingRate)
        put("year", t.year)
        put("coverArtId", t.coverArtId)
        put("techLine", t.techLine)
        put("isHiResBadgeCyan", t.isHiResBadgeCyan)
        if (t.replayGainTrackDb != null) put("rgTrack", t.replayGainTrackDb.toDouble())
        if (t.replayGainAlbumDb != null) put("rgAlbum", t.replayGainAlbumDb.toDouble())
    }

    private fun trackFromJson(obj: JSONObject): TrackItem = TrackItem(
        id = obj.optString("id", ""),
        title = obj.optString("title", ""),
        artist = obj.optString("artist", ""),
        album = obj.optString("album", ""),
        duration = obj.optString("duration", "0:00"),
        badge = obj.optString("badge", ""),
        imageUrl = SubsonicApi.sanitizeStoredUrl(obj.optString("imageUrl", "")),
        albumId = obj.optString("albumId", ""),
        artistId = obj.optString("artistId", ""),
        durationSec = obj.optInt("durationSec", 0),
        suffix = obj.optString("suffix", ""),
        bitRate = obj.optInt("bitRate", 0),
        bitDepth = obj.optInt("bitDepth", 0),
        samplingRate = obj.optInt("samplingRate", 0),
        year = obj.optInt("year", 0),
        coverArtId = obj.optString("coverArtId", ""),
        techLine = obj.optString("techLine", ""),
        isHiResBadgeCyan = obj.optBoolean("isHiResBadgeCyan", true),
        replayGainTrackDb = if (obj.has("rgTrack")) obj.optDouble("rgTrack").takeIf { !it.isNaN() }?.toFloat() else null,
        replayGainAlbumDb = if (obj.has("rgAlbum")) obj.optDouble("rgAlbum").takeIf { !it.isNaN() }?.toFloat() else null
    )

    fun savePlaybackState(track: TrackItem, positionMs: Long, queue: List<TrackItem>, queueIndex: Int) {
        if (track.id.isBlank()) return
        val queueArray = JSONArray()
        val windowStart = (queueIndex - 10).coerceAtLeast(0)
        val savedSlice = if (queue.size > 60) queue.drop(windowStart).take(60) else queue
        val savedIndex = if (queue.size > 60) (queueIndex - windowStart).coerceIn(0, (savedSlice.size - 1).coerceAtLeast(0)) else queueIndex
        savedSlice.forEach { queueArray.put(trackToJson(it)) }
        prefs.edit()
            .putString(KEY_LAST_TRACK, trackToJson(track).toString())
            .putLong(KEY_LAST_POSITION_MS, positionMs)
            .putString(KEY_LAST_QUEUE, queueArray.toString())
            .putInt(KEY_LAST_QUEUE_INDEX, savedIndex)
            .apply()
    }

    fun savePositionOnly(positionMs: Long) {
        prefs.edit().putLong(KEY_LAST_POSITION_MS, positionMs).apply()
    }

    fun loadLastTrack(): TrackItem? {
        val str = prefs.getString(KEY_LAST_TRACK, null) ?: return null
        return try {
            val obj = JSONObject(str)
            trackFromJson(obj).takeIf { it.id.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    fun loadLastPositionMs(): Long = prefs.getLong(KEY_LAST_POSITION_MS, 0L)

    fun loadLastQueue(): List<TrackItem> {
        val str = prefs.getString(KEY_LAST_QUEUE, null) ?: return emptyList()
        return try {
            val arr = JSONArray(str)
            val list = mutableListOf<TrackItem>()
            for (i in 0 until arr.length()) {
                list.add(trackFromJson(arr.getJSONObject(i)))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadLastQueueIndex(): Int = prefs.getInt(KEY_LAST_QUEUE_INDEX, 0)

    fun addTrackToHistory(track: TrackItem) {
        if (track.id.isBlank()) return
        val current = loadHistory().toMutableList()
        if (current.firstOrNull()?.id == track.id) return
        current.removeAll { it.id == track.id }
        current.add(0, track)
        val trimmed = current.take(60)
        val arr = JSONArray()
        trimmed.forEach { arr.put(trackToJson(it)) }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun loadHistory(): List<TrackItem> {
        val str = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(str)
            val list = mutableListOf<TrackItem>()
            for (i in 0 until arr.length()) {
                list.add(trackFromJson(arr.getJSONObject(i)))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun loadEqBands(defaultBands: List<Float>): List<Float> {
        val raw = prefs.getString(KEY_EQ_BANDS, null) ?: return defaultBands
        return try {
            val parts = raw.split(",")
            if (parts.size == 10) {
                parts.map { it.toFloatOrNull()?.coerceIn(-12f, 12f) ?: 0f }
            } else {
                defaultBands
            }
        } catch (_: Exception) {
            defaultBands
        }
    }

    fun saveEqBands(bands: List<Float>) {
        prefs.edit().putString(KEY_EQ_BANDS, bands.joinToString(",")).apply()
    }

    fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)

    fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    companion object {
        private const val KEY_URL = "server_url"
        private const val KEY_USER = "username"
        private const val KEY_TOKEN = "token"
        private const val KEY_SALT = "salt"

        const val KEY_CELLULAR_TRANSCODING = "cellular_transcoding"
        const val KEY_OPUS_BITRATE = "opus_bitrate"
        const val KEY_BACKEND_CODEC = "backend_codec"
        const val KEY_LOGIN_TRANSCODE = "login_transcode_option"

        const val KEY_EQ_MASTER = "eq_master_enabled"
        const val KEY_EQ_PRESET = "eq_preset_name"
        const val KEY_EQ_BANDS = "eq_bands_csv"
        const val KEY_EQ_PREAMP = "eq_preamp_db"
        const val KEY_SUB_BASS = "eq_sub_bass_db"
        const val KEY_SPATIAL = "eq_spatial_enabled"
        const val KEY_AUTO_GAIN = "eq_auto_gain_enabled"
        const val KEY_BT_STABILIZER = "bt_car_volume_stabilizer"
        const val KEY_REPLAY_GAIN_ENABLED = "replay_gain_enabled"
        const val KEY_REPLAY_GAIN_MODE = "replay_gain_mode"
        const val KEY_REPLAY_GAIN_PREAMP = "replay_gain_preamp_db"

        private const val KEY_LAST_TRACK = "last_track_state"
        private const val KEY_LAST_POSITION_MS = "last_position_ms"
        private const val KEY_LAST_QUEUE = "last_queue_state"
        private const val KEY_LAST_QUEUE_INDEX = "last_queue_index"
        private const val KEY_HISTORY = "playback_history"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_THEME_MODE = "soundwave_theme_mode"
        private const val KEY_APP_LANGUAGE = "soundwave_app_language"
        private const val KEY_SIMPLE_MODE = "soundwave_simple_mode"
    }
}
