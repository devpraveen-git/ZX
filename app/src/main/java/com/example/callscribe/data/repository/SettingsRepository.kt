package com.example.callscribe.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.callscribe.data.model.AudioSourceType
import com.example.callscribe.data.model.RecordRule
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "callscribe_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse {
        context.getSharedPreferences("callscribe_prefs", Context.MODE_PRIVATE)
    }

    private val _recordRule = MutableStateFlow(loadRecordRule())
    val recordRule: StateFlow<RecordRule> = _recordRule.asStateFlow()

    private val _isSpeakerAutoOn = MutableStateFlow(prefs.getBoolean(KEY_SPEAKER_AUTO_ON, true))
    val isSpeakerAutoOn: StateFlow<Boolean> = _isSpeakerAutoOn.asStateFlow()

    private val _audioSource = MutableStateFlow(loadAudioSource())
    val audioSource: StateFlow<AudioSourceType> = _audioSource.asStateFlow()

    private val _playNoticeTone = MutableStateFlow(prefs.getBoolean(KEY_NOTICE_TONE, false))
    val playNoticeTone: StateFlow<Boolean> = _playNoticeTone.asStateFlow()

    private val _autoDeleteDays = MutableStateFlow(prefs.getInt(KEY_AUTO_DELETE_DAYS, 30))
    val autoDeleteDays: StateFlow<Int> = _autoDeleteDays.asStateFlow()

    private val _isAppLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK, false))
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _selectedContacts = MutableStateFlow(prefs.getStringSet(KEY_SELECTED_CONTACTS, emptySet()) ?: emptySet())
    val selectedContacts: StateFlow<Set<String>> = _selectedContacts.asStateFlow()

    private val _hasAcceptedConsent = MutableStateFlow(prefs.getBoolean(KEY_CONSENT, false))
    val hasAcceptedConsent: StateFlow<Boolean> = _hasAcceptedConsent.asStateFlow()

    private val _whisperModelName = MutableStateFlow(prefs.getString(KEY_WHISPER_MODEL, "base") ?: "base")
    val whisperModelName: StateFlow<String> = _whisperModelName.asStateFlow()

    private val _defaultTargetLanguage = MutableStateFlow(prefs.getString(KEY_TARGET_LANG, "es") ?: "es")
    val defaultTargetLanguage: StateFlow<String> = _defaultTargetLanguage.asStateFlow()

    private val _isCloudFallbackEnabled = MutableStateFlow(prefs.getBoolean(KEY_CLOUD_FALLBACK, false))
    val isCloudFallbackEnabled: StateFlow<Boolean> = _isCloudFallbackEnabled.asStateFlow()

    private val _cloudApiKey = MutableStateFlow(prefs.getString(KEY_CLOUD_API_KEY, "") ?: "")
    val cloudApiKey: StateFlow<String> = _cloudApiKey.asStateFlow()

    private val _customFolderUri = MutableStateFlow(prefs.getString(KEY_CUSTOM_FOLDER, null))
    val customFolderUri: StateFlow<String?> = _customFolderUri.asStateFlow()

    private fun loadRecordRule(): RecordRule {
        val name = prefs.getString(KEY_RECORD_RULE, RecordRule.ALL.name)
        return runCatching { RecordRule.valueOf(name!!) }.getOrDefault(RecordRule.ALL)
    }

    private fun loadAudioSource(): AudioSourceType {
        val name = prefs.getString(KEY_AUDIO_SOURCE, AudioSourceType.VOICE_RECOGNITION.name)
        return runCatching { AudioSourceType.valueOf(name!!) }.getOrDefault(AudioSourceType.VOICE_RECOGNITION)
    }

    fun setRecordRule(rule: RecordRule) {
        prefs.edit().putString(KEY_RECORD_RULE, rule.name).apply()
        _recordRule.value = rule
    }

    fun setSpeakerAutoOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SPEAKER_AUTO_ON, enabled).apply()
        _isSpeakerAutoOn.value = enabled
    }

    fun setAudioSource(source: AudioSourceType) {
        prefs.edit().putString(KEY_AUDIO_SOURCE, source.name).apply()
        _audioSource.value = source
    }

    fun setPlayNoticeTone(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTICE_TONE, enabled).apply()
        _playNoticeTone.value = enabled
    }

    fun setAutoDeleteDays(days: Int) {
        prefs.edit().putInt(KEY_AUTO_DELETE_DAYS, days).apply()
        _autoDeleteDays.value = days
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
        _isAppLockEnabled.value = enabled
    }

    fun setAppPin(pin: String?) {
        prefs.edit().putString(KEY_APP_PIN, pin).apply()
    }

    fun getAppPin(): String? = prefs.getString(KEY_APP_PIN, null)

    fun setSelectedContacts(contacts: Set<String>) {
        prefs.edit().putStringSet(KEY_SELECTED_CONTACTS, contacts).apply()
        _selectedContacts.value = contacts
    }

    fun setHasAcceptedConsent(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_CONSENT, accepted).apply()
        _hasAcceptedConsent.value = accepted
    }

    fun setWhisperModelName(modelName: String) {
        prefs.edit().putString(KEY_WHISPER_MODEL, modelName).apply()
        _whisperModelName.value = modelName
    }

    fun setDefaultTargetLanguage(lang: String) {
        prefs.edit().putString(KEY_TARGET_LANG, lang).apply()
        _defaultTargetLanguage.value = lang
    }

    fun setCloudFallbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLOUD_FALLBACK, enabled).apply()
        _isCloudFallbackEnabled.value = enabled
    }

    fun setCloudApiKey(key: String) {
        prefs.edit().putString(KEY_CLOUD_API_KEY, key).apply()
        _cloudApiKey.value = key
    }

    fun setCustomFolderUri(uri: String?) {
        prefs.edit().putString(KEY_CUSTOM_FOLDER, uri).apply()
        _customFolderUri.value = uri
    }

    companion object {
        private const val KEY_RECORD_RULE = "pref_record_rule"
        private const val KEY_SPEAKER_AUTO_ON = "pref_speaker_auto_on"
        private const val KEY_AUDIO_SOURCE = "pref_audio_source"
        private const val KEY_NOTICE_TONE = "pref_notice_tone"
        private const val KEY_AUTO_DELETE_DAYS = "pref_auto_delete_days"
        private const val KEY_APP_LOCK = "pref_app_lock"
        private const val KEY_APP_PIN = "pref_app_pin"
        private const val KEY_SELECTED_CONTACTS = "pref_selected_contacts"
        private const val KEY_CONSENT = "pref_consent"
        private const val KEY_WHISPER_MODEL = "pref_whisper_model"
        private const val KEY_TARGET_LANG = "pref_target_lang"
        private const val KEY_CLOUD_FALLBACK = "pref_cloud_fallback"
        private const val KEY_CLOUD_API_KEY = "pref_cloud_api_key"
        private const val KEY_CUSTOM_FOLDER = "pref_custom_folder"
    }
}
