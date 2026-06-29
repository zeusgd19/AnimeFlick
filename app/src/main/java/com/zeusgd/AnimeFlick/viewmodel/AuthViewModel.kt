package com.zeusgd.AnimeFlick.ui.auth

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeflick.datastore.completedDataStore
import com.example.animeflick.datastore.favoritesDataStore
import com.example.animeflick.datastore.followedDataStore
import com.example.animeflick.datastore.pausedDataStore
import com.example.animeflick.datastore.seenEpisodesDataStore
import com.zeusgd.AnimeFlick.CompletedAnimes
import com.zeusgd.AnimeFlick.FavoriteAnimes
import com.zeusgd.AnimeFlick.FollowedAnimes
import com.zeusgd.AnimeFlick.PausedAnimes
import com.zeusgd.AnimeFlick.SeenEpisodes
import com.zeusgd.AnimeFlick.data.repository.AuthRepository
import com.zeusgd.AnimeFlick.model.AnimeSearched
import com.zeusgd.AnimeFlick.model.toProto
import com.zeusgd.AnimeFlick.model.toProtoCompleted
import com.zeusgd.AnimeFlick.model.toProtoFollowed
import com.zeusgd.AnimeFlick.model.toProtoPaused
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: AuthRepository
    val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _favorites = MutableStateFlow<List<AnimeSearched>>(emptyList())
    val favorites: StateFlow<List<AnimeSearched>> = _favorites.asStateFlow()

    private val _watching = MutableStateFlow<List<AnimeSearched>>(emptyList())
    val watching: StateFlow<List<AnimeSearched>> = _watching.asStateFlow()

    private val _completed = MutableStateFlow<List<AnimeSearched>>(emptyList())
    val completed: StateFlow<List<AnimeSearched>> = _completed.asStateFlow()

    private val _paused = MutableStateFlow<List<AnimeSearched>>(emptyList())
    val paused: StateFlow<List<AnimeSearched>> = _paused.asStateFlow()

    private val _watchedEpisodes = MutableStateFlow<List<String>>(emptyList())
    val watchedEpisodes: StateFlow<List<String>> = _watchedEpisodes.asStateFlow()

    val forgotPasswordState = MutableStateFlow(ForgotPasswordUiState())
    val _forgotPasswordState = forgotPasswordState

    private val _showForgotPasswordScreen = MutableStateFlow(false)
    val showForgotPasswordScreen: StateFlow<Boolean> = _showForgotPasswordScreen

    private val _showLoginScreen = mutableStateOf(false)
    val showLoginScreen: State<Boolean> get() = _showLoginScreen

    // ---- Sync timing ----
    private var lastSyncedAt: Long = 0L
    private val syncIntervalMs: Long = 5 * 60 * 1000L // 5 minutos
    private var isSyncing = false

    fun setShowLoginScreen(visible: Boolean) {
        _showLoginScreen.value = visible
    }

    fun setShowForgotPasswordScreen(show: Boolean) {
        _showForgotPasswordScreen.value = show
    }

    fun checkLoginStatus(sharedPreferences: SharedPreferences) {
        val token = sharedPreferences.getString("access_token", null)
        _isLoggedIn.value = token != null
    }

    fun setLoggedIn(value: Boolean) {
        _isLoggedIn.value = value
    }

    var showVerifyEmailScreen = MutableStateFlow(false)
        private set

    var showEmailChangePasswordScreen = MutableStateFlow(false)
        private set

    init {
        val prefs = app.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        repo = AuthRepository(prefs)
    }

    fun getUsername(): String {
        return repo.getUsername().toString()
    }

    fun getUserId(): String {
        return repo.getUserId().toString()
    }

    fun getAccessToken(): String {
        return repo.getAccessToken().toString()
    }

    fun getRefreshToken(): String {
        return repo.getRefreshToken().toString()
    }

    // ---- Sync periódico ----

    /**
     * Refresca los datos del servidor si han pasado más de 5 minutos
     * desde la última sincronización. Llamar al volver al foreground.
     */
    fun refreshIfStale(context: Context) {
        if (!_isLoggedIn.value) return
        if (isSyncing) return

        val now = System.currentTimeMillis()
        if (now - lastSyncedAt < syncIntervalMs && lastSyncedAt != 0L) return

        isSyncing = true
        viewModelScope.launch {
            try {
                loadAllFromServer(context)
                lastSyncedAt = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e("AuthVM", "Error en refreshIfStale", e)
            } finally {
                isSyncing = false
            }
        }
    }

    /**
     * Fuerza una sincronización completa sin importar el tiempo.
     * Se usa tras el primer login.
     */
    fun forceSync(context: Context) {
        if (!_isLoggedIn.value) return
        if (isSyncing) return

        isSyncing = true
        viewModelScope.launch {
            try {
                loadAllFromServer(context)
                lastSyncedAt = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e("AuthVM", "Error en forceSync", e)
            } finally {
                isSyncing = false
            }
        }
    }

    /**
     * Carga todos los datos del servidor y los escribe al DataStore local.
     */
    private suspend fun loadAllFromServer(context: Context) {
        kotlinx.coroutines.coroutineScope {
            // Cargar todo en paralelo usando coroutines
            launch { loadFavoritesAndSync(context) }
            launch { loadWatchingAndSync(context) }
            launch { loadCompletedAndSync(context) }
            launch { loadPausedAndSync(context) }
            launch { loadWatchedEpisodesAndSync(context) }
        }
    }

    // ---- Load + Sync to DataStore ----

    private suspend fun loadFavoritesAndSync(context: Context) {
        val accessToken = getAccessToken()
        val refreshToken = getRefreshToken()

        val result = repo.getFavorites(accessToken, refreshToken, context)
        result.onSuccess { animes ->
            _favorites.value = animes
            // Merge al DataStore local
            context.favoritesDataStore.updateData { current ->
                current.toBuilder()
                    .clearAnimes()
                    .addAllAnimes(animes.map { it.toProto() })
                    .build()
            }
        }.onFailure {
            Log.e("Favorites", "Error al obtener favoritos", it)
        }
    }

    private suspend fun loadWatchingAndSync(context: Context) {
        val accessToken = getAccessToken()
        val refreshToken = getRefreshToken()

        val result = repo.getProgressAnimes("viendo", accessToken, refreshToken)
        result.onSuccess { animes ->
            _watching.value = animes
            context.followedDataStore.updateData { current ->
                current.toBuilder()
                    .clearAnimes()
                    .addAllAnimes(animes.map { it.toProtoFollowed() })
                    .build()
            }
        }.onFailure {
            Log.e("Watching", "Error al obtener animes viendo", it)
        }
    }

    private suspend fun loadCompletedAndSync(context: Context) {
        val accessToken = getAccessToken()
        val refreshToken = getRefreshToken()

        val result = repo.getProgressAnimes("completado", accessToken, refreshToken)
        result.onSuccess { animes ->
            _completed.value = animes
            context.completedDataStore.updateData { current ->
                current.toBuilder()
                    .clearAnimes()
                    .addAllAnimes(animes.map { it.toProtoCompleted() })
                    .build()
            }
        }.onFailure {
            Log.e("Completed", "Error al obtener animes completados", it)
        }
    }

    private suspend fun loadPausedAndSync(context: Context) {
        val accessToken = getAccessToken()
        val refreshToken = getRefreshToken()

        val result = repo.getProgressAnimes("en pausa", accessToken, refreshToken)
        result.onSuccess { animes ->
            _paused.value = animes
            context.pausedDataStore.updateData { current ->
                current.toBuilder()
                    .clearAnimes()
                    .addAllAnimes(animes.map { it.toProtoPaused() })
                    .build()
            }
        }.onFailure {
            Log.e("Paused", "Error al obtener animes en pausa", it)
        }
    }

    private suspend fun loadWatchedEpisodesAndSync(context: Context) {
        val accessToken = getAccessToken()
        val refreshToken = getRefreshToken()

        val result = repo.getWatchedEpisodes(accessToken, refreshToken)
        result.onSuccess { list ->
            _watchedEpisodes.value = list
            context.seenEpisodesDataStore.updateData { current ->
                current.toBuilder()
                    .clearEpisodeSlugs()
                    .addAllEpisodeSlugs(list)
                    .build()
            }
        }.onFailure {
            Log.e("Watched", "Error al obtener episodios vistos", it)
        }
    }

    suspend fun loadWatchedEpisodesForAnime(animeSlug: String, context: Context) {
        val accessToken = getAccessToken()
        val refreshToken = getRefreshToken()

        val result = repo.getWatchedEpisodesByAnime(animeSlug, accessToken, refreshToken)
        result.onSuccess { list ->
            context.seenEpisodesDataStore.updateData { current ->
                // Mantenemos todos los episodios que NO son de este anime, 
                // y agregamos la lista fresca del servidor para ESTE anime.
                // Generalmente los slugs de episodios son "animeSlug-episodio-X"
                val updatedSlugs = current.episodeSlugsList.filterNot { it.startsWith("$animeSlug-") }.toMutableList()
                updatedSlugs.addAll(list)

                current.toBuilder()
                    .clearEpisodeSlugs()
                    .addAllEpisodeSlugs(updatedSlugs)
                    .build()
            }
        }.onFailure {
            Log.e("Watched", "Error al obtener episodios vistos para $animeSlug", it)
        }
    }

    // ---- Auth actions ----

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = repo.signUp(email, password, displayName)
            if (res.isSuccess) {
                showVerifyEmailScreen.value = true
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = res.exceptionOrNull()?.message
                )
            }
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit, username: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = repo.signIn(email, password, username)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = res.exceptionOrNull()?.message
                )
            }
        }
    }

    // ---- Favorites (remote) ----

    fun loadFavorites(context: Context) {
        viewModelScope.launch {
            loadFavoritesAndSync(context)
        }
    }

    fun addFavorite(anime: AnimeSearched, context: Context) {
        viewModelScope.launch {
            val accessToken = getAccessToken()
            val refreshToken = getRefreshToken()

            val result = repo.addFavorite(anime, accessToken, refreshToken, context)
            result.onSuccess {
                Log.d("Favorites", "Favorito añadido correctamente")
            }.onFailure {
                Log.e("Favorites", "Error al añadir favorito", it)
                Toast.makeText(context, "Error al añadir favorito", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteFavorite(animeSlug: String, context: Context) {
        viewModelScope.launch {
            val accessToken = getAccessToken()
            val refreshToken = getRefreshToken()

            val result = repo.deleteFavorite(animeSlug, accessToken, refreshToken, context)
            result.onSuccess {
                Log.d("Delete Favorites", "Favorito eliminado correctamente")
            }.onFailure {
                Log.e("Delete Favorites", "Error al eliminar favorito", it)
                Toast.makeText(context, "Error al eliminar favorito", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---- Progress (remote) ----

    fun setProgress(anime: AnimeSearched, status: String, context: Context) {
        viewModelScope.launch {
            val accessToken = getAccessToken()
            val refreshToken = getRefreshToken()

            val result = repo.setAnimeProgress(anime, status, accessToken, refreshToken, context)
            result.onSuccess {
                Log.d("Progress", "Anime añadido correctamente")
            }.onFailure {
                Log.e("Progress", "Error al añadir progreso", it)
                Toast.makeText(context, "Error al añadir progreso", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---- Watched episodes (remote) ----

    fun addWatchedEpisode(episodeSlug: String, context: Context) {
        viewModelScope.launch {
            val accessToken = getAccessToken()
            val refreshToken = getRefreshToken()

            val result = repo.addWatchedEpisode(episodeSlug, accessToken, refreshToken)
            result.onSuccess {
                Log.d("Watched", "Episodio visto añadido correctamente")
            }.onFailure {
                Log.e("Watched", "Error al añadir episodio visto", it)
                Toast.makeText(context, "Error al marcar episodio como visto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteWatchedEpisode(episodeSlug: String, context: Context) {
        viewModelScope.launch {
            val accessToken = getAccessToken()
            val refreshToken = getRefreshToken()

            val result = repo.deleteWatchedEpisode(episodeSlug, accessToken, refreshToken)
            result.onSuccess {
                Log.d("Watched", "Episodio visto eliminado correctamente")
            }.onFailure {
                Log.e("Watched", "Error al eliminar episodio visto", it)
                Toast.makeText(context, "Error al quitar episodio visto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---- Password reset ----

    fun resetPassword(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            _forgotPasswordState.value = _forgotPasswordState.value.copy(isLoading = true, error = null)

            val result = repo.resetPassword(email)

            result.onSuccess {
                _forgotPasswordState.value = _forgotPasswordState.value.copy(isLoading = false)
                onSuccess()
            }.onFailure {
                _forgotPasswordState.value = _forgotPasswordState.value.copy(
                    isLoading = false,
                    error = "Correo no encontrado o error del servidor"
                )
                onFailure(it.message ?: "Error desconocido")
            }
        }
    }

    // ---- Logout ----

    fun logout(context: Context) {
        repo.logout()

        viewModelScope.launch {
            // Borra todos los dataStore locales
            context.favoritesDataStore.updateData { it.toBuilder().clearAnimes().build() }
            context.followedDataStore.updateData { it.toBuilder().clearAnimes().build() }
            context.pausedDataStore.updateData { it.toBuilder().clearAnimes().build() }
            context.completedDataStore.updateData { it.toBuilder().clearAnimes().build() }
            context.seenEpisodesDataStore.updateData { it.toBuilder().clearEpisodeSlugs().build() }
        }

        // Reset sync state
        lastSyncedAt = 0L
        _favorites.value = emptyList()
        _watching.value = emptyList()
        _completed.value = emptyList()
        _paused.value = emptyList()
        _watchedEpisodes.value = emptyList()
    }

    fun setShowVerifyEmailScreen(value: Boolean) {
        showVerifyEmailScreen.value = value
    }

    fun setShowEmailChangePasswordScreen(value: Boolean) {
        showEmailChangePasswordScreen.value = value
    }
}
