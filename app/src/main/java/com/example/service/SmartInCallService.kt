package com.example.service

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.entity.CallLogEntity
import com.example.data.repository.ContactLookupHelper
import com.example.telecom.TelecomPermissionHelper
import com.example.telecom.TelephonyDiagnosticsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmartInCallService : InCallService() {

    companion object {
        private const val TAG = "SmartInCallService"
        // Versioned channel ID forces Xiaomi HyperOS / Android 15 to recreate channel with maximum priority
        const val CHANNEL_ID = "incoming_call_channel_v5_callpro"
        const val SILENT_CHANNEL_ID = "incoming_call_silent_channel_callpro"
        const val NOTIFICATION_ID = 9999
        const val ACTION_ANSWER = "com.example.ACTION_ANSWER_CALL"
        const val ACTION_REJECT = "com.example.ACTION_REJECT_CALL"
        private var activeWakeLock: PowerManager.WakeLock? = null

        fun stopIncomingAlerts(context: Context) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(NOTIFICATION_ID)
                activeWakeLock?.let {
                    if (it.isHeld) it.release()
                }
                activeWakeLock = null
            } catch (e: Exception) {
                Log.w(TAG, "Error in stopIncomingAlerts: ${e.message}")
            }
        }

        fun dismissHeadsUpBanner(context: Context) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(NOTIFICATION_ID)
            } catch (e: Exception) {
                Log.w(TAG, "Error in dismissHeadsUpBanner: ${e.message}")
            }
        }

        fun postHeadsUpNotification(context: Context, number: String, name: String) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
                val fullScreenIntent = Intent(context, com.example.ui.call.IncomingCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("SHOW_IN_CALL", true)
                }
                val fullScreenPendingIntent = PendingIntent.getActivity(
                    context, 101, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val answerIntent = Intent(context, com.example.ui.call.IncomingCallActivity::class.java).apply {
                    action = ACTION_ANSWER
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val answerPendingIntent = PendingIntent.getActivity(
                    context, 102, answerIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val rejectIntent = Intent(context, com.example.ui.call.IncomingCallActivity::class.java).apply {
                    action = ACTION_REJECT
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val rejectPendingIntent = PendingIntent.getActivity(
                    context, 103, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val displayName = name.ifEmpty { number.ifEmpty { "অপরিচিত কলার" } }
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("ইনকামিং কল...")
                    .setContentText("$displayName ($number)")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setContentIntent(fullScreenPendingIntent)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .addAction(android.R.drawable.ic_menu_call, "উত্তর দিন", answerPendingIntent)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "প্রত্যাখ্যান", rejectPendingIntent)
                    .build()

                notification.flags = notification.flags or android.app.Notification.FLAG_INSISTENT
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.w(TAG, "Error in postHeadsUpNotification: ${e.message}")
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val proximitySensorManager by lazy { com.example.telecom.ProximitySensorManager(applicationContext) }
    private var callWasAnswered = false
    private var isCallIncoming = false
    private var isCallBlocked = false
    private var currentNumber = ""
    private var currentName = ""

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            activeWakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "CallPro:IncomingCallWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(30000L) // 30 seconds safety timeout
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wakeLock: ${e.message}")
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        val number = call.details?.handle?.schemeSpecificPart ?: ""
        currentNumber = number
        val resolvedName = ContactLookupHelper.resolveContactName(applicationContext, number)
            ?: call.details?.callerDisplayName?.takeIf { it.isNotBlank() }
            ?: ""
        currentName = resolvedName

        isCallIncoming = (call.state == Call.STATE_RINGING)
        callWasAnswered = false
        isCallBlocked = false

        // Instant Layer-0 sync check against fast cache (screened by SmartCallScreeningService)
        val syncBlockResult = BlockedCallCache.isBlockedSync(number)
        if (syncBlockResult.isBlocked) {
            isCallBlocked = true
            stopIncomingAlerts(applicationContext)
            dismissNotification()
            try {
                call.reject(false, null)
            } catch (_: Exception) {}
            return
        }

        if (isCallIncoming) {
            acquireWakeLock()
        }

        // 1. Immediately proceed with call synchronously within the active Telecom Binder transaction
        proceedWithCall(call, number, resolvedName)

        // 2. Parallel background verification against Room DB blocklist
        val db = AppDatabase.getDatabase(applicationContext)
        serviceScope.launch {
            val blockedList = db.blockedNumberDao().getAllBlockedNumbersSync()
            val cleanIncoming = number.replace("[^0-9]".toRegex(), "").takeLast(10)
            val matchedBlocked = blockedList.find {
                it.phoneNumber.replace("[^0-9]".toRegex(), "").takeLast(10) == cleanIncoming
            }

            if (matchedBlocked != null) {
                // Number is blocked: record in fast cache, immediately stop alerts & wake lock, silently reject without UI
                BlockedCallCache.markBlocked(number)
                isCallBlocked = true
                stopIncomingAlerts(applicationContext)
                withContext(Dispatchers.Main) {
                    try {
                        call.reject(false, null)
                    } catch (_: Exception) {}
                }
                UnreadBlockedSpamManager.increment(applicationContext)
                db.callLogDao().insert(
                    CallLogEntity(
                        number = number,
                        name = resolvedName.ifEmpty { matchedBlocked.callerName.ifEmpty { "ব্লকড স্প্যাম কলার" } },
                        type = "BLOCKED_SPAM",
                        timestamp = System.currentTimeMillis(),
                        durationSeconds = 0,
                        note = matchedBlocked.reason
                    )
                )
                db.blockedNumberDao().incrementBlockCount(matchedBlocked.phoneNumber)
            }
        }
    }

    private fun proceedWithCall(call: Call, number: String, resolvedName: String) {
        CallStateHolder.registerService(this)
        CallStateHolder.registerCall(call, applicationContext)

        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(c: Call, state: Int) {
                super.onStateChanged(c, state)
                val audioRoute = callAudioState?.route ?: CallAudioState.ROUTE_EARPIECE
                proximitySensorManager.onAudioStateOrCallChanged(audioRoute, state)

                // When transitioning away from ringing (active, connecting, disconnected), silence alerts immediately
                if (state != Call.STATE_RINGING) {
                    dismissNotification()
                }
                if (state == Call.STATE_ACTIVE) {
                    callWasAnswered = true
                    MissedCallNotifier.dismissReminder(applicationContext, number)
                }
            }
        })

        if (call.state == Call.STATE_RINGING) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val isLocked = keyguardManager.isKeyguardLocked || keyguardManager.isDeviceLocked

            // Record diagnostics for in-app diagnostic report
            TelephonyDiagnosticsManager.recordIncomingCall(
                number = number,
                state = "STATE_RINGING",
                isLocked = isLocked
            )

            if (isLocked) {
                // WHEN LOCKED:
                // Notification with fullScreenPendingIntent launches IncomingCallActivity over Keyguard natively
                showIncomingCallNotification(number, resolvedName, isHighImportanceHeadsUp = true)
                TelephonyDiagnosticsManager.recordFsiDispatch("Notification FSI (Keyguard Active)")
            } else {
                // WHEN UNLOCKED:
                // Launch IncomingCallActivity directly for instantaneous full screen.
                val canLaunchDirectly = TelecomPermissionHelper.isDefaultDialer(this) ||
                        TelecomPermissionHelper.canDrawOverlays(this)

                var directLaunchSuccess = false
                if (canLaunchDirectly) {
                    val callIntent = Intent(this, com.example.ui.call.IncomingCallActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("SHOW_IN_CALL", true)
                    }
                    val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        android.app.ActivityOptions.makeBasic().apply {
                            setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                        }.toBundle()
                    } else {
                        null
                    }
                    try {
                        startActivity(callIntent, options)
                        directLaunchSuccess = true
                        TelephonyDiagnosticsManager.recordFsiDispatch("Direct Activity Launch (Unlocked)")
                        // Post silent ongoing notification so system shade knows of the incoming call without displaying a Heads-Up banner over the full screen
                        showIncomingCallNotification(number, resolvedName, isHighImportanceHeadsUp = false)
                    } catch (e: Exception) {
                        Log.w(TAG, "Direct startActivity failed, falling back to Heads-Up banner: ${e.message}")
                        TelephonyDiagnosticsManager.recordFsiDispatch("Heads-Up Banner Fallback (Unlocked)")
                    }
                }
                if (!directLaunchSuccess) {
                    showIncomingCallNotification(number, resolvedName, isHighImportanceHeadsUp = true)
                    TelephonyDiagnosticsManager.recordFsiDispatch("Heads-Up Banner Display (Unlocked)")
                }
            }
        } else {
            // Outgoing or active call: Launch MainActivity
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("SHOW_IN_CALL", true)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to startActivity for active call: ${e.message}")
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        proximitySensorManager.release()
        dismissNotification()
        CallStateHolder.unregisterCall(call)
        CallStateHolder.unregisterService()

        // Zero-Bleed Enforcement:
        // Never post missed call notifications for calls that were screened/blocked
        if (isCallIncoming && !callWasAnswered && !isCallBlocked && currentNumber.isNotBlank()) {
            if (!BlockedCallCache.isBlocked(currentNumber)) {
                MissedCallNotifier.postMissedCallNotification(
                    context = applicationContext,
                    number = currentNumber,
                    name = currentName
                )
            }
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        CallStateHolder.updateAudioState(audioState)
        val currentCallState = CallStateHolder.callInfo.value.state
        val telecomState = when (currentCallState) {
            CallUiState.ACTIVE -> Call.STATE_ACTIVE
            CallUiState.DIALING -> Call.STATE_DIALING
            CallUiState.RINGING -> Call.STATE_RINGING
            else -> Call.STATE_DISCONNECTED
        }
        proximitySensorManager.onAudioStateOrCallChanged(audioState.route, telecomState)
    }

    override fun onDestroy() {
        super.onDestroy()
        proximitySensorManager.release()
    }

    private fun showIncomingCallNotification(number: String, name: String, isHighImportanceHeadsUp: Boolean = true) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = if (isHighImportanceHeadsUp) CHANNEL_ID else SILENT_CHANNEL_ID

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isHighImportanceHeadsUp) {
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "ইনকামিং কল (Incoming Calls)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "ইনকামিং কলের জন্য ফুল-স্ক্রিন নোটিফিকেশন"
                    setSound(ringtoneUri, audioAttributes)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 800, 1000, 800)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    setBypassDnd(true)
                }
                notificationManager.createNotificationChannel(channel)
            } else {
                val silentChannel = NotificationChannel(
                    SILENT_CHANNEL_ID,
                    "চলমান কল তথ্য (In-Call Notification)",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "ফুলস্ক্রিন কল চলাকালীন স্ট্যাটাস নোটিফিকেশন"
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(silentChannel)
            }
        }

        // Canonical Full Screen Intent explicitly targeting IncomingCallActivity
        val fullScreenIntent = Intent(this, com.example.ui.call.IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("SHOW_IN_CALL", true)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            101,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Answer PendingIntent targeting IncomingCallActivity
        val answerIntent = Intent(this, com.example.ui.call.IncomingCallActivity::class.java).apply {
            action = ACTION_ANSWER
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val answerPendingIntent = PendingIntent.getActivity(
            this,
            102,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline PendingIntent targeting IncomingCallActivity
        val rejectIntent = Intent(this, com.example.ui.call.IncomingCallActivity::class.java).apply {
            action = ACTION_REJECT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val rejectPendingIntent = PendingIntent.getActivity(
            this,
            103,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayName = name.ifEmpty { number.ifEmpty { "অপরিচিত কলার" } }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ইনকামিং কল...")
            .setContentText("$displayName ($number)")
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_call, "উত্তর দিন", answerPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "প্রত্যাখ্যান", rejectPendingIntent)

        if (isHighImportanceHeadsUp) {
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(fullScreenPendingIntent)
        }

        val notification = builder.build()
        if (isHighImportanceHeadsUp) {
            notification.flags = notification.flags or android.app.Notification.FLAG_INSISTENT
        }
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun dismissNotification() {
        stopIncomingAlerts(applicationContext)
    }
}
