package com.example.callscribe.recording

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.callscribe.data.model.CallRecording
import com.example.callscribe.data.model.CallType
import com.example.callscribe.data.repository.RecordingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExternalRecordingImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingRepository: RecordingRepository,
    private val contactLookupHelper: ContactLookupHelper,
    private val transcriptionCoordinator: javax.inject.Provider<com.example.callscribe.transcription.TranscriptionCoordinator>
) {
    suspend fun importFromUri(uri: Uri): Result<CallRecording> = withContext(Dispatchers.IO) {
        try {
            val fileName = queryFileName(uri) ?: "imported_call_${System.currentTimeMillis()}.m4a"
            val targetDir = File(context.filesDir, "recordings").apply {
                if (!exists()) mkdirs()
            }
            val destinationFile = File(targetDir, "IMPORTED_${System.currentTimeMillis()}_$fileName")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not open input stream for URI"))

            val (extractedNumber, extractedName) = parsePhoneAndNameFromFilename(fileName)
            val durationMs = extractDuration(destinationFile)

            val resolvedName = extractedName ?: contactLookupHelper.lookupContactName(extractedNumber)

            val recording = CallRecording(
                phoneNumber = extractedNumber,
                contactName = resolvedName,
                callType = CallType.IMPORTED,
                timestamp = destinationFile.lastModified(),
                durationMs = durationMs,
                audioFilePath = destinationFile.absolutePath,
                fileSize = destinationFile.length(),
                sourceLanguage = "en"
            )

            val id = recordingRepository.saveRecording(recording)
            transcriptionCoordinator.get().processRecordingAsync(id)
            Result.success(recording.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scanDirectory(treeUri: Uri): List<CallRecording> = withContext(Dispatchers.IO) {
        val importedList = mutableListOf<CallRecording>()
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val files = root.listFiles()

        for (doc in files) {
            if (doc.isFile && isAudioFile(doc.name)) {
                val result = importFromUri(doc.uri)
                result.getOrNull()?.let { importedList.add(it) }
            }
        }
        importedList
    }

    private fun isAudioFile(name: String?): Boolean {
        if (name == null) return false
        val lower = name.lowercase()
        return lower.endsWith(".m4a") || lower.endsWith(".mp3") ||
                lower.endsWith(".aac") || lower.endsWith(".wav") ||
                lower.endsWith(".amr") || lower.endsWith(".3gp")
    }

    private fun queryFileName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) it.getString(index) else null
            } else null
        }
    }

    private fun extractDuration(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    fun parsePhoneAndNameFromFilename(filename: String): Pair<String, String?> {
        // Many dialers name recordings as: "Call recording ContactName_1234567890_date.m4a" or "Call_1234567890_date.m4a"
        val numberRegex = Regex("([+]?[0-9]{7,15})")
        val match = numberRegex.find(filename)
        val extractedNumber = match?.value ?: "Unknown"

        val cleaned = filename.substringBeforeLast(".")
            .replace("Call recording", "", ignoreCase = true)
            .replace("Call", "", ignoreCase = true)
            .replace("Record", "", ignoreCase = true)
            .replace(extractedNumber, "")
            .replace(Regex("[_\\-]"), " ")
            .trim()

        val extractedName = if (cleaned.length in 2..30 && !cleaned.all { it.isDigit() }) cleaned else null
        return Pair(extractedNumber, extractedName)
    }
}
