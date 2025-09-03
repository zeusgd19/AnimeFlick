package com.zeusgd.AnimeFlick.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


// =====================
// UI State
// =====================
data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

// =====================
// UI Content
// =====================
@Composable
fun ForgotPasswordScreenContent(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    onBackLogin: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF111111), Color(0xFF1E1E1E), Color(0xFF232323))
    )


    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))


            Text(
                text = "Restablecer contraseña",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )


            Text(
                text = "Introduce tu email y te enviaremos un enlace de recuperación.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )


            Spacer(modifier = Modifier.height(32.dp))


            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = { Text("Email", color = Color.Gray) },
                placeholder = { Text("tu@email.com", color = Color.Gray) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                textStyle = TextStyle(color = Color.White),
                modifier = Modifier.fillMaxWidth()
            )


            if (state.error != null) {
                Text(
                    text = state.error ?: "",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(start = 16.dp, top = 4.dp)
                )
            }


            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = { onBackLogin() }) {
                Text("Volver al inicio de sesión")
            }


            Button(
                onClick = onSubmit,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .alpha(0.85f),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Enviar enlace")
                }
            }
        }
    }
}


// =====================
// Real Screen Wrapper
// =====================
@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel,
) {
    val state by authViewModel.forgotPasswordState.collectAsState()
    val scope = rememberCoroutineScope()


    ForgotPasswordScreenContent(
        state = state,
        onEmailChange = {
            authViewModel._forgotPasswordState.value = state.copy(email = it, error = null)
        },
        onSubmit = {
            authViewModel.resetPassword(
                state.email,
                onSuccess = { authViewModel.setShowEmailChangePasswordScreen(true) },
                onFailure = {}
            )
        },
        onBackLogin = { authViewModel.setShowForgotPasswordScreen(false) }
    )
}




/// =====================
// Preview
// =====================
@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ForgotPasswordScreenContent(
            state = ForgotPasswordUiState(),
            onEmailChange = {},
            onSubmit = {},
            onBackLogin = {}
        )
    }
}

