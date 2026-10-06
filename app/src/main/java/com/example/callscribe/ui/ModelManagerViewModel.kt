package com.example.callscribe.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callscribe.transcription.ModelManager
import com.example.callscribe.translation.MlKitTranslationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelManagerViewModel @Inject constructor(
    private val modelManager: ModelManager,
    private val translationEngine: MlKitTranslationEngine
) : ViewModel() {

    val whisperModels: StateFlow<List<ModelManager.AiModelInfo>> = modelManager.modelsState

    fun downloadWhisperModel(modelId: String) {
        viewModelScope.launch {
            modelManager.downloadModel(modelId)
        }
    }

    fun deleteWhisperModel(modelId: String) {
        modelManager.deleteModel(modelId)
    }

    fun downloadTranslationLanguage(langCode: String) {
        viewModelScope.launch {
            translationEngine.downloadLanguageModel(langCode)
        }
    }

    fun deleteTranslationLanguage(langCode: String) {
        viewModelScope.launch {
            translationEngine.deleteLanguageModel(langCode)
        }
    }
}
