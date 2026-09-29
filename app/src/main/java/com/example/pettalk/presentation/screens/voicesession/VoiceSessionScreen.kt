package com.example.pettalk.presentation.screens.voicesession

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pettalk.data.model.SessionStatus
import com.example.pettalk.data.model.Speaker
import com.example.pettalk.presentation.viewmodels.VoiceSessionViewModel

@Composable
fun VoiceSessionScreen(
    onBackClick: () -> Unit,
    viewModel: VoiceSessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Ask for the microphone, then start the session.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startSession() else viewModel.onMicPermissionDenied()
    }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.startSession() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    LaunchedEffect(uiState.status) {
        if (uiState.status == SessionStatus.ENDED) onBackClick()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "${viewModel.petType.emoji} ${viewModel.petType.displayName} Talk",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        StatusPill(status = uiState.status, error = uiState.error)

        // Transcript
        val listState = rememberLazyListState()
        LaunchedEffect(uiState.messages.size) {
            if (uiState.messages.isNotEmpty()) {
                listState.animateScrollToItem(uiState.messages.lastIndex)
            }
        }
        if (uiState.messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "Say hello to your pet.\n" +
                        "Your words will be delivered in ${viewModel.petType.displayName.lowercase()} voice.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp),
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    MessageBubble(
                        speaker = message.speaker,
                        petEmoji = viewModel.petType.emoji,
                        text = message.text,
                    )
                }
            }
        }

        // Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.size(72.dp))
            Surface(
                onClick = { viewModel.endSession() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.CallEnd,
                        contentDescription = "End session",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.size(72.dp))
            Surface(
                onClick = { viewModel.toggleMic() },
                shape = CircleShape,
                color = if (uiState.micMuted) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (uiState.micMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = if (uiState.micMuted) "Unmute" else "Mute",
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: SessionStatus, error: String?) {
    val label = when (status) {
        SessionStatus.CONNECTING -> "Connecting…"
        SessionStatus.IDLE -> "Waiting for a sound…"
        SessionStatus.LISTENING -> "Listening…"
        SessionStatus.THINKING -> "Thinking…"
        SessionStatus.SPEAKING -> "Speaking…"
        SessionStatus.ENDED -> "Ended"
        SessionStatus.ERROR -> error ?: "Something went wrong"
    }
    Badge(
        containerColor = if (status == SessionStatus.ERROR) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(vertical = 12.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun MessageBubble(
    speaker: Speaker,
    petEmoji: String,
    text: String,
) {
    val isOwner = speaker == Speaker.OWNER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwner) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isOwner) 18.dp else 4.dp,
                bottomEnd = if (isOwner) 4.dp else 18.dp,
            ),
            color = if (isOwner) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(
                text = if (isOwner) text else "$petEmoji $text",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
