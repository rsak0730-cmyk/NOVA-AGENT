package com.editog.novaagent.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.editog.novaagent.NovaApplication
import com.editog.novaagent.R
import com.editog.novaagent.data.model.*
import com.editog.novaagent.service.DynamicIslandService
import com.editog.novaagent.ui.components.ConfirmationDialog
import com.editog.novaagent.ui.components.StyledText
import com.editog.novaagent.ui.theme.applyUiStyle
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    app: NovaApplication,
    settings: AppSettings
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages by app.chatRepository.messages.collectAsState()
    val isListening by app.voiceManager.isListening.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var activePendingAction by remember { mutableStateOf<ActionPendingConfirmation?>(null) }
    val primaryColor = Color(settings.themeColor.primaryHex)

    val hasOverlayPermission = remember(Unit) {
        Settings.canDrawOverlays(context)
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            app.voiceManager.toggleListening()
        }
    }

    // Scroll to bottom whenever messages list grows
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            try {
                listState.animateScrollToItem(messages.size - 1)
            } catch (e: Throwable) {}
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        val trimmed = userText.trim()
        textInput = ""

        coroutineScope.launch {
            app.processGlobalCommand(trimmed)
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0F))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Left: Creator Instagram Redirect
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val igUri = Uri.parse("https://instagram.com/edit.og_")
                            val intent = Intent(Intent.ACTION_VIEW, igUri)
                            try {
                                intent.setPackage("com.instagram.android")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, igUri))
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_island_sparkle),
                        contentDescription = "Nova AI",
                        tint = primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        StyledText(
                            text = "Nova Agent",
                            style = settings.textAnimationStyle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            customGlowColor = primaryColor
                        )
                        Text(
                            text = "by @edit.og_",
                            color = primaryColor.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Top Right: Clear Chat Button
                IconButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FF4444))
                        .border(1.dp, Color(0x66FF4444), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_chat),
                        contentDescription = "Delete Full Chat",
                        tint = Color(0xFFFF5555),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Floating Dynamic Island Permission Banner (if not yet granted)
            if (!Settings.canDrawOverlays(context)) {
                Surface(
                    color = Color(0xFF1E1428),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✨",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Float Dynamic Island over all apps like iOS",
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(messages, key = { index, msg -> "${msg.id}_$index" }) { _, msg ->
                    ChatBubbleItem(
                        message = msg,
                        settings = settings,
                        primaryColor = primaryColor,
                        onActionConfirm = {
                            msg.pendingConfirmation?.let { act ->
                                activePendingAction = act
                            }
                        }
                    )
                }
            }

            // Bottom Input Bar - Moves dynamically with soft keyboard!
            Surface(
                color = Color(0xFF10121A),
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                text = if (isListening) "Listening to voice..." else "Ask Jarvis (e.g. 'Open YouTube')",
                                color = Color(0xFF6B7280),
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp, max = 110.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0xFF161924),
                            unfocusedContainerColor = Color(0xFF161924),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Voice Mic Button
                    IconButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                app.voiceManager.toggleListening()
                            } else {
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isListening) Color(0xFFFF0055) else primaryColor.copy(alpha = 0.2f))
                            .border(1.dp, if (isListening) Color(0xFFFF0055) else primaryColor, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(if (isListening) R.drawable.ic_mic_off else R.drawable.ic_mic),
                            contentDescription = "Voice Command",
                            tint = if (isListening) Color.White else primaryColor
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Send Button
                    IconButton(
                        onClick = { sendMessage(textInput) },
                        enabled = textInput.isNotBlank(),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (textInput.isNotBlank()) primaryColor else Color(0xFF232736))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_send),
                            contentDescription = "Send",
                            tint = if (textInput.isNotBlank()) Color.Black else Color(0xFF6B7280)
                        )
                    }
                }
            }
        }

        // Delete Chat Confirmation Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete Full Chat?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("All conversation history with Nova Agent will be permanently wiped.", color = Color(0xFF9CA3AF)) },
                confirmButton = {
                    Button(
                        onClick = {
                            app.chatRepository.clearChat()
                            showDeleteConfirmDialog = false
                            DynamicIslandService.postAction("Chat Cleared")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444), contentColor = Color.White)
                    ) {
                        Text("Delete All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF151824)
            )
        }

        // Action Confirmation Dialog
        activePendingAction?.let { act ->
            ConfirmationDialog(
                pendingAction = act,
                onConfirm = { chosenRow ->
                    val finalNumber = chosenRow?.phoneNumber ?: act.targetNumber
                    if (act.actionType == "CALL") {
                        app.telephonyHelper.dialCall(finalNumber)
                        DynamicIslandService.postAction("Calling $finalNumber")
                    } else if (act.actionType == "SMS") {
                        val body = act.messageContent ?: "Hello from Nova Agent"
                        app.telephonyHelper.sendSms(finalNumber, body)
                        DynamicIslandService.postAction("SMS sent to $finalNumber")
                    }
                    activePendingAction = null
                },
                onDismiss = {
                    activePendingAction = null
                }
            )
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    settings: AppSettings,
    primaryColor: Color,
    onActionConfirm: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .applyUiStyle(settings.uiDesign, primaryColor)
                .padding(12.dp)
        ) {
            Column {
                if (!message.actionBadge.isNullOrBlank()) {
                    Surface(
                        color = primaryColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "⚡ ${message.actionBadge}",
                            color = primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (isUser) {
                    Text(
                        text = message.content,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                } else {
                    StyledText(
                        text = message.content,
                        style = settings.textAnimationStyle,
                        fontSize = 14.sp,
                        customGlowColor = primaryColor
                    )
                }

                if (message.pendingConfirmation != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onActionConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Review & Confirm Action", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
