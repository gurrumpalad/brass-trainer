package com.trumpet.trainer

import android.app.Application
import android.content.Context
import com.trumpet.trainer.core.audio.AudioNotePlayer
import com.trumpet.trainer.core.model.AppLanguage
import com.trumpet.trainer.core.model.InstrumentPitch
import com.trumpet.trainer.core.model.Level
import com.trumpet.trainer.core.stats.StatsRepository

/** Простой контейнер зависимостей (Hilt в прототипе избыточен). */
class AppContainer(context: Context) {
    val settings = GameSettings()
    val audio = AudioNotePlayer(settings).apply { enabled = settings.soundEnabled }
    val stats = StatsRepository(context.filesDir.resolve("stats.json")).apply { load() }
}

/** Глобальные настройки сессии (в памяти; в продакшене — DataStore). */
class GameSettings {
    var level: Level = Level.LEVEL_1
    var showNoteNames: Boolean = false
    var soundEnabled: Boolean = true
    var bpm: Int = 90
    var language: AppLanguage = AppLanguage.RUSSIAN
    var instrumentPitch: InstrumentPitch = InstrumentPitch.BB_FLAT
}

class TrumpetApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
