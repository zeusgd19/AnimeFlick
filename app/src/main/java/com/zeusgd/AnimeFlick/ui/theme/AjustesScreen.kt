package com.zeusgd.AnimeFlick.ui.theme

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zeusgd.AnimeFlick.util.LanguageManager
import com.zeusgd.AnimeFlick.util.ThemeManager

// ----------------------
// State model
// ----------------------
data class AjustesUiState(
    val isDark: Boolean,
    val selectedLang: String
)

// ----------------------
// Pure UI (Content)
// ----------------------
@Composable
fun AjustesScreenContent(
    state: AjustesUiState,
    onToggleDark: (Boolean) -> Unit,
    onSelectLang: (String) -> Unit,
    onAbout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tema")
            Spacer(Modifier.weight(1f))
            Switch(checked = state.isDark, onCheckedChange = onToggleDark)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Idioma")
            Spacer(Modifier.weight(1f))
            listOf("es", "en", "fr").forEach { lang ->
                Text(
                    text = lang.uppercase(),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { onSelectLang(lang) }
                )
            }
        }

        Text("→ Acerca de", modifier = Modifier.clickable { onAbout() })
    }
}

// ----------------------
// Real screen wrapper
// ----------------------
@Composable
fun AjustesScreen() {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    var isDark by remember { mutableStateOf(ThemeManager.isDarkModeSet(context) ?: systemDark) }
    var selectedLang by remember {
        mutableStateOf(
            LanguageManager.getSavedLocale(context)?.language ?: "es"
        )
    }

    AjustesScreenContent(
        state = AjustesUiState(isDark, selectedLang),
        onToggleDark = {
            isDark = it
            ThemeManager.setDarkMode(context, it)
            (context as? Activity)?.recreate()
        },
        onSelectLang = { lang ->
            selectedLang = lang
            LanguageManager.setLocale(context, lang)
            (context as? Activity)?.recreate()
        },
        onAbout = { /* navegación a Acerca de */ }
    )
}

// ----------------------
// Previews
// ----------------------
@Preview(name = "ES - Dark", showBackground = true, showSystemUi = true)
@Composable
fun AjustesPreviewEsDark() {
    AjustesScreenContent(
        state = AjustesUiState(isDark = true, selectedLang = "es"),
        onToggleDark = {}, onSelectLang = {}, onAbout = {}
    )
}

@Preview(name = "EN - Light", showBackground = true, showSystemUi = true)
@Composable
fun AjustesPreviewEnLight() {
    AjustesScreenContent(
        state = AjustesUiState(isDark = false, selectedLang = "en"),
        onToggleDark = {}, onSelectLang = {}, onAbout = {}
    )
}
