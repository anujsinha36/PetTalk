package com.example.pettalk.data.agora

import android.util.Base64
import com.example.pettalk.data.PetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Starts and stops the Conversational AI agent through Agora's REST API.
 *
 * Docs: https://docs.agora.io/en/conversational-ai-engine/rest-api/join-agent
 *
 * The REST credentials are meant for local experimentation only. In a real product the
 * join/leave calls belong on a backend so the customer secret never ships in the app.
 */
class ConvoAiAgentClient(private val config: AgoraConfig) {

    // Separate scope so the agent can be stopped even after the ViewModel is cleared.
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Starts an agent that joins [channel], listens to [userUid], and plays [petType]'s
     * personality. Returns the agent instance id needed to stop it later.
     */
    suspend fun startAgent(channel: String, userUid: Int, petType: PetType): String =
        withContext(Dispatchers.IO) {
            val body = buildJsonObject {
                put("name", "pettalk_${System.currentTimeMillis()}")
                if (config.preset.isNotEmpty()) {
                    put("preset", config.preset)
                }
                put("properties", buildJsonObject {
                    put("channel", channel)
                    put("token", config.rtcToken)
                    put("agent_rtc_uid", AGENT_RTC_UID)
                    putJsonArray("remote_rtc_uids") {
                        add(JsonPrimitive(userUid.toString()))
                    }
                    put("idle_timeout", 120)
                    putJsonObject("advanced_features") {
                        put("enable_rtm", true)
                    }
                    putJsonObject("llm") {
                        // Without a preset the agent needs an explicit OpenAI-compatible endpoint.
                        if (config.preset.isEmpty()) {
                            put("url", config.llmUrl)
                            put("api_key", config.llmApiKey)
                            putJsonObject("params") {
                                put("model", config.llmModel)
                            }
                        }
                        putJsonArray("system_messages") {
                            add(buildJsonObject {
                                put("role", "system")
                                put("content", petType.personalityPrompt)
                            })
                        }
                        put("max_history", 32)
                        put("greeting_message", petType.greeting)
                        put("failure_message", "Hmm, that did not come out right. Try me again.")
                    }
                    putJsonObject("asr") {
                        put("language", "en-US")
                    }
                    putJsonObject("tts") {
                        putJsonObject("params") {
                            putJsonObject("voice_setting") {
                                put("voice_id", "English_captivating_female1")
                            }
                        }
                    }
                    putJsonObject("parameters") {
                        put("data_channel", "rtm")
                        put("enable_error_message", true)
                    }
                })
            }

            val response = post("$BASE_URL/${config.appId}/join", body.toString())
            if (response.code != 200) {
                throw IOException("Starting the agent failed (${response.code}): ${response.body}")
            }
            val agentId = Json.parseToJsonElement(response.body)
                .jsonObject["agent_id"]?.jsonPrimitive?.contentOrNull
                ?: throw IOException("Agent response did not include agent_id: ${response.body}")
            agentId
        }

    /** Stops the agent. Failures are swallowed: the session is over either way. */
    fun stopAgentBestEffort(agentId: String) {
        scope.launch {
            try {
                post("$BASE_URL/${config.appId}/agents/$agentId/leave", "{}")
            } catch (_: Exception) {
                // Best effort: if the leave call fails the agent exits via idle_timeout anyway.
            }
        }
    }

    private fun post(urlString: String, body: String): RestResponse {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Basic ${basicCredentials()}")
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            return RestResponse(code, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    private fun basicCredentials(): String =
        Base64.encodeToString("${config.customerKey}:${config.customerSecret}".toByteArray(), Base64.NO_WRAP)

    private data class RestResponse(val code: Int, val body: String)

    companion object {
        private const val BASE_URL = "https://api.agora.io/api/conversational-ai-agent/v2/projects"
        private const val AGENT_RTC_UID = "1001"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
    }
}
