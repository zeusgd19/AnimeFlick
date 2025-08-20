package com.zeusgd.AnimeFlick.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zeusgd.AnimeFlick.R
import com.zeusgd.AnimeFlick.model.RecentEpisode
import androidx.compose.ui.res.stringResource

// ----------------------
// UI model
// ----------------------
data class RecentEpisodeUi(
    val coverUrl: String,
    val title: String,
    val number: Int
)

// ----------------------
// UI pura
// ----------------------
@Composable
fun RecentEpisodeItemContent(
    item: RecentEpisodeUi,
    isLoading: Boolean,
    episodeLabel: String,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.coverUrl,
            contentDescription = item.title,
            modifier = Modifier
                .size(128.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$episodeLabel ${item.number}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Red
            )
            Text(
                text = item.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
        }

        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Ver info") },
                onClick = {
                    expanded = false
                    onInfoClick()
                }
            )
        }
    }
}

// ----------------------
// Wrapper
// ----------------------
@Composable
fun RecentEpisodeItem(
    episode: RecentEpisode,
    isLoading: Boolean,
    onClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val label = stringResource(R.string.episode)
    RecentEpisodeItemContent(
        item = RecentEpisodeUi(
            coverUrl = "https://www3.animeflv.net/uploads/animes/covers/"+episode.cover.substring(episode.cover.indexOfLast { it.equals('/') } + 1, episode.cover.indexOfLast { it.equals('.') })+".jpg",
            title = episode.title,
            number = episode.number
        ),
        isLoading = isLoading,
        episodeLabel = label,
        onClick = onClick,
        onInfoClick = onInfoClick
    )
}

// ----------------------
// Previews
// ----------------------
@Preview(showBackground = true, name = "RecentEpisode - Normal")
@Composable
private fun RecentEpisodeItemPreview_Normal() {
    RecentEpisodeItemContent(
        item = RecentEpisodeUi(
            coverUrl = "https://placehold.co/300x450",
            title = "Solo Leveling",
            number = 7
        ),
        isLoading = false,
        episodeLabel = "Episodio",
        onClick = {},
        onInfoClick = {}
    )
}

@Preview(showBackground = true, name = "RecentEpisode - Título largo")
@Composable
private fun RecentEpisodeItemPreview_LongTitle() {
    RecentEpisodeItemContent(
        item = RecentEpisodeUi(
            coverUrl = "https://placehold.co/300x450",
            title = "Tensei Shitara Slime Datta Ken: Another Very Long Episode Title For Preview",
            number = 12
        ),
        isLoading = false,
        episodeLabel = "Episodio",
        onClick = {},
        onInfoClick = {}
    )
}

@Preview(showBackground = true, name = "RecentEpisode - Loading (click deshabilitado)")
@Composable
private fun RecentEpisodeItemPreview_Loading() {
    RecentEpisodeItemContent(
        item = RecentEpisodeUi(
            coverUrl = "https://placehold.co/300x450",
            title = "Jujutsu Kaisen",
            number = 3
        ),
        isLoading = true,
        episodeLabel = "Episodio",
        onClick = {},
        onInfoClick = {}
    )
}
