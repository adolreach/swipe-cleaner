package com.swipecleaner.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registro de un swipe realizado sobre una foto.
 * - mediaId: ID del MediaStore (URI base = content://media/external/images/media/<id>)
 * - action: KEEP o TRASH
 * - timestamp: instante del swipe (millis)
 * - sizeBytes: tamaño de la foto en bytes (para el contador de espacio liberado)
 * - trashUri: si fue movida a la papelera interna, URI del fichero copiado
 * - originalDeleted: true si la copia original del MediaStore ya fue borrada definitivamente
 */
@Entity(tableName = "swipe_records")
data class SwipeRecord(
    @PrimaryKey val mediaId: Long,
    val action: String, // "KEEP" | "TRASH"
    val timestamp: Long,
    val sizeBytes: Long,
    val trashUri: String? = null,
    val originalDeleted: Boolean = false
) {
    companion object {
        const val ACTION_KEEP = "KEEP"
        const val ACTION_TRASH = "TRASH"
    }
}
