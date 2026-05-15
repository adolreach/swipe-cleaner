package com.swipecleaner.app.data

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Acceso al MediaStore y gestión de la papelera interna.
 *
 * Estrategia de papelera:
 *  - Al hacer "swipe trash", se copia el fichero a la carpeta privada de la app
 *    (context.filesDir/trash/). Esto NO requiere diálogo del sistema.
 *  - El registro queda en Room con originalDeleted = false.
 *  - El borrado real del original se realiza en lote desde la pantalla de papelera
 *    mediante MediaStore.createDeleteRequest (un único diálogo para todos).
 */
class MediaRepository(private val context: Context) {

    private val trashDir: File by lazy {
        File(context.filesDir, "trash").apply { mkdirs() }
    }

    suspend fun queryPhotos(
        excludedIds: Set<Long>,
        bucketId: String? = null,
        dateFromMillis: Long? = null,
        dateToMillis: Long? = null,
        limit: Int = 200
    ): List<MediaPhoto> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val selection = StringBuilder()
        val args = mutableListOf<String>()
        if (bucketId != null) {
            selection.append("${MediaStore.Images.Media.BUCKET_ID} = ?")
            args.add(bucketId)
        }
        if (dateFromMillis != null) {
            if (selection.isNotEmpty()) selection.append(" AND ")
            selection.append("${MediaStore.Images.Media.DATE_ADDED} >= ?")
            args.add((dateFromMillis / 1000).toString())
        }
        if (dateToMillis != null) {
            if (selection.isNotEmpty()) selection.append(" AND ")
            selection.append("${MediaStore.Images.Media.DATE_ADDED} <= ?")
            args.add((dateToMillis / 1000).toString())
        }

        // Más recientes primero (preferencia del usuario)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val result = mutableListOf<MediaPhoto>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        context.contentResolver.query(
            collection,
            projection,
            if (selection.isNotEmpty()) selection.toString() else null,
            if (args.isNotEmpty()) args.toTypedArray() else null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext() && result.size < limit) {
                val id = cursor.getLong(idCol)
                if (id in excludedIds) continue
                val uri = ContentUris.withAppendedId(collection, id)
                result.add(
                    MediaPhoto(
                        id = id,
                        uri = uri,
                        displayName = cursor.getString(nameCol) ?: "",
                        dateAdded = cursor.getLong(dateCol) * 1000L,
                        sizeBytes = cursor.getLong(sizeCol),
                        bucketName = cursor.getString(bucketNameCol)
                    )
                )
            }
        }
        result
    }

    suspend fun queryAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val counts = mutableMapOf<String, Pair<String, Int>>()
        context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(idCol) ?: continue
                val bucketName = cursor.getString(nameCol) ?: "Sin nombre"
                val current = counts[bucketId]
                counts[bucketId] = bucketName to ((current?.second ?: 0) + 1)
            }
        }
        counts.map { (id, pair) -> Album(id, pair.first, pair.second) }
            .sortedByDescending { it.count }
    }

    /**
     * Copia la foto al directorio interno de papelera (NO requiere diálogo).
     * Devuelve la URI del fichero copiado o null si falla.
     */
    suspend fun copyToTrash(photo: MediaPhoto): String? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(trashDir, "${photo.id}_${photo.displayName}")
            context.contentResolver.openInputStream(photo.uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Construye el IntentSender para borrar varios originales del MediaStore en lote.
     * Solo aplicable en Android 11+ (API 30). En versiones anteriores se usa el fallback.
     */
    fun buildDeleteRequest(mediaIds: List<Long>): IntentSender? {
        if (mediaIds.isEmpty()) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null

        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val uris = mediaIds.map { ContentUris.withAppendedId(collection, it) }
        return MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
    }

    /**
     * Borrado directo para Android 10 e inferiores (puede lanzar RecoverableSecurityException
     * en Android 10 que la UI deberá capturar).
     */
    suspend fun deleteOriginalLegacy(mediaId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val uri = ContentUris.withAppendedId(collection, mediaId)
            val rows = context.contentResolver.delete(uri, null, null)
            if (rows > 0) Result.success(Unit)
            else Result.failure(IllegalStateException("No se pudo borrar"))
        } catch (e: RecoverableSecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Borra el archivo copiado en la papelera interna (tras confirmar borrado del original).
     */
    suspend fun deleteTrashCopy(trashPath: String?) = withContext(Dispatchers.IO) {
        if (trashPath != null) {
            runCatching { File(trashPath).delete() }
        }
    }

    /**
     * Restaura una foto desde la papelera interna: simplemente borra la copia
     * (el original sigue en el MediaStore mientras originalDeleted = false).
     */
    suspend fun restoreFromTrash(trashPath: String?) = deleteTrashCopy(trashPath)
}
