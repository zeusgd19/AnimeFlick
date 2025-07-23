package com.zeusgd.AnimeFlick.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel

@Composable
fun EpisodeTab(viewModel: AnimeViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var isReversed by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { isReversed = !isReversed }) {
                    Icon(
                        imageVector = Icons.Filled.SwapVert,
                        contentDescription = "Invertir orden",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    ) { paddingValues ->

        val episodeList = if (isReversed) viewModel.episodeList.reversed() else viewModel.episodeList

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(episodeList) { episode ->
                    val seenEpisodes by viewModel.isEpisodeSeenFlow(context).collectAsState(initial = emptySet())
                    val isSeen = seenEpisodes.contains(episode.slug)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !viewModel.isLoadingEpisode) {
                                viewModel.onEpisodeSelected(episode)
                                viewModel.markEpisodeSeen(context, episode.slug)
                            }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(
                            text = "Episodio ${episode.number}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSeen) Color.Red else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            if (viewModel.isLoadingEpisode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Cargando episodio...", color = Color.White)
                    }
                }
            }

            viewModel.selectedEpisode?.let { episode ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearSelectedEpisode() },
                    confirmButton = {},
                    title = { Text("Elegir servidor") },
                    text = {
                        Column {
                            Button(
                                onClick = {
                                    viewModel.clearSelectedEpisode()
                                    viewModel.onEpisodeClick(context, episode.slug, "YourUpload")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("YourUpload")
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.clearSelectedEpisode()
                                    viewModel.onEpisodeClick(context, episode.slug, "Stape")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Stape")
                            }
                        }
                    }
                )
            }
        }
    }
}