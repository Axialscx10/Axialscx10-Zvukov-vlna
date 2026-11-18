package com.example.playback

import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes as PlatformAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.data.SleepTimerOption
import com.example.isTelevision
import com.example.widget.SoundWaveWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SleepTimerState(
    val option: SleepTimerOption = SleepTimerOption.OFF,
    val remainingSec: Int = 0,
    val expiredMessage: String? = null
)

/**
 * Shared sleep timer controller driven by [PlaybackService] so timers survive Activity
 * destruction and reliably pause playback even when the UI is swiped away.
 */
object SleepTimerCoordinator {
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var remainingSec = 0
    private var onExpirePause: (() -> Unit)? = null

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (_state.value.option == SleepTimerOption.OFF ||
                _state.value.option == SleepTimerOption.END_OF_TRACK
            ) {
                return
            }
            if (remainingSec > 1) {
                remainingSec--
                _state.value = _state.value.copy(remainingSec = remainingSec)
                handler.postDelayed(this, 1000L)
            } else {
                remainingSec = 0
                onExpirePause?.invoke()
                _state.value = SleepTimerState(
                    option = SleepTimerOption.OFF,
                    remainingSec = 0,
                    expiredMessage = "Časovač spánku: Čas vypršal, prehrávanie bolo pozastavené."
                )
            }
        }
    }

    internal fun attachPauseCallback(pauseFn: (() -> Unit)?) {
        onExpirePause = pauseFn
    }

    fun setTimer(option: SleepTimerOption) {
        handler.removeCallbacks(tickRunnable)
        when (option) {
            SleepTimerOption.OFF -> {
                remainingSec = 0
                _state.value = SleepTimerState(SleepTimerOption.OFF, 0, null)
            }
            SleepTimerOption.END_OF_TRACK -> {
                remainingSec = -1
                _state.value = SleepTimerState(SleepTimerOption.END_OF_TRACK, -1, null)
            }
            else -> {
                remainingSec = option.minutes * 60
                _state.value = SleepTimerState(option, remainingSec, null)
                handler.postDelayed(tickRunnable, 1000L)
            }
        }
    }

    internal fun handleAutoTrackTransition(): Boolean {
        if (_state.value.option == SleepTimerOption.END_OF_TRACK) {
            onExpirePause?.invoke()
            _state.value = SleepTimerState(
                option = SleepTimerOption.OFF,
                remainingSec = 0,
                expiredMessage = "Časovač spánku: Prehrávanie bolo pozastavené po skončení skladby."
            )
            return true
        }
        return false
    }

    fun consumeExpiredMessage() {
        if (_state.value.expiredMessage != null) {
            _state.value = _state.value.copy(expiredMessage = null)
        }
    }

    internal fun clearCallbacksOnDestroy() {
        onExpirePause = null
        handler.removeCallbacks(tickRunnable)
    }
}

