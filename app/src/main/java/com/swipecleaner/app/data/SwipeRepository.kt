package com.swipecleaner.app.data

import kotlinx.coroutines.flow.Flow

class SwipeRepository(private val dao: SwipeDao) {

    suspend fun recordKeep(mediaId: Long, sizeBytes: Long) {
        dao.insert(
            SwipeRecord(
                mediaId = mediaId,
                action = SwipeRecord.ACTION_KEEP,
                timestamp = System.currentTimeMillis(),
                sizeBytes = sizeBytes
            )
        )
    }

    suspend fun recordTrash(mediaId: Long, sizeBytes: Long, trashUri: String?) {
        dao.insert(
            SwipeRecord(
                mediaId = mediaId,
                action = SwipeRecord.ACTION_TRASH,
                timestamp = System.currentTimeMillis(),
                sizeBytes = sizeBytes,
                trashUri = trashUri,
                originalDeleted = false
            )
        )
    }

    suspend fun markOriginalDeleted(mediaId: Long) {
        val record = dao.findById(mediaId) ?: return
        dao.update(record.copy(originalDeleted = true))
    }

    suspend fun undoLast(): SwipeRecord? {
        val last = dao.getLast() ?: return null
        dao.deleteById(last.mediaId)
        return last
    }

    /**
     * Devuelve los IDs de fotos que NO deben volver a aparecer.
     * Las fotos marcadas como KEEP reaparecen después de [reappearAfterMillis].
     * Las fotos en TRASH no reaparecen mientras estén en la papelera.
     */
    suspend fun getExcludedIds(reappearAfterMillis: Long): Set<Long> {
        val now = System.currentTimeMillis()
        val keepThreshold = now - reappearAfterMillis
        val activeKeeps = dao.getActiveKeptIds(keepThreshold)
        val trash = dao.getTrash().map { it.mediaId }
        return (activeKeeps + trash).toSet()
    }

    suspend fun resetKeptList() = dao.deleteByAction(SwipeRecord.ACTION_KEEP)
    suspend fun resetAll() = dao.deleteAll()

    fun observeTrash(): Flow<List<SwipeRecord>> = dao.observeTrash()
    suspend fun getTrash(): List<SwipeRecord> = dao.getTrash()
    fun observeFreedBytes(): Flow<Long> = dao.observeFreedBytes()
}
