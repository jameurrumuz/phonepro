package com.example.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

object ClipboardUtils {
    /**
     * Copies phone number or text to clipboard with Bengali user feedback
     */
    fun copyToClipboard(context: Context, text: String, label: String = "ফোন নম্বর") {
        val clean = text.trim()
        if (clean.isBlank()) return

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, clean)
        clipboard?.setPrimaryClip(clip)

        Toast.makeText(context, "কপি করা হয়েছে: $clean", Toast.LENGTH_SHORT).show()
    }

    /**
     * Reads primary clip text from clipboard.
     */
    fun pasteFromClipboard(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val item = clipboard.primaryClip?.getItemAt(0)
        val text = item?.text?.toString()?.trim()
        if (!text.isNullOrBlank()) {
            Toast.makeText(context, "পেস্ট করা হয়েছে", Toast.LENGTH_SHORT).show()
        }
        return text
    }
}
