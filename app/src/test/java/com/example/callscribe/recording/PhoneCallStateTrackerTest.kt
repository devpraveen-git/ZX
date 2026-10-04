package com.example.callscribe.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PhoneCallStateTrackerTest {

    private lateinit var tracker: PhoneCallStateTracker
    private var startedCallNumber: String? = null
    private var startedCallIsIncoming: Boolean? = null
    private var endedCallNumber: String? = null
    private var endedCallIsIncoming: Boolean? = null
    private var endedCallDuration: Long? = null

    @Before
    fun setup() {
        startedCallNumber = null
        startedCallIsIncoming = null
        endedCallNumber = null
        endedCallIsIncoming = null
        endedCallDuration = null

        tracker = PhoneCallStateTracker(object : PhoneCallStateTracker.CallEventListener {
            override fun onCallStarted(phoneNumber: String, isIncoming: Boolean) {
                startedCallNumber = phoneNumber
                startedCallIsIncoming = isIncoming
            }

            override fun onCallEnded(phoneNumber: String, isIncoming: Boolean, callDurationMs: Long) {
                endedCallNumber = phoneNumber
                endedCallIsIncoming = isIncoming
                endedCallDuration = callDurationMs
            }
        })
    }

    @Test
    fun incomingCallAnsweredAndEnded_triggersExpectedEvents() {
        assertFalse(tracker.isInCall)

        // 1. Phone rings
        tracker.onCallStateChanged("RINGING", "+1234567890")
        assertFalse(tracker.isInCall)
        assertEquals(null, startedCallNumber)

        // 2. User answers (OFFHOOK)
        tracker.onCallStateChanged("OFFHOOK", "+1234567890")
        assertTrue(tracker.isInCall)
        assertEquals("+1234567890", startedCallNumber)
        assertEquals(true, startedCallIsIncoming)

        // 3. User hangs up (IDLE)
        tracker.onCallStateChanged("IDLE", null)
        assertFalse(tracker.isInCall)
        assertEquals("+1234567890", endedCallNumber)
        assertEquals(true, endedCallIsIncoming)
        assertTrue((endedCallDuration ?: -1) >= 0)
    }

    @Test
    fun outgoingCallInitiatedAndEnded_triggersExpectedEvents() {
        // 1. Dial outgoing number
        tracker.onOutgoingCallInitiated("+1987654321")

        // 2. Offhook (dialing/connected)
        tracker.onCallStateChanged("OFFHOOK", null)
        assertTrue(tracker.isInCall)
        assertEquals("+1987654321", startedCallNumber)
        assertEquals(false, startedCallIsIncoming)

        // 3. Call ends
        tracker.onCallStateChanged("IDLE", null)
        assertFalse(tracker.isInCall)
        assertEquals("+1987654321", endedCallNumber)
        assertEquals(false, endedCallIsIncoming)
    }

    @Test
    fun missedIncomingCall_doesNotStartRecording() {
        // Rings then caller hangs up before answer
        tracker.onCallStateChanged("RINGING", "+15551234")
        tracker.onCallStateChanged("IDLE", null)

        assertFalse(tracker.isInCall)
        assertEquals(null, startedCallNumber)
        assertEquals(null, endedCallNumber)
    }
}
