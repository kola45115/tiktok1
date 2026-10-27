package com.tiktokmod

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.*
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/** Настройки открываются касанием ТРЕМЯ пальцами по любому экрану TikTok. */
object SettingsHook {
    @Volatile private var showing = false

    fun install() {
        XposedBridge.hookAllMethods(Activity::class.java, "dispatchTouchEvent", object : XC_MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                val ev = p.args[0] as? MotionEvent ?: return
                if (ev.actionMasked == MotionEvent.ACTION_POINTER_DOWN && ev.pointerCount == 3 && !showing) {
                    showing = true
                    val act = p.thisObject as Activity
                    act.runOnUiThread { runCatching { show(act) }.onFailure { showing = false } }
                }
            }
        })
    }

    private fun show(act: Activity) {
        val pad = (16 * act.resources.displayMetrics.density).toInt()
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }

        fun label(t: String) = TextView(act).apply { text = t; setPadding(0, pad / 2, 0, 0) }.also { col.addView(it) }

        label("Регион (подмена SIM/сети)")
        val codes = listOf("— выкл —") + RegionHook.REGIONS.keys
        val spin = Spinner(act).apply {
            adapter = ArrayAdapter(act, android.R.layout.simple_spinner_dropdown_item, codes)
            setSelection(codes.indexOf(Prefs.region()).coerceAtLeast(0))
        }.also { col.addView(it) }

        label("Акцентный цвет (HEX, пусто = выкл)")
        val hex = EditText(act).apply {
            setText(Prefs.accent().takeIf { it != 0 }?.let { String.format("#%06X", it and 0xFFFFFF) } ?: "")
            hint = "#FF2D55"
        }.also { col.addView(it) }

        label("Regex имён цветов (резерв; имена в TikTok обфусцированы)")
        val rx = EditText(act).apply { setText(Prefs.rawRegex()) }.also { col.addView(it) }

        val dbg = CheckBox(act).apply { text = "Debug: писать имена цветов в logcat"; isChecked = Prefs.debug() }
        col.addView(dbg)

        val flags = listOf(
            "no_ads" to "Убрать рекламу из ленты",
            "no_live" to "Скрыть LIVE в ленте",
            "no_ai" to "Скрыть AI-видео",
            "no_wm" to "Скачивать без водяного знака",
        ).map { (k, t) -> k to CheckBox(act).apply { text = t; isChecked = Prefs.flag(k) }.also { col.addView(it) } }

        AlertDialog.Builder(act)
            .setTitle("TikTok Mod")
            .setView(ScrollView(act).apply { addView(col, ViewGroup.LayoutParams(-1, -2)) })
            .setPositiveButton("Сохранить") { _, _ ->
                val region = codes[spin.selectedItemPosition].takeIf { spin.selectedItemPosition > 0 }
                val color = runCatching { Color.parseColor(hex.text.toString().trim()) }.getOrDefault(0)
                flags.forEach { (k, cb) -> Prefs.setFlag(k, cb.isChecked) }
                Prefs.save(region, color, rx.text.toString().ifBlank { Prefs.DEFAULT_REGEX }, dbg.isChecked)
                Toast.makeText(act, "Сохранено. Перезапустите TikTok", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Отмена", null)
            .setOnDismissListener { showing = false }
            .show()
    }
}
