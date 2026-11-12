package com.trumpet.trainer.core.audio

import com.trumpet.trainer.core.pitch.MpmPitchDetector
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrassSynthTest {

    private fun toFloats(pcm: ByteArray): FloatArray = FloatArray(pcm.size / 2) { i ->
        val lo = pcm[i * 2].toInt() and 0xFF
        val hi = pcm[i * 2 + 1].toInt() // знаковый старший байт
        ((hi shl 8) or lo).toShort().toInt() / 32768f
    }

    @Test
    fun notePcmSizeMatchesDuration() {
        val pcm = BrassSynth.notePcm(60, seconds = 0.65)
        assertEquals((BrassSynth.SAMPLE_RATE * 0.65).toInt() * 2, pcm.size)
    }

    @Test
    fun notePcmWithinRangeAndAudible() {
        val floats = toFloats(BrassSynth.notePcm(67))
        assertTrue(floats.all { it in -1f..1f })
        assertTrue("слишком тихо", floats.any { abs(it) > 0.4f })
    }

    @Test
    fun dominantFrequencyMatchesConcertFreq() {
        listOf(62, 69, 74).forEach { midi ->
            val expected = BrassSynth.concertFreqForWritten(midi)
            val floats = toFloats(BrassSynth.notePcm(midi))
            // устойчивая середина: без атаки и релиза
            val from = BrassSynth.SAMPLE_RATE / 4
            val window = floats.copyOfRange(from, from + 2048)
            val r = MpmPitchDetector.detect(window, BrassSynth.SAMPLE_RATE)
            assertTrue("высота не определилась для midi=$midi", r != null)
            assertEquals("midi=$midi", expected, r!!.frequencyHz.toDouble(), 5.0)
        }
    }

    @Test
    fun concertFreqForWrittenNote() {
        // записанная D4 на трубе in B♭ звучит как концертная C4
        assertEquals(BrassSynth.midiToFreq(60), BrassSynth.concertFreqForWritten(62), 1e-6)
    }

    @Test
    fun clicksAndBuzzNotEmpty() {
        assertTrue(BrassSynth.clickPcm(accent = true).isNotEmpty())
        assertTrue(BrassSynth.clickPcm(accent = false).isNotEmpty())
        assertTrue(BrassSynth.buzzPcm().isNotEmpty())
    }
}
