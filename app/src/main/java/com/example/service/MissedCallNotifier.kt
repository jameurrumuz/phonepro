package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object MissedCallNotifier {

    const val CHANNEL_ID = "missed_call_channel_v1"
    const val NOTIFICATION_BASE_ID = 20000
    const val ACTION_CALL_BACK = "com.example.ACTION_CALL_BACK"
    const val ACTION_DISMISS = "com.example.ACTION_DISMISS_MISSED_CALL"
    const val EXTRA_NUMBER = "extra_missed_number"
    const val EXTRA_NAME = "extra_missed_name"

    private val activeReminders = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.Main)

    fun postMissedCallNotification(context: Context, number: String, name: String) {
        if (number.isBlank()) return
        if (BlockedCallCache.isBlocked(number)) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "মিসড কল নোটিফিকেশন (Missed Calls)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "মিসড কল ও ফিরতি কল রিমাইন্ডার"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = NOTIFICATION_BASE_ID + (number.hashCode() and 0x7FFF)
        val displayName = name.ifEmpty { number }

        // Intent to launch app and call back immediately
        val callBackIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_CALL_BACK
            putExtra(EXTRA_NUMBER, number)
            putExtra(EXTRA_NAME, displayName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val callBackPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            callBackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(context, MissedCallDismissReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_NUMBER, number)
            putExtra("notification_id", notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("মিসড কল: $displayName")
            .setContentText("ফিরতি কল করতে ট্যাপ করুন: $number")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setContentIntent(callBackPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_call, "কল ব্যাক", callBackPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "স্কিপ / বাতিল", dismissPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)

        // Schedule periodic reminder if user enabled it in settings
        scheduleReminderIfEnabled(context, number, displayName, notificationId)
    }

    private fun scheduleReminderIfEnabled(
        context: Context,
        number: String,
        name: String,
        notificationId: Int
    ) {
        val prefs = context.getSharedPreferences("smart_dialer_prefs", Context.MODE_PRIVATE)
        val intervalMinutes = prefs.getInt("pref_missed_call_reminder_min", 0) // default 0 (off)

        if (intervalMinutes <= 0) return

        // Cancel any existing reminder for this number
        activeReminders[number]?.cancel()

        val job = scope.launch {
            val intervalMs = intervalMinutes * 60 * 1000L
            while (true) {
                delay(intervalMs)
                // Re-notify
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val callBackIntent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_CALL_BACK
                    putExtra(EXTRA_NUMBER, number)
                    putExtra(EXTRA_NAME, name)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val callBackPendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId + 1,
                    callBackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val dismissIntent = Intent(context, MissedCallDismissReceiver::class.java).apply {
                    action = ACTION_DISMISS
                    putExtra(EXTRA_NUMBER, number)
                    putExtra("notification_id", notificationId)
                }
                val dismissPendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId + 2,
                    dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("মিসড কল রিমাইন্ডার: $name")
                    .setContentText("আপনি এখনও কল ব্যাক করেননি: $number")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(callBackPendingIntent)
                    .setAutoCancel(true)
                    .addAction(android.R.drawable.ic_menu_call, "কল ব্যাক", callBackPendingIntent)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "স্কিপ / বাতিল", dismissPendingIntent)
                    .build()

                notificationManager.notify(notificationId, notification)
            }
        }
        activeReminders[number] = job
    }

    fun dismissReminder(context: Context, number: String, notificationId: Int? = null) {
        activeReminders[number]?.cancel()
        activeReminders.remove(number)

        val id = notificationId ?: (NOTIFICATION_BASE_ID + (number.hashCode() and 0x7FFF))
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)
    }
}

class MissedCallDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val number = intent.getStringExtra(MissedCallNotifier.EXTRA_NUMBER) ?: ""
        val notificationId = intent.getIntExtra("notification_id", -1)
        MissedCallNotifier.dismissReminder(context, number, if (notificationId != -1) notificationId else null)
    }
}
