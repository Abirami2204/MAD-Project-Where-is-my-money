package com.justspent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UsageMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var usageStatsManager: UsageStatsManager

    private var upiAppWasOpen = false
    private var lastUpiApp = ""

    companion object {
        const val CHANNEL_ID = "UsageMonitorChannel"
        const val CAPTURE_CHANNEL_ID = "ExpenseCaptureChannel"
        const val NOTIFICATION_ID = 1
        const val CAPTURE_NOTIFICATION_ID = 2
        
        val UPI_APPS = mapOf(
            "com.google.android.apps.nbu.paisa.user" to "Google Pay",
            "com.phonepe.app" to "PhonePe",
            "net.one97.paytm" to "Paytm",
            "in.amazon.mShop.android.shopping" to "Amazon Pay",
            "in.org.npci.upiapp" to "BHIM",
            "com.dreamplug.androidapp" to "CRED"
        )
    }

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        startPolling()
        return START_STICKY
    }

    private fun startPolling() {
        serviceScope.launch {
            Log.d("UsageMonitor", "=== Polling started ===")
            while (isActive) {
                try {
                    val currentAppPackage = getTopApp()
                    
                    Log.d("UsageMonitor", "Poll: topApp=$currentAppPackage | upiWasOpen=$upiAppWasOpen | lastUpi=$lastUpiApp")
                    
                    if (currentAppPackage != null) {
                        val appName = UPI_APPS[currentAppPackage]
                        
                        if (appName != null) {
                            if (!upiAppWasOpen) {
                                Log.d("UsageMonitor", ">>> UPI App OPENED: $appName ($currentAppPackage)")
                            }
                            upiAppWasOpen = true
                            lastUpiApp = appName
                        } else if (upiAppWasOpen) {
                            Log.d("UsageMonitor", ">>> UPI App CLOSED: $lastUpiApp -> now: $currentAppPackage")
                            triggerCaptureViaFullScreenIntent(lastUpiApp)
                            upiAppWasOpen = false
                        }
                    } else {
                        Log.d("UsageMonitor", "Poll: No foreground app detected (null)")
                    }
                } catch (e: Exception) {
                    Log.e("UsageMonitor", "Error in poll loop", e)
                }
                
                delay(2000)
            }
        }
    }

    private fun getTopApp(): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000

        val events = usageStatsManager.queryEvents(startTime, endTime)
        var topPackageName: String? = null
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                topPackageName = event.packageName
            }
        }
        return topPackageName
    }

    /**
     * Uses a full-screen intent notification to launch ExpenseCaptureActivity.
     * This is the Android-approved way to show UI from a foreground service
     * on Android 10+ where background activity starts are blocked.
     */
    private fun triggerCaptureViaFullScreenIntent(sourceApp: String) {
        try {
            val intent = Intent(this, com.justspent.ui.capture.ExpenseCaptureActivity::class.java).apply {
                putExtra("SOURCE_APP", sourceApp)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CAPTURE_CHANNEL_ID)
                .setContentTitle("💰 Just spent on $sourceApp?")
                .setContentText("Tap to log your expense")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .build()

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(CAPTURE_NOTIFICATION_ID, notification)
            
            Log.d("UsageMonitor", ">>> Full-screen intent notification fired for: $sourceApp")
        } catch (e: Exception) {
            Log.e("UsageMonitor", "Failed to fire full-screen intent", e)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Low-priority channel for persistent monitoring notification
            val monitorChannel = NotificationChannel(
                CHANNEL_ID,
                "Expense Tracker Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors when you close payment apps to track expenses"
            }

            // High-priority channel for capture prompts (full-screen intent)
            val captureChannel = NotificationChannel(
                CAPTURE_CHANNEL_ID,
                "Expense Capture Prompts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows expense entry prompt after closing a payment app"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(monitorChannel)
            notificationManager.createNotificationChannel(captureChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JustSpent is running")
            .setContentText("Monitoring for payment app usage")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
