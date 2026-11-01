package com.example.telecom

import android.app.Activity
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.activity.result.ActivityResultLauncher

/**
 * Production-ready helper for managing Telecom permissions,
 * RoleManager (ROLE_DIALER, ROLE_SMS), and Android 14+ Full-Screen Intent settings.
 */
object TelecomPermissionHelper {

    /**
     * Checks if the app is allowed to use Full-Screen Intents (FSI).
     * On Android 14+ (API 34+), this can be revoked by the user or system.
     */
    fun canUseFullScreenIntent(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.canUseFullScreenIntent() ?: true
        } else {
            true
        }
    }

    /**
     * Directs the user to the system Settings page to grant USE_FULL_SCREEN_INTENT on Android 14+.
     */
    fun requestFullScreenIntentPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to app details settings
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    /**
     * Verifies if the app is currently the default phone dialer.
     * Uses RoleManager on Android 10+ (Q) and TelecomManager on older versions.
     */
    fun isDefaultDialer(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
            }
        }
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        return telecomManager?.defaultDialerPackage == context.packageName
    }

    /**
     * Requests the ROLE_DIALER role from the user via RoleManager (Android 10+)
     * or TelecomManager legacy intent (Android 9 and below).
     */
    fun requestDefaultDialer(activity: Activity, launcher: ActivityResultLauncher<Intent>? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                if (launcher != null) {
                    launcher.launch(intent)
                } else {
                    activity.startActivityForResult(intent, 1001)
                }
                return
            }
        }

        // Legacy fallback for Android 9 (Pie) and below
        @Suppress("DEPRECATION")
        val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
            putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, activity.packageName)
        }
        if (launcher != null) {
            launcher.launch(intent)
        } else {
            activity.startActivity(intent)
        }
    }

    /**
     * Verifies if the app is currently the default SMS application.
     */
    fun isDefaultSmsApp(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_SMS)
            }
        }
        return Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
    }

    /**
     * Requests the ROLE_SMS role from the user.
     */
    fun requestDefaultSms(activity: Activity, launcher: ActivityResultLauncher<Intent>? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS) && !roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                if (launcher != null) {
                    launcher.launch(intent)
                } else {
                    activity.startActivityForResult(intent, 1002)
                }
                return
            }
        }

        @Suppress("DEPRECATION")
        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
            putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.packageName)
        }
        if (launcher != null) {
            launcher.launch(intent)
        } else {
            activity.startActivity(intent)
        }
    }

    /**
     * Verifies if the app has the SYSTEM_ALERT_WINDOW (Draw over other apps) permission.
     * Essential for launching incoming call full-screen UI while the device is unlocked and in another app.
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Directs the user to the system Settings page to grant SYSTEM_ALERT_WINDOW permission.
     */
    fun requestOverlayPermission(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }
}
