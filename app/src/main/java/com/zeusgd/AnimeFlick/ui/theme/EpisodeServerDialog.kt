package com.zeusgd.AnimeFlick.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zeusgd.AnimeFlick.model.Episode

@Composable
fun EpisodeServerDialog(
    episode: Episode,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Elegir servidor") },
        text = {
            Column {
                listOf("YourUpload", "Stape").forEach { server ->
                    Button(
                        onClick = { onSelect(server) },
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