package com.trumpet.trainer.core.model

/**
 * Полная таблица аппликатур трубы in B♭ (записанный строй),
 * рабочий диапазон F♯3 … C6.
 *
 * Правило построения: открытые (без клапанов) звуки образуют натуральный ряд
 * … C4, G4, C5, E5, G5, A5, C6; чтобы опуститься от открытого звука
 * на n полутонов, берут комбинацию клапанов с суммарным понижением n:
 * 1 пт — 2-й клапан; 2 пт — 1-й; 3 пт — 1+2; 4 пт — 2+3; 5 пт — 1+3; 6 пт — 1+2+3.
 *
 * Отсюда альтернативные аппликатуры: 3-й клапан эквивалентен комбинации 1+2
 * (понижение на 3 полутона), поэтому ноты с аппликатурой «1+2»
 * имеют альтернативу «3».
 */
object FingeringChart {

    const val MIN_MIDI = 54 // F♯3
    const val MAX_MIDI = 84 // C6

    private fun v(vararg numbers: Int): Set<Valve> =
        if (numbers.isEmpty()) emptySet() else numbers.map { Valve.fromNumber(it) }.toSet()

    // --- Спеллинг: диезы для F♯/C♯, бемоли для E♭/A♭/B♭ ---
    // (объявлены до chart/ALL_NOTES: они нужны при построении ALL_NOTES)

    private val sharpSpellings = mapOf(1 to "C♯", 6 to "F♯")
    private val flatSpellings = mapOf(3 to "E♭", 8 to "A♭", 10 to "B♭")
    private val naturalSpellings =
        mapOf(0 to "C", 2 to "D", 4 to "E", 5 to "F", 7 to "G", 9 to "A", 11 to "B")

    // энгармонические альтернативы для подсказки вида «F♯4 / G♭4»
    private val flatAltSpellings = mapOf(1 to "D♭", 6 to "G♭")               // к C♯, F♯
    private val sharpAltSpellings = mapOf(3 to "D♯", 8 to "G♯", 10 to "A♯") // к E♭, A♭, B♭

    private val chart: Map<Int, List<Fingering>> = mapOf(
        54 to listOf(Fingering(v(1, 2, 3))),                          // F♯3
        55 to listOf(Fingering(v(1, 3))),                             // G3
        56 to listOf(Fingering(v(2, 3))),                             // A♭3
        57 to listOf(Fingering(v(1, 2)), Fingering(v(3), true)),      // A3
        58 to listOf(Fingering(v(1))),                                // B♭3
        59 to listOf(Fingering(v(2))),                                // B3
        60 to listOf(Fingering(emptySet())),                          // C4
        61 to listOf(Fingering(v(1, 2, 3))),                          // C♯4
        62 to listOf(Fingering(v(1, 3))),                             // D4
        63 to listOf(Fingering(v(2, 3))),                             // E♭4
        64 to listOf(Fingering(v(1, 2)), Fingering(v(3), true)),      // E4
        65 to listOf(Fingering(v(1))),                                // F4
        66 to listOf(Fingering(v(2))),                                // F♯4
        67 to listOf(Fingering(emptySet())),                          // G4
        68 to listOf(Fingering(v(2, 3))),                             // A♭4
        69 to listOf(Fingering(v(1, 2)), Fingering(v(3), true)),      // A4
        70 to listOf(Fingering(v(1))),                                // B♭4
        71 to listOf(Fingering(v(2))),                                // B4
        72 to listOf(Fingering(emptySet())),                          // C5
        73 to listOf(Fingering(v(1, 2)), Fingering(v(3), true)),      // C♯5
        74 to listOf(Fingering(v(1))),                                // D5
        75 to listOf(Fingering(v(2))),                                // E♭5
        76 to listOf(Fingering(emptySet()), Fingering(v(1, 2), true)), // E5
        77 to listOf(Fingering(v(1))),                                // F5
        78 to listOf(Fingering(v(2))),                                // F♯5
        79 to listOf(Fingering(emptySet())),                          // G5
        80 to listOf(Fingering(v(2, 3))),                             // A♭5
        81 to listOf(Fingering(v(1, 2)), Fingering(v(3), true)),      // A5
        82 to listOf(Fingering(v(1)), Fingering(emptySet(), true)),   // B♭5
        83 to listOf(Fingering(v(2))),                                // B5
        84 to listOf(Fingering(emptySet())),                          // C6
    )

    /** Все ноты диапазона по возрастанию midi. */
    val ALL_NOTES: List<WrittenNote> = (MIN_MIDI..MAX_MIDI).map { noteByMidi(it) }

    private val byMidi: Map<Int, WrittenNote> = ALL_NOTES.associateBy { it.midi }

    fun fingerings(midi: Int): List<Fingering> = chart[midi] ?: emptyList()

    fun primary(midi: Int): Fingering? = fingerings(midi).firstOrNull()

    /** Ровное совпадение с одной из допустимых аппликатур (в т.ч. альтернативной). */
    fun accepts(midi: Int, pressed: Set<Valve>): Boolean =
        fingerings(midi).any { it.valves == pressed }

    /**
     * Промежуточное состояние: нажатое — непустое подмножество одной из
     * допустимых аппликатур (игрок ещё не нажал все нужные клапаны).
     */
    fun isPartial(midi: Int, pressed: Set<Valve>): Boolean =
        pressed.isEmpty() || fingerings(midi).any { pressed.all { v -> v in it.valves } }

    fun noteByMidi(midi: Int): WrittenNote = WrittenNote(
        midi = midi,
        name = spell(midi).first,
        altName = spell(midi).second,
        diatonic = diatonicOf(midi),
        alter = alterOf(midi),
    )

    fun findNote(midi: Int): WrittenNote? = byMidi[midi]

    private fun spell(midi: Int): Pair<String, String?> {
        val pc = midi % 12
        val oct = midi / 12 - 1
        val (letter, altLetter) = when {
            naturalSpellings.containsKey(pc) -> naturalSpellings.getValue(pc) to null
            sharpSpellings.containsKey(pc) -> sharpSpellings.getValue(pc) to flatAltSpellings[pc]
            else -> flatSpellings.getValue(pc) to sharpAltSpellings[pc]
        }
        return "$letter$oct" to altLetter?.let { "$it$oct" }
    }

    /** Диатонический индекс (ступень лада + октава). */
    private fun diatonicOf(midi: Int): Int {
        val pc = midi % 12
        val oct = midi / 12 - 1
        val degree = when (pc) {
            0, 1 -> 0          // C, C♯
            2 -> 1             // D
            3, 4 -> 2          // E♭, E
            5, 6 -> 3          // F, F♯
            7 -> 4             // G
            8, 9 -> 5          // A♭, A
            10, 11 -> 6        // B♭, B
            else -> error("unreachable")
        }
        return oct * 7 + degree
    }

    private fun alterOf(midi: Int): Int {
        val pc = midi % 12
        return when (pc) {
            1, 6 -> +1         // C♯, F♯
            3, 8, 10 -> -1     // E♭, A♭, B♭
            else -> 0
        }
    }
}
