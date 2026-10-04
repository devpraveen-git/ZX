package com.example.callscribe.data.repository

import com.example.callscribe.data.model.CallRecording
import com.example.callscribe.data.model.TranscriptSegment
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {
    fun getAllRecordings(): Flow<List<CallRecording>>
    fun getBookmarkedRecordings(): Flow<List<CallRecording>>
    fun getRecordingById(id: Long): Flow<CallRecording?>
    suspend fun getRecordingByIdSync(id: Long): CallRecording?
    suspend fun saveRecording(recording: CallRecording): Long
    suspend fun updateRecording(recording: CallRecording)
    suspend fun deleteRecording(id: Long)
    suspend fun deleteAllRecordings()
    fun searchRecordings(query: String): Flow<List<CallRecording>>
    fun getTranscriptSegments(recordingId: Long): Flow<List<TranscriptSegment>>
    suspend fun saveTranscriptSegments(recordingId: Long, segments: List<TranscriptSegment>)
    suspend fun toggleBookmark(id: Long)
    suspend fun updateNotes(id: Long, notes: String)
    suspend fun deleteRecordingsOlderThanDays(days: Int): Int
}
