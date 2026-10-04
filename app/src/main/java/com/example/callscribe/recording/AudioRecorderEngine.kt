package com.example.callscribe.recording

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import com.example.callscribe.data.model.AudioSourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorderEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var isRecording = false
    private var startTimeMs: Long = 0
    private var previousSpeakerState: Boolean = false
    private var previousAudioMode: Int = AudioManager.MODE_NORMAL

    val recordingActive: Boolean
        get() = isRecording

    val currentFile: File?
        get() = currentOutputFile

    val durationMs: Long
        get() = if (isRecording) System.currentTimeMillis() - startTimeMs else 0

    val maxAmplitude: Int
        get() = runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    @Synchronized
    fun startRecording(
        sourceType: AudioSourceType,
        enableSpeakerAutoOn: Boolean,
        phoneNumber: String
    ): Result<File> {
        if (isRecording) {
            return Result.failure(IllegalStateException("Recorder is already recording"))
        }

        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (enableSpeakerAutoOn) {
                applySpeakerAutoOn(audioManager)
            }

            val recordingsDir = File(context.filesDir, "recordings").apply {
                if (!exists()) mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val sanitizedNumber = phoneNumber.replace(Regex("[^0-9+]"), "_")
            val fileName = "CALL_${sanitizedNumber}_${timeStamp}.m4a"
            val file = File(recordingsDir, fileName)
            currentOutputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            val audioSource = when (sourceType) {
                AudioSourceType.VOICE_RECOGNITION -> MediaRecorder.AudioSource.VOICE_RECOGNITION
                AudioSourceType.MIC -> MediaRecorder.AudioSource.MIC
            }

            recorder.apply {
                setAudioSource(audioSource)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000) // Optimal for Whisper speech models
                setAudioEncodingBitRate(64000)
                setAudioChannels(1) // Mono for speech
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            startTimeMs = System.currentTimeMillis()

            Result.success(file)
        } catch (e: Exception) {
            cleanupSpeakerState()
            releaseRecorder()
            Result.failure(e)
        }
    }

    @Synchronized
    fun stopRecording(): Result<File?> {
        if (!isRecording) {
            return Result.success(null)
        }

        val file = currentOutputFile
        return try {
            mediaRecorder?.apply {
                runCatching { stop() }
                release()
            }
            mediaRecorder = null
            isRecording = false
            cleanupSpeakerState()
            Result.success(file)
        } catch (e: Exception) {
            cleanupSpeakerState()
            releaseRecorder()
            Result.failure(e)
        }
    }

    private fun applySpeakerAutoOn(audioManager: AudioManager) {
        try {
            previousAudioMode = audioManager.mode
            previousSpeakerState = audioManager.isSpeakerphoneOn

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val speakerDevice = audioManager.availableCommunicationDevices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                }
                if (speakerDevice != null) {
                    audioManager.setCommunicationDevice(speakerDevice)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = true
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = true
            }
        } catch (_: Exception) {
            // Ignore OEM restriction if speaker toggle fails
        }
    }

    private fun cleanupSpeakerState() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            } else {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = previousSpeakerState
                @Suppress("DEPRECATION")
                audioManager.mode = previousAudioMode
            }
        } catch (_: Exception) {
            // Silently ignore cleanup failures
        }
    }

    private fun releaseRecorder() {
        runCatching {
            mediaRecorder?.release()
        }
        mediaRecorder = null
        isRecording = false
    }
}
