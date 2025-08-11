package com.zeusgd.AnimeFlick.model

import com.zeusgd.AnimeFlick.CompletedAnime
import com.zeusgd.AnimeFlick.FavoriteAnime
import com.zeusgd.AnimeFlick.FollowedAnime
import com.zeusgd.AnimeFlick.FollowedAnimes
import com.zeusgd.AnimeFlick.PausedAnime

fun AnimeSearched.toProto(): FavoriteAnime {
    return FavoriteAnime.newBuilder()
        .setTitle(title)
        .setCover(cover)
        .setSlug(slug)
        .setRating(rating)
        .setType(type)
        .build()
}

fun AnimeSearched.toProtoFollowed(): FollowedAnime {
    return FollowedAnime.newBuilder()
        .setTitle(title)
        .setCover(cover)
        .setSlug(slug)
        .setRating(rating)
        .setType(type)
        .build()
}

fun AnimeSearched.toProtoCompleted(): CompletedAnime {
    return CompletedAnime.newBuilder()
        .setTitle(title)
        .setCover(cover)
        .setSlug(slug)
        .setRating(rating)
        .setType(type)
        .build()
}

fun AnimeSearched.toProtoPaused(): PausedAnime {
    return PausedAnime.newBuilder()
        .setTitle(title)
        .setCover(cover)
        .setSlug(slug)
        .setRating(rating)
        .setType(type)
        .build()
}
