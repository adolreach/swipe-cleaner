package com.swipecleaner.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SwipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SwipeRecord)

    @Update
    suspend fun update(record: SwipeRecord)

    @Query("DELETE FROM swipe_records WHERE mediaId = :mediaId")
    suspend fun deleteById(mediaId: Long)

    @Query("DELETE FROM swipe_records")
    suspend fun deleteAll()

    @Query("DELETE FROM swipe_records WHERE action = :action")
    suspend fun deleteByAction(action: String)

    @Query("SELECT mediaId FROM swipe_records WHERE action = 'KEEP' AND timestamp > :sinceMillis")
    suspend fun getActiveKeptIds(sinceMillis: Long): List<Long>

    @Query("SELECT * FROM swipe_records WHERE action = 'TRASH' AND originalDeleted = 0")
    fun observeTrash(): Flow<List<SwipeRecord>>

    @Query("SELECT * FROM swipe_records WHERE action = 'TRASH' AND originalDeleted = 0")
    suspend fun getTrash(): List<SwipeRecord>

    @Query("SELECT * FROM swipe_records WHERE mediaId = :mediaId LIMIT 1")
    suspend fun findById(mediaId: Long): SwipeRecord?

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM swipe_records WHERE action = 'TRASH' AND originalDeleted = 1")
    fun observeFreedBytes(): Flow<Long>

    @Query("SELECT * FROM swipe_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLast(): SwipeRecord?
}
