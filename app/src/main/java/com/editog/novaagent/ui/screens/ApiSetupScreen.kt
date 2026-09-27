package com.editog.novaagent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editog.novaagent.NovaApplication
import com.editog.novaagent.data.model.ApiConfig
import com.editog.novaagent.data.model.ApiProvider
import com.editog.novaagent.data.model.AppSettings
import com.editog.novaagent.service.DynamicIslandService
import com.editog.novaagent.ui.components.StyledText
import com.editog.novaagent.ui.theme.applyUiStyle
import kotlinx.coroutines.launch

@Composable
fun ApiSetupScreen(
    app: NovaApplication,
    settings: AppSettings
) {
    val coroutineScope = rememberCoroutineScope()
    val currentConfig by app.apiConfigRepository.config.collectAsState()

    var selectedProvider by remember { mutableStateOf(currentConfig.provider) }
    var profileName by remember { mutableStateOf(currentConfig.profileName) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var modelName by remember { mutableStateOf(currentConfig.modelName) }
    var baseUrl by remember { mutableStateOf(currentConfig.baseUrl) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var testStatusMessage by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    val primaryColor = Color(settings.themeColor.primaryHex)

    val geminiModels = listOf("gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro", "gemini-1.0-pro")
    val openRouterModels = listOf("meta-llama/llama-3.1-8b-instruct:free", "google/gemini-flash-1.5", "mistralai/mistral-7b-instruct:free", "anthropic/claude-3.5-haiku")
    val openAiModels = listOf("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo")

    val activeModelOptions = when (selectedProvider) {
        ApiProvider.GEMINI -> geminiModels
        ApiProvider.OPENROUTER -> openRouterModels
        ApiProvider.OPENAI -> openAiModels
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        StyledText(
            text = "API Setup & Intelligence",
            style = settings.textAnimationStyle,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            customGlowColor = primaryColor
        )
        Text(
            text = "Configure your AI Studio or LLM keys to give Nova Agent autonomous powers.",
            color = Color(0xFF9CA3AF),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Provider Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131622), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ApiProvider.values().forEach { provider ->
                val isSelected = selectedProvider == provider
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) primaryColor else Color.Transparent)
                        .clickable {
                            selectedProvider = provider
                            baseUrl = provider.defaultBaseUrl
                            modelName = provider.defaultModel
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (provider) {
                            ApiProvider.GEMINI -> "Gemini"
                            ApiProvider.OPENROUTER -> "OpenRouter"
                            ApiProvider.OPENAI -> "OpenAI"
                        },
                        color = if (isSelected) Color.Black else Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Card Container for 4 Fields
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .applyUiStyle(settings.uiDesign, primaryColor)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // 1st: Profile Name
                Column {
                    Text(
                        text = "1. Configuration Name",
                        color = primaryColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("e.g. My AI Studio Studio Key") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // 2nd: API Key Input
                Column {
                    Text(
                        text = "2. API Key",
                        color = primaryColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    painter = painterResource(if (isPasswordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                                    contentDescription = "Toggle Visibility",
                                    tint = Color.White
                                )
                            }
                        },
                        placeholder = { Text("Paste AI Studio / Provider API Key...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // 3rd: Model Selector
                Column {
                    Text(
                        text = "3. Select Model",
                        color = primaryColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Quick Select Model Preset:", color = Color(0xFF6B7280), fontSize = 11.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeModelOptions.take(3).forEach { opt ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (modelName == opt) primaryColor.copy(alpha = 0.3f) else Color(0xFF1B1E2B),
                                modifier = Modifier.clickable { modelName = opt }
                            ) {
                                Text(
                                    text = opt.substringAfter("/"),
                                    color = if (modelName == opt) primaryColor else Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 4th: Base URL
                Column {
                    Text(
                        text = "4. Base URL",
                        color = primaryColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("https://generativelanguage.googleapis.com") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Save & Test Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isTesting = true
                            testStatusMessage = null
                            coroutineScope.launch {
                                val testConfig = ApiConfig(
                                    provider = selectedProvider,
                                    profileName = profileName,
                                    apiKey = apiKey,
                                    modelName = modelName,
                                    baseUrl = baseUrl
                                )
                                val res = app.brainOrchestrator.processIntent("Test connection: Ping!", null, testConfig)
                                isTesting = false
                                res.onSuccess {
                                    testStatusMessage = "Connection Successful! ${it.assistant_response}"
                                    DynamicIslandService.postAction("API Connected")
                                }.onFailure {
                                    testStatusMessage = "Connection Failed: ${it.localizedMessage}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = primaryColor, strokeWidth = 2.dp)
                        } else {
                            Text("Test Key")
                        }
                    }

                    Button(
                        onClick = {
                            val newConfig = ApiConfig(
                                provider = selectedProvider,
                                profileName = profileName,
                                apiKey = apiKey,
                                modelName = modelName,
                                baseUrl = baseUrl
                            )
                            app.apiConfigRepository.saveConfig(newConfig)
                            testStatusMessage = "Saved successfully!"
                            DynamicIslandService.postAction("Config Saved")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
                    ) {
                        Text("Save Config", fontWeight = FontWeight.Bold)
                    }
                }

                if (!testStatusMessage.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF10131D), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = testStatusMessage ?: "",
                            color = if (testStatusMessage?.startsWith("Connection Success") == true || testStatusMessage?.startsWith("Saved") == true) Color(0xFF00FF66) else Color(0xFFFF5555),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
