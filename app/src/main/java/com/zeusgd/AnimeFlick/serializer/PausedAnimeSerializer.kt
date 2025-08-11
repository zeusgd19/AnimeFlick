package com.zeusgd.AnimeFlick.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.zeusgd.AnimeFlick.CompletedAnimes
import com.zeusgd.AnimeFlick.FavoriteAnimes
import com.zeusgd.AnimeFlick.PausedAnimes
import java.io.InputStream
import java.io.OutputStream

object PausedAnimeSerializer : Serializer<PausedAnimes> {
    override val defaultValue: PausedAnimes = PausedAnimes.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): PausedAnimes {
        try {
            return PausedAnimes.parseFrom(input)
        } catch (e: Exception) {
            throw CorruptionException("Cannot read proto.", e)
        }
    }

    override suspend fun writeTo(t: PausedAnimes, output: OutputStream) {
        t.writeTo(output)
    }
}
