package com.zeusgd.AnimeFlick.network

import com.zeusgd.AnimeFlick.LibreTranslateApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "https://animeflv.ahmedrangel.com/"
    private const val BASE_URL2 = "https://api-anime-swart.vercel.app/"

    val api: AnimeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnimeApiService::class.java)
    }

    val apiVercel: AnimeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL2)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnimeApiService::class.java)
    }

    val translateApi = Retrofit.Builder()
        .baseUrl("https://webapi.laratranslate.com/") // o tu instancia si montas una
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LibreTranslateApi::class.java)



}
