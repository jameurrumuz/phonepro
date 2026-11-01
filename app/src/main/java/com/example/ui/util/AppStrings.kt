package com.example.ui.util

enum class AppLanguage {
    BANGLA,
    ENGLISH
}

object AppStrings {

    fun get(language: AppLanguage): Strings = when (language) {
        AppLanguage.BANGLA -> BanglaStrings
        AppLanguage.ENGLISH -> EnglishStrings
    }

    interface Strings {
        val appName: String
        val tabRecents: String
        val tabMessages: String
        val tabDialer: String
        val tabContacts: String
        val tabAnalytics: String
        val tabSettings: String
        val tabSecurityRecord: String

        // Recents
        val callHistory: String
        val searchCallLogs: String
        val all: String
        val missedCalls: String
        val whatsApp: String
        val cellular: String
        val spamBlocked: String
        val noCallLogsFound: String
        val noCallLogsDesc: String
        val saveContact: String
        val longTapToCopy: String
        val numberCopied: String
        val switchSim: String
        val blockNumber: String
        val unblockNumber: String
        val deleteLog: String
        val clearAll: String
        val callDetailsTitle: String
        val totalCallsCount: String
        val totalDurationText: String
        val noWhatsAppTag: String
        val hasWhatsAppTag: String
        val callNow: String
        val sendSms: String

        // In-Call
        val calling: String
        val incomingCall: String
        val onCall: String
        val onHold: String
        val callEnded: String
        val unknownCaller: String
        val hiddenNumber: String
        val mute: String
        val unmute: String
        val speaker: String
        val bluetooth: String
        val bluetoothAudioActive: String
        val record: String
        val stopRecord: String
        val recordingActive: String
        val callNotes: String
        val noteSavedAuto: String
        val noteCollapse: String
        val noteExpand: String
        val notePlaceholder: String
        val copyNote: String
        val answer: String
        val decline: String
        val switchSimInCall: String
        val notePad: String

        // Messages
        val messagesTitle: String
        val searchMessages: String
        val newChat: String
        val typeMessagePlaceholder: String
        val send: String
        val defaultSmsBannerTitle: String
        val defaultSmsBannerDesc: String
        val setAsDefaultSms: String
        val noMessagesFound: String
        val noMessagesDesc: String
        val selectRecipient: String
        val enterPhoneNumber: String
        val suggestedContacts: String
        val allContacts: String
        val selectSimForSms: String

        // Analytics
        val analyticsTitle: String
        val analyticsSubtitle: String
        val callSummary: String
        val daily: String
        val monthly: String
        val byDate: String
        val selectDate: String
        val outgoingCalls: String
        val incomingCalls: String
        val missed: String
        val spamProtected: String
        val outVsInRatio: String
        val dataUsageInfo: String
        val dataUsageDesc: String
        val todayMobileData: String
        val monthlyMobileData: String
        val todayWifi: String
        val monthlyWifi: String
        val topContacts: String
        val timesSuffix: String
        val minutesSuffix: String
        val unanswered: String
        val blockedCalls: String

        // Settings
        val settingsTitle: String
        val languageSection: String
        val appLanguage: String
        val themeSection: String
        val themeMode: String
        val themeLight: String
        val themeDark: String
        val themeSystem: String
        val backupSection: String
        val localBackupTitle: String
        val localBackupDesc: String
        val createLocalBackup: String
        val restoreLocalBackup: String
        val googleDriveSection: String
        val autoBackupFrequency: String
        val scheduledBackupTime: String
        val backupNowGoogleDrive: String
        val restoreGoogleDrive: String
        val lastBackup: String
        val nextBackup: String
        val defaultAppsSection: String
        val defaultPhoneApp: String
        val defaultSmsApp: String
        val isDefault: String
        val setAsDefault: String
        val spamAndRecordingsTitle: String
        val spamAndRecordingsDesc: String
        val missedCallReminderSection: String
        val missedCallReminderFrequency: String
        val missedCallReminderDesc: String
        val cancelOrDismiss: String
    }

