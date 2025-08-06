package com.zeusgd.AnimeFlick.viewmodel

import UiState
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeflick.datastore.favoritesDataStore
import com.example.animeflick.datastore.seenEpisodesDataStore
import com.zeusgd.AnimeFlick.*
import com.zeusgd.AnimeFlick.model.*
import com.zeusgd.AnimeFlick.network.AnimeApiService
import com.zeusgd.AnimeFlick.network.RetrofitInstance
import com.zeusgd.AnimeFlick.network.RetrofitInstance.api
import com.zeusgd.AnimeFlick.network.RetrofitInstance.apiVercel
import com.zeusgd.AnimeFlick.network.RetrofitInstance.translateApi
import getAiringAnimesGroupedByWeekday
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class AnimeViewModel(
    private val savedState: SavedStateHandle
) : ViewModel() {

    var animeList by mutableStateOf<List<AnimeSearched>>(emptyList())
    var episodeList by mutableStateOf<List<Episode>>(emptyList())
    var animeInfo by mutableStateOf<Anime?>(null)
    var selectedAnime by mutableStateOf<AnimeSearched?>(null)
    var selectedEpisode by mutableStateOf<Episode?>(null)
        private set

    var currentScreen by mutableStateOf(
        Screen.valueOf(savedState["screen"] ?: Screen.Recientes.name)
    )

    var selectedDay by mutableStateOf(
        savedState["day"] ?: "Lunes"
    )

    var recentEpisodes = mutableStateListOf<RecentEpisode>()
    var airingAnimeByDay by mutableStateOf<Map<String, List<AiringAnime>>>(emptyMap())
    var isLoadingTemporada by mutableStateOf(false)
        private set

    private val _animeMap = mutableStateMapOf<String, SnapshotStateList<AnimeSearched>>()
    val animeMap: Map<String, SnapshotStateList<AnimeSearched>> = _animeMap
    private val pageMap = mutableMapOf<String, Int>()
    private val isLoadingMap = mutableMapOf<String, Boolean>()

    private val _videoOptions = mutableStateOf<List<VideoExtractor.Option>?>(null)
    val videoOptions: State<List<VideoExtractor.Option>?> = _videoOptions

    var isLoadingEpisode by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        listOf("tv", "ova", "special", "movie").forEach {
            _animeMap[it] = mutableStateListOf()
            pageMap[it] = 1
            isLoadingMap[it] = false
        }

        viewModelScope.launch {
            loadInitialContent()
            delay(500)
            loadDeferredContent()
        }
    }

    private suspend fun loadInitialContent() {
        try {
            if (recentEpisodes.isEmpty()) {
                val result = api.getRecentEpisodes()
                recentEpisodes.addAll(result.data)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadDeferredContent() {
        try {
            if (airingAnimeByDay.isEmpty()) {
                isLoadingTemporada = true
                val result = getAiringAnimesGroupedByWeekday()
                airingAnimeByDay = result.mapKeys { it.key.replaceFirstChar(Char::uppercase) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finally {
            isLoadingTemporada = false
        }
    }

    fun clearError() {
        errorMessage = null
    }

    fun onEpisodeSelected(episode: Episode) {
        selectedEpisode = episode
    }

    fun clearSelectedEpisode() {
        selectedEpisode = null
    }

    fun navigateTo(screen: Screen) = viewModelScope.launch {
        currentScreen = screen
        savedState["screen"] = screen
        when (screen) {
            Screen.Recientes -> loadInitialContent()
            Screen.Temporada -> loadDeferredContent()
            Screen.Explorar -> ensureDirectory("tv")
            else -> Unit
        }
    }

    fun selectDay(day: String) {
        selectedDay = day
        savedState["day"] = day
    }

    private suspend fun ensureDirectory(type: String) {
        if (_animeMap[type]?.isEmpty() == true) {
            loadMoreAnimes(type)
        }
    }

    fun loadMoreAnimes(type: String) {
        if (isLoadingMap[type] == true) return

        isLoadingMap[type] = true
        val page = pageMap[type] ?: 1

        viewModelScope.launch {
            try {
                val result = api.getAnimesFiltering(
                    order = "title",
                    page = page,
                    filter = AnimeApiService.AnimeFilterRequest(types = listOf(type))
                )
                _animeMap[type]?.addAll(result.data.media)
                pageMap[type] = page + 1
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMap[type] = false
            }
        }
    }

    fun getAnimeList(type: String): List<AnimeSearched> {
        return animeMap[type] ?: emptyList()
    }

    fun favoritesUiState(context: Context): Flow<UiState<FavoriteAnimes>> = flow {
        emit(UiState.Loading)
        emitAll(
            context.favoritesDataStore.data
                .map { UiState.Success(it) }
        )
    }

    fun refreshRecentEpisodes(context: Context) {
        viewModelScope.launch {
            _isRefreshing.value = true
            Toast.makeText(context, "Actualizando...", Toast.LENGTH_SHORT).show()
            val start = System.currentTimeMillis()
            try {
                val result = api.getRecentEpisodes()
                recentEpisodes.clear()
                recentEpisodes.addAll(result.data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val elapsed = System.currentTimeMillis() - start
            delay((1500L - elapsed).coerceAtLeast(0L) + 600L)
            _isRefreshing.value = false
        }
    }

    fun addFavorite(context: Context, anime: AnimeSearched) {
        viewModelScope.launch {
            context.favoritesDataStore.updateData { current ->
                current.toBuilder().addAnimes(anime.toProto()).build()
            }
        }
    }

    fun removeFavorite(context: Context, slug: String) {
        viewModelScope.launch {
            context.favoritesDataStore.updateData { current ->
                val updated = current.animesList.filterNot { it.slug == slug }
                FavoriteAnimes.newBuilder().addAllAnimes(updated).build()
            }
        }
    }

    fun markEpisodeSeen(context: Context, slug: String) {
        viewModelScope.launch {
            context.seenEpisodesDataStore.updateData { current ->
                if (!current.episodeSlugsList.contains(slug)) {
                    current.toBuilder().addEpisodeSlugs(slug).build()
                } else current
            }
        }
    }

    fun unmarkEpisodeSeen(context: Context, slug: String){
        viewModelScope.launch {
            context.seenEpisodesDataStore.updateData { current ->
                val updated = current.episodeSlugsList.filterNot { it == slug }
                SeenEpisodes.newBuilder().addAllEpisodeSlugs(updated).build()
            }
        }
    }

    fun isEpisodeSeenFlow(context: Context): Flow<Set<String>> {
        return context.seenEpisodesDataStore.data
            .map { it.episodeSlugsList.toSet() }
    }

    suspend fun searchDirect(query: String): List<AnimeSearched> {
        return try {
            val response = api.searchAnime(query)
            response.data.media
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            try {
                val response = api.searchAnime(query)
                animeList = response.data.media
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadEpisodes(anime: AnimeSearched) {
        viewModelScope.launch {
            try {
                val response = api.getAnimeInfo(anime.slug)
                selectedAnime = anime
                episodeList = response.data.episodes
                animeInfo = response.data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun translateSinopsis(texto: String, destino: String): String {
        return try {
            val respuesta = translateApi.translate(
                TranslateRequest(q = texto, source = "", target = destino)
            )
            respuesta.content.translation
        } catch (e: Exception) {
            e.printStackTrace()
            texto
        }
    }

    fun clearVideoOptions() {
        _videoOptions.value = null
    }

    suspend fun getEmbedPlayerEpisode(slug: String): List<Server> {
        return try {
            val response = api.getEmbedPlayer(slug)
            response.data.servers
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun onEpisodeClick(context: Context, episodeSlug: String, server: String) {
        viewModelScope.launch {
            isLoadingEpisode = true
            errorMessage = null

            try {
                val servers = getEmbedPlayerEpisode(episodeSlug)
                val embedUrl = servers.find { it.name.equals(server, ignoreCase = true) }?.embed
                if (embedUrl.isNullOrEmpty()) throw Exception("No se encontró el servidor '$server'")

                if (server.equals("okru", ignoreCase = true)) {
                    val options = VideoExtractor.extractOkruVideo(embedUrl)
                    if (options.isEmpty()) throw Exception("No se pudo extraer el enlace de Okru")
                    _videoOptions.value = options
                    return@launch
                }

                val mp4 = VideoExtractor.extract(server, embedUrl, context)
                if (mp4 == null) throw Exception("No se pudo extraer el enlace del vídeo")

                val isPlayable = isVideoPlayable(mp4.first, mp4.second)
                if (!isPlayable) throw Exception("El vídeo no está disponible")

                val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                    putExtra("videoUrl", mp4.first)
                    putExtra("headers", HashMap(mp4.second))
                }
                context.startActivity(intent)

            } catch (e: Exception) {
                Log.e("EPISODE_CLICK", "Error al abrir el episodio", e)
                Toast.makeText(context, "Error al cargar episodio", Toast.LENGTH_SHORT).show()
                errorMessage = e.message ?: "Error al reproducir el episodio"
            } finally {
                isLoadingEpisode = false
            }
        }
    }

    suspend fun isVideoPlayable(videoUrl: String, headers: Map<String, String>): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(videoUrl).apply {
                    headers.forEach { (k, v) -> addHeader(k, v) }
                }.build()

                val client = OkHttpClient.Builder()
                    .callTimeout(5, TimeUnit.SECONDS)
                    .build()

                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                response.close()
                return@withContext success
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }
}
