package com.example.service

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UnreadBlockedSpamManager {
    private const val PREFS_NAME = "unread_blocked_spam_prefs"
    private const val KEY_UNREAD_COUNT = "unread_blocked_count"

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _unreadCount.value = prefs.getInt(KEY_UNREAD_COUNT, 0)
    }

    fun increment(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val newCount = prefs.getInt(KEY_UNREAD_COUNT, 0) + 1
        prefs.edit().putInt(KEY_UNREAD_COUNT, newCount).apply()
        _unreadCount.value = newCount
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_UNREAD_COUNT, 0).apply()
        _unreadCount.value = 0
    }
}