    private object BanglaStrings : Strings {
        override val appName = "Call Pro"
        override val tabRecents = "রিসেন্ট"
        override val tabMessages = "মেসেজ"
        override val tabDialer = "ডায়ালার"
        override val tabContacts = "পরিচিতি"
        override val tabAnalytics = "অ্যানালিটিক্স"
        override val tabSettings = "সেটিংস"
        override val tabSecurityRecord = "রেকর্ড ও স্প্যাম"

        override val callHistory = "কল হিস্টোরি"
        override val searchCallLogs = "কল খুঁজুন..."
        override val all = "সকল"
        override val missedCalls = "মিসড কল"
        override val whatsApp = "হোয়াটসঅ্যাপ"
        override val cellular = "সাধারণ কল"
        override val spamBlocked = "ব্লকড স্প্যাম"
        override val noCallLogsFound = "কোনো কল রেকর্ড নেই"
        override val noCallLogsDesc = "নতুন কল করলে তা এখানে সংরক্ষিত হবে"
        override val saveContact = "+ সেভ"
        override val longTapToCopy = "নম্বর কপি করতে লং-ট্যাপ করুন"
        override val numberCopied = "নম্বর ক্লিপবোর্ডে কপি করা হয়েছে"
        override val switchSim = "অন্য সিম"
        override val blockNumber = "ব্লক করুন"
        override val unblockNumber = "আনব্লক করুন"
        override val deleteLog = "মুছে ফেলুন"
        override val clearAll = "সব মুছুন"
        override val callDetailsTitle = "কল বিবরণ ও ইতিহাস"
        override val totalCallsCount = "মোট কল সংখ্যা"
        override val totalDurationText = "মোট কথা বলার সময়"
        override val noWhatsAppTag = "📵 No WA"
        override val hasWhatsAppTag = "WA আছে"
        override val callNow = "কল করুন"
        override val sendSms = "এসএমএস"

        override val calling = "কল যাচ্ছে..."
        override val incomingCall = "ইনকামিং কল..."
        override val onCall = "কথা বলছেন"
        override val onHold = "হোল্ডে আছে"
        override val callEnded = "কল সমাপ্ত"
        override val unknownCaller = "অপরিচিত কলার"
        override val hiddenNumber = "নম্বর গোপন"
        override val mute = "মিউট"
        override val unmute = "আনমিউট"
        override val speaker = "স্পিকার"
        override val bluetooth = "ব্লুটুথ"
        override val bluetoothAudioActive = "ব্লুটুথ অডিও সক্রিয়"
        override val record = "রেকর্ড"
        override val stopRecord = "রেকর্ড বন্ধ"
        override val recordingActive = "কল রেকর্ড হচ্ছে"
        override val callNotes = "কল নোট ও খসড়া (অটো-সেভ)"
        override val noteSavedAuto = "✅ কল কেটে গেলেও ডাটাবেজে থাকবে"
        override val noteCollapse = "সংকোচন"
        override val noteExpand = "লিখুন"
        override val notePlaceholder = "কথা বলা অবস্থায় এখানে জরুরি নম্বর বা তথ্য লিখুন... (স্বয়ংক্রিয়ভাবে সংরক্ষিত হবে)"
        override val copyNote = "নোট কপি"
        override val answer = "উত্তর দিন"
        override val decline = "প্রত্যাখ্যান"
        override val switchSimInCall = "সিম পরিবর্তন"
        override val notePad = "নোট প্যাড"

