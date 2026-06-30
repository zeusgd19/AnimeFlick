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

    suspend fun extractMegaVideo(url: String): Pair<String, Map<String, String>>? {
        return try {
            val downLink = PatternUtil.extractLink(url) ?: return null

            Pair(downLink, emptyMap())
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

    suspend fun extractZillaNetworksVideo(url: String, context: Context): Pair<String, Map<String, String>>? =
        withContext(Dispatchers.IO) {
            try {
                // La playlist está en /m3u8/{id}
                val m3u8Url = url.replace("/play/", "/m3u8/")
                android.util.Log.d("ZillaExtractor", "Descargando m3u8: $m3u8Url")

                val client = OkHttpClient()
                val request = Request.Builder().url(m3u8Url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    android.util.Log.e("ZillaExtractor", "Error al descargar m3u8: ${response.code}")
                    return@withContext null
                }
                val rawM3u8 = response.body?.string() ?: return@withContext null
                response.close()

                // IMPORTANTE: Los segmentos tienen extensión .html pero son binarios TS.
                // NO cambiamos la extensión porque .ts devuelve 404.
                // El reproductor usará un DataSource personalizado que sobreescribe el Content-Type.
                // Solo guardamos el m3u8 en caché tal cual.
                val tmpFile = java.io.File(context.cacheDir, "zilla_playlist.m3u8")
                tmpFile.writeText(rawM3u8, Charsets.UTF_8)
                android.util.Log.d("ZillaExtractor", "M3U8 guardado en: ${tmpFile.absolutePath}")

                Pair(tmpFile.toURI().toString(), emptyMap())
            } catch (e: Exception) {
                android.util.Log.e("ZillaExtractor", "Error extrayendo Zilla", e)
                null
            }
        }

    suspend fun extract(server: String, embedUrl: String, context: Context): Pair<String, Map<String, String>>? {
        val serverLower = server.lowercase()
        
        // 1. Intentamos por el nombre explícito del servidor
        val result = when (serverLower) {
            "yourupload" -> extractYourUploadVideo(embedUrl, context)
            "stape", "streamtape" -> extractStapeVideo(embedUrl, context)
            "sw", "streamwish" -> extractStreamWishVideo(embedUrl, context)
            "mega" -> extractMegaVideo(embedUrl)
            "upnshare", "upn" -> null // Forzamos el fallback al WebView visible "Sandboxed"
            "hls" -> {
                if (embedUrl.contains("zilla-networks", ignoreCase = true)) {
                    extractZillaNetworksVideo(embedUrl, context)
                } else null
            }
            else -> null
        }
        
        if (result != null) return result
        
        // 2. Si el nombre del servidor es genérico (ej. "TioAnime"), buscamos por el dominio de la URL
        return when {
            embedUrl.contains("yourupload", ignoreCase = true) -> extractYourUploadVideo(embedUrl, context)
            embedUrl.contains("streamtape", ignoreCase = true) || embedUrl.contains("stape", ignoreCase = true) -> extractStapeVideo(embedUrl, context)
            embedUrl.contains("streamwish", ignoreCase = true) || embedUrl.contains("strwish", ignoreCase = true) || embedUrl.contains("sw", ignoreCase = true) -> extractStreamWishVideo(embedUrl, context)
            embedUrl.contains("mega.nz", ignoreCase = true) -> extractMegaVideo(embedUrl)
            embedUrl.contains("zilla-networks", ignoreCase = true) -> extractZillaNetworksVideo(embedUrl, context)
            embedUrl.contains("uns.bio", ignoreCase = true) || embedUrl.contains("upnshare", ignoreCase = true) -> null // Fallback a WebView visible
            else -> null
        }
    }
}
