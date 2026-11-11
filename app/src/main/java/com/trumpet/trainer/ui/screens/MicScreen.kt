package com.trumpet.trainer.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.trumpet.trainer.GameSettings
import com.trumpet.trainer.core.model.FingeringChart
import com.trumpet.trainer.game.MicUiState
import com.trumpet.trainer.game.MicViewModel
import com.trumpet.trainer.ui.components.GameHeader
import com.trumpet.trainer.ui.components.NotationView
import com.trumpet.trainer.ui.localization.AppStrings
import com.trumpet.trainer.ui.localization.getStrings
import com.trumpet.trainer.ui.theme.Bad
import com.trumpet.trainer.ui.theme.Good
import com.trumpet.trainer.ui.theme.TextPrimary
import com.trumpet.trainer.ui.theme.TextSecondary
import kotlin.math.abs

/** Режим с настоящей трубой: микрофон + определение высоты тона. */
@Composable
fun MicScreen(vm: MicViewModel, settings: GameSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    val strings = getStrings(settings.language)

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(hasPermission) {
        if (hasPermission) vm.startListening()
    }

    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        GameHeader(strings.micTitle, onBack)

        if (!hasPermission) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("🎤", fontSize = 56.sp)
                Spacer(Modifier.height(16.dp))
                Text(strings.micPermTitle, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    strings.micPermDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text(strings.micPermBtn)
                }
            }
        } else {
            MicBody(state, vm, settings, strings, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MicBody(
    state: MicUiState,
    vm: MicViewModel,
    settings: GameSettings,
    strings: AppStrings,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        NotationView(
            note = state.target,
            highlight = if (state.justSolved) Good else null,
            showName = settings.showNoteNames,
            language = settings.language,
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            FilledTonalButton(onClick = { vm.playTargetReference() }) { Text(strings.micReferenceBtn) }
            Button(onClick = { vm.newTarget() }) { Text(strings.micNextBtn) }
        }

        Spacer(Modifier.height(24.dp))

        val reading = state.reading
        val heardName = reading?.writtenMidi?.let { FingeringChart.findNote(it)?.displayName(settings.language) } ?: "—"
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(strings.micDetectedNote, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(
                    if (reading == null) strings.micSilence else heardName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(strings.micPitch, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(
                    if (reading == null) "—" else "${reading.frequencyHz.toInt()} ${strings.micHz}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        CentsGauge(reading?.centsOff, Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            if (reading?.centsOff != null) String.format(strings.micCentsDeviation, reading.centsOff) else strings.micCentsNone,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(Modifier.weight(1f))

        Text(
            if (state.justSolved) strings.micHitText
            else strings.micInstruction,
            style = MaterialTheme.typography.titleMedium,
            color = if (state.justSolved) Good else TextPrimary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { state.matchProgress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(10.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            String.format(strings.micSolvedCount, state.solvedCount),
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            strings.micTranspositionNote,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

/** Шкала отклонения в центах с зелёной зоной «в строе» (±35 центов). */
@Composable
private fun CentsGauge(cents: Int?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            val w = size.width
            val cy = size.height / 2f
            val half = w / 2f

            drawLine(TextSecondary, Offset(0f, cy), Offset(w, cy), strokeWidth = 2.dp.toPx())

            val zone = 35f / 50f * half
            drawLine(
                Good,
                Offset(half - zone, cy),
                Offset(half + zone, cy),
                strokeWidth = 7.dp.toPx(),
            )

            listOf(-50, -35, 0, 35, 50).forEach { c ->
                val x = half + c / 50f * half
                drawLine(
                    TextSecondary,
                    Offset(x, cy - 9.dp.toPx()),
                    Offset(x, cy + 9.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                )
            }

            val value = (cents ?: 0).coerceIn(-50, 50)
            val x = half + value / 50f * half
            val needle = when {
                cents == null -> TextSecondary
                abs(cents) <= 35 -> Good
                else -> Bad
            }
            drawLine(
                needle,
                Offset(x, cy - 18.dp.toPx()),
                Offset(x, cy + 18.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("−50", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text("0", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text("+50", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}