        override val messagesTitle = "এসএমএস ও বার্তা"
        override val searchMessages = "মেসেজ খুঁজুন..."
        override val newChat = "নতুন বার্তা"
        override val typeMessagePlaceholder = "বার্তা লিখুন..."
        override val send = "পাঠান"
        override val defaultSmsBannerTitle = "ডিফল্ট এসএমএস অ্যাপ হিসেবে সেট করুন"
        override val defaultSmsBannerDesc = "বার্তা পাঠানো এবং রিসিভ করার জন্য এটি ডিফল্ট রাখুন"
        override val setAsDefaultSms = "ডিফল্ট করুন"
        override val noMessagesFound = "কোনো বার্তা পাওয়া যায়নি"
        override val noMessagesDesc = "নতুন বার্তা এলে বা পাঠালে তা এখানে প্রদর্শিত হবে"
        override val selectRecipient = "প্রাপক নির্বাচন করুন"
        override val enterPhoneNumber = "মোবাইল নম্বর লিখুন বা কন্ট্যাক্ট খুঁজুন..."
        override val suggestedContacts = "সাজেস্টেড কন্ট্যাক্ট"
        override val allContacts = "সকল কন্ট্যাক্ট"
        override val selectSimForSms = "সিম নির্বাচন:"

        override val analyticsTitle = "কল ও ডাটা অ্যানালিটিক্স"
        override val analyticsSubtitle = "দৈনিক, মাসিক ও তারিখ অনুযায়ী অথেনটিক রিপোর্ট"
        override val callSummary = "কল সামারি"
        override val daily = "দৈনিক"
        override val monthly = "মাসিক"
        override val byDate = "তারিখ অনুযায়ী"
        override val selectDate = "তারিখ নির্বাচন করুন"
        override val outgoingCalls = "আউটগোয়িং কল"
        override val incomingCalls = "ইনকামিং কল"
        override val missed = "মিসড কল"
        override val spamProtected = "স্প্যাম প্রতিরোধ"
        override val outVsInRatio = "আউটগোয়িং বনাম ইনকামিং অনুপাত"
        override val dataUsageInfo = "ডাটা ব্যবহারের ইনফরমেশন"
        override val dataUsageDesc = "মোবাইল নেটওয়ার্ক ও ওয়াই-ফাই ডাটা ট্র্যাকার"
        override val todayMobileData = "আজকের মোবাইল ডাটা"
        override val monthlyMobileData = "চলতি মাসের মোবাইল ডাটা"
        override val todayWifi = "আজকের Wi-Fi"
        override val monthlyWifi = "মাসিক Wi-Fi"
        override val topContacts = "সর্বাধিক কথা বলা পরিচিতি"
        override val timesSuffix = "বার"
        override val minutesSuffix = "মিনিট"
        override val unanswered = "উত্তরহীন"
        override val blockedCalls = "ব্লককৃত কল"

