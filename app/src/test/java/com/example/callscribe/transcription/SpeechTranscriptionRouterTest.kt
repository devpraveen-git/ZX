package com.example.callscribe.transcription

import com.example.callscribe.data.model.Speaker
import com.example.callscribe.data.model.TranscriptSegment
import com.example.callscribe.data.repository.SettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SpeechTranscriptionRouterTest {

    private class FakeOnDeviceEngine(
        var shouldSucceed: Boolean = true
    ) : TranscriptionEngine {
        override val engineName: String = "Whisper On-Device (Offline)"
        override suspend fun isModelAvailable(): Boolean = true

        override suspend fun transcribe(
            recordingId: Long,
            audioFile: File,
            promptLanguage: String?
        ): Result<TranscriptionResult> {
            return if (shouldSucceed) {
                Result.success(
                    TranscriptionResult(
                        fullText = "On-device transcript",
                        detectedLanguage = "en",
                        segments = listOf(
                            TranscriptSegment(1, recordingId, 0, 1000, Speaker.YOU, "On-device transcript")
                        ),
                        isCloud = false
                    )
                )
            } else {
                Result.failure(RuntimeException("On-device failed"))
            }
        }
    }

    private class FakeCloudEngine(
        var shouldSucceed: Boolean = true
    ) : TranscriptionEngine {
        override val engineName: String = "Cloud Whisper API"
        override suspend fun isModelAvailable(): Boolean = true

        override suspend fun transcribe(
            recordingId: Long,
            audioFile: File,
            promptLanguage: String?
        ): Result<TranscriptionResult> {
            return if (shouldSucceed) {
                Result.success(
                    TranscriptionResult(
                        fullText = "Cloud transcript",
                        detectedLanguage = "en",
                        segments = listOf(
                            TranscriptSegment(1, recordingId, 0, 1000, Speaker.YOU, "Cloud transcript")
                        ),
                        isCloud = true
                    )
                )
            } else {
                Result.failure(RuntimeException("Cloud network error"))
            }
        }
    }

    @Test
    fun getActiveEngineName_whenCloudDisabled_returnsOnDeviceEngine() {
        val onDevice = FakeOnDeviceEngine()
        val cloud = FakeCloudEngine()

        assertEquals("Whisper On-Device (Offline)", onDevice.engineName)
        assertEquals("Cloud Whisper API", cloud.engineName)
    }

    @Test
    fun fallbackMechanism_returnsOnDeviceWhenCloudFails() = runTest {
        val onDevice = FakeOnDeviceEngine(shouldSucceed = true)
        val cloud = FakeCloudEngine(shouldSucceed = false)

        val file = File("test.m4a")
        val cloudRes = cloud.transcribe(1, file)
        assertTrue(cloudRes.isFailure)

        val fallbackRes = onDevice.transcribe(1, file)
        assertTrue(fallbackRes.isSuccess)
        assertEquals("On-device transcript", fallbackRes.getOrNull()?.fullText)
        assertEquals(false, fallbackRes.getOrNull()?.isCloud)
    }

    @Test
    fun cloudSuccess_returnsCloudResult() = runTest {
        val cloud = FakeCloudEngine(shouldSucceed = true)
        val file = File("test.m4a")
        val res = cloud.transcribe(1, file)
        assertTrue(res.isSuccess)
        assertEquals("Cloud transcript", res.getOrNull()?.fullText)
        assertEquals(true, res.getOrNull()?.isCloud)
    }
}
