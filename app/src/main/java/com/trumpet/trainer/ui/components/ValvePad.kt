package com.trumpet.trainer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trumpet.trainer.core.model.Valve
import com.trumpet.trainer.ui.theme.Bad
import com.trumpet.trainer.ui.theme.Brass
import com.trumpet.trainer.ui.theme.BrassBright
import com.trumpet.trainer.ui.theme.Good
import com.trumpet.trainer.ui.theme.Ink
import com.trumpet.trainer.ui.theme.LineColor
import com.trumpet.trainer.ui.theme.Panel
import com.trumpet.trainer.ui.theme.PanelLight
import com.trumpet.trainer.ui.theme.TextSecondary

/**
 * Три клапана трубы. Поддерживает мультитач: несколько клапанов можно
 * держать одновременно; состояние поднимается наверх целиком (Set<Valve>).
 *
 * [markCorrect]/[markWrong] — подсветка правильного ответа и ошибки игрока.
 * [onOpenClick] — опциональная кнопка явного выбора «открытая труба» (0 клапанов).
 */
@Composable
fun ValvePad(
    pressed: Set<Valve>,
    onValvesChange: (Set<Valve>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    markCorrect: Set<Valve> = emptySet(),
    markWrong: Set<Valve> = emptySet(),
    onOpenClick: (() -> Unit)? = null,
    openLabel: String = "🎺   0 — открытая труба",
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        while (true) {
                            val event = awaitPointerEvent()
                            val currentPressed = mutableSetOf<Valve>()
                            event.changes.forEach { change ->
                                if (change.pressed) {
                                    val x = change.position.x
                                    val totalWidth = size.width
                                    if (totalWidth > 0f) {
                                        val third = totalWidth / 3f
                                        val valve = when {
                                            x < third -> Valve.FIRST
                                            x < third * 2 -> Valve.SECOND
                                            else -> Valve.THIRD
                                        }
                                        currentPressed.add(valve)
                                    }
                                }
                            }
                            onValvesChange(currentPressed)
                            if (event.changes.all { !it.pressed }) {
                                onValvesChange(emptySet())
                                break
                            }
                        }
                    }
                },
            horizontalArrangement = Arrangement.Center,
        ) {
            Valve.entries.forEach { valve ->
                val isPressed = valve in pressed
                val inCorrect = valve in markCorrect
                val inWrong = valve in markWrong

                Piston(
                    valve = valve,
                    pressed = isPressed,
                    highlight = when {
                        inCorrect -> Good
                        inWrong -> Bad
                        isPressed -> Brass
                        else -> null
                    },
                )
            }
        }

        if (onOpenClick != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenClick,
                enabled = enabled,
            ) {
                Text(openLabel, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun Piston(
    valve: Valve,
    pressed: Boolean,
    highlight: Color?,
) {
    val sink by animateFloatAsState(
        targetValue = if (pressed) 10f else 0f,
        animationSpec = spring(stiffness = 700f),
        label = "piston-sink",
    )

    val bodyColor = when {
        highlight != null && pressed -> highlight
        pressed -> Brass
        highlight != null -> highlight.copy(alpha = 0.35f)
        else -> PanelLight
    }
    val capColor = when {
        pressed -> BrassBright
        highlight == Good -> Good
        highlight == Bad -> Bad
        else -> Panel
    }
    val labelColor = if (pressed) Ink else TextSecondary

    Box(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .width(86.dp)
            .height(140.dp)
            .graphicsLayer { translationY = sink }
            .clip(RoundedCornerShape(20.dp))
            .background(bodyColor)
            .border(2.dp, if (highlight != null) highlight else LineColor, RoundedCornerShape(20.dp))
            .semantics { contentDescription = "Клапан ${valve.number}" },
        contentAlignment = Alignment.BottomCenter,
    ) {
        // «шляпка» поршня сверху
        Box(
            modifier = Modifier
                .padding(top = 14.dp)
                .align(Alignment.TopCenter)
                .size(width = 54.dp, height = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(capColor),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 14.dp),
        ) {
            Text(
                text = valve.number.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = labelColor,
            )
        }
    }
}
