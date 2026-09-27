package com.editog.novaagent.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.editog.novaagent.NovaApplication
import com.editog.novaagent.data.model.AppSettings
import com.editog.novaagent.data.model.VoicemailItem
import com.editog.novaagent.service.DynamicIslandService
import com.editog.novaagent.service.VoicemailService
import com.editog.novaagent.ui.components.StyledText
import com.editog.novaagent.ui.theme.applyUiStyle
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VoicemailScreen(
    app: NovaApplication,
    settings: AppSettings
) {
    val context = LocalContext.current
    val voicemails by app.voicemailRepository.voicemails.collectAsState()
    val primaryColor = Color(settings.themeColor.primaryHex)

    val dateFormat = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0F))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                StyledText(
                    text = "Live Voicemail",
                    style = settings.textAnimationStyle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    customGlowColor = primaryColor
                )
                Text(
                    text = "iOS-Style 20s Unanswered Call Assistant",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
            }

            // Test Simulation Button
            FilledTonalButton(
                onClick = {
                    val intent = Intent(context, VoicemailService::class.java).apply {
                        putExtra("INCOMING_NUMBER", "+1 (555) 789-0142")
                    }
                    context.startService(intent)
                },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = primaryColor.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Simulate 20s Call", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131722), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PhoneCallback,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "If an incoming call rings for 20 seconds unanswered, Nova Agent answers automatically, asks the caller to record their voicemail, and transcribes it here.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (voicemails.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No voicemails yet.\nUnanswered calls (20s) will be logged here.",
                    color = Color(0xFF6B7280),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(voicemails, key = { it.id }) { item ->
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
                        imageVector = Icons.Default.DeleteOutline,
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
                        imageVector = if (item.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
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
