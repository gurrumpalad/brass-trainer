package com.trumpet.trainer.game

import androidx.lifecycle.ViewModel
import com.trumpet.trainer.AppContainer
import com.trumpet.trainer.core.model.Valve
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class StripNote(
    val midi: Int,
    val targetMs: Long,
    val hit: Boolean = false,
    val missed: Boolean = false,
)

data class RhythmUiState(
    val running: Boolean = false,
    val finished: Boolean = false,
    val levelTitle: String = "",
    val bpm: Int = 100,
    val beatMs: Long = 600,
    val notes: List<StripNote> = emptyList(),
    val nowMs: Long = 0,
    val countdown: Int = 0,
    val score: Int = 0,
    val combo: Int = 0,
    val hits: Int = 0,
    val misses: Int = 0,
    val bestCombo: Int = 0,
)

/**
 * Ритм-режим: нотная «бегущая строка», метроном и окно попадания.
 * Игровой цикл двигает время вызовами [tick] из кадровой аннатации UI
 * (withFrameNanos), вся логика — в тестируемом [RhythmEngine].
 */
class RhythmViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(RhythmUiState(bpm = container.settings.bpm))
    val state: StateFlow<RhythmUiState> = _state.asStateFlow()

    private var engine: RhythmEngine? = null
    private var lastBeat = -1

    fun start() {
        val settings = container.settings
        val level = settings.level
        val beatMs = (60_000.0 / settings.bpm).toLong()
        val precount = 4 * beatMs

        // последовательность нот: локальные шаги вокруг предыдущей ноты
        val count = 24
        val sequence = ArrayList<Int>(count)
        var prev: Int? = null
        repeat(count) {
            val midi = if (prev == null) {
                NotePicker.pick(level.range, container.stats.data)
            } else {
                NotePicker.pickNear(prev!!, level.range, container.stats.data)
            }
            sequence.add(midi)
            prev = midi
        }

        engine = RhythmEngine(
            bpm = settings.bpm,
            notes = sequence,
            startTimeMs = precount,
        ).also { it.advance(0) }

        lastBeat = -1
        _state.value = RhythmUiState(
            running = true,
            finished = false,
            levelTitle = level.title,
            bpm = settings.bpm,
            beatMs = beatMs,
            notes = sequence.map { StripNote(it, 0) }, // перезаполнится в publish()
            bestCombo = container.stats.data.bestCombo,
        )
        publish()
    }

    fun stop() {
        engine = null
        _state.update { it.copy(running = false) }
    }

    fun restart() {
        start()
    }

    /** Один кадр: dtMs — прошедшее с прошлого кадра время. */
    fun tick(dtMs: Long) {
        val e = engine ?: return
        e.advance(dtMs)

        // метроном: клик на каждой доле, акцент на первой доле такта (4/4)
        val beat = e.beatIndexAt(e.nowMs)
        if (beat > lastBeat) {
            lastBeat = beat
            container.audio.playClick(accent = beat % 4 == 0)
        }

        publish()

        if (e.finished) {
            finishRound(e)
        }
    }

    fun onValves(pressed: Set<Valve>) {
        val e = engine ?: return
        val hitNote = e.onValves(pressed)
        if (hitNote != null) {
            container.audio.playNote(hitNote.midi, 0.55f)
            publish()
        }
    }

    private fun publish() {
        val e = engine ?: return
        _state.update { s ->
            s.copy(
                running = true,
                nowMs = e.nowMs,
                countdown = e.countdownBeats(),
                notes = e.rhythmNotes.map {
                    StripNote(it.midi, it.targetMs, it.state == RhythmEngine.NoteState.HIT, it.state == RhythmEngine.NoteState.MISSED)
                },
                score = e.score,
                combo = e.combo,
                hits = e.hits,
                misses = e.misses,
                bestCombo = e.bestCombo,
            )
        }
    }

    private fun finishRound(e: RhythmEngine) {
        engine = null
        val beatMs = (60_000.0 / e.bpm).toLong()
        for (n in e.rhythmNotes) {
            container.stats.record(n.midi, correct = n.state == RhythmEngine.NoteState.HIT, responseMs = beatMs)
        }
        container.stats.recordBestCombo(e.bestCombo)
        _state.update {
            it.copy(running = false, finished = true, notes = it.notes)
        }
    }
}
