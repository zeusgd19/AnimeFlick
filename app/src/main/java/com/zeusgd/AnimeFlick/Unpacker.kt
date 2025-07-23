package com.zeusgd.AnimeFlick

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object Unpacker {
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
}