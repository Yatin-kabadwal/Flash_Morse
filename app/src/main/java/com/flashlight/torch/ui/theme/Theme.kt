package com.flashlight.torch.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FlashlightColorScheme = darkColorScheme(
    primary          = YellowMain,
    onPrimary        = Color(0xFF1A1A00),
    primaryContainer = Color(0xFF332B00),
    secondary        = OrangeAcc,
    background       = DarkBg,
    surface          = DarkSurface,
    surfaceVariant   = DarkCard,
    onBackground     = Color.White,
    onSurface        = Color.White,
    onSurfaceVariant = Color.White.copy(0.6f),
    outline          = Color.White.copy(0.1f)
)

@Composable
fun FlashlightTorchStrobeTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBg.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = FlashlightColorScheme,
        content     = content
    )
}