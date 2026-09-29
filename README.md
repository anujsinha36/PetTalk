# PetTalk 🐾

A voice-first Android entertainment app that creates a playful conversation between a pet, its owner, and AI, built for the Agora Voice AI Hackathon.

PetTalk does **not** claim to scientifically translate animal language. The owner talks, the AI answers in the pet's "voice", the pet reacts, and the AI (Agora Conversational AI) gives a playful interpretation of what the pet seems to mean.

## How the conversation works
```
Owner speaks → agent picks a mood and the app answers with a pet sound → the pet reacts → agent gives a human-language interpretation → continue
```
- **Pet sounds (current limitation):** the pet's replies are spoken by a text-to-speech voice, so PetTalk does not yet produce real cat, dog, or bird sounds. A future version will play recorded pet clips matched to the mood the agent picks.


1. The owner picks a pet type (cat / dog / bird) and starts a voice session.
2. The app joins an Agora RTC channel with the owner's microphone and starts a **Conversational AI agent** (Agora REST API) carrying the pet's personality prompt.
3. There is a single microphone, so the agent decides whose turn it is from what it hears:
   - When the **owner** speaks in clear sentences, the agent answers with pet sounds (for example "Mrrrow!").
   - When it then hears the **pet** (a meow, a bark, a chirp), it says in short English sentences what the pet seems to mean.
4. Transcripts and agent states (listening / thinking / speaking) arrive over Agora RTM and are shown as conversation bubbles.

## Stack

- Kotlin, Jetpack Compose, Material 3, MVVM
- Coroutines + StateFlow, Hilt, Compose Navigation
- Agora RTC (`io.agora.rtc:full-sdk`) for real-time audio
- Agora RTM (`io.agora:agora-rtm`) for transcripts and agent state
- Agora Conversational AI engine (started via its REST API)

## Getting started

1. Open the project in Android Studio (or run `./gradlew :app:assembleDebug`).
2. Copy `local.properties.example` to `local.properties` and fill in:
   - `AGORA_APP_ID`: from the [Agora Console](https://console.agora.io). Use a project that has **Conversational AI** enabled.
   - `AGORA_CUSTOMER_KEY` / `AGORA_CUSTOMER_SECRET`: Console → RESTful API → Customer Secret. These call the Conversational AI REST API.
   - `AGORA_RTC_TOKEN` and `AGORA_RTM_TOKEN`: see [Tokens](#tokens) below. Leave both empty only if your project has no App Certificate.
   - `CONVOAI_PRESET`: defaults to `deepgram_nova_3,openai_gpt_5_mini,minimax_speech_2_6_turbo`, which configures speech recognition, the LLM and text-to-speech with **no extra API keys**. Set it empty and fill `CONVOAI_LLM_URL/MODEL/API_KEY` to bring your own OpenAI-compatible LLM instead.
3. Do not put a space after the `=` in `local.properties`.
4. Run on a real device (the microphone and speaker matter) and allow the microphone permission.

### Tokens

The app uses fixed test values:

| Who | Channel | User ID |
| --- | --- | --- |
| Phone (owner) | `demo-channel` | `1002` |
| AI agent | `demo-channel` | `1001` |

New Agora projects require tokens, and a token only works for the channel and user ID it was generated for. In the Console, use **Generate Temp Token** (channel `demo-channel`) twice:

1. **Phone token**, RTC UID `1002`. Paste it into both `AGORA_RTC_TOKEN` and `AGORA_RTM_TOKEN` in `local.properties`.
2. **Agent token**, RTC UID `1001`. Paste it in `ConvoAiAgentClient.kt`, replacing `PASTE_AGENT_TOKEN_HERE` in the `put("token", ...)` line.

Temporary tokens expire within 24 hours, so regenerate them if the session stops working.

### Notes

- **Credentials:** the customer key and secret are secret. They are fine in `local.properties` (git-ignored) for local experiments, but a production app should proxy the agent join/leave calls through a backend. Do not commit the agent token either.
- **Pet voice setting:** the request in `ConvoAiAgentClient` includes a `tts` block with a `voice_id`. Without it the preset's text-to-speech fails with an "invalid params, empty field" error. To give each pet a different voice, use a different `voice_id` per pet.
- **Pet sounds (current limitation):** the pet's replies are spoken by a text-to-speech voice, so PetTalk does not yet produce real cat, dog, or bird sounds. A future version will play recorded pet clips matched to the mood the agent picks.
- **Pet detection:** the speech recognizer is built for human speech, so real animal noises may not always be picked up.
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

Anuj Sinha, hackathon project.