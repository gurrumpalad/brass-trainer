package com.trumpet.trainer.game

import com.trumpet.trainer.core.stats.NoteStat
import com.trumpet.trainer.core.stats.StatsData
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotePickerTest {

    @Test
    fun pickAlwaysInRange() {
        val stats = StatsData()
        repeat(500) { i ->
            val midi = NotePicker.pick(54..84, stats, random = Random(i.toLong()))
            assertTrue("вылезло из диапазона: $midi", midi in 54..84)
        }
    }

    @Test
    fun pickNearStaysInLocalWindow() {
        val stats = StatsData()
        repeat(500) { i ->
            val midi = NotePicker.pickNear(64, 60..72, stats, random = Random(i.toLong()))
            assertTrue("вылезло из окна: $midi", midi in 60..71)
        }
    }

    @Test
    fun pickNearClampsToRangeStart() {
        val stats = StatsData()
        repeat(200) { i ->
            val midi = NotePicker.pickNear(60, 60..72, stats, random = Random(i.toLong()))
            assertTrue("вылезло ниже диапазона: $midi", midi in 60..67)
        }
    }

    @Test
    fun hotNotesArePickedMuchMoreOften() {
        // нота 60 «разогрета» ошибками: вес 50 против 1
        val stats = StatsData(notes = mutableMapOf("60" to NoteStat(heat = 50.0)))
        var hot = 0
        repeat(1000) { i ->
            if (NotePicker.pick(60..61, stats, random = Random(i.toLong())) == 60) hot++
        }
        assertTrue("горячая нота выбирается слишком редко: $hot из 1000", hot > 900)
    }

    @Test
    fun excludeReducesWeight() {
        val stats = StatsData()
        val weights = NotePicker.weights(60..64, stats, exclude = 62)
        assertEquals(0.25, weights[2].second, 1e-9)
    }

    @Test
    fun weightsFollowHeat() {
        val stats = StatsData(
            notes = mutableMapOf(
                "60" to NoteStat(heat = 7.5),
                "61" to NoteStat(heat = 0.5),
            ),
        )
        val weights = NotePicker.weights(60..62, stats)
        assertEquals(7.5, weights[0].second, 1e-9)
        assertEquals(0.5, weights[1].second, 1e-9)
        assertEquals(1.0, weights[2].second, 1e-9)
    }
}
