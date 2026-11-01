package com.example.service

import android.content.Context
import android.media.AudioManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import com.example.data.repository.ContactLookupHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CallUiState {
    IDLE,
    DIALING,
    RINGING,
    ACTIVE,
    ON_HOLD,
    DISCONNECTED
}

data class ActiveCallInfo(
    val state: CallUiState = CallUiState.IDLE,
    val phoneNumber: String = "",
    val callerName: String = "",
    val callerTypeLabel: String = "Mobile",
    val photoUri: String? = null,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isBluetoothOn: Boolean = false,
    val isBluetoothAvailable: Boolean = false,
    val isRecording: Boolean = false,
    val simSlot: Int = 0,
    val simDisplayName: String = "1"
)

object CallStateHolder {
    private var currentTelecomCall: Call? = null
    private var currentInCallService: InCallService? = null
    private val _callInfo = MutableStateFlow(ActiveCallInfo())
    val callInfo: StateFlow<ActiveCallInfo> = _callInfo.asStateFlow()

    private var durationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun registerService(service: InCallService) {
        currentInCallService = service
    }

    fun unregisterService() {
        currentInCallService = null
    }

    fun registerCall(call: Call, context: Context) {
        currentTelecomCall = call
        val appContext = context.applicationContext
        autoSelectPhoneAccountIfNeeded(call, appContext)

        val number = try {
            call.details?.handle?.schemeSpecificPart
                ?: call.details?.handle?.encodedSchemeSpecificPart
                ?: ""
        } catch (e: Exception) {
            ""
        }
        
        // Resolve contact details from phonebook safely so saved numbers NEVER show "অপরিচিত কলার"
        val contactDetails = try {
            if (number.isNotBlank()) {
                ContactLookupHelper.resolveContactDetails(appContext, number)
            } else null
        } catch (e: Exception) {
            null
        }

        val resolvedName = contactDetails?.name ?: try {
            call.details?.callerDisplayName?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        } ?: if (number.isNotBlank()) "" else "অপরিচিত কলার"

        val resolvedTypeLabel = contactDetails?.typeLabel ?: "Mobile"
        val resolvedPhoto = contactDetails?.photoUri

        // Resolve SIM card information defensively
        val simDisplayName = try {
            val simInfo = com.example.telecom.SimAccountResolver.resolveCallSim(call, appContext, number)
            _callInfo.value = _callInfo.value.copy(
                simSlot = simInfo.slotIndex,
                simDisplayName = if (simInfo.slotIndex == 1) "2" else "1"
            )
            if (simInfo.slotIndex == 1) "2" else "1"
        } catch (e: Exception) {
            android.util.Log.w("CallStateHolder", "resolveCallSim error: ${e.message}")
            "1"
        }

        _callInfo.value = _callInfo.value.copy(
            callerTypeLabel = resolvedTypeLabel,
            photoUri = resolvedPhoto,
            simDisplayName = simDisplayName
        )

        val initialCallState = try { call.state } catch (e: Exception) { Call.STATE_DISCONNECTED }
        updateStateFromTelecom(initialCallState, number, resolvedName, resolvedTypeLabel, resolvedPhoto)

        try {
            call.registerCallback(object : Call.Callback() {
                override fun onStateChanged(call: Call, state: Int) {
                    super.onStateChanged(call, state)
                    autoSelectPhoneAccountIfNeeded(call, appContext)
                    updateStateFromTelecom(state, number, resolvedName, resolvedTypeLabel, resolvedPhoto)
                }
            })
        } catch (e: Exception) {
            android.util.Log.w("CallStateHolder", "registerCallback error: ${e.message}")
        }
    }

    fun setSimInfo(slot: Int, name: String) {
        _callInfo.value = _callInfo.value.copy(simSlot = slot, simDisplayName = name)
    }

