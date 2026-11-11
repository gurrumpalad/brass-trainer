package com.trumpet.trainer.core.model

enum class AppLanguage(val code: String, val displayName: String) {
    RUSSIAN("ru", "Русский"),
    ENGLISH("en", "English");

    companion object {
        fun byCode(code: String): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: RUSSIAN
    }
}
