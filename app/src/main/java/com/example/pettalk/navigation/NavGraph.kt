package com.example.pettalk.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pettalk.presentation.screens.petselection.PetSelectionScreen
import com.example.pettalk.presentation.screens.voicesession.VoiceSessionScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screens.PetSelectionScreen
    ) {
        composable<Screens.PetSelectionScreen> {
            PetSelectionScreen(
                onStartConversation = { petType ->
                    navController.navigate(Screens.VoiceSessionScreen(petTypeName = petType.name))
                }
            )
        }
        composable<Screens.VoiceSessionScreen> {
            VoiceSessionScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
