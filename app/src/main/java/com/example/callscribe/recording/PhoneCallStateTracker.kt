package com.example.callscribe.recording

class PhoneCallStateTracker(
    private val listener: CallEventListener
) {
    interface CallEventListener {
        fun onCallStarted(phoneNumber: String, isIncoming: Boolean)
        fun onCallEnded(phoneNumber: String, isIncoming: Boolean, callDurationMs: Long)
    }

    private var lastState: CallState = CallState.IDLE
    private var isIncoming: Boolean = false
    private var currentPhoneNumber: String = ""
    private var callStartTimeMs: Long = 0

    enum class CallState {
        IDLE,
        RINGING,
        OFFHOOK
    }

    val isInCall: Boolean
        get() = lastState == CallState.OFFHOOK

    fun onOutgoingCallInitiated(number: String) {
        currentPhoneNumber = number
        isIncoming = false
    }

    fun onCallStateChanged(stateString: String?, incomingNumber: String?) {
        val state = when (stateString) {
            "RINGING" -> CallState.RINGING
            "OFFHOOK" -> CallState.OFFHOOK
            "IDLE" -> CallState.IDLE
            else -> return
        }
        processStateTransition(state, incomingNumber)
    }

    fun processStateTransition(newState: CallState, incomingNumber: String?) {
        if (!incomingNumber.isNullOrEmpty()) {
            currentPhoneNumber = incomingNumber
        }

        if (lastState == newState) {
            return
        }

        when (newState) {
            CallState.RINGING -> {
                isIncoming = true
            }

            CallState.OFFHOOK -> {
                if (lastState == CallState.RINGING) {
                    // Incoming call was answered
                    isIncoming = true
                } else {
                    // Direct offhook from IDLE => Outgoing call was placed and answered
                    if (currentPhoneNumber.isEmpty() && !incomingNumber.isNullOrEmpty()) {
                        currentPhoneNumber = incomingNumber
                    }
                    isIncoming = false
                }
                callStartTimeMs = System.currentTimeMillis()
                listener.onCallStarted(
                    phoneNumber = currentPhoneNumber.ifEmpty { "Unknown" },
                    isIncoming = isIncoming
                )
            }

            CallState.IDLE -> {
                if (lastState == CallState.OFFHOOK) {
                    // Active call ended
                    val duration = if (callStartTimeMs > 0) System.currentTimeMillis() - callStartTimeMs else 0
                    listener.onCallEnded(
                        phoneNumber = currentPhoneNumber.ifEmpty { "Unknown" },
                        isIncoming = isIncoming,
                        callDurationMs = duration
                    )
                }
                // Reset state
                currentPhoneNumber = ""
                callStartTimeMs = 0
                isIncoming = false
            }
        }

        lastState = newState
    }
}
