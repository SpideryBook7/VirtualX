package dev.vpad.controller.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.vpad.controller.input.InputProcessor
import dev.vpad.controller.input.VirtualDeviceManager
import dev.vpad.controller.ui.OverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VPadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var inputProcessor: InputProcessor
    private lateinit var overlayManager: OverlayManager

    override fun onCreate() {
        super.onCreate()

        inputProcessor = InputProcessor()
        overlayManager = OverlayManager(this, inputProcessor)

        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            buildNotification("Emulating Gamepad via Shizuku..."),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            else 0
        )

        // Initialize Shizuku on IO thread, then show overlay on Main thread
        serviceScope.launch {
            val initialized = withContext(Dispatchers.IO) {
                VirtualDeviceManager.initialize()
            }
            
            if (initialized) {
                overlayManager.showOverlay()
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, buildNotification("V-PAD: Hybrid Input Mode ✓"))
            } else {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager.hideOverlay()
        VirtualDeviceManager.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "V-PAD Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("V-PAD Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "vpad_service_channel"
        const val NOTIFICATION_ID = 1
    }
}
