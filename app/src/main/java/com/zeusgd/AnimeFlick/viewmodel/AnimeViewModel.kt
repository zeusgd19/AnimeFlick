package com.zeusgd.AnimeFlick.viewmodel

import UiState
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.example.animeflick.datastore.completedDataStore
import com.example.animeflick.datastore.favoritesDataStore
import com.example.animeflick.datastore.followedDataStore
import com.example.animeflick.datastore.pausedDataStore
import com.example.animeflick.datastore.seenEpisodesDataStore
import com.zeusgd.AnimeFlick.*
import com.zeusgd.AnimeFlick.data.remote.SupabaseApiService
import com.zeusgd.AnimeFlick.data.repository.AuthRepository
import com.zeusgd.AnimeFlick.model.*
import com.zeusgd.AnimeFlick.network.AnimeApiService
import com.zeusgd.AnimeFlick.network.RetrofitInstance
import com.zeusgd.AnimeFlick.network.RetrofitInstance.api
import com.zeusgd.AnimeFlick.network.RetrofitInstance.translateApi
import com.zeusgd.AnimeFlick.ui.auth.AuthViewModel
import com.zeusgd.AnimeFlick.ui.theme.toAnimeSearched
import com.zeusgd.AnimeFlick.ui.theme.toAnimeSearchedCompleted
import com.zeusgd.AnimeFlick.ui.theme.toAnimeSearchedFollowed
import com.zeusgd.AnimeFlick.ui.theme.toAnimeSearchedPaused
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

    var availableServers by mutableStateOf<List<Server>?>(null)
        private set
    var isLoadingServers by mutableStateOf(false)
        private set

    var currentScreen by mutableStateOf(
        Screen.valueOf(savedState["screen"] ?: Screen.Recientes.name)
    )

    var selectedDay by mutableStateOf(
        savedState["day"] ?: "Lunes"
    )

    private val preferredServers = listOf("YourUpload", "Stape", "Okru", "SW", "Mega")

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

    enum class AnimeStatus { None, Watching, Completed, Paused }

    data class Playable(
        val url: String,
        val headers: Map<String, String>,
        val slug: String,
        val server: String
    )

    private fun hasSlugFollowed(u: UiState<FollowedAnimes>, slug: String): Boolean =
        (u as? UiState.Success<FollowedAnimes>)
            ?.data?.animesList?.any { it.slug == slug } == true

    private fun hasSlugCompleted(u: UiState<CompletedAnimes>, slug: String): Boolean =
        (u as? UiState.Success<CompletedAnimes>)
            ?.data?.animesList?.any { it.slug == slug } == true

    private fun hasSlugPaused(u: UiState<PausedAnimes>, slug: String): Boolean =
        (u as? UiState.Success<PausedAnimes>)
            ?.data?.animesList?.any { it.slug == slug } == true
    fun statusFlow(context: Context, slug: String): Flow<AnimeStatus> =
        combine(
            followedUiState(context),      // Flow<UiState<FollowedAnimes>>
            completedUiState(context),   // si tienes DataStore de completados */
            pausedUiState(context)       // si tienes DataStore de en pausa */
            // mientras no tengas los otros, simula vacío:
        ) { followed, completed, paused ->

            when {
                hasSlugCompleted(completed, slug) -> AnimeStatus.Completed
                hasSlugPaused(paused, slug)       -> AnimeStatus.Paused
                hasSlugFollowed(followed, slug)   -> AnimeStatus.Watching
                else               -> AnimeStatus.None
            }
        }
            .catch { emit(AnimeStatus.None) }

    fun setStatus(context: Context, anime: AnimeSearched, status: AnimeStatus) {
        viewModelScope.launch {
            // quita de todos
            removeFollowed(context, anime.slug)
            removeCompleted(context, anime.slug)
            removePaused(context, anime.slug)

            // añade solo al elegido
            when (status) {
                AnimeStatus.Watching  -> addFollowed(context, anime)   // ya la tienes
                AnimeStatus.Completed -> addCompleted(context, anime)  // crea similar a addFollowed
                AnimeStatus.Paused    -> addPaused(context, anime)     // crea similar
                AnimeStatus.None      -> Unit
            }
        }
    }

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
                recentEpisodes.addAll(result.data.map { it.copy(title = it.title.applyRenames()) })
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
                val renamedResult = result.mapValues { entry -> entry.value.map { it.copy(title = it.title.applyRenames()) } }
                airingAnimeByDay = renamedResult.mapKeys { it.key.replaceFirstChar(Char::uppercase) }
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
        availableServers = null
        isLoadingServers = true
        viewModelScope.launch {
            try {
                availableServers = getEmbedPlayerEpisode(episode.slug)
            } catch (e: Exception) {
                e.printStackTrace()
                availableServers = emptyList()
            } finally {
                isLoadingServers = false
            }
        }
    }

    fun clearSelectedEpisode() {
        selectedEpisode = null
        availableServers = null
        isLoadingServers = false
        clearVideoOptions()
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

                val sortedResult = result.data.media.map { it.copy(title = it.title.applyRenames()) }.sortedWith(
                    compareBy<AnimeSearched> {
                        val first = it.title.firstOrNull() ?: ' '
                        if (first.isLetterOrDigit()) 1 else 0
                    }.thenBy { it.title }
                )
                _animeMap[type]?.addAll(sortedResult)
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

    fun followedUiState(context: Context): Flow<UiState<FollowedAnimes>> = flow {
        emit(UiState.Loading)
        emitAll(
            context.followedDataStore.data
                .map { UiState.Success(it) }
        )
    }

    fun completedUiState(context: Context): Flow<UiState<CompletedAnimes>> = flow {
        emit(UiState.Loading)
        emitAll(
            context.completedDataStore.data
                .map { UiState.Success(it) }
        )
    }

    fun pausedUiState(context: Context): Flow<UiState<PausedAnimes>> = flow {
        emit(UiState.Loading)
        emitAll(
            context.pausedDataStore.data
                .map { UiState.Success(it) }
        )
    }

    fun refreshRecentEpisodes() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val result = api.getRecentEpisodes()
                recentEpisodes.clear()
                recentEpisodes.addAll(result.data.map { it.copy(title = it.title.applyRenames()) })
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isRefreshing.value = false
        }
    }

    fun addFavorite(context: Context, anime: AnimeSearched) {
        viewModelScope.launch {
            context.favoritesDataStore.updateData { current ->
                val alreadyExists = current.animesList.any { it.slug == anime.slug }

                if (alreadyExists) {
                    current // No hacer nada si ya está
                } else {
                    current.toBuilder().addAnimes(anime.toProto()).build()
                }
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

    fun addFollowed(context: Context, anime: AnimeSearched) {
        viewModelScope.launch {
            context.followedDataStore.updateData { current ->
                val alreadyExists = current.animesList.any { it.slug == anime.slug }

                if (alreadyExists) {
                    current // No hacer nada si ya está
                } else {
                    current.toBuilder().addAnimes(anime.toProtoFollowed()).build()
                }
            }
        }
    }

    fun removeFollowed(context: Context, slug: String) {
        viewModelScope.launch {
            context.followedDataStore.updateData { current ->
                val updated = current.animesList.filterNot { it.slug == slug }
                FollowedAnimes.newBuilder().addAllAnimes(updated).build()
            }
        }
    }

    fun addCompleted(context: Context, anime: AnimeSearched) {
        viewModelScope.launch {
            context.completedDataStore.updateData { current ->
                val alreadyExists = current.animesList.any { it.slug == anime.slug }

                if (alreadyExists) {
                    current // No hacer nada si ya está
                } else {
                    current.toBuilder().addAnimes(anime.toProtoCompleted()).build()
                }
            }
        }
    }

    fun removeCompleted(context: Context, slug: String) {
        viewModelScope.launch {
            context.completedDataStore.updateData { current ->
                val updated = current.animesList.filterNot { it.slug == slug }
                CompletedAnimes.newBuilder().addAllAnimes(updated).build()
            }
        }
    }

    fun addPaused(context: Context, anime: AnimeSearched) {
        viewModelScope.launch {
            context.pausedDataStore.updateData { current ->
                val alreadyExists = current.animesList.any { it.slug == anime.slug }

                if (alreadyExists) {
                    current // No hacer nada si ya está
                } else {
                    current.toBuilder().addAnimes(anime.toProtoPaused()).build()
                }
            }
        }
    }

    fun removePaused(context: Context, slug: String) {
        viewModelScope.launch {
            context.pausedDataStore.updateData { current ->
                val updated = current.animesList.filterNot { it.slug == slug }
                PausedAnimes.newBuilder().addAllAnimes(updated).build()
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
            Log.e("AnimeSearch", response.toString())
            response.data.media.map { it.copy(title = it.title.applyRenames()) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            try {
                val response = api.searchAnime(query)
                animeList = response.data.media.map { it.copy(title = it.title.applyRenames()) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadEpisodes(anime: AnimeSearched) {
        viewModelScope.launch {
            try {
                val response = api.getAnimeInfo(anime.slug)
                selectedAnime = anime.copy(title = anime.title.applyRenames())
                episodeList = response.data.episodes
                animeInfo = response.data.copy(title = response.data.title.applyRenames())
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

    suspend fun findPlayableForSlug(
        episodeSlug: String,
        serverOrder: List<String> = preferredServers,
        context: Context
    ): Playable? {
        val servers = getEmbedPlayerEpisode(episodeSlug)

        for (srv in serverOrder) {
            val embed = servers.find { it.name.equals(srv, ignoreCase = true) }?.embed ?: continue

            if (srv.equals("okru", ignoreCase = true)) {
                val candidate = VideoExtractor.extractOkruVideo(embed).firstOrNull() ?: continue
                if (isVideoPlayable(candidate.url, candidate.headers)) {
                    return Playable(candidate.url, candidate.headers, episodeSlug, srv)
                }
                continue
            }

            if (srv.equals("mega", ignoreCase = true)) {
                val mp4 = VideoExtractor.extract(srv, embed, context) ?: continue
                if (isVideoPlayable(mp4.first, mp4.second)) {
                    return Playable(mp4.first, mp4.second, episodeSlug, srv)
                }
                continue
            }

            val mp4 = VideoExtractor.extract(srv, embed, context) ?: continue
            if (isVideoPlayable(mp4.first, mp4.second)) {
                return Playable(mp4.first, mp4.second, episodeSlug, srv)
            }
        }
        return null
    }


    private fun nextEpisodeSlug(current: String): String? {
        // Matchea cualquier texto y un bloque numérico al final
        val m = Regex("""^(.*?)(\d+)$""").find(current) ?: return null
        val prefix = m.groupValues[1]
        val numStr = m.groupValues[2]
        val nextNum = (numStr.toLong() + 1).toString().padStart(numStr.length, '0')
        return prefix + nextNum
    }

    suspend fun findPlayableForNext(
        currentSlug: String,
        serverOrder: List<String> = preferredServers,
        context: Context
    ): Playable? {
        val next = nextEpisodeSlug(currentSlug) ?: return null
        return findPlayableForSlug(next, serverOrder, context)
    }

    private suspend fun resolvePlayableUrl(
        episodeSlug: String,
        serverOrder: List<String> = preferredServers,
        context: Context,
    ): Pair<String, Map<String, String>>? {
        val servers = getEmbedPlayerEpisode(episodeSlug)

        for (srv in serverOrder) {
            val embed = servers.find { it.name.equals(srv, ignoreCase = true) }?.embed ?: continue

            if (srv.equals("okru", ignoreCase = true)) {
                val options = VideoExtractor.extractOkruVideo(embed)
                val candidate = options.firstOrNull()
                if (candidate != null) {
                    val playable = isVideoPlayable(candidate.url, candidate.headers)
                    if (playable) {
                        onEpisodeClick(context, episodeSlug, srv, false)
                        return null // ← corta aquí
                    }
                }
                continue
            }

            if (srv.equals("mega", ignoreCase = true)) {
                val mp4 = VideoExtractor.extract(srv, embed, context)
                if (mp4 != null) {
                    val playable = isVideoPlayable(mp4.first, mp4.second)
                    if (playable) {
                        onEpisodeClick(context, episodeSlug, srv, false)
                        return null // ← corta aquí
                    }
                }
                continue
            }

            val mp4 = VideoExtractor.extract(srv, embed, context) ?: continue
            val playable = isVideoPlayable(mp4.first, mp4.second)
            if (playable) {
                onEpisodeClick(context, episodeSlug, srv, false)
                return null // ← corta aquí
            }
        }

        return null
    }

    fun onEpisodeClick(context: Context, episodeSlug: String, server: String, isNext: Boolean) {
        viewModelScope.launch {
            isLoadingEpisode = true
            errorMessage = null

            if(isNext){
                val nextSlug = nextEpisodeSlug(episodeSlug) ?: ""
                resolvePlayableUrl(nextSlug, preferredServers, context)
                return@launch
            }

            try {
                val servers = getEmbedPlayerEpisode(episodeSlug)
                val embedUrl = servers.find { it.name.equals(server, ignoreCase = true) }?.embed
                if (embedUrl.isNullOrEmpty()) throw Exception("No se encontró el servidor '$server'")

                if (server.equals("okru", ignoreCase = true)) {
                    val options = VideoExtractor.extractOkruVideo(embedUrl)
                    if (options.isEmpty()) throw Exception("No se pudo extraer el enlace de Okru")
                    _videoOptions.value = options
                    return@launch
                } else if (server.equals("mega", ignoreCase = true)) {
                    val mp4 = VideoExtractor.extract(server, embedUrl, context)
                        ?: throw Exception("No se pudo extraer el enlace de Mega")
                    val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                        putExtra("VIDEO_URL", mp4.first)
                    }
                    context.startActivity(intent)
                    return@launch
                }

                var mp4 = VideoExtractor.extract(server, embedUrl, context)
                var isWebView = false
                if (mp4 == null) {
                    if (embedUrl.contains(".m3u8") || embedUrl.contains(".mp4")) {
                        mp4 = Pair(embedUrl, emptyMap())
                    } else {
                        // Es probable que sea una página HTML (iframe) que ExoPlayer no soporta.
                        mp4 = Pair(embedUrl, emptyMap())
                        isWebView = true
                    }
                }

                // Eliminamos la comprobación isVideoPlayable porque a veces falla por headers o Cloudflare
                // ExoPlayer lo manejará mejor internamente y lanzará su propio error si falla.

                val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                    putExtra("videoUrl", mp4.first)
                    putExtra("headers", HashMap(mp4.second))
                    putExtra("currentSlug", episodeSlug)
                    putExtra("currentServer", server)
                    putExtra("isWebView", isWebView)
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

    fun syncAllRemoteAnimesToLocal(authViewModel: AuthViewModel, context: Context) {
        // 🔁 Favoritos
        authViewModel.favorites.value?.forEach { anime ->
            addFavorite(context, anime)
        }

        // 🔁 Siguiendo
        authViewModel.watching.value?.forEach { anime ->
            addFollowed(context, anime)
        }

        // 🔁 Completado
        authViewModel.completed.value?.forEach { anime ->
            addCompleted(context, anime)
        }

        // 🔁 En pausa
        authViewModel.paused.value?.forEach { anime ->
            addPaused(context, anime)
        }

        // 🔁 Episodios Vistos
        authViewModel.watchedEpisodes.value?.forEach { animeSlug ->
            markEpisodeSeen(context, animeSlug)
        }
    }

    suspend fun syncAllLocalAnimesToRemote(authViewModel: AuthViewModel, context: Context) {
            val accessToken = authViewModel.getAccessToken()
            val refreshToken = authViewModel.getRefreshToken()

            if (accessToken.isBlank() || refreshToken.isBlank()) return

            // 🟡 Favoritos
            val favData = context.favoritesDataStore.data.first()
            favData.animesList.forEach { animeProto ->
                authViewModel.addFavorite(animeProto.toAnimeSearched(), context)
            }

            // 🟡 Viendo
            val watchingData = context.followedDataStore.data.first()
            watchingData.animesList.forEach { animeProto ->
                authViewModel.setProgress(animeProto.toAnimeSearchedFollowed(), "watching", context)
            }

            // 🟡 Completado
            val completedData = context.completedDataStore.data.first()
            completedData.animesList.forEach { animeProto ->
                authViewModel.setProgress(animeProto.toAnimeSearchedCompleted(), "completed", context)
            }

            // 🟡 En pausa
            val pausedData = context.pausedDataStore.data.first()
            pausedData.animesList.forEach { animeProto ->
                authViewModel.setProgress(animeProto.toAnimeSearchedPaused(), "paused", context)
            }

            // 🟡 Episodios vistos
            val episodesData = context.seenEpisodesDataStore.data.first()
            episodesData.episodeSlugsList.forEach { slug ->
                authViewModel.addWatchedEpisode(slug, context)
            }
    }
}
