package com.zeusgd.AnimeFlick.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zeusgd.AnimeFlick.model.Episode
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment

// ----------------------
// UI Model
// ----------------------
data class ServerUi(val name: String)

// ----------------------
// Pure UI
// ----------------------
@Composable
fun EpisodeServerDialogContent(
    servers: List<ServerUi>,
    onDismiss: () -> Unit,
    onSelect: (ServerUi) -> Unit,
    title: String = "Elegir servidor"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(title) },
        text = {
            Column {
                servers.forEach { server ->
                    Button(
                        onClick = { onSelect(server) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(server.name)
                    }
                }
            }
        }
    )
}

// ----------------------
// Wrapper
// ----------------------
@Composable
fun EpisodeServerDialog(
    servers: List<String>?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    if (isLoading) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {},
            title = { Text("Elegir servidor") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        )
    } else if (servers.isNullOrEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {},
            title = { Text("Elegir servidor") },
            text = { Text("No se encontraron servidores disponibles.") }
        )
    } else {
        EpisodeServerDialogContent(
            servers = servers.map { ServerUi(it) },
            onDismiss = onDismiss,
            onSelect = { onSelect(it.name) },
            title = "Elegir servidor"
        )
    }
}

// ----------------------
// Previews
// ----------------------
@Preview(showBackground = true, name = "Dialog - Lista estándar")
@Composable
private fun EpisodeServerDialogPreview() {
    EpisodeServerDialogContent(
        servers = listOf(
            ServerUi("YourUpload"),
            ServerUi("Stape"),
            ServerUi("Okru"),
            ServerUi("SW"),
            ServerUi("Mega")
        ),
        onDismiss = {},
        onSelect = {}
    )
}
