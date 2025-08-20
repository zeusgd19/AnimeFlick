package com.zeusgd.AnimeFlick.ui.theme

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel
import kotlinx.coroutines.launch

// ----------------------
// UI model
// ----------------------
data class DirectoryItemUi(
    val title: String,
    val type: String,
    val coverUrl: String?
)

// ----------------------
// Pure UI
// ----------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DirectorioScreenContent(
    tabs: List<String>,
    selectedTabIndex: Int,
    onSelectTab: (Int) -> Unit,
    getItemsForPage: (Int) -> List<DirectoryItemUi>,
    onClickItem: (pageIndex: Int, itemIndex: Int) -> Unit,
    onNearEnd: (pageIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    nearEndThreshold: Int = 8
) {
    val pagerState = rememberPagerState(
        initialPage = selectedTabIndex,
        pageCount = { tabs.size }
    )
    val scope = rememberCoroutineScope()

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
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onSelectTab(index) },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            val items = getItemsForPage(pageIndex)

            // -------- GRID en vez de lista --------
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                itemsIndexed(items, key = { _, it -> it.title }) { index, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClickItem(pageIndex, index) },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            // Poster
                            AsyncImage(
                                model = item.coverUrl,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(2f / 3f) // 3:2 (vertical)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(Modifier.height(8.dp))
                            // Título
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            // Tipo
                            Text(
                                text = item.type,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }
                    }

                    // near-end para scroll infinito en grid
                    if (index >= items.size - nearEndThreshold) {
                        onNearEnd(pageIndex)
                    }
                }
            }
        }
    }
}


// ----------------------
// Wrapper
// ----------------------
@Composable
fun DirectorioScreen(
    context: Context,
    viewModel: AnimeViewModel
) {
    val tabs = remember { listOf("Anime", "OVA", "Especial", "Película") }
    val types = remember { listOf("tv", "ova", "special", "movie") }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Carga inicial al entrar en cada pestaña
    LaunchedEffect(selectedTab) {
        val type = types[selectedTab]
        if (viewModel.getAnimeList(type).isEmpty()) {
            viewModel.loadMoreAnimes(type)
        }
    }

    DirectorioScreenContent(
        tabs = tabs,
        selectedTabIndex = selectedTab,
        onSelectTab = { selectedTab = it },
        getItemsForPage = { page ->
            val type = types[page]
            viewModel.getAnimeList(type).map { a ->
                DirectoryItemUi(title = a.title, type = a.type, coverUrl = a.cover)
            }
        },
        onClickItem = { page, index ->
            val type = types[page]
            viewModel.getAnimeList(type).getOrNull(index)?.let { anime ->
                viewModel.loadEpisodes(anime)
            }
        },
        onNearEnd = { page ->
            val type = types[page]
            viewModel.loadMoreAnimes(type)
        }
    )
}

// ----------------------
// Previews
// ----------------------
private val previewTabs = listOf("Anime", "OVA", "Especial", "Película")
private val previewPages = listOf(
    List(8) { i -> DirectoryItemUi("Anime #$i", "tv", "https://placehold.co/300x450") },
    List(6) { i -> DirectoryItemUi("OVA #$i", "ova", "https://placehold.co/300x450") },
    List(4) { i -> DirectoryItemUi("Especial #$i", "special", "https://placehold.co/300x450") },
    List(5) { i -> DirectoryItemUi("Película #$i", "movie", "https://placehold.co/300x450") }
)

@Preview(showBackground = true, showSystemUi = true, name = "Directorio - Anime")
@Composable
fun DirectorioScreenPreview_Anime() {
    var tab by remember { mutableIntStateOf(0) }
    DirectorioScreenContent(
        tabs = previewTabs,
        selectedTabIndex = tab,
        onSelectTab = { tab = it },
        getItemsForPage = { previewPages[it] },
        onClickItem = { _, _ -> },
        onNearEnd = { _ -> } // no-op en preview
    )
}