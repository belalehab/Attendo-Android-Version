package com.attendo.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    background = Color(0xFF0F172A), // Slate 900
    surface = Color(0xFF0A0F1C), // Darker Slate
    primary = Color(0xFF2DD4BF), // Teal 400
    secondary = Color(0xFF818CF8), // Indigo 400
    error = Color(0xFFF43F5E), // Rose 500
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF)
)

@Composable
fun AttendoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
