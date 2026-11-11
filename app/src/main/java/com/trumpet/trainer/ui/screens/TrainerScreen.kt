package com.trumpet.trainer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trumpet.trainer.GameSettings
import com.trumpet.trainer.game.Answer
import com.trumpet.trainer.game.TrainerViewModel
import com.trumpet.trainer.ui.components.GameHeader
import com.trumpet.trainer.ui.components.NotationView
import com.trumpet.trainer.ui.components.ScoreBar
import com.trumpet.trainer.ui.components.ValvePad
import com.trumpet.trainer.ui.localization.AppStrings
import com.trumpet.trainer.ui.localization.getStrings
import com.trumpet.trainer.ui.theme.Bad
import com.trumpet.trainer.ui.theme.Good
import com.trumpet.trainer.ui.theme.TextSecondary

/**
 * Экран режимов «нота → аппликатура».
 * Чтение с листа (нота видна сразу) и «на слух» ([hideNoteUntilAnswer]):
 * во втором случае нота раскрывается только после ответа.
 */
@Composable
fun TrainerScreen(
    vm: TrainerViewModel,
    settings: GameSettings,
    title: String,
    hideNoteUntilAnswer: Boolean,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val answer = state.answer
    val noteVisible = !hideNoteUntilAnswer || answer != null
    val strings = getStrings(settings.language)

    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        GameHeader(title, onBack) {
            Text(
                state.levelTitle,
                color = TextSecondary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        ScoreBar(
            score = state.score,
            combo = state.combo,
            accuracy = state.accuracy,
            language = settings.language,
        )

        if (noteVisible) {
            NotationView(
                note = state.note,
                highlight = when (answer) {
                    is Answer.Correct -> Good
                    is Answer.Wrong -> Bad
                    null -> null
                },
                showName = settings.showNoteNames,
                language = settings.language,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp),
            )
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(strings.earListenPrompt, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(20.dp))
                FilledTonalButton(onClick = { vm.replaySound() }) {
                    Text(strings.earPlayAgain)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    strings.earHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }

        AnswerFeedback(answer, strings)

        ValvePad(
            pressed = state.pressed,
            onValvesChange = vm::onValvesChanged,
            onOpenClick = vm::onOpenPressed,
            openLabel = strings.openTrumpetBtn,
            markCorrect = when (answer) {
                is Answer.Correct -> answer.fingering.valves
                is Answer.Wrong -> answer.correct.firstOrNull()?.valves ?: emptySet()
                null -> emptySet()
            },
            markWrong = (answer as? Answer.Wrong)?.chosen ?: emptySet(),
            modifier = Modifier.padding(bottom = 12.dp),
        )

        TextButton(
            onClick = vm::skip,
            enabled = answer == null,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(strings.skipNoteBtn)
        }
    }
}

@Composable
private fun AnswerFeedback(answer: Answer?, strings: AppStrings) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (answer) {
            is Answer.Correct -> Text(
                text = "${strings.answerCorrect}${answer.fingering.describe(strings.openTrumpetText)}" +
                    if (answer.usedAlternate) strings.answerAlternate else "",
                color = Good,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            is Answer.Wrong -> Text(
                text = "${strings.answerWrong}" +
                    answer.correct.joinToString(strings.answerOr) { it.describe(strings.openTrumpetText) },
                color = Bad,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            null -> Text(
                strings.answerPrompt,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}
