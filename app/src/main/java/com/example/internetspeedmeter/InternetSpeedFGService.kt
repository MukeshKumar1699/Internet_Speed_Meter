package com.example.internetspeedmeter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class InternetSpeedFGService : Service() {

    private val TAG = "InternetSpeedFGService"
    private val notificationId = 1
    private lateinit var notificationManager: NotificationManager
    private val serviceScopeIO = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var networkHelper: NetworkHelper

    private var isPaused = false
    private lateinit var screenStateReceiver: ScreenStateReceiver

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        screenStateReceiver = ScreenStateReceiver(this).apply {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(this, filter)
        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        networkHelper = NetworkHelper(applicationContext)

        startForeground(notificationId, createNotification(Pair("0", "0"),null))

        // Start coroutine for monitoring speed
        serviceScopeIO.launch {
            monitorNetworkSpeed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScopeIO.cancel() // Cancel the coroutine when the service is stopped
        unregisterReceiver(screenStateReceiver)
        Log.d(TAG, "Service destroyed")
    }

    private suspend fun monitorNetworkSpeed() {
        while (serviceScopeIO.isActive) {
            if (networkHelper.isConnectedToInternet() && !isPaused) {
                Log.d(TAG, "Monitoring network speed")
                val speed = networkHelper.calculateNetworkSpeed()
                val wifiSignalStrength = if (networkHelper.isConnectedToWifi()) {
                    networkHelper.getWifiSignalStrengthPercentage()
                } else null

                // Update the notification on the main thread
                withContext(Dispatchers.Main) {
                    updateNotification(speed, wifiSignalStrength)
                }
            }
            delay(1000)  // Wait for 1 second before repeating
        }
    }

    private fun createNotification(speed: Pair<String, String>, wifiSignalStrength: Int?): Notification {
        val notificationChannelId = "network_speed_service_channel"

        val channel = NotificationChannel(
            notificationChannelId, "Network Speed Service",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        val notificationLayout = RemoteViews(packageName, R.layout.notification_custom).apply {
            setTextViewText(R.id.speedTV, "${speed.first} / ${speed.second}")

            if (networkHelper.isConnectedToWifi()) {
                setViewVisibility(R.id.signalTV, View.VISIBLE)
                wifiSignalStrength?.let {
                    setTextViewText(R.id.signalTV, "Signal: $it%")
                } ?: run {
                    setTextViewText(R.id.signalTV, "")
                }
            } else {
                setViewVisibility(R.id.signalTV, View.GONE)
            }
        }


        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Network Speed Monitor")
            .setCustomContentView(notificationLayout)
            .setSmallIcon(R.drawable.speed)
            .setOnlyAlertOnce(true)
            .setShowWhen(false) // Hide the time from the notification
            .build()
    }

    private fun updateNotification(speed: Pair<String, String>, wifiSignalStrength: Int?) {
        Log.d(TAG, "Updating notification: $speed")
        val notification = createNotification(speed, wifiSignalStrength)
        notificationManager.notify(notificationId, notification)
    }

    fun scheduleSpeedMonitoringWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(true) // Optional: run only when charging
            .build()

        val workRequest: WorkRequest = PeriodicWorkRequestBuilder<SpeedMonitoringWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }

    fun pauseService() {
        isPaused = true
        Log.d(TAG, "Service paused")
    }

    fun resumeService() {
        isPaused = false
        Log.d(TAG, "Service resumed")
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}
