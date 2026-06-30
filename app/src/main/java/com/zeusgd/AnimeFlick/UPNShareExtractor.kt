package com.zeusgd.AnimeFlick

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Extractor para UPNShare (animeav1.uns.bio y similares).
 *
 * La API del servidor devuelve datos cifrados con AES-CBC, por lo que no se
 * puede descifrar directamente. En su lugar, cargamos la página en un WebView
 * invisible y esperamos a que el reproductor interno solicite la URL del stream
 * (.m3u8). Interceptamos esa request con shouldInterceptRequest y la devolvemos
 * para reproducirla en ExoPlayer.
 */
object UPNShareExtractor {

    private val M3U8_REGEX = Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""")

    /**
     * Extrae la URL del stream .m3u8 cargando la página en un WebView oculto.
     * @param embedUrl URL de embed, ej. https://animeav1.uns.bio/#mpeetv
     * @param context  Contexto Android
     * @param timeoutMs Tiempo máximo de espera en ms (default 15s)
     * @return Par (url_m3u8, headers) o null si no se encontró
     */
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun extract(
        embedUrl: String,
        context: Context,
        timeoutMs: Long = 15_000L
    ): Pair<String, Map<String, String>>? {
        android.util.Log.d("UPNShareExtractor", "Iniciando extracción para: $embedUrl")

        val result = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val mainHandler = Handler(Looper.getMainLooper())
                var webView: WebView? = null
                var resumed = false

                fun resume(value: Pair<String, Map<String, String>>?) {
                    if (!resumed) {
                        resumed = true
                        cont.resume(value)
                        mainHandler.post {
                            webView?.stopLoading()
                            webView?.destroy()
                            webView = null
                        }
                    }
                }

                mainHandler.post {
                    val wv = WebView(context)
                    webView = wv

                    wv.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                        allowContentAccess = true
                        databaseEnabled = true
                    }
                    wv.webChromeClient = WebChromeClient()

                    wv.webViewClient = object : WebViewClient() {

                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            // Mutear videos y forzar play vía JS si es posible
                            view.evaluateJavascript("""
                                setInterval(function() {
                                    var vids = document.querySelectorAll('video');
                                    vids.forEach(v => {
                                        v.muted = true;
                                        if (v.paused) {
                                            var p = v.play();
                                            if (p !== undefined) p.catch(e => {});
                                        }
                                    });
                                }, 500);
                            """.trimIndent(), null)
                            
                            // Empezar a hacer taps NATIVOS en el centro del WebView
                            mainHandler.postDelayed(object : Runnable {
                                override fun run() {
                                    if (webView == null) return
                                    val wv = webView!!
                                    val x = wv.width / 2f
                                    val y = wv.height / 2f
                                    if (x > 0 && y > 0) {
                                        val downTime = android.os.SystemClock.uptimeMillis()
                                        val eventDown = android.view.MotionEvent.obtain(downTime, downTime, android.view.MotionEvent.ACTION_DOWN, x, y, 0)
                                        wv.dispatchTouchEvent(eventDown)
                                        
                                        val eventUp = android.view.MotionEvent.obtain(downTime, downTime + 50, android.view.MotionEvent.ACTION_UP, x, y, 0)
                                        wv.dispatchTouchEvent(eventUp)
                                        
                                        eventDown.recycle()
                                        eventUp.recycle()
                                    }
                                    mainHandler.postDelayed(this, 500)
                                }
                            }, 1000)
                        }

                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            val url = request.url.toString()
                            
                            // Aceptamos cualquier .m3u8, incluyendo cf-master (el master playlist)
                            if (url.contains(".m3u8")) {
                                android.util.Log.d("UPNShareExtractor", "✅ M3U8 encontrado: $url")

                                // Pausamos el video del webview oculto inmediatamente
                                mainHandler.post {
                                    view.evaluateJavascript("document.querySelectorAll('video').forEach(v => v.pause());", null)
                                }

                                val referer = request.requestHeaders["Referer"] ?: embedUrl
                                val headers = mapOf(
                                    "Referer" to referer,
                                    "Origin" to "https://animeav1.uns.bio",
                                    "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                )

                                resume(Pair(url, headers))
                            }

                            if (url.contains(".mp4") && url.contains("download")) {
                                android.util.Log.d("UPNShareExtractor", "✅ MP4 encontrado: $url")
                                resume(Pair(url, emptyMap()))
                            }

                            return null
                        }
                    }

                    wv.loadUrl(embedUrl)

                    // Si el usuario cancela la coroutine, limpiamos
                    cont.invokeOnCancellation {
                        mainHandler.post {
                            wv.stopLoading()
                            wv.destroy()
                        }
                    }
                }
            }
        }

        if (result == null) {
            android.util.Log.w("UPNShareExtractor", "Timeout esperando m3u8 de: $embedUrl")
        }
        return result
    }
}
