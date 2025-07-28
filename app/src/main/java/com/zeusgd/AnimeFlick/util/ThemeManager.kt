package com.zeusgd.AnimeFlick.util

import android.content.Context
import android.content.SharedPreferences

object ThemeManager {
    private const val PREF_NAME = "app_preferences"
    private const val KEY_DARK_MODE = "dark_mode"

    fun isDarkModeSet(context: Context): Boolean? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_DARK_MODE)) prefs.getBoolean(KEY_DARK_MODE, false) else null
    }

    fun setDarkMode(context: Context, isDark: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }
}
