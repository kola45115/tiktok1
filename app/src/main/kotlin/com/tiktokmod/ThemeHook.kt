package com.tiktokmod

import android.content.res.ColorStateList
import android.content.res.Resources
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/**
 * Перекраска по ЗНАЧЕНИЮ цвета. Имена ресурсов в TikTok обфусцированы (a, ag, bl, c7…),
 * поэтому по имени красить нельзя. Зато фирменный розово-красный #FE2C55 встречается
 * в ~27 ресурсах (кнопки, лайк, подписка, акценты) — заменяем его на выбранный цвет,
 * сохраняя прозрачность (альфа-канал) оригинала.
 * Резервный режим: regex по имени из окна настроек (работает, только если имя не обфусцировано).
 */
object ThemeHook {
    /** RGB-значения, которые считаем «фирменным акцентом». */
    private val BRAND = intArrayOf(0xFE2C55, 0xFF3B5C)

    private fun isBrand(c: Int): Boolean {
        val rgb = c and 0xFFFFFF
        return BRAND.any { it == rgb }
    }

    private fun recolor(orig: Int, accent: Int): Int =
        (orig and 0xFF000000.toInt()) or (accent and 0xFFFFFF)

    private fun nameMatches(res: Resources, id: Int): Boolean =
        Prefs.colorCache.getOrPut(id) {
            val name = runCatching { res.getResourceEntryName(id) }.getOrNull() ?: return@getOrPut false
            Prefs.accentRegex().matches(name)
        }

    fun install() {
        val r = Resources::class.java
        XposedBridge.hookAllMethods(r, "getColor", object : XC_MethodHook() {
            override fun afterHookedMethod(p: MethodHookParam) {
                val a = Prefs.accent()
                if (a == 0) return
                val id = p.args.firstOrNull() as? Int ?: return
                val orig = p.result as? Int ?: return
                if (Prefs.debug()) android.util.Log.d("TTMod", "color id=0x${Integer.toHexString(id)} v=#${Integer.toHexString(orig)}")
                when {
                    isBrand(orig) -> p.result = recolor(orig, a)
                    nameMatches(p.thisObject as Resources, id) -> p.result = a
                }
            }
        })
        XposedBridge.hookAllMethods(r, "getColorStateList", object : XC_MethodHook() {
            override fun afterHookedMethod(p: MethodHookParam) {
                val a = Prefs.accent()
                if (a == 0) return
                val id = p.args.firstOrNull() as? Int ?: return
                val csl = p.result as? ColorStateList ?: return
                if (isBrand(csl.defaultColor)) {
                    p.result = ColorStateList.valueOf(recolor(csl.defaultColor, a))
                } else if (nameMatches(p.thisObject as Resources, id)) {
                    p.result = ColorStateList.valueOf(a)
                }
            }
        })
    }
}
