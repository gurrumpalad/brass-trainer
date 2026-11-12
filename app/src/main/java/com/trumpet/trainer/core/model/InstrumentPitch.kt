package com.trumpet.trainer.core.model

/**
 * Строй духового инструмента (транспозиция).
 */
enum class InstrumentPitch(val semitones: Int, val displayName: String) {
    BB_FLAT(-2, "B♭"),
    EB_FLAT(-9, "E♭"),
    F(-7, "F"),
    C(0, "C"),
}
