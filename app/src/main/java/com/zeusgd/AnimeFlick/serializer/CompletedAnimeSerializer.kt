package com.zeusgd.AnimeFlick.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.zeusgd.AnimeFlick.CompletedAnimes
import com.zeusgd.AnimeFlick.FavoriteAnimes
import java.io.InputStream
import java.io.OutputStream

object CompletedAnimeSerializer : Serializer<CompletedAnimes> {
    override val defaultValue: CompletedAnimes = CompletedAnimes.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): CompletedAnimes {
        try {
            return CompletedAnimes.parseFrom(input)
        } catch (e: Exception) {
            throw CorruptionException("Cannot read proto.", e)
        }
    }

    override suspend fun writeTo(t: CompletedAnimes, output: OutputStream) {
        t.writeTo(output)
    }
}
