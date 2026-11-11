package com.trumpet.trainer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trumpet.trainer.core.model.AppLanguage
import com.trumpet.trainer.core.model.FingeringChart
import com.trumpet.trainer.core.model.Valve
import com.trumpet.trainer.game.RhythmUiState
import com.trumpet.trainer.game.RhythmViewModel
import com.trumpet.trainer.ui.components.GameHeader
import com.trumpet.trainer.ui.components.ScoreBar
import com.trumpet.trainer.ui.components.ValvePad
import com.trumpet.trainer.ui.components.drawNote
import com.trumpet.trainer.ui.components.drawStaff
import com.trumpet.trainer.ui.localization.AppStrings
import com.trumpet.trainer.ui.localization.getStrings
import com.trumpet.trainer.ui.theme.Bad
import com.trumpet.trainer.ui.theme.Brass
import com.trumpet.trainer.ui.theme.Good
import com.trumpet.trainer.ui.theme.TextPrimary
import com.trumpet.trainer.ui.theme.TextSecondary

/** Ритм-режим: бегущая строка нот, метроном, окно попадания. */
@Composable
fun RhythmScreen(
    vm: RhythmViewModel,
    language: AppLanguage = AppLanguage.RUSSIAN,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsState()
    var pressed by remember { mutableStateOf(emptySet<Valve>()) }
    val strings = getStrings(language)

    // Игровой цикл: время двигается каждый кадр, пока идёт раунд.
    LaunchedEffect(state.running) {
        if (state.running) {
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    if (last != 0L) vm.tick((now - last) / 1_000_000L)
                    last = now
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        GameHeader(strings.modeRhythmTitle, onBack) {
            Text(
                "${state.bpm} ${strings.bpmUnit}",
                color = TextSecondary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        ScoreBar(
            score = state.score,
            combo = state.combo,
            extra = String.format(strings.rhythmHitsMisses, state.hits, state.misses),
            language = language,
        )

        when {
            state.finished -> RhythmResults(
                state,
                strings,
                onRestart = {
                    pressed = emptySet()
                    vm.restart()
                },
                modifier = Modifier.weight(1f),
            )
            state.running -> {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    RhythmStrip(state, Modifier.fillMaxSize())
                    if (state.countdown > 0) {
                        Box(
                            Modifier.matchParentSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                state.countdown.toString(),
                                fontSize = 96.sp,
                                color = Brass,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Text(
                    strings.rhythmInstruction,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                ValvePad(
                    pressed = pressed,
                    onValvesChange = {
                        pressed = it
                        vm.onValves(it)
                    },
                    onOpenClick = {
                        pressed = emptySet()
                        vm.onValves(emptySet())
                    },
                    openLabel = strings.openTrumpetBtn,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
            else -> RhythmStart(
                state,
                strings,
                onStart = {
                    pressed = emptySet()
                    vm.start()
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RhythmStart(
    state: RhythmUiState,
    strings: AppStrings,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🥁", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text(strings.modeRhythmTitle, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            String.format(strings.rhythmStartSub, state.bpm, strings.bpmUnit),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onStart) { Text(strings.rhythmStartBtn) }
    }
}

@Composable
private fun RhythmResults(
    state: RhythmUiState,
    strings: AppStrings,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = state.hits + state.misses
    val accuracy = if (total == 0) 0 else state.hits * 100 / total
    Column(
        modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(if (accuracy >= 80) "🎉" else if (accuracy >= 50) "👍" else "😅", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(String.format(strings.rhythmAccuracy, accuracy), style = MaterialTheme.typography.titleLarge)
        Text(
            String.format(strings.rhythmHitsSummary, state.hits, total, state.bestCombo),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(8.dp))
        Text(String.format(strings.rhythmScore, state.score), style = MaterialTheme.typography.titleMedium, color = Brass)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onRestart) { Text(strings.rhythmRestartBtn) }
    }
}

/** Бегущая строка: нотоносец, метки долей, линия попадания и сами ноты. */
@Composable
private fun RhythmStrip(state: RhythmUiState, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        val spacePx = 12.dp.toPx()
        val left = 8.dp.toPx()
        val staffTop = 3.dp.toPx() + spacePx
        val width = size.width - left * 2
        val hitX = left + width * 0.30f
        val pxPerMs = width * 0.35f / 1000f // вперёд видно ~2.8 с

        drawStaff(measurer, left, staffTop, width, spacePx, TextSecondary)

        // метки долей на стане
        val t0 = state.nowMs - (hitX - left) / pxPerMs
        val t1 = state.nowMs + (left + width - hitX) / pxPerMs
        var beat = (t0 / state.beatMs).toLong()
        while (beat * state.beatMs <= t1) {
            if (beat >= 0) {
                val x = hitX + (beat * state.beatMs - state.nowMs) * pxPerMs
                if (x >= left && x <= left + width) {
                    val accent = beat % 4 == 0L
                    drawLine(
                        TextSecondary.copy(alpha = if (accent) 0.45f else 0.22f),
                        Offset(x, staffTop + spacePx),
                        Offset(x, staffTop + 4 * spacePx),
                        strokeWidth = if (accent) 3f else 2f,
                    )
                }
            }
            beat++
        }

        // линия попадания
        drawLine(
            Brass,
            Offset(hitX, staffTop - spacePx * 0.7f),
            Offset(hitX, staffTop + spacePx * 4.7f),
            strokeWidth = 5f,
        )

        state.notes.forEach { n ->
            val x = hitX + (n.targetMs - state.nowMs) * pxPerMs
            if (x < left - spacePx * 3 || x > left + width + spacePx * 3) return@forEach
            val note = FingeringChart.findNote(n.midi) ?: return@forEach
            val color = when {
                n.hit -> Good
                n.missed -> Bad
                else -> TextPrimary
            }
            drawNote(measurer, note, x, staffTop, spacePx, color)
        }
    }
}
