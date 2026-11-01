package com.example.analytics

import android.content.Context
import android.net.TrafficStats
import java.util.Calendar

data class DataUsageInfo(
    val dailyMobileBytes: Long,
    val monthlyMobileBytes: Long,
    val dailyWifiBytes: Long,
    val monthlyWifiBytes: Long,
    val dailyLimitMb: Int = 2048,
    val monthlyLimitMb: Int = 30720
) {
    val dailyMobileMb: Double get() = dailyMobileBytes / (1024.0 * 1024.0)
    val monthlyMobileGb: Double get() = monthlyMobileBytes / (1024.0 * 1024.0 * 1024.0)
    val dailyWifiMb: Double get() = dailyWifiBytes / (1024.0 * 1024.0)
    val monthlyWifiGb: Double get() = monthlyWifiBytes / (1024.0 * 1024.0 * 1024.0)

    val dailyUsagePercent: Float
        get() = ((dailyMobileMb / dailyLimitMb) * 100).toFloat().coerceIn(0f, 100f)

    val monthlyUsagePercent: Float
        get() = (((monthlyMobileGb * 1024) / monthlyLimitMb) * 100).toFloat().coerceIn(0f, 100f)
}

class NetworkDataMonitor(private val context: Context) {

    private val prefs = context.getSharedPreferences("smart_dialer_real_data_usage", Context.MODE_PRIVATE)

    /**
     * Reads genuine hardware network traffic from Android TrafficStats without mock data.
     */
    fun getUsageInfo(): DataUsageInfo {
        val todayKey = getTodayKey()
        val monthKey = getMonthKey()

        // Real hardware network traffic counters
        val mobileRx = TrafficStats.getMobileRxBytes()
        val mobileTx = TrafficStats.getMobileTxBytes()
        val totalRx = TrafficStats.getTotalRxBytes()
        val totalTx = TrafficStats.getTotalTxBytes()

        val currentMobileTotal = if (mobileRx >= 0 && mobileTx >= 0) mobileRx + mobileTx else 0L
        val currentTotal = if (totalRx >= 0 && totalTx >= 0) totalRx + totalTx else 0L
        val currentWifiTotal = (currentTotal - currentMobileTotal).coerceAtLeast(0L)

        // Real Daily Mobile Data tracking
        val todayBaseMobile = prefs.getLong("base_mobile_$todayKey", -1L)
        val dailyMobile = if (todayBaseMobile == -1L) {
            prefs.edit().putLong("base_mobile_$todayKey", currentMobileTotal).apply()
            0L
        } else {
            (currentMobileTotal - todayBaseMobile).coerceAtLeast(0L)
        }

        // Real Monthly Mobile Data tracking
        val monthBaseMobile = prefs.getLong("base_mobile_$monthKey", -1L)
        val monthlyMobile = if (monthBaseMobile == -1L) {
            prefs.edit().putLong("base_mobile_$monthKey", currentMobileTotal).apply()
            0L
        } else {
            (currentMobileTotal - monthBaseMobile).coerceAtLeast(0L)
        }

        // Real Daily Wi-Fi Data tracking
        val todayBaseWifi = prefs.getLong("base_wifi_$todayKey", -1L)
        val dailyWifi = if (todayBaseWifi == -1L) {
            prefs.edit().putLong("base_wifi_$todayKey", currentWifiTotal).apply()
            0L
        } else {
            (currentWifiTotal - todayBaseWifi).coerceAtLeast(0L)
        }

        // Real Monthly Wi-Fi Data tracking
        val monthBaseWifi = prefs.getLong("base_wifi_$monthKey", -1L)
        val monthlyWifi = if (monthBaseWifi == -1L) {
            prefs.edit().putLong("base_wifi_$monthKey", currentWifiTotal).apply()
            0L
        } else {
            (currentWifiTotal - monthBaseWifi).coerceAtLeast(0L)
        }

        return DataUsageInfo(
            dailyMobileBytes = dailyMobile,
            monthlyMobileBytes = monthlyMobile,
            dailyWifiBytes = dailyWifi,
            monthlyWifiBytes = monthlyWifi
        )
    }

    private fun getTodayKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.MONTH)}_${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun getMonthKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.MONTH)}"
    }
}
