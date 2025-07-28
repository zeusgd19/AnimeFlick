package com.zeusgd.AnimeFlick.ui.theme

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.zeusgd.AnimeFlick.model.Anime
import com.zeusgd.AnimeFlick.model.AnimeSearched
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AnimeDetailScreen(
    context: Context,
    anime: AnimeSearched,
    animeInfo: Anime,
    onBack: () -> Unit = {},
    viewModel: AnimeViewModel
) {
    val uiState by viewModel.favoritesUiState(context).collectAsState(initial = UiState.Loading)

    val isFavorite = when (uiState) {
        is UiState.Success -> {
            val favoritos = (uiState as UiState.Success).data
            favoritos.animesList.any { it.slug == anime.slug }
        }
        else -> false
    }

    val tabs = listOf("Información", "Episodios")
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(anime.title) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
            },
            actions = {
                IconButton(onClick = {
                    if (isFavorite) {
                        viewModel.removeFavorite(context, anime.slug)
                        Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.addFavorite(context, anime)
                        Toast.makeText(context, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorito",
                        tint = if (isFavorite) Color.Red else Color.Gray
                    )
                }
            }
        )

        val coroutineScope = rememberCoroutineScope()

        TabRow(
            selectedTabIndex = pagerState.currentPage
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> AnimeInfoTab(context,animeInfo, viewModel)
                1 -> EpisodeTab(viewModel)
            }
        }
    }
}