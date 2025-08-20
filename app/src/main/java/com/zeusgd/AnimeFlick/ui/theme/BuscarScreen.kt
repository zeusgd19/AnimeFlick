package com.zeusgd.AnimeFlick.ui.theme

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel

// ----------------------
// UI model
// ----------------------
data class SearchResultUi(
    val title: String,
    val type: String,
    val coverUrl: String?
)

// ----------------------
// Pure UI
// ----------------------
@Composable
fun BuscarScreenContent(
    results: List<SearchResultUi>,
    onClickIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(results) { index, item ->
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
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.type,
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

// ----------------------
// Wrapper
// ----------------------
@Composable
fun BuscarScreen(
    context: Context,
    viewModel: AnimeViewModel
) {
    // Adaptamos tu lista del VM al modelo de UI
    val results = remember(viewModel.animeList) {
        viewModel.animeList.map { a ->
            SearchResultUi(
                title = a.title,
                type = a.type,
                coverUrl = a.cover
            )
        }
    }

    BuscarScreenContent(
        results = results,
        onClickIndex = { idx ->
            // Re-usa el objeto original del VM por índice
            viewModel.animeList.getOrNull(idx)?.let { anime ->
                viewModel.loadEpisodes(anime)
            }
        }
    )
}

// ----------------------
// Previews
// ----------------------
private val previewResults = listOf(
    SearchResultUi("Anime 1", "tv", "https://placehold.co/300x450"),
    SearchResultUi("Anime 2", "tv", "https://placehold.co/300x450"),
    SearchResultUi("Anime 3", "tv", "https://placehold.co/300x450"),
    SearchResultUi("Anime 4", "tv", "https://placehold.co/300x450"),
    SearchResultUi("Anime 5", "tv", "https://placehold.co/300x450")
)

@Preview(showBackground = true, showSystemUi = true, name = "Buscar - Lista")
@Composable
fun BuscarScreenPreview() {
    BuscarScreenContent(
        results = previewResults,
        onClickIndex = { /* no-op in preview */ }
    )
}
