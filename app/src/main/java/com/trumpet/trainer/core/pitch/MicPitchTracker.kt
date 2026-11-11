package com.trumpet.trainer.core.pitch

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * Чтение микрофона и устойчивое определение звучащей ноты.
 *
 * Кадр ~1024 семпла анализируется [MpmPitchDetector]-ом; чтобы не «дрожать»,
 * показываем частоту только после 4 подряд стабильных кадров (медиана,
 * разброс < 3%). Калбэк вызывается из фонового потока.
 */
class MicPitchTracker(
    private val sampleRate: Int = 44100,
    private val clarityThreshold: Float = 0.78f,
    private val onReading: (Reading?) -> Unit,
) {
    /** Устойчивое чтение: частота, ясность и концертная нота (midi может быть дробной). */
    data class Reading(
        val frequencyHz: Float,
        val clarity: Float,
        val concertMidi: Float,
    )

    @Volatile
    private var running = false
    private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread {
            runLoop()
        }.apply {
            name = "mic-pitch-tracker"
            priority = Thread.MAX_PRIORITY - 1
            start()
        }
    }

    fun stop() {
        running = false
        thread?.join(800)
        thread = null
    }

    @SuppressLint("MissingPermission") // вызывающий обязан проверить RECORD_AUDIO
    private fun runLoop() {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val record = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf * 2, 16384))
            .build()

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            running = false
            onReading(null)
            return
        }

        val window = 1024
        val shorts = ShortArray(window)
        val floats = FloatArray(window)
        val history = FloatArray(4)
        var historyCount = 0

        record.startRecording()
        try {
            while (running) {
                val n = record.read(shorts, 0, window)
                if (n <= 0) continue
                for (i in 0 until n) floats[i] = shorts[i] / 32768f

                val r = MpmPitchDetector.detect(floats.copyOf(n), sampleRate)
                if (r != null && r.clarity >= clarityThreshold) {
                    history[historyCount % history.size] = r.frequencyHz
                    historyCount++
                    if (historyCount >= history.size) {
                        val sorted = history.sorted()
                        val median = (sorted[1] + sorted[2]) / 2f
                        val spread = abs(sorted.last() - sorted.first()) / median
                        if (spread < 0.03f) {
                            val midi = 69f + (12f * log2(median / 440f))
                            onReading(Reading(median, r.clarity, midi))
                        } else {
                            onReading(null)
                        }
                    }
                } else {
                    historyCount = 0
                    onReading(null)
                }
            }
        } finally {
            runCatching {
                record.stop()
                record.release()
            }
        }
    }
}

/** Концертная частота -> ближайшая нота в полутонах (концертный строй). */
fun freqToConcertMidi(freq: Float): Int = (69 + 12 * log2(freq / 440f)).roundToInt()
