package com.shortsguard.app

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object BlockPreferences {
    private const val PREFS_NAME = "shorts_guard_prefs"

    private const val KEY_HIDE_SHORTS_BUTTON = "hide_shorts_button"
    private const val KEY_BLOCK_YOUTUBE = "block_youtube"
    private const val KEY_BLOCK_CHROME = "block_chrome"
    private const val KEY_BLOCK_WHATSAPP = "block_whatsapp"

    private const val KEY_LOCKDOWN_DEADLINE_MS = "lockdown_deadline_ms"

    private const val KEY_BLOCKED_COUNT = "blocked_count"
    private const val KEY_LAST_DATE = "last_date"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Strict Lockdown Mode ---

    fun startLockdown(context: Context, durationMillis: Long) {
        val targetDeadline = System.currentTimeMillis() + durationMillis
        getPrefs(context).edit()
            .putLong(KEY_LOCKDOWN_DEADLINE_MS, targetDeadline)
            .putBoolean(KEY_HIDE_SHORTS_BUTTON, true)
            .putBoolean(KEY_BLOCK_YOUTUBE, true)
            .putBoolean(KEY_BLOCK_CHROME, true)
            .putBoolean(KEY_BLOCK_WHATSAPP, true)
            .apply()
    }

    fun setCustomDeadline(context: Context, targetDeadlineMs: Long) {
        getPrefs(context).edit()
            .putLong(KEY_LOCKDOWN_DEADLINE_MS, targetDeadlineMs)
            .putBoolean(KEY_HIDE_SHORTS_BUTTON, true)
            .putBoolean(KEY_BLOCK_YOUTUBE, true)
            .putBoolean(KEY_BLOCK_CHROME, true)
            .putBoolean(KEY_BLOCK_WHATSAPP, true)
            .apply()
    }

    fun getLockdownDeadlineMs(context: Context): Long {
        return getPrefs(context).getLong(KEY_LOCKDOWN_DEADLINE_MS, 0L)
    }

    fun isLockdownActive(context: Context): Boolean {
        return System.currentTimeMillis() < getLockdownDeadlineMs(context)
    }

    fun getRemainingLockdownMillis(context: Context): Long {
        val diff = getLockdownDeadlineMs(context) - System.currentTimeMillis()
        return if (diff > 0) diff else 0L
    }

    fun formatRemainingTime(millis: Long): String {
        val totalSec = millis / 1000
        val hours = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val secs = totalSec % 60
        return String.format(Locale.getDefault(), "%02dh : %02dm : %02ds", hours, mins, secs)
    }

    fun formatDeadlineDateTime(timestampMs: Long): String {
        if (timestampMs <= 0) return ""
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val nowCal = Calendar.getInstance()

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeStr = timeFormat.format(Date(timestampMs))

        return if (targetCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR) &&
            targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
        ) {
            "Today at $timeStr"
        } else {
            val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            dateFormat.format(Date(timestampMs))
        }
    }

    // --- Block Settings (Enforced during Lockdown) ---

    fun isHideShortsButtonEnabled(context: Context): Boolean {
        if (isLockdownActive(context)) return true
        return getPrefs(context).getBoolean(KEY_HIDE_SHORTS_BUTTON, true)
    }

    fun setHideShortsButtonEnabled(context: Context, enabled: Boolean) {
        if (isLockdownActive(context)) return // Locked during deadline
        getPrefs(context).edit().putBoolean(KEY_HIDE_SHORTS_BUTTON, enabled).apply()
    }

    fun isYoutubeBlockEnabled(context: Context): Boolean {
        if (isLockdownActive(context)) return true
        return getPrefs(context).getBoolean(KEY_BLOCK_YOUTUBE, true)
    }

    fun setYoutubeBlockEnabled(context: Context, enabled: Boolean) {
        if (isLockdownActive(context)) return // Locked during deadline
        getPrefs(context).edit().putBoolean(KEY_BLOCK_YOUTUBE, enabled).apply()
    }

    fun isChromeBlockEnabled(context: Context): Boolean {
        if (isLockdownActive(context)) return true
        return getPrefs(context).getBoolean(KEY_BLOCK_CHROME, true)
    }

    fun setChromeBlockEnabled(context: Context, enabled: Boolean) {
        if (isLockdownActive(context)) return // Locked during deadline
        getPrefs(context).edit().putBoolean(KEY_BLOCK_CHROME, enabled).apply()
    }

    fun isWhatsappBlockEnabled(context: Context): Boolean {
        if (isLockdownActive(context)) return true
        return getPrefs(context).getBoolean(KEY_BLOCK_WHATSAPP, true)
    }

    fun setWhatsappBlockEnabled(context: Context, enabled: Boolean) {
        if (isLockdownActive(context)) return // Locked during deadline
        getPrefs(context).edit().putBoolean(KEY_BLOCK_WHATSAPP, enabled).apply()
    }

    // --- Statistics ---

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
