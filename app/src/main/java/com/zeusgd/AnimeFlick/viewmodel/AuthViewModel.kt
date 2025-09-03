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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.animeflick.datastore.completedDataStore
import com.example.animeflick.datastore.favoritesDataStore
import com.example.animeflick.datastore.followedDataStore
import com.example.animeflick.datastore.pausedDataStore
import com.example.animeflick.datastore.seenEpisodesDataStore
import com.zeusgd.AnimeFlick.data.repository.AuthRepository
import com.zeusgd.AnimeFlick.model.AnimeSearched
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

    fun setSyncedOnce(context: Context, value: Boolean) {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("synced_once", value).apply()
    }

    fun hasSyncedOnce(context: Context): Boolean {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("synced_once", false)
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
        return repo.getUsername().toString();
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

    fun loadFavorites(context: Context) {
        viewModelScope.launch {
            val accessToken = getAccessToken()
            val refreshToken = getRefreshToken()

            val result = repo.getFavorites(accessToken, refreshToken, context)
            result.onSuccess { animes ->
                _favorites.value = animes
            }.onFailure {
                Log.e("Favorites", "Error al obtener favoritos", it)
                Toast.makeText(context, "Error al obtener favoritos", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun addFavorite(anime: AnimeSearched, context: Context){
        viewModelScope.launch {
            val accessToken = getAccessToken()
            val refreshToken = getRefreshToken()

            val result = repo.addFavorite(anime, accessToken,refreshToken, context)
            result.onSuccess {
                Log.d("Favorites", "Favorito añadido correctamente")
            }.onFailure {
                Log.e("Favorites", "Error al añadir favorito", it)
                Toast.makeText(context, "Error al añadir favorito", Toast.LENGTH_SHORT).show()
            }

        }
    }

    fun deleteFavorite(animeSlug: String, context: Context){
        viewModelScope.launch {
            val accessToken = getAccessToken()
            val refreshToken = getRefreshToken()

            val result = repo.deleteFavorite(animeSlug, accessToken,refreshToken, context)
            result.onSuccess {
                Log.d("Delete Favorites", "Favorito eliminado correctamente")
            }.onFailure {
                Log.e("Delete Favorites", "Error al añadir favorito", it)
                Toast.makeText(context, "Error al añadir favorito", Toast.LENGTH_SHORT).show()
            }

        }
    }

    fun setProgress(anime: AnimeSearched,status: String, context: Context){
        viewModelScope.launch {
            val accessToken = getAccessToken()
            val refreshToken = getRefreshToken()

            val result = repo.setAnimeProgress(anime, status, accessToken,refreshToken, context)
            result.onSuccess {
                Log.d("Progress", "Anime añadido correctamente")
            }.onFailure {
                Log.e("Progress", "Error al añadir progreso", it)
                Toast.makeText(context, "Error al añadir progreso", Toast.LENGTH_SHORT).show()
            }

        }
    }

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

    suspend fun loadWatchedEpisodes(context: Context) {
            val accessToken = getAccessToken()
            val refreshToken = getRefreshToken()

            val result = repo.getWatchedEpisodes(accessToken, refreshToken)
            result.onSuccess { list ->
                _watchedEpisodes.value = list
            }.onFailure {
                Log.e("Watched", "Error al obtener episodios vistos", it)
                Toast.makeText(context, "Error al cargar episodios vistos", Toast.LENGTH_SHORT).show()
            }
    }


    suspend fun loadWatching() {
            val accessToken = getAccessToken()
            val refreshToken = getRefreshToken()

            val result = repo.getProgressAnimes("viendo", accessToken, refreshToken)
            result.onSuccess { animes ->
                _watching.value = animes
            }.onFailure {
                Log.e("Watching", "Error al obtener animes viendo", it)
            }
    }

    suspend fun loadCompleted() {
            val accessToken = getAccessToken()
            val refreshToken = getRefreshToken()

            val result = repo.getProgressAnimes("completado", accessToken, refreshToken)
            result.onSuccess { animes ->
                _completed.value = animes
            }.onFailure {
                Log.e("Completed", "Error al obtener animes completados", it)
            }
    }

    suspend fun loadPaused() {
            val accessToken = getAccessToken()
            val refreshToken = getRefreshToken()

            val result = repo.getProgressAnimes("en pausa", accessToken, refreshToken)
            result.onSuccess { animes ->
                _paused.value = animes
            }.onFailure {
                Log.e("Paused", "Error al obtener animes en pausa", it)
            }
    }

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

        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("synced_once", false).apply()
    }

    fun setShowVerifyEmailScreen(value: Boolean) {
        showVerifyEmailScreen.value = value
    }

    fun setShowEmailChangePasswordScreen(value: Boolean) {
        showEmailChangePasswordScreen.value = value
    }
}
