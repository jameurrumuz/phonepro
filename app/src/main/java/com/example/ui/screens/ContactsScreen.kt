package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactItem
import com.example.telecom.WhatsAppCallingHelper
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.util.ClipboardUtils
import com.example.ui.viewmodel.DialerViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()
    val markedNoWhatsApp by viewModel.markedNoWhatsApp.collectAsState()
    val isLoading by viewModel.isContactsLoading.collectAsState()
    val hasPermission by viewModel.hasContactsPermission.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[android.Manifest.permission.READ_CONTACTS] == true
        viewModel.onContactsPermissionResult(readGranted)
    }

    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        if (contacts.isEmpty() && hasPermission) {
            viewModel.refreshContacts()
        }
        onPauseOrDispose { }
    }

    androidx.compose.runtime.LaunchedEffect(hasPermission) {
        if (hasPermission && contacts.isEmpty()) {
            viewModel.refreshContacts()
        }
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phoneNumber.contains(searchQuery)
        }
    }

    val favorites = remember(contacts) {
        contacts.filter { it.isFavorite }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header with OCR Scanner button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "পরিচিতি তালিকা",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "নম্বর কপি করতে লং-ট্যাপ করুন",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledIconButton(
                    onClick = { viewModel.openOcrScanner() },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.size(38.dp).testTag("contacts_open_ocr_scanner_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan Card OCR",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("নাম বা নম্বর দিয়ে খুঁজুন...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("contacts_search_field")
            )

            // Favorites section
            if (favorites.isNotEmpty() && searchQuery.isBlank()) {
                Text(
                    text = "প্রিয় পরিচিতি",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(favorites) { contact ->
                        FavoriteContactBadge(
                            contact = contact,
                            onWhatsAppCall = { viewModel.makeCall(contact.phoneNumber, contact.name) },
                            onMessengerCall = { viewModel.makeMessengerCall(contact.name) },
                            onCellularCall = { viewModel.makeCall(contact.phoneNumber, contact.name, forceCellular = true) },
                            onChooseSim = { viewModel.showSimSelectionDialog(contact.phoneNumber, contact.name) }
                        )
                    }
                }
            }

            // Permission Prompt Card if READ_CONTACTS not granted
            if (!hasPermission) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "গুগল পরিচিতি প্রদর্শনের অনুমতি প্রয়োজন",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "আপনার গুগল অ্যাকাউন্ট ও ফোনের পরিচিতিগুলো দেখতে ও সংরক্ষণ করতে পারমিশন দিন।",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.READ_CONTACTS,
                                        android.Manifest.permission.WRITE_CONTACTS
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("অনুমতি দিন", color = Color.White)
                        }
                    }
                }
            } else if (isLoading && filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = PrimaryBlue,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "গুগল পরিচিতি লোড হচ্ছে...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "কোনো পরিচিতি পাওয়া যায়নি" else "পরিচিতি তালিকা খালি",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "অন্য কোনো নাম বা নম্বর দিয়ে খুঁজুন" else "নতুন পরিচিতি যোগ করতে নিচের '+' বাটনে চাপুন",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Contact list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredContacts, key = { "${it.id}_${it.phoneNumber}" }) { contact ->
                        val isNoWa = remember(contact.phoneNumber, markedNoWhatsApp) {
                            val sanitized = WhatsAppCallingHelper.sanitizePhoneNumber(contact.phoneNumber)
                            markedNoWhatsApp.contains(sanitized) || markedNoWhatsApp.contains(contact.phoneNumber)
                        }

                        ContactRowCard(
                            contact = contact,
                            isNoWhatsApp = isNoWa,
                            onToggleNoWhatsApp = { viewModel.toggleMarkNoWhatsApp(contact.phoneNumber) },
                            onWhatsAppCall = { viewModel.makeCall(contact.phoneNumber, contact.name) },
                            onMessengerCall = { viewModel.makeMessengerCall(contact.name) },
                            onCellularCall = { viewModel.makeCall(contact.phoneNumber, contact.name, forceCellular = true) },
                            onChooseSim = { viewModel.showSimSelectionDialog(contact.phoneNumber, contact.name) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        // Add Contact FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_contact_fab"),
            containerColor = PrimaryBlue,
            contentColor = Color.White
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Contact")
        }

        // Add Contact Dialog
        if (showAddDialog) {
            AddContactDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, number ->
                    viewModel.contactsRepository.addContact(name, number)
                    showAddDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteContactBadge(
    contact: ContactItem,
    onWhatsAppCall: () -> Unit,
    onMessengerCall: () -> Unit,
    onCellularCall: () -> Unit,
    onChooseSim: () -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!contact.photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = contact.photoUri,
                        contentDescription = "Contact Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                    )
                } else {
                    Text(
                        text = contact.name.take(1),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = contact.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // WhatsApp
                IconButton(
                    onClick = onWhatsAppCall,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "WhatsApp Call",
                            tint = WhatsAppDarkGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(3.dp))

                // Messenger
                IconButton(
                    onClick = onMessengerCall,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0084FF).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Messenger",
                            tint = Color(0xFF0084FF),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(3.dp))

                // Cellular (Tap = call, Long-click = SIM select)
                Surface(
                    shape = CircleShape,
                    color = PrimaryBlue.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = onCellularCall,
                            onLongClick = onChooseSim
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Cellular Call",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactRowCard(
    contact: ContactItem,
    isNoWhatsApp: Boolean,
    onToggleNoWhatsApp: () -> Unit,
    onWhatsAppCall: () -> Unit,
    onMessengerCall: () -> Unit,
    onCellularCall: () -> Unit,
    onChooseSim: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("contact_item_${contact.id}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!contact.photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = contact.photoUri,
                                contentDescription = "Contact Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(40.dp).clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = contact.name.take(1),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = contact.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isNoWhatsApp) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = "📵 হোয়াটসঅ্যাপ নেই",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Phone number with long-tap copy
                        Text(
                            text = contact.phoneNumber,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.combinedClickable(
                                onClick = { isExpanded = !isExpanded },
                                onLongClick = {
                                    ClipboardUtils.copyToClipboard(context, contact.phoneNumber)
                                }
                            )
                        )
                    }
                }

                // Quick Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isNoWhatsApp) {
                        IconButton(
                            onClick = onWhatsAppCall,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("contact_whatsapp_btn_${contact.id}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "WhatsApp",
                                    tint = WhatsAppDarkGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    // Messenger
                    IconButton(
                        onClick = onMessengerCall,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("contact_messenger_btn_${contact.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0084FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Messenger",
                                tint = Color(0xFF0084FF),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Cellular (Tap = direct call, Long-click = SIM selection)
                    Surface(
                        shape = CircleShape,
                        color = PrimaryBlue.copy(alpha = 0.15f),
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = onCellularCall,
                                onLongClick = onChooseSim
                            )
                            .testTag("contact_cellular_btn_${contact.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Cellular",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Expanded Options: Mark No WhatsApp & Choose SIM
            AnimatedVisibility(visible = isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toggle No WhatsApp flag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isNoWhatsApp) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onToggleNoWhatsApp() }
                    ) {
                        Text(
                            text = if (isNoWhatsApp) "✓ চিহ্নিত: হোয়াটসঅ্যাপ নেই" else "চিহ্নিত করুন: হোয়াটসঅ্যাপ নেই",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isNoWhatsApp) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Choose SIM action
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryBlue.copy(alpha = 0.12f),
                        modifier = Modifier.clickable { onChooseSim() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.SimCard, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("সিম নির্বাচন", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, number: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন পরিচিতি যোগ করুন") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("নাম") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("new_contact_name_input")
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("ফোন নম্বর") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_contact_number_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && number.isNotBlank()) {
                        onConfirm(name, number)
                    }
                },
                enabled = name.isNotBlank() && number.isNotBlank(),
                modifier = Modifier.testTag("save_new_contact_btn")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
