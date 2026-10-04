package com.example.callscribe.recording

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            // The receiver initializes components on device boot
            // Broadcast receivers for PHONE_STATE are registered in manifest and will activate automatically.
        }
    }
}
