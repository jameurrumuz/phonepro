package com.example.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveOtpBanner(
    val code: String,
    val sender: String,
    val snippet: String,
    val totalSeconds: Int = 10,
    val remainingSeconds: Int = 10,
    val isCopied: Boolean = false
)

object OtpOverlayManager {

    private const val CHANNEL_ID = "channel_otp_smart_copy"
    private const val NOTIFICATION_ID = 8802

    private val _activeBanner = MutableStateFlow<ActiveOtpBanner?>(null)
    val activeBanner: StateFlow<ActiveOtpBanner?> = _activeBanner.asStateFlow()

    private var countdownJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun postOtp(context: Context, detected: DetectedOtp) {
        val appContext = context.applicationContext
        // 1. Auto-copy OTP code to Android Clipboard immediately
        copyToClipboard(appContext, detected.code, showToast = false)

        // 2. Setup 10-second active in-app countdown banner
        countdownJob?.cancel()
        _activeBanner.value = ActiveOtpBanner(
            code = detected.code,
            sender = detected.sender,
            snippet = detected.messageSnippet,
            totalSeconds = 10,
            remainingSeconds = 10,
            isCopied = true
        )

        countdownJob = scope.launch {
            for (sec in 10 downTo 1) {
                _activeBanner.value = _activeBanner.value?.copy(remainingSeconds = sec)
                delay(1000)
            }
            _activeBanner.value = null
        }

        // 3. Post Heads-Up Notification with 1-tap Copy action
        showOtpNotification(appContext, detected)
    }

    fun dismissBanner() {
        countdownJob?.cancel()
        countdownJob = null
        _activeBanner.value = null
    }

    fun copyManually(context: Context) {
        val banner = _activeBanner.value ?: return
        copyToClipboard(context, banner.code, showToast = true)
        _activeBanner.value = banner.copy(isCopied = true)
    }

    private fun copyToClipboard(context: Context, code: String, showToast: Boolean) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("OTP Code", code)
            clipboard?.setPrimaryClip(clip)
            if (showToast) {
                Toast.makeText(context, "OTP কোড কপি করা হয়েছে: $code", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.w("OtpOverlayManager", "Clipboard copy error: ${e.message}")
        }
    }

    private fun showOtpNotification(context: Context, detected: DetectedOtp) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart OTP Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority floating OTP notifications with auto-copy"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Copy PendingIntent
        val copyIntent = Intent(context, OtpCopyReceiver::class.java).apply {
            action = OtpCopyReceiver.ACTION_COPY_OTP
            putExtra(OtpCopyReceiver.EXTRA_OTP_CODE, detected.code)
        }
        val copyPending = PendingIntent.getBroadcast(
            context,
            detected.code.hashCode(),
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("OTP কোড: ${detected.code}")
            .setContentText("${detected.sender} থেকে ভেরিফিকেশন কোড (ক্লিপবোর্ডে কপি করা হয়েছে)")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setTimeoutAfter(10_000) // Auto-dismiss after 10 seconds
            .addAction(android.R.drawable.ic_menu_save, "কপি করুন (${detected.code})", copyPending)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            android.util.Log.w("OtpOverlayManager", "Notification post error: ${e.message}")
        }
    }
}
