package com.swipecleaner.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SwipeRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun swipeDao(): SwipeDao
}
