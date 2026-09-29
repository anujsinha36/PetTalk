package com.example.pettalk.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Coral,
    onPrimary = WarmSurface,
    primaryContainer = CoralContainer,
    secondary = Teal,
    secondaryContainer = TealContainer,
    background = WarmBackground,
    surface = WarmSurface,
)

private val DarkColors = darkColorScheme(
    primary = CoralNight,
    primaryContainer = CoralNightContainer,
    secondary = TealNight,
    secondaryContainer = TealNightContainer,
    background = NightBackground,
    surface = NightSurface,
)

@Composable
fun PetTalkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
