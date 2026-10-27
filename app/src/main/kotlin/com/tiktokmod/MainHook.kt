package com.tiktokmod

import android.app.Application
import android.content.Context
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName !in TARGETS) return
        // Ловим Context как можно раньше, чтобы читать настройки
        XposedHelpers.findAndHookMethod(
            Application::class.java, "attach", Context::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam) {
                    if (Prefs.appContext == null) Prefs.appContext = p.args[0] as Context
                }
            })
        safe("region") { RegionHook.install() }
        safe("feed") { FeedHook.install(lpparam.classLoader) }
        safe("download") { DownloadHook.install(lpparam.classLoader) }
        safe("theme") { ThemeHook.install() }
        safe("settings") { SettingsHook.install() }
        XposedBridge.log("[TTMod] loaded in ${lpparam.packageName}")
    }

    private inline fun safe(name: String, block: () -> Unit) {
        try { block() } catch (t: Throwable) { XposedBridge.log("[TTMod] $name failed: $t") }
    }

    companion object {
        val TARGETS = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
    }
}
