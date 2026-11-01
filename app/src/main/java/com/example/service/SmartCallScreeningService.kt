package com.example.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.annotation.RequiresApi
import com.example.data.AppDatabase
import com.example.data.entity.CallLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
class SmartCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        BlockedCallCache.init(applicationContext)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = try {
            callDetails.handle?.schemeSpecificPart
                ?: callDetails.handle?.encodedSchemeSpecificPart
                ?: ""
        } catch (e: Exception) {
            ""
        }

        // Layer-0 synchronous screening: 0ms overhead, instantly drops before Truecaller/OEM overlays trigger
        val syncResult = BlockedCallCache.isBlockedSync(phoneNumber)
        if (syncResult.isBlocked) {
            respondWithDisallow(callDetails)
            logBlockedCallAsync(phoneNumber, syncResult.callerName, syncResult.reason)
            return
        }

        // Fallback check against database in case cache is still warming up
        val db = AppDatabase.getDatabase(applicationContext)
        scope.launch {
            val cleanIncoming = phoneNumber.replace("[^0-9]".toRegex(), "").takeLast(10)
            val blockedList = db.blockedNumberDao().getAllBlockedNumbersSync()
            val matchedBlocked = blockedList.find {
                it.phoneNumber.replace("[^0-9]".toRegex(), "").takeLast(10) == cleanIncoming
            }
            val activeRules = db.spamRuleDao().getActiveRules()

            var matchesSpam = (matchedBlocked != null)
            var spamReason = matchedBlocked?.reason ?: "ব্লকলিস্টে থাকা নম্বর"
            var callerName = matchedBlocked?.callerName ?: "স্প্যাম কলার"

            if (!matchesSpam) {
                for (rule in activeRules) {
                    if (rule.ruleType == "PREFIX" && phoneNumber.startsWith(rule.pattern)) {
                        matchesSpam = true
                        spamReason = "স্প্যাম প্রিফিক্স: ${rule.description}"
                        break
                    }
                }
            }

            if (matchesSpam) {
                BlockedCallCache.markBlocked(phoneNumber)
                matchedBlocked?.let { BlockedCallCache.addPermanentBlocked(it) }

                respondWithDisallow(callDetails)
                logBlockedCallAsync(phoneNumber, callerName, spamReason)
            } else {
                // Allow call through normally
                val response = CallResponse.Builder().build()
                try {
                    respondToCall(callDetails, response)
                } catch (e: Exception) {
                    android.util.Log.w("SmartCallScreeningService", "respondToCall allow error: ${e.message}")
                }
            }
        }
    }

    private fun respondWithDisallow(callDetails: Call.Details) {
        try {
            val response = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipNotification(true)
                .setSkipCallLog(true) // Privacy enforcement: prevent OEM/system dialer from exposing blocked caller
                .build()

            respondToCall(callDetails, response)
        } catch (e: Exception) {
            android.util.Log.w("SmartCallScreeningService", "respondWithDisallow error: ${e.message}")
        }
    }

    private fun logBlockedCallAsync(phoneNumber: String, callerName: String, spamReason: String) {
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                UnreadBlockedSpamManager.increment(applicationContext)

                db.callLogDao().insert(
                    CallLogEntity(
                        number = phoneNumber,
                        name = callerName.ifBlank { "স্প্যাম কলার" },
                        type = "BLOCKED_SPAM",
                        timestamp = System.currentTimeMillis(),
                        durationSeconds = 0,
                        note = spamReason
                    )
                )
                db.blockedNumberDao().incrementBlockCount(phoneNumber)
            } catch (e: Exception) {
                android.util.Log.w("SmartCallScreeningService", "logBlockedCallAsync error: ${e.message}")
            }
        }
    }
}
