package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val number: String,
    val name: String,
    val type: String, // "INCOMING", "OUTGOING", "MISSED", "BLOCKED_SPAM"
    val timestamp: Long,
    val durationSeconds: Int,
    val callMode: String = "CELLULAR", // "WHATSAPP", "CELLULAR"
    val isRecorded: Boolean = false,
    val recordingPath: String? = null,
    val note: String? = null,
    val simSlot: Int = 0,
    val simDisplayName: String = "সিম ১"
)
