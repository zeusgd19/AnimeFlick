package com.zeusgd.AnimeFlick.ui.theme

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.zeusgd.AnimeFlick.model.AnimeSearched
import com.zeusgd.AnimeFlick.model.Episode
import com.zeusgd.AnimeFlick.model.RecentEpisode
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel
import kotlinx.coroutines.launch

// ----------------------
// UI Model
// ----------------------
data class RecentEpisodeListUi(
    val title: String,
    val number: Int,
    val coverUrl: String
)

// ----------------------
// Pure UI
// ----------------------
@Composable
fun RecientesScreenContent(
    episodes: List<RecentEpisodeListUi>,
    isRefreshing: Boolean,
    isLoadingEpisode: Boolean,
    onRefresh: () -> Unit,
    onClickEpisode: (index: Int) -> Unit,
    onInfoEpisode: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = onRefresh
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(episodes, key = { _, it -> it.title + it.number }) { index, ep ->
                    // Convertimos a RecentEpisodeUi (del item) justo aquí
                    RecentEpisodeItem(
                        episode = RecentEpisodeUi(               // <- el del otro fichero
                            coverUrl = ep.coverUrl,
                            title = ep.title,
                            number = ep.number
                        ).toRecentEpisode(),
                        isLoading = isLoadingEpisode,
                        onClick = { onClickEpisode(index) },
                        onInfoClick = { onInfoEpisode(index) }
                    )
                }
            }
        }
    }
}

fun RecentEpisodeUi.toRecentEpisode(): RecentEpisode {
    return RecentEpisode(
        title = title,
        number = number,
        cover = coverUrl
    )
}

// ----------------------
// Wrapper
// ----------------------
@Composable
fun RecientesScreen(
    viewModel: AnimeViewModel,
    onAnimeClick: (AnimeSearched) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingEpisode by remember { derivedStateOf { viewModel.isLoadingEpisode } }
    val selectedEpisode by remember { derivedStateOf { viewModel.selectedEpisode } }
    val episodes = remember { viewModel.recentEpisodes }
    val scope = rememberCoroutineScope()

    RecientesScreenContent(
        episodes = episodes.map { RecentEpisodeListUi(it.title, it.number, it.cover) },
        isRefreshing = isRefreshing,
        isLoadingEpisode = isLoadingEpisode,
        onRefresh = { viewModel.refreshRecentEpisodes(context) },
        onClickEpisode = { idx ->
            val episode = episodes[idx]
            scope.launch {
                val results = viewModel.searchDirect(episode.title.replace(" ", "%20"))
                val epSlug = "${results.first().slug}-${episode.number}"
                viewModel.onEpisodeSelected(
                    Episode(number = episode.number, slug = epSlug, url = "")
                )
            }
        },
        onInfoEpisode = { idx ->
            val episode = episodes[idx]
            scope.launch {
                val results = viewModel.searchDirect(episode.title.replace(" ", "%20"))
                if (results.isNotEmpty()) {
                    val exact = results.firstOrNull { it.title == episode.title } ?: results.first()
                    onAnimeClick(exact)
                } else {
                    Toast.makeText(context, "No se encontró el anime", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

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

// ----------------------
// Previews
// ----------------------
@Preview(showBackground = true, showSystemUi = false, name = "Recientes - Lista")
@Composable
private fun RecientesScreenPreview_List() {
    val sample = listOf(
        RecentEpisodeListUi("Solo Leveling", 7, "https://animeflv.net/uploads/animes/thumbs/4219.jpg"),
        RecentEpisodeListUi("Tensei Shitara Slime Datta Ken", 12, "https://www3.animeflv.net/uploads/animes/covers/4179.jpg"),
        RecentEpisodeListUi("Jujutsu Kaisen", 3, "https://www3.animeflv.net/uploads/animes/covers/4179.jpg"),
        RecentEpisodeListUi("Frieren", 20, "https://www3.animeflv.net/uploads/animes/covers/4179.jpg"),
    )
    RecientesScreenContent(
        episodes = sample,
        isRefreshing = false,
        isLoadingEpisode = false,
        onRefresh = {},
        onClickEpisode = {},
        onInfoEpisode = {}
    )
}