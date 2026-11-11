package com.trumpet.trainer.core.model

/**
 * Уровень сложности: рабочий диапазон нот.
 * Прогрессия от «первой октавы для новичка» к полному диапазону трубы.
 * Каждый следующий уровень строгим образом расширяет предыдущий.
 */
enum class Level(val range: IntRange) {
    LEVEL_1(60..67),   // C4 – G4
    LEVEL_2(60..72),   // C4 – C5
    LEVEL_3(54..72),   // F♯3 – C5
    LEVEL_4(54..79),   // F♯3 – G5
    LEVEL_5(54..84);   // F♯3 – C6

    val title: String
        get() = getTitle(AppLanguage.RUSSIAN)

    fun getTitle(language: AppLanguage): String {
        val startNote = FingeringChart.findNote(range.first)?.displayName(language) ?: ""
        val endNote = FingeringChart.findNote(range.last)?.displayName(language) ?: ""
        return "$startNote–$endNote"
    }

    companion object {
        fun byIndex(i: Int): Level = entries.getOrElse(i) { LEVEL_1 }
    }
}
