package org.rayista.subobus.data

/**
 * 一个站点的信息，对应线路 CSV 的一行。
 */
data class Stop(
    val id: String,
    val voice: String,      // 报站语音，例如“科技东园”，对应 mp3/科技东园.mp3
    val name: String,       // 站名，例如“科技新城东园”
    val lon: Double,
    val lat: Double,
    val angle: Double,
    val adIn: String,
    val adOut: String,
    val tipIn: String,
    val tipOut: String,
    val extIn: String,
    val extOut: String,
    val limit: Int,
    val distanceBefore: Int,
    val big: String,
    val external: String
)

/**
 * audiolist.csv 中的一行播报规则。
 */
data class AudioRule(
    val type: String,
    val enabled: Boolean,
    val slots: List<String>
)
