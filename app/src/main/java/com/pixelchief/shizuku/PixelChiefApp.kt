package com.pixelchief.shizuku

import android.app.Application
import com.pixelchief.shizuku.bot.BotController

class PixelChiefApp : Application() {

    lateinit var settingsStore: SettingsStore
        private set

    lateinit var botController: BotController
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
        botController = BotController(this, settingsStore)
        ShizukuManager.init()
        LogBus.info("Application", "PixelChief Application launched successfully.")
    }
}
