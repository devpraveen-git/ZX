package com.example.callscribe

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CallScribeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val recordingChannel = NotificationChannel(
                CHANNEL_RECORDING_ID,
                "Call Recording Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing status when an active phone call is being recorded"
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "CallScribe Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for completed transcripts, translation, and storage notices"
            }

            notificationManager.createNotificationChannel(recordingChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        const val CHANNEL_RECORDING_ID = "channel_call_recording"
        const val CHANNEL_ALERTS_ID = "channel_callscribe_alerts"
    }
}
