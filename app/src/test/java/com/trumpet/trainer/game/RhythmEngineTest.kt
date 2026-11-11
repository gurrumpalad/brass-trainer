package com.trumpet.trainer.game

import com.trumpet.trainer.core.model.Valve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RhythmEngineTest {

    private fun engine(notes: List<Int> = listOf(60, 62, 64), bpm: Int = 120) =
        RhythmEngine(bpm = bpm, notes = notes, startTimeMs = 2000)

    @Test
    fun targetsAreEvenlySpaced() {
        val e = engine()
        assertEquals(listOf(2000L, 2500L, 3000L), e.rhythmNotes.map { it.targetMs })
        assertEquals(500L, e.beatMs)
    }

    @Test
    fun hitInsideWindow() {
        val e = engine()
        e.advance(2000) // ровно на первой ноте
        val hit = e.onValves(emptySet()) // C4 — открытая труба
        assertNotNull(hit)
        assertEquals(1, e.hits)
        assertEquals(100, e.score)
        assertEquals(1, e.combo)
    }

    @Test
    fun wrongFingeringDoesNotHit() {
        val e = engine()
        e.advance(2000)
        assertNull(e.onValves(setOf(Valve.FIRST))) // 1-й клапан — это не C4
        assertEquals(0, e.hits)
    }

    @Test
    fun tooEarlyNoHit() {
        val e = engine()
        e.advance(1600) // на 400 мс раньше цели, окно ±320
        assertNull(e.onValves(emptySet()))
    }

    @Test
    fun lateEdgeOfWindowStillHits() {
        val e = engine()
        e.advance(2320) // ровно край окна
        assertNotNull(e.onValves(emptySet()))
    }

    @Test
    fun noteMissedAfterWindow() {
        val e = engine()
        e.advance(2421) // за окном первой ноты
        assertEquals(RhythmEngine.NoteState.MISSED, e.rhythmNotes[0].state)
        assertEquals(1, e.misses)
        assertEquals(0, e.combo)
        // пропущенную уже не взять
        assertNull(e.onValves(emptySet()))
    }

    @Test
    fun comboAndScoreGrow() {
        val e = engine()
        e.advance(2000)
        e.onValves(emptySet())                                     // C4 (60) — открыто, комбо 1
        e.advance(500)
        e.onValves(setOf(Valve.FIRST, Valve.THIRD))                // D4 (62) — 1+3, комбо 2
        e.advance(500)
        e.onValves(setOf(Valve.FIRST, Valve.SECOND))               // E4 (64) — 1+2, комбо 3
        assertEquals(3, e.hits)
        assertEquals(100 + 120 + 140, e.score)
        assertEquals(3, e.bestCombo)
        assertTrue(e.finished)
    }

    @Test
    fun missedNoteBreaksCombo() {
        val e = engine(notes = listOf(60, 67))
        e.advance(2000)
        e.onValves(emptySet()) // взяли C4, комбо 1
        e.advance(700)         // 2700: G4 (цель 2500) уже за окном (2820? нет — окно 2500+320=2820)
        e.advance(200)         // 2900 > 2820 — G4 пропущена
        assertEquals(RhythmEngine.NoteState.MISSED, e.rhythmNotes[1].state)
        assertEquals(0, e.combo)
        assertTrue(e.finished)
    }

    @Test
    fun countdownDuringPrecount() {
        val e = engine()
        e.advance(0)
        assertEquals(4, e.countdownBeats())
        e.advance(1500)
        assertEquals(1, e.countdownBeats())
        e.advance(1000) // ноты уже пошли
        assertEquals(0, e.countdownBeats())
    }

    @Test
    fun beatIndexFollowsBeats() {
        val e = engine()
        assertEquals(0, e.beatIndexAt(0))
        assertEquals(0, e.beatIndexAt(499))
        assertEquals(1, e.beatIndexAt(500))
        assertEquals(4, e.beatIndexAt(2000))
    }
}
