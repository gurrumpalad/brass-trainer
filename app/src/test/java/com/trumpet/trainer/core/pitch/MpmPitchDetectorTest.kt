package com.trumpet.trainer.core.pitch

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MpmPitchDetectorTest {

    private fun sine(freq: Double, n: Int = 2048, sr: Int = 44100): FloatArray =
        FloatArray(n) { i -> sin(2 * PI * freq * i / sr).toFloat() }

    @Test
    fun detects440() {
        val r = MpmPitchDetector.detect(sine(440.0), 44100)!!
        assertEquals(440f, r.frequencyHz, 2f)
        assertTrue("clarity=${r.clarity}", r.clarity > 0.9f)
    }

    @Test
    fun detectsConcertBb3() {
        // концертный B♭3 — низ области трубы
        val r = MpmPitchDetector.detect(sine(233.08), 44100)!!
        assertEquals(233.08f, r.frequencyHz, 2f)
    }

    @Test
    fun detectsConcertF5() {
        val r = MpmPitchDetector.detect(sine(698.46), 44100)!!
        assertEquals(698.46f, r.frequencyHz, 3f)
    }

    @Test
    fun silenceIsNull() {
        assertNull(MpmPitchDetector.detect(FloatArray(2048), 44100))
    }

    @Test
    fun tooShortIsNull() {
        assertNull(MpmPitchDetector.detect(FloatArray(32), 44100))
    }

    @Test
    fun belowRangeIsNull() {
        // 80 Гц ниже окна поиска (150 Гц): периода нет в диапазоне задержек
        assertNull(MpmPitchDetector.detect(sine(80.0), 44100))
    }
}
