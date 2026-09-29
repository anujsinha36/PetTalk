package com.example.pettalk.navigation

import kotlinx.serialization.Serializable

object Screens {

    /** Pet picker, the start destination. */
    @Serializable
    data object PetSelectionScreen

    /** Live voice conversation. Carries the selected pet's enum name. */
    @Serializable
    data class VoiceSessionScreen(val petTypeName: String)
}
