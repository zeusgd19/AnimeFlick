package com.zeusgd.AnimeFlick.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.zeusgd.AnimeFlick.FavoriteAnimes
import com.zeusgd.AnimeFlick.SeenEpisodes
import java.io.InputStream
import java.io.OutputStream

object SeenEpisodesSerializer : Serializer<SeenEpisodes> {
    override val defaultValue: SeenEpisodes = SeenEpisodes.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SeenEpisodes {
        try {
            return SeenEpisodes.parseFrom(input)
        } catch (e: Exception) {
            throw CorruptionException("Cannot read proto.", e)
        }
    }

    override suspend fun writeTo(t: SeenEpisodes, output: OutputStream) {
        t.writeTo(output)
    }
}
