package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spam_rules")
data class SpamRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pattern: String,
    val ruleType: String = "PREFIX", // "PREFIX", "KEYWORD"
    val description: String = "",
    val isEnabled: Boolean = true
)
