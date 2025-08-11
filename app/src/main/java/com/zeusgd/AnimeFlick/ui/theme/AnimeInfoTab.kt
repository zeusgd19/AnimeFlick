package com.zeusgd.AnimeFlick.ui.theme

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
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
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zeusgd.AnimeFlick.model.Anime
import com.zeusgd.AnimeFlick.model.AnimeSearched
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimeInfoTab(context: Context,anime: Anime, viewModel: AnimeViewModel, animeSearched: AnimeSearched) {

    val status by viewModel.statusFlow(context, animeSearched.slug)
        .collectAsState(initial = AnimeViewModel.AnimeStatus.None)
    var sinopsisTraducida by remember { mutableStateOf(anime.synopsis) }
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = when (status) {
        AnimeViewModel.AnimeStatus.None      -> "Ninguno"
        AnimeViewModel.AnimeStatus.Watching  -> "Viendo"
        AnimeViewModel.AnimeStatus.Completed -> "Completado"
        AnimeViewModel.AnimeStatus.Paused    -> "En pausa"
    }

    Column(modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(16.dp)) {

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(anime.cover)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f / 1f)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(anime.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Estado: ${anime.status}", style = MaterialTheme.typography.labelMedium)
        Text(text = "Rating: ${anime.rating}", style = MaterialTheme.typography.labelMedium)

        Spacer(modifier = Modifier.height(12.dp))
        Text("Géneros:", style = MaterialTheme.typography.labelLarge)
        val genres = anime.genres.orEmpty()
        if (genres.isEmpty()) {
            Text("Sin géneros disponibles", style = MaterialTheme.typography.labelSmall)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genres.forEach {
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

        Spacer(modifier = Modifier.height(16.dp))
        Text("Sinopsis", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))

        val locale = context.resources.configuration.locales[0]
        if (locale.language != "es") {
            LaunchedEffect(anime.title, locale.language) {
                sinopsisTraducida = viewModel.translateSinopsis(anime.synopsis, locale.language)
            }
        }
        Text(sinopsisTraducida, style = MaterialTheme.typography.bodyMedium)

        Box(
            modifier = Modifier
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = { },
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
                @Composable
                fun pick(s: AnimeViewModel.AnimeStatus, label: String) = DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        viewModel.setStatus(context, animeSearched, s)
                    }
                )

                pick(AnimeViewModel.AnimeStatus.None,      "Ninguno")
                pick(AnimeViewModel.AnimeStatus.Watching,  "Viendo")
                pick(AnimeViewModel.AnimeStatus.Completed, "Completado")
                pick(AnimeViewModel.AnimeStatus.Paused,    "En pausa")
            }
        }
    }
}