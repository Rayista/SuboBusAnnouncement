package org.rayista.subobus.data

import android.content.Context

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
    /** 前台是否保持屏幕常亮 */
    val keepScreenOn: Boolean = true,
    /** 按键是否震动 */
    val hapticEnabled: Boolean = true,
    /** 响度均衡：把小声的报站录音自动提上来 */
    val loudnessNormalization: Boolean = false,
    /** 界面语言 id：zh_cn / zh_tw / en */
    val language: String = "zh_cn"
)

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("subo_settings", Context.MODE_PRIVATE)

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
        keepScreenOn = prefs.getBoolean("keep_screen_on", true),
        hapticEnabled = prefs.getBoolean("haptic_enabled", true),
        loudnessNormalization = prefs.getBoolean("loudness_normalization", false),
        language = prefs.getString("language", "zh_cn") ?: "zh_cn"
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
            .putBoolean("keep_screen_on", settings.keepScreenOn)
            .putBoolean("haptic_enabled", settings.hapticEnabled)
            .putBoolean("loudness_normalization", settings.loudnessNormalization)
            .putString("language", settings.language)
            .apply()
    }
}
