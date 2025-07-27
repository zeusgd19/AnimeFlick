package com.zeusgd.AnimeFlick.ui.theme

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import checkForUpdateFromGitHub
import com.zeusgd.AnimeFlick.Screen
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel
import downloadAndInstall
import getUpdatedInfo
import updatedInfo

@RequiresApi(TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAnimeScreen(context: Context,viewModel: AnimeViewModel) {
    val screen by remember { derivedStateOf { viewModel.currentScreen } }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var apkUrl by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val url = checkForUpdateFromGitHub(context)
        val updatedInfo = getUpdatedInfo()
        if (url != null) {
            apkUrl = url
            info = updatedInfo
        }
    }

    if (apkUrl != null) {
        AlertDialog(
            onDismissRequest = { apkUrl = null },
            title = { Text("Actualización disponible") },
            text = {
                Column {
                    Text("¿Quieres actualizar la app ahora?")
                    Spacer(Modifier.height(8.dp))
                    Text("Novedades:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    info?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    downloadAndInstall(context, apkUrl!!) // Usa la función que ya tienes
                    apkUrl = null
                }) {
                    Text("Actualizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { apkUrl = null }) {
                    Text("Más tarde")
                }
            }
        )
    }

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