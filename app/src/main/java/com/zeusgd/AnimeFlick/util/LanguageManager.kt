package com.zeusgd.AnimeFlick.util

import android.content.Context
import android.content.SharedPreferences
import java.util.*

object LanguageManager {
    private const val PREF_NAME = "app_preferences"
    private const val KEY_LANG = "language"

    fun getSavedLocale(context: Context): Locale? {
        val lang = getPrefs(context).getString(KEY_LANG, null)
        return lang?.let { Locale(it) }
    }

    fun setLocale(context: Context, langCode: String) {
        getPrefs(context).edit().putString(KEY_LANG, langCode).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun applyLocale(context: Context): Context {
        val lang = getSavedLocale(context)?.language ?: return context
        val config = context.resources.configuration
        val locale = Locale(lang)
        Locale.setDefault(locale)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
