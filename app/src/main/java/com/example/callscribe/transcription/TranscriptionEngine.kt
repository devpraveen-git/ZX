package com.example.callscribe.transcription

import com.example.callscribe.data.model.TranscriptSegment
import java.io.File

data class TranscriptionResult(
    val fullText: String,
    val detectedLanguage: String,
    val segments: List<TranscriptSegment>,
    val isCloud: Boolean = false
)

interface TranscriptionEngine {
    val engineName: String
    suspend fun isModelAvailable(): Boolean
    suspend fun transcribe(
        recordingId: Long,
        audioFile: File,
        promptLanguage: String? = null
    ): Result<TranscriptionResult>
}
