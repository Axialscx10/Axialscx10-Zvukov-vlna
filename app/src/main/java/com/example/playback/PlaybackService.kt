package com.example.playback

import android.app.PendingIntent
import android.content.Intent
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
 * Supports:
 * 1. Background audio playback and system media notification (lock screen & Bluetooth).
 * 2. Home Screen Material You widget synchronization.
 * 3. Safe hardware DSP equalizer & effects.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        // Attach hardware DSP equalizer & effects to the player session safely
        try {
            AudioEffectsManager.attachAudioSession(player.audioSessionId)
            player.addListener(object : Player.Listener {
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    try {
                        AudioEffectsManager.attachAudioSession(audioSessionId)
                    } catch (_: Throwable) {}
                }
            })
        } catch (_: Throwable) {
            // Safe fallback if hardware DSP is unavailable
        }

        // Keep widget in sync with playback state
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                notifyWidgetUpdate()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != null) {
            when (action) {
                SoundWaveWidgetProvider.ACTION_PLAY_PAUSE -> {
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        if (player.mediaItemCount > 0) {
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
        AudioEffectsManager.releaseEffects()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
