package com.trumpet.trainer.core.model

/**
 * Клапан трубы. Нумерация совпадает с общепринятой:
 * 1-й клапан понижает звук на 2 полутона, 2-й — на 1, 3-й — на 3.
 */
enum class Valve(val number: Int) {
    FIRST(1),
    SECOND(2),
    THIRD(3);

    companion object {
        fun fromNumber(n: Int): Valve = entries.first { it.number == n }
    }
}

/** Аппликатура: набор нажатых клапанов. Пустое множество — открытая труба. */
data class Fingering(
    val valves: Set<Valve>,
    val isAlternate: Boolean = false,
) {
    /** Строка вида «1+2» или «открыто». */
    fun describe(openText: String = "открыто"): String =
        if (valves.isEmpty()) openText
        else valves.sortedBy { it.number }.joinToString("+") { "${it.number}" }
}
