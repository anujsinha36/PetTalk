# PetTalk 🐾

A voice-first Android entertainment app that creates a playful conversation between a pet, its owner, and AI.

PetTalk does **not** claim to scientifically translate animal language. The pet's sounds become part of the interaction, while the AI (Agora Conversation AI) provides a playful interpretation of what the pet seems to mean — and speaks the owner's words back to the pet in the pet's voice.

## How the conversation works

```
Pet sound → AI interpretation → Owner responds → AI converts the response to pet voice → continue
```

1. The owner picks a pet type (cat / dog / bird) and starts a voice session.
2. The app joins an Agora RTC channel with the owner's microphone and starts a **Conversational AI agent** (Agora REST API) carrying the pet's personality prompt.
3. Everything spoken near the phone — pet sounds and owner speech — reaches the agent. The agent interprets pet sounds playfully, and retells the owner's words in the pet's voice.
4. Transcripts and agent states (listening / thinking / speaking) arrive over Agora RTM and are rendered as the conversation bubbles.

## Stack

- Kotlin, Jetpack Compose, Material 3, MVVM
- Coroutines + StateFlow, Hilt, Compose Navigation
- Agora RTC (`io.agora.rtc:full-sdk`) for real-time audio
- Agora RTM (`io.agora:agora-rtm`) for transcripts and agent state
- Agora Conversational AI engine (started via its REST API)

## Getting started

1. Open the project in Android Studio (or run `./gradlew :app:assembleDebug`).
2. Copy `local.properties.example` to `local.properties` (already present next to `sdk.dir`) and fill in:
   - `AGORA_APP_ID` — from [Agora Console](https://console.agora.io).
   - `AGORA_CUSTOMER_KEY` / `AGORA_CUSTOMER_SECRET` — Console → API REST → Customer Secret. These call the Conversational AI REST API.
   - `AGORA_RTC_TOKEN` — leave empty while your project's **App Certificate is disabled** (recommended for development; empty tokens then work for both RTC and RTM).
   - `CONVOAI_PRESET` — defaults to `deepgram_nova_3,openai_gpt_5_mini,minimax_speech_2_6_turbo`, which configures ASR/LLM/TTS with **no extra API keys**. Set it empty and fill `CONVOAI_LLM_URL/MODEL/API_KEY` to bring your own OpenAI-compatible LLM instead.
3. Run on a real device (the microphone and speaker matter).

### Notes

- **Credentials:** customer key/secret are secret — fine in `local.properties` (git-ignored) for local experiments, but a production app should proxy the agent join/leave calls through a backend.
- **Pet voices:** all pets currently use the preset's default TTS voice. To differentiate voices per pet, add a `tts` block with a per-pet `voice_id` to the request body in `ConvoAiAgentClient`.
- **Toolchain:** AGP 8.13.2 / Kotlin 2.3.0 / Gradle 9.1.0 / compileSdk 36 / minSdk 24.

## Project layout

```
app/src/main/java/com/example/pettalk/
├── MainActivity.kt, PetTalkApplication.kt   # Hilt entry points
├── navigation/                              # Serializable routes + NavGraph
├── data/
│   ├── PetType.kt                           # Pet personalities → agent system prompts
│   ├── model/                               # ConversationMessage, SessionStatus
│   └── agora/
│       ├── AgoraConfig.kt                   # BuildConfig-backed credentials
│       ├── PetTalkVoiceClient.kt            # RTC audio + RTM events → VoiceEvent stream
│       └── ConvoAiAgentClient.kt            # Conversational AI join/leave REST calls
├── di/AppModule.kt
└── presentation/
    ├── theme/
    ├── viewmodels/                          # PetSelectionViewModel, VoiceSessionViewModel
    └── screens/                             # PetSelectionScreen, VoiceSessionScreen
```

## Owner

Anuj Sinha — entertainment/experimentation project.
