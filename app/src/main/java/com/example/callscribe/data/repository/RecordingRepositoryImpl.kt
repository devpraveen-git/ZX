package com.example.callscribe.data.repository

import com.example.callscribe.data.local.CallRecordingDao
import com.example.callscribe.data.local.CallRecordingEntity
import com.example.callscribe.data.local.TranscriptDao
import com.example.callscribe.data.local.TranscriptSegmentEntity
import com.example.callscribe.data.model.CallRecording
import com.example.callscribe.data.model.TranscriptSegment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepositoryImpl @Inject constructor(
    private val callRecordingDao: CallRecordingDao,
    private val transcriptDao: TranscriptDao
) : RecordingRepository {

    override fun getAllRecordings(): Flow<List<CallRecording>> {
        return callRecordingDao.getAllRecordings().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getBookmarkedRecordings(): Flow<List<CallRecording>> {
        return callRecordingDao.getBookmarkedRecordings().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getRecordingById(id: Long): Flow<CallRecording?> {
        return callRecordingDao.getRecordingById(id).map { it?.toDomain() }
    }

    override suspend fun getRecordingByIdSync(id: Long): CallRecording? {
        return callRecordingDao.getRecordingByIdSync(id)?.toDomain()
    }

    override suspend fun saveRecording(recording: CallRecording): Long {
        val entity = CallRecordingEntity.fromDomain(recording)
        return callRecordingDao.insertRecording(entity)
    }

    override suspend fun updateRecording(recording: CallRecording) {
        val entity = CallRecordingEntity.fromDomain(recording)
        callRecordingDao.updateRecording(entity)
    }

    override suspend fun deleteRecording(id: Long) {
        val recording = callRecordingDao.getRecordingByIdSync(id)
        if (recording != null) {
            runCatching {
                val file = File(recording.audioFilePath)
                if (file.exists()) file.delete()
            }
            callRecordingDao.deleteRecordingById(id)
        }
    }

    override suspend fun deleteAllRecordings() {
        val all = callRecordingDao.getAllRecordings()
        // delete database records
        callRecordingDao.deleteAllRecordings()
    }

    override fun searchRecordings(query: String): Flow<List<CallRecording>> {
        val formattedQuery = if (query.trim().isEmpty()) "" else "*${query.trim()}*"
        return callRecordingDao.searchRecordings(formattedQuery).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTranscriptSegments(recordingId: Long): Flow<List<TranscriptSegment>> {
        return transcriptDao.getSegmentsForRecording(recordingId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveTranscriptSegments(recordingId: Long, segments: List<TranscriptSegment>) {
        transcriptDao.deleteSegmentsForRecording(recordingId)
        val entities = segments.map { TranscriptSegmentEntity.fromDomain(it) }
        transcriptDao.insertSegments(entities)
    }

    override suspend fun toggleBookmark(id: Long) {
        val entity = callRecordingDao.getRecordingByIdSync(id) ?: return
        val updated = entity.copy(isBookmarked = !entity.isBookmarked)
        callRecordingDao.updateRecording(updated)
    }

    override suspend fun updateNotes(id: Long, notes: String) {
        val entity = callRecordingDao.getRecordingByIdSync(id) ?: return
        val updated = entity.copy(notes = notes)
        callRecordingDao.updateRecording(updated)
    }

    override suspend fun deleteRecordingsOlderThanDays(days: Int): Int {
        if (days <= 0) return 0
        val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        return callRecordingDao.deleteRecordingsOlderThan(cutoff)
    }
}
