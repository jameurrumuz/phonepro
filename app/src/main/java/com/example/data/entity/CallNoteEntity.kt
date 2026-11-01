package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_notes")
data class CallNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val callerName: String = "",
    val noteText: String,
    val timestamp: Long = System.currentTimeMillis()
)
