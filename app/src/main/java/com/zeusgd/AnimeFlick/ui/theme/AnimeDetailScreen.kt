package com.zeusgd.AnimeFlick.ui.theme

import android.app.Activity
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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.zeusgd.AnimeFlick.model.Anime
import com.zeusgd.AnimeFlick.model.AnimeSearched
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel
import kotlinx.coroutines.launch

// ----------------------
// Pure UI
// ----------------------
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AnimeDetailScreenContent(
    title: String,
    tabs: List<String>,
    selectedTabIndex: Int,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSelectTab: (Int) -> Unit,
    infoContent: @Composable () -> Unit,
    episodesContent: @Composable () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = selectedTabIndex,
        pageCount = { tabs.size }
    )
    val scope = rememberCoroutineScope()

    // Sincroniza cambios externos del índice con el pager
    LaunchedEffect(selectedTabIndex) {
        if (pagerState.currentPage != selectedTabIndex) {
            scope.launch { pagerState.animateScrollToPage(selectedTabIndex) }
        }
    }
    // Notifica cambios al arrastrar el pager
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedTabIndex) {
            onSelectTab(pagerState.currentPage)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
            },
            actions = {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorito",
                        tint = if (isFavorite) Color.Red else Color.Gray
                    )
                }
            }
        )

        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, tabTitle ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onSelectTab(index) },
                    text = { Text(tabTitle) }
                )
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> infoContent()
                1 -> episodesContent()
            }
        }
    }
}

// ----------------------
// Wrapper
// ----------------------
@OptIn(ExperimentalFoundationApi::class)
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

    val tabs = remember { listOf("Información", "Episodios") }
    var selectedTab by remember { mutableIntStateOf(0) }

    AnimeDetailScreenContent(
        title = anime.title,
        tabs = tabs,
        selectedTabIndex = selectedTab,
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = {
            if (isFavorite) {
                viewModel.removeFavorite(context, anime.slug)
                Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.addFavorite(context, anime)
                Toast.makeText(context, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
            }
            // Si quieres feedback visual inmediato, puedes forzar a cambiar tab o similar aquí
            (context as? Activity)?.let { /* opcional: it.recreate() si procede */ }
        },
        onSelectTab = { selectedTab = it },
        infoContent = { AnimeInfoTab(context, animeInfo, viewModel, anime) },
        episodesContent = { EpisodeTab(viewModel) }
    )
}

// ----------------------
// Previews
// ----------------------
@Preview(showBackground = true, showSystemUi = true, name = "Detalle - Info (no fav)")
@Composable
fun AnimeDetailPreview_Info() {
    var tab by remember { mutableIntStateOf(0) }
    AnimeDetailScreenContent(
        title = "Tensei Shitara Slime Datta Ken",
        tabs = listOf("Información", "Episodios"),
        selectedTabIndex = tab,
        isFavorite = false,
        onBack = {},
        onToggleFavorite = { /* preview */ },
        onSelectTab = { tab = it },
        infoContent = { Text("Descripción, estado, rating, géneros…") },
        episodesContent = { Text("Lista de episodios fake para preview") }
    )
}