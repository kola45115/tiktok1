package com.tiktokmod

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * Фильтр ленты. Имена классов проверены по TikTok 47.x (dex пользователя):
 *  - FeedItemList.getItems()/getAwemeList()  : список Aweme, приходящий с сервера
 *  - Aweme.isAd(), Aweme.getAwemeRawAd()      : реклама
 *  - Aweme.getRoomFeedCellStruct()            : LIVE-карточка в ленте
 *  - Aweme.getAigcInfo().getAIGCLabelType()   : метка AI-контента (0 = нет метки)
 */
object FeedHook {
    private const val AWEME = "com.ss.android.ugc.aweme.feed.model.Aweme"
    private const val FEED = "com.ss.android.ugc.aweme.feed.model.FeedItemList"

    private fun call(o: Any?, name: String): Any? =
        if (o == null) null else try { XposedHelpers.callMethod(o, name) } catch (_: Throwable) { null }

    fun shouldDrop(a: Any?): Boolean {
        if (a == null) return false
        if (Prefs.flag("no_ads")) {
            if (call(a, "isAd") == true || call(a, "getAwemeRawAd") != null) return true
        }
        if (Prefs.flag("no_live")) {
            if (call(a, "getRoomFeedCellStruct") != null) return true
        }
        if (Prefs.flag("no_ai")) {
            val t = call(call(a, "getAigcInfo"), "getAIGCLabelType") as? Int ?: 0
            if (t > 0) return true
        }
        return false
    }

    fun install(cl: ClassLoader) {
        XposedHelpers.findClass(AWEME, cl) // упадёт сразу, если класса нет в этой версии
        val hook = object : XC_MethodHook() {
            override fun afterHookedMethod(p: MethodHookParam) {
                @Suppress("UNCHECKED_CAST")
                val list = p.result as? MutableList<Any?> ?: return
                if (list.none { shouldDrop(it) }) return
                try {
                    list.removeAll { shouldDrop(it) }   // правим список на месте
                } catch (_: UnsupportedOperationException) {
                    p.result = list.filterNot { shouldDrop(it) }.toMutableList()
                }
            }
        }
        for (n in listOf("getItems", "getAwemeList")) {
            try {
                XposedBridge.hookAllMethods(XposedHelpers.findClass(FEED, cl), n, hook)
            } catch (t: Throwable) { XposedBridge.log("[TTMod] feed hook $n: $t") }
        }
    }
}
