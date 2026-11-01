package com.example.data.repository

import android.Manifest
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.example.data.model.SmsConversation
import com.example.data.model.SmsMessage
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsRepository(private val context: Context) {

    private val _conversations = MutableStateFlow<List<SmsConversation>>(emptyList())
    val conversations: StateFlow<List<SmsConversation>> = _conversations.asStateFlow()

    private val _messagesForThread = MutableStateFlow<List<SmsMessage>>(emptyList())
    val messagesForThread: StateFlow<List<SmsMessage>> = _messagesForThread.asStateFlow()

    private val _activeThreadId = MutableStateFlow<Long?>(null)
    val activeThreadId: StateFlow<Long?> = _activeThreadId.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var isObserverRegistered = false
    private var debounceJob: Job? = null

    private val smsContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(300)
                loadConversations()
                val threadId = _activeThreadId.value
                if (threadId != null && threadId != -1L) {
                    loadMessagesForThread(threadId)
                }
            }
        }
    }

    fun hasSmsPermissions(): Boolean {
        val readSms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val sendSms = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        return readSms && sendSms
    }

    fun ensureObserverRegistered() {
        if (!hasSmsPermissions() || isObserverRegistered) return
        try {
            context.contentResolver.registerContentObserver(
                Uri.parse("content://sms"),
                true,
                smsContentObserver
            )
            isObserverRegistered = true
        } catch (e: Exception) {
            android.util.Log.w("SmsRepository", "Could not register SMS ContentObserver: ${e.message}")
        }
    }

    suspend fun loadConversations() = withContext(Dispatchers.IO) {
        if (!hasSmsPermissions()) {
            return@withContext
        }
        ensureObserverRegistered()

        val list = mutableListOf<SmsConversation>()
        val uri = Uri.parse("content://sms")
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT 250"
            )

            cursor?.use {
                val threadIdIdx = it.getColumnIndex(Telephony.Sms.THREAD_ID)
                val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                val readIdx = it.getColumnIndex(Telephony.Sms.READ)

                val threadMap = mutableMapOf<Long, SmsConversation>()
                val contactNameCache = mutableMapOf<String, String>()

                while (it.moveToNext()) {
                    val threadId = it.getLong(threadIdIdx)
                    val address = it.getString(addressIdx) ?: ""
                    val body = it.getString(bodyIdx) ?: ""
                    val date = it.getLong(dateIdx)
                    val isRead = it.getInt(readIdx) != 0

                    if (!threadMap.containsKey(threadId)) {
                        val contactName = contactNameCache.getOrPut(address) {
                            ContactLookupHelper.resolveContactName(context, address) ?: ""
                        }
                        threadMap[threadId] = SmsConversation(
                            threadId = threadId,
                            address = address,
                            contactName = contactName,
                            snippet = body,
                            lastTimestamp = date,
                            unreadCount = if (!isRead) 1 else 0,
                            messageCount = 1
                        )
                    } else {
                        val existing = threadMap[threadId]!!
                        threadMap[threadId] = existing.copy(
                            unreadCount = existing.unreadCount + (if (!isRead) 1 else 0),
                            messageCount = existing.messageCount + 1
                        )
                    }
                }

                list.addAll(threadMap.values.sortedByDescending { it.lastTimestamp })
            }
            _conversations.value = list
        } catch (e: Exception) {
            android.util.Log.w("SmsRepository", "Error loading conversations: ${e.message}")
        }
    }

    /**
     * Executes atomic two-phase ContentResolver update (read = 1 and seen = 1)
     * and clears launcher app icon badge count immediately.
     */
    suspend fun markThreadMessagesAsRead(threadId: Long) = withContext(Dispatchers.IO) {
        if (!hasSmsPermissions()) return@withContext
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
            }

            context.contentResolver.update(
                Telephony.Sms.Inbox.CONTENT_URI,
                values,
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                arrayOf(threadId.toString())
            )

            try {
                context.contentResolver.update(
                    Uri.parse("content://sms"),
                    values,
                    "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                    arrayOf(threadId.toString())
                )
            } catch (_: Exception) {}

            // Clear status bar notification and launcher icon badge immediately
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel("SMS_TAG", threadId.toInt())

            // Update thread list state
            val currentList = _conversations.value.map { conv ->
                if (conv.threadId == threadId) conv.copy(unreadCount = 0) else conv
            }
            _conversations.value = currentList
        } catch (e: Exception) {
            android.util.Log.w("SmsRepository", "Failed to mark thread messages as read: ${e.message}")
        }
    }

    suspend fun loadMessagesForThread(threadId: Long) = withContext(Dispatchers.IO) {
        _activeThreadId.value = threadId
        markThreadMessagesAsRead(threadId)
        val list = mutableListOf<SmsMessage>()
        val uri = Uri.parse("content://sms")
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} DESC LIMIT 150"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(Telephony.Sms._ID)
                val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)
                val readIdx = it.getColumnIndex(Telephony.Sms.READ)

                val contactNameCache = mutableMapOf<String, String>()

                while (it.moveToNext()) {
                    val id = it.getLong(idIdx)
                    val address = it.getString(addressIdx) ?: ""
                    val body = it.getString(bodyIdx) ?: ""
                    val date = it.getLong(dateIdx)
                    val type = it.getInt(typeIdx)
                    val isRead = it.getInt(readIdx) != 0

                    val contactName = contactNameCache.getOrPut(address) {
                        ContactLookupHelper.resolveContactName(context, address) ?: ""
                    }

                    list.add(
                        SmsMessage(
                            id = id,
                            threadId = threadId,
                            address = address,
                            contactName = contactName,
                            body = body,
                            timestamp = date,
                            isIncoming = (type == Telephony.Sms.MESSAGE_TYPE_INBOX),
                            isRead = isRead
                        )
                    )
                }
            }
            _messagesForThread.value = list.reversed()
        } catch (e: Exception) {
            android.util.Log.w("SmsRepository", "Error loading messages for thread: ${e.message}")
        }
    }

    suspend fun loadMessagesForAddress(address: String) = withContext(Dispatchers.IO) {
        val clean = address.trim()
        val existingThread = _conversations.value.find {
            it.address.replace("[^0-9]".toRegex(), "") == clean.replace("[^0-9]".toRegex(), "")
        }
        if (existingThread != null) {
            loadMessagesForThread(existingThread.threadId)
        } else {
            _activeThreadId.value = -1L
            _messagesForThread.value = emptyList()
        }
    }

    suspend fun sendSms(
        destinationNumber: String,
        messageText: String,
        simSlot: Int? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (destinationNumber.isBlank() || messageText.isBlank()) return@withContext false

        try {
            val smsManager: SmsManager = if (simSlot != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val activeInfo = subManager?.activeSubscriptionInfoList?.find { it.simSlotIndex == simSlot }
                if (activeInfo != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.getSystemService(SmsManager::class.java).createForSubscriptionId(activeInfo.subscriptionId)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsManager.getSmsManagerForSubscriptionId(activeInfo.subscriptionId)
                    }
                } else {
                    getDefaultSmsManager()
                }
            } else {
                getDefaultSmsManager()
            }

            // Divide message if length > 160 characters
            val parts = smsManager.divideMessage(messageText)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(destinationNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(destinationNumber, null, messageText, null, null)
            }

            // Record sent message in Content Provider
            insertSentMessageIntoProvider(destinationNumber, messageText)

            // Refresh UI
            loadConversations()
            val threadId = _activeThreadId.value
            if (threadId != null && threadId != -1L) {
                loadMessagesForThread(threadId)
            } else {
                loadMessagesForAddress(destinationNumber)
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("SmsRepository", "sendSms error: ${e.message}", e)
            false
        }
    }

    private fun getDefaultSmsManager(): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }

    private fun insertSentMessageIntoProvider(address: String, body: String) {
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            }
            context.contentResolver.insert(Uri.parse("content://sms/sent"), values)
        } catch (e: Exception) {
            android.util.Log.w("SmsRepository", "Could not insert into sent box: ${e.message}")
        }
    }

    fun clearActiveThread() {
        _activeThreadId.value = null
        _messagesForThread.value = emptyList()
    }
}
