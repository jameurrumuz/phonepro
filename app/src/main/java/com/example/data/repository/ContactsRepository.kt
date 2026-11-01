package com.example.data.repository

import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.ContactItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ContactsRepository manages querying, observing, and modifying contacts
 * directly through the Android Contacts Provider (including Google Accounts,
 * SIM cards, and on-device storage).
 *
 * It provides:
 * 1. Thread-safe dynamic ContentObserver registration (Phone & Contacts URIs).
 * 2. Automatic reloading when Google Contacts syncs or contacts change.
 * 3. Proper handling of permission grants and revocations.
 * 4. Zero fake/demo fallback contacts: only real Contacts Provider data is exposed.
 */
class ContactsRepository(private val context: Context) {

    companion object {
        private const val TAG = "ContactsRepository"
    }

    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts: StateFlow<List<ContactItem>> = _contacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasPermissionFlow = MutableStateFlow(hasContactsPermission())
    val hasPermissionFlow: StateFlow<Boolean> = _hasPermissionFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val observerLock = Any()
    private val queryMutex = kotlinx.coroutines.sync.Mutex()
    private var isObserverRegistered = false
    private var reloadJob: Job? = null

    private val contactsContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            Log.d(TAG, "Contacts ContentObserver onChange detected (selfChange: $selfChange)")
            if (hasContactsPermission()) {
                scheduleDebouncedReload()
            } else {
                handlePermissionRevocation()
            }
        }
    }

    init {
        scope.launch {
            if (hasContactsPermission()) {
                ensureObserverRegistered()
                loadContacts()
            } else {
                _contacts.value = emptyList()
            }
        }
    }

    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasWriteContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Updates the permission state flow and unregisters observers if permission was revoked.
     * Returns the updated permission state.
     */
    fun updatePermissionStatus(): Boolean {
        val granted = hasContactsPermission()
        _hasPermissionFlow.value = granted
        if (!granted) {
            handlePermissionRevocation()
        }
        return granted
    }

    /**
     * Ensures ContentObserver is dynamically registered on ContactsProvider.
     * Guaranteed to attach whenever READ_CONTACTS permission is granted.
     * Prevents duplicate observer registration using synchronization.
     */
    fun ensureObserverRegistered() {
        synchronized(observerLock) {
            if (!hasContactsPermission()) {
                unregisterObserverLocked()
                return
            }
            if (!isObserverRegistered) {
                try {
                    context.contentResolver.registerContentObserver(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        true,
                        contactsContentObserver
                    )
                    context.contentResolver.registerContentObserver(
                        ContactsContract.Contacts.CONTENT_URI,
                        true,
                        contactsContentObserver
                    )
                    isObserverRegistered = true
                    Log.d(TAG, "Contacts ContentObserver registered successfully for Phone and Contacts URIs")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to register ContentObserver: ${e.message}")
                }
            }
        }
    }

    /**
     * Unregisters the ContentObserver when permissions are revoked or teardown occurs.
     */
    fun unregisterObserver() {
        synchronized(observerLock) {
            unregisterObserverLocked()
        }
    }

    private fun unregisterObserverLocked() {
        if (isObserverRegistered) {
            try {
                context.contentResolver.unregisterContentObserver(contactsContentObserver)
                Log.d(TAG, "Contacts ContentObserver unregistered successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering ContentObserver: ${e.message}")
            } finally {
                isObserverRegistered = false
            }
        }
    }

    /**
     * Handles permission revocation by tearing down observers and clearing in-memory contacts.
     */
    fun handlePermissionRevocation() {
        unregisterObserver()
        _hasPermissionFlow.value = false
        _contacts.value = emptyList()
    }

    private fun scheduleDebouncedReload() {
        reloadJob?.cancel()
        reloadJob = scope.launch {
            delay(300)
            loadContacts()
        }
    }

    /**
     * Queries Google Accounts, SIM cards, and device storage via ContactsProvider.
     * Uses universal AOSP and vendor-safe column projections to prevent SQLite crashes.
     */
    fun queryContactsInternal(): List<ContactItem> {
        val loaded = mutableListOf<ContactItem>()
        val dedupeIndexMap = HashMap<String, Int>(512)
        if (!hasContactsPermission()) {
            return emptyList()
        }

        try {
            // Standard columns guaranteed across all Android versions (including Xiaomi MIUI/HyperOS)
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                ContactsContract.CommonDataKinds.Phone.STARRED,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

            val cursor = try {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )
            } catch (e: Exception) {
                Log.w(TAG, "Projection query failed, attempting generic query: ${e.message}")
                try {
                    context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        null,
                        null,
                        null
                    )
                } catch (ex: Exception) {
                    Log.e(TAG, "Fatal ContactsContract query failure: ${ex.message}")
                    null
                }
            }

            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val contactIdIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val displayNameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val primaryNameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                val starIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                val photoThumbIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                while (it.moveToNext()) {
                    val id = when {
                        idIdx >= 0 && !it.getString(idIdx).isNullOrBlank() -> it.getString(idIdx)
                        contactIdIdx >= 0 && !it.getString(contactIdIdx).isNullOrBlank() -> it.getString(contactIdIdx)
                        else -> System.currentTimeMillis().toString()
                    }

                    // Multi-column name resolution: DISPLAY_NAME -> DISPLAY_NAME_PRIMARY -> Number
                    val nameCandidate = when {
                        displayNameIdx >= 0 && !it.getString(displayNameIdx).isNullOrBlank() -> it.getString(displayNameIdx)
                        primaryNameIdx >= 0 && !it.getString(primaryNameIdx).isNullOrBlank() -> it.getString(primaryNameIdx)
                        else -> null
                    }?.trim()

                    val rawNumber = if (numIdx >= 0) it.getString(numIdx)?.trim() ?: "" else ""
                    val isStarred = if (starIdx >= 0) it.getInt(starIdx) == 1 else false
                    val type = if (typeIdx >= 0) it.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    val customLabel = if (labelIdx >= 0) it.getString(labelIdx) else null
                    val typeLabel = ContactLookupHelper.getPhoneTypeLabel(type, customLabel)

                    val photo = when {
                        photoThumbIdx >= 0 && !it.getString(photoThumbIdx).isNullOrBlank() -> it.getString(photoThumbIdx)
                        photoIdx >= 0 && !it.getString(photoIdx).isNullOrBlank() -> it.getString(photoIdx)
                        else -> null
                    }

                    if (rawNumber.isNotBlank()) {
                        val displayName = if (!nameCandidate.isNullOrBlank() && nameCandidate != "অজ্ঞাত") {
                            nameCandidate
                        } else {
                            rawNumber
                        }

                        // Core Number Normalization (Last 10 Digits for Bangladesh & International) without slow regex
                        val coreDigits = extractCoreDigits(rawNumber)
                        val dedupeKey = "$coreDigits|${displayName.trim().lowercase()}"

                        val existingIndex = dedupeIndexMap[dedupeKey]
                        if (existingIndex != null) {
                            val existing = loaded[existingIndex]
                            loaded[existingIndex] = existing.copy(
                                isFavorite = existing.isFavorite || isStarred,
                                photoUri = existing.photoUri ?: photo,
                                typeLabel = if (existing.typeLabel == "Mobile" && typeLabel != "Mobile") typeLabel else existing.typeLabel
                            )
                        } else {
                            dedupeIndexMap[dedupeKey] = loaded.size
                            loaded.add(
                                ContactItem(
                                    id = id,
                                    name = displayName,
                                    phoneNumber = rawNumber,
                                    typeLabel = typeLabel,
                                    isFavorite = isStarred,
                                    hasWhatsApp = true,
                                    hasMessenger = true,
                                    photoUri = photo
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying contacts from provider: ${e.message}", e)
        }

        return loaded.sortedWith(
            compareByDescending<ContactItem> { it.isFavorite }
                .thenBy { it.name.lowercase() }
        )
    }

    /**
     * Fast non-regex digit extractor for mobile numbers (preserves last 10 digits).
     */
    private fun extractCoreDigits(number: String): String {
        val sb = StringBuilder(16)
        for (i in 0 until number.length) {
            val c = number[i]
            if (c in '0'..'9') {
                sb.append(c)
            }
        }
        return if (sb.length >= 10) sb.substring(sb.length - 10) else sb.toString()
    }

    /**
     * Loads contacts from ContactsProvider into StateFlow.
     * Ensures observer is registered if permission is granted, or clears list if revoked.
     */
    suspend fun loadContacts() = withContext(Dispatchers.IO) {
        val hasPermission = updatePermissionStatus()
        if (!hasPermission) {
            handlePermissionRevocation()
            return@withContext
        }

        ensureObserverRegistered()

        if (!queryMutex.tryLock()) {
            // Already loading contacts, prevent duplicate parallel query
            return@withContext
        }

        _isLoading.value = true
        try {
            val queried = queryContactsInternal()
            _contacts.value = queried
            Log.d(TAG, "Loaded ${queried.size} real contacts from ContactsProvider")
        } catch (e: Exception) {
            Log.e(TAG, "Exception during loadContacts(): ${e.message}", e)
        } finally {
            _isLoading.value = false
            queryMutex.unlock()
        }
    }

    /**
     * Adds a contact directly into the real Android & Google Contacts Provider.
     */
    fun addContact(name: String, number: String, typeLabel: String = "Mobile"): Boolean {
        if (!hasWriteContactsPermission() && !hasContactsPermission()) {
            Log.w(TAG, "Cannot add contact: WRITE_CONTACTS or READ_CONTACTS permission missing")
            return false
        }

        return try {
            val ops = ArrayList<ContentProviderOperation>()

            // 1. Insert RawContact
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // 2. Insert Contact Display Name
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )

            // 3. Insert Phone Number
            val phoneType = when (typeLabel.lowercase()) {
                "home" -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
                "work" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
                else -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
            }

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
                    .build()
            )

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            scope.launch {
                loadContacts()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert contact into ContactsContract: ${e.message}", e)
            false
        }
    }
}
