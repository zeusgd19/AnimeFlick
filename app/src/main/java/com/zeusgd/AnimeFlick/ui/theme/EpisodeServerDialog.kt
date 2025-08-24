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
    episode: Episode,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val defaultServers = listOf(
        ServerUi("YourUpload"),
        ServerUi("Stape"),
        ServerUi("Okru"),
        ServerUi("SW"),
        ServerUi("Mega")
    )

    EpisodeServerDialogContent(
        servers = defaultServers,
        onDismiss = onDismiss,
        onSelect = { onSelect(it.name) },
        title = "Elegir servidor"
    )
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
