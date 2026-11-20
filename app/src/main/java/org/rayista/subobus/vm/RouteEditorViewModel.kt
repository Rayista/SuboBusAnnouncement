package org.rayista.subobus.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.rayista.subobus.data.DataStore
import org.rayista.subobus.data.SettingsStore
import org.rayista.subobus.i18n.AppStrings
import org.rayista.subobus.i18n.stringsFor
import org.rayista.subobus.data.Stop
import java.util.Locale

/** 编辑器里的站点：所有字段都用字符串保存，方便输入框直接编辑。 */
data class EditableStop(
    /** 稳定标识：拖拽排序时用作 LazyColumn 的 key。 */
    val uid: Long = nextStopUid(),
    val voice: String = "",
    val name: String = "",
    val lon: String = "",
    val lat: String = "",
    val angle: String = "",
    val adIn: String = "",
    val adOut: String = "",
    val tipIn: String = "",
    val tipOut: String = "",
    val extIn: String = "",
    val extOut: String = "",
    val limit: String = "50",
    val before: String = "85",
    val big: String = "小站",
    val external: Boolean = false
)

private val stopUidSeq = java.util.concurrent.atomic.AtomicLong(1)
private fun nextStopUid(): Long = stopUidSeq.getAndIncrement()

data class RouteEditorState(
    val route: String = "",
    val isNew: Boolean = false,
    val up: Boolean = true,
    val upStops: List<EditableStop> = emptyList(),
    val downStops: List<EditableStop> = emptyList(),
    val upTouched: Boolean = false,
    val downTouched: Boolean = false,
    val dirty: Boolean = false,
    val toast: String? = null
) {
    val stops: List<EditableStop> get() = if (up) upStops else downStops
}

/**
 * 线路数据编辑器的状态：上下行各一份站点列表，保存时写成 CSV。
 */
class RouteEditorViewModel(app: Application) : AndroidViewModel(app) {

    private val data = DataStore(app.applicationContext)
    private val settingsStore = SettingsStore(app.applicationContext)
    private var t: AppStrings = stringsFor("zh_cn")

    var state by mutableStateOf(RouteEditorState())
        private set

    fun open(route: String, isNew: Boolean) {
        t = stringsFor(settingsStore.load().language)
        state = if (isNew) {
            RouteEditorState(route = "", isNew = true)
        } else {
            RouteEditorState(
                route = route,
                isNew = false,
                upStops = data.loadStops(route, true).map { it.toEditable() },
                downStops = data.loadStops(route, false).map { it.toEditable() }
            )
        }
    }

    fun switchDirection() {
        state = state.copy(up = !state.up)
    }

    fun renameRoute(newName: String) {
        val name = newName.trim()
        if (name.isBlank()) {
            toast(t.nameEmpty)
            return
        }
        if (name.any { it in "\\/:*?\"<>|" }) {
            toast(t.nameInvalid)
            return
        }
        if (name == state.route) return
        if (!state.isNew && !data.renameRoute(state.route, name)) {
            toast(t.nameExists)
            return
        }
        state = state.copy(route = name)
        toast(t.renamed(name))
    }

    fun addStation(index: Int = state.stops.size, stop: EditableStop = EditableStop()) {
        val list = state.stops.toMutableList()
        list.add(index.coerceIn(0, list.size), stop)
        applyStops(list)
    }

    fun updateStation(index: Int, stop: EditableStop) {
        val list = state.stops.toMutableList()
        if (index !in list.indices) return
        list[index] = stop
        applyStops(list)
    }

    fun deleteStation(index: Int) {
        val list = state.stops.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        applyStops(list)
    }

    /** 长按拖拽排序：把 from 移动到 to。 */
    fun moveStation(from: Int, to: Int) {
        val list = state.stops.toMutableList()
        if (from !in list.indices || to !in list.indices || from == to) return
        val item = list.removeAt(from)
        list.add(to, item)
        applyStops(list)
    }

    /** 复制上行到下行：顺序颠倒；会标记下行已修改。 */
    fun copyUpToDown(): Boolean {
        if (state.upStops.isEmpty()) {
            toast(t.noUpStops)
            return false
        }
        state = state.copy(
            downStops = state.upStops.reversed(),
            downTouched = true,
            up = false,
            dirty = true
        )
        return true
    }

    /** 保存当前线路，返回是否成功。 */
    fun save(): Boolean {
        val route = state.route.trim()
        if (route.isBlank()) {
            toast(t.nameEmpty)
            return false
        }
        val upStops = state.upStops
        val downStops = state.downStops
        val all = upStops + downStops
        if (all.isEmpty()) {
            toast(t.needOneStop)
            return false
        }
        if (all.any { it.voice.isBlank() || it.name.isBlank() }) {
            toast(t.requiredError)
            return false
        }

        if (state.upTouched || (state.isNew && upStops.isNotEmpty())) {
            if (upStops.isEmpty()) {
                data.deleteRouteDirection(route, true)
            } else {
                data.writeStops(route, true, upStops.mapIndexed { i, s -> s.toStop(i, upStops.size) })
            }
        }
        if (state.downTouched || (state.isNew && downStops.isNotEmpty())) {
            if (downStops.isEmpty()) {
                data.deleteRouteDirection(route, false)
            } else {
                data.writeStops(route, false, downStops.mapIndexed { i, s -> s.toStop(i, downStops.size) })
            }
        }

        state = state.copy(
            dirty = false,
            isNew = false,
            upTouched = false,
            downTouched = false,
            toast = t.savedToast
        )
        return true
    }

    fun clearToast() {
        state = state.copy(toast = null)
    }

    private fun applyStops(list: List<EditableStop>) {
        state = if (state.up) {
            state.copy(upStops = list, upTouched = true, dirty = true)
        } else {
            state.copy(downStops = list, downTouched = true, dirty = true)
        }
    }

    private fun toast(message: String) {
        state = state.copy(toast = message)
    }

    private fun Stop.toEditable() = EditableStop(
        voice = voice,
        name = name,
        lon = coord(lon),
        lat = coord(lat),
        angle = coord(angle),
        adIn = adIn,
        adOut = adOut,
        tipIn = tipIn,
        tipOut = tipOut,
        extIn = extIn,
        extOut = extOut,
        limit = if (limit > 0) limit.toString() else "50",
        before = if (distanceBefore > 0) distanceBefore.toString() else "85",
        big = if (big == "大站" || big == "起点站" || big == "终点站") big else "小站",
        external = external == "是"
    )

    private fun EditableStop.toStop(index: Int, total: Int): Stop {
        val normalizedBig = when {
            index == 0 -> "起点站"
            index == total - 1 -> "终点站"
            big == "大站" -> "大站"
            else -> "小站"
        }
        return Stop(
            id = (index + 1).toString(),
            voice = voice.trim(),
            name = name.trim(),
            lon = lon.trim().toDoubleOrNull() ?: 0.0,
            lat = lat.trim().toDoubleOrNull() ?: 0.0,
            angle = angle.trim().toDoubleOrNull() ?: 0.0,
            adIn = adIn.trim(),
            adOut = adOut.trim(),
            tipIn = tipIn.trim(),
            tipOut = tipOut.trim(),
            extIn = extIn.trim(),
            extOut = extOut.trim(),
            limit = limit.trim().toIntOrNull() ?: 50,
            distanceBefore = before.trim().toIntOrNull() ?: 85,
            big = normalizedBig,
            external = if (external) "是" else "否"
        )
    }

    private fun coord(value: Double): String =
        if (value == 0.0) "" else String.format(Locale.US, "%.4f", value)
}
