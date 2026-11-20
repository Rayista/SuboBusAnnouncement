package org.rayista.subobus.announce

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteOrder
import kotlin.math.pow

/**
 * 按顺序播放一个“播报项目”里的所有音频。
 *
 * 这里自己做解码（MediaExtractor + MediaCodec）并用 AudioTrack 输出，
 * 因为“按 audiolist 的内放/外放决定只从左侧或右侧出声”必须自己控制 PCM：
 * - 单声道会复制成双声道，再把不需要的一侧置 0，所以单声道也能只从一侧出声；
 * - 响度均衡也直接乘在 PCM 上，不依赖设备的音效支持。
 *
 * 关键点：每次 [play] 都是一个独立的“会话”。
 * 新会话开始时先把旧会话的声音停掉并作废旧会话编号，
 * 旧会话的协程即使还卡在阻塞的 write() 里，也只能写它自己那个已被释放的 AudioTrack，
 * 绝不会去动新会话的 AudioTrack，也不会继续播后面还没播的音频。
 */
class AudioPlayer(private val loudnessEnabled: () -> Boolean = { false }) {

    private val lock = Any()

    /** 会话编号，每次 play/stop 都 +1；任务只认自己启动时拿到的编号。 */
    private var session = 0

    /** 当前正在出声的 AudioTrack，只可能属于最新的一个会话。 */
    private var activeTrack: AudioTrack? = null

    @Volatile
    private var sessionActive = false

    /** 是否还有一段播报在播（整组音频中的任意一个）。 */
    fun isPlaying(): Boolean = sessionActive

    /**
     * 顺序播放 [cues] 里的全部音频，中途被打断则整组停止。
     * 单个文件解码失败只跳过它，不影响后面的文件。
     */
    suspend fun play(cues: List<AudioCue>) = withContext(Dispatchers.IO) {
        val my = synchronized(lock) {
            session++
            sessionActive = true
            session
        }
        // 先把上一组声音彻底停掉，再开始新的，避免两组叠在一起
        stopActiveTrack()

        try {
            for (cue in cues) {
                if (!isCurrent(my)) break
                try {
                    playCue(cue, my)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // 单个音频有问题：忽略，继续下一个
                }
            }
        } finally {
            synchronized(lock) {
                if (session == my) sessionActive = false
            }
        }
    }

