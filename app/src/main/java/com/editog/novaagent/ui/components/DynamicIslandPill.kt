package com.editog.novaagent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editog.novaagent.data.model.AppSettings

@Composable
fun DynamicIslandPreview(
    settings: AppSettings,
    actionText: String = "Nova Agent Active",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .width(settings.dynamicIslandWidth.dp)
            .height(settings.dynamicIslandHeight.dp)
            .clip(RoundedCornerShape(settings.dynamicIslandCornerRadius.dp))
            .background(Color.Black.copy(alpha = 0.92f))
            .border(
                1.5.dp,
                Color(settings.themeColor.primaryHex).copy(alpha = pulseAlpha),
                RoundedCornerShape(settings.dynamicIslandCornerRadius.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI Active",
                tint = Color(settings.themeColor.primaryHex),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = actionText,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