        override val settingsTitle = "অ্যাপ সেটিংস"
        override val languageSection = "ভাষা পরিবর্তন (Language)"
        override val appLanguage = "অ্যাপের ভাষা"
        override val themeSection = "ডিসপ্লে ও থিম"
        override val themeMode = "থিম মোড"
        override val themeLight = "লাইট থিম"
        override val themeDark = "ডার্ক থিম"
        override val themeSystem = "সিস্টেম ডিফল্ট"
        override val backupSection = "ডাটা ব্যাকআপ ও রিস্টোর"
        override val localBackupTitle = "লোকাল স্টোরেজ ব্যাকআপ"
        override val localBackupDesc = "সকল কল লগ, নোট ও সেটিংস ডিভাইসে সেভ বা শেয়ার করুন"
        override val createLocalBackup = "লোকাল ব্যাকআপ নিন"
        override val restoreLocalBackup = "লোকাল ব্যাকআপ রিস্টোর"
        override val googleDriveSection = "গুগল ড্রাইভ অটো ব্যাকআপ"
        override val autoBackupFrequency = "স্বয়ংক্রিয় ব্যাকআপের ফ্রিকোয়েন্সি"
        override val scheduledBackupTime = "ব্যাকআপের নির্দিষ্ট সময়"
        override val backupNowGoogleDrive = "এখনই ব্যাকআপ নিন (Backup Now)"
        override val restoreGoogleDrive = "গুগল ড্রাইভ থেকে রিস্টোর"
        override val lastBackup = "সর্বশেষ ব্যাকআপ"
        override val nextBackup = "পরবর্তী ব্যাকআপ"
        override val defaultAppsSection = "ডিফল্ট অ্যাপ্লিকেশন"
        override val defaultPhoneApp = "ডিফল্ট ফোন ও ডায়ালার"
        override val defaultSmsApp = "ডিফল্ট মেসেজিং অ্যাপ"
        override val isDefault = "সক্রিয় রয়েছে"
        override val setAsDefault = "ডিফল্ট সেট করুন"
        override val spamAndRecordingsTitle = "স্প্যাম প্রতিরোধ ও কল রেকর্ড"
        override val spamAndRecordingsDesc = "ব্লকড নম্বর, স্প্যাম ফিল্টার ও রেকর্ডকৃত কল পরিচালনা করুন"
        override val missedCallReminderSection = "মিসড কল রিমাইন্ডার নোটিফিকেশন"
        override val missedCallReminderFrequency = "পুনরায় নোটিফিকেশন দেওয়ার ব্যবধান"
        override val missedCallReminderDesc = "মিসড কলে কল ব্যাক না করা পর্যন্ত নির্দিষ্ট সময় পর পর নোটিফিকেশন দিয়ে মনে করিয়ে দেবে"
        override val cancelOrDismiss = "বাতিল / স্কিপ"
    }

    private object EnglishStrings : Strings {
        override val appName = "Call Pro"
        override val tabRecents = "Recents"
        override val tabMessages = "Messages"
        override val tabDialer = "Dialer"
        override val tabContacts = "Contacts"
        override val tabAnalytics = "Analytics"
        override val tabSettings = "Settings"
        override val tabSecurityRecord = "Spam & Records"

        override val callHistory = "Call History"
        override val searchCallLogs = "Search calls..."
        override val all = "All"
        override val missedCalls = "Missed"
        override val whatsApp = "WhatsApp"
        override val cellular = "Cellular"
        override val spamBlocked = "Blocked Spam"
        override val noCallLogsFound = "No Call Logs Found"
        override val noCallLogsDesc = "New calls will automatically be saved here"
        override val saveContact = "+ Save"
        override val longTapToCopy = "Long-tap to copy number"
        override val numberCopied = "Number copied to clipboard"
        override val switchSim = "Switch SIM"
        override val blockNumber = "Block"
        override val unblockNumber = "Unblock"
        override val deleteLog = "Delete"
        override val clearAll = "Clear All"
        override val callDetailsTitle = "Call Details & History"
        override val totalCallsCount = "Total Calls"
        override val totalDurationText = "Total Talk Time"
        override val noWhatsAppTag = "📵 No WA"
        override val hasWhatsAppTag = "Has WA"
        override val callNow = "Call"
        override val sendSms = "SMS"

        override val calling = "Calling..."
        override val incomingCall = "Incoming Call..."
        override val onCall = "On Call"
        override val onHold = "On Hold"
        override val callEnded = "Call Ended"
        override val unknownCaller = "Unknown Caller"
        override val hiddenNumber = "Hidden Number"
        override val mute = "Mute"
        override val unmute = "Unmute"
        override val speaker = "Speaker"
        override val bluetooth = "Bluetooth"
        override val bluetoothAudioActive = "Bluetooth Audio Active"
        override val record = "Record"
        override val stopRecord = "Stop Record"
        override val recordingActive = "Call Recording Active"
        override val callNotes = "Call Note & Scratchpad (Auto-Save)"
        override val noteSavedAuto = "✅ Saved to database permanently"
        override val noteCollapse = "Collapse"
        override val noteExpand = "Write"
        override val notePlaceholder = "Write important notes or numbers here during call... (Auto-saved)"
        override val copyNote = "Copy Note"
        override val answer = "Answer"
        override val decline = "Decline"
        override val switchSimInCall = "Switch SIM"
        override val notePad = "Notepad"

