package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.analytics.CallAnalyticsHelper
import com.example.analytics.CallStatsPeriod
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SpamRed
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.viewmodel.DialerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AnalyticsViewMode {
    DAILY,
    MONTHLY,
    BY_DATE
}

@Composable
fun AnalyticsScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dailyStats by viewModel.dailyStats.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val dateStats by viewModel.dateStats.collectAsState()
    val selectedDateTs by viewModel.selectedAnalyticsDate.collectAsState()
    val dataUsage by viewModel.dataUsage.collectAsState()
    val topContacts by viewModel.topContacts.collectAsState()
    val dateTopContacts by viewModel.dateTopContacts.collectAsState()
    val strings = viewModel.strings()

    var viewMode by remember { mutableStateOf(AnalyticsViewMode.DAILY) }

    val activeStats = when (viewMode) {
        AnalyticsViewMode.DAILY -> dailyStats
        AnalyticsViewMode.MONTHLY -> monthlyStats
        AnalyticsViewMode.BY_DATE -> dateStats
    }

    val activeTopContacts = when (viewMode) {
        AnalyticsViewMode.BY_DATE -> dateTopContacts
        else -> topContacts
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.analyticsTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.analyticsSubtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.syncCallLogs()
                        viewModel.refreshDataUsage()
                    },
                    modifier = Modifier.testTag("refresh_analytics_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Section 1: Call Analytics with Daily, Monthly, and Date-wise (তারিখ অনুযায়ী)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.callSummary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Toggle: Daily, Monthly, By Date
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = viewMode == AnalyticsViewMode.DAILY,
                                onClick = { viewMode = AnalyticsViewMode.DAILY },
                                label = { Text(strings.daily, fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = viewMode == AnalyticsViewMode.MONTHLY,
                                onClick = { viewMode = AnalyticsViewMode.MONTHLY },
                                label = { Text(strings.monthly, fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = viewMode == AnalyticsViewMode.BY_DATE,
                                onClick = { viewMode = AnalyticsViewMode.BY_DATE },
                                label = { Text(strings.byDate, fontSize = 10.sp) }
                            )
                        }
                    }

                    // Date selector row when "তারিখ অনুযায়ী" is active
                    AnimatedVisibility(visible = viewMode == AnalyticsViewMode.BY_DATE) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Previous Day Button
                                    IconButton(
                                        onClick = {
                                            val cal = Calendar.getInstance().apply { timeInMillis = selectedDateTs }
                                            cal.add(Calendar.DAY_OF_YEAR, -1)
                                            viewModel.setSelectedAnalyticsDate(cal.timeInMillis)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Previous Day",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Date Picker Clickable Text
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val cal = Calendar.getInstance().apply { timeInMillis = selectedDateTs }
                                                DatePickerDialog(
                                                    context,
                                                    { _, year, month, dayOfMonth ->
                                                        val selectedCal = Calendar.getInstance().apply {
                                                            set(year, month, dayOfMonth)
                                                        }
                                                        viewModel.setSelectedAnalyticsDate(selectedCal.timeInMillis)
                                                    },
                                                    cal.get(Calendar.YEAR),
                                                    cal.get(Calendar.MONTH),
                                                    cal.get(Calendar.DAY_OF_MONTH)
                                                ).show()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = formatSelectedDate(selectedDateTs),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    // Next Day Button
                                    IconButton(
                                        onClick = {
                                            val cal = Calendar.getInstance().apply { timeInMillis = selectedDateTs }
                                            cal.add(Calendar.DAY_OF_YEAR, 1)
                                            viewModel.setSelectedAnalyticsDate(cal.timeInMillis)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Next Day",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4 Stat Cards Grid (Authentic Data)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Outgoing
                        CallMetricBox(
                            icon = Icons.AutoMirrored.Filled.CallMade,
                            iconColor = PrimaryBlue,
                            title = strings.outgoingCalls,
                            count = "${activeStats.outgoingCount} বার",
                            duration = "${activeStats.outgoingMinutes} মিনিট",
                            modifier = Modifier.weight(1f)
                        )

                        // Incoming
                        CallMetricBox(
                            icon = Icons.AutoMirrored.Filled.CallReceived,
                            iconColor = Color(0xFF10B981),
                            title = strings.incomingCalls,
                            count = "${activeStats.incomingCount} বার",
                            duration = "${activeStats.incomingMinutes} মিনিট",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Missed
                        CallMetricBox(
                            icon = Icons.AutoMirrored.Filled.CallMissed,
                            iconColor = SpamRed,
                            title = strings.missed,
                            count = "${activeStats.missedCount} বার",
                            duration = "উত্তরহীন",
                            modifier = Modifier.weight(1f)
                        )

                        // Blocked Spam
                        CallMetricBox(
                            icon = Icons.Default.Block,
                            iconColor = Color(0xFF8B5CF6),
                            title = strings.spamProtected,
                            count = "${activeStats.blockedSpamCount} বার",
                            duration = "ব্লককৃত কল",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Incoming vs Outgoing Visual Ratio Bar
                    val totalSec = (activeStats.incomingDurationSec + activeStats.outgoingDurationSec).coerceAtLeast(1)
                    val outRatio = activeStats.outgoingDurationSec.toFloat() / totalSec

                    Text(
                        text = strings.outVsInRatio,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { outRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = PrimaryBlue,
                        trackColor = Color(0xFF10B981)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${strings.outgoingCalls}: ${(outRatio * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${strings.incomingCalls}: ${((1 - outRatio) * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Section 2: Data Usage Monitor
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DataUsage,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.dataUsageInfo,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.dataUsageDesc,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Daily Mobile Data Meter
                    DataProgressRow(
                        icon = Icons.Default.NetworkCell,
                        iconTint = Color(0xFF0284C7),
                        title = strings.todayMobileData,
                        usedText = "${String.format("%.1f", dataUsage.dailyMobileMb)} MB",
                        limitText = "${dataUsage.dailyLimitMb} MB",
                        progress = dataUsage.dailyUsagePercent / 100f,
                        progressColor = PrimaryBlue
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Monthly Mobile Data Meter
                    DataProgressRow(
                        icon = Icons.Default.NetworkCell,
                        iconTint = Color(0xFF6366F1),
                        title = strings.monthlyMobileData,
                        usedText = "${String.format("%.2f", dataUsage.monthlyMobileGb)} GB",
                        limitText = "${dataUsage.monthlyLimitMb / 1024} GB",
                        progress = dataUsage.monthlyUsagePercent / 100f,
                        progressColor = Color(0xFF6366F1)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Wi-Fi Usage Meter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = WhatsAppDarkGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.todayWifi, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${String.format("%.1f", dataUsage.dailyWifiMb)} MB",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = WhatsAppDarkGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.monthlyWifi, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${String.format("%.2f", dataUsage.monthlyWifiGb)} GB",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Top Contacted Persons
        if (activeTopContacts.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.topContacts,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        for (contact in activeTopContacts) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = contact.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${contact.totalCalls} বার কল",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${contact.totalDurationSec / 60} মিনিট",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun CallMetricBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    count: String,
    duration: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = duration,
                fontSize = 11.sp,
                color = iconColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DataProgressRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    usedText: String,
    limitText: String,
    progress: Float,
    progressColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "$usedText / $limitText",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun formatSelectedDate(timestamp: Long): String {
    val todayStart = CallAnalyticsHelper.getStartOfDayTimestamp()
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val thisStart = CallAnalyticsHelper.getStartOfDayTimestamp(cal)

    return when (thisStart) {
        todayStart -> "আজ (${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))})"
        todayStart - 86400000L -> "গতকাল (${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))})"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
