package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.widget.Toast
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
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.security.SecurityLockManager
import com.example.ui.dialogs.SecurityPinDialog
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
import androidx.compose.ui.window.Dialog
import com.example.data.repository.AutoBackupFrequency
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.ThemeMode
import com.example.ui.util.AppLanguage
import com.example.ui.viewmodel.DialerViewModel
import com.example.ui.viewmodel.MainTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: DialerViewModel,
    onNavigateToSecurityRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLang by viewModel.appLanguage.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val isDefaultDialer by viewModel.isDefaultDialer.collectAsState()
    val isDefaultSms by viewModel.isDefaultSmsApp.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val scheduledTime by viewModel.scheduledBackupTime.collectAsState()
    val backupStatusMessage by viewModel.backupStatusMessage.collectAsState()
    val missedReminderInterval by viewModel.missedCallReminderInterval.collectAsState()
    val strings = viewModel.strings()

    val isLockEnabled by SecurityLockManager.isLockEnabled.collectAsState()
    val lockedTabs by SecurityLockManager.lockedTabs.collectAsState()
    var showSetPinDialog by remember { mutableStateOf(false) }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = strings.settingsTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        // Section 1: Language Switcher (বাংলা ও ইংরেজি)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.languageSection,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LanguageSelectChip(
                            title = "বাংলা (Bengali)",
                            isSelected = currentLang == AppLanguage.BANGLA,
                            onClick = { viewModel.setAppLanguage(AppLanguage.BANGLA) },
                            modifier = Modifier.weight(1f).testTag("lang_bangla_chip")
                        )

                        LanguageSelectChip(
                            title = "English",
                            isSelected = currentLang == AppLanguage.ENGLISH,
                            onClick = { viewModel.setAppLanguage(AppLanguage.ENGLISH) },
                            modifier = Modifier.weight(1f).testTag("lang_english_chip")
                        )
                    }
                }
            }
        }

        // Section 2: Display & Theme (লাইট ও ডার্ক থিম)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.themeSection,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionChip(
                            title = strings.themeLight,
                            icon = Icons.Default.LightMode,
                            isSelected = themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f).testTag("theme_light_chip")
                        )

                        ThemeOptionChip(
                            title = strings.themeDark,
                            icon = Icons.Default.DarkMode,
                            isSelected = themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f).testTag("theme_dark_chip")
                        )

                        ThemeOptionChip(
                            title = strings.themeSystem,
                            icon = Icons.Default.Settings,
                            isSelected = themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            modifier = Modifier.weight(1f).testTag("theme_system_chip")
                        )
                    }
                }
            }
        }

        // Section: App & Feature Lock (পিন/পাসওয়ার্ড ও সিলেক্টেড ফিচার লক)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth().testTag("feature_lock_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "অ্যাপ ও ফিচার নিরাপত্তা লক",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "পিন দিয়ে নির্বাচিত ফিচার সুরক্ষিত রাখুন",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isLockEnabled,
                            onCheckedChange = { enabled ->
                                SecurityLockManager.setLockEnabled(context, enabled)
                            },
                            modifier = Modifier.testTag("toggle_feature_lock_switch")
                        )
                    }

                    if (isLockEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "লক করার ফিচারসমূহ নির্বাচন করুন:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            TextButton(
                                onClick = { showSetPinDialog = true },
                                modifier = Modifier.testTag("change_pin_button")
                            ) {
                                Text("পিন পরিবর্তন", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val featureList = listOf(
                            Triple(MainTab.RECENTS, "কল হিস্টোরি (Call History)", "recents"),
                            Triple(MainTab.MESSAGES, "মেসেজ (SMS Messages)", "messages"),
                            Triple(MainTab.DIALER, "ডায়াল প্যাড (Dialer)", "dialer"),
                            Triple(MainTab.CONTACTS, "কন্ট্যাক্টস তালিকা (Contacts)", "contacts"),
                            Triple(MainTab.ANALYTICS, "কল এ্যানালাইটিক্স (Analytics)", "analytics"),
                            Triple(MainTab.SECURITY_RECORD, "স্প্যাম ও কল রেকর্ডস (Spam & Records)", "security_record"),
                            Triple(MainTab.SETTINGS, "অ্যাপ সেটিংস পেজ (Settings)", "settings")
                        )

                        for ((tab, label, tag) in featureList) {
                            val isChecked = lockedTabs.contains(tab.name)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SecurityLockManager.setTabLock(context, tab, !isChecked)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        SecurityLockManager.setTabLock(context, tab, checked)
                                    },
                                    modifier = Modifier.testTag("lock_checkbox_$tag")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Data Backup & Restore (লোকাল ও গুগল ড্রাইভ ব্যাকআপ ১০০% কার্যকরী)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.backupSection,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Google Drive Auto-Backup Frequency Picker
                    Text(
                        text = strings.autoBackupFrequency,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val frequencies = listOf(
                            AutoBackupFrequency.OFF,
                            AutoBackupFrequency.HOURS_6,
                            AutoBackupFrequency.DAILY,
                            AutoBackupFrequency.WEEKLY
                        )

                        for (freq in frequencies) {
                            val label = if (currentLang == AppLanguage.BANGLA) freq.labelBn else freq.labelEn
                            val isSelected = backupState.frequency == freq
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setAutoBackupFrequency(freq) },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Backup Timestamps Status Box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${strings.lastBackup}:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (backupState.lastBackupTimestamp > 0) formatTimestamp(backupState.lastBackupTimestamp) else "এখনও ব্যাকআপ নেওয়া হয়নি",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (backupState.frequency != AutoBackupFrequency.OFF) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${strings.nextBackup}:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = formatTimestamp(backupState.nextScheduledTimestamp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Scheduled Specific Backup Time
                    Text(
                        text = strings.scheduledBackupTime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val times = listOf("02:00 AM", "04:00 AM", "12:00 PM", "08:00 PM")
                        for (time in times) {
                            FilterChip(
                                selected = scheduledTime == time,
                                onClick = { viewModel.setScheduledBackupTime(time) },
                                label = { Text(time, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Instant Backup Now (Direct 1-Click Backup)
                    Button(
                        onClick = { viewModel.performDirectBackupNow() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("direct_backup_now_btn")
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (currentLang == AppLanguage.BANGLA) "এখনই ব্যাকআপ করুন (Backup Now)" else "Backup Now", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (backupStatusMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = backupStatusMessage ?: "",
                                    fontSize = 11.sp,
                                    color = Color(0xFF047857),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearBackupStatusMessage() }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Button: Google Drive Backup Now (Share/Browse)
                    OutlinedButton(
                        onClick = {
                            viewModel.launchGoogleDriveBackup { intent ->
                                try {
                                    context.startActivity(Intent.createChooser(intent, strings.backupNowGoogleDrive))
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Google Drive", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("backup_google_drive_btn")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.backupNowGoogleDrive, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Local Backup and Restore buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.createLocalBackup { file ->
                                    if (file != null) {
                                        Toast.makeText(context, "লোকাল ব্যাকআপ সম্পন্ন: ${file.name}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.createLocalBackup, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.restoreLocalBackup, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Section 4: Default Applications Configuration
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.defaultAppsSection,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Default Phone App Row
                    DefaultAppRow(
                        icon = Icons.Default.Phone,
                        title = strings.defaultPhoneApp,
                        isDefault = isDefaultDialer,
                        isDefaultLabel = strings.isDefault,
                        setAsDefaultLabel = strings.setAsDefault,
                        onSetDefault = {
                            (context as? Activity)?.let {
                                viewModel.requestSetDefaultDialer(it)
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Default SMS App Row
                    DefaultAppRow(
                        icon = Icons.Default.Message,
                        title = strings.defaultSmsApp,
                        isDefault = isDefaultSms,
                        isDefaultLabel = strings.isDefault,
                        setAsDefaultLabel = strings.setAsDefault,
                        onSetDefault = {
                            (context as? Activity)?.let {
                                viewModel.requestSetDefaultSms(it)
                            }
                        }
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        val canUseFsi by viewModel.canUseFullScreenIntent.collectAsState()
                        DefaultAppRow(
                            icon = Icons.Default.Notifications,
                            title = "ফুল-স্ক্রিন কল পারমিশন (FSI)",
                            isDefault = canUseFsi,
                            isDefaultLabel = "অনুমোদিত",
                            setAsDefaultLabel = "অনুমতি দিন",
                            onSetDefault = {
                                viewModel.requestFullScreenIntentPermission(context)
                            }
                        )
                    }
                }
            }
        }

        // Section: Telephony & FSI Diagnostics (Xiaomi / Redmi Android 15 Tool)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth().testTag("telephony_diagnostics_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "টেলিফোনি ও FSI ডায়াগনস্টিক",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Xiaomi / Redmi Android 15 রানটাইম অডিট রিপোর্ট",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { showDiagnosticsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("রিপোর্ট দেখুন", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Section: Missed Call Reminders
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.missedCallReminderSection,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.missedCallReminderDesc,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val intervals = listOf(
                            0 to if (currentLang == AppLanguage.BANGLA) "বন্ধ" else "Off",
                            5 to if (currentLang == AppLanguage.BANGLA) "৫ মিনিট" else "5 Min",
                            15 to if (currentLang == AppLanguage.BANGLA) "১৫ মিনিট" else "15 Min",
                            30 to if (currentLang == AppLanguage.BANGLA) "৩০ মিনিট" else "30 Min",
                            60 to if (currentLang == AppLanguage.BANGLA) "১ ঘণ্টা" else "1 Hour"
                        )

                        for ((mins, label) in intervals) {
                            FilterChip(
                                selected = missedReminderInterval == mins,
                                onClick = { viewModel.setMissedCallReminderInterval(mins) },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Section 5: Spam Rules & Call Recordings shortcut
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSecurityRecord() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.spamAndRecordingsTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.spamAndRecordingsDesc,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "Open",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // JSON Restore Dialog
    if (showRestoreDialog) {
        RestoreBackupDialog(
            onDismiss = { showRestoreDialog = false },
            onRestore = { json ->
                viewModel.restoreFromJson(json) { success ->
                    if (success) {
                        Toast.makeText(context, "সফলভাবে ডাটা রিস্টোর সম্পন্ন হয়েছে", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "রিস্টোর ব্যর্থ হয়েছে, সঠিক JSON ফাইল প্রদান করুন", Toast.LENGTH_SHORT).show()
                    }
                    showRestoreDialog = false
                }
            }
        )
    }

    // Set / Change PIN Dialog
    if (showSetPinDialog) {
        SecurityPinDialog(
            title = "নতুন পিন কোড সেট করুন",
            subtitle = "লক করা ফিচারসমূহ আনলক করার জন্য ৪ সংখ্যার পিন দিন:",
            onPinEntered = { entered ->
                if (entered.length in 4..6) {
                    SecurityLockManager.setPin(context, entered)
                    Toast.makeText(context, "নতুন পিন কোড সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
                    true
                } else false
            },
            onSuccess = { showSetPinDialog = false },
            onDismiss = { showSetPinDialog = false }
        )
    }

    if (showDiagnosticsDialog) {
        com.example.ui.dialogs.TelephonyDiagnosticsDialog(
            isOpen = showDiagnosticsDialog,
            onDismiss = { showDiagnosticsDialog = false }
        )
    }
}

@Composable
private fun LanguageSelectChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ThemeOptionChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DefaultAppRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isDefault: Boolean,
    isDefaultLabel: String,
    setAsDefaultLabel: String,
    onSetDefault: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        if (isDefault) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF10B981).copy(alpha = 0.15f)
            ) {
                Text(
                    text = "✓ $isDefaultLabel",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        } else {
            Button(
                onClick = onSetDefault,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = setAsDefaultLabel, fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun RestoreBackupDialog(
    onDismiss: () -> Unit,
    onRestore: (String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "ব্যাকআপ ডাটা রিস্টোর",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ব্যাকআপ JSON ফাইলের কনটেন্ট এখানে পেস্ট করুন:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    placeholder = { Text("{\"version\": 1, \"callLogs\": [...]}") },
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (jsonText.isNotBlank()) {
                                onRestore(jsonText.trim())
                            }
                        },
                        enabled = jsonText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("রিস্টোর করুন", color = Color.White)
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
}
