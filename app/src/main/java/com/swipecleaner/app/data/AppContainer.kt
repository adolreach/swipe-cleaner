package com.swipecleaner.app.data

import android.content.Context
import androidx.room.Room

interface AppContainer {
    val swipeRepository: SwipeRepository
    val mediaRepository: MediaRepository
    val preferencesRepository: PreferencesRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "swipecleaner_db"
        ).fallbackToDestructiveMigration().build()
    }

    override val swipeRepository: SwipeRepository by lazy {
        SwipeRepository(database.swipeDao())
    }

    override val mediaRepository: MediaRepository by lazy {
        MediaRepository(context.applicationContext)
    }

    override val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(context.applicationContext)
    }
}
