package com.example.smsvote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary              = Blue600,
    onPrimary            = Color.White,
    primaryContainer     = Blue100,
    onPrimaryContainer   = Blue800,
    secondary            = Violet600,
    onSecondary          = Color.White,
    secondaryContainer   = Violet100,
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary             = Emerald600,
    onTertiary           = Color.White,
    background           = Slate100,
    onBackground         = Slate900,
    surface              = Color.White,
    onSurface            = Slate900,
    surfaceVariant       = Slate50,
    onSurfaceVariant     = Slate500,
    error                = Red500,
    onError              = Color.White,
    errorContainer       = Red100,
    onErrorContainer     = Color(0xFF7F1D1D),
)

private val DarkColorScheme = darkColorScheme(
    primary              = Violet200,
    onPrimary            = Color(0xFF4C1D95),
    primaryContainer     = Color(0xFF4338CA),
    onPrimaryContainer   = Violet100,
    secondary            = Blue100,
    onSecondary          = Blue800,
    background           = Slate900,
    onBackground         = Slate100,
    surface              = Slate800,
    onSurface            = Slate100,
    surfaceVariant       = Slate700,
    onSurfaceVariant     = Slate200,
    error                = Color(0xFFF87171),
    onError              = Color(0xFF7F1D1D),
    errorContainer       = Color(0xFF7F1D1D),
    onErrorContainer     = Color(0xFFFECACA),
)

@Composable
fun SMSVoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
