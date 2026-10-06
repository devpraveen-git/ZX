package com.example.callscribe.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.callscribe.data.model.Speaker
import com.example.callscribe.data.model.TranscriptSegment
import com.example.callscribe.subtitle.SrtGenerator
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailScreen(
    recordingId: Long,
    viewModel: CallDetailViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val recording by viewModel.recording.collectAsState()
    val segments by viewModel.segments.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val showTranslation by viewModel.showTranslation.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()

    var notesText by remember { mutableStateOf("") }
    var selectedTargetLang by remember { mutableStateOf("es") }
    var showLangMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(recordingId) {
        viewModel.loadRecording(recordingId)
    }

    LaunchedEffect(recording?.notes) {
        notesText = recording?.notes ?: ""
    }

    // Auto-scroll to active subtitle during playback
    val activeSegmentIndex = remember(currentPositionMs, segments) {
        segments.indexOfFirst { it.startTimeMs <= currentPositionMs && currentPositionMs <= it.endTimeMs }
    }

    LaunchedEffect(activeSegmentIndex) {
        if (activeSegmentIndex >= 0 && isPlaying) {
            listState.animateScrollToItem(activeSegmentIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(recording?.contactName ?: recording?.phoneNumber ?: "Call Detail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(recording?.phoneNumber ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleBookmark() }) {
                        Icon(
                            imageVector = if (recording?.isBookmarked == true) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (recording?.isBookmarked == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = {
                        val srt = viewModel.getExportSrt()
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Call Transcript - ${recording?.contactName ?: recording?.phoneNumber}")
                            putExtra(Intent.EXTRA_TEXT, srt)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Export Transcript (SRT)"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Player Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val durationMs = recording?.durationMs ?: 1L
                    val currentSec = currentPositionMs / 1000
                    val totalSec = durationMs / 1000

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", currentSec / 60, currentSec % 60),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Slider(
                        value = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
                        onValueChange = { frac -> viewModel.seekTo((frac * durationMs).toLong()) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.seekBackward(5000) }) {
                            Icon(Icons.Default.FastRewind, contentDescription = "Rewind 5s")
                        }

                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.seekForward(5000) }) {
                            Icon(Icons.Default.FastForward, contentDescription = "Forward 5s")
                        }

                        // Speed toggle button
                        FilledTonalButton(
                            onClick = {
                                val nextSpeed = when (playbackSpeed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    else -> 1.0f
                                }
                                viewModel.setSpeed(nextSpeed)
                            }
                        ) {
                            Text("${playbackSpeed}x")
                        }
                    }
                }
            }

            // Translation controls bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Translate:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box {
                            Button(onClick = { showLangMenu = true }) {
                                Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(selectedTargetLang.uppercase())
                            }
                            DropdownMenu(
                                expanded = showLangMenu,
                                onDismissRequest = { showLangMenu = false }
                            ) {
                                val languages = listOf("es" to "Spanish", "fr" to "French", "de" to "German", "hi" to "Hindi", "ar" to "Arabic", "zh" to "Chinese", "ja" to "Japanese")
                                languages.forEach { (code, name) ->
                                    DropdownMenuItem(
                                        text = { Text("$name ($code)") },
                                        onClick = {
                                            selectedTargetLang = code
                                            showLangMenu = false
                                            viewModel.translate(code)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dual Subtitles", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = showTranslation,
                            onCheckedChange = { viewModel.setShowTranslation(it) }
                        )
                    }
                }
            }

            if (isTranslating) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Translating audio subtitles...", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle List
            Text(
                text = "Synced Subtitles (Tap line to jump audio)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(segments) { index, segment ->
                    val isActive = index == activeSegmentIndex
                    SubtitleSegmentItem(
                        segment = segment,
                        isActive = isActive,
                        showTranslation = showTranslation,
                        onClick = { viewModel.seekTo(segment.startTimeMs) }
                    )
                }
            }

            // Notes Editor
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Call Notes & Action Items") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { viewModel.saveNotes(notesText) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Notes")
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleSegmentItem(
    segment: TranscriptSegment,
    isActive: Boolean,
    showTranslation: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val speakerLabel = if (segment.speaker == Speaker.YOU) "You" else "Other"
                val speakerColor = if (segment.speaker == Speaker.YOU) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

                Text(
                    text = speakerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = speakerColor
                )
                Text(
                    text = "${SrtGenerator.formatTimestampMinutesSeconds(segment.startTimeMs)} - ${SrtGenerator.formatTimestampMinutesSeconds(segment.endTimeMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = segment.originalText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            )

            if (showTranslation && !segment.translatedText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = segment.translatedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
