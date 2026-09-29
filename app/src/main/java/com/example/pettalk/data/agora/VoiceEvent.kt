package com.example.pettalk.data.agora

import com.example.pettalk.data.model.SessionStatus

/** Events the Agora layer emits while a voice session runs. */
sealed interface VoiceEvent {
    /** Agent presence changed (silent, listening, thinking, speaking). */
    data class StatusChanged(val status: SessionStatus) : VoiceEvent

    /** The owner said something (final speech transcription). */
    data class OwnerTranscript(val text: String) : VoiceEvent

    /** The agent finished a reply (final transcription). */
    data class AgentTranscript(val text: String) : VoiceEvent

    /** Something failed during the session. */
    data class Error(val message: String) : VoiceEvent
}
