package com.example.internetspeedmeter

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import java.util.Locale

class NetworkHelper(private val context: Context) {

    private var previousRxBytes: Long = 0
    private var previousTxBytes: Long = 0
    private var lastUpdateTime: Long = System.currentTimeMillis()

    private val wifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    fun calculateNetworkSpeed(): Pair<String, String> {
        val currentRxBytes = TrafficStats.getTotalRxBytes()
        val currentTxBytes = TrafficStats.getTotalTxBytes()
        val currentTime = System.currentTimeMillis()
        val timeElapsed = (currentTime - lastUpdateTime) / 1000

        if (timeElapsed <= 0) return Pair("0 B/s", "0 B/s")

        val downloadSpeed = (currentRxBytes - previousRxBytes) / timeElapsed
        val uploadSpeed = (currentTxBytes - previousTxBytes) / timeElapsed

        // Update previous values
        previousRxBytes = currentRxBytes
        previousTxBytes = currentTxBytes
        lastUpdateTime = currentTime

        return Pair(formatSpeed(downloadSpeed), formatSpeed(uploadSpeed))
    }

    private fun formatSpeed(speedInBytes: Long): String {
        return when {
            speedInBytes < 1024 -> String.format(Locale.ROOT, "%d B/s", speedInBytes)
            speedInBytes < 1024 * 1024 -> String.format(Locale.ROOT, "%d KB/s", speedInBytes / 1024)
            else -> String.format(Locale.ROOT, "%d MB/s", speedInBytes / (1024 * 1024))
        }
    }

    fun isConnectedToInternet(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    fun isConnectedToWifi(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    fun getWifiSignalStrengthPercentage(): Int? {
        if (isConnectedToWifi()) {
            val wifiInfo = wifiManager.connectionInfo
            return WifiManager.calculateSignalLevel(wifiInfo.rssi, 100)
        }
        return null
    }
}
