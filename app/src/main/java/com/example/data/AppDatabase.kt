package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BlockedNumberDao
import com.example.data.dao.CallLogDao
import com.example.data.dao.CallNoteDao
import com.example.data.dao.CallRecordingDao
import com.example.data.dao.SpamRuleDao
import com.example.data.entity.BlockedNumberEntity
import com.example.data.entity.CallLogEntity
import com.example.data.entity.CallNoteEntity
import com.example.data.entity.CallRecordingEntity
import com.example.data.entity.SpamRuleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CallLogEntity::class,
        BlockedNumberEntity::class,
        CallRecordingEntity::class,
        SpamRuleEntity::class,
        CallNoteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callLogDao(): CallLogDao
    abstract fun blockedNumberDao(): BlockedNumberDao
    abstract fun callRecordingDao(): CallRecordingDao
    abstract fun spamRuleDao(): SpamRuleDao
    abstract fun callNoteDao(): CallNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_dialer_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial spam database and realistic initial call logs
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            val spamDao = database.spamRuleDao()
            val blockedDao = database.blockedNumberDao()
            val callLogDao = database.callLogDao()
            val recordingDao = database.callRecordingDao()

            // Pre-seed known spam patterns and prefixes
            val initialSpamRules = listOf(
                SpamRuleEntity(pattern = "+1800", ruleType = "PREFIX", description = "আন্তর্জাতিক টোল-ফ্রি রোবোকল"),
                SpamRuleEntity(pattern = "+8801900", ruleType = "PREFIX", description = "সন্দেহভাজন ভুয়া প্রমোশনাল এসএমএস/কল"),
                SpamRuleEntity(pattern = "+92", ruleType = "PREFIX", description = "অপরিচিত আন্তর্জাতিক ফ্ল্যাশ কল স্ক্যাম"),
                SpamRuleEntity(pattern = "লটারি বিজয়ী স্ক্যাম", ruleType = "KEYWORD", description = "প্রতারণামূলক লটারি বা পুরস্কারের কল"),
                SpamRuleEntity(pattern = "অননুমোদিত লোন", ruleType = "KEYWORD", description = "উচ্চ সুদের অবৈধ লোন অ্যাপ রিকভারি")
            )
            spamDao.insertAll(initialSpamRules)

            // Pre-seed blocked numbers
            val initialBlocked = listOf(
                BlockedNumberEntity(
                    phoneNumber = "+8801700999888",
                    callerName = "টেলিమార్কেটিং অফার",
                    reason = "অবাঞ্ছিত ক্রেডিট কার্ড অফার ও স্প্যাম",
                    blockCount = 5,
                    isSpam = true
                ),
                BlockedNumberEntity(
                    phoneNumber = "+8801999112233",
                    callerName = "সন্দেহভাজন লটারি প্রতারক",
                    reason = "লটারি জেতার দাবি করে টাকা চাওয়ার ভুয়া কল",
                    blockCount = 12,
                    isSpam = true
                )
            )
            blockedDao.insertAll(initialBlocked)

            // No demo call logs or demo recordings are seeded.
            // Genuine call history is read dynamically from the phone's system call log!
        }
    }
}
