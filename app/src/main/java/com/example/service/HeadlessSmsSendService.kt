package com.example.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Required service for Android Default SMS App specification to respond via message.
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
