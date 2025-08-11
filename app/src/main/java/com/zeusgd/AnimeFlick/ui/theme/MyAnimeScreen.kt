package com.zeusgd.AnimeFlick.ui.theme

import UiState
import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zeusgd.AnimeFlick.CompletedAnime
import com.zeusgd.AnimeFlick.CompletedAnimes
import com.zeusgd.AnimeFlick.FavoriteAnime
import com.zeusgd.AnimeFlick.FollowedAnime
import com.zeusgd.AnimeFlick.FollowedAnimes
import com.zeusgd.AnimeFlick.PausedAnime
import com.zeusgd.AnimeFlick.PausedAnimes
import com.zeusgd.AnimeFlick.R
import com.zeusgd.AnimeFlick.model.AnimeSearched
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class Source { Todos,Siguiendo,Completado,EnPausa }
@SuppressLint("FlowOperatorInvokedInComposition")
@Composable
fun MyAnimeScreen(context: Context, viewModel: AnimeViewModel){
    val tabs = listOf("Todos","Siguiendo", "Completado", "En Pausa")
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val currentSource = when (pagerState.currentPage) {
        0 -> Source.Todos
        1 -> Source.Siguiendo
        2 -> Source.Completado
        else -> Source.EnPausa
    }
    val followedFlow = viewModel.followedUiState(context).mapToList { it.toList() }
    val completedFlow = viewModel.completedUiState(context).mapToList { it.toList() }
    val pausedFlow   = viewModel.pausedUiState(context).mapToList { it.toList() }

    val allFlow: Flow<UiState<List<AnimeSearched>>> =
        combine(followedFlow, completedFlow, pausedFlow) { f, c, p ->
            // Si cualquiera está cargando, mostramos loading
            if (f is UiState.Loading || c is UiState.Loading || p is UiState.Loading) {
                UiState.Loading
            } else {
                val lf = (f as? UiState.Success<List<AnimeSearched>>)?.data.orEmpty()
                val lc = (c as? UiState.Success<List<AnimeSearched>>)?.data.orEmpty()
                val lp = (p as? UiState.Success<List<AnimeSearched>>)?.data.orEmpty()
                val merged = (lf + lc + lp).distinctBy { it.slug } // sin duplicados
                UiState.Success(merged)
            }
        }

    val uiState by remember(currentSource) {
        when (currentSource) {
            Source.Todos       -> allFlow
            Source.Siguiendo   -> followedFlow
            Source.Completado  -> completedFlow
            Source.EnPausa     -> pausedFlow
        }
    }.collectAsStateWithLifecycle(initialValue = UiState.Loading)
    val scope = rememberCoroutineScope()

    val list = (uiState as? UiState.Success<List<AnimeSearched>>)?.data.orEmpty()
    val isLoading = uiState is UiState.Loading
    when {
        isLoading -> { /* spinner */ }
        list.isEmpty() -> { /* vacío */ }
        else -> { /* LazyColumn de list */ }
    }

    Column{
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 8.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = { Text(title) }
                )
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                list.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_favorite), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(list) { anime ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.loadEpisodes(anime) },
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(6.dp)
                            ) {
                                Row(Modifier.padding(12.dp)) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(anime.cover).crossfade(true).build(),
                                        contentDescription = anime.title,
                                        modifier = Modifier.size(90.dp).clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(
                                        modifier = Modifier.fillMaxWidth().align(Alignment.CenterVertically)
                                    ) {
                                        Text(anime.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                        Spacer(Modifier.height(4.dp))
                                        Text("${anime.rating}/5", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Flow<UiState<FollowedAnimes>>.asUiStateListFollowed(): Flow<Any> =
    map { s ->
        when (s) {
            UiState.Loading -> UiState.Loading
            is UiState.Success -> UiState.Success(
                s.data.animesList.map { it.toAnimeSearchedFollowed() }
            )

            else -> {}
        }
    }

private fun Flow<UiState<CompletedAnimes>>.asUiStateListCompleted(): Flow<Any> =
    map { s ->
        when (s) {
            UiState.Loading -> UiState.Loading
            is UiState.Success -> UiState.Success(
                s.data.animesList.map { it.toAnimeSearchedCompleted() } // crea extension CompletedAnime.toAnimeSearched()
            )

            else -> {}
        }
    }

private fun Flow<UiState<PausedAnimes>>.asUiStateListPaused(): Flow<Any> =
    map { s ->
        when (s) {
            UiState.Loading -> UiState.Loading
            is UiState.Success -> UiState.Success(
                s.data.animesList.map { it.toAnimeSearchedPaused() } // crea extension PausedAnime.toAnimeSearched()
            )

            else -> {}
        }
    }

fun FollowedAnime.toAnimeSearchedFollowed() = AnimeSearched(title, cover, slug, rating, type)
fun CompletedAnime.toAnimeSearchedCompleted() = AnimeSearched(title, cover, slug, rating, type)
fun PausedAnime.toAnimeSearchedPaused()    = AnimeSearched(title, cover, slug, rating, type)

private fun <T> Flow<UiState<T>>.mapToList(
    transform: (T) -> List<AnimeSearched>
): Flow<Any> =
    map { s ->
        when (s) {
            UiState.Loading -> UiState.Loading
            is UiState.Success -> UiState.Success(transform(s.data))
            else -> {}
        }
    }

private fun FollowedAnimes.toList(): List<AnimeSearched> =
    animesList.map { AnimeSearched(it.title, it.cover, it.slug, it.rating, it.type) }

private fun CompletedAnimes.toList(): List<AnimeSearched> =
    animesList.map { AnimeSearched(it.title, it.cover, it.slug, it.rating, it.type) }

private fun PausedAnimes.toList(): List<AnimeSearched> =
    animesList.map { AnimeSearched(it.title, it.cover, it.slug, it.rating, it.type) }