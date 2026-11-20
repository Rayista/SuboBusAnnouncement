package org.rayista.subobus.data

import java.nio.charset.Charset

/**
 * 极简 CSV 读取：支持引号包裹和 "" 转义，够用即可。
 */
fun parseCsv(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuote = false
    var i = 0

    while (i < text.length) {
        val c = text[i]
        when {
            inQuote -> {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuote = false
                    }
                } else {
                    field.append(c)
                }
            }

            c == '"' -> inQuote = true

            c == ',' -> {
                row.add(field.toString().trim())
                field.setLength(0)
            }

            c == '\n' -> {
                row.add(field.toString().trim())
                if (row.any { it.isNotEmpty() }) rows.add(row.toList())
                row.clear()
                field.setLength(0)
            }

            c == '\r' -> Unit

            else -> field.append(c)
        }
        i++
    }

    row.add(field.toString().trim())
    if (row.any { it.isNotEmpty() }) rows.add(row.toList())
    return rows
}

/**
 * 优先按 UTF-8 读取；不是 UTF-8 就按 GB18030 读取。
 * 这样导入旧编码的 CSV 也不会乱码，重新写回时统一是 UTF-8。
 */
fun decodeCsv(bytes: ByteArray): String {
    // UTF-16 带 BOM 的情况
    if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
        return String(bytes.copyOfRange(2, bytes.size), Charsets.UTF_16LE)
    }
    if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
        return String(bytes.copyOfRange(2, bytes.size), Charsets.UTF_16BE)
    }

    val clean = if (bytes.size >= 3 &&
        bytes[0] == 0xEF.toByte() &&
        bytes[1] == 0xBB.toByte() &&
        bytes[2] == 0xBF.toByte()
    ) {
        bytes.copyOfRange(3, bytes.size)
    } else {
        bytes
    }

    return try {
        val text = String(clean, Charsets.UTF_8)
        if (text.indexOf('\uFFFD') >= 0) throw IllegalArgumentException("not utf8")
        text
    } catch (_: Exception) {
        String(clean, Charset.forName("GB18030"))
    }
}

fun csvToUtf8(bytes: ByteArray): ByteArray = decodeCsv(bytes).encodeToByteArray()
