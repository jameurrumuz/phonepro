package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email

import androidx.compose.ui.window.Dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CallLogEntity
import com.example.data.entity.CallNoteEntity
import com.example.data.model.ContactItem
import com.example.telecom.WhatsAppCallingHelper
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.RecordOrange
import com.example.ui.theme.SpamRed
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.util.ClipboardUtils
import com.example.ui.viewmodel.DialerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentsScreen(
    viewModel: DialerViewModel,
    onOpenDialpad: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val callLogs by viewModel.allCallLogs.collectAsState()
    val smartFavorites by viewModel.smartFavorites.collectAsState()
    val estimatedSavings by viewModel.estimatedSavingsTaka.collectAsState()
    val callNotes by viewModel.callNotes.collectAsState()
    val markedNoWhatsApp by viewModel.markedNoWhatsApp.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()
    val strings = viewModel.strings()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedHistoryTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    val blockedNumbers by viewModel.blockedNumbers.collectAsState()
    val blockedSet = remember(blockedNumbers) {
        blockedNumbers.map { it.phoneNumber.replace("[^0-9]".toRegex(), "").takeLast(10) }.filter { it.isNotBlank() }.toSet()
    }

    val filteredLogs = remember(callLogs, selectedFilter, blockedSet) {
        when (selectedFilter) {
            "MISSED" -> callLogs.filter {
                it.type == "MISSED" && !blockedSet.contains(it.number.replace("[^0-9]".toRegex(), "").takeLast(10))
            }
            "WHATSAPP" -> callLogs.filter { it.callMode == "WHATSAPP" }
            "CELLULAR" -> callLogs.filter {
                it.callMode == "CELLULAR" && it.type != "BLOCKED_SPAM" && !blockedSet.contains(it.number.replace("[^0-9]".toRegex(), "").takeLast(10))
            }
            "SPAM" -> callLogs.filter {
                it.type == "BLOCKED_SPAM" || blockedSet.contains(it.number.replace("[^0-9]".toRegex(), "").takeLast(10))
            }
            else -> callLogs.filter {
                it.type != "BLOCKED_SPAM" && !blockedSet.contains(it.number.replace("[^0-9]".toRegex(), "").takeLast(10))
            }
        }
    }

    val notesByNumber = remember(callNotes) {
        callNotes.associateBy { WhatsAppCallingHelper.sanitizePhoneNumber(it.phoneNumber) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header with OCR Scanner & Cost Savings Badge (promotional subtitle removed as requested)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.callHistory,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick OCR Scanner button
                        FilledIconButton(
                            onClick = { viewModel.openOcrScanner() },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            modifier = Modifier.size(36.dp).testTag("recents_open_ocr_scanner_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Scan Card OCR",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (estimatedSavings > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WhatsAppGreen.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = WhatsAppDarkGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "৳${String.format("%.0f", estimatedSavings)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WhatsAppDarkGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Frequent & Favorite Contacts Quick Carousel
            if (smartFavorites.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Favorites",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.topContacts,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(smartFavorites) { contact ->
                                FrequentVoipCard(
                                    contact = contact,
                                    onWhatsAppCall = { viewModel.makeCall(contact.phoneNumber, contact.name) },
                                    onMessengerCall = { viewModel.makeMessengerCall(contact.name) },
                                    onCellularCall = { viewModel.makeCall(contact.phoneNumber, contact.name, forceCellular = true) },
                                    onChooseSim = { viewModel.showSimSelectionDialog(contact.phoneNumber, contact.name) }
                                )
                            }
                        }
                    }
                }
            }

            // Real Call Logs Header with Filters
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.callHistory} (${filteredLogs.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to strings.all,
                        "MISSED" to strings.missedCalls,
                        "WHATSAPP" to strings.whatsApp,
                        "CELLULAR" to strings.cellular,
                        "SPAM" to strings.spamBlocked
                    )

                    items(filters) { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Call Log items
            if (filteredLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Empty",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = strings.noCallLogsFound,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.noCallLogsDesc,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredLogs, key = { "${it.id}_${it.timestamp}" }) { log ->
                    val sanitized = WhatsAppCallingHelper.sanitizePhoneNumber(log.number)
                    val isNoWa = markedNoWhatsApp.contains(sanitized)
                    val linkedNote = notesByNumber[sanitized]

                    CallLogItemCard(
                        log = log,
                        isNoWhatsApp = isNoWa,
                        linkedNote = linkedNote,
                        onOpenHistory = { num, name ->
                            selectedHistoryTarget = Pair(num, name)
                        },
                        onToggleNoWhatsApp = { viewModel.toggleMarkNoWhatsApp(log.number) },
                        onWhatsAppCall = { viewModel.makeCall(log.number, log.name) },
                        onMessengerCall = { viewModel.makeMessengerCall(log.name) },
                        onCellularCall = { viewModel.makeCall(log.number, log.name, forceCellular = true) },
                        onSwitchSimAndCall = { viewModel.switchSimAndCall(log.number) },
                        onChooseSim = { viewModel.showSimSelectionDialog(log.number, log.name) },
                        onSaveContact = { viewModel.openSaveToGoogleContacts(log.number) },
                        onBlockAsSpam = { viewModel.blockNumber(log.number, log.name, "কল হিস্টোরি থেকে ব্লক করা হয়েছে") },
                        onDelete = { viewModel.deleteCallLog(log) }
                    )
                }
            }

            // Extra padding at bottom for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Floating Action Button to launch Dialpad at bottom-right
        FloatingActionButton(
            onClick = onOpenDialpad,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("open_dialpad_fab"),
            containerColor = PrimaryBlue,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Dialpad,
                contentDescription = "Open Dialpad",
                modifier = Modifier.size(26.dp)
            )
        }

        // Number Call History Dialog
        selectedHistoryTarget?.let { (num, name) ->
            NumberCallHistoryDialog(
                phoneNumber = num,
                callerName = name,
                allLogs = callLogs,
                callNotes = callNotes,
                strings = strings,
                onDismiss = { selectedHistoryTarget = null },
                onCall = { pNum, cName ->
                    selectedHistoryTarget = null
                    viewModel.makeCall(pNum, cName)
                },
                onSms = { pNum ->
                    selectedHistoryTarget = null
                    viewModel.loadSmsForAddress(pNum)
                    viewModel.selectTab(com.example.ui.viewmodel.MainTab.MESSAGES)
                },
                onSaveContact = { pNum, cName ->
                    selectedHistoryTarget = null
                    viewModel.openSaveToGoogleContacts(pNum)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FrequentVoipCard(
    contact: ContactItem,
    onWhatsAppCall: () -> Unit,
    onMessengerCall: () -> Unit,
    onCellularCall: () -> Unit,
    onChooseSim: () -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        modifier = Modifier.width(130.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.take(1),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = contact.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = contact.phoneNumber,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = {
                        ClipboardUtils.copyToClipboard(context, contact.phoneNumber)
                    }
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WhatsApp
                IconButton(
                    onClick = onWhatsAppCall,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Messenger
                IconButton(
                    onClick = onMessengerCall,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0084FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Messenger",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Regular Phone
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = onCellularCall,
                            onLongClick = onChooseSim
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone Call",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallLogItemCard(
    log: CallLogEntity,
    isNoWhatsApp: Boolean,
    linkedNote: CallNoteEntity?,
    onOpenHistory: (String, String) -> Unit = { _, _ -> },
    onToggleNoWhatsApp: () -> Unit,
    onWhatsAppCall: () -> Unit,
    onMessengerCall: () -> Unit,
    onCellularCall: () -> Unit,
    onSwitchSimAndCall: () -> Unit,
    onChooseSim: () -> Unit,
    onSaveContact: () -> Unit,
    onBlockAsSpam: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val isUnknownCaller = log.name.isBlank() || log.name == log.number || log.name == "অপরিচিত কলার"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (log.type == "BLOCKED_SPAM") SpamRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { isExpanded = !isExpanded },
                onLongClick = {
                    ClipboardUtils.copyToClipboard(context, log.number)
                }
            )
            .testTag("call_log_item_${log.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Direction icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when (log.type) {
                                "MISSED" -> SpamRed.copy(alpha = 0.15f)
                                "BLOCKED_SPAM" -> SpamRed.copy(alpha = 0.25f)
                                "INCOMING" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                else -> PrimaryBlue.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (log.type) {
                            "MISSED" -> Icons.AutoMirrored.Filled.CallMissed
                            "BLOCKED_SPAM" -> Icons.Default.Block
                            "INCOMING" -> Icons.AutoMirrored.Filled.CallReceived
                            else -> Icons.AutoMirrored.Filled.CallMade
                        },
                        contentDescription = log.type,
                        tint = when (log.type) {
                            "MISSED", "BLOCKED_SPAM" -> SpamRed
                            "INCOMING" -> Color(0xFF10B981)
                            else -> PrimaryBlue
                        },
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Name and compact badges
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = log.name.ifEmpty { log.number },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (log.type == "MISSED" || log.type == "BLOCKED_SPAM") SpamRed else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Compact Call Mode Badge (Reduced space footprint as requested)
                        if (log.callMode == "WHATSAPP") {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = WhatsAppGreen.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "WhatsApp",
                                        tint = WhatsAppDarkGreen,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "WA",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WhatsAppDarkGreen
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PrimaryBlue.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    val simLabel = if (log.simDisplayName.isNotBlank()) log.simDisplayName else if (log.simSlot == 1) "সিম ২" else "সিম ১"
                                    Icon(
                                        imageVector = Icons.Default.SimCard,
                                        contentDescription = simLabel,
                                        tint = if (log.simSlot == 1) Color(0xFF7C3AED) else PrimaryBlue,
                                        modifier = Modifier.size(9.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = simLabel,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (log.simSlot == 1) Color(0xFF7C3AED) else PrimaryBlue
                                    )
                                }
                            }
                        }

                        // Compact No-WA Indicator (Micro badge so it doesn't occupy extra space)
                        if (isNoWhatsApp) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = "📵",
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }

                        if (log.isRecorded) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Recorded",
                                tint = RecordOrange,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = log.number,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.combinedClickable(
                                onClick = { onOpenHistory(log.number, log.name) },
                                onLongClick = {
                                    ClipboardUtils.copyToClipboard(context, log.number)
                                }
                            )
                        )
                        if (log.durationSeconds > 0) {
                            Text(
                                text = " • ${log.durationSeconds / 60}m ${log.durationSeconds % 60}s",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isUnknownCaller && log.type != "BLOCKED_SPAM") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .clickable { onSaveContact() }
                                    .testTag("save_unknown_chip_${log.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Save Contact",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(9.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "+ সেভ",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Date & Time and Quick Call Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatLogTime(log.timestamp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onCellularCall,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("quick_call_btn_${log.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Quick Call",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Expanded action buttons & in-call auto note display
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // Display Linked In-Call Note if present
                    if (linkedNote != null && linkedNote.noteText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "সংরক্ষিত কল নোট:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = linkedNote.noteText,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        ClipboardUtils.copyToClipboard(context, linkedNote.noteText, "কল নোট")
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Note",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Action buttons Row 1: WhatsApp, Messenger, Cellular, Switch SIM
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // WhatsApp call button
                        if (!isNoWhatsApp) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = WhatsAppGreen.copy(alpha = 0.15f),
                                modifier = Modifier.clickable { onWhatsAppCall() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "WhatsApp Call",
                                        tint = WhatsAppDarkGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", fontSize = 11.sp, color = WhatsAppDarkGreen, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Messenger button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0084FF).copy(alpha = 0.15f),
                            modifier = Modifier.clickable { onMessengerCall() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Messenger",
                                    tint = Color(0xFF0084FF),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Messenger", fontSize = 11.sp, color = Color(0xFF0084FF), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Cellular call button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryBlue.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { onCellularCall() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Cellular Call",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ফোন কল", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Switch SIM and Redial
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF059669).copy(alpha = 0.15f),
                            modifier = Modifier.clickable { onSwitchSimAndCall() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Switch SIM",
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("অন্য সিম", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Action buttons Row 2: SIM Select, Mark No WhatsApp, Copy, Save, Block, Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // History dialog button
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryBlue.copy(alpha = 0.12f),
                            modifier = Modifier.clickable { onOpenHistory(log.number, log.name) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.History, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("হিস্টোরি", fontSize = 10.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Choose SIM dialog
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { onChooseSim() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.SimCard, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("সিম সিলেক্ট", fontSize = 10.sp)
                            }
                        }

                        // Mark No WhatsApp toggle
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isNoWhatsApp) Color(0xFFDC2626).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { onToggleNoWhatsApp() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isNoWhatsApp) "WA আছে" else "No WA মার্ক",
                                    fontSize = 10.sp,
                                    color = if (isNoWhatsApp) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Copy number
                        IconButton(
                            onClick = { ClipboardUtils.copyToClipboard(context, log.number) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(13.dp))
                        }

                        // Block as spam
                        IconButton(
                            onClick = onBlockAsSpam,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Block, contentDescription = "Block", tint = SpamRed, modifier = Modifier.size(13.dp))
                        }

                        // Delete
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun toBanglaDigits(num: Long): String {
    val bn = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return num.toString().map { if (it in '0'..'9') bn[it - '0'] else it }.joinToString("")
}

private fun formatLogTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = (now - timestamp).coerceAtLeast(0)

    val minutes = diff / (60 * 1000)
    val hours = diff / (3600 * 1000)
    val days = diff / (24 * 3600 * 1000)

    return when {
        minutes < 1 -> "এইমাত্র"
        minutes < 60 -> "${toBanglaDigits(minutes)} মিনিট আগে"
        hours < 24 -> "${toBanglaDigits(hours)} ঘণ্টা আগে"
        days < 30 -> "${toBanglaDigits(days)} দিন আগে"
        days < 365 -> "${toBanglaDigits(days / 30)} মাস আগে"
        else -> "${toBanglaDigits(days / 365)} বছর আগে"
    }
}


@Composable
private fun NumberCallHistoryDialog(
    phoneNumber: String,
    callerName: String,
    allLogs: List<CallLogEntity>,
    callNotes: List<com.example.data.entity.CallNoteEntity>,
    strings: com.example.ui.util.AppStrings.Strings,
    onDismiss: () -> Unit,
    onCall: (String, String) -> Unit,
    onSms: (String) -> Unit,
    onSaveContact: (String, String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cleanNumber = phoneNumber.replace("[^0-9]".toRegex(), "")

    val logsForNumber = remember(phoneNumber, allLogs) {
        allLogs.filter { log ->
            val logClean = log.number.replace("[^0-9]".toRegex(), "")
            if (cleanNumber.length >= 10 && logClean.length >= 10) {
                cleanNumber.takeLast(10) == logClean.takeLast(10)
            } else {
                cleanNumber == logClean || log.number == phoneNumber
            }
        }.sortedByDescending { it.timestamp }
    }

    val totalCalls = logsForNumber.size
    val totalSeconds = logsForNumber.sumOf { it.durationSeconds }
    val displayName = if (callerName.isNotBlank() && callerName != "বহির্গামী কল" && callerName != "অপরিচিত কলার") {
        callerName
    } else {
        phoneNumber
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = phoneNumber,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { ClipboardUtils.copyToClipboard(context, phoneNumber) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats Bar (Total Calls, Duration)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = strings.totalCallsCount,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$totalCalls",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = strings.totalDurationText,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${totalSeconds / 60}m ${totalSeconds % 60}s",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = strings.callDetailsTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Chronological History List
                if (logsForNumber.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.noCallLogsFound,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logsForNumber, key = { it.id }) { log ->
                            val linkedNote = callNotes.find { it.phoneNumber == log.number }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = when (log.type) {
                                                    "MISSED" -> Icons.AutoMirrored.Filled.CallMissed
                                                    "BLOCKED_SPAM" -> Icons.Default.Block
                                                    "INCOMING" -> Icons.AutoMirrored.Filled.CallReceived
                                                    else -> Icons.AutoMirrored.Filled.CallMade
                                                },
                                                contentDescription = null,
                                                tint = when (log.type) {
                                                    "MISSED", "BLOCKED_SPAM" -> SpamRed
                                                    "INCOMING" -> Color(0xFF10B981)
                                                    else -> PrimaryBlue
                                                },
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = when (log.type) {
                                                    "MISSED" -> strings.missed
                                                    "BLOCKED_SPAM" -> strings.spamBlocked
                                                    "INCOMING" -> strings.incomingCalls
                                                    else -> strings.outgoingCalls
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = if (log.durationSeconds > 0) "${log.durationSeconds / 60}m ${log.durationSeconds % 60}s" else "0s",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (log.durationSeconds > 0) Color(0xFF10B981) else Color.Gray
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Exact timestamp
                                    val exactDateTime = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(log.timestamp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = exactDateTime,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (log.callMode == "WHATSAPP") "WhatsApp" else "Cellular",
                                            fontSize = 10.sp,
                                            color = PrimaryBlue,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (linkedNote != null && linkedNote.noteText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "📝 ${linkedNote.noteText}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onCall(phoneNumber, callerName) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.callNow, fontSize = 12.sp, color = Color.White)
                    }

                    Button(
                        onClick = { onSms(phoneNumber) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.sendSms, fontSize = 12.sp, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { onSaveContact(phoneNumber, callerName) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.saveContact, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
