package com.tiktokmod

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * Скачивание без водяного знака:
 *  Video.getDownloadAddr() -> подменяем на Video.getDownloadNoWatermarkAddr(), если он есть
 *  Aweme.isPreventDownload() -> false (разрешить кнопку «Сохранить» там, где автор запретил)
 * Ограничение: если сервер не прислал downloadNoWatermarkAddr, подменять нечем.
 */
object DownloadHook {
    fun install(cl: ClassLoader) {
        val video = XposedHelpers.findClass("com.ss.android.ugc.aweme.feed.model.Video", cl)
        val aweme = XposedHelpers.findClass("com.ss.android.ugc.aweme.feed.model.Aweme", cl)

        for (n in listOf("getDownloadAddr", "getNewDownloadAddr", "getUIAlikeDownloadAddr")) {
            XposedBridge.hookAllMethods(video, n, object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam) {
                    if (!Prefs.flag("no_wm")) return
                    val nw = try { XposedHelpers.callMethod(p.thisObject, "getDownloadNoWatermarkAddr") } catch (_: Throwable) { null }
                    if (nw != null) p.result = nw
                }
            })
        }
        XposedBridge.hookAllMethods(aweme, "isPreventDownload", object : XC_MethodHook() {
            override fun afterHookedMethod(p: MethodHookParam) {
                if (Prefs.flag("no_wm")) p.result = false
            }
        })
    }
}
