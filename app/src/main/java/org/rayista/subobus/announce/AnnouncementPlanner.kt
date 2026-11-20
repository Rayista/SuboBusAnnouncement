package org.rayista.subobus.announce

import org.rayista.subobus.data.AudioRule
import org.rayista.subobus.data.DataStore
import org.rayista.subobus.data.Stop
import java.io.File

/** 播放声道：分离声道实验功能用。 */
enum class PlayChannel { BOTH, LEFT, RIGHT }

/** 一个待播放的音频，带自己的声道。 */
data class AudioCue(
    val file: File,
    val channel: PlayChannel = PlayChannel.BOTH
)

/**
 * 一次“项目”的播报：例如一次进站播报，里面可能有好几个 mp3 要按顺序播。
 */
data class AnnouncementStep(
    val type: String,
    val cues: List<AudioCue>,
    val detail: String,
    val stopIndex: Int
) {
    /** 兼容只关心文件列表的调用。 */
    val files: List<File> get() = cues.map { it.file }
}

/**
 * 把 audiolist.csv 的规则翻译成具体要播放的 mp3 列表。
 * 找不到音频就直接跳过，不抛异常。
 */
class AnnouncementPlanner(private val data: DataStore) {

    fun build(
        route: String,
        stops: List<Stop>,
        rules: List<AudioRule>,
        separateChannels: Boolean = false
    ): List<AnnouncementStep> {
        if (stops.isEmpty()) return emptyList()

        val ruleMap = rules.associateBy { it.type }
        val steps = mutableListOf<AnnouncementStep>()

        fun slotsOf(type: String, stopIndex: Int): List<String> {
            val rule = ruleMap[type] ?: return emptyList()
            if (!rule.enabled) return emptyList() // 标为“否”的规则整条跳过
            val result = mutableListOf<String>()
            for (slot in rule.slots) {
                if (slot == "途径站") {
                    // 只取线路 CSV 中“是否大站”标为大站的站点语音；没有就忽略
                    stops.filter { it.big == "大站" }.forEach { result.add(it.voice) }
                } else {
                    resolve(slot, route, stops, stopIndex)?.let { result.add(it) }
                }
            }
            return result
        }

        fun cuesOf(type: String, stopIndex: Int): List<AudioCue> =
            slotsOf(type, stopIndex)
                .mapNotNull { data.audioFile(it) }
                .map { AudioCue(it, channelFor(type, separateChannels)) }

        fun finish(type: String, cues: List<AudioCue>, stopIndex: Int) {
            // 没有任何可播音频就不生成这一步，避免出现空的播报
            if (cues.isEmpty()) return
            val detail = cues.joinToString(" → ") { it.file.nameWithoutExtension }
            steps.add(AnnouncementStep(type, cues, detail, stopIndex))
        }

        fun add(type: String, stopIndex: Int) {
            finish(type, cuesOf(type, stopIndex), stopIndex)
        }

        /** 进站播报 + 进站外音 是同一步：先播进站播报，再播进站外音。 */
        fun addArrival(stopIndex: Int) {
            val cues = mutableListOf<AudioCue>()
            cues.addAll(cuesOf("进站播报", stopIndex))
            if (stops[stopIndex].external == "是") {
                cues.addAll(cuesOf("进站外音", stopIndex))
            }
            finish("进站播报", cues, stopIndex)
        }

        add("起点播报", 0)

        for (i in 1 until stops.size) {
            val previous = i - 1
            // 起点播报后直接进第二站的进站播报，不再插入出站播报/出站外音
            if (i > 1) {
                add(if (i == stops.size - 1) "终点预报" else "出站播报", previous)
                if (stops[previous].external == "是") add("出站外音", previous)
            }
            // 终点站不播进站外音和进站播报，直接交给终点播报
            if (i == stops.size - 1) continue
            addArrival(i)
        }

        add("终点播报", stops.size - 1)
        return steps
    }

    /** 分离声道时：外音全部走左声道，主播报全部走右声道。 */
    private fun channelFor(type: String, separate: Boolean): PlayChannel {
        if (!separate) return PlayChannel.BOTH
        return when (type) {
            "进站外音", "出站外音" -> PlayChannel.LEFT
            "起点播报", "终点播报", "终点预报", "进站播报", "出站播报" -> PlayChannel.RIGHT
            else -> PlayChannel.BOTH
        }
    }

    private fun resolve(token: String, route: String, stops: List<Stop>, index: Int): String? {
        if (token.isBlank()) return null
        val stop = stops.getOrNull(index)

        val value = when (token) {
            "线路名称" -> route
            "起点站" -> stops.firstOrNull()?.voice
            "终点站" -> stops.lastOrNull()?.voice
            "本站" -> stop?.voice
            "下一站" -> stops.getOrNull(index + 1)?.voice
            "进站广告" -> pick(stop?.adIn, token)
            "出站广告" -> pick(stop?.adOut, token)
            "进站提示" -> pick(stop?.tipIn, token)
            "出站提示" -> pick(stop?.tipOut, token)
            "进站扩展" -> pick(stop?.extIn, token)
            "出站扩展" -> pick(stop?.extOut, token)
            else -> token
        }
        return value?.takeIf { it.isNotBlank() }
    }

    private fun pick(custom: String?, fallback: String): String =
        if (custom.isNullOrBlank()) fallback else custom
}
