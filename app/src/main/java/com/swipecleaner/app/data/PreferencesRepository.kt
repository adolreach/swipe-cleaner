package com.swipecleaner.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "swipecleaner_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val reappearAfterMillis: Long = DEFAULT_REAPPEAR_MS,
    val selectedBucketId: String? = null
) {
    companion object {
        // 6 meses por defecto
        const val DEFAULT_REAPPEAR_MS = 6L * 30L * 24L * 60L * 60L * 1000L
    }
}

class PreferencesRepository(context: Context) {

    private val ds = context.dataStore

    private val keyTheme = stringPreferencesKey("theme_mode")
    private val keyReappear = longPreferencesKey("reappear_ms")
    private val keyBucket = stringPreferencesKey("selected_bucket")

    val preferences: Flow<UserPreferences> = ds.data.map { prefs ->
        UserPreferences(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[keyTheme] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            reappearAfterMillis = prefs[keyReappear] ?: UserPreferences.DEFAULT_REAPPEAR_MS,
            selectedBucketId = prefs[keyBucket]
        )
    }

    suspend fun setTheme(mode: ThemeMode) {
        ds.edit { it[keyTheme] = mode.name }
    }

    suspend fun setReappearMillis(millis: Long) {
        ds.edit { it[keyReappear] = millis }
    }

    suspend fun setSelectedBucket(bucketId: String?) {
        ds.edit {
            if (bucketId == null) it.remove(keyBucket)
            else it[keyBucket] = bucketId
        }
    }
}
