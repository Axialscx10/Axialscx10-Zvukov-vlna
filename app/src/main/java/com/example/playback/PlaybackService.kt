package com.example.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes as PlatformAudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.widget.SoundWaveWidgetProvider

/**
 * Background playback service owning ExoPlayer and MediaSession.
 *
 * Engineered to eliminate the Car Bluetooth Pause -> Play volume drop bug (v31.0):
 * 1. Dynamically detects Bluetooth audio routing (`TYPE_BLUETOOTH_A2DP`, `TYPE_BLE_HEADSET`, etc.)
 *    and detaches all hardware `AudioEffect`s (`libbundlewrapper.so` / `Equalizer`) so Android's
 *    `AudioFlinger` uses the direct `FastMixer` A2DP path with zero DRC/limiter attenuation.
 * 2. Uses a single persistent `AudioFocusRequest(AUDIOFOCUS_GAIN)` with `.setWillPauseWhenDucked(true)`
 *    and NEVER abandons audio focus on Pause or mid-resume (`onIsPlayingChanged(true)`), preventing
 *    Android's `MediaFocusControl` and car AVRCP head units from ducking the stream during transitions.
 * 3. Performs an automatic 35ms `AudioTrack` wake-up re-trigger (`pause` -> `play` + native volume flush)
 *    150ms after resuming from a pause over Bluetooth so the car A2DP sink wakes up and locks onto
 *    100% normal volume on the very first press of Play.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var hasAudioFocus: Boolean = false
    private var resumeOnFocusGain: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // Tracks user-initiated pauses so we can perform the post-pause Bluetooth A2DP wake-up handshake
    private var lastPausedAtElapsedMs: Long = 0L
    private var wasPausedSinceLastPlay: Boolean = false
    private var isInternalBtRetrigger: Boolean = false

    private val btRetriggerPauseRunnable = Runnable {
        if (!::player.isInitialized || !player.playWhenReady) return@Runnable
        try {
            isInternalBtRetrigger = true
            player.pause()
            mainHandler.postDelayed(btRetriggerPlayRunnable, 35L)
        } catch (_: Throwable) {
            isInternalBtRetrigger = false
        }
    }

    private val btRetriggerPlayRunnable = Runnable {
        if (!::player.isInitialized) {
            isInternalBtRetrigger = false
            return@Runnable
        }
        try {
            ensureAudioFocus()
            player.play()
            forceApplyNativeVolume()
        } catch (_: Throwable) {
        } finally {
            isInternalBtRetrigger = false
        }
    }

    private val volumeRestoreRunnable120 = Runnable {
        if (::player.isInitialized && (player.playWhenReady || isInternalBtRetrigger)) {
            forceApplyNativeVolume()
            AudioEffectsManager.onPlaybackResumed()
        }
    }
    private val volumeRestoreRunnable350 = Runnable {
        if (::player.isInitialized && player.playWhenReady) {
            forceApplyNativeVolume()
        }
    }
    private val volumeRestoreRunnable700 = Runnable {
        if (::player.isInitialized && player.playWhenReady) {
            forceApplyNativeVolume()
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            syncBluetoothOutputState()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            syncBluetoothOutputState()
        }
    }

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                resumeOnFocusGain = false
                if (::player.isInitialized && player.isPlaying) {
                    player.pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                if (::player.isInitialized) {
                    resumeOnFocusGain = player.isPlaying || player.playWhenReady
                    if (player.isPlaying) {
                        player.pause()
                    }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (::player.isInitialized) {
                    if (AudioEffectsManager.shouldBypassHardwareDsp() || isBluetoothOutputActive()) {
                        forceApplyNativeVolume()
                    } else {
                        val ducked = (AudioEffectsManager.computePlayerVolume() * 0.35f).coerceIn(0.1f, 1.0f)
                        player.volume = ducked
                    }
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                if (::player.isInitialized) {
                    forceApplyNativeVolume()
                    if (resumeOnFocusGain) {
                        resumeOnFocusGain = false
                        player.play()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        syncBluetoothOutputState()
        try {
            audioManager?.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)
        } catch (_: Throwable) {}

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ false
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        forceApplyNativeVolume()
        AudioEffectsManager.setPlayerVolumeListener { _ ->
            if (::player.isInitialized) {
                forceApplyNativeVolume()
            }
        }

        try {
            AudioEffectsManager.attachAudioSession(player.audioSessionId)
            player.addListener(object : Player.Listener {
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    try {
                        syncBluetoothOutputState()
                        AudioEffectsManager.attachAudioSession(audioSessionId)
                        forceApplyNativeVolume()
                    } catch (_: Throwable) {}
                }
            })
        } catch (_: Throwable) {}

        player.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (isInternalBtRetrigger) return

                if (playWhenReady) {
                    syncBluetoothOutputState()
                    ensureAudioFocus()
                    val now = SystemClock.elapsedRealtime()
                    val pauseDurationMs = if (lastPausedAtElapsedMs > 0L) now - lastPausedAtElapsedMs else 0L
                    val shouldRetriggerBt = wasPausedSinceLastPlay &&
                        pauseDurationMs >= 500L &&
                        (isBluetoothOutputActive() || AudioEffectsManager.isBluetoothVolumeStabilizerEnabled())
                    wasPausedSinceLastPlay = false
                    schedulePostResumeVolumeLock(triggerBtHandshake = shouldRetriggerBt)
                } else {
                    cancelPostResumeVolumeLock()
                    wasPausedSinceLastPlay = true
                    lastPausedAtElapsedMs = SystemClock.elapsedRealtime()
                    // Note: Do NOT abandon AudioFocus on user Pause! Keeping AUDIOFOCUS_GAIN while
                    // paused prevents the car head unit and Android AudioFlinger from demoting or
                    // ducking our session during Pause -> Play.
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isInternalBtRetrigger) return
                if (isPlaying) {
                    ensureAudioFocus()
                    forceApplyNativeVolume()
                }
                notifyWidgetUpdate()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                forceApplyNativeVolume()
                notifyWidgetUpdate()
            }
        })

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent)
            .build()
    }

    private fun isBluetoothOutputActive(): Boolean {
        val am = audioManager ?: return false
        return try {
            val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            devices.any { dev ->
                dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                        dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            dev.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                        )) ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        dev.type == AudioDeviceInfo.TYPE_BLE_BROADCAST)
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun syncBluetoothOutputState() {
        val btConnected = isBluetoothOutputActive()
        AudioEffectsManager.setBluetoothOutputConnected(btConnected)
    }

    /**
     * Forces ExoPlayer to send `MSG_SET_VOLUME` to `DefaultAudioSink` / native `AudioTrack`
     * even when `player.volume` already equals `targetVol`, and ensures the system music stream
     * is unmuted when resuming over Car Bluetooth AVRCP.
     */
    private fun forceApplyNativeVolume() {
        if (!::player.isInitialized) return
        val targetVol = AudioEffectsManager.computePlayerVolume().coerceIn(0.1f, 1.0f)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && player.playWhenReady) {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_UNMUTE,
                    0
                )
            }
        } catch (_: Throwable) {}

        val nudge = if (targetVol >= 0.999f) 0.998f else (targetVol + 0.001f)
        player.volume = nudge
        player.volume = targetVol
    }

    private fun schedulePostResumeVolumeLock(triggerBtHandshake: Boolean) {
        forceApplyNativeVolume()
        cancelPostResumeVolumeLock()
        mainHandler.postDelayed(volumeRestoreRunnable120, 120L)
        if (triggerBtHandshake) {
            mainHandler.postDelayed(btRetriggerPauseRunnable, 150L)
        }
        mainHandler.postDelayed(volumeRestoreRunnable350, 350L)
        mainHandler.postDelayed(volumeRestoreRunnable700, 700L)
    }

    private fun cancelPostResumeVolumeLock() {
        mainHandler.removeCallbacks(btRetriggerPauseRunnable)
        mainHandler.removeCallbacks(btRetriggerPlayRunnable)
        mainHandler.removeCallbacks(volumeRestoreRunnable120)
        mainHandler.removeCallbacks(volumeRestoreRunnable350)
        mainHandler.removeCallbacks(volumeRestoreRunnable700)
        isInternalBtRetrigger = false
    }

    /**
     * Requests `AUDIOFOCUS_GAIN` using a single persistent `AudioFocusRequest` with
     * `.setWillPauseWhenDucked(true)` without ever abandoning focus mid-playback.
     */
    private fun ensureAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        PlatformAudioAttributes.Builder()
                            .setUsage(PlatformAudioAttributes.USAGE_MEDIA)
                            .setContentType(PlatformAudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(audioFocusListener, mainHandler)
                    .build()
                    .also { focusRequest = it }

                val res = am.requestAudioFocus(req)
                hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            } else {
                @Suppress("DEPRECATION")
                val res = am.requestAudioFocus(
                    audioFocusListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
                hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            }
        } catch (_: Throwable) {}
    }

    private fun abandonSmartAudioFocus() {
        val am = audioManager ?: return
        hasAudioFocus = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { am.abandonAudioFocusRequest(it) }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusListener)
            }
        } catch (_: Throwable) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != null) {
            when (action) {
                SoundWaveWidgetProvider.ACTION_PLAY_PAUSE -> {
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        if (player.mediaItemCount > 0) {
                            ensureAudioFocus()
                            player.play()
                        }
                    }
                    notifyWidgetUpdate()
                }
                SoundWaveWidgetProvider.ACTION_PREV -> {
                    if (player.hasPreviousMediaItem()) {
                        player.seekToPreviousMediaItem()
                    } else {
                        player.seekTo(0L)
                    }
                    notifyWidgetUpdate()
                }
                SoundWaveWidgetProvider.ACTION_NEXT -> {
                    if (player.hasNextMediaItem()) {
                        player.seekToNextMediaItem()
                    }
                    notifyWidgetUpdate()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun notifyWidgetUpdate() {
        val currentMedia = player.currentMediaItem
        val title = currentMedia?.mediaMetadata?.title?.toString() ?: "SoundWave"
        val artist = currentMedia?.mediaMetadata?.artist?.toString() ?: "Navidrome Hi-Fi"
        SoundWaveWidgetProvider.updateWidgetState(this, title, artist, player.isPlaying)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        cancelPostResumeVolumeLock()
        try {
            audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
        } catch (_: Throwable) {}
        AudioEffectsManager.setPlayerVolumeListener(null)
        abandonSmartAudioFocus()
        AudioEffectsManager.releaseEffects()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
