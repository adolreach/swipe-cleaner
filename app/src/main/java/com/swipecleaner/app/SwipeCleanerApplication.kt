package com.swipecleaner.app

import android.app.Application
import com.swipecleaner.app.data.AppContainer
import com.swipecleaner.app.data.DefaultAppContainer

/**
 * Application class. Inicializa el contenedor de dependencias global (manual DI).
 * Se evita Hilt/Dagger para mantener el proyecto más simple de compilar.
 */
class SwipeCleanerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
