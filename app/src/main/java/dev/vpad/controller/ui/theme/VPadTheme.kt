package dev.vpad.controller.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand colors
val VPadPrimary    = Color(0xFF00D4FF)   // Electric cyan
val VPadSecondary  = Color(0xFF7B2FBE)   // Deep purple
val VPadBackground = Color(0xFF0A0E1A)   // Almost black
val VPadSurface    = Color(0xFF131929)   // Dark navy card
val VPadSurface2   = Color(0xFF1C2539)   // Lighter card variant
val VPadOnSurface  = Color(0xFFE0E6F0)

private val DarkColorScheme = darkColorScheme(
    primary          = VPadPrimary,
    onPrimary        = Color(0xFF001F26),
    primaryContainer = Color(0xFF003544),
    secondary        = VPadSecondary,
    background       = VPadBackground,
    surface          = VPadSurface,
    onSurface        = VPadOnSurface,
    onBackground     = VPadOnSurface,
    error            = Color(0xFFFF5252),
    onError          = Color(0xFFFFFFFF),
    surfaceVariant   = VPadSurface2,
    outline          = Color(0xFF2C3A52),
)

@Composable
fun VPadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = MaterialTheme.typography,
        content     = content
    )
}
