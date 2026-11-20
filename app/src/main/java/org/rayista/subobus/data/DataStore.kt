package org.rayista.subobus.data

import android.content.Context
import android.net.Uri
import org.rayista.subobus.i18n.stringsFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * App 的数据都放在 context.filesDir 下：
 *   filesDir/bus/1/1S.csv
 *   filesDir/mp3/科技东园.mp3
 *   filesDir/system/audiolist.csv
 * 第一次启动时会把 assets 里自带的初始数据复制出来（发布版不含任何线路/音频，
 * 此时这里是空的，用户自己导入即可）；以后导入都是增量覆盖。
 */
class DataStore(private val context: Context) {

    val busDir: File get() = File(context.filesDir, "bus")
    val mp3Dir: File get() = File(context.filesDir, "mp3")
    val systemDir: File get() = File(context.filesDir, "system")

    private val readyFlag: File get() = File(context.filesDir, ".data_ready")

    /**
     * 首次启动：把 assets 里的初始数据复制到可读写目录，并把 CSV 统一成 UTF-8。
     * 返回值表示“是否真的复制到了内置数据”（发布版没有内置数据时返回 false）。
     */
    suspend fun prepare(): Boolean = withContext(Dispatchers.IO) {
        if (readyFlag.exists()) {
            false
        } else {
            val bus = copyAssetDir("bus", busDir)
            val mp3 = copyAssetDir("mp3", mp3Dir)
            val system = copyAssetDir("system", systemDir)
            normalizeCsv(busDir)
            normalizeCsv(systemDir)
            readyFlag.writeText("ready")
            bus || mp3 || system
        }
    }

    /** 复制 assets 里的目录；返回是否成功复制了至少一个文件。 */
    private fun copyAssetDir(assetPath: String, target: File): Boolean {
        // assets 里没有这个目录时 list() 会返回空数组，这里不能直接当文件去打开，
        // 否则发布版（不带内置数据）首次启动会抛异常。
        val children = try {
            context.assets.list(assetPath)
        } catch (_: Exception) {
            null
        }
        if (children.isNullOrEmpty()) {
            return copyAssetFile(assetPath, File(target, File(assetPath).name))
        }
        target.mkdirs()
        var copied = false
        for (child in children) {
            val childPath = if (assetPath.isEmpty()) child else "$assetPath/$child"
            val nested = try {
                context.assets.list(childPath)
            } catch (_: Exception) {
                null
            }
            copied = if (nested.isNullOrEmpty()) {
                copyAssetFile(childPath, File(target, child)) || copied
            } else {
                copyAssetDir(childPath, File(target, child)) || copied
            }
        }
        return copied
    }

    /** 复制 assets 里的单个文件；没有这个资源就跳过，返回是否复制成功。 */
    private fun copyAssetFile(assetPath: String, target: File): Boolean {
        return try {
            context.assets.open(assetPath).use { input ->
                target.parentFile?.mkdirs()
                target.outputStream().use { output -> input.copyTo(output) }
            }
            true
        } catch (_: Exception) {
            // 发布版不含内置测试数据，属于正常情况
            false
        }
    }

    private fun normalizeCsv(dir: File) {
        if (!dir.exists()) return
        dir.walkTopDown()
            .filter { it.isFile && it.extension.equals("csv", true) }
            .forEach { file ->
                val original = file.readBytes()
                val utf8 = csvToUtf8(original)
                if (!utf8.contentEquals(original)) file.writeBytes(utf8)
            }
    }

    /** 已导入的线路号（bus 目录下含 S 或 X 文件的文件夹，文件名大小写不敏感）。 */
    fun listRoutes(): List<String> {
        val dirs = busDir.listFiles { file: File -> file.isDirectory } ?: return emptyList()
        return dirs.map { it.name }
            .filter { route -> hasDirection(route, true) || hasDirection(route, false) }
            .sortedWith(compareBy({ it.toIntOrNull() ?: Int.MAX_VALUE }, { it }))
    }

    /** 允许 .CSV / .csv，也兼容旧版本导入留下的大写文件名。 */
    private fun routeFile(route: String, up: Boolean): File? {
        val suffix = if (up) "S" else "X"
        val name = "$route$suffix.csv"
        return File(busDir, route)
            .listFiles { file: File -> file.isFile && file.name.equals(name, ignoreCase = true) }
            ?.firstOrNull()
    }

    fun hasDirection(route: String, up: Boolean): Boolean = routeFile(route, up) != null

