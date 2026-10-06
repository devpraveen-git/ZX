package com.example.callscribe.translation

import com.example.callscribe.data.model.TranscriptSegment
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class MlKitTranslationEngine @Inject constructor() : TranslationEngine {

    private val modelManager = RemoteModelManager.getInstance()

    override suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext Result.success("")

        val srcCode = normalizeLanguageCode(sourceLanguage)
        val tgtCode = normalizeLanguageCode(targetLanguage)

        if (srcCode == tgtCode) return@withContext Result.success(text)

        var translator: Translator? = null
        try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(srcCode)
                .setTargetLanguage(tgtCode)
                .build()

            translator = Translation.getClient(options)
            val conditions = DownloadConditions.Builder().build()
            awaitTask(translator.downloadModelIfNeeded(conditions))

            val translated = awaitTask(translator.translate(text))
            Result.success(translated)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            translator?.close()
        }
    }

    override suspend fun translateSegments(
        segments: List<TranscriptSegment>,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<List<TranscriptSegment>> = withContext(Dispatchers.IO) {
        if (segments.isEmpty()) return@withContext Result.success(emptyList())

        val srcCode = normalizeLanguageCode(sourceLanguage)
        val tgtCode = normalizeLanguageCode(targetLanguage)

        if (srcCode == tgtCode) {
            return@withContext Result.success(segments.map { it.copy(translatedText = it.originalText) })
        }

        var translator: Translator? = null
        try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(srcCode)
                .setTargetLanguage(tgtCode)
                .build()

            translator = Translation.getClient(options)
            val conditions = DownloadConditions.Builder().build()
            awaitTask(translator.downloadModelIfNeeded(conditions))

            val translatedSegments = segments.map { segment ->
                if (segment.originalText.isNotBlank()) {
                    val translatedText = awaitTask(translator.translate(segment.originalText))
                    segment.copy(translatedText = translatedText)
                } else {
                    segment.copy(translatedText = "")
                }
            }

            Result.success(translatedSegments)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            translator?.close()
        }
    }

    override suspend fun downloadLanguageModel(languageCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalized = normalizeLanguageCode(languageCode)
            val model = TranslateRemoteModel.Builder(normalized).build()
            val conditions = DownloadConditions.Builder().build()
            awaitTask(modelManager.download(model, conditions))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isModelDownloaded(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val normalized = normalizeLanguageCode(languageCode)
            val model = TranslateRemoteModel.Builder(normalized).build()
            awaitTask(modelManager.isModelDownloaded(model))
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun deleteLanguageModel(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val normalized = normalizeLanguageCode(languageCode)
            val model = TranslateRemoteModel.Builder(normalized).build()
            awaitTask(modelManager.deleteDownloadedModel(model))
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun normalizeLanguageCode(code: String): String {
        val clean = code.trim().lowercase().take(2)
        return TranslateLanguage.fromLanguageTag(clean) ?: TranslateLanguage.ENGLISH
    }

    private suspend fun <T> awaitTask(task: Task<T>): T = suspendCancellableCoroutine { continuation ->
        task.addOnSuccessListener { result ->
            continuation.resume(result)
        }.addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
    }
}
