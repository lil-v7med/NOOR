package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import com.example.MainActivity
import com.example.NoorSakinahApp
import com.example.R
import com.example.util.ArabicTextUtils

class QuranAudioService : Service() {

    companion object {
        const val CHANNEL_ID = "quran_audio_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_OR_UPDATE = "com.example.audio.START_OR_UPDATE"
        const val ACTION_TOGGLE = "com.example.audio.TOGGLE"
        const val ACTION_NEXT = "com.example.audio.NEXT"
        const val ACTION_PREV = "com.example.audio.PREV"
        const val ACTION_STOP = "com.example.audio.STOP"
    }

    private var mediaSession: MediaSession? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        initMediaSession()
    }

    override fun onDestroy() {
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun initMediaSession() {
        val controller = (application as NoorSakinahApp).audioController
        val session = MediaSession(this, "NoorSakinahQuranSession")
        session.setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                controller.resumeOrPlay()
                refreshForegroundNotification(controller.state.value)
            }

            override fun onPause() {
                controller.pause()
                refreshForegroundNotification(controller.state.value)
            }

            override fun onSkipToNext() {
                controller.nextAyah()
                refreshForegroundNotification(controller.state.value)
            }

            override fun onSkipToPrevious() {
                controller.previousAyah()
                refreshForegroundNotification(controller.state.value)
            }

            override fun onStop() {
                controller.stop()
            }
        })
        session.isActive = true
        mediaSession = session
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val controller = (application as NoorSakinahApp).audioController
        when (intent?.action) {
            ACTION_TOGGLE -> {
                controller.togglePlayPause()
                refreshForegroundNotification(controller.state.value)
            }
            ACTION_NEXT -> {
                controller.nextAyah()
                refreshForegroundNotification(controller.state.value)
            }
            ACTION_PREV -> {
                controller.previousAyah()
                refreshForegroundNotification(controller.state.value)
            }
            ACTION_STOP -> {
                mediaSession?.isActive = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                refreshForegroundNotification(controller.state.value)
            }
        }
        return START_NOT_STICKY
    }

    private fun refreshForegroundNotification(state: AudioPlaybackState) {
        updateMediaSessionMetadata(state)
        startForeground(NOTIFICATION_ID, buildNotification(state))
    }

    private fun updateMediaSessionMetadata(state: AudioPlaybackState) {
        val session = mediaSession ?: return
        val controller = (application as NoorSakinahApp).audioController
        val ayahArabic = ArabicTextUtils.toEasternArabicNumerals(state.currentAyah)
        val totalArabic = ArabicTextUtils.toEasternArabicNumerals(state.totalAyahsInSurah)
        val albumArt = runCatching {
            BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        }.getOrNull()

        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, "سورة ${state.surahName} — آية $ayahArabic من $totalArabic")
            .putString(MediaMetadata.METADATA_KEY_ARTIST, state.reciter.nameArabic)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, "القرآن الكريم • محراب النور")
            .apply {
                if (state.durationMs > 0L) {
                    putLong(MediaMetadata.METADATA_KEY_DURATION, state.durationMs)
                }
                if (albumArt != null) {
                    putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArt)
                }
            }
            .build()
        session.setMetadata(metadata)

        val pbState = when {
            state.isLoading -> PlaybackState.STATE_BUFFERING
            state.isPlaying -> PlaybackState.STATE_PLAYING
            else -> PlaybackState.STATE_PAUSED
        }
        val playbackState = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_STOP
            )
            .setState(pbState, controller.currentPositionMs(), 1.0f)
            .build()
        session.setPlaybackState(playbackState)
        session.isActive = state.isActive
    }

    private fun buildNotification(state: AudioPlaybackState): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            this,
            10,
            Intent(this, QuranAudioService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleIntent = PendingIntent.getService(
            this,
            11,
            Intent(this, QuranAudioService::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = PendingIntent.getService(
            this,
            12,
            Intent(this, QuranAudioService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            13,
            Intent(this, QuranAudioService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val ayahArabic = ArabicTextUtils.toEasternArabicNumerals(state.currentAyah)
        val totalArabic = ArabicTextUtils.toEasternArabicNumerals(state.totalAyahsInSurah)
        val title = "سورة ${state.surahName} — آية $ayahArabic / $totalArabic"
        val offlineBadge = if (state.isPlayingOffline) " • أوفلاين" else ""
        val subtitle = "${state.reciter.nameArabic}$offlineBadge"
        val toggleLabel = if (state.isPlaying) "إيقاف" else "تشغيل"
        val toggleIcon = if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val mediaStyle = Notification.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
        mediaSession?.sessionToken?.let { mediaStyle.setMediaSession(it) }

        return builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setContentIntent(openIntent)
            .setOngoing(state.isPlaying)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(mediaStyle)
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_previous,
                    "السابقة",
                    prevIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    toggleIcon,
                    toggleLabel,
                    toggleIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_next,
                    "التالية",
                    nextIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "إغلاق",
                    stopIntent
                ).build()
            )
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تلاوة القرآن الكريم",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "التحكم في تشغيل تلاوة القرآن الكريم من الإشعارات وشاشة القفل"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
