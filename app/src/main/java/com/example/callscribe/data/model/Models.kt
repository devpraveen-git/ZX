package com.example.callscribe.data.model

enum class CallType {
    INCOMING,
    OUTGOING,
    IMPORTED
}

enum class Speaker {
    YOU,
    OTHER,
    UNKNOWN
}

enum class RecordRule(val displayName: String, val description: String) {
    ALL("All Calls", "Automatically record every incoming and outgoing call"),
    UNKNOWN_ONLY("Unknown Numbers Only", "Only record calls from numbers not saved in contacts"),
    SELECTED_CONTACTS("Selected Contacts Only", "Only record calls matching chosen contacts"),
    MANUAL("Manual Only", "Do not auto-record; start recording manually via notification or app")
}

enum class AudioSourceType(val displayName: String) {
    VOICE_RECOGNITION("Voice Recognition (Optimized)"),
    MIC("Microphone (Standard)")
}

data class CallRecording(
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String?,
    val callType: CallType,
    val timestamp: Long,
    val durationMs: Long,
    val audioFilePath: String,
    val fileSize: Long,
    val isTranscribed: Boolean = false,
    val isTranslated: Boolean = false,
    val transcriptionText: String? = null,
    val translatedText: String? = null,
    val sourceLanguage: String = "en",
    val targetLanguage: String? = null,
    val notes: String = "",
    val isBookmarked: Boolean = false
)

data class TranscriptSegment(
    val id: Long = 0,
    val recordingId: Long,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val speaker: Speaker,
    val originalText: String,
    val translatedText: String? = null,
    val confidence: Float = 1.0f
)
