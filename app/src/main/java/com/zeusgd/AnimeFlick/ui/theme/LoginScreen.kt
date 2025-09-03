package com.zeusgd.AnimeFlick.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------
// State model
// ---------------------------------------------------------
data class LoginUiState(
    val isLoginMode: Boolean = true,      // true = Iniciar sesión, false = Registrarse
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",     // solo registro
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

// ---------------------------------------------------------
// Pure UI (Content)
// ---------------------------------------------------------
@Composable
fun LoginScreenContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit, // <-- NUEVO
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
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
            Spacer(Modifier.height(28.dp))

            // Marca
            Text(
                text = "AnimeFlick",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 12.dp)
            )

            // Subtítulo
            Text(
                text = if (state.isLoginMode) "Inicia sesión para sincronizar tus datos"
                else "Crea tu cuenta para guardar tus favoritos",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(.9f)
            )

            Spacer(Modifier.height(28.dp))

            // Tarjeta / panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B2B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Email
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        singleLine = true,
                        label = { Text("Email", color = Color.Gray ) },
                        placeholder = { Text("tu@email.com", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        textStyle = TextStyle(Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Username (solo registro)
                    AnimatedVisibility(visible = !state.isLoginMode) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = state.username,
                                onValueChange = onUsernameChange,
                                singleLine = true,
                                label = { Text("Nombre de usuario", color = Color.Gray) },
                                placeholder = { Text("Tu nombre público", color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                textStyle = TextStyle(Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Password
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        singleLine = true,
                        label = { Text("Contraseña", color = Color.Gray) },
                        placeholder = { Text("Contraseña", color = Color.Gray)},
                        visualTransformation = if (state.isPasswordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val label = if (state.isPasswordVisible) "Ocultar" else "Mostrar"
                            Text(
                                text = label,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clickable { onTogglePasswordVisibility() }
                                    .semantics { contentDescription = label }
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (state.isLoginMode) ImeAction.Done else ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (state.isLoginMode) onSubmit() }
                        ),
                        textStyle = TextStyle(Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Confirm password (solo registro)
                    AnimatedVisibility(visible = !state.isLoginMode) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = state.confirmPassword,
                                onValueChange = onConfirmPasswordChange,
                                singleLine = true,
                                label = { Text("Repite la contraseña", color = Color.Gray) },
                                placeholder = { Text("Repite la contraseña", color = Color.Gray) },
                                visualTransformation = if (state.isPasswordVisible)
                                    VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { onSubmit() }
                                ),
                                textStyle = TextStyle(Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Error
                    AnimatedVisibility(visible = state.error != null) {
                        Text(
                            text = state.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Botón principal
                    Button(
                        onClick = onSubmit,
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
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
                            Text(if (state.isLoginMode) "Iniciar sesión" else "Crear cuenta")
                        }
                    }

                    // Forgot password (solo login)
                    AnimatedVisibility(visible = state.isLoginMode) {
                        Text(
                            text = "¿Has olvidado la contraseña?",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .clickable { onForgotPassword() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Toggle login/registro
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.isLoginMode) "¿No tienes cuenta?" else "¿Ya tienes cuenta?",
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (state.isLoginMode) "Regístrate" else "Inicia sesión",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onToggleMode() }
                )
            }
        }
    }
}


// ---------------------------------------------------------
// Real screen wrapper (gestiona estado y conecta lógica)
// ---------------------------------------------------------
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val ui by authViewModel.uiState.collectAsState()
    val showVerifyScreen by authViewModel.showVerifyEmailScreen.collectAsState()
    val showForgotPassword by authViewModel.showForgotPasswordScreen.collectAsState()
    val showEmailChangePasswordScreen by authViewModel.showEmailChangePasswordScreen.collectAsState()
    val state by authViewModel.forgotPasswordState.collectAsState()

    when {
        showVerifyScreen -> {
            VerifyEmailScreen(
                email = ui.username,
                onBackToLogin = {
                    authViewModel.setShowVerifyEmailScreen(false)
                }
            )
        }

        showEmailChangePasswordScreen -> {
            EmailChangePasswordScreen(
                email = state.email,
                onBackToLogin = {
                    authViewModel.setShowEmailChangePasswordScreen(false)
                }
            )
        }

        showForgotPassword -> {
            ForgotPasswordScreen(
                authViewModel = authViewModel,
            )
        }

        else -> {
            LoginScreenContent(
                state = ui,
                onEmailChange = { authViewModel._uiState.value = ui.copy(email = it, error = null) },
                onPasswordChange = { authViewModel._uiState.value = ui.copy(password = it, error = null) },
                onConfirmPasswordChange = { authViewModel._uiState.value = ui.copy(confirmPassword = it, error = null) },
                onTogglePasswordVisibility = {
                    authViewModel._uiState.value =
                        ui.copy(isPasswordVisible = !ui.isPasswordVisible)
                },
                onToggleMode = {
                    authViewModel._uiState.value =
                        ui.copy(isLoginMode = !ui.isLoginMode, error = null)
                },
                onSubmit = {
                    if (ui.isLoginMode) {
                        authViewModel.signIn(ui.email, ui.password, onLoginSuccess, ui.username)
                    } else {
                        authViewModel.signUp(ui.email, ui.password, ui.username)
                    }
                },
                onUsernameChange = { authViewModel._uiState.value = ui.copy(username = it, error = null) },
                onForgotPassword = {
                    authViewModel.setShowForgotPasswordScreen(true)
                }
            )
        }
    }
}

// ---------------------------------------------------------
// Previews (no requieren lógica real)
// ---------------------------------------------------------
@Preview(name = "Login - Dark", showBackground = true, showSystemUi = true)
@Composable
fun LoginPreviewDark() {
    val dummy = { _: String, _: String -> Result.success(Unit) }
    MaterialTheme(colorScheme = lightColorScheme()) {
        var ui by remember { mutableStateOf(LoginUiState(isLoginMode = true)) }
        LoginScreenContent(
            state = ui,
            onEmailChange = { ui = ui.copy(email = it) },
            onPasswordChange = { ui = ui.copy(password = it) },
            onConfirmPasswordChange = { ui = ui.copy(confirmPassword = it) },
            onTogglePasswordVisibility = { ui = ui.copy(isPasswordVisible = !ui.isPasswordVisible) },
            onToggleMode = { ui = ui.copy(isLoginMode = !ui.isLoginMode) },
            onSubmit = {},
            onUsernameChange = {},
            onForgotPassword = {}
        )
    }
}

@Preview(name = "Register - Light", showBackground = true, showSystemUi = true)
@Composable
fun RegisterPreviewLight() {
    val dummy = { _: String, _: String -> Result.success(Unit) }
    MaterialTheme(colorScheme = lightColorScheme()) {
        var ui by remember { mutableStateOf(LoginUiState(isLoginMode = false)) }
        LoginScreenContent(
            state = ui,
            onEmailChange = { ui = ui.copy(email = it) },
            onPasswordChange = { ui = ui.copy(password = it) },
            onConfirmPasswordChange = { ui = ui.copy(confirmPassword = it) },
            onTogglePasswordVisibility = { ui = ui.copy(isPasswordVisible = !ui.isPasswordVisible) },
            onToggleMode = { ui = ui.copy(isLoginMode = !ui.isLoginMode) },
            onSubmit = {},
            onUsernameChange = {},
            onForgotPassword = {}
        )
    }
}
