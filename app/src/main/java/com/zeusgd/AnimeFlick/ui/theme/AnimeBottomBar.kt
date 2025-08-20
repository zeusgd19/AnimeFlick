package com.zeusgd.AnimeFlick.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeusgd.AnimeFlick.Screen
import com.zeusgd.AnimeFlick.getLabel

// ----------------------
// UI model
// ----------------------
data class BottomBarItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// ----------------------
// Pure UI
// ----------------------
@Composable
fun AnimeBottomBarContent(
    items: List<BottomBarItem>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 0.dp
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = index == selectedIndex,
                onClick = { onSelectIndex(index) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}

// ----------------------
// Wrapper
// ----------------------
@Composable
fun AnimeBottomBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    val map = listOf(
        Screen.Recientes to BottomBarItem(Screen.Recientes.getLabel(), Icons.Filled.Home),
        Screen.Favoritos to BottomBarItem(Screen.Favoritos.getLabel(), Icons.Filled.Favorite),
        Screen.Explorar to BottomBarItem(Screen.Explorar.getLabel(), Icons.Filled.List),
        Screen.Temporada to BottomBarItem(Screen.Temporada.getLabel(), Icons.Filled.Info),
        Screen.Ajustes to BottomBarItem(Screen.Ajustes.getLabel(), Icons.Filled.Settings),
    )

    val selectedIndex = map.indexOfFirst { it.first == currentScreen }.coerceAtLeast(0)

    AnimeBottomBarContent(
        items = map.map { it.second },
        selectedIndex = selectedIndex,
        onSelectIndex = { idx -> onScreenSelected(map[idx].first) }
    )
}

// ----------------------
// Previews
// ----------------------
private val previewItems = listOf(
    BottomBarItem("Inicio", Icons.Filled.Home),
    BottomBarItem("Favoritos", Icons.Filled.Favorite),
    BottomBarItem("Explorar", Icons.Filled.List),
    BottomBarItem("Temporada", Icons.Filled.Info),
    BottomBarItem("Ajustes", Icons.Filled.Settings),
)

@Preview(showBackground = true, showSystemUi = true, name = "BottomBar - Inicio")
@Composable
fun AnimeBottomBarPreviewHome() {
    var selected by remember { mutableStateOf(0) }
    AnimeBottomBarContent(
        items = previewItems,
        selectedIndex = selected,
        onSelectIndex = { selected = it }
    )
}