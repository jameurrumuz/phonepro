package com.example.telecom

import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.service.SmartInCallService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TelephonyDiagnosticReport(
    val timestamp: Long = System.currentTimeMillis(),
    val deviceManufacturer: String = Build.MANUFACTURER,
    val deviceModel: String = Build.MODEL,
    val androidVersion: String = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    val isXiaomiOrRedmi: Boolean = false,
    val isDefaultDialer: Boolean = false,
    val canUseFullScreenIntent: Boolean = false,
    val areNotificationsEnabled: Boolean = false,
    val channelExists: Boolean = false,
    val channelImportance: Int = 0,
    val channelImportanceLabel: String = "Unknown",
    val channelCanBypassDnd: Boolean = false,
    val channelLockscreenVisibility: Int = -1,
    val hasReadContactsPermission: Boolean = false,
    val hasWriteContactsPermission: Boolean = false,
    val canDrawOverlays: Boolean = false,
    val xiaomiBackgroundStartAllowed: Boolean? = null,
    val xiaomiShowOnLockscreenAllowed: Boolean? = null,
    val lastCallTime: Long = 0L,
    val lastCallSanitizedNumber: String = "None",
    val lastCallState: String = "IDLE",
    val lastCallIsLocked: Boolean = false,
    val lastFsiDispatchTime: Long = 0L,
    val lastFsiMethod: String = "None",
    val lastActivityOnCreateTime: Long = 0L,
    val lastActivityOnNewIntentTime: Long = 0L
)

object TelephonyDiagnosticsManager {

    private val _report = MutableStateFlow(TelephonyDiagnosticReport())
    val report: StateFlow<TelephonyDiagnosticReport> = _report.asStateFlow()

    private var lastCallTime = 0L
    private var lastCallSanitizedNumber = "None"
    private var lastCallState = "IDLE"
    private var lastCallIsLocked = false
    private var lastFsiDispatchTime = 0L
    private var lastFsiMethod = "None"
    private var lastActivityOnCreateTime = 0L
    private var lastActivityOnNewIntentTime = 0L

    fun isXiaomiDevice(): Boolean {
        val man = Build.MANUFACTURER.lowercase(Locale.ROOT)
        return man.contains("xiaomi") || man.contains("redmi") || man.contains("poco")
    }

