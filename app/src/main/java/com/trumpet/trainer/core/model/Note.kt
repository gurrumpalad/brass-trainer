package com.trumpet.trainer.core.model

/**
 * Нота в записанном строе — так, как трубач читает её с листа
 * (труба in B♭ звучит на большую секунду ниже написанного).
 */
data class WrittenNote(
    val midi: Int,
    val name: String,
    val altName: String?,
    /** Диатонический индекс: 7 * октава + ступень лада (C=0 … B=6). */
    val diatonic: Int,
    /** Альтерация: -1 — бемоль, +1 — диез. */
    val alter: Int,
) {
    /**
     * Позиция на нотоносце относительно нижней линейки (E4),
     * в полу-промежутках: 0 — на линии E4, 1 — над ней, -1 — под ней и т.д.
     */
    val staffPosition: Int get() = diatonic - DIATONIC_E4

    fun displayName(language: AppLanguage): String =
        if (language == AppLanguage.RUSSIAN) toRussianNoteName(name, midi) else name

    fun displayAltName(language: AppLanguage): String? =
        altName?.let { if (language == AppLanguage.RUSSIAN) toRussianNoteName(it, midi) else it }

    companion object {
        /** Диатонический индекс E4 — нижней линейки скрипичного ключа. */
        const val DIATONIC_E4 = 7 * 4 + 2

        fun toRussianNoteName(latinName: String, midi: Int): String {
            var result = latinName
            result = result.replace("C", "До")
            result = result.replace("D", "Ре")
            result = result.replace("E", "Ми")
            result = result.replace("F", "Фа")
            result = result.replace("G", "Соль")
            result = result.replace("A", "Ля")
            result = result.replace("B", "Си")

            val octave = midi / 12 - 1
            val octaveName = when (octave) {
                2 -> "большой"
                3 -> "малой"
                4 -> "первой"
                5 -> "второй"
                6 -> "третьей"
                else -> ""
            }

            result = result.filter { !it.isDigit() }
            return if (octaveName.isNotEmpty()) "$result $octaveName" else result
        }
    }
}
