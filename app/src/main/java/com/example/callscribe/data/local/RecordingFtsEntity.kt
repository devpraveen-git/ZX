package com.example.callscribe.data.local

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "recordings_fts")
@Fts4(contentEntity = CallRecordingEntity::class)
data class RecordingFtsEntity(
    val phoneNumber: String,
    val contactName: String?,
    val transcriptionText: String?,
    val notes: String
)