    /**
     * Checks Xiaomi / MIUI proprietary AppOps permissions via reflection safely.
     * OP 10021 = OP_BACKGROUND_START_ACTIVITY (Display pop-up windows while running in the background)
     * OP 10020 = OP_SHOW_WHEN_LOCKED (Show on Lock screen)
     */
    private fun checkXiaomiAppOp(context: Context, opCode: Int): Boolean? {
        if (!isXiaomiDevice()) return null
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return null
            val method = appOps.javaClass.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            val result = method.invoke(appOps, opCode, Process.myUid(), context.packageName) as Int
            result == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            null
        }
    }

    fun recordIncomingCall(number: String, state: String, isLocked: Boolean) {
        lastCallTime = System.currentTimeMillis()
        lastCallSanitizedNumber = sanitizeNumber(number)
        lastCallState = state
        lastCallIsLocked = isLocked
    }

    fun recordFsiDispatch(method: String) {
        lastFsiDispatchTime = System.currentTimeMillis()
        lastFsiMethod = method
    }

    fun recordActivityOnCreate() {
        lastActivityOnCreateTime = System.currentTimeMillis()
    }

    fun recordActivityOnNewIntent() {
        lastActivityOnNewIntentTime = System.currentTimeMillis()
    }

    private fun sanitizeNumber(rawNumber: String): String {
        val clean = rawNumber.trim()
        if (clean.length <= 4) return clean
        val prefix = clean.take(4)
        val suffix = clean.takeLast(2)
        return "$prefix***$suffix"
    }

    /**
     * Runs full system diagnostics and updates the report state.
     */
    fun refreshDiagnostics(context: Context): TelephonyDiagnosticReport {
        val appContext = context.applicationContext
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        val isDefault = TelecomPermissionHelper.isDefaultDialer(appContext)

        val canFsi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            notificationManager?.canUseFullScreenIntent() ?: true
        } else {
            true
        }

        val notificationsEnabled = notificationManager?.areNotificationsEnabled() ?: false

        var channelExists = false
        var channelImportance = 0
        var channelImportanceLabel = "Missing"
        var channelBypassDnd = false
        var channelLockscreenVisibility = -1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            val channel = notificationManager.getNotificationChannel(SmartInCallService.CHANNEL_ID)
            if (channel != null) {
                channelExists = true
                channelImportance = channel.importance
                channelBypassDnd = channel.canBypassDnd()
                channelLockscreenVisibility = channel.lockscreenVisibility
                channelImportanceLabel = when (channel.importance) {
                    NotificationManager.IMPORTANCE_HIGH -> "HIGH (Heads-Up & FSI Allowed)"
                    NotificationManager.IMPORTANCE_MAX -> "MAX"
                    NotificationManager.IMPORTANCE_DEFAULT -> "DEFAULT (No Full-Screen)"
                    NotificationManager.IMPORTANCE_LOW -> "LOW (Silent)"
                    NotificationManager.IMPORTANCE_MIN -> "MIN (Collapsed)"
                    NotificationManager.IMPORTANCE_NONE -> "DISABLED"
                    else -> "Level $channelImportance"
                }
            }
        }

        val hasReadContacts = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val hasWriteContacts = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val canDrawOverlays = Settings.canDrawOverlays(appContext)

        val xiaomiBg = checkXiaomiAppOp(appContext, 10021)
        val xiaomiLock = checkXiaomiAppOp(appContext, 10020)

        val updated = TelephonyDiagnosticReport(
            timestamp = System.currentTimeMillis(),
            deviceManufacturer = Build.MANUFACTURER,
            deviceModel = Build.MODEL,
            androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            isXiaomiOrRedmi = isXiaomiDevice(),
            isDefaultDialer = isDefault,
            canUseFullScreenIntent = canFsi,
            areNotificationsEnabled = notificationsEnabled,
            channelExists = channelExists,
            channelImportance = channelImportance,
            channelImportanceLabel = channelImportanceLabel,
            channelCanBypassDnd = channelBypassDnd,
            channelLockscreenVisibility = channelLockscreenVisibility,
            hasReadContactsPermission = hasReadContacts,
            hasWriteContactsPermission = hasWriteContacts,
            canDrawOverlays = canDrawOverlays,
            xiaomiBackgroundStartAllowed = xiaomiBg,
            xiaomiShowOnLockscreenAllowed = xiaomiLock,
            lastCallTime = lastCallTime,
            lastCallSanitizedNumber = lastCallSanitizedNumber,
            lastCallState = lastCallState,
            lastCallIsLocked = lastCallIsLocked,
            lastFsiDispatchTime = lastFsiDispatchTime,
            lastFsiMethod = lastFsiMethod,
            lastActivityOnCreateTime = lastActivityOnCreateTime,
            lastActivityOnNewIntentTime = lastActivityOnNewIntentTime
        )

        _report.value = updated
        return updated
    }

    /**
     * Opens Xiaomi / Redmi custom permission manager for "Display pop-up windows" and "Show on Lock screen"
     */
    fun openXiaomiPermissionSettings(context: Context) {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
            setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
            putExtra("extra_pkgname", context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    putExtra("extra_pkgname", context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
            } catch (_: Exception) {
                val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(appDetails)
            }
        }
    }

    fun openNotificationChannelSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, SmartInCallService.CHANNEL_ID)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                val appDetails = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(appDetails)
            }
        }
    }

    fun formatTime(timestamp: Long): String {
        if (timestamp <= 0) return "Never"
        val sdf = SimpleDateFormat("HH:mm:ss dd MMM", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
