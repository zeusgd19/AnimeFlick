package com.zeusgd.AnimeFlick.ui.visuals

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    
    // We override everything to ensure the web-like dark mode is always applied
    secondary = Color(0xFF03DAC6),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    error = Color(0xFFCF6679),
    onError = Color.Black
)

@Composable
fun AnimeFlickTheme(
    content: @Composable () -> Unit
) {
    // Forzamos el tema oscuro sin Material You dynamic colors para igualar la web
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
