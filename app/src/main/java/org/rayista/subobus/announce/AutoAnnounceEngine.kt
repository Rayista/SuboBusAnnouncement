package org.rayista.subobus.announce

import org.rayista.subobus.data.Stop

/** 默认进站半径（米）：CSV 的“站前里程”为 0 时使用。 */
const val AUTO_DEFAULT_ARRIVAL_RADIUS_METERS = 50

/** “重合”半径（米）：车辆进入这个范围才认为已经到站。 */
const val AUTO_OVERLAP_RADIUS_METERS = 5

/** 出站半径（米）：从重合状态离开到这个距离触发出站。 */
const val AUTO_DEPARTURE_RADIUS_METERS = 50

/** 自动播报事件的类型。 */
enum class AutoAnnounceType(val label: String) {
    ARRIVAL("进站"),
    DEPARTURE("出站")
}

/**
 * 一次自动触发：哪个站点（在 stops 列表中的下标）、进站还是出站。
 */
data class AutoAnnounceEvent(
    val stopIndex: Int,
    val type: AutoAnnounceType,
    val stopName: String
)

/**
 * 基于 GPS 的自动进出站判定状态机（纯 Kotlin，不依赖 Activity/Context）。
 *
 * 使用方式：
 * 1. 选线、切换上下行、导入数据后调用 [reset]，清空历史状态；
 * 2. 每次拿到有效定位后调用 [onLocation]，返回值是本次新触发的事件。
 *
 * 关键规则：
 * - 进站不要求按顺序：只要车辆进入某站“站前里程”范围就触发，中间没经过的站自动跳过；
 * - 同一站进站、出站各只触发一次（除非 [reset]）；
 * - 出站必须先进入过 5 米“重合”范围，再离开到 50 米才触发。
 */
class AutoAnnounceEngine(
    /** CSV 的“度分”经纬度转十进制度，复用 ViewModel 已有的实现。 */
    private val toDecimal: (Double) -> Double,
    /** Haversine 距离计算，复用 ViewModel 已有的实现。返回米。 */
    private val distanceMeters: (lat1: Double, lon1: Double, lat2: Double, lon2: Double) -> Int,
    /** “站前里程”为 0 时的兜底进站半径。 */
    private val defaultArrivalRadius: Int = AUTO_DEFAULT_ARRIVAL_RADIUS_METERS,
    /** 到站“重合”判定半径。 */
    private var overlapRadius: Int = AUTO_OVERLAP_RADIUS_METERS,
    /** 离站触发半径。 */
    private var departureRadius: Int = AUTO_DEPARTURE_RADIUS_METERS
) {

    /** 手动覆盖的进站半径；null 表示按线路 CSV 的站前里程。 */
    private var arrivalOverride: Int? = null

    /**
     * 应用“进出站灵敏度”设置：
     * - arrival 为 null 时按 CSV 的站前里程；否则所有站点统一用它。
     * 不会清空已触发记录，只更新半径。
     */
    fun setSensitivity(arrival: Int?, overlap: Int, departure: Int) {
        arrivalOverride = arrival
        overlapRadius = overlap
        departureRadius = departure
        points = points.map { point ->
            point.copy(arrivalRadius = arrival ?: csvRadiusOf(point.index) ?: defaultArrivalRadius)
        }
    }

    private fun csvRadiusOf(index: Int): Int? {
        val stop = originalStops.getOrNull(index) ?: return null
        return if (stop.distanceBefore > 0) stop.distanceBefore else null
    }

    private var originalStops: List<Stop> = emptyList()

    /** 内部缓存的站点坐标，避免每次定位都重新解析经纬度。 */
    private data class StopPoint(
        val index: Int,
        val name: String,
        val lat: Double,
        val lon: Double,
        val arrivalRadius: Int
    )

    private var points: List<StopPoint> = emptyList()
    private val arrivalTriggered = mutableSetOf<Int>()
    private val departureTriggered = mutableSetOf<Int>()
    private val overlappingStops = mutableSetOf<Int>()

    /**
     * 重新选线/切向/导入数据后调用：替换站点并清空所有触发记录。
     * 没有经纬度（0/0）的站点不参与自动判定，避免误报。
     */
    fun reset(stops: List<Stop>) {
        originalStops = stops
        points = stops.mapIndexedNotNull { index, stop ->
            if (stop.lat == 0.0 && stop.lon == 0.0) return@mapIndexedNotNull null
            val csvRadius = if (stop.distanceBefore > 0) stop.distanceBefore else defaultArrivalRadius
            val radius = arrivalOverride ?: csvRadius
            StopPoint(
                index = index,
                name = stop.name,
                lat = toDecimal(stop.lat),
                lon = toDecimal(stop.lon),
                arrivalRadius = radius
            )
        }
        arrivalTriggered.clear()
        departureTriggered.clear()
        overlappingStops.clear()
    }

    /**
     * 输入一次 GPS 位置（十进制度），返回本次新触发的事件（可能为空列表）。
     * 无效坐标（0/0）直接忽略，防止无定位时误触发。
     */
    fun onLocation(lat: Double, lon: Double): List<AutoAnnounceEvent> {
        if (lat == 0.0 && lon == 0.0) return emptyList()
        val events = mutableListOf<AutoAnnounceEvent>()

        // 1. 出站：之前处于“重合中”的站点，距离增大到阈值后触发一次
        val stillOverlapping = mutableSetOf<Int>()
        for (index in overlappingStops) {
            val point = points.firstOrNull { it.index == index } ?: continue
            val distance = distanceMeters(lat, lon, point.lat, point.lon)
            if (distance >= departureRadius) {
                if (departureTriggered.add(index)) {
                    events.add(AutoAnnounceEvent(index, AutoAnnounceType.DEPARTURE, point.name))
                }
            } else {
                stillOverlapping.add(index)
            }
        }
        overlappingStops.clear()
        overlappingStops.addAll(stillOverlapping)

        // 2. 进站 + 记录“重合”：遍历全部站点，所以天然支持跳站
        for (point in points) {
            val distance = distanceMeters(lat, lon, point.lat, point.lon)
            if (distance <= overlapRadius) {
                overlappingStops.add(point.index)
            }
            if (point.index !in arrivalTriggered && distance <= point.arrivalRadius) {
                arrivalTriggered.add(point.index)
                events.add(AutoAnnounceEvent(point.index, AutoAnnounceType.ARRIVAL, point.name))
            }
        }

        // 事件顺序：先播上一站的出站，再播本站的进站
        return events
    }
}
