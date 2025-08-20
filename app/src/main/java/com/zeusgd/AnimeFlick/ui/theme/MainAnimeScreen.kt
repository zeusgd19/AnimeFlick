package com.zeusgd.AnimeFlick.ui.theme

import android.content.Context
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zeusgd.AnimeFlick.R
import com.zeusgd.AnimeFlick.Screen
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel
import kotlinx.coroutines.launch
import checkForUpdateFromGitHub
import downloadAndInstall
import getUpdatedInfo

// ----------------------
// UI Model
// ----------------------
data class DrawerItemUi(
    val icon: @Composable (() -> Unit),
    val label: String
)

// ----------------------
// UI Pura
// ----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAnimeScreenContent(
    // Top bar
    showTopBar: Boolean,
    titleText: String,
    isSearching: Boolean,
    searchQuery: String,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    searchPlaceholder: String,

    // Drawer
    drawerItems: List<DrawerItemUi>,
    onDrawerItemClick: (index: Int) -> Unit,
    drawerHeader: @Composable () -> Unit,

    // Update dialog
    showUpdateDialog: Boolean,
    updateInfo: String?,
    onConfirmUpdate: () -> Unit,
    onDismissUpdate: () -> Unit,

    // Bottom bar
    showBottomBar: Boolean,
    bottomBar: @Composable () -> Unit,

    // Content
    content: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = onDismissUpdate,
            title = { Text("Actualización disponible") },
            text = {
                Column {
                    Text("¿Quieres actualizar la app ahora?")
                    Spacer(Modifier.height(8.dp))
                    Text("Novedades:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    updateInfo?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmUpdate) { Text("Actualizar") }
            },
            dismissButton = {
                TextButton(onClick = onDismissUpdate) { Text("Más tarde") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Transparent,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                // Header (slot)
                Box(
                    modifier = Modifier
                        .background(Color.Blue)
                        .height(150.dp)
                        .fillMaxWidth()
                ) { drawerHeader() }

                drawerItems.forEachIndexed { index, item ->
                    NavigationDrawerItem(
                        icon = item.icon,
                        label = { Text(item.label) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onDrawerItemClick(index)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (showTopBar) {
                    TopAppBar(
                        title = {
                            if (isSearching) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text(text = searchPlaceholder) },
                                    singleLine = true
                                )
                            } else {
                                Text(text = titleText)
                            }
                        },
                        actions = {
                            IconButton(onClick = onToggleSearch) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar"
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menú",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (showBottomBar) bottomBar()
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                content()
            }
        }
    }
}

// ----------------------
// Wrapper
// ----------------------
@RequiresApi(TIRAMISU)
@Composable
fun MainAnimeScreen(
    context: Context,
    viewModel: AnimeViewModel,
    window: android.view.Window
) {
    val screen by remember { derivedStateOf { viewModel.currentScreen } }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var apkUrl by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    // Check de actualización al abrir
    LaunchedEffect(Unit) {

        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val url = checkForUpdateFromGitHub(context)
        val updatedInfo = getUpdatedInfo()
        if (url != null) {
            apkUrl = url
            info = updatedInfo
        }
    }

    val selectedAnime = viewModel.selectedAnime
    val showTopBar = screen != Screen.Ajustes && selectedAnime == null
    val showBottomBar = selectedAnime == null

    MainAnimeScreenContent(
        // Top bar
        showTopBar = showTopBar,
        titleText = screen.name,
        isSearching = isSearching,
        searchQuery = searchQuery,
        onToggleSearch = {
            isSearching = !isSearching
            if (!isSearching) {
                searchQuery = ""
                viewModel.search("")
            }
        },
        onSearchQueryChange = {
            searchQuery = it
            viewModel.search(it)
        },
        searchPlaceholder = "Buscar…",

        // Drawer
        drawerItems = listOf(
            DrawerItemUi(
                icon = { Icon(Icons.Default.Star, contentDescription = "Mis Animes") },
                label = "Mis Animes"
            )
        ),
        onDrawerItemClick = { index ->
            when (index) {
                0 -> viewModel.navigateTo(Screen.MisAnime)
            }
        },
        drawerHeader = {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(R.drawable.logo_a)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        },

        // Update dialog
        showUpdateDialog = apkUrl != null,
        updateInfo = info,
        onConfirmUpdate = {
            apkUrl?.let { url -> downloadAndInstall(context, url) }
            apkUrl = null
        },
        onDismissUpdate = { apkUrl = null },

        // Bottom bar
        showBottomBar = showBottomBar,
        bottomBar = {
            AnimeBottomBar(
                currentScreen = screen,
                onScreenSelected = {
                    viewModel.navigateTo(it)
                    searchQuery = ""
                    viewModel.search("")
                }
            )
        },

        // Content (routing)
        content = {
            if (selectedAnime != null) {
                AnimeDetailScreen(
                    context = context,
                    anime = selectedAnime,
                    animeInfo = viewModel.animeInfo!!,
                    onBack = {
                        viewModel.animeInfo = null
                        viewModel.selectedAnime = null
                    },
                    viewModel = viewModel
                )
            } else {
                if (isSearching && searchQuery.isNotBlank()) {
                    BuscarScreen(context, viewModel)
                } else {
                    when (screen) {
                        Screen.Recientes -> RecientesScreen(viewModel) { animeSearched ->
                            viewModel.loadEpisodes(animeSearched)
                        }

                        Screen.Favoritos -> FavoritosScreen(viewModel)
                        Screen.Explorar -> DirectorioScreen(context, viewModel)
                        Screen.Temporada -> TemporadaTab(context, viewModel) { animeSearched ->
                            viewModel.loadEpisodes(animeSearched)
                        }

                        Screen.Ajustes -> AjustesScreen()
                        Screen.MisAnime -> MyAnimeScreen(context, viewModel)
                    }
                }
            }
        }
    )
}

// ----------------------
// Previews
// ----------------------
@Preview(showBackground = true, showSystemUi = true, name = "Main - Lista")
@Composable
private fun MainAnimeScreenPreview_List() {
    MainAnimeScreenContent(
        showTopBar = true,
        titleText = "Recientes",
        isSearching = false,
        searchQuery = "",
        onToggleSearch = {},
        onSearchQueryChange = {},
        searchPlaceholder = "Buscar…",
        drawerItems = listOf(
            DrawerItemUi(
                icon = { Icon(Icons.Default.Star, contentDescription = null) },
                label = "Mis Animes"
            )
        ),
        onDrawerItemClick = {},
        drawerHeader = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF3366FF))
            )
        },
        showUpdateDialog = false,
        updateInfo = null,
        onConfirmUpdate = {},
        onDismissUpdate = {},
        showBottomBar = false,
        bottomBar = {
            // Placeholder simple para preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) { Text("BottomBar") }
        },
        content = {
            // Placeholder de contenido
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) { Text("Contenido de la pantalla") }
        }
    )
}