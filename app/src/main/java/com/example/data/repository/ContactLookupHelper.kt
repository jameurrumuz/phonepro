package com.example.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat

data class ResolvedContactInfo(
    val name: String,
    val typeLabel: String = "Mobile", // "Home", "Mobile", "Work"
    val photoUri: String? = null,
    val formattedNumber: String = ""
)

object ContactLookupHelper {

    private const val TAG = "ContactLookupHelper"

    /**
     * Checks if READ_CONTACTS permission is granted.
     */
    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Maps ContactsContract phone type int to user-friendly label (Home, Mobile, Work)
     */
    fun getPhoneTypeLabel(type: Int, customLabel: String?): String {
        return when (type) {
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
            ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "Main"
            ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "Other"
            ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> customLabel?.takeIf { it.isNotBlank() } ?: "Mobile"
            else -> "Mobile"
        }
    }

    /**
     * Normalizes phone number variants for Bangladesh & international formats.
     * Given any format (+88017..., 88017..., 017..., 17...), returns a set of candidate representations.
     */
    fun getNormalizedNumberVariants(rawNumber: String): List<String> {
        val clean = rawNumber.trim()
        if (clean.isBlank()) return emptyList()

        val digits = clean.replace("[^0-9]".toRegex(), "")
        val variants = mutableListOf<String>()

        variants.add(clean)

        if (digits.isNotBlank()) {
            variants.add(digits)

            val core10 = if (digits.length >= 10) digits.takeLast(10) else digits

            if (core10.startsWith("1") && core10.length == 10) {
                variants.add("0$core10")       // 01XXXXXXXXX
                variants.add("+880$core10")    // +8801XXXXXXXXX
                variants.add("880$core10")     // 8801XXXXXXXXX
                variants.add(core10)           // 1XXXXXXXXX
            }
        }

        return variants.distinct()
    }

    /**
     * Resolves complete contact details (name, label like "Home", photoUri, number)
     * from Google Contacts / Android Contacts Provider.
     */
    fun resolveContactDetails(context: Context, phoneNumber: String): ResolvedContactInfo? {
        val cleanNumber = phoneNumber.trim()
        if (cleanNumber.isBlank() || !hasContactsPermission(context)) return null

        val variants = getNormalizedNumberVariants(cleanNumber)

        // 1. Multi-variant query via PhoneLookup URI
        for (variant in variants) {
            try {
                val uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(variant)
                )
                val projection = arrayOf(
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.NUMBER,
                    ContactsContract.PhoneLookup.TYPE,
                    ContactsContract.PhoneLookup.LABEL,
                    ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
                    ContactsContract.PhoneLookup.PHOTO_URI
                )

                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                        val numIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.NUMBER)
                        val typeIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.TYPE)
                        val labelIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.LABEL)
                        val photoThumbIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)
                        val photoIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)

                        val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                        if (!name.isNullOrBlank() && name != "বহির্গামী কল" && name != "অপরিচিত কলার") {
                            val num = if (numIdx >= 0) cursor.getString(numIdx) ?: cleanNumber else cleanNumber
                            val type = if (typeIdx >= 0) cursor.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                            val label = if (labelIdx >= 0) cursor.getString(labelIdx) else null
                            val photo = if (photoThumbIdx >= 0) cursor.getString(photoThumbIdx)
                                else if (photoIdx >= 0) cursor.getString(photoIdx) else null

                            return ResolvedContactInfo(
                                name = name,
                                typeLabel = getPhoneTypeLabel(type, label),
                                photoUri = photo,
                                formattedNumber = num
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "PhoneLookup failed for variant $variant: ${e.message}")
            }
        }

        // 2. Comprehensive fallback: Query CommonDataKinds.Phone and match by last 10 digits
        val targetDigits = cleanNumber.replace("[^0-9]".toRegex(), "")
        if (targetDigits.length >= 7) {
            val coreTarget = if (targetDigits.length >= 10) targetDigits.takeLast(10) else targetDigits
            try {
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.LABEL,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                )

                val last7 = targetDigits.takeLast(7)
                val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
                val selectionArgs = arrayOf("%$last7%")

                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                    val labelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                    val photoThumbIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                    val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                    while (cursor.moveToNext()) {
                        val candidateNum = if (numIdx >= 0) cursor.getString(numIdx) ?: "" else ""
                        val candidateDigits = candidateNum.replace("[^0-9]".toRegex(), "")
                        val candidateCore = if (candidateDigits.length >= 10) candidateDigits.takeLast(10) else candidateDigits

                        if (candidateCore == coreTarget) {
                            val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                            if (!name.isNullOrBlank() && name != "বহির্গামী কল" && name != "অপরিচিত কলার") {
                                val type = if (typeIdx >= 0) cursor.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                                val label = if (labelIdx >= 0) cursor.getString(labelIdx) else null
                                val photo = if (photoThumbIdx >= 0) cursor.getString(photoThumbIdx)
                                    else if (photoIdx >= 0) cursor.getString(photoIdx) else null

                                return ResolvedContactInfo(
                                    name = name,
                                    typeLabel = getPhoneTypeLabel(type, label),
                                    photoUri = photo,
                                    formattedNumber = candidateNum
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fallback CommonDataKinds search failed: ${e.message}")
            }
        }

        return null
    }

    /**
     * Resolves contact display name from Android ContactsContract (including Google Account contacts).
     */
    fun resolveContactName(context: Context, phoneNumber: String): String? {
        return resolveContactDetails(context, phoneNumber)?.name
    }

    /**
     * Checks if a phone number exists in contacts.
     */
    fun isSavedContact(context: Context, phoneNumber: String): Boolean {
        return resolveContactDetails(context, phoneNumber) != null
    }
}
