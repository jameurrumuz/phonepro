package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    suspend fun getAllCallLogsSync(): List<CallLogEntity>

    @Query("SELECT * FROM call_logs WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getCallsBetween(startTime: Long, endTime: Long): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE type = :type ORDER BY timestamp DESC")
    fun getCallsByType(type: String): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(callLog: CallLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(callLogs: List<CallLogEntity>)

    @Delete
    suspend fun delete(callLog: CallLogEntity)

    @Query("DELETE FROM call_logs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM call_logs")
    suspend fun getCallCount(): Int
}
