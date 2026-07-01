package com.zeusgd.AnimeFlick.ui.theme

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 50f
                        )
                    )
            )

            // Episode Badge
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE50914))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "$episodeLabel ${item.number}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Options menu
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
            ) {
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Más opciones",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
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
            
            // Title
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
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
    val label = "Ep" // Abreviado para el badge
    RecentEpisodeItemContent(
        item = RecentEpisodeUi(
            coverUrl = episode.cover,
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
            coverUrl = "https://placehold.co/1920x1080",
            title = "Solo Leveling",
            number = 7
        ),
        isLoading = false,
        episodeLabel = "Ep",
        onClick = {},
        onInfoClick = {}
    )
}
