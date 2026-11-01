package com.example.data.repository

import android.content.Context
import com.example.telecom.WhatsAppCallingHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ContactNoWhatsAppManager {
    private const val PREFS_NAME = "smart_dialer_wa_marks"
    private const val KEY_PREFIX = "no_wa_"

    private val _markedNumbers = MutableStateFlow<Set<String>>(emptySet())
    val markedNumbers: StateFlow<Set<String>> = _markedNumbers.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.all.keys
            .filter { it.startsWith(KEY_PREFIX) && prefs.getBoolean(it, false) }
            .map { it.removePrefix(KEY_PREFIX) }
            .toSet()
        _markedNumbers.value = set
    }

    fun isMarkedNoWhatsApp(context: Context, rawNumber: String): Boolean {
        val sanitized = WhatsAppCallingHelper.sanitizePhoneNumber(rawNumber)
        if (sanitized.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("$KEY_PREFIX$sanitized", false)
    }

    fun setMarkedNoWhatsApp(context: Context, rawNumber: String, isNoWhatsApp: Boolean) {
        val sanitized = WhatsAppCallingHelper.sanitizePhoneNumber(rawNumber)
        if (sanitized.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("$KEY_PREFIX$sanitized", isNoWhatsApp).apply()

        val current = _markedNumbers.value.toMutableSet()
        if (isNoWhatsApp) {
            current.add(sanitized)
        } else {
            current.remove(sanitized)
        }
        _markedNumbers.value = current
    }

    fun toggleMarkedNoWhatsApp(context: Context, rawNumber: String): Boolean {
        val newState = !isMarkedNoWhatsApp(context, rawNumber)
        setMarkedNoWhatsApp(context, rawNumber, newState)
        return newState
    }
}