    fun loadStops(route: String, up: Boolean): List<Stop> {
        val useUp = if (hasDirection(route, up)) up else !up
        val file = routeFile(route, useUp) ?: return emptyList()

        val rows = parseCsv(decodeCsv(file.readBytes()))
        if (rows.size < 2) return emptyList()

        val header = rows[0].map { it.trim().trim('"').replace("\uFEFF", "") }
        fun column(name: String, fallback: Int): Int =
            header.indexOf(name).takeIf { it >= 0 } ?: fallback

        val cId = column("站台编号", 0)
        val cVoice = column("报站语音", 1)
        val cName = column("站名", 2)
        val cLon = column("经度", 3)
        val cLat = column("纬度", 4)
        val cAngle = column("角度", 5)
        val cAdIn = column("进站广告", 6)
        val cAdOut = column("出站广告", 7)
        val cTipIn = column("进站提示", 8)
        val cTipOut = column("出站提示", 9)
        val cExtIn = column("进站扩展", 10)
        val cExtOut = column("出站扩展", 11)
        val cLimit = column("限速", 12)
        val cBefore = column("站前里程", 13)
        val cBig = column("是否大站", 14)
        val cExternal = column("外音开否", 15)

        return rows.drop(1)
            .filter { it.firstOrNull()?.isNotBlank() == true }
            .map { row ->
                fun s(index: Int) = row.getOrNull(index)?.trim() ?: ""
                Stop(
                    id = s(cId),
                    voice = s(cVoice),
                    name = s(cName),
                    lon = s(cLon).toDoubleOrNull() ?: 0.0,
                    lat = s(cLat).toDoubleOrNull() ?: 0.0,
                    angle = s(cAngle).toDoubleOrNull() ?: 0.0,
                    adIn = s(cAdIn),
                    adOut = s(cAdOut),
                    tipIn = s(cTipIn),
                    tipOut = s(cTipOut),
                    extIn = s(cExtIn),
                    extOut = s(cExtOut),
                    limit = s(cLimit).toIntOrNull() ?: 0,
                    distanceBefore = s(cBefore).toIntOrNull() ?: 0,
                    big = s(cBig),
                    external = s(cExternal)
                )
            }
    }

    fun loadRules(): List<AudioRule> {
        val file = File(systemDir, "audiolist.csv")
        if (!file.exists()) return defaultRules()
        val rows = parseCsv(decodeCsv(file.readBytes()))
        return rows.drop(1)
            .filter { it.firstOrNull()?.isNotBlank() == true }
            .map { row ->
                val slots = (2..16).map { row.getOrNull(it)?.trim() ?: "" }
                    .filter { it.isNotBlank() }
                AudioRule(
                    type = row.getOrNull(0)?.trim() ?: "",
                    enabled = row.getOrNull(1)?.trim() == "是",
                    slots = slots
                )
            }
            .ifEmpty { defaultRules() }
    }

    /** 找不到 mp3 就返回 null，调用方直接跳过，不报错。 */
    fun audioFile(name: String): File? {
        val safe = name.trim()
            .replace("/", "_")
            .replace("\\", "_")
            .replace("..", "_")
        if (safe.isEmpty()) return null
        val file = File(mp3Dir, "$safe.mp3")
        return if (file.exists() && file.length() > 0) file else null
    }

    fun writeCsv(relativePath: String, bytes: ByteArray) {
        val file = File(context.filesDir, relativePath)
        file.parentFile?.mkdirs()
        file.writeBytes(csvToUtf8(bytes))
    }

    /** 线路编辑器保存：站台编号按当前顺序重新生成。 */
    fun writeStops(route: String, up: Boolean, stops: List<Stop>) {
        val dir = File(busDir, route)
        dir.mkdirs()
        val file = File(dir, "$route${if (up) "S" else "X"}.csv")
        val header = listOf(
            "站台编号", "报站语音", "站名", "经度", "纬度", "角度",
            "进站广告", "出站广告", "进站提示", "出站提示", "进站扩展", "出站扩展",
            "限速", "站前里程", "是否大站", "外音开否"
        )
        val text = buildString {
            append(header.joinToString(",")).append('\n')
            stops.forEachIndexed { index, stop ->
                val row = listOf(
                    (index + 1).toString(), stop.voice, stop.name,
                    coordText(stop.lon), coordText(stop.lat), coordText(stop.angle),
                    stop.adIn, stop.adOut, stop.tipIn, stop.tipOut,
                    stop.extIn, stop.extOut,
                    stop.limit.toString(), stop.distanceBefore.toString(),
                    stop.big, stop.external
                )
                append(row.joinToString(",") { escapeCsv(it) }).append('\n')
            }
        }
        file.writeText(text, Charsets.UTF_8)
    }

    fun deleteRouteDirection(route: String, up: Boolean) {
        routeFile(route, up)?.delete()
    }

    /** 重命名线路：文件夹 + 里面的 S/X 文件一起改名。目标已存在时返回 false。 */
    fun renameRoute(oldName: String, newName: String): Boolean {
        if (oldName == newName) return true
        val oldDir = File(busDir, oldName)
        if (!oldDir.isDirectory) return false
        val newDir = File(busDir, newName)
        if (newDir.exists()) return false
        if (!oldDir.renameTo(newDir)) return false
        listOf("S", "X").forEach { suffix ->
            val oldFile = newDir.listFiles { f ->
                f.isFile && f.name.equals("$oldName$suffix.csv", ignoreCase = true)
            }?.firstOrNull() ?: return@forEach
            oldFile.renameTo(File(newDir, "$newName$suffix.csv"))
        }
        return true
    }

