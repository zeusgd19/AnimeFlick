
package com.zeusgd.AnimeFlick

import android.content.Context
import android.widget.Toast
import com.google.android.exoplayer2.util.Log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

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

    suspend fun extract(server: String, embedUrl: String, context: Context): Pair<String, Map<String, String>>? {
        return when (server.lowercase()) {
            "yourupload" -> extractYourUploadVideo(embedUrl, context)
            "stape" -> extractStapeVideo(embedUrl, context)
            else -> null
        }
    }
}