/**
 * Background playback service owning ExoPlayer and MediaSession.
 *
 * Key architecture & stability fixes (v32.0):
 * 1. Uses a shared 256 MB LRU [SimpleCache] + [OkHttpDataSource] with generous timeouts and 8 retries
 *    so mobile 5G handovers or brief tunnels in the car do not drop playback.
 * 2. Registers [ConnectivityManager.NetworkCallback] to auto-resume from the exact position when
 *    connectivity recovers after a temporary [PlaybackException] network outage.
 * 3. Keeps hardware EQ / BassBoost active over Bluetooth & Car stereos by default; only bypasses DSP
 *    when the user manually turns on the Emergency Car Stabilizer toggle.
 * 4. Removes the 35ms pause->play Bluetooth retrigger glitch and `ADJUST_UNMUTE` system override.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private var audioManager: AudioManager? = null
    private var connectivityManager: ConnectivityManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var hasAudioFocus: Boolean = false
    private var resumeOnFocusGain: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private var pendingNetworkRecovery: Boolean = false
    private var networkRecoveryAttempts: Int = 0

    private val networkRetryRunnable = Runnable {
        attemptNetworkRecovery("delayed_retry")
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (pendingNetworkRecovery) {
                mainHandler.post {
                    attemptNetworkRecovery("network_available")
                }
            }
        }
    }

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                resumeOnFocusGain = false
                cancelNetworkRecovery()
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
                    if (AudioEffectsManager.isBluetoothVolumeStabilizerEnabled()) {
                        applyPlayerVolume()
                    } else {
                        val ducked = (AudioEffectsManager.computePlayerVolume() * 0.35f).coerceIn(0.1f, 1.0f)
                        player.volume = ducked
                    }
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                if (::player.isInitialized) {
                    applyPlayerVolume()
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
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        registerNetworkMonitor()

        val isTv = isTelevision(this)
        val isLowRam = (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true
        val maxBufMs = if (isTv || isLowRam) 45_000 else 120_000
        val minBufMs = if (isTv || isLowRam) 15_000 else 30_000

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ minBufMs,
                /* maxBufferMs = */ maxBufMs,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 4_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val retryPolicy = object : DefaultLoadErrorHandlingPolicy(8) {
            override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                val backoff = ((loadErrorInfo.errorCount - 1).coerceAtLeast(0) * 1000L).coerceIn(800L, 5_000L)
                return if (loadErrorInfo.errorCount <= 8) backoff else C.TIME_UNSET
            }
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(MediaCacheManager.buildDataSourceFactory(this))
            .setLoadErrorHandlingPolicy(retryPolicy)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
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

        applyPlayerVolume()
        AudioEffectsManager.setPlayerVolumeListener { _ ->
            mainHandler.post {
                if (::player.isInitialized) {
                    applyPlayerVolume()
                }
            }
        }

        SleepTimerCoordinator.attachPauseCallback {
            if (::player.isInitialized && (player.isPlaying || player.playWhenReady)) {
                player.pause()
            }
        }

        try {
            AudioEffectsManager.attachAudioSession(player.audioSessionId)
        } catch (_: Throwable) {}

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                try {
                    AudioEffectsManager.attachAudioSession(audioSessionId)
                    applyPlayerVolume()
                } catch (_: Throwable) {}
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady) {
                    ensureAudioFocus()
                    applyPlayerVolume()
                    AudioEffectsManager.onPlaybackResumed()
                } else {
                    cancelNetworkRecovery()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    cancelNetworkRecovery()
                    applyPlayerVolume()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    ensureAudioFocus()
                    applyPlayerVolume()
                }
                notifyWidgetUpdate()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    SleepTimerCoordinator.handleAutoTrackTransition()
                }
                syncReplayGainFromMediaItem(mediaItem)
                applyPlayerVolume()
                notifyWidgetUpdate()
            }

            override fun onPlayerError(error: PlaybackException) {
                if (isRecoverableNetworkError(error) && player.playWhenReady && networkRecoveryAttempts < MAX_AUTO_RECOVERY_ATTEMPTS) {
                    pendingNetworkRecovery = true
                    networkRecoveryAttempts++
                    val delayMs = (2500L * networkRecoveryAttempts).coerceAtMost(12_000L)
                    mainHandler.removeCallbacks(networkRetryRunnable)
                    mainHandler.postDelayed(networkRetryRunnable, delayMs)
                } else {
                    cancelNetworkRecovery()
                }
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

    private fun isRecoverableNetworkError(error: PlaybackException): Boolean {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_TIMEOUT -> true
            else -> error.errorCodeName.contains("NETWORK", ignoreCase = true)
        }
    }

    private fun attemptNetworkRecovery(@Suppress("UNUSED_PARAMETER") source: String) {
        if (!::player.isInitialized || !pendingNetworkRecovery) return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            cancelNetworkRecovery()
            return
        }
        val idx = player.currentMediaItemIndex.coerceIn(0, player.mediaItemCount - 1)
        val pos = player.currentPosition.coerceAtLeast(0L)
        ensureAudioFocus()
        player.seekTo(idx, pos)
        player.prepare()
        player.play()
    }

    private fun cancelNetworkRecovery() {
        pendingNetworkRecovery = false
        networkRecoveryAttempts = 0
        mainHandler.removeCallbacks(networkRetryRunnable)
    }

    private fun registerNetworkMonitor() {
        val cm = connectivityManager ?: return
        try {
            val req = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(req, networkCallback)
        } catch (_: Throwable) {}
    }

    private fun syncReplayGainFromMediaItem(mediaItem: MediaItem?) {
        val extras = mediaItem?.mediaMetadata?.extras
        val trackDb = if (extras != null && extras.containsKey("rgTrack")) extras.getFloat("rgTrack") else null
        val albumDb = if (extras != null && extras.containsKey("rgAlbum")) extras.getFloat("rgAlbum") else null
        AudioEffectsManager.setActiveTrackReplayGain(trackDb, albumDb)
    }

    private fun applyPlayerVolume() {
        if (!::player.isInitialized) return
        val targetVol = AudioEffectsManager.computePlayerVolume().coerceIn(0.1f, 1.0f)
        if (kotlin.math.abs(player.volume - targetVol) > 0.001f) {
            player.volume = targetVol
        }
    }

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
                    // False allows AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK callback to reach our listener
                    .setWillPauseWhenDucked(false)
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
        if (action != null && ::player.isInitialized) {
            when (action) {
                SoundWaveWidgetProvider.ACTION_PLAY_PAUSE -> {
                    if (player.isPlaying) {
                        player.pause()
                    } else if (player.mediaItemCount > 0) {
                        ensureAudioFocus()
                        if (player.playbackState == Player.STATE_IDLE) {
                            player.prepare()
                        }
                        player.play()
                    }
                    notifyWidgetUpdate()
                }
                SoundWaveWidgetProvider.ACTION_PREV -> {
                    if (player.mediaItemCount > 0) {
                        if (player.hasPreviousMediaItem()) {
                            player.seekToPreviousMediaItem()
                        } else {
                            player.seekTo(0L)
                        }
                        notifyWidgetUpdate()
                    }
                }
                SoundWaveWidgetProvider.ACTION_NEXT -> {
                    if (player.mediaItemCount > 0) {
                        if (player.hasNextMediaItem()) {
                            player.seekToNextMediaItem()
                        }
                        notifyWidgetUpdate()
                    }
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
        cancelNetworkRecovery()
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (_: Throwable) {}
        SleepTimerCoordinator.clearCallbacksOnDestroy()
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

    companion object {
        private const val MAX_AUTO_RECOVERY_ATTEMPTS = 5
    }
}
