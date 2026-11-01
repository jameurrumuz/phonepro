package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.AppDatabase
import com.example.data.entity.BlockedNumberEntity
import com.example.data.entity.CallLogEntity
import com.example.data.entity.CallNoteEntity
import com.example.data.entity.SpamRuleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AutoBackupFrequency(val labelBn: String, val labelEn: String, val intervalMillis: Long) {
    OFF("বন্ধ", "Disabled", 0L),
    HOURS_6("প্রতি ৬ ঘণ্টা", "Every 6 Hours", 6 * 3600 * 1000L),
    DAILY("দৈনিক (২৪ ঘণ্টা)", "Daily (24 Hours)", 24 * 3600 * 1000L),
    WEEKLY("সাপ্তাহিক (৭ দিন)", "Weekly (7 Days)", 7 * 24 * 3600 * 1000L)
}

data class BackupStateInfo(
    val lastBackupTimestamp: Long = 0L,
    val frequency: AutoBackupFrequency = AutoBackupFrequency.DAILY,
    val nextScheduledTimestamp: Long = 0L,
    val totalLogsBackedUp: Int = 0,
    val isAutoBackupActive: Boolean = true
)

class BackupManager(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val prefs = context.getSharedPreferences("smart_dialer_backup_prefs", Context.MODE_PRIVATE)

    fun getBackupState(): BackupStateInfo {
        val lastTimestamp = prefs.getLong("last_backup_timestamp", 0L)
        val freqName = prefs.getString("backup_frequency", AutoBackupFrequency.DAILY.name) ?: AutoBackupFrequency.DAILY.name
        val frequency = try { AutoBackupFrequency.valueOf(freqName) } catch (_: Exception) { AutoBackupFrequency.DAILY }

        val nextTimestamp = if (frequency != AutoBackupFrequency.OFF && lastTimestamp > 0L) {
            lastTimestamp + frequency.intervalMillis
        } else if (frequency != AutoBackupFrequency.OFF) {
            System.currentTimeMillis() + frequency.intervalMillis
        } else {
            0L
        }

        return BackupStateInfo(
            lastBackupTimestamp = lastTimestamp,
            frequency = frequency,
            nextScheduledTimestamp = nextTimestamp,
            isAutoBackupActive = (frequency != AutoBackupFrequency.OFF)
        )
    }

    fun setAutoBackupFrequency(frequency: AutoBackupFrequency) {
        prefs.edit()
            .putString("backup_frequency", frequency.name)
            .apply()
    }

    fun getScheduledBackupTime(): String = prefs.getString("scheduled_backup_time", "02:00 AM") ?: "02:00 AM"

    fun setScheduledBackupTime(timeStr: String) {
        prefs.edit().putString("scheduled_backup_time", timeStr).apply()
    }

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "SmartDialer")
        root.put("timestamp", System.currentTimeMillis())

        // Call Logs
        val logs = db.callLogDao().getAllCallLogsSync()
        val logsArray = JSONArray()
        for (log in logs) {
            val obj = JSONObject().apply {
                put("number", log.number)
                put("name", log.name)
                put("type", log.type)
                put("timestamp", log.timestamp)
                put("durationSeconds", log.durationSeconds)
                put("callMode", log.callMode)
                put("isRecorded", log.isRecorded)
            }
            logsArray.put(obj)
        }
        root.put("callLogs", logsArray)

        // Notes
        val notes = db.callNoteDao().getAllNotesSync()
        val notesArray = JSONArray()
        for (note in notes) {
            val obj = JSONObject().apply {
                put("phoneNumber", note.phoneNumber)
                put("callerName", note.callerName)
                put("noteText", note.noteText)
                put("timestamp", note.timestamp)
            }
            notesArray.put(obj)
        }
        root.put("callNotes", notesArray)

        // Blocked Numbers
        val blocked = db.blockedNumberDao().getAllBlockedNumbersSync()
        val blockedArray = JSONArray()
        for (b in blocked) {
            val obj = JSONObject().apply {
                put("phoneNumber", b.phoneNumber)
                put("callerName", b.callerName)
                put("reason", b.reason)
                put("isSpam", b.isSpam)
            }
            blockedArray.put(obj)
        }
        root.put("blockedNumbers", blockedArray)

        // Marked No-WhatsApp list
        val noWa = ContactNoWhatsAppManager.markedNumbers.value
        val noWaArray = JSONArray()
        for (num in noWa) {
            noWaArray.put(num)
        }
        root.put("markedNoWhatsApp", noWaArray)

        root.toString(2)
    }

    suspend fun exportLocalBackup(): File = withContext(Dispatchers.IO) {
        val json = createBackupJson()
        val backupDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val timeString = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(backupDir, "SmartDialer_Backup_$timeString.json")
        file.writeText(json)

        prefs.edit().putLong("last_backup_timestamp", System.currentTimeMillis()).apply()
        file
    }

    suspend fun performGoogleDriveBackupIntent(): Intent = withContext(Dispatchers.IO) {
        val file = exportLocalBackup()
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Smart Dialer Google Drive Backup")
            putExtra(Intent.EXTRA_TEXT, "Smart Dialer Cloud/Drive Backup - ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.google.android.apps.docs") // Target Google Drive if available
        }
        intent
    }

    suspend fun restoreFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            // Restore Call Logs
            if (root.has("callLogs")) {
                val array = root.getJSONArray("callLogs")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val log = CallLogEntity(
                        number = obj.getString("number"),
                        name = obj.optString("name", ""),
                        type = obj.optString("type", "OUTGOING"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        durationSeconds = obj.optInt("durationSeconds", 0),
                        callMode = obj.optString("callMode", "CELLULAR"),
                        isRecorded = obj.optBoolean("isRecorded", false)
                    )
                    db.callLogDao().insert(log)
                }
            }

            // Restore Call Notes
            if (root.has("callNotes")) {
                val array = root.getJSONArray("callNotes")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val note = CallNoteEntity(
                        phoneNumber = obj.getString("phoneNumber"),
                        callerName = obj.optString("callerName", ""),
                        noteText = obj.getString("noteText"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    db.callNoteDao().insert(note)
                }
            }

            // Restore Blocked Numbers
            if (root.has("blockedNumbers")) {
                val array = root.getJSONArray("blockedNumbers")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val blocked = BlockedNumberEntity(
                        phoneNumber = obj.getString("phoneNumber"),
                        callerName = obj.optString("callerName", ""),
                        reason = obj.optString("reason", "Restored"),
                        isSpam = obj.optBoolean("isSpam", true)
                    )
                    db.blockedNumberDao().insert(blocked)
                }
            }

            // Restore Marked No-WhatsApp
            if (root.has("markedNoWhatsApp")) {
                val array = root.getJSONArray("markedNoWhatsApp")
                for (i in 0 until array.length()) {
                    val num = array.getString(i)
                    ContactNoWhatsAppManager.toggleMarkedNoWhatsApp(context, num)
                }
            }

            true
        } catch (e: Exception) {
            android.util.Log.e("BackupManager", "restore error: ${e.message}", e)
            false
        }
    }

    fun checkAndTriggerAutoBackupIfNeeded(): Boolean {
        val state = getBackupState()
        if (state.frequency == AutoBackupFrequency.OFF) return false

        val now = System.currentTimeMillis()
        if (state.lastBackupTimestamp == 0L || (now - state.lastBackupTimestamp) >= state.frequency.intervalMillis) {
            prefs.edit().putLong("last_backup_timestamp", now).apply()
            return true
        }
        return false
    }
}
