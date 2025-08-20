package com.zeusgd.AnimeFlick.ui.theme

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zeusgd.AnimeFlick.model.AnimeSearched
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ----------------------
// UI model
// ----------------------
data class AiringAnimeUi(
    val title: String,
    val coverUrl: String?,
    val slug: String? = null
)

// ----------------------
// UI Pura
// ----------------------
@Composable
fun TemporadaTabContent(
    days: List<String>,
    selectedDayIndex: Int,
    onSelectDay: (Int) -> Unit,
    isLoading: Boolean,
    itemsForDay: (Int) -> List<AiringAnimeUi>,
    onClickItem: (dayIndex: Int, itemIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeIndex = selectedDayIndex.coerceIn(0, days.lastIndex)

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = safeIndex,
            edgePadding = 8.dp
        ) {
            days.forEachIndexed { index, day ->
                Tab(
                    selected = safeIndex == index,
                    onClick = { onSelectDay(index) },
                    text = { Text(day) }
                )
            }
        }

        Crossfade(targetState = Pair(safeIndex, isLoading), label = "day-crossfade") { (idx, loading) ->
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else {
                val items = itemsForDay(idx)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    itemsIndexed(items) { itemIndex, airing ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClickItem(idx, itemIndex) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = airing.coverUrl,
                                contentDescription = airing.title,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = airing.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------
// WRAPPER
// ----------------------
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TemporadaTab(
    context: Context,
    viewModel: AnimeViewModel,
    onAnimeClick: (AnimeSearched) -> Unit
) {
    val days = remember { listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo") }
    val selectedDay = viewModel.selectedDay
    var selectedIndex by remember(selectedDay) {
        mutableIntStateOf(days.indexOf(selectedDay).let { if (it >= 0) it else 0 })
    }

    val airingMap = viewModel.airingAnimeByDay
    val isLoading = viewModel.isLoadingTemporada
    val scope = rememberCoroutineScope()

    TemporadaTabContent(
        days = days,
        selectedDayIndex = selectedIndex,
        onSelectDay = { idx ->
            selectedIndex = idx
            viewModel.selectedDay = days[idx]
        },
        isLoading = isLoading,
        itemsForDay = { idx ->
            val dayKey = days[idx]
            airingMap[dayKey].orEmpty().map { airing ->
                AiringAnimeUi(
                    title = airing.title,
                    coverUrl = airing.cover,
                    slug = airing.slug
                )
            }
        },
        onClickItem = { idx, itemIdx ->
            val dayKey = days[idx]
            val airing = airingMap[dayKey].orEmpty().getOrNull(itemIdx) ?: return@TemporadaTabContent
            scope.launch(Dispatchers.Main) {
                val query = airing.title.replace("Anime", "").trim().replace(" ", "%20")
                val result = viewModel.searchDirect(query)
                if (result.isNotEmpty()) {
                    val chosen = if (result.size > 1) {
                        result.firstOrNull { it.slug == airing.slug } ?: result.first()
                    } else {
                        result.first()
                    }
                    onAnimeClick(chosen)
                }
            }
        }
    )
}

// ----------------------
// PREVIEWS
// ----------------------
@Preview(showBackground = true, showSystemUi = true, name = "Temporada - Loading")
@Composable
private fun TemporadaTabPreview_Loading() {
    val days = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    TemporadaTabContent(
        days = days,
        selectedDayIndex = 0,
        onSelectDay = {},
        isLoading = true,
        itemsForDay = { emptyList() },
        onClickItem = { _, _ -> }
    )
}

@Preview(showBackground = true, showSystemUi = true, name = "Temporada - Con datos")
@Composable
private fun TemporadaTabPreview_Data() {
    val days = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    val fakeDayLists = listOf(
        List(6) { i -> AiringAnimeUi("Solo Leveling #$i", "https://placehold.co/160x220") },
        List(4) { i -> AiringAnimeUi("Slime Datta Ken #$i", "https://placehold.co/160x220") },
        List(5) { i -> AiringAnimeUi("Frieren #$i", "https://placehold.co/160x220") },
        List(3) { i -> AiringAnimeUi("Jujutsu Kaisen #$i", "https://placehold.co/160x220") },
        List(2) { i -> AiringAnimeUi("Kimetsu no Yaiba #$i", "https://placehold.co/160x220") },
        List(1) { _ -> AiringAnimeUi("Made in Abyss", "https://placehold.co/160x220") },
        emptyList()
    )
    var selected by remember { mutableIntStateOf(1) }

    TemporadaTabContent(
        days = days,
        selectedDayIndex = selected,
        onSelectDay = { selected = it },
        isLoading = false,
        itemsForDay = { idx -> fakeDayLists[idx] },
        onClickItem = { _, _ -> }
    )
}
