package com.zeusgd.AnimeFlick.data.remote

import com.zeusgd.AnimeFlick.model.AnimeSearched
import retrofit2.Response
import retrofit2.http.*


// -------- Retrofit API --------
interface SupabaseApiService {

    @POST("auth/signup")
    suspend fun signUp(
        @Body body: AuthBody
    ): Response<SignUpResponse>

    @POST("auth/signin")
    suspend fun signIn(
        @Body body: AuthBody
    ): Response<SignInResponse>

    @GET("auth/me")
    suspend fun me(
        @Header("Authorization") bearer: String // "Bearer <access_token>"
    ): Response<MeResponse>

    // Opcional: si quieres leer el texto plano de /auth/verify
    @GET("auth/verify")
    suspend fun verify(): Response<String>

    @GET("favorites")
    suspend fun getFavorites(@Header("Authorization") Bearer: String): Response<FavoriteAnimeResponse>

    @POST("favorites/add")
    suspend fun addFavorite(@Body body: FavoriteBody, @Header("Authorization") Bearer: String): Response<FavoriteAddWrapper>

    @POST("favorites/delete")
    suspend fun deleteFavorite(@Body body: DeleteFavoriteBody, @Header("Authorization") Bearer: String): Response<FavoriteAddWrapper>

    @POST("anime/progress")
    suspend fun setAnimeProgress(@Body body: ProgressBody, @Header("Authorization") Bearer: String): Response<ProgressAddWrapper>

    @GET("anime/progress")
    suspend fun getAnimeProgress(@Query("status") status: String, @Header("Authorization") Bearer: String): Response<ProgressAnimeReponse>

    @POST("anime/watched")
    suspend fun addWatchedEpisode(@Body body: WatchedEpisodeBody, @Header("Authorization") bearer: String): Response<WatchedEpisodeAddResponse>

    @POST("anime/watched/delete")
    suspend fun deleteWatchedEpisode(@Body body: WatchedEpisodeDeleteBody, @Header("Authorization") bearer: String): Response<WatchedEpisodeAddResponse>

    @GET("anime/watched")
    suspend fun getWatchedEpisodes(@Header("Authorization") bearer: String): Response<WatchedEpisodeListResponse>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordBody): Response<ResetPasswordResponse>


    @POST("auth/refresh")
    suspend fun refreshToken(@Body body: RefreshBody): Response<Refresh>



    // -------- DTOs --------
    data class AuthBody(
        val email: String,
        val password: String,
        val display_name: String? = null   // opcional, solo se usa en signup
    )

    data class ResetPasswordBody(
        val email: String
    )

    data class ResetPasswordResponse(
        val message: String
    )

    data class FavoriteAnimeResponse(
        val message: String?,
        val favorites: List<AnimeSearched>
    )

    data class ProgressAnimeReponse(
        val message: String?,
        val animes: List<AnimeSearched>
    )

    data class favoriteAnime(
        val anime: AnimeSearched
    )

    data class RefreshBody(
        val refresh_token: String?
    )

    data class Refresh(
        val access_token: String,
        val refresh_token: String,
        val token_type: String?,
        val expires_in: Long?,
        val user: SupabaseUser?
    )

    data class FavoriteBody(
        val anime_slug: String,
        val title: String,
        val cover: String,
        val rating: String,
        val type: String
    )

    data class ProgressBody(
        val anime_slug: String,
        val title: String,
        val cover: String,
        val rating: String,
        val type: String,
        val status: String
    )

    data class ProgressAddWrapper(
        val message: String,
        val data: List<ProgressInsertResponse>
    )

    data class ProgressInsertResponse(
        val id: Int,
        val user_id: String,
        val anime_slug: String,
        val created_at: String
    )

    data class DeleteFavoriteBody(
        val anime_slug: String
    )

    data class SupabaseUser(
        val id: String,
        val email: String? = null,
        val phone: String? = null,
        val display_name: String?,
        val user_metadata: SupabaseMetadataUser? = null
    )

    data class SupabaseMetadataUser(
        val display_name: String?
    )

    data class FavoriteAddWrapper(
        val message: String,
        val data: List<FavoritoInsertResponse>
    )

    data class FavoritoInsertResponse(
        val id: Int,
        val user_id: String,
        val anime_slug: String,
        val created_at: String
    )

    data class SignInResponse(
        val access_token: String?,
        val refresh_token: String?,
        val token_type: String? = null,
        val expires_in: Long? = null,
        val user: SupabaseUser? = null,
        val status: String? = null // "complete" en tu backend
    )

    data class SignUpResponse(
        val message: String? = null,           // "Sign up OK..." (tu backend)
        val access_token: String? = null,      // si desactivas confirmación por email
        val refresh_token: String? = null,     // idem
        val user: SupabaseUser? = null
    )

    data class WatchedEpisodeBody(
        val episode_slug: String
    )

    data class WatchedEpisodeDeleteBody(
        val episode_slug: String
    )

    data class WatchedEpisodeAddResponse(
        val message: String,
        val data: List<WatchedEpisodeInserted>
    )

    data class WatchedEpisodeInserted(
        val id: Int,
        val user_id: String,
        val episode_slug: String,
        val created_at: String
    )

    data class WatchedEpisodeListResponse(
        val message: String,
        val episodes: List<String> // lista de slugs
    )

}

// Si quieres mapear /auth/me tal cual:
typealias MeResponse = SupabaseApiService.SupabaseUser
