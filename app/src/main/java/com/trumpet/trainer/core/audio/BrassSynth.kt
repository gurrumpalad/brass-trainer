package com.trumpet.trainer.core.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Простейший аддитивный синтез «трубы» для эталонных звуков тренажёра.
 * Работает целиком в JVM — легко покрывается юнит-тестами.
 *
 * В продакшене заменяется на сэмплы (SoundPool) или FluidSynth + SoundFont,
 * интерфейс при этом не меняется.
 */
object BrassSynth {

    const val SAMPLE_RATE = 44100

    /** Амплитуды гармоник, характерные для медных духовых. */
    private val HARMONICS = doubleArrayOf(
        1.00, 0.92, 0.80, 0.62, 0.48, 0.36, 0.27, 0.20, 0.14, 0.10, 0.07, 0.05,
    )

    private const val TWO_PI = 2.0 * PI

    fun midiToFreq(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    /** Концертная высота для записанной ноты трубы in B♭ (на тон ниже). */
    fun concertFreqForWritten(midi: Int): Double = midiToFreq(midi - 2)

    /** PCM 16 bit little-endian, моно, [SAMPLE_RATE]. */
    fun notePcm(midi: Int, seconds: Double = 0.65, peak: Float = 0.85f): ByteArray {
        val n = (seconds * SAMPLE_RATE).toInt()
        val out = ByteArray(n * 2)
        val freq = midiToFreq(midi)
        val phaseStep = TWO_PI * freq / SAMPLE_RATE
        var phase = 0.0
        val gain = tanh(2.0) // нормировка софт-клиппинга
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE

            // огибающая: атака 30 мс, спад к 0.8, релиз 180 мс
            val attack = min(1.0, t / 0.030)
            val body = 0.8 + 0.2 * exp(-t / 0.14)
            val relStart = seconds - 0.18
            val release = if (t > relStart) min(1.0, (seconds - t) / 0.18) else 1.0
            val env = attack * body * release

            // лёгкое вибрато после 120 мс
            val vib = if (t > 0.12) 1.0 + 0.004 * sin(TWO_PI * 5.5 * (t - 0.12)) else 1.0

            var s = 0.0
            for (k in HARMONICS.indices) {
                s += HARMONICS[k] * sin(phase * (k + 1) * vib)
            }
            val clipped = tanh(1.6 * s / 2.0) / gain
            val value = (clipped * env * peak).coerceIn(-1.0, 1.0)

            val pcm16 = (value * 32767.0).toInt()
            out[i * 2] = (pcm16 and 0xFF).toByte()
            out[i * 2 + 1] = ((pcm16 shr 8) and 0xFF).toByte()
            phase += phaseStep
            if (phase >= TWO_PI) phase -= TWO_PI
        }
        return out
    }

    /** Клик метронома. accent — сильная доля (звонче и выше). */
    fun clickPcm(accent: Boolean): ByteArray {
        val seconds = 0.06
        val n = (seconds * SAMPLE_RATE).toInt()
        val out = ByteArray(n * 2)
        val f = if (accent) 1800.0 else 1200.0
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t / 0.012)
            val value = (sin(TWO_PI * f * t) * env * 0.8).coerceIn(-1.0, 1.0)
            val pcm16 = (value * 32767.0).toInt()
            out[i * 2] = (pcm16 and 0xFF).toByte()
            out[i * 2 + 1] = ((pcm16 shr 8) and 0xFF).toByte()
        }
        return out
    }

    /** Короткий «звук» ошибки. */
    fun buzzPcm(): ByteArray {
        val seconds = 0.28
        val n = (seconds * SAMPLE_RATE).toInt()
        val out = ByteArray(n * 2)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = if (t < 0.01) t / 0.01 else exp(-(t - 0.01) / 0.09)
            val raw = if (sin(TWO_PI * 150.0 * t) >= 0) 1.0 else -0.7
            val value = (raw * env * 0.5).coerceIn(-1.0, 1.0)
            val pcm16 = (value * 32767.0).toInt()
            out[i * 2] = (pcm16 and 0xFF).toByte()
            out[i * 2 + 1] = ((pcm16 shr 8) and 0xFF).toByte()
        }
        return out
    }
}
