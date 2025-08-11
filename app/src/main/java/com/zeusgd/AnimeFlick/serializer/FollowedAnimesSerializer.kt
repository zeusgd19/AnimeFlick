package com.zeusgd.AnimeFlick.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.zeusgd.AnimeFlick.FollowedAnimes
import java.io.InputStream
import java.io.OutputStream

object FollowedAnimesSerializer : Serializer<FollowedAnimes> {
    override val defaultValue: FollowedAnimes = FollowedAnimes.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): FollowedAnimes {
        try {
            return FollowedAnimes.parseFrom(input)
        } catch (e: Exception) {
            throw CorruptionException("Cannot read proto.", e)
        }
    }

    override suspend fun writeTo(t: FollowedAnimes, output: OutputStream) {
        t.writeTo(output)
    }
}
