package com.example.callscribe.transcription

import com.example.callscribe.data.repository.SettingsRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechTranscriptionRouter @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val onDeviceEngine: OnDeviceSpeechEngine,
    private val cloudEngine: CloudTranscriptionEngine
) {
    var isCloudEnabledProvider: () -> Boolean = { settingsRepository.isCloudFallbackEnabled.value }
    var cloudApiKeyProvider: () -> String = { settingsRepository.cloudApiKey.value }

    suspend fun transcribe(
        recordingId: Long,
        audioFile: File,
        promptLanguage: String? = null
    ): Result<TranscriptionResult> {
        val cloudEnabled = isCloudEnabledProvider()
        val hasCloudKey = cloudApiKeyProvider().isNotBlank()

        if (cloudEnabled && hasCloudKey) {
            val cloudResult = cloudEngine.transcribe(recordingId, audioFile, promptLanguage)
            if (cloudResult.isSuccess) {
                return cloudResult
            }
            // If cloud fails, gracefully fallback to on-device engine
        }

        return onDeviceEngine.transcribe(recordingId, audioFile, promptLanguage)
    }

    fun getActiveEngineName(): String {
        val cloudEnabled = isCloudEnabledProvider()
        val hasCloudKey = cloudApiKeyProvider().isNotBlank()
        return if (cloudEnabled && hasCloudKey) {
            cloudEngine.engineName
        } else {
            onDeviceEngine.engineName
        }
    }
}
