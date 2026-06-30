package com.zeusgd.AnimeFlick.network

import com.zeusgd.AnimeFlick.LibreTranslateApi
import com.zeusgd.AnimeFlick.data.remote.SupabaseApiService
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private const val BASE_URL = "https://animeflick.com/"
    private const val BASE_URL2 = "https://zeusgd19.pythonanywhere.com/"

    // Cache de 10 MB en disco para evitar llamadas repetidas a Vercel
    private val cacheDir: File by lazy {
        File(System.getProperty("java.io.tmpdir") ?: "/tmp", "animeflick_http_cache")
    }
    private val cache: Cache by lazy { Cache(cacheDir, 10L * 1024 * 1024) } // 10 MB

    // Interceptor que añade Cache-Control a las respuestas si el servidor no lo incluye
    private val cacheInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        // Si el servidor ya envía Cache-Control, lo respetamos.
        // Si no, forzamos un cache de 5 minutos en el cliente.
        if (response.header("Cache-Control").isNullOrEmpty()) {
            response.newBuilder()
                .header("Cache-Control", "public, max-age=300")
                .removeHeader("Pragma")
                .build()
        } else {
            response
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(cache)
            .addNetworkInterceptor(cacheInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val api: AnimeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnimeApiService::class.java)
    }

    val pyhtonApiSupabase: SupabaseApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL2)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseApiService::class.java)
    }

    val translateApi: LibreTranslateApi = Retrofit.Builder()
        .baseUrl("https://webapi.laratranslate.com/") // o tu instancia si montas una
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LibreTranslateApi::class.java)
}
