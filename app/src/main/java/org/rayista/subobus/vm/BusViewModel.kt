package org.rayista.subobus.vm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.rayista.subobus.announce.AnnouncementPlanner
import org.rayista.subobus.announce.AnnouncementStep
import org.rayista.subobus.announce.AudioCue
import org.rayista.subobus.AnnouncerService
import org.rayista.subobus.announce.AudioPlayer
import org.rayista.subobus.announce.AutoAnnounceEngine
import org.rayista.subobus.announce.AutoAnnounceEvent
import org.rayista.subobus.announce.AutoAnnounceType
import org.rayista.subobus.announce.AUTO_DEPARTURE_RADIUS_METERS
import org.rayista.subobus.announce.AUTO_OVERLAP_RADIUS_METERS
import org.rayista.subobus.data.AppSettings
import org.rayista.subobus.data.AudioRule
import org.rayista.subobus.data.SettingsStore
import org.rayista.subobus.data.ZipScope
import org.rayista.subobus.data.DataStore
import org.rayista.subobus.data.Importer
import org.rayista.subobus.data.Stop
import org.rayista.subobus.i18n.AppStrings
import org.rayista.subobus.i18n.stringsFor
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/** 自动播报时，定位精度差于这个值就不参与判定，避免误触发。 */
private const val AUTO_MAX_ACCURACY_METERS = 100f

/** 自动事件对应的实际播报步骤（含 steps 下标，方便同步手动游标）。 */
private data class AutoPlayItem(
    val event: AutoAnnounceEvent,
    val stepIndex: Int,
    val step: AnnouncementStep
)

data class UiState(
    val ready: Boolean = false,
    val routes: List<String> = emptyList(),
    val route: String = "",
    val up: Boolean = true,
    val routeMode: String = "上行",
    val canSwitch: Boolean = false,
    val currentName: String = "",
    val nextName: String = "—",
    val startName: String = "",
    val endName: String = "",
    val limit: Int = 0,
    val speed: Int = 0,
    val gps: String = "无",
    val currentLat: Double = 0.0,
    val currentLon: Double = 0.0,
    val nextLat: Double = 0.0,
    val nextLon: Double = 0.0,
    val distanceToNext: Int = 0,
    val feedback: String = "设备就绪",
    val key: String = "—",
    val playing: Boolean = false,
    // 全局设置
    val limitOverride: Boolean = false,
    val manualLimit: Int = 60,
    val sensitivityOverride: Boolean = false,
    val arrivalSensitivity: Int = 50,
    val overlapSensitivity: Int = 5,
    val departureSensitivity: Int = 50,
    val doubleTapOverwrite: Boolean = false,
    val ruleOverride: Boolean = false,
    val ruleStart: Boolean = true,
    val ruleEnd: Boolean = true,
    val ruleEndForecast: Boolean = true,
    val ruleArrival: Boolean = true,
    val ruleDeparture: Boolean = true,
    val ruleArrivalExternal: Boolean = true,
    val ruleDepartureExternal: Boolean = true,
    val separateChannels: Boolean = false,
    val themeId: String = "dark_blue",
    val overspeedEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val hapticEnabled: Boolean = true,
    val loudnessNormalization: Boolean = false,
    val language: String = "zh_cn",
    /** 超速提醒播报次数 */
    val overspeedRepeat: Int = 2,
    /** 超速提醒间隔（秒） */
    val overspeedInterval: Int = 5,
    /** 通知栏是否显示快捷操作按钮 */
    val notificationActions: Boolean = true
)

class BusViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext = app.applicationContext
    private val settingsStore = SettingsStore(appContext)
    private var settings: AppSettings = settingsStore.load()
    private var strings: AppStrings = stringsFor(settings.language)

    var uiState by mutableStateOf(
        UiState(
            limitOverride = settings.limitOverride,
            manualLimit = settings.manualLimit,
            sensitivityOverride = settings.sensitivityOverride,
            arrivalSensitivity = settings.arrivalSensitivity,
            overlapSensitivity = settings.overlapSensitivity,
            departureSensitivity = settings.departureSensitivity,
            doubleTapOverwrite = settings.doubleTapOverwrite,
            ruleOverride = settings.ruleOverride,
            ruleStart = settings.ruleStart,
            ruleEnd = settings.ruleEnd,
            ruleEndForecast = settings.ruleEndForecast,
            ruleArrival = settings.ruleArrival,
            ruleDeparture = settings.ruleDeparture,
            ruleArrivalExternal = settings.ruleArrivalExternal,
            ruleDepartureExternal = settings.ruleDepartureExternal,
            separateChannels = settings.separateChannels,
            themeId = settings.themeId,
            overspeedEnabled = settings.overspeedEnabled,
            keepScreenOn = settings.keepScreenOn,
            hapticEnabled = settings.hapticEnabled,
            loudnessNormalization = settings.loudnessNormalization,
            language = settings.language,
            overspeedRepeat = settings.overspeedRepeat,
            overspeedInterval = settings.overspeedInterval,
            notificationActions = settings.notificationActions
        )
    )
        private set

    private val data = DataStore(appContext)
    private val importer = Importer(appContext, data)
    private val planner = AnnouncementPlanner(data)
    private val player = AudioPlayer { settings.loudnessNormalization }
    // 自动进出站判定：复用下面已有的经纬度转换与距离计算，不重复造轮子
    private val autoEngine = AutoAnnounceEngine(
        toDecimal = { csvToDecimal(it) },
        distanceMeters = { lat1, lon1, lat2, lon2 -> distanceMeters(lat1, lon1, lat2, lon2) }
    )
    // 超速语音走独立播放器，可以和报站、服务音叠加
    private val overspeedPlayer = AudioPlayer { settings.loudnessNormalization }
    private var overspeedJob: Job? = null

    private var stops: List<Stop> = emptyList()
    private var steps: List<AnnouncementStep> = emptyList()
    private var cursor = 0
    private var selectedIndex = 0
    private var lastPlayedCursor: Int? = null
    private var lastStep: AnnouncementStep? = null
    private var playJob: Job? = null
    private var locationManager: LocationManager? = null
    private var nextPoint: Pair<Double, Double>? = null

    /** CSV 里的经纬度是“度分”格式，例如 11930.6582 = 119°30.6582′，转成十进制度 */
    private fun csvToDecimal(value: Double): Double {
        val sign = if (value < 0) -1.0 else 1.0
        val v = abs(value)
        val degree = floor(v / 100.0)
        val minutes = v - degree * 100.0
        return sign * (degree + minutes / 60.0)
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val radius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (radius * c).toInt()
    }

    /** 统一更新当前站、下一站坐标和距离 */
    private fun applyStation(index: Int) {
        val stop = stops.getOrNull(index)
        val nextStop = stops.getOrNull(index + 1)
        val point = nextStop?.let { csvToDecimal(it.lat) to csvToDecimal(it.lon) }
        nextPoint = point
        val hasLocation = uiState.currentLat != 0.0 || uiState.currentLon != 0.0
        uiState = uiState.copy(
            currentName = stop?.name ?: "",
            nextName = nextStop?.name ?: "—",
            limit = if (uiState.limitOverride) uiState.manualLimit else (stop?.limit ?: 0),
            nextLat = point?.first ?: 0.0,
            nextLon = point?.second ?: 0.0,
            distanceToNext = if (point != null && hasLocation) {
                distanceMeters(uiState.currentLat, uiState.currentLon, point.first, point.second)
            } else 0
        )
    }

    init {
        applyEngineSensitivity()
        viewModelScope.launch {
            val copied = data.prepare()
            val routes = data.listRoutes()
            val first = routes.firstOrNull() ?: ""
            uiState = uiState.copy(
                ready = true,
                routes = routes,
                // 发布版没有内置数据时，首次启动直接提示去导入或新增线路
                feedback = when {
                    copied -> strings.dataReady
                    routes.isEmpty() -> strings.noRoutes
                    else -> strings.ready
                }
            )
            if (first.isNotEmpty()) reloadRoute(first, true)
            startOverspeedMonitor()
        }
    }

    private fun feedback(text: String) {
        uiState = uiState.copy(feedback = text)
    }

    private fun stopPlayback() {
        playJob?.cancel()
        playJob = null
        player.stopCurrent()
    }

    /** 按设置覆写 audiolist.csv 的“是否播报”；未开启覆写时完全按 CSV。 */
    private fun isRuleEnabled(type: String): Boolean = when (type) {
        "起点播报" -> settings.ruleStart
        "终点播报" -> settings.ruleEnd
        "终点预报" -> settings.ruleEndForecast
        "进站播报" -> settings.ruleArrival
        "出站播报" -> settings.ruleDeparture
        "进站外音" -> settings.ruleArrivalExternal
        "出站外音" -> settings.ruleDepartureExternal
        else -> true
    }

    private fun effectiveRules(): List<AudioRule> {
        val base = data.loadRules()
        if (!settings.ruleOverride) return base
        return base.map { it.copy(enabled = isRuleEnabled(it.type)) }
    }

    /** 规则或声道设置变化后重建播报步骤，尽量保留当前进度。 */
    private fun rebuildSteps() {
        if (stops.isEmpty()) {
            steps = emptyList()
            return
        }
        val keep = cursor
        steps = planner.build(uiState.route, stops, effectiveRules(), settings.separateChannels)
        cursor = keep.coerceIn(0, steps.lastIndex.coerceAtLeast(0))
        lastPlayedCursor = null
    }

    private fun reloadRoute(route: String, up: Boolean) {
        if (route.isBlank()) {
            stops = emptyList()
            steps = emptyList()
            autoEngine.reset(emptyList())
            nextPoint = null
            uiState = uiState.copy(
                route = "",
                currentName = "",
                nextName = "—",
                startName = "",
                endName = "",
                limit = 0,
                nextLat = 0.0,
                nextLon = 0.0,
                distanceToNext = 0
            )
            return
        }

        val hasS = data.hasDirection(route, true)
        val hasX = data.hasDirection(route, false)
        val actualUp = if (data.hasDirection(route, up)) up else true

        stops = data.loadStops(route, actualUp)
        steps = planner.build(route, stops, effectiveRules(), settings.separateChannels)
        // 换线/换方向后，进站、出站和“重合”状态全部重新开始
        autoEngine.reset(stops)
        cursor = 0
        selectedIndex = 0
        lastPlayedCursor = null
        lastStep = null

        val isLoop = stops.isNotEmpty() && stops.first().voice == stops.last().voice
        val mode = when {
            hasS && hasX -> if (actualUp) strings.up else strings.down
            isLoop -> strings.loop
            else -> strings.oneWay
        }

        applyStation(0)
        uiState = uiState.copy(
            route = route,
            up = actualUp,
            routeMode = mode,
            canSwitch = hasS && hasX,
            startName = stops.firstOrNull()?.name ?: "",
            endName = stops.lastOrNull()?.name ?: "",
            playing = false
        )
    }

    private fun routeDisplay(route: String): String =
        if (route.isNotEmpty() && route.last().isDigit()) route + strings.routeSuffix else route

    fun selectRoute(route: String) {
        stopPlayback()
        reloadRoute(route, uiState.up)
        feedback(strings.routeSelected(routeDisplay(route)))
    }

    /** 线路编辑器保存后调用：刷新线路列表并切到该线路。 */
    fun refreshRoutes(preferred: String) {
        stopPlayback()
        val routes = data.listRoutes()
        uiState = uiState.copy(routes = routes)
        if (preferred.isNotBlank() && routes.contains(preferred)) {
            reloadRoute(preferred, true)
            feedback(strings.routeSaved(routeDisplay(preferred)))
        }
    }

    fun toggleDirection() {
        if (!uiState.canSwitch) {
            feedback(uiState.routeMode + strings.cannotSwitch)
            return
        }
        stopPlayback()
        reloadRoute(uiState.route, !uiState.up)
        feedback(strings.switchedTo + uiState.routeMode)
    }

    // ==================== 全局设置 ====================
    private fun updateSettings(newSettings: AppSettings) {
        settings = newSettings
        settingsStore.save(newSettings)
    }

    /** 把灵敏度设置同步到自动判定引擎；关时按 CSV 站前里程 + 默认重合/出站半径。 */
    private fun applyEngineSensitivity() {
        if (settings.sensitivityOverride) {
            autoEngine.setSensitivity(
                settings.arrivalSensitivity,
                settings.overlapSensitivity,
                settings.departureSensitivity
            )
        } else {
            autoEngine.setSensitivity(null, AUTO_OVERLAP_RADIUS_METERS, AUTO_DEPARTURE_RADIUS_METERS)
        }
    }

    fun setLimitOverride(enabled: Boolean) {
        updateSettings(settings.copy(limitOverride = enabled))
        uiState = uiState.copy(limitOverride = enabled)
        applyStation(selectedIndex)
    }

    fun setManualLimit(value: Int) {
        val v = value.coerceIn(1, 999)
        updateSettings(settings.copy(manualLimit = v))
        uiState = uiState.copy(manualLimit = v)
        applyStation(selectedIndex)
    }

    fun setSensitivityOverride(enabled: Boolean) {
        updateSettings(settings.copy(sensitivityOverride = enabled))
        uiState = uiState.copy(sensitivityOverride = enabled)
        applyEngineSensitivity()
    }

    fun setArrivalSensitivity(value: Int) {
        val v = value.coerceIn(1, 999)
        updateSettings(settings.copy(arrivalSensitivity = v))
        uiState = uiState.copy(arrivalSensitivity = v)
        applyEngineSensitivity()
    }

    fun setOverlapSensitivity(value: Int) {
        val v = value.coerceIn(1, 999)
        updateSettings(settings.copy(overlapSensitivity = v))
        uiState = uiState.copy(overlapSensitivity = v)
        applyEngineSensitivity()
    }

    fun setDepartureSensitivity(value: Int) {
        val v = value.coerceIn(1, 999)
        updateSettings(settings.copy(departureSensitivity = v))
        uiState = uiState.copy(departureSensitivity = v)
        applyEngineSensitivity()
    }

    fun setDoubleTapOverwrite(enabled: Boolean) {
        updateSettings(settings.copy(doubleTapOverwrite = enabled))
        uiState = uiState.copy(doubleTapOverwrite = enabled)
    }

    fun setRuleOverride(enabled: Boolean) {
        updateSettings(settings.copy(ruleOverride = enabled))
        uiState = uiState.copy(ruleOverride = enabled)
        rebuildSteps()
    }

    /** 某个播报类型是否播报（覆写 audiolist.csv 时生效）。 */
    fun setRuleEnabled(type: String, enabled: Boolean) {
        val s = when (type) {
            "起点播报" -> settings.copy(ruleStart = enabled)
            "终点播报" -> settings.copy(ruleEnd = enabled)
            "终点预报" -> settings.copy(ruleEndForecast = enabled)
            "进站播报" -> settings.copy(ruleArrival = enabled)
            "出站播报" -> settings.copy(ruleDeparture = enabled)
            "进站外音" -> settings.copy(ruleArrivalExternal = enabled)
            "出站外音" -> settings.copy(ruleDepartureExternal = enabled)
            else -> settings
        }
        updateSettings(s)
        uiState = uiState.copy(
            ruleStart = s.ruleStart,
            ruleEnd = s.ruleEnd,
            ruleEndForecast = s.ruleEndForecast,
            ruleArrival = s.ruleArrival,
            ruleDeparture = s.ruleDeparture,
            ruleArrivalExternal = s.ruleArrivalExternal,
            ruleDepartureExternal = s.ruleDepartureExternal
        )
        rebuildSteps()
    }

    fun setSeparateChannels(enabled: Boolean) {
        updateSettings(settings.copy(separateChannels = enabled))
        uiState = uiState.copy(separateChannels = enabled)
        rebuildSteps()
    }

    fun setThemeId(themeId: String) {
        updateSettings(settings.copy(themeId = themeId))
        uiState = uiState.copy(themeId = themeId)
    }

    fun setOverspeedRepeat(count: Int) {
        val value = count.coerceIn(1, 20)
        updateSettings(settings.copy(overspeedRepeat = value))
        uiState = uiState.copy(overspeedRepeat = value)
    }

    fun setOverspeedInterval(seconds: Int) {
        val value = seconds.coerceIn(1, 999)
        updateSettings(settings.copy(overspeedInterval = value))
        uiState = uiState.copy(overspeedInterval = value)
    }

    /** 通知栏快捷操作开关：关掉后通知仍显示状态，但不再带「报站 / 停止」按钮。 */
    fun setNotificationActions(enabled: Boolean) {
        updateSettings(settings.copy(notificationActions = enabled))
        uiState = uiState.copy(notificationActions = enabled)
        // 立刻刷新一次通知，不用等轮询
        AnnouncerService.refresh(appContext)
    }

    fun setOverspeedEnabled(enabled: Boolean) {
        updateSettings(settings.copy(overspeedEnabled = enabled))
        uiState = uiState.copy(overspeedEnabled = enabled)
    }

    fun setKeepScreenOn(enabled: Boolean) {
        updateSettings(settings.copy(keepScreenOn = enabled))
        uiState = uiState.copy(keepScreenOn = enabled)
    }

    fun setHapticEnabled(enabled: Boolean) {
        updateSettings(settings.copy(hapticEnabled = enabled))
        uiState = uiState.copy(hapticEnabled = enabled)
    }

    fun setLoudnessNormalization(enabled: Boolean) {
        updateSettings(settings.copy(loudnessNormalization = enabled))
        uiState = uiState.copy(loudnessNormalization = enabled)
    }

    fun setLanguage(languageId: String) {
        updateSettings(settings.copy(language = languageId))
        strings = stringsFor(languageId)

        // 只刷新会随语言变化的线路模式文案，不重置当前站点和播放进度
        val route = uiState.route
        if (route.isBlank() || stops.isEmpty()) {
            uiState = uiState.copy(language = languageId, feedback = strings.ready)
            return
        }
        val hasS = data.hasDirection(route, true)
        val hasX = data.hasDirection(route, false)
        val isLoop = stops.first().voice == stops.last().voice
        val mode = when {
            hasS && hasX -> if (uiState.up) strings.up else strings.down
            isLoop -> strings.loop
            else -> strings.oneWay
        }
        uiState = uiState.copy(language = languageId, routeMode = mode, feedback = strings.ready)
    }

    /** 双击当前站：用手机当前位置覆写线路 CSV 里该站的经纬度。 */
    fun overwriteStationLocation() {
        if (!uiState.doubleTapOverwrite) return
        val lat = uiState.currentLat
        val lon = uiState.currentLon
        if (lat == 0.0 && lon == 0.0) {
            Toast.makeText(appContext, strings.noLocation, Toast.LENGTH_SHORT).show()
            return
        }
        val stop = stops.getOrNull(selectedIndex)
        if (stop == null || stop.id.isBlank()) return

        val ok = data.updateStopCoordinate(uiState.route, uiState.up, stop.id, lat, lon)
        if (!ok) {
            Toast.makeText(appContext, strings.overwriteFailed, Toast.LENGTH_SHORT).show()
            return
        }

        // 重新读取 CSV，并保留当前选中的站点，避免画面跳回起点
        val keep = selectedIndex
        stops = data.loadStops(uiState.route, uiState.up)
        steps = planner.build(uiState.route, stops, effectiveRules(), settings.separateChannels)
        autoEngine.reset(stops)
        selectedIndex = keep.coerceIn(0, stops.lastIndex.coerceAtLeast(0))
        applyStation(selectedIndex)
        Toast.makeText(appContext, strings.overwriteSuccess, Toast.LENGTH_SHORT).show()
    }

    /** 手动报站：播完一项后不自动推进，再按一次才进入下一项。 */
    fun announce() {
        if (!uiState.ready || steps.isEmpty()) {
            feedback(strings.noRouteSelected)
            return
        }
        // 再按一次报站：打断当前播放，并直接推进到下一组
        val interrupted = player.isPlaying()
        stopPlayback()

        playJob = viewModelScope.launch {
            uiState = uiState.copy(playing = true)

            if (interrupted || lastPlayedCursor == cursor) {
                if (cursor == steps.lastIndex) {
                    if (uiState.canSwitch) {
                        reloadRoute(uiState.route, !uiState.up)
                        feedback(strings.endOfRouteSwitch(uiState.routeMode))
                    } else {
                        cursor = 0
                        lastPlayedCursor = null
                        feedback(strings.endOfRouteRestart)
                    }
                } else {
                    cursor++
                    lastPlayedCursor = null
                }
            }

            if (steps.isEmpty()) {
                uiState = uiState.copy(playing = false, feedback = strings.routeDataEmpty)
                return@launch
            }

            val step = steps[cursor]
            lastStep = step
            selectedIndex = step.stopIndex
            applyStation(step.stopIndex)
            uiState = uiState.copy(feedback = "${step.type}：${step.detail}")

            player.play(step.cues)

            lastPlayedCursor = cursor
            uiState = uiState.copy(
                playing = false,
                feedback = strings.announceFinished(step.type)
            )
        }
    }

    /** 终止整条播报（例如整段进站播报），连同后面还没播的音频一起停掉。 */
    fun stop() {
        if (player.isPlaying() || playJob?.isActive == true) {
            lastPlayedCursor = cursor
            stopPlayback()
            uiState = uiState.copy(playing = false, feedback = strings.stopped)
        } else {
            feedback(strings.noAudioPlaying)
        }
    }

    /** 上下箭头：只切换站点，不播报；之后按“报站”会播报所选站点。 */
    fun moveStation(delta: Int) {
        if (stops.isEmpty()) {
            feedback(strings.noStops)
            return
        }
        val next = (selectedIndex + delta).coerceIn(0, stops.lastIndex)
        if (next == selectedIndex) {
            feedback(if (delta < 0) strings.atStart else strings.atEnd)
            return
        }

        stopPlayback()
        uiState = uiState.copy(playing = false)
        selectedIndex = next
        val stop = stops[next]

        // 该站对应的主播报：起点用起点播报，中间站用进站播报，终点用终点播报
        val type = when {
            next == 0 -> "起点播报"
            next == stops.lastIndex && steps.none { it.stopIndex == next && it.type == "进站播报" } -> "终点播报"
            else -> "进站播报"
        }
        val target = steps.indexOfFirst { it.stopIndex == next && it.type == type }
        if (target >= 0) {
            cursor = target
            lastPlayedCursor = null
        }

        applyStation(next)
        uiState = uiState.copy(feedback = strings.selectedStation(stop.name))
    }

    /** 重播刚播完、还没有推进到下一项的那一项。 */
    fun repeat() {
        val step = lastStep
        if (step == null) {
            feedback(strings.nothingToRepeat)
            return
        }
        stopPlayback()
        playJob = viewModelScope.launch {
            uiState = uiState.copy(playing = true, feedback = strings.repeatPrefix(step.detail))
            player.play(step.cues)
            uiState = uiState.copy(playing = false, feedback = strings.repeatDone)
        }
    }

    /** 数字键：播放“服务音[数字].mp3”，找不到就静默忽略。 */
    fun pressKey(key: String) {
        uiState = uiState.copy(key = key)
        val file = data.audioFile("服务音$key")
        if (file == null) {
            feedback(strings.serviceToneMissing(key))
            return
        }
        // 服务音打断当前报站；下次按报站时从下一组继续
        if (player.isPlaying()) lastPlayedCursor = cursor
        stopPlayback()
        playJob = viewModelScope.launch {
            uiState = uiState.copy(playing = true, feedback = strings.serviceTonePlaying(key))
            player.play(listOf(AudioCue(file)))
            uiState = uiState.copy(playing = false, feedback = strings.serviceToneDone(key))
        }
    }

    fun importRoutesZip(uri: Uri) {
        importWith(ZipScope.ROUTES, uri, strings.importingRoutes)
    }

    fun importAudioZip(uri: Uri) {
        importWith(ZipScope.AUDIO, uri, strings.importingAudio)
    }

    fun importAllZip(uri: Uri) {
        importWith(ZipScope.ALL, uri, strings.importingAll)
    }

    fun importRules(uri: Uri) {
        takeReadPermission(uri)
        viewModelScope.launch {
            feedback(strings.importingRules)
            val message = try {
                if (importer.isZip(uri)) {
                    importer.importZip(uri, ZipScope.RULES)
                } else {
                    importer.importCsv(uri)
                }
            } catch (_: SecurityException) {
                strings.readDenied
            } catch (e: Exception) {
                strings.importFailed(e.message ?: strings.unknownError)
            }
            refreshAfterImport(message)
        }
    }

    /** 一键导出 bus / mp3 / system 为一个 zip。 */
    fun exportAllData(uri: Uri) {
        viewModelScope.launch {
            feedback(strings.exporting)
            feedback(data.exportAll(uri))
        }
    }

    /**
     * 尽量把所选文件的读取权限“持久化”。
     * 某些车机的文件提供方在返回 URI 后权限很短暂，先拿住再读更稳。
     * 不支持持久授权的提供方会抛异常，忽略即可（临时授权仍然有效）。
     */
    private fun takeReadPermission(uri: Uri) {
        try {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
    }

    private fun importWith(scope: ZipScope, uri: Uri, tip: String) {
        takeReadPermission(uri)
        viewModelScope.launch {
            feedback(tip)
            val isZip = try {
                importer.isZip(uri)
            } catch (_: SecurityException) {
                feedback(strings.readDenied)
                return@launch
            }
            if (!isZip) {
                feedback(strings.notZip)
                return@launch
            }
            val message = try {
                importer.importZip(uri, scope)
            } catch (e: Exception) {
                strings.importFailed(e.message ?: strings.unknownError)
            }
            refreshAfterImport(message)
        }
    }

    private fun refreshAfterImport(message: String) {
        stopPlayback()
        val routes = data.listRoutes()
        val current = uiState.route
        val selected = if (routes.contains(current)) current else (routes.firstOrNull() ?: "")
        reloadRoute(selected, uiState.up)
        val routesText = if (routes.isEmpty()) {
            strings.noRoutesAvailable
        } else {
            strings.routesAvailable(routes.joinToString(", "))
        }
        uiState = uiState.copy(routes = routes, feedback = "$message（$routesText）")
    }

    /**
     * 超速提醒：按设置的次数与间隔播“超速语音”（默认 2 次、间隔 5 秒），
     * 播完一轮后若仍超速就继续下一轮。独立播放器，可与报站/服务音叠加。
     */
    private fun startOverspeedMonitor() {
        overspeedJob?.cancel()
        overspeedJob = viewModelScope.launch {
            val file = data.audioFile("超速语音")
            while (isActive) {
                val limit = uiState.limit
                if (file != null && settings.overspeedEnabled && limit > 0 && uiState.speed > limit) {
                    val times = settings.overspeedRepeat.coerceIn(1, 20)
                    val gap = settings.overspeedInterval.coerceIn(1, 999) * 1000L
                    for (i in 0 until times) {
                        if (!isActive) break
                        overspeedPlayer.play(listOf(AudioCue(file)))
                        // 最后一次播完也要等一个间隔，再判断是否继续下一轮
                        delay(gap)
                    }
                } else {
                    delay(1000)
                }
            }
        }
    }

    /** 找到自动事件对应的播报步骤：优先标准类型，起终点用起点/终点播报代替。 */
    private fun findAutoStep(event: AutoAnnounceEvent): AutoPlayItem? {
        if (steps.isEmpty()) return null
        val wantedType = if (event.type == AutoAnnounceType.ARRIVAL) "进站播报" else "出站播报"
        var index = steps.indexOfFirst { it.stopIndex == event.stopIndex && it.type == wantedType }

        // planner 不为起点/终点生成“进站播报”，改用起点播报 / 终点播报。
        // 注意这里比较的是站点下标（stops），不是播报步骤下标（steps）。
        if (index < 0 && event.type == AutoAnnounceType.ARRIVAL) {
            index = when (event.stopIndex) {
                0 -> steps.indexOfFirst { it.stopIndex == 0 && it.type == "起点播报" }
                stops.lastIndex -> steps.indexOfFirst {
                    it.stopIndex == event.stopIndex && it.type == "终点播报"
                }
                else -> -1
            }
        }
        // 终点前一站的“出站播报”在 planner 里叫“终点预报”
        if (index < 0 && event.type == AutoAnnounceType.DEPARTURE &&
            event.stopIndex == stops.lastIndex - 1
        ) {
            index = steps.indexOfFirst { it.stopIndex == event.stopIndex && it.type == "终点预报" }
        }
        return if (index >= 0) AutoPlayItem(event, index, steps[index]) else null
    }

    /** 自动播报：打断当前播放，把本次事件按顺序播完，并同步手动游标。 */
    private fun playAutoEvents(events: List<AutoAnnounceEvent>) {
        val items = events.mapNotNull { findAutoStep(it) }
        if (items.isEmpty()) return

        stopPlayback()
        playJob = viewModelScope.launch {
            uiState = uiState.copy(playing = true)
            for (item in items) {
                if (!isActive) break
                selectedIndex = item.event.stopIndex
                cursor = item.stepIndex
                lastPlayedCursor = item.stepIndex
                lastStep = item.step
                applyStation(item.event.stopIndex)
                uiState = uiState.copy(feedback = "自动${item.event.type.label}：${item.step.detail}")
                player.play(item.step.cues)
            }
            uiState = uiState.copy(playing = false)
        }
    }

    /** 由 Activity 在拿到定位权限后调用。 */
    @SuppressLint("MissingPermission")
    fun startLocation() {
        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return
        locationManager = manager

        val granted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            uiState = uiState.copy(gps = "无")
            return
        }

        val provider = if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            LocationManager.GPS_PROVIDER
        } else {
            LocationManager.NETWORK_PROVIDER
        }

        try {
            manager.requestLocationUpdates(provider, 1000L, 0f, locationListener)
        } catch (_: SecurityException) {
            uiState = uiState.copy(gps = "无")
        }
    }

    fun locationDenied() {
        uiState = uiState.copy(gps = "无")
    }

    @Suppress("DEPRECATION")
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val level = when {
                location.accuracy <= 10 -> "强"
                location.accuracy <= 50 -> "中"
                else -> "弱"
            }
            val speed = if (location.hasSpeed()) {
                (location.speed * 3.6f).toInt().coerceIn(0, 200)
            } else {
                uiState.speed
            }
            val lat = location.latitude
            val lon = location.longitude
            val distance = nextPoint?.let { distanceMeters(lat, lon, it.first, it.second) } ?: 0
            uiState = uiState.copy(
                gps = level,
                speed = speed,
                currentLat = lat,
                currentLon = lon,
                distanceToNext = distance
            )

            // 无有效定位（0/0）或精度太差时不判定，避免误触发
            val hasValidFix = (lat != 0.0 || lon != 0.0) &&
                location.accuracy <= AUTO_MAX_ACCURACY_METERS
            if (hasValidFix) {
                val events = autoEngine.onLocation(lat, lon)
                if (events.isNotEmpty()) playAutoEvents(events)
            }
        }

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) {
            uiState = uiState.copy(gps = "无")
        }
    }

    override fun onCleared() {
        playJob?.cancel()
        overspeedJob?.cancel()
        overspeedPlayer.release()
        locationManager?.removeUpdates(locationListener)
        player.release()
        super.onCleared()
    }
}
