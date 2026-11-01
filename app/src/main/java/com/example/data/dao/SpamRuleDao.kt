package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.SpamRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpamRuleDao {
    @Query("SELECT * FROM spam_rules")
    fun getAllRules(): Flow<List<SpamRuleEntity>>

    @Query("SELECT * FROM spam_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<SpamRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: SpamRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<SpamRuleEntity>)

    @Delete
    suspend fun delete(rule: SpamRuleEntity)

    @Query("UPDATE spam_rules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun toggleRule(id: Long, isEnabled: Boolean)
}
