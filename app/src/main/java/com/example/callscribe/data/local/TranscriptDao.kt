package com.example.callscribe.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {

    @Query("SELECT * FROM transcript_segments WHERE recordingId = :recordingId ORDER BY startTimeMs ASC")
    fun getSegmentsForRecording(recordingId: Long): Flow<List<TranscriptSegmentEntity>>

    @Query("SELECT * FROM transcript_segments WHERE recordingId = :recordingId ORDER BY startTimeMs ASC")
    suspend fun getSegmentsForRecordingSync(recordingId: Long): List<TranscriptSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<TranscriptSegmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: TranscriptSegmentEntity): Long

    @Update
    suspend fun updateSegment(segment: TranscriptSegmentEntity)

    @Query("DELETE FROM transcript_segments WHERE recordingId = :recordingId")
    suspend fun deleteSegmentsForRecording(recordingId: Long)
}
