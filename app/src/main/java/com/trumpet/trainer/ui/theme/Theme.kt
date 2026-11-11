package com.trumpet.trainer.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

val Brass = Color(0xFFE8B44A)
val BrassBright = Color(0xFFF5D48A)
val Ink = Color(0xFF0E1016)
val Panel = Color(0xFF171A24)
val PanelLight = Color(0xFF232839)
val LineColor = Color(0xFF2A2E3E)
val TextPrimary = Color(0xFFE6E8F0)
val TextSecondary = Color(0xFFB4BACC)
val Good = Color(0xFF6FD08C)
val Bad = Color(0xFFFF6B6B)

private val Scheme = darkColorScheme(
    primary = Brass,
    onPrimary = Ink,
    secondary = Color(0xFF8FA1E8),
    onSecondary = Ink,
    tertiary = Good,
    onTertiary = Ink,
    background = Ink,
    onBackground = TextPrimary,
    surface = Ink,
    onSurface = TextPrimary,
    surfaceVariant = Panel,
    onSurfaceVariant = TextSecondary,
    outline = LineColor,
    error = Bad,
    onError = Ink,
)

/** Тёмная «латунная» тема — игровой вид. */
@Composable
fun TrumpetTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content,
        )
    }
}
