package com.tiktokmod

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

/** Настройки хранятся в SharedPreferences самого TikTok-процесса (root не нужен). */
object Prefs {
    @Volatile var appContext: Context? = null
    /** кэш "resId -> подходит ли под regex акцента"; сбрасывается при сохранении */
    val colorCache = ConcurrentHashMap<Int, Boolean>()

    const val DEFAULT_REGEX = "(?i).*(pink|brand|accent|like).*"

    private fun sp(): SharedPreferences? =
        appContext?.getSharedPreferences("ttmod_prefs", Context.MODE_PRIVATE)

    /** ISO-код региона (например "US") или null, если подмена выключена */
    fun region(): String? = sp()?.getString("region", null)?.takeIf { it.isNotBlank() }
    fun accent(): Int = sp()?.getInt("accent", 0) ?: 0
    fun accentRegex(): Regex =
        runCatching { Regex(sp()?.getString("accent_regex", DEFAULT_REGEX) ?: DEFAULT_REGEX) }
            .getOrElse { Regex(DEFAULT_REGEX) }
    fun flag(key: String, def: Boolean = true): Boolean = sp()?.getBoolean(key, def) ?: def
    fun setFlag(key: String, v: Boolean) { sp()?.edit()?.putBoolean(key, v)?.apply() }
    fun debug(): Boolean = sp()?.getBoolean("debug", false) ?: false

    fun save(region: String?, accent: Int, regex: String, debug: Boolean) {
        sp()?.edit()
            ?.putString("region", region ?: "")
            ?.putInt("accent", accent)
            ?.putString("accent_regex", regex)
            ?.putBoolean("debug", debug)
            ?.apply()
        colorCache.clear()
    }

    fun rawRegex(): String = sp()?.getString("accent_regex", DEFAULT_REGEX) ?: DEFAULT_REGEX
}
