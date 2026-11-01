package com.example.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.example.data.AppDatabase
import com.example.data.entity.CallLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class RealCallLogRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val _systemCallLogs = MutableStateFlow<List<CallLogEntity>>(emptyList())
    val systemCallLogs: StateFlow<List<CallLogEntity>> = _systemCallLogs.asStateFlow()

    suspend fun syncRealCallLogs() = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _systemCallLogs.value = emptyList()
            return@withContext
        }

        val logs = mutableListOf<CallLogEntity>()

        try {
            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.PHONE_ACCOUNT_ID
            )

            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 150"
            )

            val availableSims = com.example.telecom.SimManager.getAvailableSims(context)

            cursor?.use {
                val idIdx = it.getColumnIndex(CallLog.Calls._ID)
                val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = it.getColumnIndex(CallLog.Calls.DURATION)
                val accountIdIdx = it.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getLong(idIdx) else 0L
                    val number = if (numIdx >= 0) it.getString(numIdx) ?: "অপরিচিত" else "অপরিচিত"
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "" else ""
                    val typeInt = if (typeIdx >= 0) it.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                    val date = if (dateIdx >= 0) it.getLong(dateIdx) else System.currentTimeMillis()
                    val duration = if (durIdx >= 0) it.getInt(durIdx) else 0
                    val accountId = if (accountIdIdx >= 0) it.getString(accountIdIdx) else null

                    val mappedType = when (typeInt) {
                        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                        CallLog.Calls.MISSED_TYPE -> "MISSED"
                        CallLog.Calls.REJECTED_TYPE, CallLog.Calls.BLOCKED_TYPE -> "BLOCKED_SPAM"
                        else -> "INCOMING"
                    }

                    // Resolve SIM slot & name using centralized SimAccountResolver
                    val resolvedSim = com.example.telecom.SimAccountResolver.resolveFromAccountId(accountId, context, number)
                    val simSlot = resolvedSim.slotIndex
                    val simName = resolvedSim.displayName

                    val resolvedName = if (name.isNotBlank() && name != number && name != "অপরিচিত") {
                        name
                    } else {
                        ContactLookupHelper.resolveContactName(context, number) ?: number
                    }

                    logs.add(
                        CallLogEntity(
                            id = id,
                            number = number,
                            name = resolvedName,
                            type = mappedType,
                            timestamp = date,
                            durationSeconds = duration,
                            callMode = "CELLULAR",
                            simSlot = simSlot,
                            simDisplayName = simName
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        _systemCallLogs.value = logs
    }
}
