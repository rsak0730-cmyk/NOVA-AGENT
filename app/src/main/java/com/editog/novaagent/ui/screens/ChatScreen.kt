package com.editog.novaagent.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.editog.novaagent.NovaApplication
import com.editog.novaagent.data.model.*
import com.editog.novaagent.service.AgentAccessibilityService
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
    val apiConfig by app.apiConfigRepository.config.collectAsState()
    val isListening by app.voiceManager.isListening.collectAsState()
    val voiceResult by app.voiceManager.recognizedText.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var activePendingAction by remember { mutableStateOf<ActionPendingConfirmation?>(null) }

    val primaryColor = Color(settings.themeColor.primaryHex)

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            app.voiceManager.toggleListening()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.size > 1) {
            try {
                listState.animateScrollToItem(messages.size - 1)
            } catch (e: Throwable) {
                // Ignore initial scroll bounds
            }
        }
    }

    fun executeDecision(decision: AgentDecision) {
        DynamicIslandService.postAction("Nova: ${decision.thought.take(24)}")

        when (val cmd = decision.command) {
            is AgentCommand.OpenApp -> {
                val opened = app.appLauncher.openAppByName(cmd.app_name)
                val badge = if (opened) "Opened ${cmd.app_name}" else "App ${cmd.app_name} not found"
                app.chatRepository.addMessage(
                    ChatMessage(sender = MessageSender.AGENT, content = decision.assistant_response, actionBadge = badge)
                )
                DynamicIslandService.postAction(badge)
            }

            is AgentCommand.Scroll -> {
                val service = AgentAccessibilityService.instance
                val scrolled = service?.scrollMedia(cmd.direction) ?: false
                val badge = if (scrolled) "Scrolled ${cmd.direction.uppercase()}" else "Accessibility needed to scroll"
                app.chatRepository.addMessage(
                    ChatMessage(sender = MessageSender.AGENT, content = decision.assistant_response, actionBadge = badge)
                )
            }

            is AgentCommand.Click -> {
                val service = AgentAccessibilityService.instance
                val clicked = service?.clickElement(cmd.target_text) ?: false
                val badge = if (clicked) "Clicked \"${cmd.target_text}\"" else "Could not find element"
                app.chatRepository.addMessage(
                    ChatMessage(sender = MessageSender.AGENT, content = decision.assistant_response, actionBadge = badge)
                )
            }

            is AgentCommand.TypeText -> {
                val service = AgentAccessibilityService.instance
                val typed = service?.typeTextIntoInput(cmd.target, cmd.text) ?: false
                val badge = if (typed) "Typed \"${cmd.text}\"" else "Input field not focused"
                app.chatRepository.addMessage(
                    ChatMessage(sender = MessageSender.AGENT, content = decision.assistant_response, actionBadge = badge)
                )
            }

            is AgentCommand.FindContact -> {
                val contacts = app.contactsHelper.searchContacts(cmd.name)
                if (contacts.isEmpty()) {
                    app.chatRepository.addMessage(
                        ChatMessage(sender = MessageSender.AGENT, content = "No contact found matching \"${cmd.name}\".")
                    )
                } else if (contacts.size == 1) {
                    val single = contacts.first()
                    activePendingAction = ActionPendingConfirmation(
                        actionType = "CALL",
                        targetName = single.name,
                        targetNumber = single.phoneNumber,
                        candidateRows = contacts
                    )
                    app.chatRepository.addMessage(
                        ChatMessage(
                            sender = MessageSender.AGENT,
                            content = "Found contact ${single.name} (${single.phoneNumber}). Should I make the call?",
                            pendingConfirmation = activePendingAction
                        )
                    )
                } else {
                    activePendingAction = ActionPendingConfirmation(
                        actionType = "CALL",
                        targetName = cmd.name,
                        targetNumber = contacts.first().phoneNumber,
                        candidateRows = contacts
                    )
                    app.chatRepository.addMessage(
                        ChatMessage(
                            sender = MessageSender.AGENT,
                            content = "Multiple numbers found for \"${cmd.name}\". Please choose a row number to proceed with the call:",
                            pendingConfirmation = activePendingAction
                        )
                    )
                }
            }

            is AgentCommand.MakeCall -> {
                activePendingAction = ActionPendingConfirmation(
                    actionType = "CALL",
                    targetName = cmd.contact_name,
                    targetNumber = cmd.phone_number
                )
                app.chatRepository.addMessage(
                    ChatMessage(
                        sender = MessageSender.AGENT,
                        content = "Confirm: Call ${cmd.contact_name} at ${cmd.phone_number}?",
                        pendingConfirmation = activePendingAction
                    )
                )
            }

            is AgentCommand.SendSms -> {
                activePendingAction = ActionPendingConfirmation(
                    actionType = "SMS",
                    targetName = cmd.contact_name,
                    targetNumber = cmd.phone_number,
                    messageContent = cmd.message
                )
                app.chatRepository.addMessage(
                    ChatMessage(
                        sender = MessageSender.AGENT,
                        content = "Confirm sending SMS to ${cmd.contact_name} (${cmd.phone_number}) with text: \"${cmd.message}\"?",
                        pendingConfirmation = activePendingAction
                    )
                )
            }

            is AgentCommand.GeneralResponse -> {
                app.chatRepository.addMessage(
                    ChatMessage(sender = MessageSender.AGENT, content = decision.assistant_response)
                )
            }

            is AgentCommand.InspectScreen -> {
                val screenSummary = AgentAccessibilityService.instance?.inspectCurrentScreen() ?: "Screen unavailable"
                app.chatRepository.addMessage(
                    ChatMessage(
                        sender = MessageSender.AGENT,
                        content = "Watchdog live analysis:\n$screenSummary",
                        actionBadge = "Watchdog Inspection"
                    )
                )
            }
        }

        app.voiceManager.speak(decision.assistant_response)
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        val trimmed = userText.trim()
        textInput = ""

        coroutineScope.launch {
            app.processGlobalCommand(trimmed)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0F))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Left: Creator Instagram Redirect hidden behind Agent Name
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
                        imageVector = Icons.Default.Star,
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

                // Top Right: Delete Full Chat Button
                IconButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FF4444))
                        .border(1.dp, Color(0x66FF4444), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Full Chat",
                        tint = Color(0xFFFF5555),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
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

                if (isProcessing) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = primaryColor
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Nova is thinking and analyzing screen...",
                                color = Color(0xFF9CA3AF),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Bottom Input Bar
            Surface(
                color = Color(0xFF10121A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                text = if (isListening) "Listening to voice..." else "Ask Nova (e.g., 'Open YouTube and scroll')",
                                color = Color(0xFF6B7280),
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp, max = 100.dp),
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

                    // Voice Mic Button with runtime permission check
                    IconButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                app.voiceManager.toggleListening()
                            } else {
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isListening) Color(0xFFFF0055) else primaryColor.copy(alpha = 0.2f))
                            .border(1.dp, if (isListening) Color(0xFFFF0055) else primaryColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Close else Icons.Default.PlayArrow,
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
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (textInput.isNotBlank()) primaryColor else Color(0xFF232736))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
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

        // Action Confirmation Dialog (Call / SMS confirmation)
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
