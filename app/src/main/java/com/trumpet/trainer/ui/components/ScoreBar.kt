package com.trumpet.trainer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trumpet.trainer.core.model.AppLanguage
import com.trumpet.trainer.ui.localization.getStrings
import com.trumpet.trainer.ui.theme.Brass
import com.trumpet.trainer.ui.theme.Good
import com.trumpet.trainer.ui.theme.TextSecondary

/** Полоска счёта: очки, серия, точность. */
@Composable
fun ScoreBar(
    score: Int,
    combo: Int,
    accuracy: Int? = null,
    extra: String? = null,
    language: AppLanguage = AppLanguage.RUSSIAN,
    modifier: Modifier = Modifier,
) {
    val strings = getStrings(language)
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${strings.scoreLabel} $score",
            style = MaterialTheme.typography.titleMedium,
            color = Brass,
            fontWeight = FontWeight.Bold,
        )
        if (combo > 0) {
            Text(
                "${strings.comboLabel} ×$combo",
                style = MaterialTheme.typography.titleMedium,
                color = Good,
            )
        }
        if (accuracy != null) {
            Text(
                "$accuracy%",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
            )
        }
        if (extra != null) {
            Text(extra, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}
