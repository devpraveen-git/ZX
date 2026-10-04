package com.example.callscribe.transcription

import com.example.callscribe.data.model.Speaker
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SpeakerDiarizerTest {

    private lateinit var diarizer: SpeakerDiarizer

    @Before
    fun setup() {
        diarizer = SpeakerDiarizer()
    }

    @Test
    fun diarizeSegments_detectsAlternatingSpeakersAcrossSilenceGaps() {
        val rawSegments = listOf(
            SpeakerDiarizer.RawAudioSegment(
                startTimeMs = 0,
                endTimeMs = 3000,
                text = "Hi, how are you?",
                averageRmsEnergy = 0.5f
            ),
            // Silence gap = 4000 - 3000 = 1000ms (> 600ms conversational turn gap)
            SpeakerDiarizer.RawAudioSegment(
                startTimeMs = 4000,
                endTimeMs = 7000,
                text = "I'm doing well, thank you!",
                averageRmsEnergy = 0.5f
            ),
            // Silence gap = 8500 - 7000 = 1500ms (> 600ms)
            SpeakerDiarizer.RawAudioSegment(
                startTimeMs = 8500,
                endTimeMs = 11000,
                text = "Great to hear that.",
                averageRmsEnergy = 0.5f
            )
        )

        val result = diarizer.diarizeSegments(recordingId = 1, rawSegments = rawSegments)

        assertEquals(3, result.size)
        assertEquals(Speaker.YOU, result[0].speaker)
        assertEquals(Speaker.OTHER, result[1].speaker)
        assertEquals(Speaker.YOU, result[2].speaker)
    }

    @Test
    fun diarizeSegments_energyVariance_assignsYouToHighEnergyAndOtherToLowEnergy() {
        val rawSegments = listOf(
            SpeakerDiarizer.RawAudioSegment(
                startTimeMs = 0,
                endTimeMs = 2000,
                text = "Near field speaker talking",
                averageRmsEnergy = 0.85f // High energy
            ),
            SpeakerDiarizer.RawAudioSegment(
                startTimeMs = 2200,
                endTimeMs = 4000,
                text = "Far field speakerphone voice",
                averageRmsEnergy = 0.20f // Low energy
            )
        )

        val result = diarizer.diarizeSegments(recordingId = 2, rawSegments = rawSegments)

        assertEquals(2, result.size)
        assertEquals(Speaker.YOU, result[0].speaker)
        assertEquals(Speaker.OTHER, result[1].speaker)
    }
}
