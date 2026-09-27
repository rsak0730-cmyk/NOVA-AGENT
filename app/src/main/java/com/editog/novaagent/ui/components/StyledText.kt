package com.editog.novaagent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    fontStyle: FontStyle? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    customGlowColor: Color = Color(0xFF00F0FF)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_engine")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_glint"
    )

    when (style) {
        TextAnimationStyle.SOLID -> {
            // White typography with a flowing silver-white shimmer sweep
            val shimmerBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f),
                    Color(0xFFE2E8F0),
                    Color(0xFFFFFFFF),
                    Color(0xFFAAAAAA),
                    Color.White.copy(alpha = 0.85f)
                ),
                start = Offset(shimmerOffset, 0f),
                end = Offset(shimmerOffset + 240f, 60f),
                tileMode = TileMode.Clamp
            )
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(brush = shimmerBrush, fontSize = fontSize, fontWeight = fontWeight, fontStyle = fontStyle, textAlign = textAlign, lineHeight = lineHeight),
                maxLines = maxLines,
                overflow = overflow
            )
        }

        TextAnimationStyle.GRADIENT -> {
            // Neon Cyan to Magenta gradient with an animated electric light shimmer passing across
            val shimmerBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00F0FF),
                    Color(0xFFBF00FF),
                    Color(0xFFFFFFFF), // Shimmer glint center
                    Color(0xFFFF007F),
                    Color(0xFF00F0FF)
                ),
                start = Offset(shimmerOffset, 0f),
                end = Offset(shimmerOffset + 320f, 100f),
                tileMode = TileMode.Clamp
            )
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(brush = shimmerBrush, fontSize = fontSize, fontWeight = fontWeight, fontStyle = fontStyle, textAlign = textAlign, lineHeight = lineHeight),
                maxLines = maxLines,
                overflow = overflow
            )
        }

        TextAnimationStyle.AURORA -> {
            // Animated multicolor aurora with continuous bright specular sweep
            val auroraBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00FF66),
                    Color(0xFF00F0FF),
                    Color(0xFFFFFFFF),
                    Color(0xFFBF00FF),
                    Color(0xFFFFEA00),
                    Color(0xFF00FF66)
                ),
                start = Offset(shimmerOffset * 0.7f, 0f),
                end = Offset((shimmerOffset * 0.7f) + 360f, 120f),
                tileMode = TileMode.Clamp
            )
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(brush = auroraBrush, fontSize = fontSize, fontWeight = fontWeight, fontStyle = fontStyle, textAlign = textAlign, lineHeight = lineHeight),
                maxLines = maxLines,
                overflow = overflow
            )
        }

        TextAnimationStyle.GLOW -> {
            // Neon glow halo with an animated shimmering reflection highlight
            val glowBrush = Brush.linearGradient(
                colors = listOf(
                    customGlowColor,
                    Color.White,
                    customGlowColor
                ),
                start = Offset(shimmerOffset, 0f),
                end = Offset(shimmerOffset + 200f, 60f),
                tileMode = TileMode.Clamp
            )
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(
                    brush = glowBrush,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    fontStyle = fontStyle,
                    textAlign = textAlign,
                    lineHeight = lineHeight,
                    shadow = Shadow(
                        color = customGlowColor.copy(alpha = 0.85f),
                        offset = Offset(0f, 0f),
                        blurRadius = 18f
                    )
                ),
                maxLines = maxLines,
                overflow = overflow
            )
        }

        TextAnimationStyle.GLASS -> {
            // Frosted ice neon cyan with crystal glass shimmer reflection
            val glassBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF80D8FF),
                    Color(0xFF00F0FF),
                    Color(0xFFFFFFFF),
                    Color(0xFFE0FFFF),
                    Color(0xFF00B0FF)
                ),
                start = Offset(shimmerOffset, 0f),
                end = Offset(shimmerOffset + 260f, 80f),
                tileMode = TileMode.Clamp
            )
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(brush = glassBrush, fontSize = fontSize, fontWeight = fontWeight, fontStyle = fontStyle, textAlign = textAlign, lineHeight = lineHeight),
                maxLines = maxLines,
                overflow = overflow
            )
        }

        TextAnimationStyle.METALLIC -> {
            // Chrome / Gold metallic surface with a moving polished light glint
            val metallicBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFD4AF37),
                    Color(0xFFFFF8DC),
                    Color(0xFFFFFFFF),
                    Color(0xFFA67C1E),
                    Color(0xFFD4AF37)
                ),
                start = Offset(shimmerOffset, 0f),
                end = Offset(shimmerOffset + 280f, 100f),
                tileMode = TileMode.Clamp
            )
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(brush = metallicBrush, fontSize = fontSize, fontWeight = fontWeight, fontStyle = fontStyle, textAlign = textAlign, lineHeight = lineHeight),
                maxLines = maxLines,
                overflow = overflow
            )
        }

        TextAnimationStyle.HOLOGRAPHIC -> {
            // Iridescent rainbow prism with animated shimmer flare
            val holoBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFF3366),
                    Color(0xFF33CCFF),
                    Color(0xFFFFFFFF),
                    Color(0xFF33FF99),
                    Color(0xFFFFCC00),
                    Color(0xFFCC33FF)
                ),
                start = Offset(shimmerOffset, 0f),
                end = Offset(shimmerOffset + 340f, 90f),
                tileMode = TileMode.Clamp
            )
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(brush = holoBrush, fontSize = fontSize, fontWeight = fontWeight, fontStyle = fontStyle, textAlign = textAlign, lineHeight = lineHeight),
                maxLines = maxLines,
                overflow = overflow
            )
        }

        TextAnimationStyle.LIQUID -> {
            // Fluid water surface reflection with moving neon waves
            val liquidBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0072FF),
                    Color(0xFF00F0FF),
                    Color(0xFFFFFFFF),
                    Color(0xFF00C6FF),
                    Color(0xFF0072FF)
                ),
                start = Offset(shimmerOffset, 0f),
                end = Offset(shimmerOffset + 260f, 110f),
                tileMode = TileMode.Clamp
            )
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(brush = liquidBrush, fontSize = fontSize, fontWeight = fontWeight, fontStyle = fontStyle, textAlign = textAlign, lineHeight = lineHeight),
                maxLines = maxLines,
                overflow = overflow
            )
        }

        TextAnimationStyle.THREE_D -> {
            // 3D extrusion depth shadow with a bright surface shimmer gleam
            val surfaceBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE2E8F0),
                    Color(0xFFFFFFFF),
                    Color(0xFF94A3B8),
                    Color(0xFFFFFFFF)
                ),
                start = Offset(shimmerOffset, 0f),
                end = Offset(shimmerOffset + 220f, 50f),
                tileMode = TileMode.Clamp
            )
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(
                    brush = surfaceBrush,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    fontStyle = fontStyle,
                    textAlign = textAlign,
                    lineHeight = lineHeight,
                    shadow = Shadow(
                        color = Color(0xFF0055FF),
                        offset = Offset(3.5f, 3.5f),
                        blurRadius = 4f
                    )
                ),
                maxLines = maxLines,
                overflow = overflow
            )
        }

        TextAnimationStyle.OUTLINE -> {
            // Outline with traveling neon shimmer light
            val outlineBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.7f),
                    Color(0xFF00F0FF),
                    Color.White,
                    Color(0xFFBF00FF),
                    Color.White.copy(alpha = 0.7f)
                ),
                start = Offset(shimmerOffset, 0f),
                end = Offset(shimmerOffset + 200f, 60f),
                tileMode = TileMode.Clamp
            )
            Text(
                text = text,
                modifier = modifier,
                style = TextStyle(
                    brush = outlineBrush,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    fontStyle = fontStyle,
                    textAlign = textAlign,
                    lineHeight = lineHeight,
                    shadow = Shadow(
                        color = Color(0x6600F0FF),
                        offset = Offset(0f, 0f),
                        blurRadius = 6f
                    )
                ),
                maxLines = maxLines,
                overflow = overflow
            )
        }
    }
}
