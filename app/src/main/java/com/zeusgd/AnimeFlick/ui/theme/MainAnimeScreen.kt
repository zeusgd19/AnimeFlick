package com.zeusgd.AnimeFlick.ui.theme

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.zeusgd.AnimeFlick.Screen
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAnimeScreen(context: Context,viewModel: AnimeViewModel) {
    val screen by remember { derivedStateOf { viewModel.currentScreen } }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            if (screen != Screen.Ajustes && viewModel.selectedAnime == null) {
                TopAppBar(
                    title = {
                        if (isSearching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    viewModel.search(it)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Buscar anime") },
                                singleLine = true
                            )
                        } else {
                            Text(screen.name)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            isSearching = !isSearching
                            if (!isSearching) {
                                searchQuery = ""
                                viewModel.search("")
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar"
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (viewModel.selectedAnime == null) {
                AnimeBottomBar(currentScreen = screen, onScreenSelected = {
                    viewModel.navigateTo(it)
                    searchQuery = ""
                    viewModel.search("")
                })
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (viewModel.selectedAnime != null) {
                AnimeDetailScreen(
                    context,
                    anime = viewModel.selectedAnime!!,
                    animeInfo = viewModel.animeInfo!!,
                    onBack = {
                        viewModel.animeInfo = null
                        viewModel.selectedAnime = null
                    },
                    viewModel = viewModel
                )
            } else {
                if (isSearching && searchQuery.isNotBlank()) {
                    BuscarScreen(context,viewModel)
                } else {
                    when (screen) {
                        Screen.Recientes -> RecientesScreen(viewModel) { animeSearched ->
                            viewModel.loadEpisodes(animeSearched)
                        }
                        Screen.Favoritos -> FavoritosScreen(viewModel)
                        Screen.Explorar -> DirectorioScreen(context, viewModel)
                        Screen.Temporada -> TemporadaTab(context,viewModel) { animeSearched ->
                            viewModel.loadEpisodes(animeSearched)
                        }
                        Screen.Ajustes -> AjustesScreen()
                    }
                }
            }
        }
    }
}