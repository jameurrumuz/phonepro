package com.example.telecom

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.content.ContextCompat

data class SimAccountInfo(
    val slotIndex: Int, // 0 for SIM 1, 1 for SIM 2
    val displayName: String,
    val carrierName: String,
    val subscriptionId: Int,
    val phoneAccountHandle: PhoneAccountHandle? = null,
    val isPreferred: Boolean = false
)

object SimManager {
    private const val PREFS_NAME = "smart_dialer_sim_prefs"
    private const val PREF_KEY_PREFIX = "pref_sim_"

    /**
     * Retrieves available SIM cards. If permissions are missing or emulator has no physical SIMs,
     * returns default SIM 1 and SIM 2 options with recognizable Bangladeshi carrier labels so
     * dual-SIM functionality is always accessible and testable.
     */
    fun getAvailableSims(context: Context): List<SimAccountInfo> {
        val simList = mutableListOf<SimAccountInfo>()

        val hasPhonePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val callCapableAccounts: List<PhoneAccountHandle> = try {
            telecomManager?.callCapablePhoneAccounts ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        if (hasPhonePermission) {
            try {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val activeList: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList

                if (!activeList.isNullOrEmpty()) {
                    activeList.forEachIndexed { index, subInfo ->
                        val slot = subInfo.simSlotIndex.takeIf { it >= 0 } ?: index
                        val carrier = subInfo.carrierName?.toString()?.takeIf { it.isNotBlank() }
                            ?: subInfo.displayName?.toString()?.takeIf { it.isNotBlank() }
                            ?: "সিম ${slot + 1}"
                        val handle = callCapableAccounts.getOrNull(index)

                        simList.add(
                            SimAccountInfo(
                                slotIndex = slot,
                                displayName = "সিম ${slot + 1} ($carrier)",
                                carrierName = carrier,
                                subscriptionId = subInfo.subscriptionId,
                                phoneAccountHandle = handle
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w("SimManager", "Error querying subscriptions: ${e.message}")
            }
        }

        // Fallback if no SIMs returned from system API (e.g. single-SIM or emulator or permission not yet granted)
        if (simList.isEmpty()) {
            val handle1 = callCapableAccounts.getOrNull(0)
            val handle2 = callCapableAccounts.getOrNull(1)

            simList.add(
                SimAccountInfo(
                    slotIndex = 0,
                    displayName = "সিম ১ (SIM 1)",
                    carrierName = "সিম ১",
                    subscriptionId = 1,
                    phoneAccountHandle = handle1
                )
            )
            simList.add(
                SimAccountInfo(
                    slotIndex = 1,
                    displayName = "সিম ২ (SIM 2)",
                    carrierName = "সিম ২",
                    subscriptionId = 2,
                    phoneAccountHandle = handle2
                )
            )
        }

        return simList
    }

    /**
     * Get preferred SIM slot (0 or 1) stored for this number, or null if none saved.
     */
    fun getPreferredSimSlot(context: Context, rawNumber: String): Int? {
        val sanitized = WhatsAppCallingHelper.sanitizePhoneNumber(rawNumber)
        if (sanitized.isBlank()) return null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getInt("$PREF_KEY_PREFIX$sanitized", -1)
        return if (value in 0..1) value else null
    }

    /**
     * Set preferred SIM slot for this phone number.
     */
    fun setPreferredSimSlot(context: Context, rawNumber: String, slotIndex: Int) {
        val sanitized = WhatsAppCallingHelper.sanitizePhoneNumber(rawNumber)
        if (sanitized.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt("$PREF_KEY_PREFIX$sanitized", slotIndex).apply()
    }

    /**
     * Remove preferred SIM preference for this number.
     */
    fun clearPreferredSim(context: Context, rawNumber: String) {
        val sanitized = WhatsAppCallingHelper.sanitizePhoneNumber(rawNumber)
        if (sanitized.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("$PREF_KEY_PREFIX$sanitized").apply()
    }

    /**
     * Injects multi-SIM extras for MediaTek Helio, Xiaomi HyperOS/MIUI, and standard Android.
     */
    fun applySimExtrasToCallIntent(
        intent: Intent,
        simInfo: SimAccountInfo?
    ) {
        if (simInfo == null) return
        val slot = simInfo.slotIndex
        val subId = simInfo.subscriptionId

        // AOSP & MediaTek / Xiaomi HyperOS dual-SIM intent extras
        intent.putExtra("com.android.phone.extra.slot", slot)
        intent.putExtra("simSlot", slot)
        intent.putExtra("slot", slot)
        intent.putExtra("sim_slot", slot)
        intent.putExtra("phone_id", slot)
        intent.putExtra("subscription", subId)
        intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", simInfo.phoneAccountHandle)

        simInfo.phoneAccountHandle?.let { handle ->
            intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
        }
    }
}
