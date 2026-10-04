package com.example.callscribe.subtitle

import com.example.callscribe.data.model.Speaker
import com.example.callscribe.data.model.TranscriptSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SrtGeneratorTest {

    private val sampleSegments = listOf(
        TranscriptSegment(
            id = 1,
            recordingId = 10,
            startTimeMs = 1250,
            endTimeMs = 4500,
            speaker = Speaker.YOU,
            originalText = "Hello, can you hear me clearly?",
            translatedText = "Hola, ¿puedes escucharme claramente?"
        ),
        TranscriptSegment(
            id = 2,
            recordingId = 10,
            startTimeMs = 5000,
            endTimeMs = 8200,
            speaker = Speaker.OTHER,
            originalText = "Yes, I hear you loud and clear.",
            translatedText = "Sí, te escucho alto y claro."
        )
    )

    @Test
    fun generateSrt_formatsStandardSrtLayout() {
        val srt = SrtGenerator.generateSrt(sampleSegments, useTranslated = false)

        val expected = """
            1
            00:00:01,250 --> 00:00:04,500
            [You]: Hello, can you hear me clearly?

            2
            00:00:05,000 --> 00:00:08,200
            [Other]: Yes, I hear you loud and clear.
        """.trimIndent() + "\n"

        assertEquals(expected, srt)
    }

    @Test
    fun generateSrt_withTranslation_usesTranslatedText() {
        val srt = SrtGenerator.generateSrt(sampleSegments, useTranslated = true)

        assertTrue(srt.contains("Hola, ¿puedes escucharme claramente?"))
        assertTrue(srt.contains("Sí, te escucho alto y claro."))
    }

    @Test
    fun generateVtt_formatsWebVttHeaderAndVoiceTags() {
        val vtt = SrtGenerator.generateVtt(sampleSegments, useTranslated = false)

        assertTrue(vtt.startsWith("WEBVTT"))
        assertTrue(vtt.contains("00:00:01.250 --> 00:00:04.500"))
        assertTrue(vtt.contains("<v You>Hello, can you hear me clearly?</v>"))
        assertTrue(vtt.contains("<v Other>Yes, I hear you loud and clear.</v>"))
    }

    @Test
    fun parseSrt_parsesBackToTranscriptSegments() {
        val srt = SrtGenerator.generateSrt(sampleSegments, useTranslated = false)
        val parsed = SrtGenerator.parseSrt(srt, recordingId = 10)

        assertEquals(2, parsed.size)
        assertEquals(1250L, parsed[0].startTimeMs)
        assertEquals(4500L, parsed[0].endTimeMs)
        assertEquals(Speaker.YOU, parsed[0].speaker)
        assertEquals("Hello, can you hear me clearly?", parsed[0].originalText)

        assertEquals(5000L, parsed[1].startTimeMs)
        assertEquals(8200L, parsed[1].endTimeMs)
        assertEquals(Speaker.OTHER, parsed[1].speaker)
        assertEquals("Yes, I hear you loud and clear.", parsed[1].originalText)
    }

    @Test
    fun formatTimestamp_formatsHoursMinutesSecondsMillisCorrectly() {
        val millis = 3661500L // 1 hour, 1 minute, 1 second, 500 ms
        val srtFormatted = SrtGenerator.formatTimestampSrt(millis)
        val vttFormatted = SrtGenerator.formatTimestampVtt(millis)

        assertEquals("01:01:01,500", srtFormatted)
        assertEquals("01:01:01.500", vttFormatted)
    }
}
