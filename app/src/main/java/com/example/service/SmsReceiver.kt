package com.example.service

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.sms.OtpDetector
import com.example.sms.OtpOverlayManager

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Telephony.Sms.Intents.SMS_DELIVER_ACTION ||
            action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        ) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val combinedBody = StringBuilder()
            var sender = ""
            var timestamp = System.currentTimeMillis()

            for (sms in messages) {
                if (sender.isEmpty()) {
                    sender = sms.displayOriginatingAddress ?: sms.originatingAddress ?: ""
                    if (sms.timestampMillis > 0) {
                        timestamp = sms.timestampMillis
                    }
                }
                combinedBody.append(sms.displayMessageBody ?: "")
            }

            val fullText = combinedBody.toString()

            // CRITICAL: When the app is the Default SMS App, Android system sends SMS_DELIVER_ACTION.
            // Under this action, Android OS delegates writing the message into the provider to the default app.
            // If not written to Telephony.Sms.Inbox, the message is dropped and will not appear in the inbox.
            if (action == Telephony.Sms.Intents.SMS_DELIVER_ACTION ||
                com.example.telecom.TelecomPermissionHelper.isDefaultSmsApp(context)
            ) {
                try {
                    val values = ContentValues().apply {
                        put(Telephony.Sms.ADDRESS, sender)
                        put(Telephony.Sms.BODY, fullText)
                        put(Telephony.Sms.DATE, timestamp)
                        put(Telephony.Sms.DATE_SENT, timestamp)
                        put(Telephony.Sms.READ, 0)
                        put(Telephony.Sms.SEEN, 0)
                        put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                    }
                    val insertedUri = context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
                    Log.d(TAG, "Successfully inserted incoming SMS into inbox: $insertedUri for $sender")
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting SMS into provider inbox: ${e.message}", e)
                }
            }

            if (fullText.isNotBlank()) {
                val detected = OtpDetector.detectOtp(sender, fullText)
                if (detected != null) {
                    OtpOverlayManager.postOtp(context, detected)
                }
            }
        }
    }
}
