package com.aim.vpn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE53935),
    onPrimary = Color.White,
    secondary = Color(0xFF1A1A24),
    onSecondary = Color.White,
    background = Color(0xFF0A0A0F),
    onBackground = Color.White,
    surface = Color(0xFF12121A),
    onSurface = Color.White,
    error = Color(0xFFEF5350)
)

@Composable
fun VipCoolWireTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
