package com.kers.gengyun.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    secondary = Color(0xFF558B2F),
    tertiary = Color(0xFFF9A825),
    background = Color(0xFFF5F5F5),
    surface = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color.Black,
    secondary = Color(0xFFA5D6A7),
    tertiary = Color(0xFFFFD54F),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

@Composable
fun GengyunTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
