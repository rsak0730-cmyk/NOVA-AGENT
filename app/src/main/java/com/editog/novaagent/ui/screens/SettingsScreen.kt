package com.editog.novaagent.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.editog.novaagent.data.model.*
import com.editog.novaagent.service.AgentAccessibilityService
import com.editog.novaagent.service.DynamicIslandService
import com.editog.novaagent.ui.components.DynamicIslandPreview
import com.editog.novaagent.ui.components.StyledText
import com.editog.novaagent.ui.theme.applyUiStyle

@Composable
fun SettingsScreen(
    app: NovaApplication,
    currentSettings: AppSettings
) {
    val context = LocalContext.current
    val primaryColor = Color(currentSettings.themeColor.primaryHex)
    val isAccessibilityActive by AgentAccessibilityService.isServiceActive.collectAsState()

    var showGlowColorPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0F))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title
        Column {
            StyledText(
                text = "System & UI Settings",
                style = currentSettings.textAnimationStyle,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                customGlowColor = primaryColor
            )
            Text(
                text = "Customize visuals, animations, Dynamic Island, and automation triggers.",
                color = Color(0xFF9CA3AF),
                fontSize = 13.sp
            )
        }

        // Section 1: Android Shortcuts & Accessibility Permission Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .applyUiStyle(currentSettings.uiDesign, primaryColor)
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Accessibility & Volume Up Shortcut",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isAccessibilityActive) "Active: Press Vol+ to toggle listening anytime" else "Disabled: Tap to enable in Android settings",
                            color = if (isAccessibilityActive) Color(0xFF00FF66) else Color(0xFFFF5555),
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAccessibilityActive) Color(0xFF1E293B) else primaryColor,
                            contentColor = if (isAccessibilityActive) Color.White else Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isAccessibilityActive) "Configured" else "Enable", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.height(10.dp))

                // Overlay Permission & Service Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dynamic Island Floating Overlay",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        val hasOverlay = Settings.canDrawOverlays(context)
                        val isIslandRunning = DynamicIslandService.instance != null
                        Text(
                            text = if (isIslandRunning) "Active: Floating on top of apps" else if (hasOverlay) "Ready: Toggle to show floating pill" else "Disabled: Tap 'Perms' to grant overlay",
                            color = if (isIslandRunning) Color(0xFF00FF66) else Color(0xFF9CA3AF),
                            fontSize = 12.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!Settings.canDrawOverlays(context)) {
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2433)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text("Perms", color = primaryColor, fontSize = 11.sp)
                            }
                        }

                        Switch(
                            checked = currentSettings.dynamicIslandEnabled && Settings.canDrawOverlays(context),
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (Settings.canDrawOverlays(context)) {
                                        app.settingsRepository.updateSettings(currentSettings.copy(dynamicIslandEnabled = true))
                                        try {
                                            context.startService(Intent(context, DynamicIslandService::class.java))
                                        } catch (e: Throwable) {
                                            Log.e("SettingsScreen", "Failed to start DynamicIslandService", e)
                                        }
                                    } else {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    }
                                } else {
                                    app.settingsRepository.updateSettings(currentSettings.copy(dynamicIslandEnabled = false))
                                    try {
                                        context.stopService(Intent(context, DynamicIslandService::class.java))
                                    } catch (e: Throwable) {
                                        Log.e("SettingsScreen", "Failed to stop DynamicIslandService", e)
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = primaryColor,
                                checkedTrackColor = primaryColor.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        }

        // Section 2: Theme Color Change
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .applyUiStyle(currentSettings.uiDesign, primaryColor)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "1. Theme Color Change",
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Select from the 8 neon accent colorways:",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ThemeColor.values().forEach { tColor ->
                        val isSelected = currentSettings.themeColor == tColor
                        val dotColor = Color(tColor.primaryHex)

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .border(
                                    if (isSelected) 3.dp else 1.dp,
                                    if (isSelected) Color.White else Color.Transparent,
                                    CircleShape
                                )
                                .clickable {
                                    app.settingsRepository.updateSettings(currentSettings.copy(themeColor = tColor))
                                    DynamicIslandService.postAction("Theme: ${tColor.displayName}")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (tColor == ThemeColor.NEON_WHITE || tColor == ThemeColor.NEON_YELLOW) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: UI Design Switcher (23 styles)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .applyUiStyle(currentSettings.uiDesign, primaryColor)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "2. UI Design Architecture",
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Current: ${currentSettings.uiDesign.displayName}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                // Grid / Flow of UI styles
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val allStyles = UiDesignStyle.values().toList()
                    allStyles.chunked(2).forEach { rowStyles ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowStyles.forEach { style ->
                                val isSelected = currentSettings.uiDesign == style
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            app.settingsRepository.updateSettings(currentSettings.copy(uiDesign = style))
                                            DynamicIslandService.postAction("UI: ${style.displayName}")
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) primaryColor else Color(0xFF141724),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color.White else Color(0x22FFFFFF)
                                    )
                                ) {
                                    Text(
                                        text = style.displayName,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                            if (rowStyles.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Text Color / Animation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .applyUiStyle(currentSettings.uiDesign, primaryColor)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "3. Text Color / Animation",
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Dynamic rendering engine for headers and AI responses:",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextAnimationStyle.values().forEach { animStyle ->
                        val isSelected = currentSettings.textAnimationStyle == animStyle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) primaryColor.copy(alpha = 0.2f) else Color(0xFF131622))
                                .border(
                                    1.dp,
                                    if (isSelected) primaryColor else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    app.settingsRepository.updateSettings(currentSettings.copy(textAnimationStyle = animStyle))
                                    DynamicIslandService.postAction("Text: ${animStyle.displayName}")
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StyledText(
                                text = animStyle.displayName,
                                style = animStyle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                customGlowColor = primaryColor
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 5: Dynamic Island Settings
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .applyUiStyle(currentSettings.uiDesign, primaryColor)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "4. Dynamic Island Settings",
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Positioning, geometry, and animation physics:",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Live Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color(0xFF090A0F), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    DynamicIslandPreview(
                        settings = currentSettings,
                        actionText = "Live Island Preview"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // X-Axis Offset Slider
                Text(
                    text = "X-Axis Offset: ${currentSettings.dynamicIslandX} px",
                    color = Color.White,
                    fontSize = 13.sp
                )
                Slider(
                    value = currentSettings.dynamicIslandX.toFloat(),
                    onValueChange = { app.settingsRepository.updateSettings(currentSettings.copy(dynamicIslandX = it.toInt())) },
                    valueRange = -300f..300f,
                    colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                )

                // Y-Axis Offset Slider
                Text(
                    text = "Y-Axis Offset: ${currentSettings.dynamicIslandY} px",
                    color = Color.White,
                    fontSize = 13.sp
                )
                Slider(
                    value = currentSettings.dynamicIslandY.toFloat(),
                    onValueChange = { app.settingsRepository.updateSettings(currentSettings.copy(dynamicIslandY = it.toInt())) },
                    valueRange = 0f..200f,
                    colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                )

                // Size: Width Slider
                Text(
                    text = "Pill Width: ${currentSettings.dynamicIslandWidth} dp",
                    color = Color.White,
                    fontSize = 13.sp
                )
                Slider(
                    value = currentSettings.dynamicIslandWidth.toFloat(),
                    onValueChange = { app.settingsRepository.updateSettings(currentSettings.copy(dynamicIslandWidth = it.toInt())) },
                    valueRange = 140f..360f,
                    colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                )

                // Size: Height Slider
                Text(
                    text = "Pill Height: ${currentSettings.dynamicIslandHeight} dp",
                    color = Color.White,
                    fontSize = 13.sp
                )
                Slider(
                    value = currentSettings.dynamicIslandHeight.toFloat(),
                    onValueChange = { app.settingsRepository.updateSettings(currentSettings.copy(dynamicIslandHeight = it.toInt())) },
                    valueRange = 36f..80f,
                    colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                )

                // Corner Rounding Slider
                Text(
                    text = "Corner Rounding Count: ${currentSettings.dynamicIslandCornerRadius} dp",
                    color = Color.White,
                    fontSize = 13.sp
                )
                Slider(
                    value = currentSettings.dynamicIslandCornerRadius.toFloat(),
                    onValueChange = { app.settingsRepository.updateSettings(currentSettings.copy(dynamicIslandCornerRadius = it.toInt())) },
                    valueRange = 6f..40f,
                    colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                )

                // Animation Smoothness (Timeline / Duration Setup)
                Text(
                    text = "Animation Smoothness (Timeline): ${currentSettings.dynamicIslandAnimationDurationMs} ms",
                    color = Color.White,
                    fontSize = 13.sp
                )
                Slider(
                    value = currentSettings.dynamicIslandAnimationDurationMs.toFloat(),
                    onValueChange = { app.settingsRepository.updateSettings(currentSettings.copy(dynamicIslandAnimationDurationMs = it.toInt())) },
                    valueRange = 150f..800f,
                    colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                )
            }
        }
    }
}
