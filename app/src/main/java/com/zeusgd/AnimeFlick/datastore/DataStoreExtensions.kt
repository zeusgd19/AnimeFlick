package com.example.animeflick.datastore

import com.zeusgd.AnimeFlick.serializer.SeenEpisodesSerializer
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.zeusgd.AnimeFlick.CompletedAnimes
import com.zeusgd.AnimeFlick.FavoriteAnimes
import com.zeusgd.AnimeFlick.FollowedAnimes
import com.zeusgd.AnimeFlick.PausedAnimes
import com.zeusgd.AnimeFlick.serializer.FavoriteAnimesSerializer
import com.zeusgd.AnimeFlick.SeenEpisodes
import com.zeusgd.AnimeFlick.serializer.CompletedAnimeSerializer
import com.zeusgd.AnimeFlick.serializer.FollowedAnimesSerializer
import com.zeusgd.AnimeFlick.serializer.PausedAnimeSerializer

val Context.favoritesDataStore: DataStore<FavoriteAnimes> by dataStore(
    fileName = "favorites.pb",
    serializer = FavoriteAnimesSerializer
)

val Context.seenEpisodesDataStore: DataStore<SeenEpisodes> by dataStore(
    fileName = "seen_episodes.pb",
    serializer = SeenEpisodesSerializer
)

val Context.followedDataStore: DataStore<FollowedAnimes> by dataStore(
    fileName = "followed.pb",
    serializer = FollowedAnimesSerializer
)

val Context.completedDataStore: DataStore<CompletedAnimes> by dataStore(
    fileName = "completed.pb",
    serializer = CompletedAnimeSerializer
)

val Context.pausedDataStore: DataStore<PausedAnimes> by dataStore(
    fileName = "paused.pb",
    serializer = PausedAnimeSerializer
)
