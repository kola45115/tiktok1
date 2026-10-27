package com.tiktokmod

import android.telephony.TelephonyManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/** Подмена страны SIM/сети, которую TikTok отправляет на сервер как регион. */
object RegionHook {
    /** ISO -> MCC+MNC реального оператора */
    val REGIONS = linkedMapOf(
        "US" to "310260", "GB" to "23410", "DE" to "26201", "FR" to "20801",
        "IT" to "22201", "ES" to "21404", "PL" to "26001", "UA" to "25501",
        "KZ" to "40101", "TR" to "28601", "BR" to "72406", "MX" to "33402",
        "CA" to "302220", "AU" to "50501", "NZ" to "53001", "JP" to "44010",
        "KR" to "45005", "ID" to "51010", "SA" to "42001", "AE" to "42402",
    )

    fun install() {
        val tm = TelephonyManager::class.java
        for (n in listOf("getSimCountryIso", "getNetworkCountryIso")) {
            XposedBridge.hookAllMethods(tm, n, object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam) {
                    Prefs.region()?.let { p.result = it.lowercase() }
                }
            })
        }
        for (n in listOf("getSimOperator", "getNetworkOperator")) {
            XposedBridge.hookAllMethods(tm, n, object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam) {
                    Prefs.region()?.let { r -> REGIONS[r]?.let { p.result = it } }
                }
            })
        }
    }
}
