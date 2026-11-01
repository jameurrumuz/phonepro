package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.ui.util.ClipboardUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactItem
import com.example.telecom.SpeedDialManager
import com.example.ui.dialogs.SpeedDialAssignDialog
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.DialerViewModel

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DialerPadScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dialpadText by viewModel.dialpadText.collectAsState()
    val isWhatsAppFirst by viewModel.isWhatsAppFirst.collectAsState()
    val isDefaultDialer by viewModel.isDefaultDialer.collectAsState()
    val matchedContacts by viewModel.matchedContacts.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val speedDials by SpeedDialManager.speedDialMap.collectAsState()

    var speedDialAssignKey by remember { mutableStateOf<Char?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Default dialer alert & controls
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Default Dialer Status Banner
            if (!isDefaultDialer) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Default app",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ডিফল্ট ফোন অ্যাপ হিসেবে সেট করুন",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Button(
                            onClick = {
                                (context as? Activity)?.let { viewModel.requestSetDefaultDialer(it) }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("set_default_dialer_btn")
                        ) {
                            Text("সেট করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Subtle badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ডিফল্ট ফোন অ্যাপ সক্রিয়",
                        fontSize = 11.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Top action bar: WhatsApp First Toggle & Save Contact Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isWhatsAppFirst,
                    onClick = { viewModel.toggleWhatsAppFirst() },
                    label = {
                        Text(
                            text = if (isWhatsAppFirst) "হোয়াটসঅ্যাপ প্রথম কল" else "সাধারণ সেলুলার কল",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isWhatsAppFirst) WhatsAppGreen else MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isWhatsAppFirst) WhatsAppGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = if (isWhatsAppFirst) WhatsAppDarkGreen else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("whatsapp_first_chip")
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // OCR Camera Scanner Button
                    FilledIconButton(
                        onClick = { viewModel.openOcrScanner() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("dialer_ocr_scanner_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "OCR Scanner",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (dialpadText.isNotBlank() && !dialpadText.contains("*") && !dialpadText.contains("#")) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { viewModel.openSaveToGoogleContacts(dialpadText) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp).testTag("save_to_contacts_dialpad_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Save Contact",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ সেভ করুন",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Matching Contacts Row as user types
            AnimatedVisibility(visible = matchedContacts.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(matchedContacts) { contact ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.clickable {
                                viewModel.setDialpadNumber(contact.phoneNumber)
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = contact.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${contact.phoneNumber})",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Typed Number Display with USSD indicator
            val isUssd = dialpadText.contains("*") || dialpadText.contains("#")

            if (isUssd) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "⚡ USSD সার্ভিস কোড (ব্যালেন্স / মিনিট / এমবি চেক)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            val numberScrollState = rememberScrollState()
            LaunchedEffect(dialpadText) {
                if (dialpadText.isNotEmpty()) {
                    numberScrollState.scrollTo(numberScrollState.maxValue)
                }
            }

            val digitFontSize = when {
                dialpadText.length > 20 -> 16.sp
                dialpadText.length > 14 -> 20.sp
                dialpadText.length > 10 -> 24.sp
                else -> 28.sp
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Paste Icon Button
                FilledIconButton(
                    onClick = {
                        val clip = ClipboardUtils.pasteFromClipboard(context)
                        if (!clip.isNullOrBlank()) {
                            viewModel.appendDialpadText(clip)
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("dialpad_paste_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste number",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Centered Single-line Number Display
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(horizontal = 8.dp)
                        .combinedClickable(
                            onClick = {
                                val clip = ClipboardUtils.pasteFromClipboard(context)
                                if (dialpadText.isEmpty() && !clip.isNullOrBlank()) {
                                    viewModel.appendDialpadText(clip)
                                }
                            },
                            onLongClick = {
                                val clip = ClipboardUtils.pasteFromClipboard(context)
                                if (!clip.isNullOrBlank()) {
                                    viewModel.appendDialpadText(clip)
                                } else if (dialpadText.isNotBlank()) {
                                    ClipboardUtils.copyToClipboard(context, dialpadText)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(numberScrollState),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = dialpadText.ifEmpty { "নম্বর ডায়াল বা পেস্ট করুন" },
                            fontSize = digitFontSize,
                            fontWeight = FontWeight.Bold,
                            color = if (dialpadText.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }

                // Backspace with single-tap delete and long-press clear all
                if (dialpadText.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = { viewModel.onDialpadBackspace() },
                                onLongClick = { viewModel.onDialpadClear() }
                            )
                            .testTag("dialpad_backspace_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace (Long tap to clear all)",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(38.dp))
                }
            }
        }

        // Middle Section: 12-key Dialpad with Speed Dial (Keys 1-9)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val rows = listOf(
                listOf(KeyPadItem('1', ""), KeyPadItem('2', "ABC"), KeyPadItem('3', "DEF")),
                listOf(KeyPadItem('4', "GHI"), KeyPadItem('5', "JKL"), KeyPadItem('6', "MNO")),
                listOf(KeyPadItem('7', "PQRS"), KeyPadItem('8', "TUV"), KeyPadItem('9', "WXYZ")),
                listOf(KeyPadItem('*', ""), KeyPadItem('0', "+"), KeyPadItem('#', ""))
            )

            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (item in row) {
                        DialpadKey(
                            item = item,
                            onClick = { viewModel.onDialpadDigit(item.digit) },
                            onLongClick = {
                                if (item.digit == '0') {
                                    viewModel.onDialpadDigit('+')
                                } else if (item.digit in '1'..'9') {
                                    val speedDial = SpeedDialManager.getSpeedDial(item.digit)
                                    if (speedDial != null) {
                                        viewModel.makeCall(speedDial.phoneNumber, speedDial.contactName)
                                    } else {
                                        speedDialAssignKey = item.digit
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Speed Dial Assignment Dialog
        speedDialAssignKey?.let { digit ->
            val existing = SpeedDialManager.getSpeedDial(digit)
            SpeedDialAssignDialog(
                digit = digit,
                currentNumber = existing?.phoneNumber,
                currentName = existing?.contactName,
                contacts = contacts,
                onSave = { num, name ->
                    SpeedDialManager.setSpeedDial(context, digit, num, name)
                    speedDialAssignKey = null
                },
                onRemove = {
                    SpeedDialManager.removeSpeedDial(context, digit)
                    speedDialAssignKey = null
                },
                onDismiss = { speedDialAssignKey = null }
            )
        }

        // Bottom Section: Dual Call Buttons (Instant Cellular Phone Call + WhatsApp Data Call)
        val isUssdCode = dialpadText.contains("*") || dialpadText.contains("#")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // WhatsApp Call Button (for internet data calling when not USSD)
            if (!isUssdCode) {
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = WhatsAppGreen.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .size(54.dp)
                        .clickable {
                            if (dialpadText.isNotBlank()) {
                                viewModel.makeCall(dialpadText, forceCellular = false)
                            }
                        }
                        .testTag("dialer_whatsapp_direct_button")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "WhatsApp Call",
                            tint = WhatsAppDarkGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(54.dp))
            }

            // Big Primary Phone / USSD Call Button (Tap to Call with preferred SIM, Long-press to choose SIM)
            Surface(
                modifier = Modifier
                    .size(width = 160.dp, height = 62.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .combinedClickable(
                        onClick = {
                            if (dialpadText.isNotBlank()) {
                                viewModel.makeCall(dialpadText, forceCellular = true)
                            }
                        },
                        onLongClick = {
                            if (dialpadText.isNotBlank()) {
                                viewModel.showSimSelectionDialog(dialpadText, isUssd = isUssdCode)
                            }
                        }
                    )
                    .testTag("dialer_primary_call_button"),
                shape = RoundedCornerShape(32.dp),
                color = if (isUssdCode) Color(0xFFD97706) else Color(0xFF10B981),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isUssdCode) "USSD ডায়াল" else "ফোন কল",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "সিম বাছতে লং-ট্যাপ",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Clear button
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .size(54.dp)
                    .clickable { viewModel.onDialpadClear() }
                    .testTag("dialer_clear_button")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "মুছুন",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

data class KeyPadItem(
    val digit: Char,
    val letters: String
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DialpadKey(
    item: KeyPadItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("key_${item.digit}")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.digit.toString(),
                fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.letters.isNotEmpty()) {
                Text(
                    text = item.letters,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