    private fun autoSelectPhoneAccountIfNeeded(call: Call, context: Context) {
        try {
            if (call.state == Call.STATE_SELECT_PHONE_ACCOUNT) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
                val accounts = telecomManager?.callCapablePhoneAccounts
                if (!accounts.isNullOrEmpty()) {
                    val defaultAccount = telecomManager.getUserSelectedOutgoingPhoneAccount() ?: accounts.first()
                    call.phoneAccountSelected(defaultAccount, false)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CallStateHolder", "autoSelectPhoneAccountIfNeeded error: ${e.message}")
        }
    }

    fun unregisterCall(call: Call) {
        if (currentTelecomCall == call) {
            currentTelecomCall = null
            stopDurationTimer()
            _callInfo.value = _callInfo.value.copy(
                state = CallUiState.DISCONNECTED
            )
            scope.launch {
                delay(1200)
                _callInfo.value = ActiveCallInfo(state = CallUiState.IDLE)
            }
        }
    }

    fun updateAudioState(audioState: CallAudioState) {
        val isMuted = audioState.isMuted
        val isSpeaker = (audioState.route == CallAudioState.ROUTE_SPEAKER)
        val isBluetooth = (audioState.route == CallAudioState.ROUTE_BLUETOOTH)
        val hasBluetooth = (audioState.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH) != 0

        _callInfo.value = _callInfo.value.copy(
            isMuted = isMuted,
            isSpeakerOn = isSpeaker,
            isBluetoothOn = isBluetooth,
            isBluetoothAvailable = hasBluetooth
        )
    }

    fun setOutgoingCallInfo(number: String, callerName: String) {
        _callInfo.value = _callInfo.value.copy(
            phoneNumber = number,
            callerName = callerName
        )
    }

    private fun updateStateFromTelecom(
        state: Int,
        number: String,
        name: String,
        typeLabel: String = "Mobile",
        photoUri: String? = null
    ) {
        val uiState = when (state) {
            Call.STATE_RINGING -> CallUiState.RINGING
            Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_SELECT_PHONE_ACCOUNT -> CallUiState.DIALING
            Call.STATE_ACTIVE -> {
                startDurationTimer()
                CallUiState.ACTIVE
            }
            Call.STATE_HOLDING -> CallUiState.ON_HOLD
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                stopDurationTimer()
                CallUiState.DISCONNECTED
            }
            else -> CallUiState.IDLE
        }

        val current = _callInfo.value
        val bestName = when {
            name.isNotBlank() && name != "বহির্গামী কল" && name != "অপরিচিত কলার" -> name
            current.callerName.isNotBlank() && current.callerName != "বহির্গামী কল" && current.callerName != "অপরিচিত কলার" -> current.callerName
            else -> name.ifEmpty { current.callerName }
        }

        _callInfo.value = _callInfo.value.copy(
            state = uiState,
            phoneNumber = number.ifEmpty { current.phoneNumber },
            callerName = bestName,
            callerTypeLabel = typeLabel.ifEmpty { current.callerTypeLabel },
            photoUri = photoUri ?: current.photoUri
        )
    }

    private fun startDurationTimer() {
        if (durationJob?.isActive == true) return
        durationJob = scope.launch {
            while (true) {
                delay(1000)
                _callInfo.value = _callInfo.value.copy(
                    durationSeconds = _callInfo.value.durationSeconds + 1
                )
            }
        }
    }

    private fun stopDurationTimer() {
        durationJob?.cancel()
        durationJob = null
    }

    fun answer() {
        currentInCallService?.let {
            SmartInCallService.stopIncomingAlerts(it.applicationContext)
        }
        try {
            currentTelecomCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
        } catch (e: Exception) {
            android.util.Log.w("CallStateHolder", "answer() error: ${e.message}")
        }
    }

    fun reject() {
        currentInCallService?.let {
            SmartInCallService.stopIncomingAlerts(it.applicationContext)
        }
        try {
            currentTelecomCall?.reject(false, null)
        } catch (e: Exception) {
            android.util.Log.w("CallStateHolder", "reject() error: ${e.message}")
        }
        currentTelecomCall?.let { unregisterCall(it) }
    }

    fun disconnect() {
        try {
            currentTelecomCall?.disconnect()
        } catch (e: Exception) {
            android.util.Log.w("CallStateHolder", "disconnect() error: ${e.message}")
        }
    }

    fun toggleMute(context: Context) {
        val newMute = !_callInfo.value.isMuted
        try {
            currentInCallService?.setMuted(newMute)
        } catch (_: Exception) {}
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.isMicrophoneMute = newMute
        } catch (_: Exception) {}
        _callInfo.value = _callInfo.value.copy(isMuted = newMute)
    }

    fun toggleSpeaker(context: Context) {
        val newSpeaker = !_callInfo.value.isSpeakerOn
        try {
            if (newSpeaker) {
                currentInCallService?.setAudioRoute(CallAudioState.ROUTE_SPEAKER)
            } else {
                currentInCallService?.setAudioRoute(CallAudioState.ROUTE_EARPIECE)
            }
        } catch (_: Exception) {}
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.isSpeakerphoneOn = newSpeaker
        } catch (_: Exception) {}
        _callInfo.value = _callInfo.value.copy(
            isSpeakerOn = newSpeaker,
            isBluetoothOn = false
        )
    }

    fun toggleBluetooth(context: Context) {
        val newBluetooth = !_callInfo.value.isBluetoothOn
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (newBluetooth) {
            try {
                currentInCallService?.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH)
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
                audioManager.isSpeakerphoneOn = false
            } catch (_: Exception) {}
            _callInfo.value = _callInfo.value.copy(
                isBluetoothOn = true,
                isSpeakerOn = false
            )
        } else {
            try {
                currentInCallService?.setAudioRoute(CallAudioState.ROUTE_EARPIECE)
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            } catch (_: Exception) {}
            _callInfo.value = _callInfo.value.copy(isBluetoothOn = false)
        }
    }

    fun setRecording(isRecording: Boolean) {
        _callInfo.value = _callInfo.value.copy(isRecording = isRecording)
    }
}
