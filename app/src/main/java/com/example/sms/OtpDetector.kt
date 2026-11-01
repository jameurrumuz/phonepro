package com.example.sms

import com.example.data.repository.ContactOcrScanner

data class DetectedOtp(
    val code: String,
    val sender: String,
    val messageSnippet: String,
    val detectedAt: Long = System.currentTimeMillis()
)

object OtpDetector {

    private val OTP_KEYWORDS = listOf(
        "otp", "code", "verification", "verify", "pin", "password", "passcode",
        "পাসকোড", "কোড", "ভেরিফিকেশন", "পিন", "ওয়ান টাইম",
        "bkash", "nagad", "upay", "rocket", "cellfin", "bank", "login", "security"
    )

    private val DIGIT_CANDIDATE_REGEX = Regex("(?:\\b|[^0-9])([0-9]{4,8})(?:\\b|[^0-9])")

    /**
     * Inspects an incoming SMS text and sender to extract verification OTP codes.
     * Supports both English digits (0-9) and Bengali numerals (০-৯).
     */
    fun detectOtp(sender: String, messageBody: String): DetectedOtp? {
        if (messageBody.isBlank()) return null

        val convertedBody = ContactOcrScanner.convertBanglaDigitsToEnglish(messageBody)
        val lowerConverted = convertedBody.lowercase()

        // Verify if message contains OTP / Security keywords
        val hasOtpKeyword = OTP_KEYWORDS.any { lowerConverted.contains(it) }
        if (!hasOtpKeyword) return null

        // Find candidate digit sequences between 4 and 8 digits
        val matches = DIGIT_CANDIDATE_REGEX.findAll(convertedBody)
        for (match in matches) {
            val candidate = match.groupValues[1]
            
            // Exclude year-like sequences like 2024, 2025, 2026 unless specifically prefixed by OTP/code
            if (candidate.length == 4 && (candidate.startsWith("202") || candidate.startsWith("199"))) {
                // If it looks like a year, verify if "code" or "otp" is right next to it
                val idx = convertedBody.indexOf(candidate)
                val windowStart = (idx - 15).coerceAtLeast(0)
                val windowEnd = (idx + candidate.length + 15).coerceAtMost(convertedBody.length)
                val contextWindow = convertedBody.substring(windowStart, windowEnd).lowercase()
                if (!contextWindow.contains("otp") && !contextWindow.contains("code") && !contextWindow.contains("কোড")) {
                    continue
                }
            }

            // Exclude phone number prefixes like 017, 018, 019 if 11 digits
            if (candidate.length >= 10) continue

            return DetectedOtp(
                code = candidate,
                sender = sender,
                messageSnippet = messageBody.take(120)
            )
        }

        return null
    }
}
