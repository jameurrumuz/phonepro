package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.security.SecurityLockManager
import com.example.service.CallUiState
import com.example.service.SmartInCallService
import com.example.ui.components.DialerBottomNavBar
import com.example.ui.components.FloatingOtpBanner
import com.example.ui.components.IncomingCallHeadsUpBanner
import com.example.ui.components.WhatsAppFallbackDialog
import com.example.ui.dialogs.OcrScannerDialog
import com.example.ui.dialogs.SecurityPinDialog
import com.example.ui.dialogs.SimSelectionDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DialerPadScreen
import com.example.ui.screens.InCallScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.RecentsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpamAndRecordingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.util.AppStrings
import com.example.ui.viewmodel.DialerViewModel
import com.example.ui.viewmodel.MainTab

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: DialerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enable screen turn-on and showing over lockscreen for incoming and active calls
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        viewModel = DialerViewModel(applicationContext)
        handleIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDefaultDialerStatus()
        viewModel.refreshDataUsage()
        viewModel.syncCallLogs()
        viewModel.refreshContacts()
    }

    override fun onStop() {
        super.onStop()
        // Instantly relock protected features when user minimizes app, switches tasks, or locks screen
        SecurityLockManager.onAppBackgrounded()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        // Handle answer/reject from incoming call notification
        when (intent.action) {
            SmartInCallService.ACTION_ANSWER -> {
                viewModel.answerCall()
                return
            }
            SmartInCallService.ACTION_REJECT -> {
                viewModel.rejectCall()
                return
            }
            com.example.service.MissedCallNotifier.ACTION_CALL_BACK -> {
                val number = intent.getStringExtra(com.example.service.MissedCallNotifier.EXTRA_NUMBER) ?: ""
                val name = intent.getStringExtra(com.example.service.MissedCallNotifier.EXTRA_NAME) ?: ""
                if (number.isNotBlank()) {
                    com.example.service.MissedCallNotifier.dismissReminder(this, number)
                    viewModel.makeCall(number, name)
                }
                return
            }
        }

        val data = intent.data
        if (data != null) {
            if (data.scheme == "tel") {
                val number = data.schemeSpecificPart
                if (!number.isNullOrBlank()) {
                    viewModel.setDialpadNumber(number)
                    viewModel.selectTab(MainTab.DIALER)
                }
            } else if (data.scheme == "sms" || data.scheme == "smsto") {
                val number = data.schemeSpecificPart
                if (!number.isNullOrBlank()) {
                    viewModel.loadSmsForAddress(number)
                    viewModel.selectTab(MainTab.MESSAGES)
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: DialerViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()
    val fallbackCallState by viewModel.fallbackCallState.collectAsState()
    val simSelectionState by viewModel.simSelectionState.collectAsState()
    val ocrScannerState by viewModel.ocrScannerState.collectAsState()
    val activeNoteText by viewModel.activeCallNoteText.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val unreadSpamCount by viewModel.unreadBlockedSpamCount.collectAsState()
    val activeOtpBanner by viewModel.activeOtpBanner.collectAsState()
    val strings = viewModel.strings()

    var lockedTabPromptTarget by remember { mutableStateOf<MainTab?>(null) }
    var isIncomingFullScreenRequested by remember { mutableStateOf(true) }
    var isInCallShareContactOpen by remember { mutableStateOf(false) }
    val contacts by viewModel.contacts.collectAsState()

    LaunchedEffect(activeCall.state) {
        if (activeCall.state == CallUiState.IDLE) {
            isIncomingFullScreenRequested = true
        }
    }

    // Dynamic backlight management: only keep screen awake during active or ringing calls
    val hostActivity = context as? ComponentActivity
    LaunchedEffect(activeCall.state) {
        if (activeCall.state == CallUiState.ACTIVE || activeCall.state == CallUiState.RINGING) {
            hostActivity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            hostActivity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun handleTabSelection(targetTab: MainTab) {
        if (currentTab != targetTab) {
            // Relock previous tab when user navigates away, so re-entering requires PIN again
            SecurityLockManager.relockTab(currentTab)
        }
        if (SecurityLockManager.isFeatureLocked(context, targetTab)) {
            lockedTabPromptTarget = targetTab
        } else {
            if (targetTab == MainTab.SETTINGS || targetTab == MainTab.SECURITY_RECORD) {
                viewModel.clearUnreadBlockedSpam()
            }
            viewModel.selectTab(targetTab)
        }
    }

    // Request runtime permissions on first launch
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.refreshDefaultDialerStatus()
        viewModel.syncCallLogs()
        val readGranted = permissions[Manifest.permission.READ_CONTACTS] == true
        viewModel.onContactsPermissionResult(readGranted)
        viewModel.refreshSms()
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            permissions.add(Manifest.permission.READ_PHONE_NUMBERS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    val unlockedTabs by SecurityLockManager.unlockedTabsInSession.collectAsState()
    val isLockGloballyEnabled by SecurityLockManager.isLockEnabled.collectAsState()
    val lockedTabsConfigured by SecurityLockManager.lockedTabs.collectAsState()

    // Back handling to return to recents (home) tab
    BackHandler(enabled = currentTab != MainTab.RECENTS && activeCall.state == CallUiState.IDLE) {
        handleTabSelection(MainTab.RECENTS)
    }

    // Fullscreen in-call view when active or dialing or user tapped heads-up to expand
    val showFullScreenCall = activeCall.state == CallUiState.ACTIVE ||
            activeCall.state == CallUiState.DIALING ||
            activeCall.state == CallUiState.ON_HOLD ||
            activeCall.state == CallUiState.DISCONNECTED ||
            (activeCall.state == CallUiState.RINGING && isIncomingFullScreenRequested)

    if (showFullScreenCall) {
        InCallScreen(
            callInfo = activeCall,
            activeNoteText = activeNoteText,
            strings = strings,
            onNoteTextChanged = { viewModel.updateActiveCallNote(it) },
            onAnswer = { viewModel.answerCall() },
            onReject = { viewModel.rejectCall() },
            onEndCall = { viewModel.endCall() },
            onToggleMute = { viewModel.toggleMute() },
            onToggleSpeaker = { viewModel.toggleSpeaker() },
            onToggleBluetooth = { viewModel.toggleBluetooth() },
            onToggleRecord = {
                viewModel.toggleCallRecording(
                    activeCall.phoneNumber,
                    activeCall.callerName
                )
            },
            onSwitchSim = { viewModel.switchActiveCallSim() },
            onShareContact = { isInCallShareContactOpen = true },
            onQuickSmsDecline = { msg ->
                viewModel.rejectCall()
                viewModel.sendSmsMessage(activeCall.phoneNumber, msg)
            }
        )

        if (isInCallShareContactOpen) {
            com.example.ui.dialogs.InCallShareContactDialog(
                isOpen = isInCallShareContactOpen,
                targetCallerNumber = activeCall.phoneNumber,
                contacts = contacts,
                onSendContact = { contact ->
                    viewModel.sendContactViaSms(activeCall.phoneNumber, contact.name, contact.phoneNumber)
                },
                onDismiss = { isInCallShareContactOpen = false }
            )
        }
    } else {
        Scaffold(
            bottomBar = {
                DialerBottomNavBar(
                    selectedTab = currentTab,
                    strings = strings,
                    unreadSpamCount = unreadSpamCount,
                    onTabSelected = { handleTabSelection(it) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_animation"
                ) { tab ->
                    val isTabLocked = isLockGloballyEnabled &&
                            lockedTabsConfigured.contains(tab.name) &&
                            !unlockedTabs.contains(tab.name)

                    if (isTabLocked) {
                        FeatureLockPlaceholder(
                            tab = tab,
                            onUnlockClicked = { lockedTabPromptTarget = tab }
                        )
                    } else {
                        when (tab) {
                            MainTab.RECENTS -> RecentsScreen(
                                viewModel = viewModel,
                                onOpenDialpad = { handleTabSelection(MainTab.DIALER) }
                            )
                            MainTab.MESSAGES -> MessagesScreen(viewModel = viewModel)
                            MainTab.DIALER -> DialerPadScreen(viewModel = viewModel)
                            MainTab.CONTACTS -> ContactsScreen(viewModel = viewModel)
                            MainTab.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
                            MainTab.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                onNavigateToSecurityRecord = { handleTabSelection(MainTab.SECURITY_RECORD) }
                            )
                            MainTab.SECURITY_RECORD -> SpamAndRecordingsScreen(viewModel = viewModel)
                        }
                    }
                }

                // WhatsApp Fallback Dialog
                if (fallbackCallState.isOpen) {
                    WhatsAppFallbackDialog(
                        phoneNumber = fallbackCallState.phoneNumber,
                        callerName = fallbackCallState.callerName,
                        onFallbackToCellular = { viewModel.triggerCellularFallbackNow() },
                        onDismiss = { viewModel.dismissFallbackDialog() }
                    )
                }

                // Pre-Call SIM Selection Dialog
                if (simSelectionState.isOpen) {
                    SimSelectionDialog(
                        state = simSelectionState,
                        onSelectSim = { sim, alwaysRemember ->
                            viewModel.selectSimAndProceedCall(sim, alwaysRemember)
                        },
                        onDismiss = { viewModel.dismissSimSelection() }
                    )
                }

                // Camera OCR Scanner Dialog
                if (ocrScannerState.isOpen) {
                    OcrScannerDialog(
                        state = ocrScannerState,
                        onProcessBitmap = { bitmap ->
                            viewModel.processOcrBitmap(bitmap)
                        },
                        onCallNumber = { number, name ->
                            viewModel.makeCall(number, name)
                        },
                        onSaveToContacts = { number, name ->
                            viewModel.openSaveToGoogleContacts(number, name)
                        },
                        onLoadToDialpad = { number ->
                            viewModel.setDialpadNumber(number)
                            handleTabSelection(MainTab.DIALER)
                        },
                        onDismiss = { viewModel.closeOcrScanner() }
                    )
                }

                // Heads-Up Incoming Call Overlay Banner on any screen
                if (activeCall.state == CallUiState.RINGING && !isIncomingFullScreenRequested) {
                    IncomingCallHeadsUpBanner(
                        callInfo = activeCall,
                        onAnswer = { viewModel.answerCall() },
                        onReject = { viewModel.rejectCall() },
                        onExpandFullScreen = { isIncomingFullScreenRequested = true },
                        onQuickSmsDecline = { msg ->
                            viewModel.rejectCall()
                            viewModel.sendSmsMessage(activeCall.phoneNumber, msg)
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp)
                    )
                }

                // Floating Smart OTP Overlay Banner
                FloatingOtpBanner(
                    banner = activeOtpBanner,
                    onCopy = { viewModel.copyOtpBannerCode() },
                    onDismiss = { viewModel.dismissOtpBanner() },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = if (activeCall.state == CallUiState.RINGING) 90.dp else 8.dp)
                )

                // Security PIN Dialog for Locked Tab
                lockedTabPromptTarget?.let { targetTab ->
                    SecurityPinDialog(
                        title = "ফিচারটি আনলক করুন",
                        subtitle = "এই ফিচারটিতে প্রবেশ করতে আপনার ৪ সংখ্যার পিন কোড দিন:",
                        onPinEntered = { pin ->
                            SecurityLockManager.verifyPin(context, pin)
                        },
                        onSuccess = {
                            SecurityLockManager.unlockTabForSession(targetTab)
                            if (targetTab == MainTab.SETTINGS || targetTab == MainTab.SECURITY_RECORD) {
                                viewModel.clearUnreadBlockedSpam()
                            }
                            viewModel.selectTab(targetTab)
                            lockedTabPromptTarget = null
                        },
                        onDismiss = {
                            lockedTabPromptTarget = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureLockPlaceholder(
    tab: MainTab,
    onUnlockClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "সুরক্ষিত ফিচার",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Text(
                text = "ফিচারটি লক করা আছে",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "এই ট্যাবের তথ্যগুলো দেখতে আপনার ৪ সংখ্যার পিন কোড দিয়ে আনলক করুন।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onUnlockClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("পিন কোড দিয়ে আনলক করুন")
            }
        }
    }
}
