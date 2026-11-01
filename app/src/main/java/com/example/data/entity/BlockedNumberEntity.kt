package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_numbers")
data class BlockedNumberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val callerName: String = "",
    val reason: String = "স্প্যাম বা অবাঞ্ছিত নম্বর",
    val addedTimestamp: Long = System.currentTimeMillis(),
    val blockCount: Int = 0,
    val isSpam: Boolean = true
)
