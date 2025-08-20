package com.zeusgd.AnimeFlick.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// ----------------------
// Pure UI
// ----------------------
@Composable
fun LoadingOverlayContent(
    text: String,
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 0.4f
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text(text, color = Color.White)
        }
    }
}

// ----------------------
// Wrapper
// ----------------------
@Composable
fun LoadingOverlay(text: String = "Cargando...") {
    LoadingOverlayContent(text = text)
}


// ----------------------
// Previews
// ----------------------
@Preview(showBackground = true, showSystemUi = true, name = "Overlay - Default")
@Composable
private fun LoadingOverlayPreview_Default() {
    LoadingOverlayContent(text = "Cargando...")
}

@Preview(showBackground = true, showSystemUi = true, name = "Overlay - Mensaje largo")
@Composable
private fun LoadingOverlayPreview_Long() {
    LoadingOverlayContent(
        text = "Procesando datos y preparando todo...",
        backgroundAlpha = 0.55f
    )
}