    private fun coordText(value: Double): String =
        if (value == 0.0) "" else String.format(java.util.Locale.US, "%.4f", value)

    /**
     * 把某个站点的经纬度写回线路 CSV。
     * 传入的是十进制度（手机 GPS），CSV 里保存的是“度分”格式（DDMM.MMMM）。
     * 返回 true 表示写入成功。
     */
    fun updateStopCoordinate(route: String, up: Boolean, stopId: String, lat: Double, lon: Double): Boolean {
        val file = routeFile(route, if (hasDirection(route, up)) up else !up) ?: return false
        val rows = parseCsv(decodeCsv(file.readBytes())).toMutableList()
        if (rows.size < 2) return false

        val header = rows[0].map { it.trim().trim('"').replace("\uFEFF", "") }
        fun column(name: String, fallback: Int): Int =
            header.indexOf(name).takeIf { it >= 0 } ?: fallback
        val cId = column("站台编号", 0)
        val cLon = column("经度", 3)
        val cLat = column("纬度", 4)

        var changed = false
        for (i in 1 until rows.size) {
            val row = rows[i].toMutableList()
            if (row.getOrNull(cId)?.trim() != stopId) continue
            while (row.size <= maxOf(cLon, cLat)) row.add("")
            row[cLon] = decimalToDdMm(lon)
            row[cLat] = decimalToDdMm(lat)
            rows[i] = row
            changed = true
            break
        }
        if (!changed) return false

        val text = rows.joinToString("\n") { row -> row.joinToString(",") { escapeCsv(it) } }
        file.writeBytes(text.toByteArray(Charsets.UTF_8))
        return true
    }

    /** 十进制度 -> 度分格式（119.51097 -> 11930.6582）。 */
    private fun decimalToDdMm(value: Double): String {
        val sign = if (value < 0) -1.0 else 1.0
        val v = kotlin.math.abs(value)
        val degree = kotlin.math.floor(v)
        val minutes = (v - degree) * 60.0
        val ddmm = degree * 100.0 + minutes
        return String.format(java.util.Locale.US, "%.4f", sign * ddmm)
    }

    private fun escapeCsv(field: String): String {
        return if (field.contains(',') || field.contains('"') || field.contains('\n')) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }

    fun writeBinary(relativePath: String, bytes: ByteArray) {
        val file = File(context.filesDir, relativePath)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    /** 把 bus/、mp3/、system/ 整体打包，写到用户选择的位置。 */
    suspend fun exportAll(uri: Uri): String = withContext(Dispatchers.IO) {
        val t = stringsFor(SettingsStore(context).load().language)
        val stream = context.contentResolver.openOutputStream(uri)
            ?: return@withContext t.exportCannotWrite
        var count = 0
        var bytes = 0L
        try {
            ZipOutputStream(stream.buffered()).use { zip ->
                listOf("bus" to busDir, "mp3" to mp3Dir, "system" to systemDir)
                    .forEach { (prefix, dir) ->
                        if (!dir.exists()) return@forEach
                        dir.walkTopDown()
                            .filter { it.isFile && it.length() > 0 }
                            .forEach { file ->
                                val rel = file.relativeTo(dir).invariantSeparatorsPath
                                zip.putNextEntry(ZipEntry("$prefix/$rel"))
                                file.inputStream().use { input -> input.copyTo(zip) }
                                zip.closeEntry()
                                count++
                                bytes += file.length()
                            }
                    }
            }
        } catch (e: Exception) {
            return@withContext t.failed(e.message ?: t.unknownError)
        }
        if (count == 0) t.exportNothing else t.exportDone(count, bytes / 1024 / 1024)
    }

    private fun defaultRules(): List<AudioRule> = listOf(
        AudioRule("起点播报", true, listOf("起点音1", "线路名称", "起点音2", "起点站", "起点音3", "终点站", "起点音4", "途径站", "起点音5", "起点音6", "下一站", "出站扩展")),
        AudioRule("终点播报", true, listOf("进站广告", "终点音2", "终点站", "终点音4", "进站提示", "终点音5", "进站扩展")),
        AudioRule("终点预报", true, listOf("出站广告", "终点音1", "终点音2", "出站提示", "终点站", "终点音3", "出站扩展")),
        AudioRule("进站播报", true, listOf("进站音1", "进站广告", "本站", "进站音2", "进站提示", "进站音3")),
        AudioRule("出站播报", true, listOf("出站广告", "出站音1", "下一站", "出站提示", "出站音2", "出站扩展")),
        AudioRule("进站外音", true, listOf("进站外音1", "线路名称", "进站外音3", "终点站")),
        AudioRule("出站外音", true, listOf("进站外音1", "进站外音2", "终点站"))
    )
}
