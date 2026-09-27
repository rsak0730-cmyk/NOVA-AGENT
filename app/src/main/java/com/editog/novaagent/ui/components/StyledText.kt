package com.editog.novaagent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.editog.novaagent.data.model.TextAnimationStyle

@Composable
fun StyledText(
    text: String,
    style: TextAnimationStyle,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    customGlowColor: Color = Color(0xFF00F0FF)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "text_anim")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_shift"
    )

    when (style) {
        TextAnimationStyle.SOLID -> {
            Text(
                text = text,
                modifier = modifier,
                color = Color.White,
                fontSize = fontSize,
                fontWeight = fontWeight
            )
        }

        TextAnimationStyle.GRADIENT -> {
            val brush = Brush.linearGradient(
                colors = listOf(Color(0xFF00F0FF), Color(0xFFBF00FF), Color(0xFFFF007F)),
                start = Offset(0f, 0f),
                end = Offset(400f, 100f)
            )
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(brush = brush, fontSize = fontSize, fontWeight = fontWeight)
            )
        }

        TextAnimationStyle.AURORA -> {
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00FF66),
                    Color(0xFF00F0FF),
                    Color(0xFFBF00FF),
                    Color(0xFFFFEA00),
                    Color(0xFF00FF66)
                ),
                start = Offset(animatedOffset % 300f, 0f),
                end = Offset((animatedOffset % 300f) + 300f, 150f),
                tileMode = TileMode.Clamp
            )
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(brush = brush, fontSize = fontSize, fontWeight = fontWeight)
            )
        }

        TextAnimationStyle.GLOW -> {
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(
                    color = customGlowColor,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    shadow = Shadow(
                        color = customGlowColor.copy(alpha = 0.8f),
                        offset = Offset(0f, 0f),
                        blurRadius = 16f
                    )
                )
            )
        }

        TextAnimationStyle.GLASS -> {
            val brush = Brush.linearGradient(
                colors = listOf(Color(0xFFB0E0E6), Color(0xFF00F0FF), Color(0xFFE0FFFF)),
                start = Offset((animatedOffset * 0.5f) % 200f, 0f),
                end = Offset(((animatedOffset * 0.5f) % 200f) + 200f, 100f),
                tileMode = TileMode.Clamp
            )
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(brush = brush, fontSize = fontSize, fontWeight = fontWeight)
            )
        }

        TextAnimationStyle.METALLIC -> {
            val metallicBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE0E0E0),
                    Color(0xFF888888),
                    Color(0xFFFFFFFF),
                    Color(0xFFAAAAAA)
                ),
                start = Offset(0f, 0f),
                end = Offset(300f, 150f)
            )
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(brush = metallicBrush, fontSize = fontSize, fontWeight = fontWeight)
            )
        }

        TextAnimationStyle.HOLOGRAPHIC -> {
            val holoBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFFF3366),
                    Color(0xFF33CCFF),
                    Color(0xFF33FF99),
                    Color(0xFFFFCC00),
                    Color(0xFFCC33FF)
                )
            )
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(brush = holoBrush, fontSize = fontSize, fontWeight = fontWeight)
            )
        }

        TextAnimationStyle.LIQUID -> {
            val liquidBrush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF00F0FF),
                    Color(0xFF0055FF),
                    Color(0xFF00F0FF)
                )
            )
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(brush = liquidBrush, fontSize = fontSize, fontWeight = fontWeight)
            )
        }

        TextAnimationStyle.THREE_D -> {
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(
                    color = Color.White,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    shadow = Shadow(
                        color = Color(0xFF0055FF),
                        offset = Offset(4f, 4f),
                        blurRadius = 4f
                    )
                )
            )
        }

        TextAnimationStyle.OUTLINE -> {
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(
                    color = Color.White,
                    fontSize = fontSize,
                    fontWeight = fontWeight
                )
            )
        }
    }
}
