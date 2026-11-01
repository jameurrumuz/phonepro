package com.example.telecom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.Call
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.content.ContextCompat

data class ResolvedSimInfo(
    val slotIndex: Int, // 0 for SIM 1, 1 for SIM 2
    val displayName: String, // e.g. "সিম ১ (Grameenphone)" or "সিম ১"
    val carrierName: String,
    val subscriptionId: Int
)

object SimAccountResolver {

    /**
     * Resolves dual-SIM slot and carrier details for an ongoing or incoming Telecom Call.
     */
    fun resolveCallSim(call: Call, context: Context, phoneNumber: String = ""): ResolvedSimInfo {
        val accountHandle: PhoneAccountHandle? = call.details?.accountHandle
        return resolveFromHandleOrNumber(accountHandle, context, phoneNumber)
    }

    /**
     * Resolves dual-SIM slot and carrier details given an accountId string from system CallLog.
     */
    fun resolveFromAccountId(accountId: String?, context: Context, phoneNumber: String = ""): ResolvedSimInfo {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission && !accountId.isNullOrBlank()) {
            try {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val activeList: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList

                if (!activeList.isNullOrEmpty()) {
                    val matched = activeList.find { sub ->
                        sub.subscriptionId.toString() == accountId ||
                        sub.simSlotIndex.toString() == accountId ||
                        sub.iccId.equals(accountId, ignoreCase = true) ||
                        accountId.contains(sub.subscriptionId.toString())
                    }
                    if (matched != null) {
                        return mapSubscription(matched)
                    }
                }
            } catch (e: Exception) {
                Log.w("SimAccountResolver", "Error resolving subscription from accountId: ${e.message}")
            }
        }

        // Fallback to preferred SIM slot if set for this number
        if (phoneNumber.isNotBlank()) {
            val prefSlot = SimManager.getPreferredSimSlot(context, phoneNumber)
            if (prefSlot != null) {
                return fallbackSim(prefSlot)
            }
        }

        return fallbackSim(0)
    }

    /**
     * Resolves dual-SIM slot and carrier details from PhoneAccountHandle or stored number preferences.
     */
    fun resolveFromHandleOrNumber(
        accountHandle: PhoneAccountHandle?,
        context: Context,
        phoneNumber: String = ""
    ): ResolvedSimInfo {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val callCapableAccounts: List<PhoneAccountHandle> = try {
            telecomManager?.callCapablePhoneAccounts ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        if (hasPermission) {
            try {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val activeList: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList

                if (!activeList.isNullOrEmpty()) {
                    val handleId = accountHandle?.id ?: ""

                    // 1. Direct match on Subscription ID or ICCID
                    if (handleId.isNotBlank()) {
                        val matched = activeList.find { sub ->
                            sub.subscriptionId.toString() == handleId ||
                            sub.iccId.equals(handleId, ignoreCase = true) ||
                            handleId.contains(sub.subscriptionId.toString())
                        }
                        if (matched != null) {
                            return mapSubscription(matched)
                        }
                    }

                    // 2. Direct match on PhoneAccountHandle position in callCapableAccounts
                    if (accountHandle != null && callCapableAccounts.isNotEmpty()) {
                        val index = callCapableAccounts.indexOfFirst { it == accountHandle }
                        if (index in activeList.indices) {
                            return mapSubscription(activeList[index])
                        }
                    }

                    // 3. Fallback to preferred SIM
                    if (phoneNumber.isNotBlank()) {
                        val prefSlot = SimManager.getPreferredSimSlot(context, phoneNumber)
                        if (prefSlot != null) {
                            val matched = activeList.find { it.simSlotIndex == prefSlot }
                            if (matched != null) return mapSubscription(matched)
                        }
                    }

                    // 4. Default to first active SIM
                    return mapSubscription(activeList.first())
                }
            } catch (e: Exception) {
                Log.w("SimAccountResolver", "Error resolving subscription info: ${e.message}")
            }
        }

        // Fallback for emulator or no phone state permission
        if (phoneNumber.isNotBlank()) {
            val prefSlot = SimManager.getPreferredSimSlot(context, phoneNumber)
            if (prefSlot != null) {
                return fallbackSim(prefSlot)
            }
        }

        // Check handle index in callCapableAccounts
        if (accountHandle != null && callCapableAccounts.size > 1) {
            val idx = callCapableAccounts.indexOfFirst { it == accountHandle }
            if (idx == 1) return fallbackSim(1)
        }

        return fallbackSim(0)
    }

    private fun mapSubscription(sub: SubscriptionInfo): ResolvedSimInfo {
        val slot = if (sub.simSlotIndex >= 0) sub.simSlotIndex else 0
        val carrier = sub.carrierName?.toString()?.takeIf { it.isNotBlank() }
            ?: sub.displayName?.toString()?.takeIf { it.isNotBlank() }
            ?: "সিম ${slot + 1}"
        val localizedSlot = if (slot == 1) "সিম ২" else "সিম ১"
        val display = "$localizedSlot ($carrier)"

        return ResolvedSimInfo(
            slotIndex = slot,
            displayName = display,
            carrierName = carrier,
            subscriptionId = sub.subscriptionId
        )
    }

    private fun fallbackSim(slot: Int): ResolvedSimInfo {
        val localized = if (slot == 1) "সিম ২" else "সিম ১"
        return ResolvedSimInfo(
            slotIndex = slot,
            displayName = localized,
            carrierName = localized,
            subscriptionId = slot + 1
        )
    }
}
