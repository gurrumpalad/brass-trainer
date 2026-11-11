package com.trumpet.trainer.core.pitch

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Определение высоты тона методом Маклеода (MPM, «A Smarter Way to Find Pitch»).
 *
 * Чистая JVM-математика — покрывается юнит-тестами. Труба монофонична,
 * поэтому метод работает надёжно; вычисление NSDF прямое (без FFT),
 * для окна ~1024 семпла этого достаточно на 40–80 кадрах в секунду.
 */
object MpmPitchDetector {

    data class Result(val frequencyHz: Float, val clarity: Float)

    /**
     * @param samples окно сигнала, нормированное примерно к [-1, 1]
     * @return оценка частоты и «ясность» (0..1, ~1 — уверенный периодический сигнал),
     *         либо null, если высота не определяется.
     */
    fun detect(
        samples: FloatArray,
        sampleRate: Int,
        minFreqHz: Float = 150f,
        maxFreqHz: Float = 1400f,
    ): Result? {
        if (samples.size < 64) return null

        val tauMin = max(2, (sampleRate / maxFreqHz).toInt())
        val tauMax = min(samples.size / 2, (sampleRate / minFreqHz).toInt())
        if (tauMax <= tauMin + 2) return null

        // Нормализованная функция квадратичной разности (NSDF)
        val nsdf = FloatArray(tauMax + 1)
        for (tau in 0..tauMax) {
            var ac = 0.0
            var norm = 0.0
            for (i in 0 until samples.size - tau) {
                val a = samples[i].toDouble()
                val b = samples[i + tau].toDouble()
                ac += a * b
                norm += a * a + b * b
            }
            nsdf[tau] = if (norm > 1e-12) (2.0 * ac / norm).toFloat() else 0f
        }

        // Глобальный максимум NSDF
        var globalMax = 0f
        for (t in tauMin..tauMax) if (nsdf[t] > globalMax) globalMax = nsdf[t]
        if (globalMax < 0.3f) return null

        // Первый «значимый» локальный максимум (>= 0.85 * глобального)
        val threshold = 0.85f * globalMax
        var tau = tauMin
        var chosen = -1
        while (tau < tauMax) {
            val rising = nsdf[tau - 1] < nsdf[tau]
            if (rising && nsdf[tau] >= nsdf[tau + 1] && nsdf[tau] >= threshold) {
                chosen = tau
                break
            }
            tau++
        }
        if (chosen < 0) return null

        // Параболическая интерполяция для субсемпловой точности
        val y0 = nsdf[chosen - 1]
        val y1 = nsdf[chosen]
        val y2 = nsdf[chosen + 1]
        val denom = y0 - 2 * y1 + y2
        val delta = if (abs(denom) > 1e-9) 0.5f * (y0 - y2) / denom else 0f
        val refined = chosen + delta.coerceIn(-1f, 1f)

        val freq = sampleRate / refined
        if (freq < minFreqHz || freq > maxFreqHz) return null

        return Result(freq, y1.coerceIn(0f, 1f))
    }
}
