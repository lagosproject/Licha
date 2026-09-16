package com.lakescorp.twitchchattts.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.lakescorp.twitchchattts.MainActivity
import com.lakescorp.twitchchattts.R
import com.lakescorp.twitchchattts.data.repository.SettingsRepository
import com.lakescorp.twitchchattts.di.ApplicationScope
import com.lakescorp.twitchchattts.domain.ChatSessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground Service that keeps the application alive and processing chat audio
 * when the app is backgrounded or when the device screen is off.
 *
 * Acquires a partial wake lock and wifi lock to prevent Doze mode / CPU suspension,
 * and displays an ongoing notification with Play/Pause and Stop actions.
 */
@AndroidEntryPoint
class ChatForegroundService : Service() {

    @Inject
    lateinit var chatSessionManager: ChatSessionManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    private var settingsJob: Job? = null
    @Volatile private var stopOnAppClose: Boolean = true
    @Volatile private var isPaused: Boolean = false

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var currentChannel: String = ""

    companion object {
        const val ACTION_START = "com.lakescorp.twitchchattts.action.START_FOREGROUND"
        const val ACTION_STOP = "com.lakescorp.twitchchattts.action.STOP_FOREGROUND"
        const val ACTION_PAUSE = "com.lakescorp.twitchchattts.action.PAUSE_FOREGROUND"
        const val ACTION_PLAY = "com.lakescorp.twitchchattts.action.PLAY_FOREGROUND"
        const val EXTRA_CHANNEL = "extra_channel"

        private const val NOTIFICATION_ID = 9001
        private const val CHANNEL_ID = "licha_chat_tts_foreground_channel"

        fun start(context: Context, channel: String) {
            val intent = Intent(context, ChatForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CHANNEL, channel)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("ChatForegroundService", "Failed to start foreground service", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ChatForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e("ChatForegroundService", "Failed to stop foreground service", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        settingsJob = appScope.launch {
            settingsRepository.stopOnAppClose.collect {
                stopOnAppClose = it
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: currentChannel
                currentChannel = channel.ifEmpty { chatSessionManager.currentChannelName }
                isPaused = false
                updateNotification(currentChannel, isPaused = false)
                acquireLocks()
            }
            ACTION_PAUSE -> {
                isPaused = true
                releaseLocks()
                chatSessionManager.disconnect(stopService = false)
                val channel = currentChannel.ifEmpty { chatSessionManager.currentChannelName }
                updateNotification(channel, isPaused = true)
            }
            ACTION_PLAY -> {
                isPaused = false
                val channel = currentChannel.ifEmpty { chatSessionManager.currentChannelName }
                updateNotification(channel, isPaused = false)
                acquireLocks()
                chatSessionManager.reconnect()
            }
            ACTION_STOP -> {
                stopServiceInternal()
            }
        }
        return START_NOT_STICKY
    }

    private fun stopServiceInternal() {
        releaseLocks()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        chatSessionManager.disconnect(stopService = false)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (stopOnAppClose) {
            Log.d("ChatForegroundService", "onTaskRemoved: stopping background service per user setting")
            stopServiceInternal()
        }
    }

    private fun updateNotification(channel: String, isPaused: Boolean) {
        createNotificationChannel()

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleAction = if (isPaused) ACTION_PLAY else ACTION_PAUSE
        val toggleIntent = Intent(this, ChatForegroundService::class.java).apply {
            action = toggleAction
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ChatForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelDisplay = if (channel.isNotEmpty()) channel else "Twitch"
        val (playPauseIcon, playPauseText) = if (isPaused) {
            android.R.drawable.ic_media_play to getString(R.string.service_action_play)
        } else {
            android.R.drawable.ic_media_pause to getString(R.string.service_action_pause)
        }

        val contentText = if (isPaused) {
            getString(R.string.service_notification_paused, channelDisplay)
        } else {
            getString(R.string.service_notification_connected, channelDisplay)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(!isPaused)
            .addAction(
                playPauseIcon,
                playPauseText,
                togglePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.service_action_close),
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.service_notification_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    @Suppress("DEPRECATION")
    private fun acquireLocks() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Licha:ChatForegroundWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire()
            }
        }
        if (wifiLock == null) {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wifiManager?.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "Licha:ChatForegroundWifiLock"
            )?.apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e("ChatForegroundService", "Error releasing WakeLock", e)
        }
        wakeLock = null

        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (e: Exception) {
            Log.e("ChatForegroundService", "Error releasing WifiLock", e)
        }
        wifiLock = null
    }

    override fun onDestroy() {
        settingsJob?.cancel()
        releaseLocks()
        super.onDestroy()
    }
}
