package com.example.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.MainActivity
import com.example.data.SettingsStore
import com.example.widget.SoundWaveWidgetProvider
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Background playback service extending MediaLibraryService to support:
 * 1. Android Auto and Automotive head units (media browsing tree & controls).
 * 2. MediaSession notification and lock screen controls.
 * 3. Home Screen Material You widget synchronization.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer
    private lateinit var settings: SettingsStore

    private val librarySessionCallback = object : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(MEDIA_ID_ROOT)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setTitle("SoundWave")
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val items = when (parentId) {
                MEDIA_ID_ROOT -> {
                    listOf(
                        buildBrowseFolder(MEDIA_ID_HISTORY, "Nedávno prehrávané"),
                        buildBrowseFolder(MEDIA_ID_QUEUE, "Aktuálny zoznam (Fronta)"),
                        buildBrowseFolder(MEDIA_ID_FAVORITES, "Rýchly výber (Navidrome)")
                    )
                }
                MEDIA_ID_HISTORY -> {
                    settings.loadHistory().map { track ->
                        MediaItem.Builder()
                            .setMediaId(track.id)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(track.title)
                                    .setArtist(track.artist)
                                    .setAlbumTitle(track.album)
                                    .setIsPlayable(true)
                                    .setIsBrowsable(false)
                                    .build()
                            )
                            .build()
                    }
                }
                MEDIA_ID_QUEUE -> {
                    settings.loadLastQueue().map { track ->
                        MediaItem.Builder()
                            .setMediaId(track.id)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(track.title)
                                    .setArtist(track.artist)
                                    .setAlbumTitle(track.album)
                                    .setIsPlayable(true)
                                    .setIsBrowsable(false)
                                    .build()
                            )
                            .build()
                    }
                }
                else -> emptyList()
            }
            return Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
            )
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val track = settings.loadLastTrack()
            val item = if (track != null && track.id == mediaId) {
                MediaItem.Builder()
                    .setMediaId(track.id)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setIsPlayable(true)
                            .build()
                    )
                    .build()
            } else {
                MediaItem.Builder()
                    .setMediaId(mediaId)
                    .setMediaMetadata(MediaMetadata.Builder().setIsPlayable(true).build())
                    .build()
            }
            return Futures.immediateFuture(LibraryResult.ofItem(item, null))
        }
    }

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)

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
            // Some TV platforms or audio sinks do not support hardware AudioEffects
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

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, librarySessionCallback)
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

    private fun buildBrowseFolder(id: String, title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaLibrarySession?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        AudioEffectsManager.releaseEffects()
        mediaLibrarySession?.run {
            player.release()
            release()
        }
        mediaLibrarySession = null
        super.onDestroy()
    }

    companion object {
        const val MEDIA_ID_ROOT = "root"
        const val MEDIA_ID_HISTORY = "history"
        const val MEDIA_ID_QUEUE = "queue"
        const val MEDIA_ID_FAVORITES = "favorites"
    }
}
