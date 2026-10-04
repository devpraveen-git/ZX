package com.example.callscribe.recording

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioAlertHelper @Inject constructor() {

    fun playRecordingNoticeBeep() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
        } catch (_: Exception) {
            // Tone generator error fallback
        }
    }
}
