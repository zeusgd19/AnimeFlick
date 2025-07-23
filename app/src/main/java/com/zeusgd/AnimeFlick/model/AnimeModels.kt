package com.zeusgd.AnimeFlick.model

data class AnimeSearchResponse(
    val data: Media
)

data class AnimeRecentEpisodesResponse(
    val data: List<RecentEpisode>
)

data class AnimeEpisodeResponse(
    val data: Episodes
)

data class AnimesOnAirResponse(
    val data: List<AnimesOnAir>
)

data class AnimesOnAir(
    val title: String,
    val type: String,
    val slug: String,
    val url: String
)

data class AiringAnime(
    val title: String,
    val slug: String,
    val airingData: String,
    val cover : String?,
)

data class AnimeInfoResponse(
    val data: Anime
)

data class AnimeEmbedPlayerReponse(
    val data: Servers
)

data class Servers(
    val servers: List<Server>
)

data class Server(
    val name: String,
    val download: String,
    val embed: String
)

data class Anime(
    val title: String,
    val cover: String,
    val synopsis: String,
    val status: String,
    val rating: String,
    val genres: List<String>,
    val next_airing_episode: String?,
    val episodes: List<Episode>
)

data class Media(
    val media: List<AnimeSearched>
)

data class AnimeSearched(
    val title: String,
    val cover: String,
    val slug: String,
    val rating: String,
    val type: String
)

data class Episodes(
    val episodes: List<Episode>
)

data class Title(
    val romaji: String
)


data class Episode(
    val number: Int,
    val url: String,
    val slug: String
)

data class RecentEpisode(
    val title: String,
    val cover: String,
    val number: Int
)
