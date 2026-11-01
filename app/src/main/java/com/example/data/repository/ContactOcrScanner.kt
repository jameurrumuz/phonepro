package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class ScannedContactResult(
    val rawText: String,
    val phoneNumbers: List<String>,
    val suggestedName: String?
)

object ContactOcrScanner {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    private val BANGLA_TO_ENG_DIGITS = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    /**
     * Converts all Bengali digits (০-৯) to standard ASCII digits (0-9).
     */
    fun convertBanglaDigitsToEnglish(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(BANGLA_TO_ENG_DIGITS[ch] ?: ch)
        }
        return sb.toString()
    }

    /**
     * Process bitmap image using ML Kit Text Recognition and regex phone extractor
     */
    suspend fun scanImageForContacts(bitmap: Bitmap): ScannedContactResult {
        return suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val text = visionText.text
                        val result = extractContactsFromText(text)
                        continuation.resume(result)
                    }
                    .addOnFailureListener { e ->
                        Log.e("ContactOcrScanner", "OCR failed: ${e.message}")
                        continuation.resume(
                            ScannedContactResult(
                                rawText = "",
                                phoneNumbers = emptyList(),
                                suggestedName = null
                            )
                        )
                    }
            } catch (e: Exception) {
                Log.e("ContactOcrScanner", "Unexpected error: ${e.message}")
                continuation.resume(
                    ScannedContactResult(
                        rawText = "",
                        phoneNumbers = emptyList(),
                        suggestedName = null
                    )
                )
            }
        }
    }

    /**
     * Extracts phone numbers and probable contact names from text, supporting both English
     * and Bengali digits (০-৯), multi-part numbers, and card labels.
     */
    fun extractContactsFromText(text: String): ScannedContactResult {
        val foundNumbers = linkedSetOf<String>()

        // 1. Convert all Bengali digits to ASCII
        val convertedText = convertBanglaDigitsToEnglish(text)

        // 2. Scan every line for visiting card labels and numbers
        val lines = convertedText.lines().map { it.trim() }.filter { it.isNotBlank() }

        for (line in lines) {
            // Extract all digit chunks
            // A line may have: "Phone: 01711-223344 / 01811-556677" or "মোবাইল: ০১৮১১২২৩৩৪৪"
            // Split by common delimiters: '/', ',', ';', '|', '&', 'or', 'বা'
            val subTokens = line.split(Regex("[/,;|&]|\\s+বা\\s+|\\s+or\\s+"))

            for (token in subTokens) {
                val rawDigits = token.replace("[^0-9+]".toRegex(), "")
                
                // Match Bangladeshi 11-digit numbers (013..019)
                if (rawDigits.startsWith("01") && rawDigits.length == 11) {
                    val operatorDigit = rawDigits[2]
                    if (operatorDigit in '3'..'9') {
                        foundNumbers.add(rawDigits)
                    }
                } else if ((rawDigits.startsWith("+8801") && rawDigits.length == 14) ||
                           (rawDigits.startsWith("8801") && rawDigits.length == 13)) {
                    val normalized = if (rawDigits.startsWith("+")) rawDigits else "+$rawDigits"
                    val operatorDigit = normalized[5]
                    if (operatorDigit in '3'..'9') {
                        foundNumbers.add(normalized)
                    }
                }
            }

            // Also check for embedded phone numbers inside the line via regex
            val bdPattern = Regex("(?:\\+?88[- ]?)?01[3-9](?:[- .]?\\d){8}")
            val matches = bdPattern.findAll(line)
            for (m in matches) {
                val clean = m.value.replace("[- .]".toRegex(), "")
                if (clean.length in 11..14) {
                    foundNumbers.add(clean)
                }
            }
        }

        // 3. Fallback regex for spaced or handwritten numbers (e.g. "0 1 7 1 1 2 2 3 3 4 4")
        val spacedPattern = Regex("(?:0\\s*1\\s*[3-9](?:\\s*\\d){8})")
        val spacedMatches = spacedPattern.findAll(convertedText)
        for (m in spacedMatches) {
            val clean = m.value.replace("\\s+".toRegex(), "")
            if (clean.length == 11) {
                foundNumbers.add(clean)
            }
        }

        // 4. Fallback international patterns (e.g. +1..., +44..., etc.)
        val intlPattern = Regex("\\+(?:[0-9][- ]?){9,14}[0-9]")
        val intlMatches = intlPattern.findAll(convertedText)
        for (m in intlMatches) {
            val clean = m.value.replace("[- ]".toRegex(), "")
            if (clean.length in 10..15 && !clean.startsWith("+8801")) {
                foundNumbers.add(clean)
            }
        }

        // 5. Contact name extraction
        val rawLines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        var suggestedName: String? = null

        val ignoredHeaderKeywords = setOf(
            "phone", "mobile", "tel", "cell", "contact", "email", "web", "www", "com",
            "মোবাইল", "ফোন", "নাম্বার", "টেলিফোন", "ঠিকানা", "অফিস", "শাখা", "ব্যাংক",
            "কার্ড", "ভিজিটিং", "হেড", "মেসার্স", "দোকান", "লিমিটেড", "প্রাঃ", "লিমঃ"
        )

        for (line in rawLines) {
            val convertedLine = convertBanglaDigitsToEnglish(line)
            val hasDigits = convertedLine.any { it.isDigit() }
            if (!hasDigits && line.length in 2..35) {
                val lower = line.lowercase()
                val isIgnored = ignoredHeaderKeywords.any { lower.contains(it) } || lower.contains("@")
                if (!isIgnored) {
                    suggestedName = line
                    break
                }
            }
        }

        return ScannedContactResult(
            rawText = text,
            phoneNumbers = foundNumbers.toList(),
            suggestedName = suggestedName
        )
    }
}
