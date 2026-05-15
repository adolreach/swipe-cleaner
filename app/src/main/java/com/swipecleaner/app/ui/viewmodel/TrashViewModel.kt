package com.swipecleaner.app.ui.viewmodel

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swipecleaner.app.SwipeCleanerApplication
import com.swipecleaner.app.data.MediaRepository
import com.swipecleaner.app.data.SwipeRecord
import com.swipecleaner.app.data.SwipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrashUiState(
    val items: List<SwipeRecord> = emptyList(),
    val pendingDeleteIds: List<Long> = emptyList()
)

class TrashViewModel(
    private val mediaRepo: MediaRepository,
    private val swipeRepo: SwipeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TrashUiState())
    val state: StateFlow<TrashUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            swipeRepo.observeTrash().collect { items ->
                _state.update { it.copy(items = items) }
            }
        }
    }

    /** Construye el IntentSender para que la UI lance la confirmación del sistema. */
    suspend fun buildEmptyTrashIntent(): IntentSender? {
        val ids = _state.value.items.map { it.mediaId }
        _state.update { it.copy(pendingDeleteIds = ids) }
        return mediaRepo.buildDeleteRequest(ids)
    }

    /** Llamar cuando el usuario haya aceptado el diálogo del sistema. */
    fun onDeleteConfirmed() {
        viewModelScope.launch {
            val pending = _state.value.pendingDeleteIds
            val items = _state.value.items.filter { it.mediaId in pending }
            items.forEach { rec ->
                mediaRepo.deleteTrashCopy(rec.trashUri)
                swipeRepo.markOriginalDeleted(rec.mediaId)
            }
            _state.update { it.copy(pendingDeleteIds = emptyList()) }
        }
    }

    fun onDeleteCancelled() {
        _state.update { it.copy(pendingDeleteIds = emptyList()) }
    }

    fun restoreSingle(record: SwipeRecord) {
        viewModelScope.launch {
            mediaRepo.restoreFromTrash(record.trashUri)
            // Borrado puntual del registro
            // Usamos un truco: insertar un KEEP en su lugar para que no reaparezca de inmediato.
            swipeRepo.recordKeep(record.mediaId, record.sizeBytes)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SwipeCleanerApplication
                TrashViewModel(
                    app.container.mediaRepository,
                    app.container.swipeRepository
                )
            }
        }
    }
}
