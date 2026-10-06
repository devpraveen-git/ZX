package com.example.callscribe.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callscribe.data.model.CallRecording
import com.example.callscribe.data.repository.RecordingRepository
import com.example.callscribe.data.repository.SettingsRepository
import com.example.callscribe.recording.AudioRecorderEngine
import com.example.callscribe.recording.ExternalRecordingImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CallFilter {
    ALL,
    BOOKMARKED,
    TRANSCRIBED
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val settingsRepository: SettingsRepository,
    private val audioRecorderEngine: AudioRecorderEngine,
    private val externalRecordingImporter: ExternalRecordingImporter
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(CallFilter.ALL)
    val selectedFilter: StateFlow<CallFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val hasAcceptedConsent: StateFlow<Boolean> = settingsRepository.hasAcceptedConsent

    val isRecordingActive: Boolean
        get() = audioRecorderEngine.recordingActive

    val recordings: StateFlow<List<CallRecording>> = combine(
        recordingRepository.getAllRecordings(),
        _selectedFilter,
        _searchQuery
    ) { all, filter, query ->
        val filtered = when (filter) {
            CallFilter.ALL -> all
            CallFilter.BOOKMARKED -> all.filter { it.isBookmarked }
            CallFilter.TRANSCRIBED -> all.filter { it.isTranscribed }
        }
        if (query.isBlank()) {
            filtered
        } else {
            filtered.filter {
                (it.contactName?.contains(query, ignoreCase = true) == true) ||
                        it.phoneNumber.contains(query, ignoreCase = true) ||
                        (it.transcriptionText?.contains(query, ignoreCase = true) == true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(filter: CallFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleBookmark(recordingId: Long) {
        viewModelScope.launch {
            recordingRepository.toggleBookmark(recordingId)
        }
    }

    fun deleteRecording(recordingId: Long) {
        viewModelScope.launch {
            recordingRepository.deleteRecording(recordingId)
        }
    }

    fun importRecording(uri: Uri) {
        viewModelScope.launch {
            externalRecordingImporter.importFromUri(uri)
        }
    }
}
