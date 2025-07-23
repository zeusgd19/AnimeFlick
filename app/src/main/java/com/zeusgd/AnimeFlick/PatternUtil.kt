package com.zeusgd.AnimeFlick

import java.util.regex.Pattern

object  PatternUtil {
    fun extractLink(html: String): String? {
        val matcher = Pattern.compile("https?://[a-zA-Z0-9.=?/!&#_\\-]+|/[a-zA-Z0-9.=?/!&#_\\-]+").matcher(html)
        matcher.find()
        return matcher.group(0)
    }

    fun yuvideoLink(link: String): String? {
        val pattern = Pattern.compile("file: ?'(.*vidcache.*mp4)'")
        val matcher = pattern.matcher(link)
        matcher.find()
        return matcher.group(1)
    }
}