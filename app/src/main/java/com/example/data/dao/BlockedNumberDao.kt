package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.BlockedNumberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedNumberDao {
    @Query("SELECT * FROM blocked_numbers ORDER BY addedTimestamp DESC")
    fun getAllBlockedNumbers(): Flow<List<BlockedNumberEntity>>

    @Query("SELECT * FROM blocked_numbers ORDER BY addedTimestamp DESC")
    suspend fun getAllBlockedNumbersSync(): List<BlockedNumberEntity>

    @Query("SELECT * FROM blocked_numbers WHERE phoneNumber = :number LIMIT 1")
    suspend fun findBlockedNumber(number: String): BlockedNumberEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE phoneNumber = :number)")
    suspend fun isNumberBlocked(number: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blocked: BlockedNumberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(blockedList: List<BlockedNumberEntity>)

    @Delete
    suspend fun delete(blocked: BlockedNumberEntity)

    @Query("DELETE FROM blocked_numbers WHERE phoneNumber = :number")
    suspend fun deleteByNumber(number: String)

    @Query("UPDATE blocked_numbers SET blockCount = blockCount + 1 WHERE phoneNumber = :number")
    suspend fun incrementBlockCount(number: String)
}
