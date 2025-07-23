package com.example.animeflick.datastore

import com.zeusgd.AnimeFlick.serializer.SeenEpisodesSerializer
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.zeusgd.AnimeFlick.FavoriteAnimes
import com.zeusgd.AnimeFlick.serializer.FavoriteAnimesSerializer
import com.zeusgd.AnimeFlick.SeenEpisodes

val Context.favoritesDataStore: DataStore<FavoriteAnimes> by dataStore(
    fileName = "favorites.pb",
    serializer = FavoriteAnimesSerializer
)

val Context.seenEpisodesDataStore: DataStore<SeenEpisodes> by dataStore(
    fileName = "seen_episodes.pb",
    serializer = SeenEpisodesSerializer
)
