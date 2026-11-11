package com.trumpet.trainer.game

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trumpet.trainer.AppContainer
import com.trumpet.trainer.core.audio.BrassSynth
import com.trumpet.trainer.core.model.FingeringChart
import com.trumpet.trainer.core.model.WrittenNote
import com.trumpet.trainer.core.pitch.MicPitchTracker
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MicReading(
    /** Нота, которую слышит микрофон, в записанном строе (концертная + 2 полутона). */
    val writtenMidi: Int,
    /** Отклонение от цели в центах (если цель задана). */
    val centsOff: Int?,
    val clarity: Float,
    val frequencyHz: Float,
)

data class MicUiState(
    val listening: Boolean = false,
    val target: WrittenNote? = null,
    val reading: MicReading? = null,
    /** 0..1 — как долго держится попадание в цель. */
    val matchProgress: Float = 0f,
    val solvedCount: Int = 0,
    val justSolved: Boolean = false,
    val levelTitle: String = "",
)

/**
 * Режим с микрофоном: играем на настоящей трубе, приложение слушает.
 * Микрофон даёт концертную высоту — сравниваем с целью, переведённой
 * в концертный строй (записанная нота - 2 полутона для трубы in B♭).
 */
class MicViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(MicUiState(levelTitle = container.settings.level.title))
    val state: StateFlow<MicUiState> = _state.asStateFlow()

    private var tracker: MicPitchTracker? = null
    private var matchSince = 0L

    fun startListening() {
        if (_state.value.listening) return
        stopListening()
        tracker = MicPitchTracker { reading -> onReading(reading) }
        tracker?.start()
        _state.update { it.copy(listening = true) }
        if (_state.value.target == null) newTarget(playReference = false)
    }

    fun stopListening() {
        tracker?.stop()
        tracker = null
        matchSince = 0
        _state.update { it.copy(listening = false, reading = null, matchProgress = 0f) }
    }

    fun newTarget(playReference: Boolean = true) {
        val level = container.settings.level
        val midi = NotePicker.pick(level.range, container.stats.data, exclude = _state.value.target?.midi)
        val note = FingeringChart.noteByMidi(midi)
        matchSince = 0
        _state.update { it.copy(target = note, matchProgress = 0f, justSolved = false, levelTitle = level.title) }
        if (playReference) container.audio.playNote(midi, 0.8f)
    }

    fun playTargetReference() {
        _state.value.target?.let { container.audio.playNote(it.midi, 0.8f) }
    }

    private fun onReading(reading: MicPitchTracker.Reading?) {
        if (reading == null) {
            matchSince = 0
            _state.update { it.copy(reading = null, matchProgress = 0f) }
            return
        }

        val writtenMidi = (reading.concertMidi.roundToInt() + 2)
        val target = _state.value.target
        val centsOff = target?.let {
            val expected = BrassSynth.concertFreqForWritten(it.midi)
            (1200 * log2(reading.frequencyHz / expected)).roundToInt()
        }

        val inTune = target != null &&
            writtenMidi == target.midi &&
            centsOff != null &&
            abs(centsOff) <= 35

        if (inTune && _state.value.justSolved) {
            // цель уже сыграна — ждём перевыбор после паузы
            _state.update { it.copy(reading = MicReading(writtenMidi, centsOff, reading.clarity, reading.frequencyHz)) }
            return
        }

        if (inTune) {
            if (matchSince == 0L) matchSince = SystemClock.elapsedRealtime()
            val held = SystemClock.elapsedRealtime() - matchSince
            val progress = (held / 600f).coerceIn(0f, 1f)
            if (progress >= 1f) {
                container.stats.record(target.midi, correct = true, responseMs = held)
                container.audio.playNote(target.midi, 0.8f)
                _state.update {
                    it.copy(
                        solvedCount = it.solvedCount + 1,
                        justSolved = true,
                        matchProgress = 1f,
                        reading = MicReading(writtenMidi, centsOff, reading.clarity, reading.frequencyHz),
                    )
                }
                viewModelScope.launch {
                    delay(1200)
                    newTarget(playReference = false)
                }
            } else {
                _state.update {
                    it.copy(matchProgress = progress, reading = MicReading(writtenMidi, centsOff, reading.clarity, reading.frequencyHz))
                }
            }
        } else {
            matchSince = 0
            _state.update {
                it.copy(matchProgress = 0f, reading = MicReading(writtenMidi, centsOff, reading.clarity, reading.frequencyHz))
            }
        }
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }
}
