package com.example.callscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.callscribe.data.model.AudioSourceType
import com.example.callscribe.data.model.RecordRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val recordRule by viewModel.recordRule.collectAsState()
    val isSpeakerAutoOn by viewModel.isSpeakerAutoOn.collectAsState()
    val audioSource by viewModel.audioSource.collectAsState()
    val playNoticeTone by viewModel.playNoticeTone.collectAsState()
    val autoDeleteDays by viewModel.autoDeleteDays.collectAsState()
    val isCloudFallbackEnabled by viewModel.isCloudFallbackEnabled.collectAsState()
    val cloudApiKey by viewModel.cloudApiKey.collectAsState()

    var showSpeakerDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(cloudApiKey) }

    if (showSpeakerDialog) {
        AlertDialog(
            onDismissRequest = { showSpeakerDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text("Why Speaker Mode Auto-On?") },
            text = {
                Text(
                    "Starting with Android 9 (Pie) through Android 15/16, Google blocked third-party call recording access to internal call audio (VOICE_CALL). Non-system apps can only capture sound via the microphone.\n\n" +
                            "By automatically enabling the speakerphone during a recorded call, the microphone captures both your voice and the other caller's voice clearly without requiring root access or Accessibility Service exploits."
                )
            },
            confirmButton = {
                TextButton(onClick = { showSpeakerDialog = false }) { Text("Got it") }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Everything?") },
            text = { Text("This will permanently remove all audio recordings, transcripts, and subtitles from this device. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllRecordings()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Auto-Record Rules
            Text("Auto-Recording Strategy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    RecordRule.values().forEach { rule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = recordRule == rule,
                                onClick = { viewModel.setRecordRule(rule) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(rule.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(rule.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }

            // Audio & Speaker Settings
            Text("Audio Capture Strategy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Speaker Mode Auto-On", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { showSpeakerDialog = true }) {
                                Icon(Icons.Default.Info, contentDescription = "Info", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Switch(
                            checked = isSpeakerAutoOn,
                            onCheckedChange = { viewModel.setSpeakerAutoOn(it) }
                        )
                    }
                    Text(
                        "Auto-enables loudspeaker during recorded calls so the microphone captures both speakers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Audio Source:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    AudioSourceType.values().forEach { src ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = audioSource == src,
                                onClick = { viewModel.setAudioSource(src) }
                            )
                            Text(src.displayName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // OEM Autostart Guidance
            Text("Device Autostart (${viewModel.detectedBrand.name})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(viewModel.getOemBrandInstructions(), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { viewModel.openOemAutostartSettings() }) {
                        Text("Open ${viewModel.detectedBrand.name} Autostart Settings")
                    }
                }
            }

            // Privacy & Legal Compliance
            Text("Privacy, Security & Legal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Legal Recording Notice Tone", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Plays a brief beep tone at the start of recorded calls for dual-consent compliance.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = playNoticeTone,
                            onCheckedChange = { viewModel.setPlayNoticeTone(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Delete Recordings", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(if (autoDeleteDays == 0) "Never" else "$autoDeleteDays days", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Never", 7 to "7 Days", 30 to "30 Days", 90 to "90 Days").forEach { (days, label) ->
                            OutlinedButton(
                                onClick = { viewModel.setAutoDeleteDays(days) }
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }

            // Cloud Mode (Optional)
            Text("Cloud Speech API (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Cloud Fallback", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = isCloudFallbackEnabled,
                            onCheckedChange = { viewModel.setCloudFallbackEnabled(it) }
                        )
                    }
                    if (isCloudFallbackEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                viewModel.setCloudApiKey(it)
                            },
                            label = { Text("OpenAI / Cloud API Key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Destructive Action: Delete Everything
            Button(
                onClick = { showDeleteConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Everything (One-Tap Wipe)")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
