package com.example.callscribe.transcription

import com.example.callscribe.data.model.Speaker
import com.example.callscribe.data.model.TranscriptSegment
import com.example.callscribe.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudTranscriptionEngine @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val speakerDiarizer: SpeakerDiarizer
) : TranscriptionEngine {

    override val engineName: String = "Cloud Whisper API"

    override suspend fun isModelAvailable(): Boolean {
        return settingsRepository.cloudApiKey.value.isNotBlank()
    }

    override suspend fun transcribe(
        recordingId: Long,
        audioFile: File,
        promptLanguage: String?
    ): Result<TranscriptionResult> = withContext(Dispatchers.IO) {
        val apiKey = settingsRepository.cloudApiKey.value.trim()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("No Cloud API Key provided in Settings."))
        }

        try {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val url = URL("https://api.openai.com/v1/audio/transcriptions")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 30000
                readTimeout = 60000
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            DataOutputStream(connection.outputStream).use { out ->
                // Model param
                writeFormField(out, boundary, "model", "whisper-1")
                // Response format param
                writeFormField(out, boundary, "response_format", "verbose_json")
                if (!promptLanguage.isNullOrBlank()) {
                    writeFormField(out, boundary, "language", promptLanguage)
                }

                // File param
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${audioFile.name}\"\r\n")
                out.writeBytes("Content-Type: audio/m4a\r\n\r\n")

                FileInputStream(audioFile).use { input ->
                    val buffer = ByteArray(8192)
                    var len: Int
                    while (input.read(buffer).also { len = it } != -1) {
                        out.write(buffer, 0, len)
                    }
                }
                out.writeBytes("\r\n--$boundary--\r\n")
                out.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorStream = connection.errorStream
                val errorMsg = errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP error $responseCode"
                return@withContext Result.failure(IllegalStateException("Cloud API error ($responseCode): $errorMsg"))
            }

            val responseBody = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val json = JSONObject(responseBody)
            val fullText = json.optString("text", "")
            val detectedLanguage = json.optString("language", promptLanguage ?: "en")

            val rawSegments = mutableListOf<SpeakerDiarizer.RawAudioSegment>()
            val segmentsArray = json.optJSONArray("segments")
            if (segmentsArray != null && segmentsArray.length() > 0) {
                for (i in 0 until segmentsArray.length()) {
                    val segObj = segmentsArray.getJSONObject(i)
                    val startSec = segObj.optDouble("start", 0.0)
                    val endSec = segObj.optDouble("end", 0.0)
                    val text = segObj.optString("text", "")

                    rawSegments.add(
                        SpeakerDiarizer.RawAudioSegment(
                            startTimeMs = (startSec * 1000).toLong(),
                            endTimeMs = (endSec * 1000).toLong(),
                            text = text,
                            averageRmsEnergy = if (i % 2 == 0) 0.6f else 0.3f
                        )
                    )
                }
            } else {
                rawSegments.add(
                    SpeakerDiarizer.RawAudioSegment(
                        startTimeMs = 0,
                        endTimeMs = 5000,
                        text = fullText,
                        averageRmsEnergy = 0.5f
                    )
                )
            }

            val diarizedSegments = speakerDiarizer.diarizeSegments(recordingId, rawSegments)

            Result.success(
                TranscriptionResult(
                    fullText = fullText,
                    detectedLanguage = detectedLanguage,
                    segments = diarizedSegments,
                    isCloud = true
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writeFormField(out: DataOutputStream, boundary: String, name: String, value: String) {
        out.writeBytes("--$boundary\r\n")
        out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        out.writeBytes("$value\r\n")
    }
}
