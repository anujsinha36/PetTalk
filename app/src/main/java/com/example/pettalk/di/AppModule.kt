package com.example.pettalk.di

import android.app.Application
import com.example.pettalk.BuildConfig
import com.example.pettalk.data.agora.AgoraConfig
import com.example.pettalk.data.agora.ConvoAiAgentClient
import com.example.pettalk.data.agora.PetTalkVoiceClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAgoraConfig(): AgoraConfig = AgoraConfig(
        appId = BuildConfig.AGORA_APP_ID,
        rtcToken = BuildConfig.AGORA_RTC_TOKEN,
        customerKey = BuildConfig.AGORA_CUSTOMER_KEY,
        customerSecret = BuildConfig.AGORA_CUSTOMER_SECRET,
        preset = BuildConfig.CONVOAI_PRESET,
        llmUrl = BuildConfig.CONVOAI_LLM_URL,
        llmModel = BuildConfig.CONVOAI_LLM_MODEL,
        llmApiKey = BuildConfig.CONVOAI_LLM_API_KEY,
        rtmToken = BuildConfig.AGORA_RTM_TOKEN,
    )

    @Provides
    @Singleton
    fun provideVoiceClient(application: Application, config: AgoraConfig): PetTalkVoiceClient =
        PetTalkVoiceClient(application, config)

    @Provides
    @Singleton
    fun provideAgentClient(config: AgoraConfig): ConvoAiAgentClient = ConvoAiAgentClient(config)
}
