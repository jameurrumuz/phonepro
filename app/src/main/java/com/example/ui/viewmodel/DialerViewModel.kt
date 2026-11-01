package com.example.ui.viewmodel

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.analytics.CallAnalyticsHelper
import com.example.analytics.CallStatsPeriod
import com.example.analytics.ContactCallSummary
import com.example.analytics.DataUsageInfo
import com.example.analytics.NetworkDataMonitor
import com.example.data.AppDatabase
import com.example.data.entity.BlockedNumberEntity
import com.example.data.entity.CallLogEntity
import com.example.data.entity.CallNoteEntity
import com.example.data.entity.CallRecordingEntity
import com.example.data.entity.SpamRuleEntity
import com.example.data.model.ContactItem
import com.example.data.model.SmsConversation
import com.example.data.model.SmsMessage
import com.example.data.repository.AutoBackupFrequency
import com.example.data.repository.BackupManager
import com.example.data.repository.BackupStateInfo
import com.example.data.repository.ContactLookupHelper
import com.example.data.repository.ContactNoWhatsAppManager
import com.example.data.repository.ContactOcrScanner
import com.example.data.repository.ContactsRepository
import com.example.data.repository.RealCallLogRepository
import com.example.data.repository.ScannedContactResult
import com.example.data.repository.SmsRepository
import com.example.recorder.CallRecorderManager
import com.example.recorder.PlayerStatus
import com.example.recorder.RecordingStatus
import com.example.security.SecurityLockManager
import com.example.service.CallStateHolder
import com.example.service.BlockedCallCache
import com.example.service.UnreadBlockedSpamManager
import com.example.telecom.MessengerCallingHelper
import com.example.telecom.SimAccountInfo
import com.example.telecom.SimManager
import com.example.telecom.TelecomPermissionHelper
import com.example.telecom.WhatsAppCallingHelper
import com.example.ui.theme.ThemeMode
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

enum class MainTab {
    RECENTS, // Default Landing Screen
    MESSAGES,
    DIALER,
    CONTACTS,
    ANALYTICS,
    SETTINGS,
    SECURITY_RECORD
}

data class FallbackCallState(
    val isOpen: Boolean = false,
    val phoneNumber: String = "",
    val callerName: String = "",
    val countdown: Int = 10
)

data class SimSelectionState(
    val isOpen: Boolean = false,
    val phoneNumber: String = "",
    val callerName: String = "",
    val isUssd: Boolean = false,
    val availableSims: List<SimAccountInfo> = emptyList(),
    val rememberedSlot: Int? = null
)

data class OcrScannerState(
    val isOpen: Boolean = false,
    val isScanning: Boolean = false,
    val result: ScannedContactResult? = null
)

class DialerViewModel(private val context: Context) : ViewModel() {

    private val db = AppDatabase.getDatabase(context)
    val contactsRepository = ContactsRepository(context)
    val realCallLogRepo = RealCallLogRepository(context)
    val smsRepository = SmsRepository(context)
    val backupManager = BackupManager(context)
    val recorderManager = CallRecorderManager(context)
    val dataMonitor = NetworkDataMonitor(context)

    private val prefs = context.getSharedPreferences("smart_dialer_prefs", Context.MODE_PRIVATE)

