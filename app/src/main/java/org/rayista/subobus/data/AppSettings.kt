package org.rayista.subobus.data

import android.content.Context
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * 全局设置（保存在 SharedPreferences，重启后仍然有效）。
 */
data class AppSettings(
    /** 是否用手动限速覆盖线路 CSV 的站点限速 */
    val limitOverride: Boolean = false,
    /** 手动限速值（1-999），覆盖开启时所有站点都用它 */
    val manualLimit: Int = 60,
    /** 是否用手动灵敏度覆盖线路 CSV 的站前里程 */
    val sensitivityOverride: Boolean = false,
    /** 进站灵敏度（米），覆盖开启时所有站点都用它 */
    val arrivalSensitivity: Int = 50,
    /** 重合灵敏度（米） */
    val overlapSensitivity: Int = 5,
    /** 出站灵敏度（米） */
    val departureSensitivity: Int = 50,
    /** 勾选后，主页面双击当前站会用当前经纬度覆写线路 CSV */
    val doubleTapOverwrite: Boolean = false,
    /** 是否用下面的开关覆写 audiolist.csv 的“是否播报” */
    val ruleOverride: Boolean = false,
    val ruleStart: Boolean = true,
    val ruleEnd: Boolean = true,
    val ruleEndForecast: Boolean = true,
    val ruleArrival: Boolean = true,
    val ruleDeparture: Boolean = true,
    val ruleArrivalExternal: Boolean = true,
    val ruleDepartureExternal: Boolean = true,
    /** 实验性：外音走左声道、主播报走右声道 */
    val separateChannels: Boolean = false,
    /** 主题配色 id */
    val themeId: String = "dark_blue",
    /** 是否播报超速提醒 */
    val overspeedEnabled: Boolean = true,
    /** 超速提醒播报次数（默认 2 次） */
    val overspeedRepeat: Int = 2,
    /** 超速提醒间隔时间（秒，默认 5 秒） */
    val overspeedInterval: Int = 5,
    /** 开启后台运行：显示常驻通知（含「报站 / 停止」）并允许后台定位报站 */
    val backgroundRun: Boolean = true,
    /** 前台是否保持屏幕常亮 */
    val keepScreenOn: Boolean = true,
    /** 按键是否震动 */
    val hapticEnabled: Boolean = true,
    /** 响度均衡：把小声的报站录音自动提上来 */
    val loudnessNormalization: Boolean = false,
    /** 界面语言 id：zh_cn / zh_tw / en */
    val language: String = "zh_cn"
)

class SettingsStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("subo_settings", Context.MODE_PRIVATE)

    /**
     * 用户还没在设置里选过语言时，跟随系统语言：
     * 繁体中文（台湾/香港/澳门或 Hant 脚本）→ zh_tw，其它中文 → zh_cn，其余 → en。
     */
    private fun systemLanguage(): String {
        val locale = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                LocaleList.getDefault()[0]
            } else {
                Locale.getDefault()
            }
        } catch (_: Exception) {
            Locale.getDefault()
        }
        if (locale.language.lowercase() != "zh") return "en"
        val script = try {
            locale.script
        } catch (_: Exception) {
            ""
        }
        val country = locale.country.uppercase()
        return if (script == "Hant" || country in setOf("TW", "HK", "MO")) "zh_tw" else "zh_cn"
    }

    fun load(): AppSettings = AppSettings(
        limitOverride = prefs.getBoolean("limit_override", false),
        manualLimit = prefs.getInt("manual_limit", 60).coerceIn(1, 999),
        sensitivityOverride = prefs.getBoolean("sensitivity_override", false),
        arrivalSensitivity = prefs.getInt("arrival_sensitivity", 50).coerceIn(1, 999),
        overlapSensitivity = prefs.getInt("overlap_sensitivity", 5).coerceIn(1, 999),
        departureSensitivity = prefs.getInt("departure_sensitivity", 50).coerceIn(1, 999),
        doubleTapOverwrite = prefs.getBoolean("double_tap_overwrite", false),
        ruleOverride = prefs.getBoolean("rule_override", false),
        ruleStart = prefs.getBoolean("rule_start", true),
        ruleEnd = prefs.getBoolean("rule_end", true),
        ruleEndForecast = prefs.getBoolean("rule_end_forecast", true),
        ruleArrival = prefs.getBoolean("rule_arrival", true),
        ruleDeparture = prefs.getBoolean("rule_departure", true),
        ruleArrivalExternal = prefs.getBoolean("rule_arrival_external", true),
        ruleDepartureExternal = prefs.getBoolean("rule_departure_external", true),
        separateChannels = prefs.getBoolean("separate_channels", false),
        themeId = prefs.getString("theme_id", "dark_blue") ?: "dark_blue",
        overspeedEnabled = prefs.getBoolean("overspeed_enabled", true),
        overspeedRepeat = prefs.getInt("overspeed_repeat", 2).coerceIn(1, 20),
        overspeedInterval = prefs.getInt("overspeed_interval", 5).coerceIn(1, 999),
        backgroundRun = prefs.getBoolean("background_run", true),
        keepScreenOn = prefs.getBoolean("keep_screen_on", true),
        hapticEnabled = prefs.getBoolean("haptic_enabled", true),
        loudnessNormalization = prefs.getBoolean("loudness_normalization", false),
        language = prefs.getString("language", null) ?: systemLanguage()
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putBoolean("limit_override", settings.limitOverride)
            .putInt("manual_limit", settings.manualLimit)
            .putBoolean("sensitivity_override", settings.sensitivityOverride)
            .putInt("arrival_sensitivity", settings.arrivalSensitivity)
            .putInt("overlap_sensitivity", settings.overlapSensitivity)
            .putInt("departure_sensitivity", settings.departureSensitivity)
            .putBoolean("double_tap_overwrite", settings.doubleTapOverwrite)
            .putBoolean("rule_override", settings.ruleOverride)
            .putBoolean("rule_start", settings.ruleStart)
            .putBoolean("rule_end", settings.ruleEnd)
            .putBoolean("rule_end_forecast", settings.ruleEndForecast)
            .putBoolean("rule_arrival", settings.ruleArrival)
            .putBoolean("rule_departure", settings.ruleDeparture)
            .putBoolean("rule_arrival_external", settings.ruleArrivalExternal)
            .putBoolean("rule_departure_external", settings.ruleDepartureExternal)
            .putBoolean("separate_channels", settings.separateChannels)
            .putString("theme_id", settings.themeId)
            .putBoolean("overspeed_enabled", settings.overspeedEnabled)
            .putInt("overspeed_repeat", settings.overspeedRepeat)
            .putInt("overspeed_interval", settings.overspeedInterval)
            .putBoolean("background_run", settings.backgroundRun)
            .putBoolean("keep_screen_on", settings.keepScreenOn)
            .putBoolean("haptic_enabled", settings.hapticEnabled)
            .putBoolean("loudness_normalization", settings.loudnessNormalization)
            .putString("language", settings.language)
            .apply()
    }
}
