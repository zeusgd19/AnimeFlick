package com.zeusgd.AnimeFlick

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.*
import okhttp3.OkHttpClient
import okhttp3.Request as OkRequest
import java.io.InputStream

/**
 * DataSource personalizado para Zilla-Networks.
 * Los segmentos HLS tienen extensión .html con Content-Type: text/html
 * pero son binarios MPEG-TS. Esta clase los intercepta y los sirve
 * a ExoPlayer como datos crudos sin procesar el Content-Type.
 */
class ZillaDataSource(
    private val delegate: DataSource
) : DataSource {

    private var okInputStream: InputStream? = null
    private var okResponse: okhttp3.Response? = null
    private var isZillaSegment = false
    private var totalBytes = -1L
    private var currentUri: android.net.Uri? = null

    override fun addTransferListener(transferListener: TransferListener) {
        delegate.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val uriString = dataSpec.uri.toString()
        // Interceptamos TODOS los .html de zilla-networks (segmentos + init.html)
        isZillaSegment = uriString.contains("zilla-networks.com/segs/") && uriString.endsWith(".html")
        currentUri = dataSpec.uri

        return if (isZillaSegment) {
            android.util.Log.d("ZillaDataSource", "Interceptando segmento: $uriString")
            val request = OkRequest.Builder()
                .url(uriString)
                .addHeader("Accept", "*/*")
                .build()
            val response = sharedClient.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                throw HttpDataSource.HttpDataSourceException(
                    "Respuesta ${response.code} para segmento Zilla",
                    dataSpec,
                    HttpDataSource.HttpDataSourceException.TYPE_OPEN
                )
            }
            val body = response.body
            if (body == null) {
                response.close()
                throw HttpDataSource.HttpDataSourceException(
                    "Body vacío para segmento Zilla",
                    dataSpec,
                    HttpDataSource.HttpDataSourceException.TYPE_OPEN
                )
            }
            okResponse = response
            totalBytes = body.contentLength()
            okInputStream = body.byteStream()
            if (totalBytes < 0) C.LENGTH_UNSET.toLong() else totalBytes
        } else {
            delegate.open(dataSpec)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        return if (isZillaSegment) {
            val stream = okInputStream ?: return C.RESULT_END_OF_INPUT
            val read = stream.read(buffer, offset, readLength)
            if (read == -1) C.RESULT_END_OF_INPUT else read
        } else {
            delegate.read(buffer, offset, readLength)
        }
    }

    // Siempre devolvemos la URI actual — StatsDataSource exige que no sea null
    override fun getUri(): android.net.Uri? = if (isZillaSegment) currentUri else delegate.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        if (isZillaSegment) emptyMap() else delegate.responseHeaders

    override fun close() {
        if (isZillaSegment) {
            okInputStream?.close()
            okInputStream = null
            okResponse?.close()
            okResponse = null
        } else {
            delegate.close()
        }
    }

    companion object {
        // Cliente compartido — reutiliza conexiones HTTP entre todos los segmentos
        val sharedClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    class Factory(private val context: Context) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            val httpFactory = DefaultHttpDataSource.Factory()
            val defaultFactory = DefaultDataSource.Factory(context, httpFactory)
            return ZillaDataSource(defaultFactory.createDataSource())
        }
    }
}