    // Language state (Bangla & English)
    private val savedLangName = prefs.getString("pref_language", AppLanguage.BANGLA.name) ?: AppLanguage.BANGLA.name
    private val _appLanguage = MutableStateFlow(
        try { AppLanguage.valueOf(savedLangName) } catch (_: Exception) { AppLanguage.BANGLA }
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    fun strings(): AppStrings.Strings = AppStrings.get(_appLanguage.value)

    fun setAppLanguage(lang: AppLanguage) {
        _appLanguage.value = lang
        prefs.edit().putString("pref_language", lang.name).apply()
    }

    // Theme Mode state (Light, Dark, System)
    private val savedThemeName = prefs.getString("pref_theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
    private val _themeMode = MutableStateFlow(
        try { ThemeMode.valueOf(savedThemeName) } catch (_: Exception) { ThemeMode.SYSTEM }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("pref_theme_mode", mode.name).apply()
    }

    // Landing screen is RECENTS as requested
    private val _currentTab = MutableStateFlow(MainTab.RECENTS)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _dialpadText = MutableStateFlow("")
    val dialpadText: StateFlow<String> = _dialpadText.asStateFlow()

    private val _isWhatsAppFirst = MutableStateFlow(prefs.getBoolean("pref_wa_first", true))
    val isWhatsAppFirst: StateFlow<Boolean> = _isWhatsAppFirst.asStateFlow()

    private val _isDefaultDialer = MutableStateFlow(TelecomPermissionHelper.isDefaultDialer(context))
    val isDefaultDialer: StateFlow<Boolean> = _isDefaultDialer.asStateFlow()

    private val _isDefaultSmsApp = MutableStateFlow(TelecomPermissionHelper.isDefaultSmsApp(context))
    val isDefaultSmsApp: StateFlow<Boolean> = _isDefaultSmsApp.asStateFlow()

    private val _canUseFullScreenIntent = MutableStateFlow(TelecomPermissionHelper.canUseFullScreenIntent(context))
    val canUseFullScreenIntent: StateFlow<Boolean> = _canUseFullScreenIntent.asStateFlow()

    private val _isDefaultSmsBannerDismissed = MutableStateFlow(
        prefs.getBoolean("pref_sms_banner_dismissed", false)
    )
    val isDefaultSmsBannerDismissed: StateFlow<Boolean> = _isDefaultSmsBannerDismissed.asStateFlow()

    private val _missedCallReminderInterval = MutableStateFlow(
        prefs.getInt("pref_missed_call_reminder_min", 15)
    )
    val missedCallReminderInterval: StateFlow<Int> = _missedCallReminderInterval.asStateFlow()

    private val _scheduledBackupTime = MutableStateFlow(
        prefs.getString("scheduled_backup_time", "02:00 AM") ?: "02:00 AM"
    )
    val scheduledBackupTime: StateFlow<String> = _scheduledBackupTime.asStateFlow()

    private val _backupStatusMessage = MutableStateFlow<String?>(null)
    val backupStatusMessage: StateFlow<String?> = _backupStatusMessage.asStateFlow()

    private val _fallbackCallState = MutableStateFlow(FallbackCallState())
    val fallbackCallState: StateFlow<FallbackCallState> = _fallbackCallState.asStateFlow()

    // Pre-call SIM selection dialog state
    private val _simSelectionState = MutableStateFlow(SimSelectionState())
    val simSelectionState: StateFlow<SimSelectionState> = _simSelectionState.asStateFlow()

    // Camera OCR Scanner state
    private val _ocrScannerState = MutableStateFlow(OcrScannerState())
    val ocrScannerState: StateFlow<OcrScannerState> = _ocrScannerState.asStateFlow()

    // In-call scratchpad active note
    private val _activeCallNoteText = MutableStateFlow("")
    val activeCallNoteText: StateFlow<String> = _activeCallNoteText.asStateFlow()

    private val _dataUsage = MutableStateFlow(dataMonitor.getUsageInfo())
    val dataUsage: StateFlow<DataUsageInfo> = _dataUsage.asStateFlow()

    // Backup State
    private val _backupState = MutableStateFlow(backupManager.getBackupState())
    val backupState: StateFlow<BackupStateInfo> = _backupState.asStateFlow()

    // Date-wise analytics selected date (default today)
    private val _selectedAnalyticsDate = MutableStateFlow(System.currentTimeMillis())
    val selectedAnalyticsDate: StateFlow<Long> = _selectedAnalyticsDate.asStateFlow()

    // Contacts marked as having No WhatsApp / Inactive on WhatsApp
    val markedNoWhatsApp: StateFlow<Set<String>> = ContactNoWhatsAppManager.markedNumbers

    // Real combined call logs
    private val roomCallLogs = db.callLogDao().getAllCallLogs()
    private val systemCallLogs = realCallLogRepo.systemCallLogs

    val allCallLogs: StateFlow<List<CallLogEntity>> = combine(roomCallLogs, systemCallLogs) { roomList, sysList ->
        (roomList + sysList)
            .distinctBy { "${it.number}_${it.timestamp / 1000}" }
            .sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val blockedNumbers: StateFlow<List<BlockedNumberEntity>> = db.blockedNumberDao().getAllBlockedNumbers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val callRecordings: StateFlow<List<CallRecordingEntity>> = db.callRecordingDao().getAllRecordings()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val spamRules: StateFlow<List<SpamRuleEntity>> = db.spamRuleDao().getAllRules()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val callNotes: StateFlow<List<CallNoteEntity>> = db.callNoteDao().getAllNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val contacts: StateFlow<List<ContactItem>> = contactsRepository.contacts
    val isContactsLoading: StateFlow<Boolean> = contactsRepository.isLoading
    val hasContactsPermission: StateFlow<Boolean> = contactsRepository.hasPermissionFlow

    // Player and recorder states
    val recordingStatus: StateFlow<RecordingStatus> = recorderManager.recordingStatus
    val playerStatus: StateFlow<PlayerStatus> = recorderManager.playerStatus

    // In-Call state
    val activeCall = CallStateHolder.callInfo

    // SMS States
    val smsConversations: StateFlow<List<SmsConversation>> = smsRepository.conversations
    val activeSmsMessages: StateFlow<List<SmsMessage>> = smsRepository.messagesForThread
    val activeThreadId: StateFlow<Long?> = smsRepository.activeThreadId

    // Computed smart contact search for typed digits
    val matchedContacts: StateFlow<List<ContactItem>> = combine(dialpadText, contacts) { text, contactList ->
        if (text.isBlank()) emptyList()
        else {
            val query = text.trim()
            contactList.filter {
                it.phoneNumber.contains(query) || it.name.contains(query, ignoreCase = true)
            }.take(5)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Smart Favorites & Frequent Contacts (Deduplicated by normalized number)
    val smartFavorites: StateFlow<List<ContactItem>> = combine(contacts, allCallLogs) { contactList, logs ->
        val logCounts = logs.groupBy { log ->
            val digits = log.number.replace("[^0-9]".toRegex(), "")
            if (digits.length >= 10) digits.takeLast(10) else digits
        }

        contactList
            .distinctBy { contact ->
                val digits = contact.phoneNumber.replace("[^0-9]".toRegex(), "")
                if (digits.length >= 10) digits.takeLast(10) else digits
            }
            .map { contact ->
                val digits = contact.phoneNumber.replace("[^0-9]".toRegex(), "")
                val key = if (digits.length >= 10) digits.takeLast(10) else digits
                val matchingCalls = logCounts[key] ?: emptyList()
                val totalSec = matchingCalls.sumOf { it.durationSeconds }
                val count = matchingCalls.size
                contact.copy(
                    callCount = count,
                    totalTalkSeconds = totalSec,
                    isFavorite = contact.isFavorite || count >= 2,
                    hasWhatsApp = !ContactNoWhatsAppManager.isMarkedNoWhatsApp(context, contact.phoneNumber),
                    hasMessenger = true
                )
            }
            .filter { it.isFavorite || it.callCount > 0 }
            .sortedWith(compareByDescending<ContactItem> { it.isFavorite }.thenByDescending { it.totalTalkSeconds })
            .take(10)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Estimated Money Saved in Taka
    val estimatedSavingsTaka: StateFlow<Double> = allCallLogs.combine(_dialpadText) { logs, _ ->
        val waSeconds = logs.filter { it.callMode == "WHATSAPP" }.sumOf { it.durationSeconds }
        val waMinutes = waSeconds / 60.0
        (waMinutes * 1.50)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val dailyStats: StateFlow<CallStatsPeriod> = allCallLogs.combine(_dialpadText) { logs, _ ->
        val startOfDay = CallAnalyticsHelper.getStartOfDayTimestamp()
        CallAnalyticsHelper.calculateStats(logs, startOfDay)
    }.stateIn(viewModelScope, SharingStarted.Lazily, CallStatsPeriod())

    val monthlyStats: StateFlow<CallStatsPeriod> = allCallLogs.combine(_dialpadText) { logs, _ ->
        val startOfMonth = CallAnalyticsHelper.getStartOfMonthTimestamp()
        CallAnalyticsHelper.calculateStats(logs, startOfMonth)
    }.stateIn(viewModelScope, SharingStarted.Lazily, CallStatsPeriod())

    // Date-wise authentic stats for selected date
    val dateStats: StateFlow<CallStatsPeriod> = combine(allCallLogs, selectedAnalyticsDate) { logs, dateTs ->
        val cal = Calendar.getInstance().apply { timeInMillis = dateTs }
        val start = CallAnalyticsHelper.getStartOfDayTimestamp(cal)
        val end = CallAnalyticsHelper.getEndOfDayTimestamp(cal)
        CallAnalyticsHelper.calculateStats(logs, start, end)
    }.stateIn(viewModelScope, SharingStarted.Lazily, CallStatsPeriod())

    val topContacts: StateFlow<List<ContactCallSummary>> = allCallLogs.combine(_dialpadText) { logs, _ ->
        CallAnalyticsHelper.getTopContacts(logs, 5)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val dateTopContacts: StateFlow<List<ContactCallSummary>> = combine(allCallLogs, selectedAnalyticsDate) { logs, dateTs ->
        val cal = Calendar.getInstance().apply { timeInMillis = dateTs }
        val start = CallAnalyticsHelper.getStartOfDayTimestamp(cal)
        val end = CallAnalyticsHelper.getEndOfDayTimestamp(cal)
        CallAnalyticsHelper.getTopContacts(logs, 5, start, end)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Unread Blocked Spam Calls Count
    val unreadBlockedSpamCount: StateFlow<Int> = UnreadBlockedSpamManager.unreadCount

    fun clearUnreadBlockedSpam() {
        UnreadBlockedSpamManager.clear(context)
    }

    init {
        viewModelScope.launch {
            BlockedCallCache.init(context)
            cleanOldDemoLogsIfNeeded()
            deduplicateBlockedNumbers()
            SecurityLockManager.init(context)
            UnreadBlockedSpamManager.init(context)
            com.example.telecom.SpeedDialManager.init(context)
            ContactNoWhatsAppManager.init(context)
            contactsRepository.loadContacts()
            realCallLogRepo.syncRealCallLogs()
            smsRepository.loadConversations()
            refreshDataUsage()
            refreshBackupState()
            checkAutoBackup()
        }
    }

    val activeOtpBanner: StateFlow<com.example.sms.ActiveOtpBanner?> = com.example.sms.OtpOverlayManager.activeBanner

    fun dismissOtpBanner() {
        com.example.sms.OtpOverlayManager.dismissBanner()
    }

    fun copyOtpBannerCode() {
        com.example.sms.OtpOverlayManager.copyManually(context)
    }

    fun refreshContacts() {
        contactsRepository.updatePermissionStatus()
        viewModelScope.launch {
            contactsRepository.loadContacts()
        }
    }

    fun onContactsPermissionResult(granted: Boolean) {
        contactsRepository.updatePermissionStatus()
        if (granted) {
            contactsRepository.ensureObserverRegistered()
            refreshContacts()
        } else {
            contactsRepository.handlePermissionRevocation()
        }
    }

    fun refreshSms() {
        viewModelScope.launch {
            smsRepository.loadConversations()
        }
    }

    private suspend fun deduplicateBlockedNumbers() {
        try {
            val all = db.blockedNumberDao().getAllBlockedNumbersSync()
            val seen = mutableSetOf<String>()
            for (item in all) {
                val key = item.phoneNumber.replace("[^0-9]".toRegex(), "").takeLast(10)
                if (key.length >= 7 && seen.contains(key)) {
                    db.blockedNumberDao().delete(item)
                } else if (key.length >= 7) {
                    seen.add(key)
                }
            }
        } catch (_: Exception) {}
    }

    private suspend fun cleanOldDemoLogsIfNeeded() {
        val isCleaned = prefs.getBoolean("demo_logs_cleaned_v2", false)
        if (!isCleaned) {
            db.callLogDao().clearAll()
            prefs.edit().putBoolean("demo_logs_cleaned_v2", true).apply()
        }
    }

    fun syncCallLogs() {
        viewModelScope.launch {
            realCallLogRepo.syncRealCallLogs()
            smsRepository.loadConversations()
        }
    }

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun onDialpadDigit(char: Char) {
        _dialpadText.value += char
    }

    fun onDialpadBackspace() {
        if (_dialpadText.value.isNotEmpty()) {
            _dialpadText.value = _dialpadText.value.dropLast(1)
        }
    }

    fun onDialpadClear() {
        _dialpadText.value = ""
    }

    fun appendDialpadText(text: String) {
        val clean = text.replace("[^0-9*#+]".toRegex(), "")
        _dialpadText.value += clean
    }

    fun setDialpadNumber(number: String) {
        _dialpadText.value = number
    }

    fun setSelectedAnalyticsDate(timestamp: Long) {
        _selectedAnalyticsDate.value = timestamp
    }

    fun toggleWhatsAppFirst() {
        val next = !_isWhatsAppFirst.value
        _isWhatsAppFirst.value = next
        prefs.edit().putBoolean("pref_wa_first", next).apply()
    }

    fun refreshDefaultDialerStatus() {
        _isDefaultDialer.value = TelecomPermissionHelper.isDefaultDialer(context)
        _isDefaultSmsApp.value = TelecomPermissionHelper.isDefaultSmsApp(context)
        _canUseFullScreenIntent.value = TelecomPermissionHelper.canUseFullScreenIntent(context)
    }

    fun requestFullScreenIntentPermission(context: Context) {
        TelecomPermissionHelper.requestFullScreenIntentPermission(context)
    }

    fun dismissDefaultSmsBanner() {
        _isDefaultSmsBannerDismissed.value = true
        prefs.edit().putBoolean("pref_sms_banner_dismissed", true).apply()
    }

    fun resetDefaultSmsBanner() {
        _isDefaultSmsBannerDismissed.value = false
        prefs.edit().putBoolean("pref_sms_banner_dismissed", false).apply()
    }

    fun setMissedCallReminderInterval(minutes: Int) {
        _missedCallReminderInterval.value = minutes
        prefs.edit().putInt("pref_missed_call_reminder_min", minutes).apply()
    }

    fun setScheduledBackupTime(timeStr: String) {
        _scheduledBackupTime.value = timeStr
        prefs.edit().putString("scheduled_backup_time", timeStr).apply()
        backupManager.setScheduledBackupTime(timeStr)
    }

    fun clearBackupStatusMessage() {
        _backupStatusMessage.value = null
    }

    fun performDirectBackupNow() {
        viewModelScope.launch {
            try {
                val file = backupManager.exportLocalBackup()
                refreshBackupState()
                _backupStatusMessage.value = if (_appLanguage.value == AppLanguage.BANGLA) {
                    "ব্যাকআপ সফলভাবে সম্পন্ন হয়েছে: " + file.name
                } else {
                    "Backup completed successfully: " + file.name
                }
            } catch (e: Exception) {
                _backupStatusMessage.value = if (_appLanguage.value == AppLanguage.BANGLA) {
                    "ব্যাকআপ ব্যর্থ হয়েছে: " + (e.message ?: "Error")
                } else {
                    "Backup failed: " + (e.message ?: "Error")
                }
            }
        }
    }

    fun requestSetDefaultDialer(activity: Activity) {
        TelecomPermissionHelper.requestDefaultDialer(activity)
    }

    fun requestSetDefaultSms(activity: Activity) {
        TelecomPermissionHelper.requestDefaultSms(activity)
    }

    // SMS functions
    fun loadSmsThread(threadId: Long) {
        viewModelScope.launch {
            smsRepository.loadMessagesForThread(threadId)
        }
    }

    fun loadSmsForAddress(address: String) {
        viewModelScope.launch {
            smsRepository.loadMessagesForAddress(address)
        }
    }

    fun sendSmsMessage(destination: String, text: String, simSlot: Int? = null, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = smsRepository.sendSms(destination, text, simSlot)
            onResult(success)
        }
    }

    fun sendContactViaSms(
        destinationNumber: String,
        contactName: String,
        contactPhone: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        val messageText = "পরিচিতি: $contactName\nমোবাইল নম্বর: $contactPhone"
        sendSmsMessage(destinationNumber, messageText, null, onResult)
    }

    fun clearActiveSmsThread() {
        smsRepository.clearActiveThread()
    }

    // Backup & Restore functions
    fun refreshBackupState() {
        _backupState.value = backupManager.getBackupState()
    }

    fun setAutoBackupFrequency(freq: AutoBackupFrequency) {
        backupManager.setAutoBackupFrequency(freq)
        refreshBackupState()
    }

    fun createLocalBackup(onDone: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val file = backupManager.exportLocalBackup()
                refreshBackupState()
                onDone(file)
            } catch (e: Exception) {
                onDone(null)
            }
        }
    }

    fun launchGoogleDriveBackup(onIntentReady: (Intent) -> Unit) {
        viewModelScope.launch {
            try {
                val intent = backupManager.performGoogleDriveBackupIntent()
                refreshBackupState()
                onIntentReady(intent)
            } catch (e: Exception) {
                android.util.Log.e("DialerViewModel", "Google Drive backup intent failed: ${e.message}")
            }
        }
    }

    fun restoreFromJson(jsonString: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.restoreFromJson(jsonString)
            if (result) {
                syncCallLogs()
                refreshBackupState()
            }
            onDone(result)
        }
    }

    private fun checkAutoBackup() {
        viewModelScope.launch {
            if (backupManager.checkAndTriggerAutoBackupIfNeeded()) {
                try {
                    backupManager.exportLocalBackup()
                    refreshBackupState()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Resolves genuine contact name for a number.
     * Guaranteed to never return "বহির্গামী কল" for a saved contact.
     */
    fun resolveContactName(number: String): String {
        val fromLookup = ContactLookupHelper.resolveContactName(context, number)
        if (!fromLookup.isNullOrBlank()) return fromLookup

        val inMemory = contacts.value.find {
            it.phoneNumber.replace("[^0-9]".toRegex(), "") == number.replace("[^0-9]".toRegex(), "")
        }
        return inMemory?.name ?: ""
    }

    /**
     * Primary smart calling dispatcher:
     * 1. Resolves contact name so saved numbers NEVER show "বহির্গামী কল".
     * 2. Checks if marked as "No WhatsApp".
     * 3. Checks preferred SIM or opens SIM selection sheet.
     */
    fun makeCall(
        rawNumber: String,
        callerName: String = "",
        forceCellular: Boolean = false,
        explicitSimSlot: Int? = null
    ) {
        val number = rawNumber.trim()
        if (number.isBlank()) return

        val isUssd = WhatsAppCallingHelper.isUssdCode(number)
        val resolvedName = if (callerName.isNotBlank() && callerName != "বহির্গামী কল" && callerName != "অপরিচিত কলার") {
            callerName
        } else {
            resolveContactName(number)
        }

        // Set immediate info on in-call screen
        CallStateHolder.setOutgoingCallInfo(number, resolvedName)

        val isMarkedNoWa = ContactNoWhatsAppManager.isMarkedNoWhatsApp(context, number)

        // 1. If USSD code or marked as No WhatsApp or forced cellular:
        if (isUssd || isMarkedNoWa || forceCellular || !_isWhatsAppFirst.value) {
            placeCellularWithSimCheck(number, resolvedName, isUssd, explicitSimSlot)
            return
        }

        // 2. WhatsApp first route
        val openedWhatsApp = WhatsAppCallingHelper.openWhatsAppCall(context, number)
        if (openedWhatsApp) {
            viewModelScope.launch {
                db.callLogDao().insert(
                    CallLogEntity(
                        number = number,
                        name = resolvedName.ifEmpty { number },
                        type = "OUTGOING",
                        timestamp = System.currentTimeMillis(),
                        durationSeconds = 0,
                        callMode = "WHATSAPP"
                    )
                )
            }

            // Show fallback prompt with 1-click cellular call option
            _fallbackCallState.value = FallbackCallState(
                isOpen = true,
                phoneNumber = number,
                callerName = resolvedName,
                countdown = 10
            )
        } else {
            // WhatsApp not available, place cellular call with SIM check
            placeCellularWithSimCheck(number, resolvedName, isUssd = false, explicitSimSlot = explicitSimSlot)
        }
    }

    /**
     * Checks if preferred SIM is set or if pre-call SIM dialog is needed
     */
    private fun placeCellularWithSimCheck(
        number: String,
        resolvedName: String,
        isUssd: Boolean,
        explicitSimSlot: Int?
    ) {
        val availableSims = SimManager.getAvailableSims(context)
        val preferredSlot = explicitSimSlot ?: SimManager.getPreferredSimSlot(context, number)

        if (preferredSlot != null) {
            // We have a remembered or explicitly chosen SIM
            val targetSim = availableSims.find { it.slotIndex == preferredSlot } ?: availableSims.firstOrNull()
            executeCellularCall(number, resolvedName, targetSim)
        } else if (availableSims.size > 1) {
            // Multiple SIMs available and no preference stored: show SIM selection prompt!
            _simSelectionState.value = SimSelectionState(
                isOpen = true,
                phoneNumber = number,
                callerName = resolvedName,
                isUssd = isUssd,
                availableSims = availableSims,
                rememberedSlot = null
            )
        } else {
            // Single SIM
            executeCellularCall(number, resolvedName, availableSims.firstOrNull())
        }
    }

    private fun executeCellularCall(number: String, resolvedName: String, simInfo: SimAccountInfo?) {
        WhatsAppCallingHelper.launchCellularCall(context, number, simInfo)
        viewModelScope.launch {
            db.callLogDao().insert(
                CallLogEntity(
                    number = number,
                    name = resolvedName.ifEmpty { number },
                    type = "OUTGOING",
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = 0,
                    callMode = "CELLULAR"
                )
            )
        }
    }

    fun showSimSelectionDialog(number: String, callerName: String = "", isUssd: Boolean = false) {
        val clean = number.trim()
        if (clean.isBlank()) return
        val available = SimManager.getAvailableSims(context)
        val resolved = callerName.ifEmpty { resolveContactName(clean) }
        _simSelectionState.value = SimSelectionState(
            isOpen = true,
            phoneNumber = clean,
            callerName = resolved,
            isUssd = isUssd,
            availableSims = available,
            rememberedSlot = SimManager.getPreferredSimSlot(context, clean)
        )
    }

    /**
     * Called when user selects SIM from Pre-Call Dialog
     */
    fun selectSimAndProceedCall(sim: SimAccountInfo, alwaysUseForThisNumber: Boolean) {
        val state = _simSelectionState.value
        _simSelectionState.value = SimSelectionState(isOpen = false)

        if (state.phoneNumber.isBlank()) return

        if (alwaysUseForThisNumber) {
            SimManager.setPreferredSimSlot(context, state.phoneNumber, sim.slotIndex)
        }

        executeCellularCall(state.phoneNumber, state.callerName, sim)
    }

    fun dismissSimSelection() {
        _simSelectionState.value = SimSelectionState(isOpen = false)
    }

    /**
     * Redial using the other SIM (Switch SIM feature requested by user)
     */
    fun switchSimAndCall(phoneNumber: String, currentSlot: Int? = null) {
        val available = SimManager.getAvailableSims(context)
        val targetSlot = if (currentSlot == 0) 1 else 0
        val targetSim = available.find { it.slotIndex == targetSlot } ?: available.getOrNull(targetSlot) ?: available.firstOrNull()
        val name = resolveContactName(phoneNumber)
        executeCellularCall(phoneNumber, name, targetSim)
    }

    /**
     * Switch SIM directly during active call: ends current call and redials with opposite SIM
     */
    fun switchActiveCallSim() {
        val number = activeCall.value.phoneNumber
        if (number.isBlank()) return
        CallStateHolder.disconnect()
        viewModelScope.launch {
            kotlinx.coroutines.delay(600)
            switchSimAndCall(number, currentSlot = 0)
        }
    }

    /**
     * Marks contact as having No WhatsApp / Inactive on WhatsApp
     */
    fun toggleMarkNoWhatsApp(phoneNumber: String) {
        ContactNoWhatsAppManager.toggleMarkedNoWhatsApp(context, phoneNumber)
    }

    fun isMarkedNoWhatsApp(phoneNumber: String): Boolean {
        return ContactNoWhatsAppManager.isMarkedNoWhatsApp(context, phoneNumber)
    }

    private val callNoteDebouncer = com.example.ui.util.CallNoteDebouncer(viewModelScope) { phoneNumber, text ->
        val callInfo = activeCall.value
        val callerName = if (callInfo.phoneNumber == phoneNumber) callInfo.callerName else ""
        saveCallNote(phoneNumber, callerName, text)
    }

    /**
     * Auto-saves in-call scratchpad notes into Room database with 600ms debounce
     */
    fun updateActiveCallNote(text: String) {
        _activeCallNoteText.value = text
        val callInfo = activeCall.value
        if (callInfo.phoneNumber.isNotBlank()) {
            callNoteDebouncer.onTextChanged(callInfo.phoneNumber, text)
        }
    }

    fun saveCallNote(phoneNumber: String, callerName: String, text: String) {
        if (text.isBlank() || phoneNumber.isBlank()) return
        viewModelScope.launch {
            val existing = db.callNoteDao().getLatestNoteForNumber(phoneNumber)
            if (existing != null && (System.currentTimeMillis() - existing.timestamp) < 300000) {
                // Update within 5 minutes of ongoing session
                db.callNoteDao().update(existing.copy(noteText = text, timestamp = System.currentTimeMillis()))
            } else {
                db.callNoteDao().insert(
                    CallNoteEntity(
                        phoneNumber = phoneNumber,
                        callerName = callerName.ifEmpty { resolveContactName(phoneNumber) },
                        noteText = text,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun deleteCallNote(note: CallNoteEntity) {
        viewModelScope.launch {
            db.callNoteDao().delete(note)
        }
    }

    // Camera OCR Scanner methods
    fun openOcrScanner() {
        _ocrScannerState.value = OcrScannerState(isOpen = true)
    }

    fun closeOcrScanner() {
        _ocrScannerState.value = OcrScannerState(isOpen = false)
    }

    fun processOcrBitmap(bitmap: Bitmap) {
        _ocrScannerState.value = _ocrScannerState.value.copy(isScanning = true)
        viewModelScope.launch {
            val result = ContactOcrScanner.scanImageForContacts(bitmap)
            _ocrScannerState.value = _ocrScannerState.value.copy(
                isScanning = false,
                result = result
            )
        }
    }

    fun makeMessengerCall(contactName: String) {
        MessengerCallingHelper.openMessenger(context, contactName)
    }

    fun triggerCellularFallbackNow() {
        val state = _fallbackCallState.value
        _fallbackCallState.value = FallbackCallState(isOpen = false)
        if (state.phoneNumber.isNotBlank()) {
            makeCall(state.phoneNumber, state.callerName, forceCellular = true)
        }
    }

    fun dismissFallbackDialog() {
        _fallbackCallState.value = FallbackCallState(isOpen = false)
    }

    fun blockNumber(number: String, name: String = "", reason: String = "স্প্যাম বা অবাঞ্ছিত কলার") {
        viewModelScope.launch {
            val cleanIncoming = number.replace("[^0-9]".toRegex(), "").takeLast(10)
            val blockedList = db.blockedNumberDao().getAllBlockedNumbersSync()
            val alreadyBlocked = blockedList.find {
                it.phoneNumber.replace("[^0-9]".toRegex(), "").takeLast(10) == cleanIncoming
            }
            if (alreadyBlocked != null) {
                // Already blocked: prevent duplicate entry, update blockCount
                db.blockedNumberDao().incrementBlockCount(alreadyBlocked.phoneNumber)
                return@launch
            }
            val newEntity = BlockedNumberEntity(
                phoneNumber = number,
                callerName = name,
                reason = reason,
                blockCount = 1,
                isSpam = true
            )
            db.blockedNumberDao().insert(newEntity)
            BlockedCallCache.addPermanentBlocked(newEntity)
        }
    }

    fun unblockNumber(blocked: BlockedNumberEntity) {
        viewModelScope.launch {
            db.blockedNumberDao().delete(blocked)
            BlockedCallCache.removePermanentBlocked(blocked.phoneNumber)
        }
    }

    fun deleteCallLog(callLog: CallLogEntity) {
        viewModelScope.launch {
            db.callLogDao().delete(callLog)
        }
    }

    fun clearAllCallLogs() {
        viewModelScope.launch {
            db.callLogDao().clearAll()
        }
    }

    fun deleteRecording(recording: CallRecordingEntity) {
        recorderManager.deleteRecording(recording)
    }

    fun playRecording(recording: CallRecordingEntity) {
        recorderManager.playRecording(recording)
    }

    fun pausePlayer() {
        recorderManager.pausePlayer()
    }

    fun stopPlayer() {
        recorderManager.stopPlayer()
    }

    fun refreshDataUsage() {
        _dataUsage.value = dataMonitor.getUsageInfo()
    }

    // In-call actions
    fun answerCall() = CallStateHolder.answer()
    fun rejectCall() = CallStateHolder.reject()
    fun endCall() = CallStateHolder.disconnect()
    fun toggleMute() = CallStateHolder.toggleMute(context)
    fun toggleSpeaker() = CallStateHolder.toggleSpeaker(context)
    fun toggleBluetooth() = CallStateHolder.toggleBluetooth(context)
    fun toggleCallRecording(callerNumber: String, callerName: String) {
        if (activeCall.value.isRecording) {
            recorderManager.stopRecording(callerNumber, callerName)
        } else {
            recorderManager.startRecording(callerNumber, callerName)
        }
    }

    fun openSaveToGoogleContacts(number: String, name: String = "") {
        val clean = number.trim()
        if (clean.isBlank()) return
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, clean)
            if (name.isNotBlank()) {
                putExtra(android.provider.ContactsContract.Intents.Insert.NAME, name)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val altIntent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                type = android.provider.ContactsContract.Contacts.CONTENT_ITEM_TYPE
                putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, clean)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(altIntent)
            } catch (_: Exception) {}
        }
    }
}
