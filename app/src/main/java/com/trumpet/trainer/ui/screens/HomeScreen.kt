package com.trumpet.trainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trumpet.trainer.AppContainer
import com.trumpet.trainer.core.model.AppLanguage
import com.trumpet.trainer.core.model.InstrumentPitch
import com.trumpet.trainer.core.model.Level
import com.trumpet.trainer.ui.localization.getStrings
import com.trumpet.trainer.ui.theme.Brass
import com.trumpet.trainer.ui.theme.Ink
import com.trumpet.trainer.ui.theme.LineColor
import com.trumpet.trainer.ui.theme.Panel
import com.trumpet.trainer.ui.theme.PanelLight
import com.trumpet.trainer.ui.theme.TextSecondary

/** Главный экран: выбор режима тренировки и настройки. */
@Composable
fun HomeScreen(
    container: AppContainer,
    onStartSight: () -> Unit,
    onStartRhythm: () -> Unit,
    onStartEar: () -> Unit,
    onStartMic: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val settings = container.settings
    var language by remember { mutableStateOf(settings.language) }
    var instrumentPitch by remember { mutableStateOf(settings.instrumentPitch) }
    var level by remember { mutableStateOf(settings.level) }
    var bpm by remember { mutableStateOf(settings.bpm.toFloat()) }
    var showNames by remember { mutableStateOf(settings.showNoteNames) }
    var soundOn by remember { mutableStateOf(settings.soundEnabled) }

    val strings = getStrings(language)

    LaunchedEffect(Unit) { container.audio.enabled = settings.soundEnabled }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎺", fontSize = 44.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    strings.appTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    strings.appSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        ModeCard("🎵", strings.modeSightTitle, strings.modeSightDesc, onStartSight)
        ModeCard("🥁", strings.modeRhythmTitle, strings.modeRhythmDesc, onStartRhythm)
        ModeCard("👂", strings.modeEarTitle, strings.modeEarDesc, onStartEar)
        ModeCard("🎤", strings.modeMicTitle, strings.modeMicDesc, onStartMic)

        OutlinedButton(onClick = onOpenStats, modifier = Modifier.fillMaxWidth()) {
            Text(strings.statsButton)
        }

        HorizontalDivider()

        Text(strings.settingsTitle, style = MaterialTheme.typography.titleMedium, color = Brass)

        Text(strings.settingLanguage, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { lang ->
                Chip(
                    selected = lang == language,
                    onClick = {
                        language = lang
                        settings.language = lang
                    },
                    text = lang.displayName,
                )
            }
        }

        Text(strings.settingInstrumentPitch, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InstrumentPitch.entries.forEach { pitch ->
                Chip(
                    selected = pitch == instrumentPitch,
                    onClick = {
                        instrumentPitch = pitch
                        settings.instrumentPitch = pitch
                    },
                    text = pitch.displayName,
                )
            }
        }

        Text(strings.settingRange, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Level.entries.forEach { l ->
                Chip(
                    selected = l == level,
                    onClick = {
                        level = l
                        settings.level = l
                    },
                    text = "${strings.levelPrefix} ${l.ordinal + 1}",
                )
            }
        }
        Text(
            "${strings.levelCurrent} ${level.getTitle(language)}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )

        Text("${strings.settingBpm} ${bpm.toInt()} ${strings.bpmUnit}", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = bpm,
            onValueChange = {
                bpm = it
                settings.bpm = it.toInt()
            },
            valueRange = 60f..160f,
        )

        SettingRow(strings.settingShowNames, showNames) {
            showNames = it
            settings.showNoteNames = it
        }
        SettingRow(strings.settingSound, soundOn) {
            soundOn = it
            settings.soundEnabled = it
            container.audio.enabled = it
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ModeCard(emoji: String, title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
    ) {
        Row(
            Modifier
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(emoji, fontSize = 30.sp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Text("→", color = TextSecondary, fontSize = 18.sp)
        }
    }
}

@Composable
private fun Chip(selected: Boolean, onClick: () -> Unit, text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Brass else PanelLight)
            .border(1.dp, if (selected) Brass else LineColor, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Ink else TextSecondary,
        )
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
