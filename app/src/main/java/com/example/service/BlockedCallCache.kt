package com.example.service

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.entity.BlockedNumberEntity
import com.example.data.entity.SpamRuleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * High-performance, zero-latency in-memory cache for screened and blocked phone numbers.
 * Acts as an airtight Layer-0 barrier between CallScreeningService, SmartInCallService,
 * and MissedCallNotifier to eliminate notification bleed and ringtone latency with 0ms overhead.
 */
object BlockedCallCache {
    // Map of normalized 10-digit number -> BlockedNumberEntity
    private val permanentBlockedMap = ConcurrentHashMap<String, BlockedNumberEntity>()
    
    // Active prefix rules
    private val activePrefixes = CopyOnWriteArrayList<SpamRuleEntity>()
    
    // Short-term screened numbers (e.g. screened at Layer 0 to prevent downstream bleed)
    private val temporaryScreened = ConcurrentHashMap<String, Long>()
    private const val EXPIRATION_MS = 60_000L // 1 minute active cache window

    @Volatile
    private var isInitialized = false

    /**
     * Pre-warms the in-memory cache from Room database asynchronously on app start.
     */
    fun init(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(appContext)
                val blockedList = db.blockedNumberDao().getAllBlockedNumbersSync()
                val rules = db.spamRuleDao().getActiveRules()

                permanentBlockedMap.clear()
                for (b in blockedList) {
                    val clean = cleanNumber(b.phoneNumber)
                    if (clean.isNotBlank()) {
                        permanentBlockedMap[clean] = b
                    }
                }

                activePrefixes.clear()
                activePrefixes.addAll(rules.filter { it.ruleType == "PREFIX" })
                isInitialized = true
            } catch (e: Exception) {
                android.util.Log.w("BlockedCallCache", "Failed to init cache: ${e.message}")
            }
        }
    }

    /**
     * Synchronously checks if a number is blocked (0ms execution on binder thread).
     */
    fun isBlockedSync(rawNumber: String): BlockResult {
        val clean = cleanNumber(rawNumber)
        
        // 1. Check permanent database-backed blocked list
        if (clean.isNotBlank()) {
            val matched = permanentBlockedMap[clean]
            if (matched != null) {
                return BlockResult(
                    isBlocked = true,
                    reason = matched.reason.ifBlank { "ব্লকলিস্টে থাকা নম্বর" },
                    callerName = matched.callerName
                )
            }
        }

        // 2. Check active prefix spam rules
        val rawTrimmed = rawNumber.trim()
        for (rule in activePrefixes) {
            if (rawTrimmed.startsWith(rule.pattern)) {
                return BlockResult(
                    isBlocked = true,
                    reason = "স্প্যাম প্রিফিক্স: ${rule.description}",
                    callerName = "স্প্যাম কলার"
                )
            }
        }

        // 3. Check temporary screened cache
        if (clean.isNotBlank()) {
            val timestamp = temporaryScreened[clean]
            if (timestamp != null) {
                if (System.currentTimeMillis() - timestamp <= EXPIRATION_MS) {
                    return BlockResult(
                        isBlocked = true,
                        reason = "স্ক্রিন্ড স্প্যাম নম্বর",
                        callerName = "স্প্যাম কলার"
                    )
                } else {
                    temporaryScreened.remove(clean)
                }
            }
        }

        return BlockResult(isBlocked = false, reason = "", callerName = "")
    }

    fun isBlocked(rawNumber: String): Boolean {
        return isBlockedSync(rawNumber).isBlocked
    }

    fun markBlocked(rawNumber: String) {
        val clean = cleanNumber(rawNumber)
        if (clean.isNotBlank()) {
            temporaryScreened[clean] = System.currentTimeMillis()
        }
    }

    fun addPermanentBlocked(entity: BlockedNumberEntity) {
        val clean = cleanNumber(entity.phoneNumber)
        if (clean.isNotBlank()) {
            permanentBlockedMap[clean] = entity
            temporaryScreened[clean] = System.currentTimeMillis()
        }
    }

    fun removePermanentBlocked(phoneNumber: String) {
        val clean = cleanNumber(phoneNumber)
        if (clean.isNotBlank()) {
            permanentBlockedMap.remove(clean)
            temporaryScreened.remove(clean)
        }
    }

    fun clear() {
        permanentBlockedMap.clear()
        temporaryScreened.clear()
        activePrefixes.clear()
    }

    private fun cleanNumber(num: String): String {
        val digits = num.replace("[^0-9]".toRegex(), "")
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    data class BlockResult(
        val isBlocked: Boolean,
        val reason: String,
        val callerName: String
    )
}
