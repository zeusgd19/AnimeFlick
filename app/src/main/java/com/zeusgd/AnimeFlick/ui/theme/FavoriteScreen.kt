package com.zeusgd.AnimeFlick.ui.theme

import UiState
import android.util.Log
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.zeusgd.AnimeFlick.ui.auth.AuthViewModel
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(items) { index, item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClickIndex(index) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AsyncImage(
                                    model = item.coverUrl,
                                    contentDescription = item.title,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f/4f)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
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
fun FavoritosScreen(viewModel: AnimeViewModel, authViewModel: AuthViewModel) {
    val context = LocalContext.current

    val emptyText = stringResource(R.string.no_favorite)

    val uiState by viewModel.favoritesUiState(context).collectAsState(initial = UiState.Loading)

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
            val favoritos = (uiState as UiState.Success).data.animesList
            val items = remember(favoritos) {
                favoritos.map { f ->
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
                    favoritos.getOrNull(idx)?.let { fav ->
                        viewModel.loadEpisodes(fav.toAnimeSearched())
                    }
                }
            )
        }

        else -> {
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
