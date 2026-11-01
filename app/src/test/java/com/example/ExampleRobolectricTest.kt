package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.analytics.CallAnalyticsHelper
import com.example.data.entity.CallLogEntity
import com.example.data.repository.AutoBackupFrequency
import com.example.data.repository.BackupManager
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppStrings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Call Pro", appName)
    }

    @Test
    fun `verify bilingual strings for Bangla and English`() {
        val bnStrings = AppStrings.get(AppLanguage.BANGLA)
        val enStrings = AppStrings.get(AppLanguage.ENGLISH)

        assertEquals("রিসেন্ট", bnStrings.tabRecents)
        assertEquals("Recents", enStrings.tabRecents)

        assertEquals("মেসেজ", bnStrings.tabMessages)
        assertEquals("Messages", enStrings.tabMessages)

        assertEquals("সেটিংস", bnStrings.tabSettings)
        assertEquals("Settings", enStrings.tabSettings)
    }

    @Test
    fun `verify date-wise analytics calculation`() {
        val cal = Calendar.getInstance()
        val todayStart = CallAnalyticsHelper.getStartOfDayTimestamp(cal)
        val todayEnd = CallAnalyticsHelper.getEndOfDayTimestamp(cal)

        val logs = listOf(
            CallLogEntity(
                number = "01700000001",
                name = "Rahim",
                type = "OUTGOING",
                timestamp = todayStart + 1000,
                durationSeconds = 120,
                callMode = "CELLULAR"
            ),
            CallLogEntity(
                number = "01700000002",
                name = "Karim",
                type = "INCOMING",
                timestamp = todayStart + 5000,
                durationSeconds = 60,
                callMode = "WHATSAPP"
            ),
            CallLogEntity(
                number = "01700000003",
                name = "Spammer",
                type = "BLOCKED_SPAM",
                timestamp = todayStart + 8000,
                durationSeconds = 0,
                callMode = "CELLULAR"
            ),
            // Old call yesterday
            CallLogEntity(
                number = "01700000004",
                name = "Yesterday Friend",
                type = "OUTGOING",
                timestamp = todayStart - 50000,
                durationSeconds = 300,
                callMode = "CELLULAR"
            )
        )

        val todayStats = CallAnalyticsHelper.calculateStats(logs, todayStart, todayEnd)
        assertEquals(1, todayStats.outgoingCount)
        assertEquals(2, todayStats.outgoingMinutes)
        assertEquals(1, todayStats.incomingCount)
        assertEquals(1, todayStats.incomingMinutes)
        assertEquals(1, todayStats.blockedSpamCount)
        assertEquals(3, todayStats.totalMinutes)
    }

    @Test
    fun `verify backup manager json export and state`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val backupManager = BackupManager(context)

        backupManager.setAutoBackupFrequency(AutoBackupFrequency.HOURS_6)
        val state = backupManager.getBackupState()
        assertEquals(AutoBackupFrequency.HOURS_6, state.frequency)
        assertTrue(state.isAutoBackupActive)

        val json = backupManager.createBackupJson()
        assertNotNull(json)
        assertTrue(json.contains("SmartDialer"))
        assertTrue(json.contains("callLogs"))
    }

    @Test
    fun `verify Bengali and handwritten OCR number extraction`() {
        // Test Bengali numeral conversion
        val bnText = "জরুরি নাম্বার: ০১৭১১-২২৩৩৪৪ (অফিস)"
        val result = com.example.data.repository.ContactOcrScanner.extractContactsFromText(bnText)
        assertTrue("Expected to extract number from Bengali digits", result.phoneNumbers.contains("01711223344"))

        // Test spaced handwritten format
        val handwritten = "0 1 7 1 2 3 4 5 6 7 8"
        val hwResult = com.example.data.repository.ContactOcrScanner.extractContactsFromText(handwritten)
        assertTrue("Expected to extract spaced handwritten number", hwResult.phoneNumbers.contains("01712345678"))
    }

    @Test
    fun `verify SecurityLockManager PIN protection and selective tab locks`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.security.SecurityLockManager.init(context)
        com.example.security.SecurityLockManager.setPin(context, "9876")
        assertTrue(com.example.security.SecurityLockManager.verifyPin(context, "9876"))
        org.junit.Assert.assertFalse(com.example.security.SecurityLockManager.verifyPin(context, "0000"))

        // Enable feature lock for Call History (Recents)
        com.example.security.SecurityLockManager.setLockEnabled(context, true)
        com.example.security.SecurityLockManager.setTabLock(context, com.example.ui.viewmodel.MainTab.RECENTS, true)
        assertTrue(com.example.security.SecurityLockManager.isFeatureLocked(context, com.example.ui.viewmodel.MainTab.RECENTS))

        // Unlock for session
        com.example.security.SecurityLockManager.unlockTabForSession(com.example.ui.viewmodel.MainTab.RECENTS)
        org.junit.Assert.assertFalse(com.example.security.SecurityLockManager.isFeatureLocked(context, com.example.ui.viewmodel.MainTab.RECENTS))
    }

    @Test
    fun `verify SpeedDialManager key assignment`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.telecom.SpeedDialManager.init(context)
        com.example.telecom.SpeedDialManager.setSpeedDial(context, '2', "01812345678", "রহিম")

        val entry = com.example.telecom.SpeedDialManager.getSpeedDial('2')
        assertNotNull(entry)
        assertEquals("01812345678", entry?.phoneNumber)
        assertEquals("রহিম", entry?.contactName)
    }
}
