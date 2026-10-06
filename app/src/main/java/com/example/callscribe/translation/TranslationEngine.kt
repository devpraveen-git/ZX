package com.example.callscribe.translation

import com.example.callscribe.data.model.TranscriptSegment

interface TranslationEngine {
    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<String>

    suspend fun translateSegments(
        segments: List<TranscriptSegment>,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<List<TranscriptSegment>>

    suspend fun downloadLanguageModel(languageCode: String): Result<Unit>
    suspend fun isModelDownloaded(languageCode: String): Boolean
    suspend fun deleteLanguageModel(languageCode: String): Boolean
}
