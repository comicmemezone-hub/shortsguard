package com.shortsguard.app

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BlockPreferences {
    private const val PREFS_NAME = "shorts_guard_prefs"

    private const val KEY_HIDE_SHORTS_BUTTON = "hide_shorts_button"
    private const val KEY_BLOCK_YOUTUBE = "block_youtube"
    private const val KEY_BLOCK_CHROME = "block_chrome"
    private const val KEY_BLOCK_WHATSAPP = "block_whatsapp"

    private const val KEY_BLOCKED_COUNT = "blocked_count"
    private const val KEY_LAST_DATE = "last_date"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isHideShortsButtonEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HIDE_SHORTS_BUTTON, true)
    }

    fun setHideShortsButtonEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HIDE_SHORTS_BUTTON, enabled).apply()
    }

    fun isYoutubeBlockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BLOCK_YOUTUBE, true)
    }

    fun setYoutubeBlockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BLOCK_YOUTUBE, enabled).apply()
    }

    fun isChromeBlockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BLOCK_CHROME, true)
    }

    fun setChromeBlockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BLOCK_CHROME, enabled).apply()
    }

    fun isWhatsappBlockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BLOCK_WHATSAPP, true)
    }

    fun setWhatsappBlockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BLOCK_WHATSAPP, enabled).apply()
    }

    fun getTodayBlockedCount(context: Context): Int {
        val prefs = getPrefs(context)
        val today = getTodayDateString()
        val savedDate = prefs.getString(KEY_LAST_DATE, "")

        return if (savedDate == today) {
            prefs.getInt(KEY_BLOCKED_COUNT, 0)
        } else {
            0
        }
    }

    fun incrementBlockedCount(context: Context): Int {
        val prefs = getPrefs(context)
        val today = getTodayDateString()
        val savedDate = prefs.getString(KEY_LAST_DATE, "")

        val currentCount = if (savedDate == today) {
            prefs.getInt(KEY_BLOCKED_COUNT, 0)
        } else {
            0
        }

        val newCount = currentCount + 1
        prefs.edit()
            .putString(KEY_LAST_DATE, today)
            .putInt(KEY_BLOCKED_COUNT, newCount)
            .apply()

        return newCount
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
