package com.swipecleaner.app.data

import android.net.Uri

/**
 * Representación en memoria de una foto del MediaStore.
 */
data class MediaPhoto(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val sizeBytes: Long,
    val bucketName: String?
)

data class Album(
    val bucketId: String,
    val name: String,
    val count: Int
)
