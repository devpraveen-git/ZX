package com.example.callscribe.translation

import com.example.callscribe.data.model.TranscriptSegment
import com.example.callscribe.data.repository.RecordingRepository
import com.example.callscribe.subtitle.SrtGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationCoordinator @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val translationEngine: MlKitTranslationEngine
) {
    suspend fun translateRecording(
        recordingId: Long,
        targetLanguage: String
    ): Result<List<TranscriptSegment>> = withContext(Dispatchers.IO) {
        val recording = recordingRepository.getRecordingByIdSync(recordingId)
            ?: return@withContext Result.failure(IllegalArgumentException("Recording $recordingId not found"))

        val segments = recordingRepository.getTranscriptSegments(recordingId).firstOrNull() ?: emptyList()
        if (segments.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("No transcript segments found to translate"))
        }

        val sourceLang = recording.sourceLanguage.ifBlank { "en" }
        val result = translationEngine.translateSegments(segments, sourceLang, targetLanguage)

        result.onSuccess { translatedSegments ->
            recordingRepository.saveTranscriptSegments(recordingId, translatedSegments)

            val fullTranslated = translatedSegments.joinToString(" ") { it.translatedText ?: it.originalText }
            val updated = recording.copy(
                isTranslated = true,
                translatedText = fullTranslated,
                targetLanguage = targetLanguage
            )
            recordingRepository.updateRecording(updated)

            // Export translated SRT companion
            runCatching {
                val audioFile = File(recording.audioFilePath)
                if (audioFile.exists()) {
                    val srtFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}_$targetLanguage.srt")
                    srtFile.writeText(SrtGenerator.generateSrt(translatedSegments, useTranslated = true))
                }
            }
        }

        result
    }
}
