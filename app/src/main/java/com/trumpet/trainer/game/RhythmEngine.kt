package com.trumpet.trainer.game

import com.trumpet.trainer.core.model.FingeringChart
import com.trumpet.trainer.core.model.Valve
import kotlin.math.abs
import kotlin.math.max

/**
 * Движок ритм-режима: чистая логика без Android-зависимостей,
 * управляется вызовами [advance] (из игрового цикла) и [onValves] (ввод).
 */
class RhythmEngine(
    val bpm: Int,
    val notes: List<Int>,
    val startTimeMs: Long,
    val hitWindowMs: Long = 320,
    val beatsPerNote: Double = 1.0,
) {
    enum class NoteState { PENDING, HIT, MISSED }

    class RhythmNote(val midi: Int, val targetMs: Long) {
        var state: NoteState = NoteState.PENDING
            internal set
    }

    val beatMs: Long = (60_000.0 / bpm).toLong()
    val rhythmNotes: List<RhythmNote> = notes.mapIndexed { i, midi ->
        RhythmNote(midi, startTimeMs + (i * beatsPerNote * 60_000.0 / bpm).toLong())
    }

    var nowMs: Long = 0L
        private set

    var score = 0
        private set
    var combo = 0
        private set
    var bestCombo = 0
        private set
    var hits = 0
        private set
    var misses = 0
        private set

    val finished: Boolean get() = rhythmNotes.all { it.state != NoteState.PENDING }

    fun advance(dtMs: Long) {
        nowMs += dtMs
        for (n in rhythmNotes) {
            if (n.state == NoteState.PENDING && nowMs > n.targetMs + hitWindowMs) {
                n.state = NoteState.MISSED
                misses++
                combo = 0
            }
        }
    }

    /**
     * Текущее состояние клапанов. Если в окне попадания есть нота,
     * для которой это верная аппликатура — засчитываем попадание.
     * Возврат — задетая нота (null — промах/не в окне).
     */
    fun onValves(pressed: Set<Valve>): RhythmNote? {
        val note = rhythmNotes.firstOrNull {
            it.state == NoteState.PENDING &&
                abs(nowMs - it.targetMs) <= hitWindowMs &&
                FingeringChart.accepts(it.midi, pressed)
        } ?: return null

        note.state = NoteState.HIT
        hits++
        combo++
        bestCombo = max(bestCombo, combo)
        score += 100 + 20 * (combo - 1)
        return note
    }

    /** Индекс ближайшей следующей доли (для метронома), отсчёт от 0. */
    fun beatIndexAt(timeMs: Long): Int = (timeMs / beatMs).toInt()

    /** Осталось долей до первой ноты (счёт-ин), 0 — ноты уже пошли. */
    fun countdownBeats(): Int {
        val remain = (startTimeMs - nowMs + beatMs - 1) / beatMs
        return remain.coerceAtLeast(0).toInt()
    }
}
