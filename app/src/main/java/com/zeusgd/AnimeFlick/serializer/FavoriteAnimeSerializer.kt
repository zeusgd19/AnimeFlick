package com.zeusgd.AnimeFlick.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.zeusgd.AnimeFlick.FavoriteAnimes
import java.io.InputStream
import java.io.OutputStream

object FavoriteAnimesSerializer : Serializer<FavoriteAnimes> {
    override val defaultValue: FavoriteAnimes = FavoriteAnimes.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): FavoriteAnimes {
        try {
            return FavoriteAnimes.parseFrom(input)
        } catch (e: Exception) {
            throw CorruptionException("Cannot read proto.", e)
        }
    }

    override suspend fun writeTo(t: FavoriteAnimes, output: OutputStream) {
        t.writeTo(output)
    }
}
