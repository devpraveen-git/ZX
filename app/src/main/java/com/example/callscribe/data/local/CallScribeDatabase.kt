package com.example.callscribe.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CallRecordingEntity::class,
        TranscriptSegmentEntity::class,
        RecordingFtsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CallScribeDatabase : RoomDatabase() {
    abstract fun callRecordingDao(): CallRecordingDao
    abstract fun transcriptDao(): TranscriptDao
}
