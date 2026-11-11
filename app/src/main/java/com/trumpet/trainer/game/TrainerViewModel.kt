package com.trumpet.trainer.game

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trumpet.trainer.AppContainer
import com.trumpet.trainer.core.model.Fingering
import com.trumpet.trainer.core.model.FingeringChart
import com.trumpet.trainer.core.model.Valve
import com.trumpet.trainer.core.model.WrittenNote
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GameMode { SIGHT_READING, EAR_TRAINING }

sealed interface Answer {
    data class Correct(val usedAlternate: Boolean, val fingering: Fingering) : Answer
    data class Wrong(val correct: List<Fingering>, val chosen: Set<Valve>) : Answer
}

data class TrainerUiState(
    val mode: GameMode,
    val levelTitle: String = "",
    val note: WrittenNote? = null,
    val pressed: Set<Valve> = emptySet(),
    val answer: Answer? = null,
    val score: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val attempts: Int = 0,
    val correctCount: Int = 0,
) {
    val accuracy: Int get() = if (attempts == 0) 0 else correctCount * 100 / attempts
}

/**
 * Общая логика режимов «нота -> аппликатура»:
 * чтение с листа и на слух.
 *
 * Проверка ответа срабатывает в момент нажатия:
 *  - точное совпадение с допустимой аппликатурой -> верно;
 *  - нажатое не является подмножеством никакой допустимой -> ошибка;
 *  - иначе ждём остальные клапаны (игрок ещё не договорил).
 */
class TrainerViewModel(
    private val mode: GameMode,
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(
        TrainerUiState(mode, levelTitle = container.settings.level.title)
    )
    val state: StateFlow<TrainerUiState> = _state.asStateFlow()

    private var noteShownAt = 0L
    private var awaitingNext = false

    init {
        nextNote(playSound = mode == GameMode.EAR_TRAINING)
    }

    fun onValvesChanged(pressed: Set<Valve>) {
        val s = _state.value
        _state.update { it.copy(pressed = pressed) }
        if (awaitingNext || s.note == null) return

        val midi = s.note.midi
        val valid = FingeringChart.fingerings(midi)

        val exact = valid.firstOrNull { it.valves == pressed }
        when {
            exact != null -> accept(exact)
            pressed.isNotEmpty() && valid.none { pressed.all { v -> v in it.valves } } ->
                reject(pressed, valid)
        }
    }

    /** Явный ответ «открытая труба» (0 клапанов). */
    fun onOpenPressed() {
        val s = _state.value
        _state.update { it.copy(pressed = emptySet()) }
        if (awaitingNext || s.note == null) return

        val midi = s.note.midi
        val valid = FingeringChart.fingerings(midi)
        val exact = valid.firstOrNull { it.valves.isEmpty() }

        if (exact != null) {
            accept(exact)
        } else {
            reject(emptySet(), valid)
        }
    }

    fun replaySound() {
        val note = _state.value.note ?: return
        container.audio.playNote(note.midi)
    }

    fun skip() {
        if (awaitingNext) return
        awaitingNext = true
        viewModelScope.launch {
            delay(250)
            awaitingNext = false
            nextNote(playSound = mode == GameMode.EAR_TRAINING)
        }
    }

    private fun accept(fingering: Fingering) {
        val s = _state.value
        val note = s.note ?: return
        val responseMs = SystemClock.elapsedRealtime() - noteShownAt
        container.stats.record(note.midi, correct = true, responseMs = responseMs)
        container.audio.playNote(note.midi, 0.75f)

        val newCombo = s.combo + 1
        _state.update {
            it.copy(
                answer = Answer.Correct(fingering.isAlternate, fingering),
                score = it.score + 100 + 20 * it.combo,
                combo = newCombo,
                bestCombo = maxOf(it.bestCombo, newCombo),
                attempts = it.attempts + 1,
                correctCount = it.correctCount + 1,
            )
        }
        container.stats.recordBestCombo(newCombo)
        scheduleNext()
    }

    private fun reject(chosen: Set<Valve>, correct: List<Fingering>) {
        val s = _state.value
        val note = s.note ?: return
        val responseMs = SystemClock.elapsedRealtime() - noteShownAt
        container.stats.record(note.midi, correct = false, responseMs = responseMs)
        container.audio.playBuzz()

        _state.update {
            it.copy(
                answer = Answer.Wrong(correct, chosen),
                combo = 0,
                attempts = it.attempts + 1,
            )
        }
        scheduleNext(delayMs = 2800)
    }

    private fun scheduleNext(delayMs: Long = 950) {
        awaitingNext = true
        viewModelScope.launch {
            delay(delayMs)
            awaitingNext = false
            nextNote(playSound = mode == GameMode.EAR_TRAINING)
        }
    }

    private fun nextNote(playSound: Boolean) {
        val level = container.settings.level
        val prev = _state.value.note?.midi
        val midi = NotePicker.pick(level.range, container.stats.data, exclude = prev)
        val note = FingeringChart.noteByMidi(midi)
        noteShownAt = SystemClock.elapsedRealtime()
        _state.update {
            it.copy(
                note = note,
                pressed = emptySet(),
                answer = null,
                levelTitle = level.title,
            )
        }
        if (playSound) {
            viewModelScope.launch {
                delay(350)
                container.audio.playNote(midi)
            }
        }
    }
}
