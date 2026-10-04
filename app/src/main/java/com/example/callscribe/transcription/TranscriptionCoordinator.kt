package com.example.callscribe.transcription

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.callscribe.CallScribeApp
import com.example.callscribe.MainActivity
import com.example.callscribe.data.repository.RecordingRepository
import com.example.callscribe.subtitle.SrtGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingRepository: RecordingRepository,
    private val transcriptionRouter: SpeechTranscriptionRouter
) {
    private val coordinatorScope = CoroutineScope(Dispatchers.IO)

    fun processRecordingAsync(recordingId: Long) {
        coordinatorScope.launch {
            processRecording(recordingId)
        }
    }

    suspend fun processRecording(recordingId: Long): Result<TranscriptionResult> = withContext(Dispatchers.IO) {
        val recording = recordingRepository.getRecordingByIdSync(recordingId)
            ?: return@withContext Result.failure(IllegalArgumentException("Recording $recordingId not found"))

        val audioFile = File(recording.audioFilePath)
        if (!audioFile.exists()) {
            return@withContext Result.failure(IllegalStateException("Audio file not found: ${recording.audioFilePath}"))
        }

        val result = transcriptionRouter.transcribe(
            recordingId = recordingId,
            audioFile = audioFile,
            promptLanguage = recording.sourceLanguage
        )

        result.onSuccess { transcription ->
            // 1. Save segments to Room
            recordingRepository.saveTranscriptSegments(recordingId, transcription.segments)

            // 2. Update parent recording entity (updates Room FTS index automatically)
            val updatedRecording = recording.copy(
                isTranscribed = true,
                transcriptionText = transcription.fullText,
                sourceLanguage = transcription.detectedLanguage
            )
            recordingRepository.updateRecording(updatedRecording)

            // 3. Export companion SRT & VTT files alongside audio
            runCatching {
                val srtFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.srt")
                srtFile.writeText(SrtGenerator.generateSrt(transcription.segments))

                val vttFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.vtt")
                vttFile.writeText(SrtGenerator.generateVtt(transcription.segments))
            }

            // 4. Notify user
            showTranscriptionCompletedNotification(recording.contactName ?: recording.phoneNumber, recordingId)
        }

        result
    }

    private fun showTranscriptionCompletedNotification(displayName: String, recordingId: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("recording_id", recordingId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            recordingId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CallScribeApp.CHANNEL_ALERTS_ID)
            .setContentTitle("Transcript Ready")
            .setContentText("Call with $displayName has been transcribed.")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((2000 + recordingId).toInt(), notification)
    }
}
