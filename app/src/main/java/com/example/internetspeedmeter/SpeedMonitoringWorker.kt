package com.example.internetspeedmeter

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters

class SpeedMonitoringWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private val networkHelper = NetworkHelper(appContext)

    override fun doWork(): Result {
        if (!networkHelper.isConnectedToWifi()) {
            return Result.success() // Exit if not connected
        }

        val (downloadSpeed, uploadSpeed) = networkHelper.calculateNetworkSpeed()

        // Create Data object to pass results
        val outputData = Data.Builder()
            .putString("downloadSpeed", downloadSpeed)
            .putString("uploadSpeed", uploadSpeed)
            .build()

        return Result.success(outputData) // Return success with the Data object
    }
}
