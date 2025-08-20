package com.zeusgd.AnimeFlick.ui.theme

import UiState
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zeusgd.AnimeFlick.*
import com.zeusgd.AnimeFlick.model.AnimeSearched
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.zeusgd.AnimeFlick.R

// ------------------------------------
//  UI Model
// ------------------------------------
data class AnimeCardUi(
    val title: String,
    val ratingText: String?,
    val coverUrl: String?
)

private fun AnimeSearched.toUi() =
    AnimeCardUi(title = title, ratingText = "${rating}/5", coverUrl = cover)

enum class Source { Todos, Siguiendo, Completado, EnPausa }

// ------------------------------------
//  UI Pura
// ------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAnimeScreenContent(
    tabs: List<String>,
    selectedTabIndex: Int,
    onSelectTab: (Int) -> Unit,
    stateForPage: (Int) -> UiState<List<AnimeCardUi>>,
    onClickItem: (pageIndex: Int, itemIndex: Int) -> Unit,
    emptyMessage: String,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = selectedTabIndex,
        pageCount = { tabs.size }
    )
    val scope = rememberCoroutineScope()

    // Mantener Tab <-> Pager sincronizados
    LaunchedEffect(selectedTabIndex) {
        if (pagerState.currentPage != selectedTabIndex) {
            scope.launch { pagerState.animateScrollToPage(selectedTabIndex) }
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedTabIndex) {
            onSelectTab(pagerState.currentPage)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 8.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onSelectTab(index) },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (val state = stateForPage(page)) {
                UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Success -> {
                    val list = state.data
                    if (list.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(emptyMessage, style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(list) { index, item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onClickItem(page, index) },
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(6.dp)
                                ) {
                                    Row(Modifier.padding(12.dp)) {
                                        AsyncImage(
                                            model = item.coverUrl,
                                            contentDescription = item.title,
                                            modifier = Modifier
                                                .size(90.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.CenterVertically)
                                        ) {
                                            Text(
                                                item.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 2
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            item.ratingText?.let {
                                                Text(
                                                    it,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Si tienes estado Error, puedes representarlo aquí
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(emptyMessage, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

// ------------------------------------
//  Wrapper
// ------------------------------------
@SuppressLint("FlowOperatorInvokedInComposition")
@Suppress("FunctionName")
@Composable
fun MyAnimeScreen(
    context: Context,
    viewModel: AnimeViewModel
) {
    val tabs = remember { listOf("Todos", "Siguiendo", "Completado", "En Pausa") }

    // ---- Flows de dominio -> List<AnimeSearched>
    val followedFlow: Flow<UiState<List<AnimeSearched>>> =
        viewModel.followedUiState(context).mapToSearched { it.toListFollowed() }

    val completedFlow: Flow<UiState<List<AnimeSearched>>> =
        viewModel.completedUiState(context).mapToSearched { it.toListCompleted() }

    val pausedFlow: Flow<UiState<List<AnimeSearched>>> =
        viewModel.pausedUiState(context).mapToSearched { it.toListPaused() }

    val allFlow: Flow<UiState<List<AnimeSearched>>> =
        combine(followedFlow, completedFlow, pausedFlow) { f, c, p ->
            // Si cualquiera está cargando -> Loading
            if (f is UiState.Loading || c is UiState.Loading || p is UiState.Loading) {
                UiState.Loading
            } else {
                val lf = (f as? UiState.Success<List<AnimeSearched>>)?.data.orEmpty()
                val lc = (c as? UiState.Success<List<AnimeSearched>>)?.data.orEmpty()
                val lp = (p as? UiState.Success<List<AnimeSearched>>)?.data.orEmpty()
                val merged = (lf + lc + lp).distinctBy { it.slug }
                UiState.Success(merged)
            }
        }

    // Estado de pestaña seleccionada
    var selectedTab by remember { mutableIntStateOf(0) }

    // Colecciones por pestaña (dominio)
    val stateTodos by allFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
    val stateFollowed by followedFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
    val stateCompleted by completedFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
    val statePaused by pausedFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)

    // Mapear a UI para la Content
    fun UiState<List<AnimeSearched>>.toUi(): UiState<List<AnimeCardUi>> =
        when (this) {
            UiState.Loading -> UiState.Loading
            is UiState.Success -> UiState.Success(this.data.map { it.toUi() })
            else -> this as UiState<List<AnimeCardUi>> // por si tienes más estados (Error)
        }

    val stateForPage: (Int) -> UiState<List<AnimeCardUi>> = { page ->
        when (page) {
            0 -> stateTodos.toUi()
            1 -> stateFollowed.toUi()
            2 -> stateCompleted.toUi()
            else -> statePaused.toUi()
        }
    }

    val emptyMsg = stringResource(R.string.no_favorite)

    MyAnimeScreenContent(
        tabs = tabs,
        selectedTabIndex = selectedTab,
        onSelectTab = { selectedTab = it },
        stateForPage = stateForPage,
        onClickItem = { page, index ->
            val domainList = when (page) {
                0 -> (stateTodos as? UiState.Success)?.data
                1 -> (stateFollowed as? UiState.Success)?.data
                2 -> (stateCompleted as? UiState.Success)?.data
                else -> (statePaused as? UiState.Success)?.data
            }.orEmpty()

            domainList.getOrNull(index)?.let { anime ->
                viewModel.loadEpisodes(anime)
            }
        },
        emptyMessage = emptyMsg
    )
}

// ------------------------------------
//  HELPERS de mapeo/transformación
// ------------------------------------
private fun <T> Flow<UiState<T>>.mapToSearched(
    transform: (T) -> List<AnimeSearched>
): Flow<UiState<List<AnimeSearched>>> =
    map { s ->
        when (s) {
            UiState.Loading -> UiState.Loading
            is UiState.Success -> UiState.Success(transform(s.data))
            else -> s as UiState<List<AnimeSearched>>
        }
    }

private fun FollowedAnimes.toListFollowed(): List<AnimeSearched> =
    animesList.map { it.toAnimeSearchedFollowed() }

private fun CompletedAnimes.toListCompleted(): List<AnimeSearched> =
    animesList.map { it.toAnimeSearchedCompleted() }

private fun PausedAnimes.toListPaused(): List<AnimeSearched> =
    animesList.map { it.toAnimeSearchedPaused() }

// Mantengo tus extensiones originales:
fun FollowedAnime.toAnimeSearchedFollowed() = AnimeSearched(title, cover, slug, rating, type)
fun CompletedAnime.toAnimeSearchedCompleted() = AnimeSearched(title, cover, slug, rating, type)
fun PausedAnime.toAnimeSearchedPaused() = AnimeSearched(title, cover, slug, rating, type)

@Preview(showBackground = true, showSystemUi = true, name = "MyAnime - Lista/Loading/Vacío")
@Composable
fun MyAnimeScreenPreview_Mixed() {
    MaterialTheme {
        Surface {
            var selectedTab by remember { mutableIntStateOf(0) }

            val tabs = listOf("Todos", "Siguiendo", "Completado", "En Pausa")

            // Datos de ejemplo
            val listTodos = listOf(
                AnimeCardUi("Solo Leveling", "4.8/5", "https://placehold.co/300x450"),
                AnimeCardUi("Slime Datta Ken", "4.7/5", "https://placehold.co/300x450"),
                AnimeCardUi("Jujutsu Kaisen", "4.6/5", "https://placehold.co/300x450"),
            )
            val listFollowed = listOf(
                AnimeCardUi("Frieren", "4.9/5", "https://placehold.co/300x450"),
                AnimeCardUi("Kimetsu no Yaiba", "4.6/5", "https://placehold.co/300x450")
            )

            // Estado por pestaña: 0 lista, 1 lista, 2 loading, 3 vacío
            val stateForPage: (Int) -> UiState<List<AnimeCardUi>> = { page ->
                when (page) {
                    0 -> UiState.Success(listTodos)
                    1 -> UiState.Success(listFollowed)
                    2 -> UiState.Loading
                    else -> UiState.Success(emptyList())
                }
            }

            MyAnimeScreenContent(
                tabs = tabs,
                selectedTabIndex = selectedTab,
                onSelectTab = { selectedTab = it },
                stateForPage = stateForPage,
                onClickItem = { _, _ -> /* no-op preview */ },
                emptyMessage = "Sin animes en esta sección"
            )
        }
    }
}