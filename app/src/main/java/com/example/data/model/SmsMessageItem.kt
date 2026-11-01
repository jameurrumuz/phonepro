package com.example.data.model

data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val contactName: String = "",
    val body: String,
    val timestamp: Long,
    val isIncoming: Boolean,
    val isRead: Boolean = true
)

data class SmsConversation(
    val threadId: Long,
    val address: String,
    val contactName: String = "",
    val snippet: String,
    val lastTimestamp: Long,
    val unreadCount: Int = 0,
    val messageCount: Int = 0
)
