package com.zeusgd.AnimeFlick

import android.content.Context
import de.prosiebensat1digital.oasisjsbridge.JsBridge
import de.prosiebensat1digital.oasisjsbridge.JsBridgeConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object Unpacker {

    private val packedRegex2 =
        "eval\\((function\\(p,a,c,k,e,?[dr]?\\).*.split\\('\\|'\\).*)\\)".toRegex()
    suspend fun getHtml(context: Context, link: String, delay: Long = 5000): String? {
        val evaluator = WebJS(context)
        return withContext(Dispatchers.Main) {
            suspendCoroutine { continuation ->
                evaluator.evalOnFinish(
                    link,
                    "(\"<html>\"+document.getElementsByTagName(\"html\")[0].innerHTML+\"<\\/html>\")",
                    delay
                ) {
                    continuation.resume(it)
                }
            }
        }
    }

    suspend fun unpackWeb(context: Context, link: String): String {
        val safeContext = context.applicationContext
        val html = getHtml(context, link) ?: return ""
        val packedCode = packedRegex2.find(html)?.destructured?.component1()
        if (packedCode == null) return html
        val jsBridge = JsBridge(JsBridgeConfig.bareConfig(), safeContext)
        return jsBridge.evaluateBlocking("function prnt() {var txt = $packedCode; return txt;}prnt();")
    }

}