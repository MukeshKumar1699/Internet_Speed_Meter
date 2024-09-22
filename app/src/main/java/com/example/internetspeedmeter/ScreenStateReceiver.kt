package com.example.internetspeedmeter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenStateReceiver(private val service: InternetSpeedFGService) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            service.pauseService()
        } else if (intent.action == Intent.ACTION_SCREEN_ON) {
            service.resumeService()
        }
    }
}
