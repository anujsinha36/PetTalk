package com.example.pettalk.data.agora

import android.content.Context
import android.util.Log
import com.example.pettalk.data.model.SessionStatus
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtm.ErrorInfo
import io.agora.rtm.MessageEvent
import io.agora.rtm.PresenceEvent
import io.agora.rtm.ResultCallback
import io.agora.rtm.RtmClient
import io.agora.rtm.RtmConfig
import io.agora.rtm.RtmConstants
import io.agora.rtm.RtmEventListener
import io.agora.rtm.SubscribeOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Owns the Agora RTC and RTM clients for a voice session and turns their callbacks
 * into a simple [VoiceEvent] stream that the ViewModel can render.
 *
 * The agent itself runs on Agora's Conversational AI engine ([ConvoAiAgentClient] starts
 * it); this class only transports the owner's microphone audio up and the pet's voice
 * down, and receives transcripts and agent states over RTM.
 */
class PetTalkVoiceClient(
    private val context: Context,
    private val config: AgoraConfig,
) {
    private val TAG = "PetTalk_Debug_123"
    // The client is a singleton, so this scope intentionally outlives any ViewModel.
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _events = MutableSharedFlow<VoiceEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<VoiceEvent> = _events.asSharedFlow()

    private var rtcEngine: RtcEngine? = null
    private var rtmClient: RtmClient? = null
    private var channelName: String? = null

    /** Joins the RTC channel with [uid] and subscribes to agent messages over RTM. */
    suspend fun start(channel: String, uid: Int) = withContext(Dispatchers.IO) {
        if (rtcEngine != null || rtmClient != null) {
            stopInternal()
        }
        channelName = channel
        setupRtc(channel, uid)
        setupRtm(channel, uid.toString())
    }

    /** Mutes or unmutes the owner's microphone. */
    fun setMicMuted(muted: Boolean) {
        rtcEngine?.muteLocalAudioStream(muted)
    }

    /** Leaves the channel and releases both clients. Safe to call repeatedly. */
    fun stop() {
        scope.launch(Dispatchers.IO) { stopInternal() }
    }

    private suspend fun stopInternal() {
        try {
            rtcEngine?.leaveChannel()
        } catch (_: Exception) {
        }
        try {
            rtmClient?.logout(object : ResultCallback<Void> {
                override fun onSuccess(responseInfo: Void?) = Unit
                override fun onFailure(errorInfo: ErrorInfo) = Unit
            })
        } catch (_: Exception) {
        }
        try {
            rtmClient?.removeEventListener(rtmListener)
        } catch (_: Exception) {
        }
        try {
            RtmClient.release()
        } catch (_: Exception) {
        }
        try {
            RtcEngine.destroy()
        } catch (_: Exception) {
        }
        rtmClient = null
        rtcEngine = null
        channelName = null
    }

    private fun setupRtc(channel: String, uid: Int) {
        val handler = object : IRtcEngineEventHandler() {
            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                Log.d(TAG, "2 - RTC JOIN SUCCESS | channel=$channel | uid=$uid")
                emit(VoiceEvent.StatusChanged(SessionStatus.IDLE))
            }

            override fun onError(err: Int) {
                Log.e(TAG, "3 - RTC ERROR | code=$err")
                emit(VoiceEvent.Error("RTC error $err"))
            }
            override fun onLocalAudioStateChanged(
                state: Int,
                error: Int
            ) {
                Log.d(
                    TAG,
                    "4 - MIC STATE | state=$state | error=$error"
                )
            }
        }
        val engine = RtcEngine.create(context, config.appId, handler)
        rtcEngine = engine
        // Conversational AI audio profile: must be set before joining the channel.
        engine.setAudioScenario(Constants.AUDIO_SCENARIO_AI_CLIENT)
        val audioResult = engine.enableAudio()
        Log.d(TAG, "8 - enableAudio returned=$audioResult")
        engine.enableAudioVolumeIndication(200, 3, true)
        engine.disableVideo()
        engine.setEnableSpeakerphone(true)
        engine.joinChannel(config.rtcToken, channel, "", uid)
    }

    private suspend fun setupRtm(channel: String, uid: String) {
        val client = RtmClient.create(RtmConfig.Builder(config.appId, uid).build())
        rtmClient = client
        client.addEventListener(rtmListener)
        awaitCallback { onResult ->
            client.login(config.rtmToken, object : ResultCallback<Void> {
                override fun onSuccess(responseInfo: Void?) = onResult(null)
                override fun onFailure(errorInfo: ErrorInfo) =
                    onResult(IllegalStateException("RTM login failed: ${errorInfo.errorCode} ${errorInfo.errorReason}"))
            })
        }
        awaitCallback { onResult ->
            val options = SubscribeOptions().apply {
                withMessage = true
                withPresence = true
            }
            client.subscribe(channel, options, object : ResultCallback<Void> {
                override fun onSuccess(responseInfo: Void?) = onResult(null)
                override fun onFailure(errorInfo: ErrorInfo) =
                    onResult(IllegalStateException("RTM subscribe failed: ${errorInfo.errorCode} ${errorInfo.errorReason}"))
            })
        }
    }

    private val rtmListener = object : RtmEventListener {
        override fun onMessageEvent(event: MessageEvent?) {
            event ?: return
            val message = event.message ?: return
            val json = when (val data = message.data) {
                is String -> data
                is ByteArray -> String(data, Charsets.UTF_8)
                else -> return
            }
            handleAgentMessage(json)
        }

        override fun onPresenceEvent(event: PresenceEvent?) {
            event ?: return
            if (event.eventType != RtmConstants.RtmPresenceEventType.REMOTE_STATE_CHANGED) return
            if (event.channelType != RtmConstants.RtmChannelType.MESSAGE) return
            val state = event.stateItems["state"] ?: return
            emit(VoiceEvent.StatusChanged(mapAgentState(state)))
        }
    }

    /**
     * Parses agent messages sent over RTM. The important shapes are:
     * - {"object": "user.transcription", "text": "...", "final": true}        (owner speech)
     * - {"object": "assistant.transcription", "text": "...", "turn_status": 1} (agent reply, 1 = finished)
     * - {"object": "message.error", "message": "..."}                          (agent errors)
     */
    private fun handleAgentMessage(json: String) {
        val root = try {
            Json.parseToJsonElement(json).jsonObject
        } catch (_: Exception) {
            return
        }
        val objectType = root["object"]?.jsonPrimitive?.contentOrNull ?: return
        when (objectType) {
            "user.transcription" -> {
                val text = root["text"]?.jsonPrimitive?.contentOrNull ?: return
                val final = root["final"]?.jsonPrimitive?.booleanOrNull ?: false
                if (final && text.isNotBlank()) {
                    emit(VoiceEvent.OwnerTranscript(text))
                }
            }

            "assistant.transcription" -> {
                val text = root["text"]?.jsonPrimitive?.contentOrNull ?: return
                val turnStatus = root["turn_status"]?.jsonPrimitive?.intOrNull ?: return
                // 0 = in progress, 1 = finished, 2 = interrupted. Only finished turns become bubbles.
                if (turnStatus == 1 && text.isNotBlank()) {
                    emit(VoiceEvent.AgentTranscript(text))
                }
            }

            "message.error" -> {
                val message = root["message"]?.jsonPrimitive?.contentOrNull ?: return
                emit(VoiceEvent.Error("Agent error: $message"))
            }
        }
    }

    private fun mapAgentState(state: String): SessionStatus = when (state) {
        "listening" -> SessionStatus.LISTENING
        "thinking" -> SessionStatus.THINKING
        "speaking" -> SessionStatus.SPEAKING
        else -> SessionStatus.IDLE
    }

    private fun emit(event: VoiceEvent) {
        _events.tryEmit(event)
    }

    /** Wraps an RTM-style callback into a suspend call. */
    private suspend fun awaitCallback(call: (onResult: (Exception?) -> Unit) -> Unit) {
        suspendCancellableCoroutine { continuation ->
            call { exception ->
                if (exception == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }
}
