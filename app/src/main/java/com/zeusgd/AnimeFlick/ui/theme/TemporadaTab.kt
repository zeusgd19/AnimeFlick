package com.zeusgd.AnimeFlick.ui.theme

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zeusgd.AnimeFlick.model.Anime
import com.zeusgd.AnimeFlick.model.AnimeSearched
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TemporadaTab(context: Context, viewModel: AnimeViewModel, onAnimeClick: (AnimeSearched) -> Unit) {

    val airingMap = viewModel.airingAnimeByDay
    val selectedDay = viewModel.selectedDay
    val days = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    Column {
        ScrollableTabRow(
            selectedTabIndex = days.indexOf(selectedDay),
            edgePadding = 8.dp
        ) {
            days.forEachIndexed { index, day ->
                Tab(
                    selected = selectedDay == day,
                    onClick = {
                        viewModel.selectedDay = day
                    },
                    text = { Text(day) }
                )
            }
        }

        Crossfade(targetState = selectedDay, label = "day-crossfade") { day ->
            if (viewModel.isLoadingTemporada) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val animes = airingMap[day].orEmpty()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(animes) { airing ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val result = viewModel.searchDirect(
                                            airing.title.replace("Anime", "").trim()
                                        )

                                        if (result.isNotEmpty()) {
                                            if (result.size > 1) {
                                                val newResult = result.filter { animeSearched ->
                                                    animeSearched.slug == airing.slug
                                                }.first()
                                                onAnimeClick(newResult)
                                            } else {
                                                onAnimeClick(result.first())
                                            }
                                        }
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(airing.cover)
                                    .crossfade(true)
                                    .build(),
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