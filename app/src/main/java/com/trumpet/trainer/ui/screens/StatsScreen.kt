package com.trumpet.trainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.trumpet.trainer.core.model.AppLanguage
import com.trumpet.trainer.core.model.FingeringChart
import com.trumpet.trainer.core.model.WrittenNote
import com.trumpet.trainer.core.stats.NoteStat
import com.trumpet.trainer.core.stats.StatsRepository
import com.trumpet.trainer.ui.components.GameHeader
import com.trumpet.trainer.ui.localization.getStrings
import com.trumpet.trainer.ui.theme.Bad
import com.trumpet.trainer.ui.theme.Brass
import com.trumpet.trainer.ui.theme.Good
import com.trumpet.trainer.ui.theme.Panel
import com.trumpet.trainer.ui.theme.PanelLight
import com.trumpet.trainer.ui.theme.TextPrimary
import com.trumpet.trainer.ui.theme.TextSecondary

/** Статистика тренировок: точность по нотам, «нагрев», лучшая серия. */
@Composable
fun StatsScreen(
    stats: StatsRepository,
    language: AppLanguage = AppLanguage.RUSSIAN,
    onBack: () -> Unit,
) {
    var data by remember { mutableStateOf(stats.data) }
    var confirmReset by remember { mutableStateOf(false) }
    val strings = getStrings(language)

    Column(Modifier.fillMaxSize()) {
        GameHeader(strings.statsTitle, onBack)

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SummaryCard(strings.statsTotalAnswers, data.totalAnswers.toString(), Modifier.weight(1f))
            SummaryCard(strings.statsBestCombo, data.bestCombo.toString(), Modifier.weight(1f))
        }

        val rows = data.notes.entries
            .filter { it.value.attempts > 0 }
            .map { (key, stat) -> StatRow(key.toInt(), stat, FingeringChart.findNote(key.toInt())) }
            .sortedBy { it.stat.accuracyPercent }

        if (rows.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(strings.statsEmpty, color = TextSecondary)
            }
        } else {
            Text(
                strings.statsTopProblem,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            LazyColumn(Modifier.weight(1f)) {
                items(rows) { row ->
                    NoteStatRow(row, openText = strings.openTrumpetText)
                    HorizontalDivider(color = Panel)
                }
            }
        }

        TextButton(
            onClick = { confirmReset = true },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp),
        ) {
            Text(strings.statsResetBtn, color = Bad)
        }
    }

    if (confirmReset) {
        Dialog(onDismissRequest = { confirmReset = false }) {
            Card(colors = CardDefaults.cardColors(containerColor = PanelLight)) {
                Column(Modifier.padding(24.dp)) {
                    Text(strings.statsResetConfirmTitle, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        strings.statsResetConfirmDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextButton(onClick = { confirmReset = false }) { Text(strings.statsCancelBtn) }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            stats.reset()
                            data = stats.data
                            confirmReset = false
                        }) { Text(strings.statsResetActionBtn) }
                    }
                }
            }
        }
    }
}

private data class StatRow(
    val midi: Int,
    val stat: NoteStat,
    val note: WrittenNote?,
)

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Panel),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = Brass,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun NoteStatRow(row: StatRow, openText: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            row.note?.name ?: "?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(64.dp),
        )
        Text(
            FingeringChart.primary(row.midi)?.describe(openText) ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.width(72.dp),
        )
        Column(Modifier.weight(1f)) {
            val fraction = row.stat.accuracyPercent / 100f
            val barColor = when {
                row.stat.accuracyPercent >= 80 -> Good
                row.stat.accuracyPercent >= 50 -> Brass
                else -> Bad
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PanelLight),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor),
                )
            }
        }
        Text(
            "${row.stat.accuracyPercent}%",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(60.dp)
                .padding(start = 12.dp),
        )
    }
}
