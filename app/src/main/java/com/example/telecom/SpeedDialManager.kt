package com.example.telecom

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SpeedDialEntry(
    val digit: Char,
    val phoneNumber: String,
    val contactName: String
)

object SpeedDialManager {
    private const val PREFS_NAME = "speed_dial_prefs"

    private val _speedDialMap = MutableStateFlow<Map<Char, SpeedDialEntry>>(emptyMap())
    val speedDialMap: StateFlow<Map<Char, SpeedDialEntry>> = _speedDialMap.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val map = mutableMapOf<Char, SpeedDialEntry>()
        for (d in '1'..'9') {
            val num = prefs.getString("speed_dial_num_$d", null)
            val name = prefs.getString("speed_dial_name_$d", null) ?: num
            if (!num.isNullOrBlank()) {
                map[d] = SpeedDialEntry(d, num, name ?: num)
            }
        }
        // Pre-seed Key 1 to Voicemail/Emergency or favorite if empty
        if (!map.containsKey('1')) {
            map['1'] = SpeedDialEntry('1', "999", "জাতীয় জরুরি সেবা (৯৯৯)")
        }
        _speedDialMap.value = map
    }

    fun getSpeedDial(digit: Char): SpeedDialEntry? {
        return _speedDialMap.value[digit]
    }

    fun setSpeedDial(context: Context, digit: Char, number: String, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("speed_dial_num_$digit", number)
            .putString("speed_dial_name_$digit", name)
            .apply()
        val current = _speedDialMap.value.toMutableMap()
        current[digit] = SpeedDialEntry(digit, number, name)
        _speedDialMap.value = current
    }

    fun removeSpeedDial(context: Context, digit: Char) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("speed_dial_num_$digit")
            .remove("speed_dial_name_$digit")
            .apply()
        val current = _speedDialMap.value.toMutableMap()
        current.remove(digit)
        _speedDialMap.value = current
    }
}
