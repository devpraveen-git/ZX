package com.example.callscribe.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.callscribe.data.model.Speaker
import com.example.callscribe.data.model.TranscriptSegment

@Entity(
    tableName = "transcript_segments",
    foreignKeys = [
        ForeignKey(
            entity = CallRecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recordingId"])]
)
data class TranscriptSegmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordingId: Long,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val speaker: String,
    val originalText: String,
    val translatedText: String? = null,
    val confidence: Float = 1.0f
) {
    fun toDomain(): TranscriptSegment {
        return TranscriptSegment(
            id = id,
            recordingId = recordingId,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            speaker = runCatching { Speaker.valueOf(speaker) }.getOrDefault(Speaker.UNKNOWN),
            originalText = originalText,
            translatedText = translatedText,
            confidence = confidence
        )
    }

    companion object {
        fun fromDomain(model: TranscriptSegment): TranscriptSegmentEntity {
            return TranscriptSegmentEntity(
                id = model.id,
                recordingId = model.recordingId,
                startTimeMs = model.startTimeMs,
                endTimeMs = model.endTimeMs,
                speaker = model.speaker.name,
                originalText = model.originalText,
                translatedText = model.translatedText,
                confidence = model.confidence
            )
        }
    }
}
