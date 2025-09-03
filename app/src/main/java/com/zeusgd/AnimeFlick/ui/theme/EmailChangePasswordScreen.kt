package com.zeusgd.AnimeFlick.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun EmailChangePasswordScreen(
    email: String, // ← ahora este email viene del viewModel
    onBackToLogin: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF111111), Color(0xFF1E1E1E), Color(0xFF232323))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Título AnimeFlick
            Text(
                text = "AnimeFlick",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(Modifier.height(12.dp))

            // Subtítulo
            Text(
                text = "Te hemos enviado un correo a:",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            // Email mostrado dinámicamente
            Text(
                text = email,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Texto explicativo
            Text(
                text = "Abre tu bandeja de entrada y entre al enlace enviado para cambiar tu contraseña.",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Spacer(Modifier.height(32.dp))

            // Botón para volver al login
            Button(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) {
                Text("Volver al login")
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun EmailChangePasswordScreenPreview() {
    EmailChangePasswordScreen(
        email = "usuario@example.com",
        onBackToLogin = {}
    )
}
