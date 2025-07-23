package com.zeusgd.AnimeFlick.network

import com.zeusgd.AnimeFlick.model.AiringAnime
import com.zeusgd.AnimeFlick.model.AnimeEmbedPlayerReponse
import com.zeusgd.AnimeFlick.model.AnimeInfoResponse
import com.zeusgd.AnimeFlick.model.AnimeRecentEpisodesResponse
import com.zeusgd.AnimeFlick.model.AnimeSearchResponse
import com.zeusgd.AnimeFlick.model.AnimesOnAirResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AnimeApiService {
    @GET("api/search")
    suspend fun searchAnime(@Query("query") query: String): AnimeSearchResponse

    @GET("api/anime/{slug}")
    suspend fun getAnimeInfo(@Path("slug") slug: String): AnimeInfoResponse

    @GET("api/list/latest-episodes")
    suspend fun getRecentEpisodes(): AnimeRecentEpisodesResponse

    @GET("api/anime/episode/{slug}")
    suspend fun getEmbedPlayer(@Path("slug") slug: String): AnimeEmbedPlayerReponse

    @GET("api/airing-animes")
    suspend fun getGroupedAiringAnimes(): Map<String, List<AiringAnime>>

    @GET("api/airing-animes")
    suspend fun getAiringAnimesByDay(@Query("day") day: String): List<AiringAnime>

    @POST("api/search/by-filter")
    suspend fun getAnimesFiltering(@Query("order") order: String, @Query("page") page: Int, @Body filter: AnimeFilterRequest): AnimeSearchResponse


    data class AnimeFilterRequest(
        val types: List<String> = emptyList(),
        val genres: List<String> = emptyList(),
        val statuses: List<Int> = emptyList()
    )
}
