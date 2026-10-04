package com.example.callscribe.recording

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.callscribe.CallScribeApp
import com.example.callscribe.MainActivity
import com.example.callscribe.data.model.CallRecording
import com.example.callscribe.data.model.CallType
import com.example.callscribe.data.repository.RecordingRepository
import com.example.callscribe.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class CallRecordingService : Service() {

    @Inject
    lateinit var recorderEngine: AudioRecorderEngine

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var recordingRepository: RecordingRepository

    @Inject
    lateinit var contactLookupHelper: ContactLookupHelper

    @Inject
    lateinit var audioAlertHelper: AudioAlertHelper

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null
    private var currentPhoneNumber: String = "Unknown"
    private var isIncoming: Boolean = true
    private var callStartTimeMs: Long = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val number = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
                val incoming = intent.getBooleanExtra(EXTRA_IS_INCOMING, true)
                startRecordingSession(number, incoming)
            }
            ACTION_STOP_RECORDING -> {
                stopRecordingSession()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecordingSession(phoneNumber: String, incoming: Boolean) {
        currentPhoneNumber = phoneNumber
        isIncoming = incoming
        callStartTimeMs = System.currentTimeMillis()

        val contactName = contactLookupHelper.lookupContactName(phoneNumber)
        val initialNotification = buildNotification(contactName ?: phoneNumber, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        // Play brief notice beep if legal requirement setting is active
        if (settingsRepository.playNoticeTone.value) {
            audioAlertHelper.playRecordingNoticeBeep()
        }

        // Start recording audio
        val audioSource = settingsRepository.audioSource.value
        val speakerAutoOn = settingsRepository.isSpeakerAutoOn.value
        val result = recorderEngine.startRecording(
            sourceType = audioSource,
            enableSpeakerAutoOn = speakerAutoOn,
            phoneNumber = phoneNumber
        )

        if (result.isFailure) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        startNotificationTicker(contactName ?: phoneNumber)
    }

    private fun startNotificationTicker(displayName: String) {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive && recorderEngine.recordingActive) {
                delay(1000)
                val durationSec = recorderEngine.durationMs / 1000
                val notification = buildNotification(displayName, durationSec)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun stopRecordingSession() {
        tickerJob?.cancel()
        val durationMs = recorderEngine.durationMs
        val stopResult = recorderEngine.stopRecording()
        val recordedFile = stopResult.getOrNull()

        serviceScope.launch(Dispatchers.IO) {
            if (recordedFile != null && recordedFile.exists() && recordedFile.length() > 0) {
                val contactName = contactLookupHelper.lookupContactName(currentPhoneNumber)
                val callRecording = CallRecording(
                    phoneNumber = currentPhoneNumber,
                    contactName = contactName,
                    callType = if (isIncoming) CallType.INCOMING else CallType.OUTGOING,
                    timestamp = callStartTimeMs,
                    durationMs = durationMs,
                    audioFilePath = recordedFile.absolutePath,
                    fileSize = recordedFile.length(),
                    sourceLanguage = "en"
                )
                recordingRepository.saveRecording(callRecording)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildNotification(displayName: String, durationSeconds: Long): Notification {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)

        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, CallRecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CallScribeApp.CHANNEL_RECORDING_ID)
            .setContentTitle("Recording Call: $displayName")
            .setContentText("Recording in progress • $timeString")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        recorderEngine.stopRecording()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_RECORDING = "com.example.callscribe.action.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.example.callscribe.action.STOP_RECORDING"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_IS_INCOMING = "extra_is_incoming"

        fun start(context: Context, phoneNumber: String, isIncoming: Boolean) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_START_RECORDING
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(EXTRA_IS_INCOMING, isIncoming)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }
    }
}
