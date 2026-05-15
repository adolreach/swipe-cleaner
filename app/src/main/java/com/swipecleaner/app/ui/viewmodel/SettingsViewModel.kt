package com.swipecleaner.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swipecleaner.app.SwipeCleanerApplication
import com.swipecleaner.app.data.Album
import com.swipecleaner.app.data.MediaRepository
import com.swipecleaner.app.data.PreferencesRepository
import com.swipecleaner.app.data.SwipeRepository
import com.swipecleaner.app.data.ThemeMode
import com.swipecleaner.app.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val prefs: UserPreferences = UserPreferences(),
    val albums: List<Album> = emptyList()
)

class SettingsViewModel(
    private val prefsRepo: PreferencesRepository,
    private val swipeRepo: SwipeRepository,
    private val mediaRepo: MediaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefsRepo.preferences.collect { prefs ->
                _state.update { it.copy(prefs = prefs) }
            }
        }
    }

    fun loadAlbums() {
        viewModelScope.launch {
            val albums = mediaRepo.queryAlbums()
            _state.update { it.copy(albums = albums) }
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setTheme(mode) }
    }

    fun setReappearMonths(months: Int) {
        val millis = months * 30L * 24L * 60L * 60L * 1000L
        viewModelScope.launch { prefsRepo.setReappearMillis(millis) }
    }

    fun setBucket(bucketId: String?) {
        viewModelScope.launch { prefsRepo.setSelectedBucket(bucketId) }
    }

    fun resetKeptList() {
        viewModelScope.launch { swipeRepo.resetKeptList() }
    }

    fun resetAll() {
        viewModelScope.launch { swipeRepo.resetAll() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SwipeCleanerApplication
                SettingsViewModel(
                    app.container.preferencesRepository,
                    app.container.swipeRepository,
                    app.container.mediaRepository
                )
            }
        }
    }
}
