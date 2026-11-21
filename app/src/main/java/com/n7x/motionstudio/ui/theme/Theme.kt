package com.n7x.motionstudio.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

val Amoled = Color(0xFF000000)
val SurfaceDark = Color(0xFF111114)
val SurfaceHigh = Color(0xFF1B1B20)
val Accent = Color(0xFF00E5FF)

private val N7XColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    background = Amoled,
    onBackground = Color(0xFFEDEDED),
    surface = SurfaceDark,
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = Color(0xFFB0B0B8),
    error = Color(0xFFFF5370)
)

@Composable
fun N7XTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = N7XColors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}
