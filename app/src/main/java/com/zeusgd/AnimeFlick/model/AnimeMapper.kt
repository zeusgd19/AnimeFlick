package com.zeusgd.AnimeFlick.model

import com.zeusgd.AnimeFlick.FavoriteAnime

fun AnimeSearched.toProto(): FavoriteAnime {
    return FavoriteAnime.newBuilder()
        .setTitle(title)
        .setCover(cover)
        .setSlug(slug)
        .setRating(rating)
        .setType(type)
        .build()
}
