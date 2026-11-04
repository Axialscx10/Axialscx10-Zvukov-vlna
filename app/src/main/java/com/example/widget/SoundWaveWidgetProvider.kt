package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class SoundWaveWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        when (action) {
            ACTION_PLAY_PAUSE, ACTION_PREV, ACTION_NEXT -> {
                // Forward control action to PlaybackService
                val serviceIntent = Intent(context, com.example.playback.PlaybackService::class.java).apply {
                    this.action = action
                }
                try {
                    context.startService(serviceIntent)
                } catch (_: Throwable) {}
            }
            ACTION_UPDATE_STATE -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "SoundWave"
                val artist = intent.getStringExtra(EXTRA_ARTIST) ?: "Pripravené na prehrávanie"
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                currentTitle = title
                currentArtist = artist
                SoundWaveWidgetProvider.isPlaying = isPlaying

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, SoundWaveWidgetProvider::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                for (widgetId in allWidgetIds) {
                    updateAppWidget(context, appWidgetManager, widgetId)
                }
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.soundwave.widget.ACTION_PLAY_PAUSE"
        const val ACTION_PREV = "com.example.soundwave.widget.ACTION_PREV"
        const val ACTION_NEXT = "com.example.soundwave.widget.ACTION_NEXT"
        const val ACTION_UPDATE_STATE = "com.example.soundwave.widget.ACTION_UPDATE_STATE"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        private var currentTitle: String = "SoundWave"
        private var currentArtist: String = "Navidrome Hi-Fi"
        private var isPlaying: Boolean = false

        fun updateWidgetState(context: Context, title: String, artist: String, playing: Boolean) {
            currentTitle = title.ifBlank { "SoundWave" }
            currentArtist = artist.ifBlank { "Navidrome Hi-Fi" }
            isPlaying = playing

            val intent = Intent(context, SoundWaveWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_STATE
                putExtra(EXTRA_TITLE, currentTitle)
                putExtra(EXTRA_ARTIST, currentArtist)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            context.sendBroadcast(intent)
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.soundwave_widget_layout)

            views.setTextViewText(R.id.widget_title, currentTitle)
            views.setTextViewText(R.id.widget_artist, currentArtist)

            val playIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            views.setImageViewResource(R.id.widget_btn_play, playIcon)

            // Open app on click
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // Action intents
            views.setOnClickPendingIntent(R.id.widget_btn_play, getActionPendingIntent(context, ACTION_PLAY_PAUSE, 1))
            views.setOnClickPendingIntent(R.id.widget_btn_prev, getActionPendingIntent(context, ACTION_PREV, 2))
            views.setOnClickPendingIntent(R.id.widget_btn_next, getActionPendingIntent(context, ACTION_NEXT, 3))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getActionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, SoundWaveWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
