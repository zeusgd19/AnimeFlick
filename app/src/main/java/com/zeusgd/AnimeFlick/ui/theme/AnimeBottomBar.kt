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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeusgd.AnimeFlick.Screen
import com.zeusgd.AnimeFlick.getLabel

@Composable
fun AnimeBottomBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    val items = listOf(
        Screen.Recientes to Icons.Default.Home,
        Screen.Favoritos to Icons.Default.Favorite,
        Screen.Explorar to Icons.Default.List,
        Screen.Temporada to Icons.Default.Info,
        Screen.Ajustes to Icons.Default.Settings
    )

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 0.dp
    ) {
        items.forEach { (screen, icon) ->
            NavigationBarItem(
                selected = currentScreen == screen,
                onClick = { onScreenSelected(screen) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = screen.getLabel(),
                        fontSize = 11.sp
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}