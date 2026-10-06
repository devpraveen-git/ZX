package com.example.callscribe.ui

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callscribe.data.model.CallRecording
import com.example.callscribe.data.model.TranscriptSegment
import com.example.callscribe.data.repository.RecordingRepository
import com.example.callscribe.subtitle.SrtGenerator
import com.example.callscribe.translation.TranslationCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CallDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingRepository: RecordingRepository,
    private val translationCoordinator: TranslationCoordinator
) : ViewModel() {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _recording = MutableStateFlow<CallRecording?>(null)
    val recording: StateFlow<CallRecording?> = _recording.asStateFlow()

    private val _segments = MutableStateFlow<List<TranscriptSegment>>(emptyList())
    val segments: StateFlow<List<TranscriptSegment>> = _segments.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _showTranslation = MutableStateFlow(false)
    val showTranslation: StateFlow<Boolean> = _showTranslation.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    fun loadRecording(recordingId: Long) {
        viewModelScope.launch {
            recordingRepository.getRecordingById(recordingId).collect { rec ->
                _recording.value = rec
                rec?.let {
                    initPlayer(it.audioFilePath)
                }
            }
        }
        viewModelScope.launch {
            recordingRepository.getTranscriptSegments(recordingId).collect { segs ->
                _segments.value = segs
            }
        }
    }

    private fun initPlayer(audioPath: String) {
        val file = File(audioPath)
        if (!file.exists()) return

        releasePlayer()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0
                }
            }
        } catch (_: Exception) {
            // Player initialization fallback
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            progressJob?.cancel()
        } else {
            player.start()
            _isPlaying.value = true
            startProgressTicker()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        val clamped = positionMs.coerceIn(0L, player.duration.toLong())
        player.seekTo(clamped.toInt())
        _currentPositionMs.value = clamped
    }

    fun seekForward(deltaMs: Long = 5000L) {
        val cur = _currentPositionMs.value
        seekTo(cur + deltaMs)
    }

    fun seekBackward(deltaMs: Long = 5000L) {
        val cur = _currentPositionMs.value
        seekTo(cur - deltaMs)
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        mediaPlayer?.let { player ->
            runCatching {
                player.playbackParams = player.playbackParams.setSpeed(speed)
            }
        }
    }

    fun setShowTranslation(show: Boolean) {
        _showTranslation.value = show
    }

    fun translate(targetLanguage: String) {
        val rec = _recording.value ?: return
        _isTranslating.value = true
        viewModelScope.launch {
            translationCoordinator.translateRecording(rec.id, targetLanguage)
            _showTranslation.value = true
            _isTranslating.value = false
        }
    }

    fun saveNotes(notes: String) {
        val rec = _recording.value ?: return
        viewModelScope.launch {
            recordingRepository.updateNotes(rec.id, notes)
        }
    }

    fun toggleBookmark() {
        val rec = _recording.value ?: return
        viewModelScope.launch {
            recordingRepository.toggleBookmark(rec.id)
        }
    }

    fun getExportSrt(): String {
        return SrtGenerator.generateSrt(_segments.value, useTranslated = _showTranslation.value)
    }

    fun getExportTxt(): String {
        return SrtGenerator.generatePlainText(_segments.value, useTranslated = _showTranslation.value)
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let {
                    _currentPositionMs.value = it.currentPosition.toLong()
                }
                delay(100)
            }
        }
    }

    private fun releasePlayer() {
        progressJob?.cancel()
        mediaPlayer?.runCatching {
            stop()
            release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPositionMs.value = 0
    }

    override fun onCleared() {
        releasePlayer()
        super.onCleared()
    }
}
