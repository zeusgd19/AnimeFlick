package com.zeusgd.AnimeFlick.ui.theme

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zeusgd.AnimeFlick.model.Anime
import com.zeusgd.AnimeFlick.model.AnimeSearched
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel

// ----------------------
// Modelos UI
// ----------------------
data class AnimeDetailInfo(
    val title: String,
    val coverUrl: String?,
    val statusText: String?,
    val ratingText: String?,
    val genres: List<String>,
    val synopsis: String
)

enum class DetailStatus { NONE, WATCHING, COMPLETED, PAUSED }

private val defaultStatusOptions = listOf(
    DetailStatus.NONE to "Ninguno",
    DetailStatus.WATCHING to "Viendo",
    DetailStatus.COMPLETED to "Completado",
    DetailStatus.PAUSED to "En pausa"
)

// ----------------------
// Pure UI
// ----------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimeInfoTabContent(
    info: AnimeDetailInfo,
    selectedStatus: DetailStatus,
    onSelectStatus: (DetailStatus) -> Unit,
    modifier: Modifier = Modifier,
    statusOptions: List<Pair<DetailStatus, String>> = defaultStatusOptions
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = statusOptions.firstOrNull { it.first == selectedStatus }?.second ?: ""

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        AsyncImage(
            model = info.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f / 1f)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(Modifier.height(16.dp))
        Text(info.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        info.statusText?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
        info.ratingText?.let { Text(it, style = MaterialTheme.typography.labelMedium) }

        Spacer(Modifier.height(12.dp))
        Text("Géneros:", style = MaterialTheme.typography.labelLarge)
        if (info.genres.isEmpty()) {
            Text("Sin géneros disponibles", style = MaterialTheme.typography.labelSmall)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                info.genres.forEach {
                    Text(
                        text = it,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Sinopsis", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(info.synopsis, style = MaterialTheme.typography.bodyMedium)

        Box(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                enabled = false,
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Desplegar opciones"
                        )
                    }
                },
                label = { Text("Opción") }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                statusOptions.forEach { (opt, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            expanded = false
                            onSelectStatus(opt)
                        }
                    )
                }
            }
        }
    }
}

// ----------------------
// Wrapper
// ----------------------
@Composable
fun AnimeInfoTab(
    context: Context,
    anime: Anime,
    viewModel: AnimeViewModel,
    animeSearched: AnimeSearched
) {
    val status by viewModel.statusFlow(context, animeSearched.slug)
        .collectAsState(initial = AnimeViewModel.AnimeStatus.None)

    // Mapear estado del VM -> estado de UI
    val uiStatus = when (status) {
        AnimeViewModel.AnimeStatus.None -> DetailStatus.NONE
        AnimeViewModel.AnimeStatus.Watching -> DetailStatus.WATCHING
        AnimeViewModel.AnimeStatus.Completed -> DetailStatus.COMPLETED
        AnimeViewModel.AnimeStatus.Paused -> DetailStatus.PAUSED
    }

    // Sinopsis (con posible traducción)
    var synopsis by remember { mutableStateOf(anime.synopsis) }
    val locale = context.resources.configuration.locales[0]
    LaunchedEffect(anime.title, locale.language) {
        synopsis = if (locale.language != "es") {
            viewModel.translateSinopsis(anime.synopsis, locale.language)
        } else {
            anime.synopsis
        }
    }

    AnimeInfoTabContent(
        info = AnimeDetailInfo(
            title = anime.title,
            coverUrl = anime.cover,
            statusText = "Estado: ${anime.status}",
            ratingText = "Rating: ${anime.rating}",
            genres = anime.genres.orEmpty(),
            synopsis = synopsis
        ),
        selectedStatus = uiStatus,
        onSelectStatus = { sel ->
            val mapped = when (sel) {
                DetailStatus.NONE -> AnimeViewModel.AnimeStatus.None
                DetailStatus.WATCHING -> AnimeViewModel.AnimeStatus.Watching
                DetailStatus.COMPLETED -> AnimeViewModel.AnimeStatus.Completed
                DetailStatus.PAUSED -> AnimeViewModel.AnimeStatus.Paused
            }
            viewModel.setStatus(context, animeSearched, mapped)
        }
    )
}

// ----------------------
// Previews
// ----------------------
@Preview(showBackground = true, showSystemUi = true, name = "InfoTab - Light")
@Composable
fun AnimeInfoTabPreview_Light() {
    var selected by remember { mutableStateOf(DetailStatus.NONE) }
    AnimeInfoTabContent(
        info = AnimeDetailInfo(
            title = "Nombre Anime",
            coverUrl = "https://placehold.co/600x400",
            statusText = "Estado: En emisión",
            ratingText = "Rating: 4.7",
            genres = listOf("Acción", "Aventura", "Fantasía"),
            synopsis = "Lorem ipsum"
        ),
        selectedStatus = selected,
        onSelectStatus = { selected = it }
    )
}