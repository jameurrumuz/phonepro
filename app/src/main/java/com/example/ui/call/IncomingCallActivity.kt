package com.example.ui.call

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.AppDatabase
import com.example.data.entity.CallNoteEntity
import com.example.service.CallStateHolder
import com.example.service.CallUiState
import com.example.service.SmartInCallService
import com.example.ui.screens.InCallScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IncomingCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        configureLockScreenDisplay()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.telecom.TelephonyDiagnosticsManager.recordActivityOnCreate()
        handleIntentAction(intent)

        val db = AppDatabase.getDatabase(applicationContext)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                val callInfo by CallStateHolder.callInfo.collectAsState()
                var noteText by remember { mutableStateOf("") }
                var isShareContactOpen by remember { mutableStateOf(false) }
                val contactsRepo = remember { com.example.data.repository.ContactsRepository(applicationContext) }
                val contacts by contactsRepo.contacts.collectAsState()
                val scope = rememberCoroutineScope()
                val strings = AppStrings.get(AppLanguage.BANGLA)

                val noteDebouncer = remember(scope) {
                    com.example.ui.util.CallNoteDebouncer(scope) { phoneNumber, text ->
                        if (phoneNumber.isNotBlank() && text.isNotBlank()) {
                            db.callNoteDao().insert(
                                CallNoteEntity(
                                    phoneNumber = phoneNumber,
                                    noteText = text,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    contactsRepo.loadContacts()
                }

                var hasEverBeenActiveOrRinging by remember { mutableStateOf(false) }

                // Close activity only after call was active/ringing and then finished
                LaunchedEffect(callInfo.state) {
                    if (callInfo.state == CallUiState.RINGING ||
                        callInfo.state == CallUiState.ACTIVE ||
                        callInfo.state == CallUiState.DIALING
                    ) {
                        hasEverBeenActiveOrRinging = true
                    } else if (hasEverBeenActiveOrRinging &&
                        (callInfo.state == CallUiState.DISCONNECTED || callInfo.state == CallUiState.IDLE)
                    ) {
                        kotlinx.coroutines.delay(600)
                        finishAndRemoveTask()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    InCallScreen(
                        callInfo = callInfo,
                        activeNoteText = noteText,
                        strings = strings,
                        onNoteTextChanged = { newNote ->
                            noteText = newNote
                            noteDebouncer.onTextChanged(callInfo.phoneNumber, newNote)
                        },
                        onAnswer = {
                            SmartInCallService.stopIncomingAlerts(this@IncomingCallActivity)
                            CallStateHolder.answer()
                            unlockDeviceKeyguard()
                        },
                        onReject = {
                            SmartInCallService.stopIncomingAlerts(this@IncomingCallActivity)
                            CallStateHolder.reject()
                            finishAndRemoveTask()
                        },
                        onEndCall = {
                            SmartInCallService.stopIncomingAlerts(this@IncomingCallActivity)
                            CallStateHolder.disconnect()
                            finishAndRemoveTask()
                        },
                        onToggleMute = {
                            CallStateHolder.toggleMute(this@IncomingCallActivity)
                        },
                        onToggleSpeaker = {
                            CallStateHolder.toggleSpeaker(this@IncomingCallActivity)
                        },
                        onToggleBluetooth = {
                            CallStateHolder.toggleBluetooth(this@IncomingCallActivity)
                        },
                        onToggleRecord = {
                            CallStateHolder.setRecording(!callInfo.isRecording)
                        },
                        onSwitchSim = {
                            val nextSlot = if (callInfo.simSlot == 0) 1 else 0
                            val nextName = if (nextSlot == 1) "সিম ২" else "সিম ১"
                            CallStateHolder.setSimInfo(nextSlot, nextName)
                            if (callInfo.phoneNumber.isNotBlank()) {
                                com.example.telecom.SimManager.setPreferredSimSlot(
                                    this@IncomingCallActivity,
                                    callInfo.phoneNumber,
                                    nextSlot
                                )
                            }
                        },
                        onShareContact = {
                            isShareContactOpen = true
                        },
                        onQuickSmsDecline = { msg ->
                            SmartInCallService.stopIncomingAlerts(this@IncomingCallActivity)
                            CallStateHolder.reject()
                            scope.launch(Dispatchers.IO) {
                                val smsRepo = com.example.data.repository.SmsRepository(applicationContext)
                                smsRepo.sendSms(callInfo.phoneNumber, msg)
                            }
                            finishAndRemoveTask()
                        }
                    )

                    if (isShareContactOpen) {
                        com.example.ui.dialogs.InCallShareContactDialog(
                            isOpen = isShareContactOpen,
                            targetCallerNumber = callInfo.phoneNumber,
                            contacts = contacts,
                            onSendContact = { contact ->
                                scope.launch(Dispatchers.IO) {
                                    val smsRepo = com.example.data.repository.SmsRepository(applicationContext)
                                    val text = "পরিচিতি: ${contact.name}\nমোবাইল নম্বর: ${contact.phoneNumber}"
                                    smsRepo.sendSms(callInfo.phoneNumber, text)
                                }
                            },
                            onDismiss = { isShareContactOpen = false }
                        )
                    }
                }
            }
        }
    }

    private fun configureLockScreenDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }

    private fun unlockDeviceKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        com.example.telecom.TelephonyDiagnosticsManager.recordActivityOnNewIntent()
        handleIntentAction(intent)
    }

    private fun handleIntentAction(intent: android.content.Intent?) {
        when (intent?.action) {
            SmartInCallService.ACTION_ANSWER -> {
                SmartInCallService.stopIncomingAlerts(this)
                CallStateHolder.answer()
                unlockDeviceKeyguard()
            }
            SmartInCallService.ACTION_REJECT -> {
                SmartInCallService.stopIncomingAlerts(this)
                CallStateHolder.reject()
                finishAndRemoveTask()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure heads-up banner doesn't obscure the full-screen call UI while activity is visible in foreground
        SmartInCallService.dismissHeadsUpBanner(this)
    }

    override fun onStop() {
        super.onStop()
        val currentCall = CallStateHolder.callInfo.value
        // If the user navigates away or presses home while ringing, re-post heads-up banner so user doesn't miss the call
        if (currentCall.state == CallUiState.RINGING && !isFinishing) {
            SmartInCallService.postHeadsUpNotification(this, currentCall.phoneNumber, currentCall.callerName)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SmartInCallService.stopIncomingAlerts(this)
    }
}
