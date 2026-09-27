package com.editog.novaagent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.editog.novaagent.data.model.ThemeColor

@Composable
fun NovaAgentTheme(
    themeColor: ThemeColor = ThemeColor.NEON_BLUE,
    content: @Composable () -> Unit
) {
    val primaryColor = Color(themeColor.primaryHex)
    val accentColor = Color(themeColor.accentHex)

    val colorScheme = darkColorScheme(
        primary = primaryColor,
        secondary = accentColor,
        tertiary = NeonPurple,
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant,
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
