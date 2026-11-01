package com.example.sms

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

class OtpCopyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_COPY_OTP) {
            val code = intent.getStringExtra(EXTRA_OTP_CODE) ?: return
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("OTP Code", code)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, "OTP কোড কপি করা হয়েছে: $code", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ACTION_COPY_OTP = "com.example.ACTION_COPY_OTP"
        const val EXTRA_OTP_CODE = "extra_otp_code"
    }
}
