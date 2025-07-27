package com.zeusgd.AnimeFlick

import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi

import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel
import com.zeusgd.AnimeFlick.ui.theme.GodAnimeTheme
import com.zeusgd.AnimeFlick.ui.theme.MainAnimeScreen

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<AnimeViewModel>()

    @RequiresApi(TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GodAnimeTheme {
                MainAnimeScreen(this@MainActivity,viewModel)
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
    Ajustes
}
