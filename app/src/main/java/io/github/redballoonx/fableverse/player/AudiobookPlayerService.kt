package io.github.redballoonx.fableverse.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Service für Audio-Wiedergabe im Hintergrund
 */
@OptIn(UnstableApi::class)
class AudiobookPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()

        // Notification Channel erstellen
        createNotificationChannel()

        // ExoPlayer initialisieren
        player = ExoPlayer.Builder(this).build()

        // MediaSession erstellen
        mediaSession = MediaSession.Builder(this, player!!)
            .setCallback(MediaSessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audiobook Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for audiobook playback"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        // Hier können wir Custom-Commands hinzufügen
    }

    companion object {
        private const val CHANNEL_ID = "audiobook_playback"
    }
}