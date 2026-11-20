package org.rayista.subobus.data

import android.content.Context
import android.net.Uri
import org.rayista.subobus.i18n.stringsFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream

/** 导入范围：线路 / 音频 / 规则 / 全部 */
enum class ZipScope { ROUTES, AUDIO, RULES, ALL }

/**
 * 通过系统文件选择器导入。zip 是增量覆盖：同路径覆盖，其他文件保留。
 * CSV 会检测编码并统一写成 UTF-8。
 */
class Importer(private val context: Context, private val data: DataStore) {

    /** 单独导入 audiolist.csv */
    suspend fun importCsv(uri: Uri): String = withContext(Dispatchers.IO) {
        val t = stringsFor(SettingsStore(context).load().language)
        val bytes = readUri(uri) ?: return@withContext t.readFileFailed
        data.writeCsv("system/audiolist.csv", bytes)
        t.rulesImported
    }

    suspend fun importZip(uri: Uri, scope: ZipScope): String = withContext(Dispatchers.IO) {
        val t = stringsFor(SettingsStore(context).load().language)
        val input = context.contentResolver.openInputStream(uri)
            ?: return@withContext t.readZipFailed

        var routeCount = 0
        var mp3Count = 0
        var ruleCount = 0
        var ignored = 0

        val wantRoutes = scope == ZipScope.ROUTES || scope == ZipScope.ALL
        val wantAudio = scope == ZipScope.AUDIO || scope == ZipScope.ALL
        val wantRules = scope == ZipScope.RULES || scope == ZipScope.ALL

        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    // 支持压缩包外面再套一层文件夹，例如 SuboBusAnnouncement/bus/11/11S.CSV
                    val parts = entry.name.replace('\\', '/')
                        .split('/')
                        .filter { it.isNotBlank() && it != "." && it != ".." }
                    val lower = parts.map { it.lowercase() }
                    val file = parts.lastOrNull() ?: ""
                    val busIndex = lower.indexOf("bus")
                    val mp3Index = lower.indexOf("mp3")
                    val systemIndex = lower.indexOf("system")

                    val isRule = file.equals("audiolist.csv", true) &&
                        (parts.size == 1 || (systemIndex >= 0 && parts.size == systemIndex + 2))

                    val isRoute = routeFileRegex.matches(file) &&
                        ((parts.size == 1 && busIndex < 0) ||
                            (busIndex >= 0 && parts.size == busIndex + 3 &&
                                parts[busIndex + 1].isNotBlank()))

                    val isAudio = file.endsWith(".mp3", true) &&
                        mp3Index >= 0 && parts.size == mp3Index + 2

                    when {
                        isRule && wantRules -> {
                            data.writeCsv("system/audiolist.csv", zip.readBytes())
                            ruleCount++
                        }

                        isRoute && wantRoutes -> {
                            val route = if (busIndex >= 0) {
                                parts[busIndex + 1]
                            } else {
                                // 根目录文件：去掉扩展名和末尾的 S/X 作为线路名（支持文字线路）
                                parts.last().substringBeforeLast('.').dropLast(1)
                            }
                            if (route.isNotBlank()) {
                                data.writeCsv(
                                    "bus/$route/${normalizeCsvName(file)}",
                                    zip.readBytes()
                                )
                                routeCount++
                            } else {
                                ignored++
                            }
                        }

                        isAudio && wantAudio -> {
                            data.writeBinary("mp3/${normalizeMp3Name(file)}", zip.readBytes())
                            mp3Count++
                        }

                        else -> ignored++
                    }
                }
                zip.closeEntry()
            }
        }

        t.importDone(routeCount, mp3Count, ruleCount, ignored)
    }

    /** 判断选择器给的是不是 zip（也兼容看不到扩展名的情况） */
    fun isZip(uri: Uri): Boolean = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.read() == 0x50 && input.read() == 0x4B // "PK"
        } ?: false
    } catch (_: Exception) {
        false
    }

    /** 线路文件名：任意线路名 + 末尾 S/X + .csv（支持“通勤1号S.csv”这种文字线路）。 */
    private val routeFileRegex = Regex("^.+[sSxX]\\.csv$", RegexOption.IGNORE_CASE)

    /** 11S.CSV -> 11S.csv；通勤1号s.csv -> 通勤1号S.csv，保证和 DataStore 的查找名字一致。 */
    private fun normalizeCsvName(name: String): String {
        val base = name.substringBeforeLast('.')
        val normalized = if (base.isNotEmpty() &&
            (base.last() == 's' || base.last() == 'S' || base.last() == 'x' || base.last() == 'X')
        ) {
            base.dropLast(1) + base.last().uppercaseChar()
        } else {
            base
        }
        return "$normalized.csv"
    }

    private fun normalizeMp3Name(name: String): String =
        name.substringBeforeLast('.') + ".mp3"

    private fun readUri(uri: Uri): ByteArray? =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
}
