package com.example.analytics

import com.example.data.entity.CallLogEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CallStatsPeriod(
    val incomingCount: Int = 0,
    val incomingDurationSec: Int = 0,
    val outgoingCount: Int = 0,
    val outgoingDurationSec: Int = 0,
    val missedCount: Int = 0,
    val blockedSpamCount: Int = 0
) {
    val totalCount: Int get() = incomingCount + outgoingCount + missedCount + blockedSpamCount
    val totalDurationSec: Int get() = incomingDurationSec + outgoingDurationSec
    val incomingMinutes: Int get() = incomingDurationSec / 60
    val outgoingMinutes: Int get() = outgoingDurationSec / 60
    val totalMinutes: Int get() = totalDurationSec / 60
}

data class ContactCallSummary(
    val name: String,
    val number: String,
    val totalCalls: Int,
    val totalDurationSec: Int,
    val isWhatsAppUser: Boolean
)

object CallAnalyticsHelper {

    fun getStartOfDayTimestamp(calendar: Calendar = Calendar.getInstance()): Long {
        val cal = (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getEndOfDayTimestamp(calendar: Calendar = Calendar.getInstance()): Long {
        val cal = (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun getStartOfMonthTimestamp(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getTimestampsForDate(year: Int, month: Int, day: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
        }
        return Pair(getStartOfDayTimestamp(cal), getEndOfDayTimestamp(cal))
    }

    fun calculateStats(logs: List<CallLogEntity>, startTime: Long, endTime: Long = Long.MAX_VALUE): CallStatsPeriod {
        var incCount = 0
        var incDur = 0
        var outCount = 0
        var outDur = 0
        var missed = 0
        var spam = 0

        for (log in logs) {
            if (log.timestamp in startTime..endTime) {
                when (log.type) {
                    "INCOMING" -> {
                        incCount++
                        incDur += log.durationSeconds
                    }
                    "OUTGOING" -> {
                        outCount++
                        outDur += log.durationSeconds
                    }
                    "MISSED" -> {
                        missed++
                    }
                    "BLOCKED_SPAM" -> {
                        spam++
                    }
                }
            }
        }

        return CallStatsPeriod(
            incomingCount = incCount,
            incomingDurationSec = incDur,
            outgoingCount = outCount,
            outgoingDurationSec = outDur,
            missedCount = missed,
            blockedSpamCount = spam
        )
    }

    fun getTopContacts(logs: List<CallLogEntity>, limit: Int = 5, startTime: Long = 0L, endTime: Long = Long.MAX_VALUE): List<ContactCallSummary> {
        val filtered = logs.filter { it.timestamp in startTime..endTime }
        val group = filtered.groupBy { log ->
            val digits = log.number.replace("[^0-9]".toRegex(), "")
            if (digits.length >= 10) digits.takeLast(10) else digits
        }
        return group.map { (_, callList) ->
            val bestName = callList.firstOrNull { it.name.isNotBlank() && it.name != it.number }?.name
                ?: callList.firstOrNull()?.name?.ifEmpty { callList.first().number }
                ?: callList.first().number
            val canonicalNumber = callList.maxByOrNull { it.number.length }?.number ?: callList.first().number
            val totalSec = callList.sumOf { it.durationSeconds }
            val hasWhatsApp = callList.any { it.callMode == "WHATSAPP" }
            ContactCallSummary(
                name = bestName,
                number = canonicalNumber,
                totalCalls = callList.size,
                totalDurationSec = totalSec,
                isWhatsAppUser = hasWhatsApp
            )
        }
            .sortedByDescending { it.totalDurationSec }
            .take(limit)
    }

    fun formatDateOnly(timestamp: Long): String {
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
