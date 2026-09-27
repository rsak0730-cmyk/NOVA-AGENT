package com.editog.novaagent.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.editog.novaagent.data.model.AppSettings
import com.editog.novaagent.data.model.VoicemailItem
import com.editog.novaagent.service.DynamicIslandService
import com.editog.novaagent.ui.components.StyledText
import com.editog.novaagent.ui.theme.applyUiStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VoicemailScreen(
    app: NovaApplication,
    settings: AppSettings
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val voicemails by app.voicemailRepository.voicemails.collectAsState()
    val userProfile by app.voicemailRepository.userProfile.collectAsState()

    var ownerNameInput by remember(userProfile.ownerName) { mutableStateOf(userProfile.ownerName) }
    var phoneInput by remember(userProfile.phoneNumber) { mutableStateOf(userProfile.phoneNumber) }
    var greetingInput by remember(userProfile.greetingMessage) { mutableStateOf(userProfile.greetingMessage) }
    var ringSecondsInput by remember(userProfile.ringSeconds) { mutableStateOf(userProfile.ringSeconds.toFloat()) }
    var guardianActiveInput by remember(userProfile.isGuardianActive) { mutableStateOf(userProfile.isGuardianActive) }
    var screeningModeInput by remember(userProfile.screeningMode) { mutableStateOf(userProfile.screeningMode) }

    var isSavedFeedback by remember { mutableStateOf(false) }
    var isSimulating by remember { mutableStateOf(false) }
    var isProfileExpanded by remember { mutableStateOf(userProfile.phoneNumber.isBlank()) }

    val primaryColor = Color(settings.themeColor.primaryHex)
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }

    val hasPhoneStatePermission = remember(Unit) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                StyledText(
                    text = "Live Voicemail",
                    style = settings.textAnimationStyle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    customGlowColor = primaryColor
                )
                StyledText(
                    text = "iOS-Style 20s Unanswered Call Assistant",
                    style = settings.textAnimationStyle,
                    fontSize = 11.5.sp,
                    customGlowColor = primaryColor
                )
            }

            // Simulate 20s Call Button
            Button(
                onClick = {
                    if (isSimulating) return@Button
                    isSimulating = true
                    coroutineScope.launch {
                        val callerLabel = "Alex Rivera"
                        DynamicIslandService.postAction("📞 Ringing: $callerLabel (5s / ${userProfile.ringSeconds}s)")
                        delay(4000)
                        DynamicIslandService.postAction("📞 Ringing: $callerLabel (15s / ${userProfile.ringSeconds}s)")
                        delay(4000)
                        DynamicIslandService.postAction("🎙️ Agent Answering Voicemail...")
                        val spokenGreeting = if (greetingInput.isNotBlank()) greetingInput else "The subscriber is unavailable. Please leave a voicemail after the tone."
                        app.voiceManager.speak(spokenGreeting)
                        delay(5500)

                        val newVoicemail = VoicemailItem(
                            callerName = callerLabel,
                            phoneNumber = "+1 (555) 234-8901",
                            timestamp = System.currentTimeMillis(),
                            durationSeconds = 18,
                            transcript = "Hey ${if (ownerNameInput.isNotBlank()) ownerNameInput else "there"}! I called your number and it rang for ${userProfile.ringSeconds} seconds without answer. Calling regarding our meeting update. Please call me back!"
                        )
                        app.voicemailRepository.addVoicemail(newVoicemail)
                        DynamicIslandService.postAction("New Voicemail from $callerLabel")
                        isSimulating = false
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSimulating) Color(0xFFFF0055) else Color(0xFF1E2232),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isSimulating) "Simulating..." else "Simulate 20s Call",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Real User Mobile Number & Voicemail Profile Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .applyUiStyle(settings.uiDesign, primaryColor)
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isProfileExpanded = !isProfileExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_voicemail),
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StyledText(
                            text = "My Mobile Line & Voicemail Profile",
                            style = settings.textAnimationStyle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            customGlowColor = primaryColor
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (guardianActiveInput && phoneInput.isNotBlank()) Color(0x3300FF66) else Color(0x22FFFFFF),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (guardianActiveInput && phoneInput.isNotBlank()) "🟢 Active" else "⚪ Setup",
                                color = if (guardianActiveInput && phoneInput.isNotBlank()) Color(0xFF00FF66) else Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isProfileExpanded) "▲" else "▼",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }

                if (isProfileExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))

                    StyledText(
                        text = "Configure your real mobile phone number, owner name, and custom agent greeting. If an incoming call rings for 20 seconds without answer, Nova Agent intervenes, speaks to the caller, and records their voicemail.",
                        style = settings.textAnimationStyle,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        customGlowColor = primaryColor
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Field 1: User / Owner Name
                    Text("Owner / My Name:", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(3.dp))
                    OutlinedTextField(
                        value = ownerNameInput,
                        onValueChange = {
                            ownerNameInput = it
                            isSavedFeedback = false
                        },
                        placeholder = { Text("e.g. Alex or your name", color = Color(0xFF64748B), fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0xFF0E111A),
                            unfocusedContainerColor = Color(0xFF0E111A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Field 2: Real Mobile Phone Number
                    Text("My Active Mobile Phone Number:", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(3.dp))
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = {
                            phoneInput = it
                            isSavedFeedback = false
                        },
                        placeholder = { Text("e.g. +1 (555) 019-2834 or your SIM number", color = Color(0xFF64748B), fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0xFF0E111A),
                            unfocusedContainerColor = Color(0xFF0E111A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Field 3: Custom Voicemail Greeting Prompt
                    Text("Custom Agent Greeting to Callers:", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(3.dp))
                    OutlinedTextField(
                        value = greetingInput,
                        onValueChange = {
                            greetingInput = it
                            isSavedFeedback = false
                        },
                        placeholder = { Text("Hello, I am unavailable right now. Please leave your message after the tone.", color = Color(0xFF64748B), fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp, max = 90.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0xFF0E111A),
                            unfocusedContainerColor = Color(0xFF0E111A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Field 4: Ringing Wait Time Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Unanswered Ringing Delay:", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("${ringSecondsInput.toInt()} seconds", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = ringSecondsInput,
                        onValueChange = {
                            ringSecondsInput = it
                            isSavedFeedback = false
                        },
                        valueRange = 10f..45f,
                        steps = 6,
                        colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                    )

                    // Field 5: Screening Mode selection chips
                    Text("Screening Mode:", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("All Callers", "Unknown Only", "Contacts Only").forEach { mode ->
                            val isSelected = screeningModeInput == mode
                            Surface(
                                color = if (isSelected) primaryColor else Color(0xFF1E2232),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable {
                                    screeningModeInput = mode
                                    isSavedFeedback = false
                                }
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSelected) Color.Black else Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Field 6: Guardian Enable / Disable Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Voicemail Auto-Answer Guardian", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Agent answers when unanswered for ${ringSecondsInput.toInt()}s", color = Color(0xFF94A3B8), fontSize = 10.5.sp)
                        }
                        Switch(
                            checked = guardianActiveInput,
                            onCheckedChange = {
                                guardianActiveInput = it
                                isSavedFeedback = false
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = primaryColor, checkedTrackColor = primaryColor.copy(alpha = 0.4f))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Save Profile Button
                    Button(
                        onClick = {
                            app.voicemailRepository.saveVoicemailProfile(
                                ownerName = ownerNameInput,
                                phoneNumber = phoneInput,
                                greetingMessage = greetingInput,
                                ringSeconds = ringSecondsInput.toInt(),
                                isGuardianActive = guardianActiveInput,
                                screeningMode = screeningModeInput
                            )
                            isSavedFeedback = true
                            DynamicIslandService.postAction("Voicemail Profile Saved")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isSavedFeedback) "Voicemail Profile Saved ✓" else "Save Voicemail Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    // Collapsed Summary
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (userProfile.phoneNumber.isNotBlank())
                            "Line: ${userProfile.phoneNumber} (${if (userProfile.ownerName.isNotBlank()) userProfile.ownerName else "User"}) • Answers after ${userProfile.ringSeconds}s • Tap to expand"
                        else
                            "No phone number added yet. Tap to expand and setup your mobile line.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                }

                // Telephony permission check
                if (!hasPhoneStatePermission) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.READ_CALL_LOG,
                                    Manifest.permission.RECORD_AUDIO
                                )
                            )
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "⚠️ Tap to grant phone & incoming call detection permissions",
                            color = Color(0xFFFFEA00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Voicemails List
        if (voicemails.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_voicemail),
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StyledText(
                        text = "No voicemails yet.\nUnanswered calls (${userProfile.ringSeconds}s) to your mobile line will appear here.",
                        style = settings.textAnimationStyle,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp,
                        customGlowColor = primaryColor
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(voicemails, key = { index, item -> "${item.id}_$index" }) { _, item ->
                    VoicemailCard(
                        item = item,
                        settings = settings,
                        primaryColor = primaryColor,
                        formattedDate = dateFormat.format(Date(item.timestamp)),
                        onTogglePlay = {
                            app.voicemailRepository.togglePlayback(item.id)
                            if (!item.isPlaying) {
                                app.voiceManager.speak("Playing voicemail from ${item.callerName}: ${item.transcript}")
                                DynamicIslandService.postAction("Playing Voicemail: ${item.callerName}")
                            } else {
                                app.voiceManager.stopSpeaking()
                                DynamicIslandService.postAction("Playback Stopped")
                            }
                        },
                        onDelete = {
                            app.voicemailRepository.deleteVoicemail(item.id)
                            DynamicIslandService.postAction("Voicemail Deleted")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VoicemailCard(
    item: VoicemailItem,
    settings: AppSettings,
    primaryColor: Color,
    formattedDate: String,
    onTogglePlay: () -> Unit,
    onDelete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_anim")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveform"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .applyUiStyle(settings.uiDesign, primaryColor)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    StyledText(
                        text = item.callerName,
                        style = settings.textAnimationStyle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        customGlowColor = primaryColor
                    )
                    Text(
                        text = "${item.phoneNumber} • $formattedDate",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp
                    )
                }

                // Delete Voicemail Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_chat),
                        contentDescription = "Delete Voicemail",
                        tint = Color(0xFFFF5555),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transcript Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F1118), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                StyledText(
                    text = "\"${item.transcript}\"",
                    style = settings.textAnimationStyle,
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    customGlowColor = primaryColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Playback Bar with Listen On/Off Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Listen On / Off Button
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (item.isPlaying) Color(0xFFFF0055) else primaryColor)
                ) {
                    Icon(
                        painter = painterResource(if (item.isPlaying) R.drawable.ic_stop else R.drawable.ic_play),
                        contentDescription = if (item.isPlaying) "Stop Listening" else "Listen",
                        tint = Color.Black
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (item.isPlaying) "Listening to Voicemail (Playing...)" else "Tap to Listen (${item.durationSeconds}s)",
                        color = if (item.isPlaying) primaryColor else Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        fontWeight = if (item.isPlaying) FontWeight.Bold else FontWeight.Normal
                    )

                    // Audio Waveform Visualization
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..24) {
                            val heightFactor = if (item.isPlaying) (if (i % 2 == 0) waveScale else 2f - waveScale) else 1f
                            val baseHeight = (8 + (i % 7) * 3).dp * heightFactor
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(baseHeight.coerceIn(4.dp, 28.dp))
                                    .background(
                                        if (item.isPlaying) primaryColor else Color(0xFF33384B),
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
