package com.trumpet.trainer.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.trumpet.trainer.GameSettings
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Проигрывание эталонных нот и метронома.
 *
 * Ноты синтезируются [BrassSynth] при первом обращении и кэшируются
 * в статических буферах AudioTrack (MODE_STATIC) — минимальная задержка
 * повторного запуска. Для прототипа этого достаточно; в продакшене
 * стратегия заменяется на сэмплы/SoundFont без изменения интерфейса.
 */
class AudioNotePlayer(private val settings: GameSettings) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    data class NoteKey(val midi: Int, val semitones: Int)

    private val noteTracks = ConcurrentHashMap<NoteKey, AudioTrack>()
    private var clickAccent: AudioTrack? = null
    private var clickNormal: AudioTrack? = null
    private var buzzTrack: AudioTrack? = null

    @Volatile
    var enabled: Boolean = true

    fun playNote(midi: Int, volume: Float = 0.9f) {
        if (!enabled) return
        val semitones = settings.instrumentPitch.semitones
        scope.launch { noteTrack(midi, semitones).playOnce(volume) }
    }

    fun playClick(accent: Boolean) {
        if (!enabled) return
        val track = if (accent) clickAccentTrack() else clickNormalTrack()
        scope.launch { track.playOnce(0.6f) }
    }

    fun playBuzz() {
        if (!enabled) return
        scope.launch { buzzTrack().playOnce(0.7f) }
    }

    fun release() {
        scope.cancel()
        noteTracks.values.forEach { runCatching { it.release() } }
        noteTracks.clear()
        clickAccent?.let { runCatching { it.release() } }
        clickNormal?.let { runCatching { it.release() } }
        buzzTrack?.let { runCatching { it.release() } }
        clickAccent = null
        clickNormal = null
        buzzTrack = null
    }

    private fun noteTrack(midi: Int, semitones: Int): AudioTrack =
        noteTracks.getOrPut(NoteKey(midi, semitones)) { build(BrassSynth.notePcm(midi, semitones = semitones)) }

    private fun clickAccentTrack(): AudioTrack =
        clickAccent ?: build(BrassSynth.clickPcm(accent = true)).also { clickAccent = it }

    private fun clickNormalTrack(): AudioTrack =
        clickNormal ?: build(BrassSynth.clickPcm(accent = false)).also { clickNormal = it }

    private fun buzzTrack(): AudioTrack =
        buzzTrack ?: build(BrassSynth.buzzPcm()).also { buzzTrack = it }

    private fun build(pcm: ByteArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(BrassSynth.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size)
            .build()
        track.write(pcm, 0, pcm.size)
        return track
    }

    private fun AudioTrack.playOnce(volume: Float) {
        runCatching {
            setVolume(volume)
            if (playState == AudioTrack.PLAYSTATE_PLAYING) stop()
            setPlaybackHeadPosition(0)
            play()
        }
    }
}
