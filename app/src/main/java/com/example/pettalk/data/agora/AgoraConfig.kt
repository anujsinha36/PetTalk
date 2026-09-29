package com.example.pettalk.data.agora

/**
 * Credentials and settings for Agora and the Conversational AI agent.
 * Values come from BuildConfig, which is fed from local.properties.
 */
data class AgoraConfig(
    val appId: String,
    val rtcToken: String,
    val rtmToken: String,
    val customerKey: String,
    val customerSecret: String,
    /** Comma-separated ConvoAI presets. When empty, the explicit LLM settings below are used. */
    val preset: String,
    val llmUrl: String,
    val llmModel: String,
    val llmApiKey: String,
)
