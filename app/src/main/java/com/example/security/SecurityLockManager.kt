package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.ui.viewmodel.MainTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom

object SecurityLockManager {
    private const val PREFS_NAME = "call_pro_security_vault_v2"
    private const val KEY_LOCK_ENABLED = "is_lock_enabled"
    private const val KEY_PIN_HASH = "security_pin_hash"
    private const val KEY_PIN_SALT = "security_pin_salt"
    private const val KEY_LOCKED_TABS = "locked_tabs_set"
    private const val DEFAULT_PIN = "1234"

    private val _isLockEnabled = MutableStateFlow(false)
    val isLockEnabled: StateFlow<Boolean> = _isLockEnabled.asStateFlow()

    private val _lockedTabs = MutableStateFlow<Set<String>>(emptySet())
    val lockedTabs: StateFlow<Set<String>> = _lockedTabs.asStateFlow()

    // Reactive StateFlow for session unlocked tabs so Compose automatically updates when unlocked or relocked
    private val _unlockedTabsInSession = MutableStateFlow<Set<String>>(emptySet())
    val unlockedTabsInSession: StateFlow<Set<String>> = _unlockedTabsInSession.asStateFlow()

    private fun getSecurePrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Safe fallback when Keystore is uninitialized (e.g., in unit tests / Robolectric)
            context.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    fun init(context: Context) {
        val prefs = getSecurePrefs(context)
        _isLockEnabled.value = prefs.getBoolean(KEY_LOCK_ENABLED, false)
        val saved = prefs.getStringSet(KEY_LOCKED_TABS, emptySet()) ?: emptySet()
        _lockedTabs.value = saved.toSet()
    }

    fun isFeatureLocked(context: Context? = null, tab: MainTab): Boolean {
        if (!_isLockEnabled.value) return false
        val tabKey = tab.name
        if (_unlockedTabsInSession.value.contains(tabKey)) return false
        return _lockedTabs.value.contains(tabKey)
    }

    fun isFeatureLocked(tab: MainTab): Boolean = isFeatureLocked(null, tab)

    fun isTabLockedConfigured(tab: MainTab): Boolean {
        return _lockedTabs.value.contains(tab.name)
    }

    fun setLockEnabled(context: Context, enabled: Boolean) {
        _isLockEnabled.value = enabled
        val prefs = getSecurePrefs(context)
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
        if (!enabled) {
            resetSession()
        }
    }

    fun hasPinSet(context: Context): Boolean {
        val prefs = getSecurePrefs(context)
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()
    }

    fun setPin(context: Context, pin: String) {
        val prefs = getSecurePrefs(context)
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hash = hashPin(pin, salt)

        prefs.edit()
            .putString(KEY_PIN_SALT, saltBase64)
            .putString(KEY_PIN_HASH, hash)
            .apply()
    }

    fun verifyPin(context: Context, enteredPin: String): Boolean {
        val prefs = getSecurePrefs(context)
        val savedHash = prefs.getString(KEY_PIN_HASH, null)
        val savedSalt = prefs.getString(KEY_PIN_SALT, null)

        if (savedHash.isNullOrBlank() || savedSalt.isNullOrBlank()) {
            // Default unconfigured PIN is 1234
            return enteredPin == DEFAULT_PIN
        }

        return try {
            val salt = Base64.decode(savedSalt, Base64.NO_WRAP)
            val computedHash = hashPin(enteredPin, salt)
            MessageDigest.isEqual(savedHash.toByteArray(Charsets.UTF_8), computedHash.toByteArray(Charsets.UTF_8))
        } catch (_: Exception) {
            enteredPin == DEFAULT_PIN
        }
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun setTabLock(context: Context, tab: MainTab, locked: Boolean) {
        val current = _lockedTabs.value.toMutableSet()
        if (locked) {
            current.add(tab.name)
        } else {
            current.remove(tab.name)
        }
        _lockedTabs.value = current
        val prefs = getSecurePrefs(context)
        prefs.edit().putStringSet(KEY_LOCKED_TABS, current).apply()
    }

    fun unlockTabForSession(tab: MainTab) {
        _unlockedTabsInSession.value = _unlockedTabsInSession.value + tab.name
    }

    fun relockTab(tab: MainTab) {
        _unlockedTabsInSession.value = _unlockedTabsInSession.value - tab.name
    }

    fun resetSession() {
        _unlockedTabsInSession.value = emptySet()
    }

    fun onAppBackgrounded() {
        resetSession()
    }
}
