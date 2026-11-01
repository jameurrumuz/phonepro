package com.example.ui.dialogs

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.telecom.TelecomPermissionHelper
import com.example.telecom.TelephonyDiagnosticsManager
import com.example.ui.theme.PrimaryBlue

@Composable
fun TelephonyDiagnosticsDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val report by TelephonyDiagnosticsManager.report.collectAsState()

    LaunchedEffect(Unit) {
        TelephonyDiagnosticsManager.refreshDiagnostics(context)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryBlue.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "টেলিফোনি ও FSI ডায়াগনস্টিক",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${report.deviceManufacturer} ${report.deviceModel} • Android ${report.androidVersion}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { TelephonyDiagnosticsManager.refreshDiagnostics(context) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PrimaryBlue)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Xiaomi Warning Card if on Xiaomi / Redmi
                if (report.isXiaomiOrRedmi) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Xiaomi / Redmi HyperOS সিকিউরিটি নোটিশ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "শাওমি ফোনে লকস্ক্রিন ও ফুল-স্ক্রিন কল দেখাতে MIUI-এর 'Display pop-up windows while running in the background' এবং 'Show on Lock screen' অপশন চালু থাকা বাধ্যতামূলক।",
                                fontSize = 11.sp,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { TelephonyDiagnosticsManager.openXiaomiPermissionSettings(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("শাওমি পারমিশন সেটিংস খুলুন", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Core System Diagnostics List
                Text("১. সিস্টেম ও পারমিশন স্ট্যাটাস", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                Spacer(modifier = Modifier.height(6.dp))

                DiagnosticItemRow("ডিফল্ট ফোন অ্যাপ (Default Dialer)", report.isDefaultDialer, if (report.isDefaultDialer) "সক্রিয়" else "নিষ্ক্রিয়")
                DiagnosticItemRow("Full-Screen Intent (FSI)", report.canUseFullScreenIntent, if (report.canUseFullScreenIntent) "অনুমোদিত" else "ব্লকড")
                DiagnosticItemRow("নোটিফিকেশন অনুমতি", report.areNotificationsEnabled, if (report.areNotificationsEnabled) "অনুমোদিত" else "বন্ধ")
                DiagnosticItemRow("কল নোটিফিকেশন চ্যানেল", report.channelExists, "${report.channelImportanceLabel} (Bypass DND: ${report.channelCanBypassDnd})")
                DiagnosticItemRow("গুগল কন্ট্যাক্ট রিড পারমিশন", report.hasReadContactsPermission, if (report.hasReadContactsPermission) "অনুমোদিত" else "অনুমতি নেই")
                DiagnosticItemRow("কন্ট্যাক্ট রাইট পারমিশন", report.hasWriteContactsPermission, if (report.hasWriteContactsPermission) "অনুমোদিত" else "অনুমতি নেই")
                DiagnosticItemRow("Display over other apps", report.canDrawOverlays, if (report.canDrawOverlays) "অনুমোদিত" else "অনুমতি নেই")

                if (report.isXiaomiOrRedmi) {
                    val bgText = when (report.xiaomiBackgroundStartAllowed) {
                        true -> "অনুমোদিত"
                        false -> "ব্লকড"
                        null -> "অজ্ঞাত / চেক করুন"
                    }
                    DiagnosticItemRow("MIUI Background Pop-up", report.xiaomiBackgroundStartAllowed == true, bgText)

                    val lockText = when (report.xiaomiShowOnLockscreenAllowed) {
                        true -> "অনুমোদিত"
                        false -> "ব্লকড"
                        null -> "অজ্ঞাত / চেক করুন"
                    }
                    DiagnosticItemRow("MIUI Show on Lock Screen", report.xiaomiShowOnLockscreenAllowed == true, lockText)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Runtime Call & Intent Event Telemetry
                Text("২. সর্বশেষ ইনকামিং কল ও FSI রানটাইম ইভেন্ট", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                Spacer(modifier = Modifier.height(6.dp))

                TelemetryItemRow("সর্বশেষ রিং হওয়ার সময়", TelephonyDiagnosticsManager.formatTime(report.lastCallTime))
                TelemetryItemRow("কলার নম্বর (প্রাইভেসি ফিল্টার)", report.lastCallSanitizedNumber)
                TelemetryItemRow("টেলিফোনিক কল স্টেট", report.lastCallState)
                TelemetryItemRow("কল আসার সময় ডিভাইস লক ছিল?", if (report.lastCallIsLocked) "হ্যাঁ (Locked)" else "না (Unlocked)")
                TelemetryItemRow("সর্বশেষ FSI ডিসপ্যাচ মেথড", report.lastFsiMethod)
                TelemetryItemRow("IncomingCallActivity.onCreate()", TelephonyDiagnosticsManager.formatTime(report.lastActivityOnCreateTime))
                TelemetryItemRow("IncomingCallActivity.onNewIntent()", TelephonyDiagnosticsManager.formatTime(report.lastActivityOnNewIntentTime))

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Action Buttons
                Text("৩. সিস্টেম সেটিংস ফিক্স বাটনসমূহ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            TelecomPermissionHelper.requestDefaultDialer(context as Activity)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ডিফল্ট ডায়ালার", fontSize = 11.sp)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        OutlinedButton(
                            onClick = {
                                TelecomPermissionHelper.requestFullScreenIntentPermission(context)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("FSI অনুমতি", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            TelecomPermissionHelper.requestOverlayPermission(context)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ওভারলে অনুমতি", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            TelephonyDiagnosticsManager.openNotificationChannelSettings(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("চ্যানেল সেটিংস", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItemRow(label: String, isOk: Boolean, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isOk) Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isOk) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = detail,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isOk) Color(0xFF047857) else Color(0xFFB91C1C),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun TelemetryItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
