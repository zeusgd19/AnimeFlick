package com.zeusgd.AnimeFlick.ui.theme

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import com.zeusgd.AnimeFlick.model.AnimeSearched
import com.zeusgd.AnimeFlick.model.Episode
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun RecientesScreen(
    viewModel: AnimeViewModel,
    onAnimeClick: (AnimeSearched) -> Unit
) {
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val context = LocalContext.current
    val isLoadingEpisode by remember { derivedStateOf { viewModel.isLoadingEpisode } }
    val selectedEpisode by remember { derivedStateOf { viewModel.selectedEpisode } }
    val episodes = remember { viewModel.recentEpisodes }
    val listState = rememberLazyListState()

    SwipeRefresh(
        state = remember { SwipeRefreshState(isRefreshing) },
        onRefresh = { viewModel.refreshRecentEpisodes(context) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(episodes, key = { it.title + it.number }) { episode ->
                    RecentEpisodeItem(
                        episode = episode,
                        isLoading = isLoadingEpisode,
                        onClick = {
                            CoroutineScope(Dispatchers.Main).launch {
                                val results =
                                    viewModel.searchDirect(episode.title.replace(" ", "%20"))
                                val episodeSlug = "${results.first().slug}-${episode.number}"
                                viewModel.onEpisodeSelected(
                                    Episode(
                                        number = episode.number,
                                        slug = episodeSlug,
                                        url = ""
                                    )
                                )
                            }
                        },
                        onInfoClick = {
                            CoroutineScope(Dispatchers.Main).launch {
                                val results = viewModel.searchDirect(episode.title.replace(" ", "%20"))
                                if (results.isNotEmpty()) {
                                    onAnimeClick(results.first())
                                } else {
                                    Toast.makeText(context, "No se encontró el anime", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }

            if (isLoadingEpisode) {
                LoadingOverlay("Cargando episodio...")
            }

            selectedEpisode?.let { episode ->
                EpisodeServerDialog(
                    episode = episode,
                    onDismiss = { viewModel.clearSelectedEpisode() },
                    onSelect = { server ->
                        viewModel.clearSelectedEpisode()
                        viewModel.onEpisodeClick(context, episode.slug, server)
                    }
                )
            }
        }
    }
}