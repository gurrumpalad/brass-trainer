package com.trumpet.trainer.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FingeringChartTest {

    @Test
    fun everyNoteInRangeHasFingering() {
        for (midi in FingeringChart.MIN_MIDI..FingeringChart.MAX_MIDI) {
            assertTrue("нет аппликатуры для midi=$midi", FingeringChart.fingerings(midi).isNotEmpty())
        }
    }

    @Test
    fun openNotesHaveEmptyFingering() {
        // натуральный ряд трубы: C4, G4, C5, E5, G5, C6 — без клапанов
        listOf(60, 67, 72, 76, 79, 84).forEach { midi ->
            assertEquals("midi=$midi", emptySet<Valve>(), FingeringChart.primary(midi)!!.valves)
        }
    }

    @Test
    fun basicFingerings() {
        assertEquals(setOf(Valve.FIRST, Valve.THIRD), FingeringChart.primary(62)!!.valves)  // D4 (1+3)
        assertEquals(setOf(Valve.SECOND, Valve.THIRD), FingeringChart.primary(63)!!.valves) // E♭4 (2+3)
        assertEquals(setOf(Valve.FIRST, Valve.SECOND), FingeringChart.primary(64)!!.valves) // E4 (1+2)
        assertEquals(
            setOf(Valve.FIRST, Valve.SECOND, Valve.THIRD),
            FingeringChart.primary(61)!!.valves,                                            // C♯4 (1+2+3)
        )
        assertEquals(setOf(Valve.SECOND, Valve.THIRD), FingeringChart.primary(68)!!.valves)  // A♭4
        assertEquals(setOf(Valve.FIRST, Valve.THIRD), FingeringChart.primary(55)!!.valves)   // G3
        assertEquals(
            setOf(Valve.FIRST, Valve.SECOND, Valve.THIRD),
            FingeringChart.primary(54)!!.valves,                                           // F♯3
        )
    }

    @Test
    fun alternativesUseThirdValve() {
        // A3, E4, A4, C♯5, A5: альтернатива 1+2 — это 3-й клапан
        listOf(57, 64, 69, 73, 81).forEach { midi ->
            val fingerings = FingeringChart.fingerings(midi)
            assertTrue("midi=$midi", fingerings.size >= 2)
            assertTrue(fingerings.any { it.isAlternate && it.valves == setOf(Valve.THIRD) })
            assertTrue(fingerings.any { !it.isAlternate && it.valves == setOf(Valve.FIRST, Valve.SECOND) })
        }
    }

    @Test
    fun acceptsExactAndAlternative() {
        assertTrue(FingeringChart.accepts(64, setOf(Valve.FIRST, Valve.SECOND))) // E4
        assertTrue(FingeringChart.accepts(64, setOf(Valve.THIRD)))
        assertFalse(FingeringChart.accepts(64, setOf(Valve.SECOND)))
        assertFalse(FingeringChart.accepts(64, setOf(Valve.FIRST, Valve.THIRD)))
    }

    @Test
    fun partialWhileBuildingFingering() {
        // {1} — начало аппликатуры 1+2 для E4: игрок ещё не договорил
        assertTrue(FingeringChart.isPartial(64, setOf(Valve.FIRST)))
        // {1,3} не подмножество ни 1+2, ни 3 — сразу ошибка
        assertFalse(FingeringChart.isPartial(64, setOf(Valve.FIRST, Valve.THIRD)))
        // пустое множество всегда промежуточное
        assertTrue(FingeringChart.isPartial(64, emptySet()))
    }

    @Test
    fun spellings() {
        assertEquals("C4", FingeringChart.noteByMidi(60).name)
        assertEquals("F♯4", FingeringChart.noteByMidi(66).name)
        assertEquals("G♭4", FingeringChart.noteByMidi(66).altName)
        assertEquals("E♭4", FingeringChart.noteByMidi(63).name)
        assertEquals("D♯4", FingeringChart.noteByMidi(63).altName)
        assertEquals("B♭4", FingeringChart.noteByMidi(70).name)
        assertEquals("C♯5", FingeringChart.noteByMidi(73).name)
    }

    @Test
    fun staffPositions() {
        assertEquals(0, FingeringChart.noteByMidi(64).staffPosition)   // E4 — нижняя линейка
        assertEquals(-2, FingeringChart.noteByMidi(60).staffPosition)  // C4 — добавочная снизу
        assertEquals(8, FingeringChart.noteByMidi(77).staffPosition)   // F5 — верхняя линейка
        assertEquals(9, FingeringChart.noteByMidi(79).staffPosition)   // G5 — промежуток над станом
        assertEquals(10, FingeringChart.noteByMidi(81).staffPosition)  // A5 — добавочная сверху
        assertEquals(-6, FingeringChart.noteByMidi(54).staffPosition)  // F♯3 — глубоко под станом
    }

    @Test
    fun describeFormatting() {
        assertEquals("открыто", Fingering(emptySet()).describe())
        assertEquals("1+3", Fingering(setOf(Valve.THIRD, Valve.FIRST)).describe())
    }
}
