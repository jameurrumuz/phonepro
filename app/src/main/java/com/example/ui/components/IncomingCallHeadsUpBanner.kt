package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.service.ActiveCallInfo
import com.example.service.CallUiState

/**
 * Samsung Galaxy One UI Call Pop-Up (Heads-Up Banner).
 * Faithfully styled after Samsung One UI Phone incoming call banner.
 */
@Composable
fun IncomingCallHeadsUpBanner(
    callInfo: ActiveCallInfo,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onExpandFullScreen: () -> Unit,
    onQuickSmsDecline: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isRinging = (callInfo.state == CallUiState.RINGING)
    var showQuickSmsMenu by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isRinging,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        val displayName = when {
            callInfo.callerName.isNotBlank() && callInfo.callerName != "বহির্গামী কল" && callInfo.callerName != "অপরিচিত কলার" -> callInfo.callerName
            callInfo.phoneNumber.isNotBlank() -> callInfo.phoneNumber
            else -> "Unknown Caller"
        }

        val typeLabel = callInfo.callerTypeLabel.ifEmpty { "Mobile" }
        val subtitle = if (callInfo.phoneNumber.isNotBlank()) "$typeLabel ${callInfo.phoneNumber}" else typeLabel
        val simBadge = if (callInfo.simSlot == 1 || callInfo.simDisplayName == "2") "2" else "1"
        val simColor = if (simBadge == "2") Color(0xFF059669) else Color(0xFF2563EB)

        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF23252A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clickable { onExpandFullScreen() }
                .testTag("incoming_heads_up_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Top Row: Info (Left) + Circular Contact Photo (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // SIM badge + "Incoming call"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = simColor,
                                modifier = Modifier.size(16.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = simBadge,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "Incoming call",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Big Caller Name
                        Text(
                            text = displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Subtitle: Home / Mobile + Number
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Contact Avatar Photo (Circular)
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!callInfo.photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = callInfo.photoUri,
                                contentDescription = "Caller Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = displayName.take(1).uppercase(),
                                color = Color(0xFFCBD5E1),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Row: "Send message v" (Left) + Accept / Decline Circles (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // "Send message v" pill button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF33363D),
                        modifier = Modifier.clickable {
                            showQuickSmsMenu = !showQuickSmsMenu
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Send message",
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Quick SMS",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Action Buttons (Green Accept + Red Decline)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Green Answer button
                        FilledIconButton(
                            onClick = onAnswer,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF22C55E),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("heads_up_answer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Answer",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Red Decline button
                        FilledIconButton(
                            onClick = onReject,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("heads_up_reject_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Quick Decline SMS Options Dropdown
                if (showQuickSmsMenu) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E2024))
                            .padding(8.dp),
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
                                        showQuickSmsMenu = false
                                        onQuickSmsDecline(msg)
                                        onReject()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
