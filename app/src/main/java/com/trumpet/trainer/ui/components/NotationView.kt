package com.trumpet.trainer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trumpet.trainer.core.model.AppLanguage
import com.trumpet.trainer.core.model.WrittenNote
import com.trumpet.trainer.ui.theme.TextSecondary

/**
 * Нота на нотоносце (скрипичный ключ). Промежуток стана ~ 14dp,
 * [highlight] окрашивает головку (зелёный/красный для фидбека).
 */
@Composable
fun NotationView(
    note: WrittenNote?,
    modifier: Modifier = Modifier,
    highlight: Color? = null,
    showName: Boolean = false,
    language: AppLanguage = AppLanguage.RUSSIAN,
    spaceDp: Dp = 14.dp,
    staffColor: Color = TextSecondary,
) {
    val measurer = rememberTextMeasurer()
    val noteColor = highlight ?: MaterialTheme.colorScheme.onBackground

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(spaceDp * 7)
        ) {
            val spacePx = spaceDp.toPx()
            val staffTop = spacePx * 1.5f
            val contentRight = size.width - spacePx

            val notesX = drawStaff(measurer, left = spacePx, top = staffTop, width = contentRight - spacePx, spacePx = spacePx, color = staffColor)
            val note = note
            if (note != null) {
                val centerX = notesX + (contentRight - notesX) * 0.45f
                drawNote(measurer, note, centerX, staffTop, spacePx, noteColor)
            }
        }
        if (showName && note != null) {
            val nameStr = note.displayName(language)
            val altStr = note.displayAltName(language)?.let { " / $it" } ?: ""
            Text(
                text = nameStr + altStr,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
