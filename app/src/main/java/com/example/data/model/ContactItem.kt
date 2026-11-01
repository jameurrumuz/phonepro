package com.example.data.model

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val typeLabel: String = "Mobile",
    val isFavorite: Boolean = false,
    val hasWhatsApp: Boolean = true,
    val hasMessenger: Boolean = true,
    val callCount: Int = 0,
    val totalTalkSeconds: Int = 0,
    val photoUri: String? = null
) {
    val totalMinutes: Int get() = totalTalkSeconds / 60
    val isVoipRecommended: Boolean get() = hasWhatsApp || hasMessenger
}
