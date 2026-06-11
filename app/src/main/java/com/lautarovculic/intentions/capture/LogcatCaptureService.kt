package com.lautarovculic.intentions.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lautarovculic.intentions.R
import com.lautarovculic.intentions.core.model.CaptureBackend
import com.lautarovculic.intentions.core.root.RootExecutor
import com.lautarovculic.intentions.core.shell.LogcatCommands
import com.lautarovculic.intentions.di.AppContainer
import com.lautarovculic.intentions.di.IntentionsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

// Capture backend B: foreground service that streams su -c logcat into capture events.
class LogcatCaptureService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var container: AppContainer
    private var sessionId: String = ""

    override fun onCreate() {
        super.onCreate()
        container = IntentionsApp.from(application)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        sessionId = UUID.randomUUID().toString()
        startForegroundCompat()
        container.captureRunning.value = true
        container.currentCaptureSession.value = sessionId

        scope.launch {
            container.captureRepository.startSession(sessionId, "Root logcat observer", CaptureBackend.ROOT_LOGCAT)
            val rootOk = container.rootExecutor.isRootAvailable()
            if (!rootOk) {
                container.captureRepository.record(
                    com.lautarovculic.intentions.core.model.CapturedIntentEvent(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        timestamp = System.currentTimeMillis(),
                        backend = CaptureBackend.ROOT_LOGCAT,
                        resultNote = "Root unavailable — logcat backend cannot run",
                        rawEvidence = "su -c logcat failed: no root",
                    )
                )
                stopSelf()
                return@launch
            }
            val parser = LogcatParser()
            container.rootExecutor.stream(LogcatCommands.streamFiltered()) { line ->
                val event = parser.parse(line, sessionId, System.currentTimeMillis())
                if (event != null) container.captureRepository.record(event)
                true // keep streaming until the service is destroyed
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.launch { container.captureRepository.stopSession(sessionId) }
        container.captureRunning.value = false
        container.currentCaptureSession.value = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat() {
        val channelId = "intentions_capture"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(channelId, "Capture", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, LogcatCaptureService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Intentions — capturing IPC")
            .setContentText("Root logcat observer running")
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFFF7A18.toInt())
            .setOngoing(true)
            .addAction(0, "Stop", stopIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    companion object {
        const val ACTION_STOP = "com.lautarovculic.intentions.CAPTURE_STOP"
        private const val NOTIF_ID = 0x1A7E

        fun start(context: Context) {
            val intent = Intent(context, LogcatCaptureService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, LogcatCaptureService::class.java).setAction(ACTION_STOP))
        }
    }
}
