package com.swipecleaner.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swipecleaner.app.SwipeCleanerApplication
import com.swipecleaner.app.data.MediaPhoto
import com.swipecleaner.app.data.MediaRepository
import com.swipecleaner.app.data.PreferencesRepository
import com.swipecleaner.app.data.SwipeRecord
import com.swipecleaner.app.data.SwipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SwipeUiState(
    val photos: List<MediaPhoto> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val freedBytes: Long = 0L,
    val canUndo: Boolean = false,
    val emptyMessage: String? = null
) {
    val currentPhoto: MediaPhoto? get() = photos.getOrNull(currentIndex)
    val nextPhoto: MediaPhoto? get() = photos.getOrNull(currentIndex + 1)
}

class SwipeViewModel(
    private val mediaRepo: MediaRepository,
    private val swipeRepo: SwipeRepository,
    private val prefsRepo: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SwipeUiState())
    val state: StateFlow<SwipeUiState> = _state.asStateFlow()

    private var lastUndoStack: ArrayDeque<UndoEntry> = ArrayDeque()

    private data class UndoEntry(val mediaId: Long, val trashPath: String?)

    init {
        observeFreedBytes()
    }

    private fun observeFreedBytes() {
        viewModelScope.launch {
            swipeRepo.observeFreedBytes().collect { bytes ->
                _state.update { it.copy(freedBytes = bytes) }
            }
        }
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, emptyMessage = null) }
            val prefs = prefsRepo.preferences.first()
            val excluded = swipeRepo.getExcludedIds(prefs.reappearAfterMillis)
            val photos = mediaRepo.queryPhotos(
                excludedIds = excluded,
                bucketId = prefs.selectedBucketId,
                limit = Int.MAX_VALUE
            )
            _state.update {
                it.copy(
                    photos = photos,
                    currentIndex = 0,
                    isLoading = false,
                    emptyMessage = if (photos.isEmpty()) {
                        "No quedan fotos por revisar. Puedes reiniciar la lista en Ajustes."
                    } else null
                )
            }
        }
    }

    fun keepCurrent() {
        val photo = _state.value.currentPhoto ?: return
        viewModelScope.launch {
            swipeRepo.recordKeep(photo.id, photo.sizeBytes)
            lastUndoStack.addLast(UndoEntry(photo.id, null))
            advance()
        }
    }

    fun trashCurrent() {
        val photo = _state.value.currentPhoto ?: return
        viewModelScope.launch {
            val trashPath = mediaRepo.copyToTrash(photo)
            swipeRepo.recordTrash(photo.id, photo.sizeBytes, trashPath)
            lastUndoStack.addLast(UndoEntry(photo.id, trashPath))
            advance()
        }
    }

    private fun advance() {
        _state.update {
            it.copy(
                currentIndex = it.currentIndex + 1,
                canUndo = true,
                emptyMessage = if (it.currentIndex + 1 >= it.photos.size) {
                    "Has revisado todas las fotos disponibles."
                } else null
            )
        }
    }

    fun undo() {
        viewModelScope.launch {
            val last = swipeRepo.undoLast() ?: return@launch
            if (last.action == SwipeRecord.ACTION_TRASH) {
                mediaRepo.deleteTrashCopy(last.trashUri)
            }
            lastUndoStack.removeLastOrNull()
            _state.update {
                it.copy(
                    currentIndex = (it.currentIndex - 1).coerceAtLeast(0),
                    canUndo = lastUndoStack.isNotEmpty(),
                    emptyMessage = null
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SwipeCleanerApplication
                SwipeViewModel(
                    app.container.mediaRepository,
                    app.container.swipeRepository,
                    app.container.preferencesRepository
                )
            }
        }
    }
}