    /** 播放单个音频（仍属于 [my] 会话）。 */
    private suspend fun playCue(cue: AudioCue, my: Int) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var audioTrack: AudioTrack? = null
        try {
            extractor.setDataSource(cue.file.absolutePath)

            var trackIndex = -1
            var foundFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    foundFormat = format
                    break
                }
            }
            if (trackIndex < 0) return
            val format = foundFormat ?: return
            extractor.selectTrack(trackIndex)

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val sourceChannels = format
                .getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                .coerceIn(1, 2)

            val codec = MediaCodec.createDecoderByType(mime)
            decoder = codec
            codec.configure(format, null, null, 0)
            codec.start()

            // 响度均衡：把 mB 增益换成线性倍数，直接乘到采样上
            val gainFactor = if (loudnessEnabled()) {
                val gainMb = LoudnessAnalyzer.gainMb(cue.file)
                10.0.pow(gainMb / 2000.0).toFloat()
            } else {
                1f
            }

            val minBuffer = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = if (minBuffer > 0) minBuffer else sampleRate * 4

            val newTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            // 建轨期间可能已经被新会话取代，那就直接丢掉这条轨
            audioTrack = newTrack
            if (!registerTrack(my, newTrack)) return
            newTrack.play()

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var pcmFloat = false
            var totalFrames = 0L

            while (!outputDone && isCurrent(my)) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val buffer = codec.getInputBuffer(inIndex)
                        if (buffer != null) {
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
                }

                when (val outIndex = codec.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (codec.outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            pcmFloat = codec.outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING) ==
                                AudioFormat.ENCODING_PCM_FLOAT
                        }
                    }

                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit

                    else -> if (outIndex >= 0) {
                        if (info.size > 0 && isCurrent(my)) {
                            val buffer = codec.getOutputBuffer(outIndex)
                            if (buffer != null) {
                                buffer.position(info.offset)
                                buffer.limit(info.offset + info.size)
                                totalFrames += if (pcmFloat) {
                                    writeFloatBuffer(
                                        newTrack, buffer, sourceChannels, cue.channel, gainFactor, my
                                    )
                                } else {
                                    writeShortBuffer(
                                        newTrack, buffer, sourceChannels, cue.channel, gainFactor, my
                                    )
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

            // 等缓冲区里的声音真正播完，避免最后半句被下一句截断。
            // 加个最大等待时间，防止某些设备 playbackHeadPosition 不推进导致卡死。
            if (isCurrent(my) && totalFrames > 0) {
                val timeoutMs = totalFrames * 1000L / sampleRate + 800
                var waited = 0L
                while (newTrack.playbackHeadPosition.toLong() < totalFrames &&
                    isCurrent(my) && waited < timeoutMs
                ) {
                    delay(20)
                    waited += 20
                }
            }
        } finally {
            audioTrack?.let { releaseTrack(it) }
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

    /** 把这条轨登记为当前会话的输出轨；已被新会话取代则返回 false。 */
    private fun registerTrack(my: Int, audioTrack: AudioTrack): Boolean = synchronized(lock) {
        if (session != my) {
            false
        } else {
            activeTrack = audioTrack
            true
        }
    }

    private fun isCurrent(my: Int): Boolean = synchronized(lock) { session == my }

    /** 16-bit PCM：按“内放/外放”把声音写到左或右声道，另一侧置 0。 */
    private fun writeShortBuffer(
        audioTrack: AudioTrack,
        buffer: java.nio.ByteBuffer,
        sourceChannels: Int,
        channel: PlayChannel,
        gain: Float,
        my: Int
    ): Long {
        val shorts = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val source = ShortArray(shorts.remaining())
        shorts.get(source)
        val frames = source.size / sourceChannels
        if (frames <= 0) return 0

        val out = ShortArray(frames * 2)
        var si = 0
        for (frame in 0 until frames) {
            val left = source[si].toFloat()
            val right = if (sourceChannels >= 2) source[si + 1].toFloat() else left
            si += sourceChannels

            val gl = (left * gain).toInt().coerceIn(-32768, 32767).toShort()
            val gr = (right * gain).toInt().coerceIn(-32768, 32767).toShort()

            when (channel) {
                PlayChannel.BOTH -> {
                    out[frame * 2] = gl
                    out[frame * 2 + 1] = gr
                }

                PlayChannel.LEFT -> {
                    out[frame * 2] = gl
                    out[frame * 2 + 1] = 0
                }

                PlayChannel.RIGHT -> {
                    out[frame * 2] = 0
                    out[frame * 2 + 1] = gr
                }
            }
        }
        return writeAll(audioTrack, out, my)
    }

    /** 浮点 PCM：转成 16-bit 后同样按声道输出。 */
    private fun writeFloatBuffer(
        audioTrack: AudioTrack,
        buffer: java.nio.ByteBuffer,
        sourceChannels: Int,
        channel: PlayChannel,
        gain: Float,
        my: Int
    ): Long {
        val floats = buffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        val source = FloatArray(floats.remaining())
        floats.get(source)
        val frames = source.size / sourceChannels
        if (frames <= 0) return 0

        val out = ShortArray(frames * 2)
        var si = 0
        for (frame in 0 until frames) {
            val left = source[si] * 32767f
            val right = if (sourceChannels >= 2) source[si + 1] * 32767f else left
            si += sourceChannels

            val gl = (left * gain).toInt().coerceIn(-32768, 32767).toShort()
            val gr = (right * gain).toInt().coerceIn(-32768, 32767).toShort()

            when (channel) {
                PlayChannel.BOTH -> {
                    out[frame * 2] = gl
                    out[frame * 2 + 1] = gr
                }

                PlayChannel.LEFT -> {
                    out[frame * 2] = gl
                    out[frame * 2 + 1] = 0
                }

                PlayChannel.RIGHT -> {
                    out[frame * 2] = 0
                    out[frame * 2 + 1] = gr
                }
            }
        }
        return writeAll(audioTrack, out, my)
    }

    private fun writeAll(audioTrack: AudioTrack, data: ShortArray, my: Int): Long {
        var offset = 0
        while (offset < data.size && isCurrent(my)) {
            val written = try {
                audioTrack.write(data, offset, data.size - offset)
            } catch (_: Exception) {
                -1
            }
            if (written <= 0) break
            offset += written
        }
        return (data.size / 2).toLong()
    }

    /** 正常收尾：释放一条已经播完（或已作废）的输出轨。 */
    private fun releaseTrack(audioTrack: AudioTrack) {
        synchronized(lock) {
            if (activeTrack === audioTrack) activeTrack = null
        }
        try {
            audioTrack.pause()
        } catch (_: Exception) {
        }
        try {
            audioTrack.flush()
        } catch (_: Exception) {
        }
        try {
            audioTrack.stop()
        } catch (_: Exception) {
        }
        try {
            audioTrack.release()
        } catch (_: Exception) {
        }
    }

    /** 强行停掉当前会话正在出声的那条轨，让阻塞中的 write 立刻返回。 */
    private fun stopActiveTrack() {
        val current = synchronized(lock) {
            val t = activeTrack
            activeTrack = null
            t
        } ?: return
        try {
            current.pause()
        } catch (_: Exception) {
        }
        try {
            current.flush()
        } catch (_: Exception) {
        }
        try {
            current.stop()
        } catch (_: Exception) {
        }
        try {
            current.release()
        } catch (_: Exception) {
        }
    }

    /**
     * 终止整组播报：作废当前会话（后续 cue 不再继续播），并立刻停掉正在出声的音频。
     */
    fun stopCurrent() {
        synchronized(lock) {
            session++
            sessionActive = false
        }
        stopActiveTrack()
    }

    fun release() {
        stopCurrent()
    }
}
