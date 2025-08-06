
package com.zeusgd.AnimeFlick

import android.content.Context
import android.widget.Toast
import com.google.android.exoplayer2.util.Log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.ConnectionSpec
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.text.StringEscapeUtils
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.helper.HttpConnection

object VideoExtractor {

    private val packedRegex2 =
        "eval\\((function\\(p,a,c,k,e,?[dr]?\\).*.split\\('\\|'\\).*)\\)".toRegex()

    suspend fun extractYourUploadVideo(url: String, context: Context): Pair<String, Map<String, String>>? =
        withContext(Dispatchers.IO) {
            try {
                val downLink = PatternUtil.extractLink(url)
                val videoLink = PatternUtil.yuvideoLink(runBlocking(Dispatchers.Main) {
                    downLink?.let { Unpacker.getHtml(context, it, 8000) }!!
                })

                val client = OkHttpClient().newBuilder()
                    .connectionSpecs(
                        listOf(
                            ConnectionSpec.CLEARTEXT,
                            ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                                .allEnabledTlsVersions()
                                .allEnabledCipherSuites()
                                .build()
                        )
                    )
                    .followRedirects(false)
                    .build()

                val request = if (videoLink != null && downLink != null) {
                    Request.Builder()
                        .url(videoLink)
                        .addHeader("Referer", downLink)
                        .build()
                } else {
                    null
                }

                val response = request?.let { client.newCall(it).execute() }
                val refVideoLink = response?.header("Location")
                if (response != null) {
                    response.close()
                }

                val headers = mapOf(
                    "Range" to "bytes=0-",
                    "Referer" to "https://www.yourupload.com/"
                )

                return@withContext refVideoLink?.let { Pair(it, headers) }

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }

    suspend fun extractStapeVideo(url: String, context: Context): Pair<String, Map<String, String>>? =
        withContext(Dispatchers.IO) {
            try {
                val downLink = PatternUtil.extractLink(url)
                Log.d("STAPE", "DownLink extraído: $downLink")
                if (downLink.isNullOrEmpty()) return@withContext null

                val html = withContext(Dispatchers.Main) {
                    Unpacker.getHtml(context, downLink)
                }
                Log.d("STAPE", "HTML recibido: ${html?.take(200)}")


                val doc = Jsoup.parse(html ?: "", "https://streamtape.com")
                val videoSrc = doc.select("video#mainvideo").attr("src")
                Log.d("STAPE", "videoSrc extraído: $videoSrc")

                if (videoSrc.isNullOrEmpty()) return@withContext null



                val intermediateUrl = if (videoSrc.startsWith("http")) videoSrc else "https:$videoSrc"

                val client = OkHttpClient.Builder()
                    .followRedirects(false)
                    .build()

                val request = Request.Builder()
                    .url(intermediateUrl)
                    .addHeader("Referer", downLink)
                    .addHeader("User-Agent", "Mozilla/5.0 (Android) AppleWebKit/537.36 Chrome/114.0.0.0 Mobile Safari/537.36")
                    .build()

                val response = client.newCall(request).execute()
                val redirectedUrl = response.header("Location")
                response.close()

                if (redirectedUrl.isNullOrEmpty()) return@withContext null

                val headers = mapOf(
                    "Referer" to redirectedUrl,
                    "Range" to "bytes=0-" // 👈 Esto es lo que evita el audio desfasado
                )

                return@withContext Pair(redirectedUrl, headers)

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }

    suspend fun extractOkruVideo(baseLink: String): List<Option> {
        return withContext(Dispatchers.IO) {
            try {
                val downLink = PatternUtil.extractLink(baseLink) // Tu función que obtiene el enlace directo
                val doc = Jsoup.connect(downLink)
                    .userAgent(HttpConnection.DEFAULT_UA)
                    .get()

                val html = doc.html()

                val (source) = Regex("data-options=\"(.*?)\"").find(html)?.destructured
                    ?: throw IllegalStateException("No se encontró el atributo data-options")

                val jsonText = StringEscapeUtils.unescapeHtml4(source)

                val videosJson = JSONObject(
                    JSONObject(jsonText)
                        .getJSONObject("flashvars")
                        .getString("metadata")
                ).getJSONArray("videos")

                val options = mutableListOf<Option>()

                for (i in 0 until videosJson.length()) {
                    val video = videosJson.getJSONObject(i)
                    val url = video.getString("url")
                    val name = when (video.getString("name")) {
                        "mobile" -> "144p"
                        "lowest" -> "240p"
                        "low" -> "360p"
                        "sd" -> "480p"
                        "hd" -> "720p"
                        "full" -> "1080p"
                        "quad" -> "2000p"
                        "ultra" -> "4000p"
                        else -> "Default"
                    }

                    val headers = mapOf(
                        "User-Agent" to HttpConnection.DEFAULT_UA,
                    )

                    options.add(
                        Option(
                            name = "Okru",
                            quality = name,
                            url = url,
                            headers = headers
                        )
                    )
                }

                if (options.isEmpty()) throw Exception("No se encontraron calidades")

                options

            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun extractStreamWishVideo(url: String, context: Context): Pair<String, Map<String, String>>? {
        return try {
            val downLink = PatternUtil.extractLink(url) ?: return null
            val unpack = Unpacker.unpackWeb(context, downLink)
            val option = """(?:hls\d"|file): ?"((http[^\"]+m3u8[^\"]*))""".toRegex().findAll(unpack).first()
            val (link) = option.destructured

            // Sin headers personalizados, igual que Ukiku
            Pair(link, emptyMap())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class Option(
        val name: String,              // Nombre del servidor o del anime
        val quality: String,           // Ej: "480p", "720p"
        val url: String,               // Enlace al vídeo
        val headers: Map<String, String> = emptyMap() // Cabeceras opcionales
    )

    suspend fun extract(server: String, embedUrl: String, context: Context): Pair<String, Map<String, String>>? {
        return when (server.lowercase()) {
            "yourupload" -> extractYourUploadVideo(embedUrl, context)
            "stape" -> extractStapeVideo(embedUrl, context)
            "sw" -> extractStreamWishVideo(embedUrl, context)
            else -> null
        }
    }
}
