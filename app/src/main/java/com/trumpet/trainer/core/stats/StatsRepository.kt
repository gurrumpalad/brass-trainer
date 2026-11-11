package com.trumpet.trainer.core.stats

import java.io.File
import java.util.concurrent.Executors
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Статистика по одной ноте. */
@Serializable
data class NoteStat(
    var attempts: Int = 0,
    var correct: Int = 0,
    var totalResponseMs: Long = 0,
    var heat: Double = 1.0,
) {
    val wrong: Int get() = attempts - correct
    val accuracyPercent: Int get() = if (attempts == 0) 0 else correct * 100 / attempts
    val avgResponseMs: Long get() = if (attempts == 0) 0 else totalResponseMs / attempts
}

@Serializable
data class StatsData(
    val notes: MutableMap<String, NoteStat> = mutableMapOf(),
    var bestCombo: Int = 0,
    var totalAnswers: Int = 0,
)

/**
 * Хранение статистики в JSON-файле (filesDir/stats.json).
 *
 * «Нагрев» (heat) — вес ноты при выборе следующего вопроса: ошибка
 * разогревает ноту (+2), правильный ответ остужает (×0.55, минимум 0.5).
 * Так приложение чаще спрашивает то, что «плавится» — упрощённый аналог
 * интервального повторения (SM-2). В продакшене путь миграции — Room.
 */
class StatsRepository(private val file: File) {

    private val json = Json { ignoreUnknownKeys = true }
    private val io = Executors.newSingleThreadExecutor()

    @Volatile
    var data: StatsData = StatsData()
        private set

    fun load() {
        data = runCatching { json.decodeFromString<StatsData>(file.readText()) }
            .getOrDefault(StatsData())
    }

    fun record(noteMidi: Int, correct: Boolean, responseMs: Long) {
        val key = noteMidi.toString()
        val stat = data.notes.getOrPut(key) { NoteStat() }
        stat.attempts++
        stat.totalResponseMs += responseMs
        data.totalAnswers++
        if (correct) {
            stat.correct++
            stat.heat = (stat.heat * 0.55).coerceAtLeast(0.5)
        } else {
            stat.heat = (stat.heat + 2.0).coerceAtMost(12.0)
        }
        saveAsync()
    }

    fun recordBestCombo(combo: Int) {
        if (combo > data.bestCombo) {
            data.bestCombo = combo
            saveAsync()
        }
    }

    fun statFor(noteMidi: Int): NoteStat = data.notes.getOrPut(noteMidi.toString()) { NoteStat() }

    private fun saveAsync() {
        io.execute { saveNow() }
    }

    /** Синхронное сохранение (используется в тестах). */
    fun saveNow() {
        val snapshot = data
        runCatching {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(StatsData.serializer(), snapshot))
            if (!tmp.renameTo(file)) {
                file.writeText(json.encodeToString(StatsData.serializer(), snapshot))
                tmp.delete()
            }
        }
    }

    /** Полный сброс статистики. */
    fun reset() {
        data = StatsData()
        saveAsync()
    }
}
