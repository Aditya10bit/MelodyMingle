package com.example.myapplication.services

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.myapplication.MainActivity
import com.example.myapplication.MyExoplayer
import com.example.myapplication.R
import com.example.myapplication.models.SongModels

class MusicService : Service() {
    private val CHANNEL_ID = "MusicPlayerChannel"
    private val NOTIFICATION_ID = 1
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager

    // Flag to indicate that startForeground has been called once
    private var isForegroundServiceStarted = false

    companion object {
        const val ACTION_PLAY = "com.example.myapplication.PLAY"
        const val ACTION_PAUSE = "com.example.myapplication.PAUSE"
        const val ACTION_NEXT = "com.example.myapplication.NEXT"
        const val ACTION_PREVIOUS = "com.example.myapplication.PREVIOUS"
        const val ACTION_STOP = "com.example.myapplication.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        setupMediaSession()
        // Immediately start as foreground with an empty notification.
        startForeground(NOTIFICATION_ID, createEmptyNotification())
        isForegroundServiceStarted = true
    }

    private fun createEmptyNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.mpplayer)
            .setContentTitle("Music Player")
            .setContentText("Loading...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                MyExoplayer.stop()
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                handleIntent(intent)
                updateNotification() // update notification with current song details
            }
        }
        // Use START_STICKY so the service remains running during playback.
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music player controls"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    MyExoplayer.playPause()
                    updateNotification()
                }
                override fun onPause() {
                    MyExoplayer.playPause()
                    updateNotification()
                }
                override fun onStop() {
                    MyExoplayer.stop()
                    stopSelf()
                }
                override fun onSkipToNext() {
                    MyExoplayer.playNext()
                    updateNotification()
                }
                override fun onSkipToPrevious() {
                    MyExoplayer.playPrevious()
                    updateNotification()
                }
            })
            isActive = true
        }
    }

    /**
     * Update the notification immediately with a placeholder (or null art)
     * then asynchronously update it once the cover image is loaded.
     */
    private fun updateNotification() {
        val currentSong = MyExoplayer.getCurrentSong() ?: return

        // Immediately show notification without waiting for Glide
        showNotification(currentSong, null)

        // Now load cover art asynchronously and update the notification when ready.
        Glide.with(applicationContext)
            .asBitmap()
            .load(currentSong.coverUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    showNotification(currentSong, resource)
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    showNotification(currentSong, null)
                }
            })
    }

    private fun showNotification(song: SongModels, albumArt: Bitmap?) {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, MusicService::class.java).setAction(
                if (MyExoplayer.isPlaying()) ACTION_PAUSE else ACTION_PLAY
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, MusicService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.mpplayer)
            .setLargeIcon(albumArt)
            .setContentTitle(song.title)
            .setContentText(song.subtitle)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_previous, "Previous", createActionIntent(ACTION_PREVIOUS))
            .addAction(
                if (MyExoplayer.isPlaying()) R.drawable.ic_play_pause else R.drawable.ic_play_play,
                if (MyExoplayer.isPlaying()) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(R.drawable.ic_next, "Next", createActionIntent(ACTION_NEXT))
            .addAction(R.drawable.ic_close, "Stop", stopIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2))
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()

        // Update the foreground notification.
        // Calling startForeground again is acceptable.
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createActionIntent(action: String): PendingIntent {
        val requestCode = when (action) {
            ACTION_PREVIOUS -> 1
            ACTION_NEXT -> 2
            else -> 3
        }
        return PendingIntent.getService(
            this,
            requestCode,
            Intent(this, MusicService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_PLAY -> MyExoplayer.playPause()
            ACTION_PAUSE -> MyExoplayer.playPause()
            ACTION_NEXT -> MyExoplayer.playNext()
            ACTION_PREVIOUS -> MyExoplayer.playPrevious()
        }
        updateNotification()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        MyExoplayer.stop()
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        MyExoplayer.stop()
        stopForeground(true)
    }

    override fun onBind(intent: Intent?) = null
}
