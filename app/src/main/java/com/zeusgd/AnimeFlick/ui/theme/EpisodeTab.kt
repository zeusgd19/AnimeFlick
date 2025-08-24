package com.zeusgd.AnimeFlick.ui.theme

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zeusgd.AnimeFlick.VideoPlayerActivity
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel
import java.util.Locale

// ----------------------
// UI Model
// ----------------------
data class EpisodeUi(
    val slug: String,
    val number: Int,
    val seen: Boolean
)

// ----------------------
// Pure UI
// ----------------------
@Composable
fun EpisodeTabContent(
    episodes: List<EpisodeUi>,
    isLoading: Boolean,
    isReversed: Boolean,
    onToggleOrder: () -> Unit,
    onClickEpisode: (index: Int) -> Unit,
    serverDialogIndex: Int?,                 // índice del episodio con diálogo abierto; null si cerrado
    servers: List<String>,
    onSelectServer: (server: String) -> Unit,
    onDismissServerDialog: () -> Unit,
    videoOptions: List<String>?,             // null = cerrado; lista = abierto
    onSelectVideoQuality: (index: Int) -> Unit,
    onDismissVideoOptions: () -> Unit,
    errorMessage: String?,                   // para snackbars
    onConsumeError: () -> Unit,
    episodeLabel: String = "Episodio"        // texto pre-localizado
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar snackbar en cuanto llegue un error y ¡consumirlo!
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onConsumeError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onToggleOrder) {
                    Icon(
                        imageVector = Icons.Filled.SwapVert,
                        contentDescription = "Invertir orden",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(episodes) { index, ep ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isLoading) { onClickEpisode(index) }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        val label = episodeLabel.lowercase(Locale.ROOT)
                            .replaceFirstChar { it.uppercaseChar() }
                        Text(
                            text = "$label ${ep.number}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (ep.seen) Color.Red else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Overlay de carga
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Cargando episodio...", color = Color.White)
                    }
                }
            }

            // Diálogo de servidores
            serverDialogIndex?.let {
                AlertDialog(
                    onDismissRequest = onDismissServerDialog,
                    confirmButton = {},
                    title = { Text("Elegir servidor") },
                    text = {
                        Column {
                            servers.forEach { server ->
                                Button(
                                    onClick = { onSelectServer(server) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(server)
                                }
                            }
                        }
                    }
                )
            }

            // Diálogo de calidades
            videoOptions?.let { opts ->
                AlertDialog(
                    onDismissRequest = onDismissVideoOptions,
                    confirmButton = {},
                    title = { Text("Elige calidad") },
                    text = {
                        Column {
                            opts.forEachIndexed { idx, q ->
                                Button(
                                    onClick = { onSelectVideoQuality(idx) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(q)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

// ----------------------
// Wrapper
// ----------------------
@Composable
fun EpisodeTab(viewModel: AnimeViewModel) {
    val context = LocalContext.current

    // Orden actual
    var isReversed by remember { mutableStateOf(false) }
    val baseList = viewModel.episodeList
    val listForUi = if (isReversed) baseList.asReversed() else baseList

    // Episodios vistos (una sola colección, no por item)
    val seenEpisodes by viewModel.isEpisodeSeenFlow(context)
        .collectAsState(initial = emptySet())

    // Lista UI
    val episodesUi = remember(listForUi, seenEpisodes) {
        listForUi.map { e -> EpisodeUi(e.slug, e.number, e.slug in seenEpisodes) }
    }

    // Error
    val errorMessage = viewModel.errorMessage

    // Diálogo de servidores: se abre si hay selectedEpisode
    val selected = viewModel.selectedEpisode
    val serverDialogIndex = selected?.let { sel ->
        listForUi.indexOfFirst { it.slug == sel.slug }.takeIf { it >= 0 }
    }

    // Opciones de vídeo (si no es null => abrir)
    val videoOptionsState = viewModel.videoOptions.value
    val videoQualities = videoOptionsState?.map { it.quality }

    // Label localizable para "Episodio"
    val episodeLabel = "Episodio"

    EpisodeTabContent(
        episodes = episodesUi,
        isLoading = viewModel.isLoadingEpisode,
        isReversed = isReversed,
        onToggleOrder = { isReversed = !isReversed },
        onClickEpisode = { idx ->
            val e = listForUi[idx]
            if (e.slug !in seenEpisodes) {
                viewModel.onEpisodeSelected(e)
                viewModel.markEpisodeSeen(context, e.slug)
            } else {
                viewModel.unmarkEpisodeSeen(context, e.slug)
            }
        },
        serverDialogIndex = serverDialogIndex,
        servers = listOf("YourUpload", "Stape", "Okru", "SW", "Mega"),
        onSelectServer = { server ->
            viewModel.clearSelectedEpisode()
            // Dispara la carga de opciones de vídeo para ese server
            selected?.let { ep ->
                viewModel.onEpisodeClick(context, ep.slug, server)
            }
        },
        onDismissServerDialog = { viewModel.clearSelectedEpisode() },
        videoOptions = videoQualities,
        onSelectVideoQuality = { optIndex ->
            videoOptionsState?.getOrNull(optIndex)?.let { option ->
                viewModel.clearVideoOptions()
                val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                    putExtra("videoUrl", option.url)
                    putExtra("headers", HashMap(option.headers))
                }
                context.startActivity(intent)
            }
        },
        onDismissVideoOptions = { viewModel.clearVideoOptions() },
        errorMessage = errorMessage,
        onConsumeError = { viewModel.clearError() },
        episodeLabel = episodeLabel.toString()
    )
}

// ----------------------
// Previews
// ----------------------
@Preview(showBackground = true, showSystemUi = true, name = "Episodes - Normal")
@Composable
fun EpisodeTabPreview_Normal() {
    val sample = List(12) { i ->
        EpisodeUi(slug = "ep-$i", number = i + 1, seen = i % 3 == 0)
    }
    EpisodeTabContent(
        episodes = sample,
        isLoading = false,
        isReversed = false,
        onToggleOrder = {},
        onClickEpisode = {},
        serverDialogIndex = null,
        servers = listOf("YourUpload", "Stape", "Okru", "SW", "Mega"),
        onSelectServer = {},
        onDismissServerDialog = {},
        videoOptions = null,
        onSelectVideoQuality = {},
        onDismissVideoOptions = {},
        errorMessage = null,
        onConsumeError = {},
        episodeLabel = "Episodio"
    )
}

@Preview(showBackground = true, showSystemUi = true, name = "Episodes - Loading + Dialogs")
@Composable
fun EpisodeTabPreview_Loading_Dialogs() {
    val sample = List(5) { i ->
        EpisodeUi(slug = "ep-$i", number = i + 1, seen = i == 0)
    }
    EpisodeTabContent(
        episodes = sample,
        isLoading = true,
        isReversed = true,
        onToggleOrder = {},
        onClickEpisode = {},
        serverDialogIndex = 1,
        servers = listOf("YourUpload", "Stape", "Okru", "SW", "Mega"),
        onSelectServer = {},
        onDismissServerDialog = {},
        videoOptions = listOf("1080p", "720p", "480p"),
        onSelectVideoQuality = {},
        onDismissVideoOptions = {},
        errorMessage = "Error de ejemplo",
        onConsumeError = {},
        episodeLabel = "Episodio"
    )
}
