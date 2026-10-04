package com.example.callscribe.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.callscribe.data.model.CallRecording
import com.example.callscribe.data.model.CallType

@Entity(tableName = "call_recordings")
data class CallRecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String?,
    val callType: String,
    val timestamp: Long,
    val durationMs: Long,
    val audioFilePath: String,
    val fileSize: Long,
    val isTranscribed: Boolean = false,
    val isTranslated: Boolean = false,
    val transcriptionText: String? = null,
    val translatedText: String? = null,
    val sourceLanguage: String = "en",
    val targetLanguage: String? = null,
    val notes: String = "",
    val isBookmarked: Boolean = false
) {
    fun toDomain(): CallRecording {
        return CallRecording(
            id = id,
            phoneNumber = phoneNumber,
            contactName = contactName,
            callType = runCatching { CallType.valueOf(callType) }.getOrDefault(CallType.INCOMING),
            timestamp = timestamp,
            durationMs = durationMs,
            audioFilePath = audioFilePath,
            fileSize = fileSize,
            isTranscribed = isTranscribed,
            isTranslated = isTranslated,
            transcriptionText = transcriptionText,
            translatedText = translatedText,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            notes = notes,
            isBookmarked = isBookmarked
        )
    }

    companion object {
        fun fromDomain(model: CallRecording): CallRecordingEntity {
            return CallRecordingEntity(
                id = model.id,
                phoneNumber = model.phoneNumber,
                contactName = model.contactName,
                callType = model.callType.name,
                timestamp = model.timestamp,
                durationMs = model.durationMs,
                audioFilePath = model.audioFilePath,
                fileSize = model.fileSize,
                isTranscribed = model.isTranscribed,
                isTranslated = model.isTranslated,
                transcriptionText = model.transcriptionText,
                translatedText = model.translatedText,
                sourceLanguage = model.sourceLanguage,
                targetLanguage = model.targetLanguage,
                notes = model.notes,
                isBookmarked = model.isBookmarked
            )
        }
    }
}
