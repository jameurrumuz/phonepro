package com.example.telecom

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat

object WhatsAppCallingHelper {

    private const val WHATSAPP_PKG = "com.whatsapp"
    private const val WHATSAPP_BIZ_PKG = "com.whatsapp.w4b"

    fun isWhatsAppInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return isPackageInstalled(pm, WHATSAPP_PKG) || isPackageInstalled(pm, WHATSAPP_BIZ_PKG)
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getInstalledWhatsAppPackage(context: Context): String? {
        val pm = context.packageManager
        return when {
            isPackageInstalled(pm, WHATSAPP_PKG) -> WHATSAPP_PKG
            isPackageInstalled(pm, WHATSAPP_BIZ_PKG) -> WHATSAPP_BIZ_PKG
            else -> null
        }
    }

    /**
     * Checks if the number is a USSD / MMI code (e.g. *778#, *121#, *566#)
     */
    fun isUssdCode(rawNumber: String): Boolean {
        val trimmed = rawNumber.trim()
        return trimmed.contains("*") || trimmed.contains("#")
    }

    /**
     * Formats phone number for WhatsApp E.164 without leading plus or symbols.
     * Defaults local 11-digit Bangladesh numbers starting with 01 to 8801...
     */
    fun formatNumberForWhatsApp(rawNumber: String): String {
        val digitsOnly = rawNumber.replace("[^0-9+]".toRegex(), "")
        return when {
            digitsOnly.startsWith("+") -> digitsOnly.removePrefix("+")
            digitsOnly.startsWith("01") && digitsOnly.length == 11 -> "88$digitsOnly"
            digitsOnly.startsWith("880") -> digitsOnly
            else -> digitsOnly
        }
    }

    /**
     * Launch WhatsApp call/chat
     */
    fun openWhatsAppCall(context: Context, rawNumber: String): Boolean {
        // USSD codes cannot be called via WhatsApp
        if (isUssdCode(rawNumber)) {
            return false
        }

        val cleanNumber = formatNumberForWhatsApp(rawNumber)
        if (cleanNumber.isBlank()) return false

        val pkg = getInstalledWhatsAppPackage(context)

        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber")
                pkg?.let { setPackage(it) }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("WhatsAppCall", "Failed to open WhatsApp: ${e.message}")
            false
        }
    }

    /**
     * Cleans number while strictly preserving valid telecom characters:
     * '+' for international numbers, '*' and '#' for USSD, and digits.
     * Removes spaces, dashes, parentheses.
     */
    fun sanitizePhoneNumber(rawNumber: String): String {
        val trimmed = rawNumber.trim()
        val isUssd = trimmed.contains("*") || trimmed.contains("#")
        return if (isUssd) {
            trimmed.replace("[^0-9*#+,;]".toRegex(), "")
        } else {
            val hasPlus = trimmed.startsWith("+")
            val digits = trimmed.replace("[^0-9]".toRegex(), "")
            if (hasPlus) "+$digits" else digits
        }
    }

    /**
     * Launch standard cellular phone call or USSD code immediately.
     * Uses Uri.fromParts("tel", sanitized, null) which reliably formats
     * standard phone numbers (without converting + to %2B) and USSD codes.
     * Direct Intent.ACTION_CALL reliably instructs the radio/modem to place the real call.
     */
    fun launchCellularCall(context: Context, rawNumber: String, simInfo: SimAccountInfo? = null) {
        val sanitized = sanitizePhoneNumber(rawNumber)
        if (sanitized.isBlank()) return

        // Uri.fromParts properly preserves '+' for international numbers and encodes '#' as '%23' for USSD
        val telUri = Uri.fromParts("tel", sanitized, null)

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        // 1. Direct ACTION_CALL for zero-delay genuine cellular modem call
        if (hasCallPermission) {
            try {
                val callIntent = Intent(Intent.ACTION_CALL, telUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                SimManager.applySimExtrasToCallIntent(callIntent, simInfo)
                context.startActivity(callIntent)
                return
            } catch (e: Exception) {
                Log.e("Dialer", "ACTION_CALL failed, falling back to ACTION_DIAL: ${e.message}")
            }
        }

        // 2. Fallback to ACTION_DIAL if permission not granted or ACTION_CALL failed
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL, telUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            SimManager.applySimExtrasToCallIntent(dialIntent, simInfo)
            context.startActivity(dialIntent)
        } catch (e: Exception) {
            Log.e("Dialer", "ACTION_DIAL also failed: ${e.message}")
        }
    }
}
