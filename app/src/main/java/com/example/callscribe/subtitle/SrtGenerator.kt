package com.example.callscribe.subtitle

import com.example.callscribe.data.model.Speaker
import com.example.callscribe.data.model.TranscriptSegment
import java.util.Locale

object SrtGenerator {

    fun generateSrt(segments: List<TranscriptSegment>, useTranslated: Boolean = false): String {
        val sb = StringBuilder()
        segments.sortedBy { it.startTimeMs }.forEachIndexed { index, segment ->
            sb.append("${index + 1}\n")
            sb.append("${formatTimestampSrt(segment.startTimeMs)} --> ${formatTimestampSrt(segment.endTimeMs)}\n")
            val speakerPrefix = when (segment.speaker) {
                Speaker.YOU -> "[You]: "
                Speaker.OTHER -> "[Other]: "
                Speaker.UNKNOWN -> ""
            }
            val text = if (useTranslated && !segment.translatedText.isNullOrBlank()) {
                segment.translatedText
            } else {
                segment.originalText
            }
            sb.append("$speakerPrefix$text\n\n")
        }
        return sb.toString().trimEnd() + "\n"
    }

    fun generateVtt(segments: List<TranscriptSegment>, useTranslated: Boolean = false): String {
        val sb = StringBuilder()
        sb.append("WEBVTT\n\n")
        segments.sortedBy { it.startTimeMs }.forEach { segment ->
            sb.append("${formatTimestampVtt(segment.startTimeMs)} --> ${formatTimestampVtt(segment.endTimeMs)}\n")
            val speakerTag = when (segment.speaker) {
                Speaker.YOU -> "<v You>"
                Speaker.OTHER -> "<v Other>"
                Speaker.UNKNOWN -> ""
            }
            val closeTag = if (speakerTag.isNotEmpty()) "</v>" else ""
            val text = if (useTranslated && !segment.translatedText.isNullOrBlank()) {
                segment.translatedText
            } else {
                segment.originalText
            }
            sb.append("$speakerTag$text$closeTag\n\n")
        }
        return sb.toString().trimEnd() + "\n"
    }

    fun generatePlainText(segments: List<TranscriptSegment>, useTranslated: Boolean = false): String {
        val sb = StringBuilder()
        segments.sortedBy { it.startTimeMs }.forEach { segment ->
            val speaker = when (segment.speaker) {
                Speaker.YOU -> "You"
                Speaker.OTHER -> "Other"
                Speaker.UNKNOWN -> "Speaker"
            }
            val time = formatTimestampMinutesSeconds(segment.startTimeMs)
            val text = if (useTranslated && !segment.translatedText.isNullOrBlank()) {
                segment.translatedText
            } else {
                segment.originalText
            }
            sb.append("[$time] $speaker: $text\n")
        }
        return sb.toString()
    }

    fun formatTimestampSrt(millis: Long): String {
        val totalSeconds = millis / 1000
        val ms = millis % 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, ms)
    }

    fun formatTimestampVtt(millis: Long): String {
        val totalSeconds = millis / 1000
        val ms = millis % 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, ms)
    }

    fun formatTimestampMinutesSeconds(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun parseSrt(srtContent: String, recordingId: Long = 0): List<TranscriptSegment> {
        val segments = mutableListOf<TranscriptSegment>()
        val blocks = srtContent.trim().split(Regex("\n\\s*\n"))
        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size >= 3) {
                // line 0: index
                // line 1: timestamp range
                val timeLine = lines[1]
                val timeParts = timeLine.split("-->").map { it.trim() }
                if (timeParts.size == 2) {
                    val startMs = parseSrtTimestampToMillis(timeParts[0])
                    val endMs = parseSrtTimestampToMillis(timeParts[1])
                    val contentLines = lines.drop(2).joinToString(" ")
                    var speaker = Speaker.UNKNOWN
                    var cleanText = contentLines
                    if (contentLines.startsWith("[You]:", ignoreCase = true)) {
                        speaker = Speaker.YOU
                        cleanText = contentLines.substringAfter(":").trim()
                    } else if (contentLines.startsWith("[Other]:", ignoreCase = true)) {
                        speaker = Speaker.OTHER
                        cleanText = contentLines.substringAfter(":").trim()
                    }

                    segments.add(
                        TranscriptSegment(
                            recordingId = recordingId,
                            startTimeMs = startMs,
                            endTimeMs = endMs,
                            speaker = speaker,
                            originalText = cleanText
                        )
                    )
                }
            }
        }
        return segments
    }

    private fun parseSrtTimestampToMillis(timestamp: String): Long {
        return try {
            val parts = timestamp.replace(".", ",").split(":")
            val hours = parts[0].toLong()
            val minutes = parts[1].toLong()
            val secParts = parts[2].split(",")
            val seconds = secParts[0].toLong()
            val millis = secParts.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLong() ?: 0L
            (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
        } catch (_: Exception) {
            0L
        }
    }
}
