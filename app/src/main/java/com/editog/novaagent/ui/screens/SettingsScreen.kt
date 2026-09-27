package com.editog.novaagent.ui.screens

import android.content.Intent
import android.os.Build
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
import androidx.compose.ui.res.painterResource
import com.editog.novaagent.R
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
import com.editog.novaagent.MainActivity
import com.editog.novaagent.ui.theme.TexturePreviewBox

@Composable
fun SettingsScreen(
    app: NovaApplication,
    currentSettings: AppSettings
) {
    val context = LocalContext.current
    val primaryColor = Color(currentSettings.themeColor.primaryHex)
    val isShizukuAvailable by app.shizukuManager.isAvailable.collectAsState()
    val hasShizukuPermission by app.shizukuManager.hasPermission.collectAsState()
    val isWakeWordListening by app.wakeWordManager.isWakeWordListening.collectAsState()
    val isAccessibilityActive by AgentAccessibilityService.isServiceActive.collectAsState()

    var showGlowColorPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            
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

                // Samsung / Android 15 Restricted Settings Guidance Box
                if (!isAccessibilityActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161922), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF333A4D), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Samsung Galaxy / Android 15 Notice:",
                                color = Color(0xFFFFEA00),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "If Accessibility shows 'Restricted setting':\n1. Tap 'Unblock Setting' below to open App Info.\n2. Tap the 3 dots (⋮) in the top-right corner.\n3. Tap 'Allow restricted settings' & enter PIN.\n4. Return and turn on Nova Agent!",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Throwable) {
                                        Log.e("SettingsScreen", "Failed to open app details", e)
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFEA00)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEA00)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Unblock Setting (Open App Info)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

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
                                            val sIntent = Intent(context, DynamicIslandService::class.java)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                context.startForegroundService(sIntent)
                                            } else {
                                                context.startService(sIntent)
                                            }
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
                                    painter = painterResource(R.drawable.ic_check),
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

        // Section 2: Real Textured Theme
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .applyUiStyle(currentSettings.uiDesign, primaryColor)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "2. Real Textured Theme",
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Procedural tactile physical textures rendered live across entire app:",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Current Active Texture Live Interactive Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, primaryColor, RoundedCornerShape(12.dp))
                ) {
                    TexturePreviewBox(
                        texture = currentSettings.textureStyle,
                        primaryColor = primaryColor,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x55000000))
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = primaryColor,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE TEXTURE",
                                        color = Color.Black,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentSettings.textureStyle.displayName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentSettings.textureStyle.description,
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // List of All 10 Real Textured Themes
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextureStyle.values().forEach { texture ->
                        val isSelected = currentSettings.textureStyle == texture
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) primaryColor.copy(alpha = 0.18f) else Color(0x99131622))
                                .border(
                                    1.dp,
                                    if (isSelected) primaryColor else Color(0x22FFFFFF),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    app.settingsRepository.updateSettings(currentSettings.copy(textureStyle = texture))
                                    DynamicIslandService.postAction("Texture: ${texture.displayName.take(18)}")
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mini Live Texture Swatch
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isSelected) primaryColor else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            ) {
                                TexturePreviewBox(
                                    texture = texture,
                                    primaryColor = primaryColor,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = texture.displayName,
                                    color = if (isSelected) primaryColor else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    text = texture.description,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
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
                    text = "4. Text Color / Animation",
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
                                    painter = painterResource(R.drawable.ic_check),
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
                    text = "5. Dynamic Island Settings",
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

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            app.settingsRepository.updateSettings(
                                currentSettings.copy(
                                    dynamicIslandX = 0,
                                    dynamicIslandY = 40,
                                    dynamicIslandWidth = 220,
                                    dynamicIslandHeight = 48,
                                    dynamicIslandCornerRadius = 24
                                )
                            )
                            DynamicIslandService.postAction("Island Centered")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                    ) {
                        Text("Center Island", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            DynamicIslandService.postAction("Testing Live Island")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
                    ) {
                        Text("Test Live Island", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 6: Shizuku Zero-Touch Autopilot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .applyUiStyle(currentSettings.uiDesign, primaryColor)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "6. ⚡ Shizuku Zero-Touch Autopilot",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Control all Android apps autonomously via ADB privileges:",
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = currentSettings.shizukuAutopilotEnabled,
                        onCheckedChange = {
                            app.settingsRepository.updateSettings(currentSettings.copy(shizukuAutopilotEnabled = it))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor, checkedTrackColor = primaryColor.copy(alpha = 0.4f))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status Badge
                val (statusText, statusBg, statusColor) = when {
                    hasShizukuPermission -> Triple("🟢 Active (ADB Privileged) — Full Zero-Touch Control", Color(0xFF0D2818), Color(0xFF00FF66))
                    isShizukuAvailable -> Triple("🟡 Shizuku Running — Permission Required", Color(0xFF2D2408), Color(0xFFFFCC00))
                    else -> Triple("⚪ Shizuku Service Not Running / Disconnected", Color(0xFF1E212D), Color(0xFF9CA3AF))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "With Shizuku authorized, Jarvis can tap, swipe, scroll shorts/reels, type text, and press home/back across ANY app on your device without user touch.",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val act = MainActivity.currentActivity
                            if (act != null) {
                                app.shizukuManager.requestPermission(act)
                            }
                        },
                        enabled = isShizukuAvailable && !hasShizukuPermission,
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF232736),
                            disabledContentColor = Color(0xFF6B7280)
                        )
                    ) {
                        Text(
                            text = if (hasShizukuPermission) "Authorized ✓" else "Authorize Shizuku",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            app.shizukuManager.checkShizukuState()
                            DynamicIslandService.postAction(if (hasShizukuPermission) "Shizuku Connected" else "Checking Shizuku...")
                        },
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                    ) {
                        Text("Check Status", fontSize = 12.sp)
                    }
                }
            }
        }

        // Section 7: Always-On "Hey Jarvis" Wake Word
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .applyUiStyle(currentSettings.uiDesign, primaryColor)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "7. 🎙️ Always-On 'Hey Jarvis' Wake Word",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Hands-free voice wake & sleep across all apps & background:",
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = currentSettings.wakeWordEnabled,
                        onCheckedChange = { enabled ->
                            app.settingsRepository.updateSettings(currentSettings.copy(wakeWordEnabled = enabled))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor, checkedTrackColor = primaryColor.copy(alpha = 0.4f))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Wake Word Status Badge
                val wakeStatusText = if (currentSettings.wakeWordEnabled) {
                    if (isWakeWordListening) "🟢 Standby Active — Say 'Hey Jarvis wake up'" else "🟢 Wake Word Service Running in Background"
                } else {
                    "⚪ Wake Word Disabled"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (currentSettings.wakeWordEnabled) Color(0xFF0D2818) else Color(0xFF1E212D))
                        .border(1.dp, (if (currentSettings.wakeWordEnabled) Color(0xFF00FF66) else Color(0xFF6B7280)).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = wakeStatusText,
                        color = if (currentSettings.wakeWordEnabled) Color(0xFF00FF66) else Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "• Say "Hey Jarvis wake up" or "Wake up": Turns ON listening mode from background or any app.\n• Say "Hey Jarvis turn off" or "Go to sleep": Turns OFF listening mode and returns Jarvis to quiet standby.",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
