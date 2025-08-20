package com.zeusgd.AnimeFlick.ui.theme

import UiState
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zeusgd.AnimeFlick.FavoriteAnime
import com.zeusgd.AnimeFlick.R
import com.zeusgd.AnimeFlick.model.AnimeSearched
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel

// ----------------------
// UI Model
// ----------------------
data class FavoriteItemUi(
    val title: String,
    val ratingText: String?,     // ej. "4.8/5"
    val coverUrl: String?
)

// ----------------------
// Pure UI
// ----------------------
@Composable
fun FavoritosScreenContent(
    items: List<FavoriteItemUi>,
    isLoading: Boolean,
    emptyMessage: String,
    onClickIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emptyMessage, style = MaterialTheme.typography.bodyLarge)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(items) { index, item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClickIndex(index) },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                AsyncImage(
                                    model = item.coverUrl,
                                    contentDescription = item.title,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.CenterVertically)
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
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
fun FavoritosScreen(viewModel: AnimeViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.favoritesUiState(context).collectAsState(initial = UiState.Loading)

    val emptyText = stringResource(R.string.no_favorite)

    when (uiState) {
        is UiState.Loading -> {
            FavoritosScreenContent(
                items = emptyList(),
                isLoading = true,
                emptyMessage = emptyText,
                onClickIndex = {}
            )
        }

        is UiState.Success -> {
            val favoritos = (uiState as UiState.Success).data
            val items = remember(favoritos.animesList) {
                favoritos.animesList.map { f ->
                    FavoriteItemUi(
                        title = f.title,
                        ratingText = "${f.rating}/5",
                        coverUrl = f.cover
                    )
                }
            }
            FavoritosScreenContent(
                items = items,
                isLoading = false,
                emptyMessage = emptyText,
                onClickIndex = { idx ->
                    favoritos.animesList.getOrNull(idx)?.let { fav ->
                        viewModel.loadEpisodes(fav.toAnimeSearched())
                    }
                }
            )
        }

        else -> {
            // Estado de error u otros -> muestra vacío
            FavoritosScreenContent(
                items = emptyList(),
                isLoading = false,
                emptyMessage = emptyText,
                onClickIndex = {}
            )
        }
    }
}

// ----------------------
// Helper
// ----------------------
fun FavoriteAnime.toAnimeSearched(): AnimeSearched {
    return AnimeSearched(
        title = title,
        cover = cover,
        slug = slug,
        rating = rating,
        type = type
    )
}

// ----------------------
// Previews
// ----------------------
@Preview(showBackground = true, showSystemUi = true, name = "Favoritos - Vacío")
@Composable
fun FavoritosPreview_Empty() {
    FavoritosScreenContent(
        items = emptyList(),
        isLoading = false,
        emptyMessage = "Mensaje no animes favoritos.",
        onClickIndex = {}
    )
}

@Preview(showBackground = true, showSystemUi = true, name = "Favoritos - Lista")
@Composable
fun FavoritosPreview_List() {
    val sample = listOf(
        FavoriteItemUi("Anime 1", "4.8/5", "https://placehold.co/300x450"),
        FavoriteItemUi("Anime 2", "4.7/5", "https://placehold.co/300x450"),
        FavoriteItemUi("Anime 3", "4.6/5", "https://placehold.co/300x450")
    )
    FavoritosScreenContent(
        items = sample,
        isLoading = false,
        emptyMessage = "",
        onClickIndex = {}
    )
}
