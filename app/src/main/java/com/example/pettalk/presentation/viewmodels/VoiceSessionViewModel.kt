package com.example.pettalk.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.pettalk.data.PetType
import com.example.pettalk.data.agora.AgoraConfig
import com.example.pettalk.data.agora.ConvoAiAgentClient
import com.example.pettalk.data.agora.PetTalkVoiceClient
import com.example.pettalk.data.agora.VoiceEvent
import com.example.pettalk.data.model.ConversationMessage
import com.example.pettalk.data.model.SessionStatus
import com.example.pettalk.data.model.Speaker
import com.example.pettalk.navigation.Screens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoiceSessionUiState(
    val status: SessionStatus = SessionStatus.CONNECTING,
    val messages: List<ConversationMessage> = emptyList(),
    val micMuted: Boolean = false,
    val error: String? = null,
)

/**
 * Runs one voice session: joins the Agora channel, starts the Conversational AI agent
 * for the selected pet, and turns session events into the conversation transcript.
 */
@HiltViewModel
class VoiceSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val voiceClient: PetTalkVoiceClient,
    private val agentClient: ConvoAiAgentClient,
) : ViewModel() {

    val petType: PetType =
        PetType.fromName(savedStateHandle.toRoute<Screens.VoiceSessionScreen>().petTypeName)

    private val _uiState = MutableStateFlow(VoiceSessionUiState())
    val uiState: StateFlow<VoiceSessionUiState> = _uiState.asStateFlow()

    private var sessionStarted = false
    private var agentId: String? = null

    init {
        viewModelScope.launch {
            voiceClient.events.collect { event -> _uiState.update { applyEvent(it, event) } }
        }
    }

    /** Called once the microphone permission is granted. */
    fun startSession() {
        if (sessionStarted) return
        sessionStarted = true
        _uiState.update { it.copy(status = SessionStatus.CONNECTING) }
        viewModelScope.launch {
            try {
                // A fresh channel per session keeps agent UID collisions away.
                val channel = "demo-channel"
                val uid = 1001
                voiceClient.start(channel, uid)
                agentId = agentClient.startAgent(channel, uid, petType)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        status = SessionStatus.ERROR,
                        error = e.message ?: "Could not start the session"
                    )
                }
            }
        }
    }

    fun onMicPermissionDenied() {
        _uiState.update {
            it.copy(
                status = SessionStatus.ERROR,
                error = "Microphone permission is needed so your pet can hear you."
            )
        }
    }

    fun toggleMic() {
        val muted = !_uiState.value.micMuted
        voiceClient.setMicMuted(muted)
        _uiState.update { it.copy(micMuted = muted) }
    }

    /** Marks the session ended; actual teardown happens in onCleared so it survives navigation. */
    fun endSession() {
        _uiState.update { it.copy(status = SessionStatus.ENDED) }
    }

    override fun onCleared() {
        agentId?.let { agentClient.stopAgentBestEffort(it) }
        voiceClient.stop()
        super.onCleared()
    }

    private fun applyEvent(state: VoiceSessionUiState, event: VoiceEvent): VoiceSessionUiState =
        when (event) {
            is VoiceEvent.StatusChanged -> state.copy(status = event.status)

            is VoiceEvent.OwnerTranscript -> state.copy(
                messages = state.messages + ConversationMessage(
                    id = state.messages.size.toLong(),
                    speaker = Speaker.OWNER,
                    text = event.text,
                )
            )

            is VoiceEvent.AgentTranscript -> state.copy(
                messages = state.messages + ConversationMessage(
                    id = state.messages.size.toLong(),
                    speaker = Speaker.AI,
                    text = event.text,
                )
            )

            is VoiceEvent.Error -> state.copy(status = SessionStatus.ERROR, error = event.message)
        }
}
