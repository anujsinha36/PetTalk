package com.example.pettalk.data.model

/** Who a conversation bubble belongs to. */
enum class Speaker {
    /** The owner, from their live speech transcription. */
    OWNER,

    /** The AI, interpreting the pet or speaking to it in the pet's voice. */
    AI,
}

/** One bubble in the conversation transcript. */
data class ConversationMessage(
    val id: Long,
    val speaker: Speaker,
    val text: String,
)

/** High-level state of the voice session, shown as a status pill in the UI. */
enum class SessionStatus {
    /** Joining the channel / starting the agent. */
    CONNECTING,

    /** The agent has joined and is silent, waiting for sound. */
    IDLE,

    /** The agent is listening to the pet and the owner. */
    LISTENING,

    /** The agent is interpreting. */
    THINKING,

    /** The agent is talking. */
    SPEAKING,

    /** The session ended. */
    ENDED,

    /** Something failed. */
    ERROR,
}
