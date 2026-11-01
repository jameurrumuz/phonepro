package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.CallNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: CallNoteEntity): Long

    @Update
    suspend fun update(note: CallNoteEntity)

    @Delete
    suspend fun delete(note: CallNoteEntity)

    @Query("SELECT * FROM call_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<CallNoteEntity>>

    @Query("SELECT * FROM call_notes ORDER BY timestamp DESC")
    suspend fun getAllNotesSync(): List<CallNoteEntity>

    @Query("SELECT * FROM call_notes WHERE phoneNumber = :phoneNumber ORDER BY timestamp DESC")
    fun getNotesForNumber(phoneNumber: String): Flow<List<CallNoteEntity>>

    @Query("SELECT * FROM call_notes WHERE phoneNumber = :phoneNumber ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestNoteForNumber(phoneNumber: String): CallNoteEntity?

    @Query("DELETE FROM call_notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
