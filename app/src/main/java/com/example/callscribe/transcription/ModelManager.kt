package com.example.callscribe.transcription

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class AiModelInfo(
        val id: String,
        val displayName: String,
        val sizeDescription: String,
        val fileName: String,
        val downloadUrl: String,
        val isInstalled: Boolean = false,
        val isDownloading: Boolean = false,
        val downloadProgress: Float = 0f
    )

    private val modelsDir: File
        get() = File(context.filesDir, "models").apply { if (!exists()) mkdirs() }

    private val defaultModels = listOf(
        AiModelInfo(
            id = "whisper_tiny",
            displayName = "Whisper Tiny (Fastest)",
            sizeDescription = "~75 MB",
            fileName = "ggml-tiny.bin",
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin"
        ),
        AiModelInfo(
            id = "whisper_base",
            displayName = "Whisper Base (Balanced)",
            sizeDescription = "~142 MB",
            fileName = "ggml-base.bin",
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"
        ),
        AiModelInfo(
            id = "whisper_small",
            displayName = "Whisper Small (High Accuracy)",
            sizeDescription = "~466 MB",
            fileName = "ggml-small.bin",
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin"
        )
    )

    private val _modelsState = MutableStateFlow(loadModelsState())
    val modelsState: StateFlow<List<AiModelInfo>> = _modelsState.asStateFlow()

    fun getModelFile(fileName: String): File {
        return File(modelsDir, fileName)
    }

    fun isModelInstalled(fileName: String): Boolean {
        val file = getModelFile(fileName)
        return file.exists() && file.length() > 0
    }

    private fun loadModelsState(): List<AiModelInfo> {
        return defaultModels.map { model ->
            model.copy(isInstalled = isModelInstalled(model.fileName))
        }
    }

    suspend fun downloadModel(modelId: String): Result<File> = withContext(Dispatchers.IO) {
        val model = _modelsState.value.firstOrNull { it.id == modelId }
            ?: return@withContext Result.failure(IllegalArgumentException("Model $modelId not found"))

        val targetFile = getModelFile(model.fileName)
        updateModelState(modelId) { it.copy(isDownloading = true, downloadProgress = 0f) }

        try {
            val url = URL(model.downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
            }
            connection.connect()

            val totalBytes = connection.contentLength.toFloat()
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val progress = (downloadedBytes / totalBytes).coerceIn(0f, 1f)
                            updateModelState(modelId) { it.copy(downloadProgress = progress) }
                        }
                    }
                }
            }

            updateModelState(modelId) {
                it.copy(isDownloading = false, isInstalled = true, downloadProgress = 1f)
            }
            Result.success(targetFile)
        } catch (e: Exception) {
            if (targetFile.exists()) targetFile.delete()
            updateModelState(modelId) {
                it.copy(isDownloading = false, isInstalled = false, downloadProgress = 0f)
            }
            Result.failure(e)
        }
    }

    fun deleteModel(modelId: String): Boolean {
        val model = _modelsState.value.firstOrNull { it.id == modelId } ?: return false
        val file = getModelFile(model.fileName)
        val deleted = if (file.exists()) file.delete() else false
        updateModelState(modelId) { it.copy(isInstalled = false, downloadProgress = 0f) }
        return deleted
    }

    private fun updateModelState(modelId: String, transform: (AiModelInfo) -> AiModelInfo) {
        _modelsState.value = _modelsState.value.map {
            if (it.id == modelId) transform(it) else it
        }
    }
}
