package com.trumpet.trainer.core.stats

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsRepositoryTest {

    private fun tempFile(): File = File.createTempFile("stats", ".json").apply { deleteOnExit() }

    @Test
    fun recordUpdatesHeatAndCounts() {
        val repo = StatsRepository(tempFile())
        repo.record(60, correct = false, responseMs = 500)
        val afterWrong = repo.statFor(60)
        assertEquals(1, afterWrong.attempts)
        assertEquals(3.0, afterWrong.heat, 1e-9) // 1.0 + 2 за ошибку

        repo.record(60, correct = true, responseMs = 300)
        val afterRight = repo.statFor(60)
        assertEquals(1, afterRight.correct)
        assertEquals(3.0 * 0.55, afterRight.heat, 1e-9) // остыли
        assertEquals(2, repo.data.totalAnswers)
    }

    @Test
    fun heatIsClamped() {
        val repo = StatsRepository(tempFile())
        repeat(10) { repo.record(61, correct = false, responseMs = 0) }
        assertEquals(12.0, repo.statFor(61).heat, 1e-9) // максимум нагрева

        repeat(10) { repo.record(61, correct = true, responseMs = 0) }
        assertEquals(0.5, repo.statFor(61).heat, 1e-9) // минимум
    }

    @Test
    fun avgResponseAggregated() {
        val repo = StatsRepository(tempFile())
        repo.record(62, correct = true, responseMs = 100)
        repo.record(62, correct = true, responseMs = 300)
        assertEquals(200L, repo.statFor(62).avgResponseMs)
    }

    @Test
    fun bestComboOnlyGrows() {
        val repo = StatsRepository(tempFile())
        repo.recordBestCombo(7)
        repo.recordBestCombo(3)
        assertEquals(7, repo.data.bestCombo)
    }

    @Test
    fun roundtripThroughFile() {
        val file = tempFile()
        val repo = StatsRepository(file)
        repo.record(60, correct = true, responseMs = 420)
        repo.record(60, correct = false, responseMs = 700)
        repo.recordBestCombo(5)
        repo.saveNow()

        val loaded = StatsRepository(file).apply { load() }
        val stat = loaded.data.notes.getValue("60")
        assertEquals(2, stat.attempts)
        assertEquals(1, stat.correct)
        assertEquals(5, loaded.data.bestCombo)
        assertEquals(2, loaded.data.totalAnswers)
    }

    @Test
    fun resetClearsEverything() {
        val repo = StatsRepository(tempFile())
        repo.record(60, correct = true, responseMs = 100)
        repo.recordBestCombo(9)
        repo.reset()

        assertEquals(0, repo.data.totalAnswers)
        assertEquals(0, repo.data.bestCombo)
        assertTrue(repo.data.notes.isEmpty())
    }
}
