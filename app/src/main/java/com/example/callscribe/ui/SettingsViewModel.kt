package com.example.callscribe.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callscribe.data.model.AudioSourceType
import com.example.callscribe.data.model.RecordRule
import com.example.callscribe.data.repository.RecordingRepository
import com.example.callscribe.data.repository.SettingsRepository
import com.example.callscribe.recording.OemAutoStartHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    val recordRule: StateFlow<RecordRule> = settingsRepository.recordRule
    val isSpeakerAutoOn: StateFlow<Boolean> = settingsRepository.isSpeakerAutoOn
    val audioSource: StateFlow<AudioSourceType> = settingsRepository.audioSource
    val playNoticeTone: StateFlow<Boolean> = settingsRepository.playNoticeTone
    val autoDeleteDays: StateFlow<Int> = settingsRepository.autoDeleteDays
    val isAppLockEnabled: StateFlow<Boolean> = settingsRepository.isAppLockEnabled
    val whisperModelName: StateFlow<String> = settingsRepository.whisperModelName
    val defaultTargetLanguage: StateFlow<String> = settingsRepository.defaultTargetLanguage
    val isCloudFallbackEnabled: StateFlow<Boolean> = settingsRepository.isCloudFallbackEnabled
    val cloudApiKey: StateFlow<String> = settingsRepository.cloudApiKey
    val hasAcceptedConsent: StateFlow<Boolean> = settingsRepository.hasAcceptedConsent

    val detectedBrand = OemAutoStartHelper.detectBrand()
    val isIgnoringBatteryOptimizations: Boolean
        get() = OemAutoStartHelper.isIgnoringBatteryOptimizations(context)

    fun setRecordRule(rule: RecordRule) = settingsRepository.setRecordRule(rule)
    fun setSpeakerAutoOn(enabled: Boolean) = settingsRepository.setSpeakerAutoOn(enabled)
    fun setAudioSource(source: AudioSourceType) = settingsRepository.setAudioSource(source)
    fun setPlayNoticeTone(enabled: Boolean) = settingsRepository.setPlayNoticeTone(enabled)
    fun setAutoDeleteDays(days: Int) = settingsRepository.setAutoDeleteDays(days)
    fun setAppLockEnabled(enabled: Boolean) = settingsRepository.setAppLockEnabled(enabled)
    fun setAppPin(pin: String?) = settingsRepository.setAppPin(pin)
    fun getAppPin(): String? = settingsRepository.getAppPin()
    fun setWhisperModelName(model: String) = settingsRepository.setWhisperModelName(model)
    fun setDefaultTargetLanguage(lang: String) = settingsRepository.setDefaultTargetLanguage(lang)
    fun setCloudFallbackEnabled(enabled: Boolean) = settingsRepository.setCloudFallbackEnabled(enabled)
    fun setCloudApiKey(key: String) = settingsRepository.setCloudApiKey(key)
    fun setHasAcceptedConsent(accepted: Boolean) = settingsRepository.setHasAcceptedConsent(accepted)

    fun deleteAllRecordings() {
        viewModelScope.launch {
            recordingRepository.deleteAllRecordings()
        }
    }

    fun getOemBrandInstructions(): String {
        return OemAutoStartHelper.getBrandInstructions(detectedBrand)
    }

    fun openOemAutostartSettings() {
        val intent = OemAutoStartHelper.getOemAutostartIntent(context)
        if (intent != null) {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun openBatteryOptimizationSettings() {
        val intent = OemAutoStartHelper.getRequestBatteryOptimizationIntent(context)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
