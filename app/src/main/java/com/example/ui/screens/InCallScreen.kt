package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.service.ActiveCallInfo
import com.example.service.CallUiState
import com.example.ui.util.AppStrings
import com.example.ui.util.ClipboardUtils

/**
 * Samsung Galaxy One UI Call Screen.
 * Implements pixel-accurate layout for Samsung One UI Incoming Call (Screen 3)
 * and Active Ongoing Call (Screen 2).
 */
@Composable
fun InCallScreen(
    callInfo: ActiveCallInfo,
    activeNoteText: String = "",
    strings: AppStrings.Strings,
    onNoteTextChanged: (String) -> Unit = {},
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleBluetooth: () -> Unit,
    onToggleRecord: () -> Unit,
    onSwitchSim: () -> Unit = {},
    onShareContact: () -> Unit = {},
    onQuickSmsDecline: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isNoteExpanded by remember { mutableStateOf(false) }
    var isKeypadOpen by remember { mutableStateOf(false) }
    var showQuickSmsSheet by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseHalo by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_halo"
    )

    // Samsung ambient atmosphere gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF14171A),
            Color(0xFF1A221E),
            Color(0xFF1D2621),
            Color(0xFF13171A)
        )
    )

    val displayName = when {
        callInfo.callerName.isNotBlank() && callInfo.callerName != "বহির্গামী কল" && callInfo.callerName != strings.unknownCaller -> callInfo.callerName
        callInfo.phoneNumber.isNotBlank() -> callInfo.phoneNumber
        else -> strings.unknownCaller
    }

    val typeLabel = callInfo.callerTypeLabel.ifEmpty { "Mobile" }
    val subtitle = if (callInfo.phoneNumber.isNotBlank()) "$typeLabel  ${callInfo.phoneNumber}" else typeLabel
    val simBadge = if (callInfo.simSlot == 1 || callInfo.simDisplayName == "2") "2" else "1"
    val simColor = if (simBadge == "2") Color(0xFF059669) else Color(0xFF2563EB)

    val topStatusText = when (callInfo.state) {
        CallUiState.RINGING -> "Incoming call"
        CallUiState.DIALING -> "Calling..."
        CallUiState.ACTIVE -> {
            val minutes = callInfo.durationSeconds / 60
            val seconds = callInfo.durationSeconds % 60
            String.format("%02d:%02d", minutes, seconds)
        }
        CallUiState.ON_HOLD -> strings.onHold
        CallUiState.DISCONNECTED -> strings.callEnded
        else -> "Calling..."
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 1. Top SIM Badge & Call Status Indicator (Matching Samsung One UI Header)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = simColor,
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = simBadge,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = topStatusText,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Caller Name (Big Bold White)
            Text(
                text = displayName,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Subtitle: Home / Mobile + Number
            Text(
                text = subtitle,
                fontSize = 15.sp,
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable {
                    if (callInfo.phoneNumber.isNotBlank()) {
                        ClipboardUtils.copyToClipboard(context, callInfo.phoneNumber)
                    }
                }
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 4. Large Circular Contact Avatar Photo (Matching Samsung One UI Center Avatar)
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .background(Color(0xFF263038)),
                contentAlignment = Alignment.Center
            ) {
                if (!callInfo.photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = callInfo.photoUri,
                        contentDescription = "Contact Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(148.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = displayName.take(1).uppercase(),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // In-Call Scratchpad drawer if open
            if (isNoteExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2228)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = strings.callNotes,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = activeNoteText,
                            onValueChange = onNoteTextChanged,
                            placeholder = { Text(strings.notePlaceholder, fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF475569)
                            ),
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 5. Bottom Call Controls
            if (callInfo.state == CallUiState.RINGING) {
                // ==========================================
                // SAMSUNG INCOMING CALL FULL SCREEN CONTROLS
                // ==========================================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Big Green Accept & Red Reject with Translucent Glow Halo Rings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Accept Call (Left)
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .scale(pulseHalo)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E).copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            FilledIconButton(
                                onClick = onAnswer,
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color(0xFF22C55E),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .size(68.dp)
                                    .testTag("answer_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Answer Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Reject Call (Right)
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .scale(pulseHalo)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            FilledIconButton(
                                onClick = onReject,
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color(0xFFEF4444),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .size(68.dp)
                                    .testTag("decline_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "Decline Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // "Send message" with upward chevron at the bottom
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showQuickSmsSheet = !showQuickSmsSheet }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (showQuickSmsSheet) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Quick SMS",
                            tint = Color(0xFFE2E8F0),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Send message",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Quick SMS Dropdown Sheet
                    if (showQuickSmsSheet) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1E2228))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val quickMessages = listOf(
                                "আমি একটু পরে কল ব্যাক করছি।",
                                "আমি এখন মিটিংয়ে আছি, পরে কথা বলছি।",
                                "এখন কথা বলা সম্ভব হচ্ছে না, টেক্সট করুন।"
                            )
                            quickMessages.forEach { msg ->
                                Text(
                                    text = msg,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            showQuickSmsSheet = false
                                            onQuickSmsDecline(msg)
                                            onReject()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // SAMSUNG ONGOING ACTIVE CALL CONTROLS (Screen 2)
                // ==========================================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Floating Dark Control Card (2 rows x 3 columns)
                    Card(
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2228).copy(alpha = 0.95f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Row 1: Add call (+), Video call, Bluetooth
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                SamsungCallActionItem(
                                    icon = Icons.Default.PersonAdd,
                                    label = "Share contact",
                                    isActive = false,
                                    onClick = onShareContact
                                )

                                SamsungCallActionItem(
                                    icon = Icons.Default.EditNote,
                                    label = if (isNoteExpanded) "Close note" else "Live note",
                                    isActive = isNoteExpanded,
                                    activeTint = Color(0xFF38BDF8),
                                    onClick = { isNoteExpanded = !isNoteExpanded }
                                )

                                SamsungCallActionItem(
                                    icon = Icons.Default.Bluetooth,
                                    label = "Bluetooth",
                                    isActive = callInfo.isBluetoothOn,
                                    activeTint = Color(0xFF38BDF8),
                                    onClick = onToggleBluetooth
                                )
                            }

                            // Row 2: Speaker, Mute, Keypad
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                SamsungCallActionItem(
                                    icon = Icons.Default.VolumeUp,
                                    label = "Speaker",
                                    isActive = callInfo.isSpeakerOn,
                                    activeTint = Color(0xFF38BDF8),
                                    onClick = onToggleSpeaker
                                )

                                SamsungCallActionItem(
                                    icon = if (callInfo.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    label = if (callInfo.isMuted) "Unmute" else "Mute",
                                    isActive = callInfo.isMuted,
                                    activeTint = Color(0xFFEF4444),
                                    onClick = onToggleMute
                                )

                                SamsungCallActionItem(
                                    icon = Icons.Default.Dialpad,
                                    label = "Keypad",
                                    isActive = isKeypadOpen,
                                    activeTint = Color(0xFF38BDF8),
                                    onClick = { isKeypadOpen = !isKeypadOpen }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Big Red Circular End Call Button
                    FilledIconButton(
                        onClick = onEndCall,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("end_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Individual action item inside the Samsung One UI Floating Control Card
 */
@Composable
private fun SamsungCallActionItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeTint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isActive) activeTint.copy(alpha = 0.2f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeTint else Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            color = if (isActive) activeTint else Color(0xFFCBD5E1),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
