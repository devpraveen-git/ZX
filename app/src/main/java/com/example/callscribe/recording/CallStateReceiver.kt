package com.example.callscribe.recording

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.example.callscribe.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallStateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var contactLookupHelper: ContactLookupHelper

    companion object {
        // Shared tracker across broadcast receiver invocations
        private var tracker: PhoneCallStateTracker? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        ensureTracker(context)

        if (action == Intent.ACTION_NEW_OUTGOING_CALL) {
            @Suppress("DEPRECATION")
            val outgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: ""
            tracker?.onOutgoingCallInitiated(outgoingNumber)
            return
        }

        if (action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            @Suppress("DEPRECATION")
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            tracker?.onCallStateChanged(stateStr, incomingNumber)
        }
    }

    private fun ensureTracker(context: Context) {
        if (tracker == null) {
            tracker = PhoneCallStateTracker(object : PhoneCallStateTracker.CallEventListener {
                override fun onCallStarted(phoneNumber: String, isIncoming: Boolean) {
                    val rule = settingsRepository.recordRule.value
                    val selectedContacts = settingsRepository.selectedContacts.value
                    val evaluator = RecordingRuleEvaluator { number ->
                        contactLookupHelper.isKnownContact(number)
                    }

                    if (evaluator.shouldRecord(rule, phoneNumber, selectedContacts)) {
                        CallRecordingService.start(context, phoneNumber, isIncoming)
                    }
                }

                override fun onCallEnded(phoneNumber: String, isIncoming: Boolean, callDurationMs: Long) {
                    CallRecordingService.stop(context)
                }
            })
        }
    }
}
