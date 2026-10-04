package com.example.callscribe.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRecordingDao {

    @Query("SELECT * FROM call_recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<CallRecordingEntity>>

    @Query("SELECT * FROM call_recordings WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarkedRecordings(): Flow<List<CallRecordingEntity>>

    @Query("SELECT * FROM call_recordings WHERE id = :id")
    fun getRecordingById(id: Long): Flow<CallRecordingEntity?>

    @Query("SELECT * FROM call_recordings WHERE id = :id")
    suspend fun getRecordingByIdSync(id: Long): CallRecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: CallRecordingEntity): Long

    @Update
    suspend fun updateRecording(recording: CallRecordingEntity)

    @Delete
    suspend fun deleteRecording(recording: CallRecordingEntity)

    @Query("DELETE FROM call_recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Long)

    @Query("DELETE FROM call_recordings WHERE timestamp < :thresholdTimestamp")
    suspend fun deleteRecordingsOlderThan(thresholdTimestamp: Long): Int

    @Query("DELETE FROM call_recordings")
    suspend fun deleteAllRecordings()

    @Query("""
        SELECT call_recordings.* FROM call_recordings
        JOIN recordings_fts ON call_recordings.id = recordings_fts.rowid
        WHERE recordings_fts MATCH :query
        ORDER BY timestamp DESC
    """)
    fun searchRecordings(query: String): Flow<List<CallRecordingEntity>>
}
