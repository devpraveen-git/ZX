package com.example.callscribe.transcription

import com.example.callscribe.data.model.Speaker
import com.example.callscribe.data.model.TranscriptSegment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeakerDiarizer @Inject constructor() {

    data class RawAudioSegment(
        val startTimeMs: Long,
        val endTimeMs: Long,
        val text: String,
        val averageRmsEnergy: Float,
        val confidence: Float = 0.95f
    )

    fun diarizeSegments(
        recordingId: Long,
        rawSegments: List<RawAudioSegment>
    ): List<TranscriptSegment> {
        if (rawSegments.isEmpty()) return emptyList()

        val energies = rawSegments.map { it.averageRmsEnergy }
        val medianEnergy = energies.sorted().let { sorted ->
            if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2f
            } else {
                sorted[sorted.size / 2]
            }
        }

        var currentSpeaker = Speaker.YOU
        var lastEndTimeMs = 0L

        return rawSegments.mapIndexed { index, seg ->
            val silenceGap = if (index > 0) seg.startTimeMs - lastEndTimeMs else 0L

            val speaker = when {
                // If there's a significant conversational pause (> 600ms), speaker likely alternated
                silenceGap > 600L -> {
                    currentSpeaker = if (currentSpeaker == Speaker.YOU) Speaker.OTHER else Speaker.YOU
                    currentSpeaker
                }
                // Energy heuristic: Near-field local speaker (You) has higher RMS than speakerphone output (Other)
                seg.averageRmsEnergy > (medianEnergy * 1.15f) -> {
                    Speaker.YOU
                }
                seg.averageRmsEnergy < (medianEnergy * 0.85f) -> {
                    Speaker.OTHER
                }
                else -> {
                    currentSpeaker
                }
            }

            currentSpeaker = speaker
            lastEndTimeMs = seg.endTimeMs

            TranscriptSegment(
                id = 0,
                recordingId = recordingId,
                startTimeMs = seg.startTimeMs,
                endTimeMs = seg.endTimeMs,
                speaker = speaker,
                originalText = seg.text.trim(),
                confidence = seg.confidence
            )
        }
    }
}
