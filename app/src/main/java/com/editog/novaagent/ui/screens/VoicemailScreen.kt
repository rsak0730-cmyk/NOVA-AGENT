package com.editog.novaagent.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    val savedUserPhone by app.voicemailRepository.userPhoneNumber.collectAsState()

    var userPhoneInput by remember(savedUserPhone) { mutableStateOf(savedUserPhone) }
    var isPhoneSavedFeedback by remember { mutableStateOf(false) }
    var isSimulating by remember { mutableStateOf(false) }

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
            
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                Text(
                    text = "iOS-Style 20s Unanswered Call Assistant",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
            }

            // Simulate 20s Call Button
            Button(
                onClick = {
                    if (isSimulating) return@Button
                    isSimulating = true
                    coroutineScope.launch {
                        DynamicIslandService.postAction("📞 Ringing: Alex (5s / 20s)")
                        delay(4000)
                        DynamicIslandService.postAction("📞 Ringing: Alex (15s / 20s)")
                        delay(4000)
                        DynamicIslandService.postAction("🎙️ Agent Answering Voicemail...")
                        app.voiceManager.speak("The owner is currently unavailable. Please leave a voicemail after the tone.")
                        delay(5000)

                        val newVoicemail = VoicemailItem(
                            callerName = "Alex Rivera",
                            phoneNumber = if (savedUserPhone.isNotBlank()) "+1 (555) 234-8901" else "+1 (555) 234-8901",
                            timestamp = System.currentTimeMillis(),
                            durationSeconds = 18,
                            transcript = "Hey! I called your phone number but it rang for 20 seconds. Calling to confirm our plans for tonight. Let me know if that works!"
                        )
                        app.voicemailRepository.addVoicemail(newVoicemail)
                        DynamicIslandService.postAction("New Voicemail from Alex")
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
                    text = if (isSimulating) "Simulating 20s..." else "Simulate 20s Call",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Real User Mobile Number Card
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
                    Text(
                        text = "My Active Mobile Line",
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Surface(
                        color = Color(0x3300FF66),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (savedUserPhone.isNotBlank()) "🟢 AI Guardian Active" else "⚪ Add Your Number",
                            color = if (savedUserPhone.isNotBlank()) Color(0xFF00FF66) else Color(0xFFCBD5E1),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Add your real phone number below. If any incoming call rings for 20 seconds without you picking up, Nova Agent talks to the caller, asks them to leave a voicemail, and transcribes it here.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = userPhoneInput,
                        onValueChange = {
                            userPhoneInput = it
                            isPhoneSavedFeedback = false
                        },
                        placeholder = {
                            Text(
                                text = "e.g. +1 555-0199 or your SIM #",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
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

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            app.voicemailRepository.saveUserPhoneNumber(userPhoneInput)
                            isPhoneSavedFeedback = true
                            DynamicIslandService.postAction("Line Saved: ${userPhoneInput.take(16)}")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Text(
                            text = if (isPhoneSavedFeedback) "Saved ✓" else "Save Line",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Permission warning if telephony is not granted
                if (!hasPhoneStatePermission) {
                    Spacer(modifier = Modifier.height(8.dp))
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
                            text = "⚠️ Tap to grant phone & call detection permissions",
                            color = Color(0xFFFFEA00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

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
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No voicemails yet.\nUnanswered calls (20s) to your mobile line will appear here.",
                        color = Color(0xFF6B7280),
                        fontSize = 13.5.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Text(
                        text = item.callerName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
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
                Text(
                    text = "\"${item.transcript}\"",
                    color = Color(0xFFE2E8F0),
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
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
                        .size(44.dp)
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
