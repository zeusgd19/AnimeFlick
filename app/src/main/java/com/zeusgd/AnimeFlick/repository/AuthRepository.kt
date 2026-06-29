package com.zeusgd.AnimeFlick.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.zeusgd.AnimeFlick.data.remote.SupabaseApiService
import com.zeusgd.AnimeFlick.data.remote.SupabaseApiService.AuthBody
import com.zeusgd.AnimeFlick.data.remote.SupabaseApiService.FavoriteBody
import com.zeusgd.AnimeFlick.data.remote.SupabaseApiService.DeleteFavoriteBody
import com.zeusgd.AnimeFlick.model.AnimeSearched
import com.zeusgd.AnimeFlick.network.RetrofitInstance.pyhtonApiSupabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.internal.http.hasBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthRepository(private val prefs: SharedPreferences) {

    private val api = pyhtonApiSupabase

    suspend fun signUp(email: String, password: String, displayName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.signUp(AuthBody(email, password, displayName))
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.errorBody()?.string() ?: "Error al registrarse"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun signIn(email: String, password: String, username: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.signIn(AuthBody(email, password))
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.access_token != null) {
                        saveTokens(body.access_token, body.refresh_token!!, body.user?.id ?: "", body.user?.user_metadata?.display_name ?: "User")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("No se recibieron tokens"))
                    }
                } else {
                    Result.failure(Exception(response.errorBody()?.string() ?: "Error al iniciar sesión"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun saveTokens(accessToken: String, refreshToken: String, userId: String, username: String) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putString("user_id", userId)
            .putString("username", username)
            .apply()
        Log.e("Token", accessToken)
    }

    suspend fun addFavorite(anime: AnimeSearched, accessToken: String, refreshToken: String, context: Context): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.addFavorite(FavoriteBody(anime.slug, anime.title, anime.cover, anime.rating, anime.type), "Bearer $accessToken")
                if (response.isSuccessful) {
                    return@withContext Result.success(Unit)
                } else {
                    val error = response.errorBody()?.string()
                    if (error?.contains("token is invalid") == true || error?.contains("bad_jwt") == true) {

                        // Token expirado, renovamos
                        val refreshResult = refreshToken(refreshToken)
                        if (refreshResult.isSuccess) {
                            val newTokens = refreshResult.getOrNull()!!
                            // Guardar tokens (SharedPreferences o lo que uses)
                            saveTokens(newTokens.access_token, newTokens.refresh_token, newTokens.user?.id ?: "", newTokens.user?.user_metadata?.display_name ?: "User")

                            // Reintentar con el nuevo token
                            val retryResponse = api.addFavorite(FavoriteBody(anime.slug, anime.title, anime.cover, anime.rating, anime.type), "Bearer ${newTokens.access_token}")
                            if (retryResponse.isSuccessful) {
                                return@withContext Result.success(Unit)
                            } else {
                                return@withContext Result.failure(Exception(retryResponse.errorBody()?.string() ?: "Error al reintentar con nuevo token"))
                            }
                        } else {
                            return@withContext Result.failure(Exception("Token expirado y no se pudo renovar"))
                        }
                    } else {
                        return@withContext Result.failure(Exception(error ?: "Error al añadir favorito"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    suspend fun deleteFavorite(
        animeSlug: String,
        accessToken: String,
        refreshToken: String,
        context: Context
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.deleteFavorite(DeleteFavoriteBody(animeSlug), "Bearer $accessToken")
                if (response.isSuccessful) {
                    if (response.body()?.message == "Favorito eliminado correctamente") {
                        return@withContext Result.success(Unit)
                    } else {
                        return@withContext Result.failure(Exception("No se eliminó correctamente"))
                    }
                } else {
                    val error = response.errorBody()?.string()
                    Log.e("Error", error.toString())

                    if (error?.contains("token is invalid") == true || error?.contains("bad_jwt") == true) {
                        // Token expirado, renovamos
                        val refreshResult = refreshToken(refreshToken)
                        Log.e("REFRESH", refreshResult.toString())

                        if (refreshResult.isSuccess) {
                            val newTokens = refreshResult.getOrNull()!!
                            saveTokens(
                                newTokens.access_token,
                                newTokens.refresh_token,
                                newTokens.user?.id ?: "",
                                newTokens.user?.user_metadata?.display_name ?: "User"
                            )

                            // Reintentar con el nuevo token
                            val retryResponse = api.deleteFavorite(DeleteFavoriteBody(animeSlug), "Bearer ${newTokens.access_token}")
                            if (retryResponse.isSuccessful && retryResponse.body()?.message == "Favorito eliminado correctamente") {
                                return@withContext Result.success(Unit)
                            } else {
                                return@withContext Result.failure(Exception(retryResponse.errorBody()?.string() ?: "Error al reintentar con nuevo token"))
                            }
                        } else {
                            return@withContext Result.failure(Exception("Token expirado y no se pudo renovar"))
                        }
                    } else {
                        return@withContext Result.failure(Exception(error ?: "Error al eliminar un favorito"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getFavorites(
        accessToken: String,
        refreshToken: String,
        context: Context
    ): Result<List<AnimeSearched>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getFavorites("Bearer $accessToken")
                if (response.isSuccessful) {
                    val favorites = response.body()?.favorites?.map { it } ?: emptyList()
                    Log.e("Favoritos", response.body().toString())
                    return@withContext Result.success(favorites)
                } else {
                    val error = response.errorBody()?.string()
                    Log.e("Error", error.toString())

                    if (error?.contains("token is invalid") == true || error?.contains("bad_jwt") == true) {
                        val refreshResult = refreshToken(refreshToken)
                        Log.e("REFRESH", refreshResult.toString())

                        if (refreshResult.isSuccess) {
                            val newTokens = refreshResult.getOrNull()!!
                            saveTokens(
                                newTokens.access_token,
                                newTokens.refresh_token,
                                newTokens.user?.id ?: "",
                                newTokens.user?.user_metadata?.display_name ?: "User"
                            )

                            // Reintentar con el nuevo token
                            val retryResponse = api.getFavorites("Bearer ${newTokens.access_token}")
                            if (retryResponse.isSuccessful) {
                                val favorites = retryResponse.body()?.favorites?.map { it } ?: emptyList()
                                return@withContext Result.success(favorites)
                            } else {
                                return@withContext Result.failure(Exception(retryResponse.errorBody()?.string() ?: "Error al reintentar con nuevo token"))
                            }
                        } else {
                            return@withContext Result.failure(Exception("Token expirado y no se pudo renovar"))
                        }
                    } else {
                        return@withContext Result.failure(Exception(error ?: "Error al obtener favoritos"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun setAnimeProgress(
        anime: AnimeSearched,
        status: String, // "viendo", "completado", "pausa"
        accessToken: String,
        refreshToken: String,
        context: Context
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.setAnimeProgress(
                    SupabaseApiService.ProgressBody(
                        anime_slug = anime.slug,
                        title = anime.title,
                        cover = anime.cover,
                        rating = anime.rating,
                        type = anime.type,
                        status = status
                    ),
                    "Bearer $accessToken"
                )

                if (response.isSuccessful) {
                    return@withContext Result.success(Unit)
                } else {
                    val error = response.errorBody()?.string()
                    if (error?.contains("token is invalid") == true || error?.contains("bad_jwt") == true) {
                        val refreshResult = refreshToken(refreshToken)
                        if (refreshResult.isSuccess) {
                            val newTokens = refreshResult.getOrNull()!!
                            saveTokens(
                                newTokens.access_token,
                                newTokens.refresh_token,
                                newTokens.user?.id ?: "",
                                newTokens.user?.user_metadata?.display_name ?: "User"
                            )

                            // Retry con nuevo token
                            val retryResponse = api.setAnimeProgress(
                                SupabaseApiService.ProgressBody(
                                    anime_slug = anime.slug,
                                    title = anime.title,
                                    cover = anime.cover,
                                    rating = anime.rating,
                                    type = anime.type,
                                    status = status
                                ),
                                "Bearer ${newTokens.access_token}"
                            )

                            if (retryResponse.isSuccessful) {
                                return@withContext Result.success(Unit)
                            } else {
                                return@withContext Result.failure(Exception(retryResponse.errorBody()?.string() ?: "Error al reintentar con nuevo token"))
                            }
                        } else {
                            return@withContext Result.failure(Exception("Token expirado y no se pudo renovar"))
                        }
                    } else {
                        return@withContext Result.failure(Exception(error ?: "Error al actualizar progreso"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    suspend fun getProgressAnimes(
        status: String,
        accessToken: String,
        refreshToken: String
    ): Result<List<AnimeSearched>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getAnimeProgress(status, "Bearer $accessToken")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        return@withContext Result.success(body.animes)
                    } else {
                        return@withContext Result.failure(Exception("Respuesta vacía"))
                    }
                } else {
                    val error = response.errorBody()?.string()
                    if (error?.contains("token is invalid") == true || error?.contains("bad_jwt") == true) {
                        val refreshResult = refreshToken(refreshToken)
                        if (refreshResult.isSuccess) {
                            val newTokens = refreshResult.getOrNull()!!
                            saveTokens(
                                newTokens.access_token,
                                newTokens.refresh_token,
                                newTokens.user?.id ?: "",
                                newTokens.user?.user_metadata?.display_name ?: "User"
                            )

                            val retryResponse = api.getAnimeProgress(status, "Bearer ${newTokens.access_token}")
                            if (retryResponse.isSuccessful) {
                                val retryBody = retryResponse.body()
                                if (retryBody != null) {
                                    return@withContext Result.success(retryBody.animes)
                                } else {
                                    return@withContext Result.failure(Exception("Respuesta vacía tras retry"))
                                }
                            } else {
                                return@withContext Result.failure(Exception(retryResponse.errorBody()?.string() ?: "Error al reintentar con nuevo token"))
                            }
                        } else {
                            return@withContext Result.failure(Exception("Token expirado y no se pudo renovar"))
                        }
                    } else {
                        return@withContext Result.failure(Exception(error ?: "Error al obtener progreso"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    suspend fun addWatchedEpisode(
        episodeSlug: String,
        accessToken: String,
        refreshToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.addWatchedEpisode(
                SupabaseApiService.WatchedEpisodeBody(episodeSlug),
                "Bearer $accessToken"
            )

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val error = response.errorBody()?.string()
                if (error?.contains("token is invalid") == true || error?.contains("bad_jwt") == true) {
                    val refreshResult = refreshToken(refreshToken)
                    if (refreshResult.isSuccess) {
                        val newTokens = refreshResult.getOrNull()!!
                        saveTokens(
                            newTokens.access_token,
                            newTokens.refresh_token,
                            newTokens.user?.id ?: "",
                            newTokens.user?.user_metadata?.display_name ?: "User"
                        )

                        val retryResponse = api.addWatchedEpisode(
                            SupabaseApiService.WatchedEpisodeBody(episodeSlug),
                            "Bearer ${newTokens.access_token}"
                        )

                        if (retryResponse.isSuccessful) {
                            Result.success(Unit)
                        } else {
                            Result.failure(Exception(retryResponse.errorBody()?.string() ?: "Error al reintentar añadir episodio"))
                        }
                    } else {
                        Result.failure(Exception("Token expirado y no se pudo renovar"))
                    }
                } else {
                    Result.failure(Exception(error ?: "Error al añadir episodio visto"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    suspend fun deleteWatchedEpisode(
        episodeSlug: String,
        accessToken: String,
        refreshToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteWatchedEpisode(
                SupabaseApiService.WatchedEpisodeDeleteBody(episodeSlug),
                "Bearer $accessToken"
            )

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val error = response.errorBody()?.string()
                if (error?.contains("token is invalid") == true || error?.contains("bad_jwt") == true) {
                    val refreshResult = refreshToken(refreshToken)
                    if (refreshResult.isSuccess) {
                        val newTokens = refreshResult.getOrNull()!!
                        saveTokens(
                            newTokens.access_token,
                            newTokens.refresh_token,
                            newTokens.user?.id ?: "",
                            newTokens.user?.user_metadata?.display_name ?: "User"
                        )

                        val retryResponse = api.deleteWatchedEpisode(
                            SupabaseApiService.WatchedEpisodeDeleteBody(episodeSlug),
                            "Bearer ${newTokens.access_token}"
                        )

                        if (retryResponse.isSuccessful) {
                            Result.success(Unit)
                        } else {
                            Result.failure(Exception(retryResponse.errorBody()?.string() ?: "Error al reintentar borrar episodio"))
                        }
                    } else {
                        Result.failure(Exception("Token expirado y no se pudo renovar"))
                    }
                } else {
                    Result.failure(Exception(error ?: "Error al borrar episodio visto"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getWatchedEpisodes(
        accessToken: String,
        refreshToken: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getWatchedEpisodes("Bearer $accessToken")

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body.episodes)
                } else {
                    Result.failure(Exception("Respuesta vacía"))
                }
            } else {
                val error = response.errorBody()?.string()
                if (error?.contains("token is invalid") == true || error?.contains("bad_jwt") == true) {
                    val refreshResult = refreshToken(refreshToken)
                    if (refreshResult.isSuccess) {
                        val newTokens = refreshResult.getOrNull()!!
                        saveTokens(
                            newTokens.access_token,
                            newTokens.refresh_token,
                            newTokens.user?.id ?: "",
                            newTokens.user?.user_metadata?.display_name ?: "User"
                        )

                        val retryResponse = api.getWatchedEpisodes("Bearer ${newTokens.access_token}")
                        if (retryResponse.isSuccessful) {
                            val retryBody = retryResponse.body()
                            if (retryBody != null) {
                                Result.success(retryBody.episodes)
                            } else {
                                Result.failure(Exception("Respuesta vacía tras retry"))
                            }
                        } else {
                            Result.failure(Exception(retryResponse.errorBody()?.string() ?: "Error al reintentar con nuevo token"))
                        }
                    } else {
                        Result.failure(Exception("Token expirado y no se pudo renovar"))
                    }
                } else {
                    Result.failure(Exception(error ?: "Error al obtener episodios vistos"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWatchedEpisodesByAnime(
        animeSlug: String,
        accessToken: String,
        refreshToken: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getWatchedEpisodesByAnime(animeSlug, "Bearer $accessToken")

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body.episodes)
                } else {
                    Result.failure(Exception("Respuesta vacía"))
                }
            } else {
                val error = response.errorBody()?.string()
                if (error?.contains("token is invalid") == true || error?.contains("bad_jwt") == true) {
                    val refreshResult = refreshToken(refreshToken)
                    if (refreshResult.isSuccess) {
                        val newTokens = refreshResult.getOrNull()!!
                        saveTokens(
                            newTokens.access_token,
                            newTokens.refresh_token,
                            newTokens.user?.id ?: "",
                            newTokens.user?.user_metadata?.display_name ?: "User"
                        )

                        val retryResponse = api.getWatchedEpisodesByAnime(animeSlug, "Bearer ${newTokens.access_token}")
                        if (retryResponse.isSuccessful) {
                            val retryBody = retryResponse.body()
                            if (retryBody != null) {
                                Result.success(retryBody.episodes)
                            } else {
                                Result.failure(Exception("Respuesta vacía tras retry"))
                            }
                        } else {
                            Result.failure(Exception(retryResponse.errorBody()?.string() ?: "Error al reintentar con nuevo token"))
                        }
                    } else {
                        Result.failure(Exception("Token expirado y no se pudo renovar"))
                    }
                } else {
                    Result.failure(Exception(error ?: "Error al obtener episodios vistos para $animeSlug"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun resetPassword(email: String): Result<SupabaseApiService.ResetPasswordResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.resetPassword(SupabaseApiService.ResetPasswordBody(email))

            if(response.isSuccessful){
                Result.success(response.body())
            } else {
                Result.failure(Exception("Ha habido un error al enviar el mail de reseteo de contraseña"))
            }
        } catch (e: Exception){
            Result.failure(e)
        } as Result<SupabaseApiService.ResetPasswordResponse>
    }






    suspend fun refreshToken(refreshToken: String): Result<SupabaseApiService.Refresh> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.refreshToken(SupabaseApiService.RefreshBody(refreshToken))
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e("REFRESH_ERROR", "Error al refrescar token: $errorBody")
                    Result.failure(Exception(errorBody))
                }
            } catch (e: Exception) {
                Log.e("REFRESH_ERROR", "Excepción lanzada: ${e.localizedMessage}", e)
                Result.failure(e)
            }
        }
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getUserId(): String? = prefs.getString("user_id", null)

    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun getUsername(): String? = prefs.getString("username", null)
    fun logout() {
        prefs.edit().clear().apply()
    }
}
