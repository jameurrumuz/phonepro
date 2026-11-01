package com.example.telecom

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object MessengerCallingHelper {

    private const val MESSENGER_PKG = "com.facebook.orca"
    private const val MESSENGER_LITE_PKG = "com.facebook.mlite"

    fun isMessengerInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return isPackageInstalled(pm, MESSENGER_PKG) || isPackageInstalled(pm, MESSENGER_LITE_PKG)
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getInstalledMessengerPackage(context: Context): String? {
        val pm = context.packageManager
        return when {
            isPackageInstalled(pm, MESSENGER_PKG) -> MESSENGER_PKG
            isPackageInstalled(pm, MESSENGER_LITE_PKG) -> MESSENGER_LITE_PKG
            else -> null
        }
    }

    /**
     * Opens Messenger app or profile link
     */
    fun openMessenger(context: Context, contactName: String = ""): Boolean {
        val pkg = getInstalledMessengerPackage(context)
        return try {
            if (pkg != null) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                    return true
                }
            }

            // Fallback to web Messenger
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://m.me/")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
