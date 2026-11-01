package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.CallRecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRecordingDao {
    @Query("SELECT * FROM call_recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<CallRecordingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: CallRecordingEntity): Long

    @Delete
    suspend fun delete(recording: CallRecordingEntity)

    @Query("DELETE FROM call_recordings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE call_recordings SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String)

    @Query("SELECT COUNT(*) FROM call_recordings")
    suspend fun getCount(): Int
}
