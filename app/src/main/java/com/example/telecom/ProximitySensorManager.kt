package com.example.telecom

import android.content.Context
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.util.Log

class ProximitySensorManager(context: Context) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    init {
        try {
            if (powerManager?.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) == true) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "CallPro::ProximityWakeLock"
                ).apply {
                    setReferenceCounted(false)
                }
            }
        } catch (e: Exception) {
            Log.w("ProximitySensorManager", "Failed to initialize proximity wake lock: ${e.message}")
        }
    }

    fun onAudioStateOrCallChanged(audioRoute: Int, callState: Int) {
        val isCallActiveOrDialing = (callState == Call.STATE_ACTIVE ||
                callState == Call.STATE_DIALING ||
                callState == Call.STATE_CONNECTING)
        val isHandsetEarpiece = (audioRoute == CallAudioState.ROUTE_EARPIECE)

        if (isCallActiveOrDialing && isHandsetEarpiece) {
            acquire()
        } else {
            release()
        }
    }

    fun acquire() {
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(2 * 60 * 60 * 1000L) // Safety 2 hour maximum
                Log.d("ProximitySensorManager", "Proximity wake lock ACQUIRED")
            }
        } catch (e: Exception) {
            Log.w("ProximitySensorManager", "Error acquiring wake lock: ${e.message}")
        }
    }

    fun release() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
                Log.d("ProximitySensorManager", "Proximity wake lock RELEASED")
            }
        } catch (e: Exception) {
            try {
                wakeLock?.release()
            } catch (_: Exception) {}
        }
    }
}