        override val messagesTitle = "Messages & SMS"
        override val searchMessages = "Search messages..."
        override val newChat = "New Message"
        override val typeMessagePlaceholder = "Type a message..."
        override val send = "Send"
        override val defaultSmsBannerTitle = "Set as Default SMS App"
        override val defaultSmsBannerDesc = "Allow sending and receiving text messages seamlessly"
        override val setAsDefaultSms = "Set Default"
        override val noMessagesFound = "No Messages Found"
        override val noMessagesDesc = "Incoming and sent messages will appear here"
        override val selectRecipient = "Select Recipient"
        override val enterPhoneNumber = "Enter phone number or search contacts..."
        override val suggestedContacts = "Suggested Contacts"
        override val allContacts = "All Contacts"
        override val selectSimForSms = "Select SIM:"

        override val analyticsTitle = "Call & Data Analytics"
        override val analyticsSubtitle = "Daily, monthly, and date-wise authentic reports"
        override val callSummary = "Call Summary"
        override val daily = "Daily"
        override val monthly = "Monthly"
        override val byDate = "By Date"
        override val selectDate = "Select Date"
        override val outgoingCalls = "Outgoing Calls"
        override val incomingCalls = "Incoming Calls"
        override val missed = "Missed Calls"
        override val spamProtected = "Spam Blocked"
        override val outVsInRatio = "Outgoing vs Incoming Ratio"
        override val dataUsageInfo = "Data Usage Information"
        override val dataUsageDesc = "Mobile network & Wi-Fi traffic monitor"
        override val todayMobileData = "Today's Mobile Data"
        override val monthlyMobileData = "Monthly Mobile Data"
        override val todayWifi = "Today's Wi-Fi"
        override val monthlyWifi = "Monthly Wi-Fi"
        override val topContacts = "Most Contacted Persons"
        override val timesSuffix = "times"
        override val minutesSuffix = "min"
        override val unanswered = "Unanswered"
        override val blockedCalls = "Blocked Calls"

        override val settingsTitle = "App Settings"
        override val languageSection = "Language"
        override val appLanguage = "App Language"
        override val themeSection = "Display & Theme"
        override val themeMode = "Theme Mode"
        override val themeLight = "Light Theme"
        override val themeDark = "Dark Theme"
        override val themeSystem = "System Default"
        override val backupSection = "Backup & Restore"
        override val localBackupTitle = "Local Storage Backup"
        override val localBackupDesc = "Export call logs, notes, and settings to device storage"
        override val createLocalBackup = "Create Local Backup"
        override val restoreLocalBackup = "Restore from Local File"
        override val googleDriveSection = "Google Drive Auto-Backup"
        override val autoBackupFrequency = "Auto-Backup Frequency"
        override val scheduledBackupTime = "Scheduled Backup Time"
        override val backupNowGoogleDrive = "Backup Now"
        override val restoreGoogleDrive = "Restore from Google Drive"
        override val lastBackup = "Last Backup"
        override val nextBackup = "Next Scheduled Backup"
        override val defaultAppsSection = "Default Applications"
        override val defaultPhoneApp = "Default Phone & Dialer"
        override val defaultSmsApp = "Default Messaging App"
        override val isDefault = "Active Default"
        override val setAsDefault = "Set as Default"
        override val spamAndRecordingsTitle = "Spam Protection & Recordings"
        override val spamAndRecordingsDesc = "Manage blocked numbers, spam rules, and recorded calls"
        override val missedCallReminderSection = "Missed Call Reminder Notifications"
        override val missedCallReminderFrequency = "Reminder Repeat Interval"
        override val missedCallReminderDesc = "Periodically reminds you of unreturned missed calls until you call back or skip"
        override val cancelOrDismiss = "Cancel / Skip"
    }
}
