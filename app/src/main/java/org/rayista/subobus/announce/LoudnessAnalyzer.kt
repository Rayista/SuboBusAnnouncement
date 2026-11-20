package org.rayista.subobus.announce

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * 响度分析：把音频解码成 PCM，算 RMS 响度，再换算成“提到目标响度”的增益。
 * 结果按文件路径+大小+时间缓存，避免每次播放都重新解码。
 */
object LoudnessAnalyzer {

    /** 目标响度（dBFS）。比它小的音频往上提，比它大的不动。 */
    private const val TARGET_DBFS = -14.0

    /** 最大提升 15 dB，避免过度放大导致破音。 */
    private const val MAX_GAIN_DB = 15.0

    private val cache = ConcurrentHashMap<String, Int>()

    /** 返回 LoudnessEnhancer 需要的增益（毫贝，mB）。0 表示不需要处理。 */
    suspend fun gainMb(file: File): Int = withContext(Dispatchers.IO) {
        val key = "${file.absolutePath}|${file.length()}|${file.lastModified()}"
        cache[key] ?: analyze(file).also { cache[key] = it }
    }

    private fun analyze(file: File): Int {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        return try {
            extractor.setDataSource(file.absolutePath)
            var trackIndex = -1
            var foundFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val m = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (m.startsWith("audio/")) {
                    trackIndex = i
                    foundFormat = f
                    break
                }
            }
            if (trackIndex < 0) return 0
            val format = foundFormat ?: return 0
            extractor.selectTrack(trackIndex)

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return 0
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            val codec = MediaCodec.createDecoderByType(mime)
            decoder = codec
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var sumSquares = 0.0
            var sampleCount = 0L
            // 最多分析 30 秒，够判断响度了，也避免长音频卡太久
            val maxSamples = sampleRate.toLong() * channels.coerceAtLeast(1) * 30
            var pcmFloat = false

            while (!outputDone && sampleCount < maxSamples) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val buffer = codec.getInputBuffer(inIndex) ?: continue
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(
                                inIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                when (val outIndex = codec.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val out = codec.outputFormat
                        if (out.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            pcmFloat = out.getInteger(MediaFormat.KEY_PCM_ENCODING) ==
                                AudioFormat.ENCODING_PCM_FLOAT
                        }
                    }

                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit

                    else -> if (outIndex >= 0) {
                        if (info.size > 0 && !pcmFloat) {
                            val buffer = codec.getOutputBuffer(outIndex)
                            if (buffer != null) {
                                buffer.position(info.offset)
                                buffer.limit(info.offset + info.size)
                                val shorts = ShortArray(info.size / 2)
                                buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                                for (s in shorts) {
                                    sumSquares += s.toDouble() * s
                                    sampleCount++
                                    if (sampleCount >= maxSamples) break
                                }
                            }
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            if (sampleCount == 0L) return 0
            val rms = sqrt(sumSquares / sampleCount)
            if (rms <= 1.0) return 0
            val dbfs = 20.0 * log10(rms / 32768.0)
            val gainDb = (TARGET_DBFS - dbfs).coerceIn(0.0, MAX_GAIN_DB)
            (gainDb * 100).toInt()
        } catch (_: Exception) {
            0
        } finally {
            try {
                decoder?.stop()
            } catch (_: Exception) {
            }
            try {
                decoder?.release()
            } catch (_: Exception) {
            }
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }
    }
}
