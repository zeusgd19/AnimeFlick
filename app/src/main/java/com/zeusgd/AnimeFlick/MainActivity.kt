package com.zeusgd.AnimeFlick

import android.content.Context
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zeusgd.AnimeFlick.ui.visuals.AnimeFlickTheme

import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel
import com.zeusgd.AnimeFlick.ui.theme.MainAnimeScreen
import com.zeusgd.AnimeFlick.util.LanguageManager

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<AnimeViewModel>()

    override fun attachBaseContext(newBase: Context) {
        val updated = LanguageManager.applyLocale(newBase)
        super.attachBaseContext(updated)
    }

    @RequiresApi(TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AnimeFlickTheme {
                MainAnimeScreen(this@MainActivity, viewModel, window)
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            when {
                viewModel.selectedAnime != null -> {
                    viewModel.selectedAnime = null
                    viewModel.animeInfo = null
                }

                viewModel.currentScreen != Screen.Recientes -> {
                    viewModel.currentScreen = Screen.Recientes
                }

                else -> {
                    finish()
                }
            }
        }
    }

}

enum class Screen {
    Recientes,
    Favoritos,
    Explorar,
    Temporada,
    Ajustes,
    MisAnime
}

@Composable
fun Screen.getLabel(): String {
    return when (this) {
        Screen.Recientes -> stringResource(R.string.recents)
        Screen.Favoritos -> stringResource(R.string.favorite)
        Screen.Explorar -> stringResource(R.string.explorer)
        Screen.Temporada -> stringResource(R.string.season)
        Screen.Ajustes -> stringResource(R.string.settings)
        Screen.MisAnime -> "Mis Animes"
    }
}
