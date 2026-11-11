package com.trumpet.trainer.game

import com.trumpet.trainer.core.stats.StatsData
import kotlin.random.Random

/**
 * Взвешенный выбор следующей ноты: чаще спрашиваем «горячие» ноты
 * (по которым были ошибки), избегаем немедленного повтора.
 */
object NotePicker {

    /** Веса нот диапазона по данным статистики. */
    fun weights(range: IntRange, stats: StatsData, exclude: Int? = null): List<Pair<Int, Double>> =
        range.map { midi ->
            var w = stats.notes[midi.toString()]?.heat ?: 1.0
            if (midi == exclude) w *= 0.25
            midi to w
        }

    fun pick(
        range: IntRange,
        stats: StatsData,
        exclude: Int? = null,
        random: Random = Random.Default,
    ): Int {
        val w = weights(range, stats, exclude)
        val total = w.sumOf { it.second }
        var r = random.nextDouble() * total
        for ((midi, weight) in w) {
            r -= weight
            if (r <= 0) return midi
        }
        return w.last().first
    }

    /** Локальное окно вокруг предыдущей ноты — для плавных «мелодий» в ритм-режиме. */
    fun pickNear(
        previous: Int,
        range: IntRange,
        stats: StatsData,
        maxJump: Int = 7,
        random: Random = Random.Default,
    ): Int {
        val lo = maxOf(previous - maxJump, range.first)
        val hi = minOf(previous + maxJump, range.last)
        if (lo > hi) return pick(range, stats, random = random)
        return pick(lo..hi, stats, random = random)
    }
}
