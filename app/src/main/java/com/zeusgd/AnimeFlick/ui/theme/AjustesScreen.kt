package com.zeusgd.AnimeFlick.ui.theme

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zeusgd.AnimeFlick.R
import com.zeusgd.AnimeFlick.util.LanguageManager
import com.zeusgd.AnimeFlick.util.ThemeManager
import java.util.Locale

@Composable
fun AjustesScreen() {

    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    var isDark by remember {
        mutableStateOf(ThemeManager.isDarkModeSet(context) ?: systemDark)
    }

    var selectedLanguage by remember {
        mutableStateOf(LanguageManager.getSavedLocale(context)?.language ?: Locale.getDefault().language)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.theme))
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isDark,
                onCheckedChange = {
                    isDark = it
                    ThemeManager.setDarkMode(context, it)
                    (context as? Activity)?.recreate()

                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.language))
            Spacer(modifier = Modifier.weight(1f))
            listOf("es", "en", "fr").forEach { lang ->
                Text(
                    text = lang.uppercase(),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable {
                            selectedLanguage = lang
                            LanguageManager.setLocale(context, lang)
                            (context as? Activity)?.recreate()
                        }
                )
            }
        }
        Text("→ Acerca de")
    }
}