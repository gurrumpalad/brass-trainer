package com.trumpet.trainer.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.trumpet.trainer.R
import com.trumpet.trainer.core.model.WrittenNote

/**
 * Отрисовка нот на Canvas глифами шрифта Bravura (SMuFL).
 *
 * Соглашение SMuFL: 1 em шрифта = высота стана (4 промежутка),
 * поэтому размер шрифта = 4 * spacePx; глифы лежат в PUA-области Unicode.
 */
object StaffRenderer {
    val bravura: FontFamily = FontFamily(Font(R.font.bravura))
}

private const val G_CLEF = "\uE050"      // ключ соль
private const val NOTEHEAD = "\uE0A4"    // головка четвертной
private const val SHARP = "\uE262"
private const val FLAT = "\uE260"

/** Кэш отрисованных глифов: (символ, размер, цвет) -> layout. */
private val layoutCache = HashMap<Long, TextLayoutResult>()

private fun TextMeasurer.measureGlyph(glyph: String, style: TextStyle): TextLayoutResult {
    val key = glyph.hashCode().toLong() * 1_000_003L +
        style.fontSize.value.toRawBits().toLong() * 61L +
        style.color.hashCode().toLong()
    return layoutCache.getOrPut(key) { measure(glyph, style) }
}

/** Стиль Bravura для стана с промежутком spacePx. Требует DrawScope (Density). */
private fun DrawScope.bravuraStyle(spacePx: Float, color: Color): TextStyle =
    TextStyle(fontFamily = StaffRenderer.bravura, color = color, fontSize = (spacePx * 4f).toSp())

/** y-координата ноты на стане (staffTop — верхняя линейка F5). */
fun noteY(staffTop: Float, spacePx: Float, note: WrittenNote): Float =
    staffTop + 4f * spacePx - note.staffPosition * spacePx / 2f

/**
 * Пять линеек + ключ соль. Возвращает x-координату после ключа
 * (откуда можно ставить ноты).
 */
fun DrawScope.drawStaff(
    measurer: TextMeasurer,
    left: Float,
    top: Float,
    width: Float,
    spacePx: Float,
    color: Color,
    clefColor: Color = color,
): Float {
    val stroke = spacePx * 0.08f
    for (i in 0..4) {
        val y = top + i * spacePx
        drawLine(color, Offset(left, y), Offset(left + width, y), strokeWidth = stroke)
    }

    val style = bravuraStyle(spacePx, clefColor)
    val layout = measurer.measureGlyph(G_CLEF, style)
    val box = layout.getBoundingBox(0)
    val gLineY = top + 3f * spacePx // вторая линейка снизу — G4, здесь «сидит» ключ
    drawText(
        layout,
        topLeft = Offset(left + spacePx * 0.4f, gLineY - layout.getLineBaseline(0)),
    )
    return left + spacePx * 0.4f + box.width + spacePx * 0.8f
}

/** Одиночная нота с штилем, случайным знаком и добавочными линейками. */
fun DrawScope.drawNote(
    measurer: TextMeasurer,
    note: WrittenNote,
    centerX: Float,
    staffTop: Float,
    spacePx: Float,
    color: Color,
    withStem: Boolean = true,
) {
    val y = noteY(staffTop, spacePx, note)

    // добавочные линейки
    val half = spacePx * 0.95f
    val stroke = spacePx * 0.08f
    if (note.staffPosition > 8) {
        var pos = 10
        while (pos <= note.staffPosition) {
            val ly = staffTop + 4f * spacePx - pos * spacePx / 2f
            drawLine(color, Offset(centerX - half, ly), Offset(centerX + half, ly), strokeWidth = stroke)
            pos += 2
        }
    }
    if (note.staffPosition < 0) {
        var pos = -2
        while (pos >= note.staffPosition) {
            val ly = staffTop + 4f * spacePx - pos * spacePx / 2f
            drawLine(color, Offset(centerX - half, ly), Offset(centerX + half, ly), strokeWidth = stroke)
            pos -= 2
        }
    }

    // случайный знак
    val style = bravuraStyle(spacePx, color)
    if (note.alter != 0) {
        val glyph = if (note.alter > 0) SHARP else FLAT
        val layout = measurer.measureGlyph(glyph, style)
        val box = layout.getBoundingBox(0)
        drawText(
            layout,
            topLeft = Offset(
                centerX - spacePx * 1.6f - box.width - box.left,
                y - box.height / 2f - box.top,
            ),
        )
    }

    // головка
    val head = measurer.measureGlyph(NOTEHEAD, style)
    val headBox = head.getBoundingBox(0)
    drawText(
        head,
        topLeft = Offset(centerX - headBox.width / 2f - headBox.left, y - headBox.height / 2f - headBox.top),
    )

    // штиль: до средней линии — вверх, выше — вниз
    if (withStem) {
        val stemWidth = spacePx * 0.12f
        val stemLen = 3.5f * spacePx
        if (note.staffPosition <= 4) {
            val x = centerX + headBox.width / 2f - stemWidth / 2f
            drawLine(
                color,
                Offset(x, y - headBox.height * 0.1f),
                Offset(x, y - stemLen),
                strokeWidth = stemWidth,
                cap = StrokeCap.Round,
            )
        } else {
            val x = centerX - headBox.width / 2f + stemWidth / 2f
            drawLine(
                color,
                Offset(x, y + headBox.height * 0.1f),
                Offset(x, y + stemLen),
                strokeWidth = stemWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}
