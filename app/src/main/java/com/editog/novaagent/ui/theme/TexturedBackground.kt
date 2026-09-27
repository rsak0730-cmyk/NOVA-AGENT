package com.editog.novaagent.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.editog.novaagent.data.model.AppTextureStyle
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TexturedBackground(
    texture: AppTextureStyle,
    primaryColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawTexturePattern(texture, primaryColor)
        }
        content()
    }
}

@Composable
fun TexturePreviewBox(
    texture: AppTextureStyle,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawTexturePattern(texture, primaryColor)
    }
}

fun DrawScope.drawTexturePattern(
    texture: AppTextureStyle,
    primaryColor: Color
) {
    when (texture) {
        AppTextureStyle.CARBON_FIBER -> {
            // Base carbon weave dark charcoal
            drawRect(Color(0xFF0C0E14))

            val tileSize = 20.dp.toPx()
            val halfTile = tileSize / 2f
            val width = size.width
            val height = size.height

            var y = 0f
            var row = 0
            while (y < height) {
                var x = 0f
                var col = 0
                while (x < width) {
                    val isAlt = (row + col) % 2 == 0
                    val blockColor = if (isAlt) Color(0xFF141822) else Color(0xFF181D2A)
                    drawRect(
                        color = blockColor,
                        topLeft = Offset(x, y),
                        size = Size(halfTile, halfTile)
                    )

                    // Draw diagonal weave thread lines
                    val lineColor = if (isAlt) Color(0x33FFFFFF) else Color(0x22000000)
                    for (i in 0..2) {
                        val offset = i * (halfTile / 3f)
                        if (isAlt) {
                            drawLine(
                                color = lineColor,
                                start = Offset(x + offset, y),
                                end = Offset(x, y + offset),
                                strokeWidth = 1.2f
                            )
                        } else {
                            drawLine(
                                color = lineColor,
                                start = Offset(x + offset, y + halfTile),
                                end = Offset(x + halfTile, y + offset),
                                strokeWidth = 1.2f
                            )
                        }
                    }
                    x += halfTile
                    col++
                }
                y += halfTile
                row++
            }

            // Subtle carbon fiber diagonal sheen reflection
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.05f),
                        Color(0x10FFFFFF),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(width, height)
                )
            )
        }

        AppTextureStyle.BRUSHED_TITANIUM -> {
            // Metallic slate base
            drawRect(Color(0xFF11141C))

            val width = size.width
            val height = size.height

            // Micro brushed horizontal striations
            var y = 0f
            var seed = 42L
            while (y < height) {
                seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
                val alpha = (seed % 18 + 6) / 255f
                val isLight = (seed % 2) == 0L
                val color = if (isLight) Color.White.copy(alpha = alpha * 0.7f) else Color.Black.copy(alpha = alpha)

                drawLine(
                    color = color,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.2f
                )
                y += 2.dp.toPx()
            }

            // Specular reflection band across the metal
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x05FFFFFF),
                        primaryColor.copy(alpha = 0.07f),
                        Color(0x20FFFFFF),
                        Color.Transparent
                    ),
                    start = Offset(0f, height * 0.2f),
                    end = Offset(width, height * 0.8f)
                )
            )
        }

        AppTextureStyle.HEXAGON_CYBER_MESH -> {
            drawRect(Color(0xFF090B10))

            val hexRadius = 16.dp.toPx()
            val hexHeight = hexRadius * 2f
            val hexWidth = kotlin.math.sqrt(3f) * hexRadius
            val width = size.width
            val height = size.height

            var row = 0
            var y = 0f
            while (y < height + hexHeight) {
                var col = 0
                val xOffset = if (row % 2 == 1) hexWidth / 2f else 0f
                var x = xOffset - hexWidth
                while (x < width + hexWidth) {
                    // Draw hexagon outline
                    val path = Path()
                    for (i in 0..5) {
                        val angle = Math.toRadians((60.0 * i) + 30.0).toFloat()
                        val px = x + hexRadius * cos(angle)
                        val py = y + hexRadius * sin(angle)
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    drawPath(path, color = Color(0x242A384F), style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f))

                    // Intersecting glowing nodes
                    if ((row + col) % 3 == 0) {
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.35f),
                            radius = 2.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 1.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }

                    x += hexWidth
                    col++
                }
                y += hexHeight * 0.75f
                row++
            }

            // Ambient cyber glow overlay
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.09f), Color.Transparent),
                    center = Offset(width * 0.5f, height * 0.4f),
                    radius = width * 0.7f
                )
            )
        }

        AppTextureStyle.CIRCUIT_BOARD -> {
            drawRect(Color(0xFF080C14))

            val width = size.width
            val height = size.height
            val step = 32.dp.toPx()

            // Draw circuit traces (grid-aligned orthogonal with 45-deg corners)
            val traceColor = primaryColor.copy(alpha = 0.16f)
            val brightTrace = Color(0x3500F0FF)

            var x = 16.dp.toPx()
            var stepCount = 0
            while (x < width) {
                val isBright = stepCount % 4 == 0
                val col = if (isBright) brightTrace else traceColor

                // Vertical bus trace with doglegs
                val path = Path().apply {
                    moveTo(x, 0f)
                    var curY = 0f
                    while (curY < height) {
                        curY += step * 1.5f
                        lineTo(x, curY)
                        lineTo(x + 12.dp.toPx(), curY + 12.dp.toPx())
                        curY += 12.dp.toPx()
                        lineTo(x + 12.dp.toPx(), curY + step)
                        curY += step
                    }
                }
                drawPath(path, color = col, style = androidx.compose.ui.graphics.drawscope.Stroke(1.4f))

                // PCB via pads (solder points)
                var vy = 24.dp.toPx()
                while (vy < height) {
                    drawCircle(
                        color = Color(0xFF1E2838),
                        radius = 3.5.dp.toPx(),
                        center = Offset(x, vy)
                    )
                    drawCircle(
                        color = if (isBright) primaryColor else Color(0x44FFFFFF),
                        radius = 1.8.dp.toPx(),
                        center = Offset(x, vy)
                    )
                    vy += step * 2f
                }

                x += step
                stepCount++
            }

            // Cross-horizontal bus lines
            var y = 20.dp.toPx()
            while (y < height) {
                drawLine(
                    color = traceColor.copy(alpha = 0.12f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.2f
                )
                y += step * 1.8f
            }
        }

        AppTextureStyle.PERFORATED_LEATHER -> {
            // Luxury dark leather base
            drawRect(Color(0xFF101217))

            val width = size.width
            val height = size.height
            val spacing = 14.dp.toPx()
            val holeRadius = 2.2.dp.toPx()

            var row = 0
            var y = spacing / 2f
            while (y < height) {
                val xOffset = if (row % 2 == 1) spacing / 2f else 0f
                var x = xOffset
                while (x < width) {
                    // Deep dark hole cavity
                    drawCircle(
                        color = Color(0xFF060709),
                        radius = holeRadius,
                        center = Offset(x, y)
                    )
                    // Lower specular crescent highlight for authentic 3D depth
                    drawArc(
                        color = Color(0x28FFFFFF),
                        startAngle = 20f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(x - holeRadius, y - holeRadius + 0.6f),
                        size = Size(holeRadius * 2f, holeRadius * 2f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(0.8f)
                    )
                    x += spacing
                }
                y += spacing * 0.866f
                row++
            }
        }

        AppTextureStyle.OBSIDIAN_GRANITE -> {
            // Volcanic dark mineral stone base
            drawRect(Color(0xFF0E1017))

            val width = size.width
            val height = size.height

            // Procedural mineral crystal flecks
            var seed = 1234567L
            val count = (width * height / 350f).toInt().coerceIn(400, 3000)
            for (i in 0 until count) {
                seed = (seed * 1664525L + 1013904223L) and 0xffffffffL
                val px = (seed % width.toLong()).toFloat()
                seed = (seed * 1664525L + 1013904223L) and 0xffffffffL
                val py = (seed % height.toLong()).toFloat()
                seed = (seed * 1664525L + 1013904223L) and 0xffffffffL
                val alpha = (seed % 35 + 8) / 255f
                val radius = (seed % 3 + 1) * 0.6f

                val isMineralLight = (seed % 3) == 0L
                val fleckColor = if (isMineralLight) Color.White.copy(alpha = alpha) else Color(0xFF1E2430).copy(alpha = alpha)

                drawCircle(
                    color = fleckColor,
                    radius = radius,
                    center = Offset(px, py)
                )
            }

            // Volcanic glass specular sweep
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x0CFFFFFF),
                        primaryColor.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(width, height)
                )
            )
        }

        AppTextureStyle.BLUEPRINT_GRID -> {
            drawRect(Color(0xFF090E18))

            val width = size.width
            val height = size.height
            val minorStep = 10.dp.toPx()
            val majorStep = 50.dp.toPx()

            // Minor grid lines
            var x = 0f
            while (x < width) {
                val isMajor = (x % majorStep).toInt() in 0..2
                val color = if (isMajor) primaryColor.copy(alpha = 0.22f) else Color(0x10FFFFFF)
                val stroke = if (isMajor) 1.2f else 0.8f
                drawLine(color, Offset(x, 0f), Offset(x, height), strokeWidth = stroke)
                x += minorStep
            }

            var y = 0f
            while (y < height) {
                val isMajor = (y % majorStep).toInt() in 0..2
                val color = if (isMajor) primaryColor.copy(alpha = 0.22f) else Color(0x10FFFFFF)
                val stroke = if (isMajor) 1.2f else 0.8f
                drawLine(color, Offset(0f, y), Offset(width, y), strokeWidth = stroke)
                y += minorStep
            }

            // Coordinate crosshairs at major intersections
            var mx = 0f
            while (mx < width) {
                var my = 0f
                while (my < height) {
                    val cross = 4.dp.toPx()
                    drawLine(primaryColor.copy(alpha = 0.6f), Offset(mx - cross, my), Offset(mx + cross, my), 1.2f)
                    drawLine(primaryColor.copy(alpha = 0.6f), Offset(mx, my - cross), Offset(mx, my + cross), 1.2f)
                    my += majorStep
                }
                mx += majorStep
            }
        }

        AppTextureStyle.COSMOS_STARDUST -> {
            drawRect(Color(0xFF06070E))

            val width = size.width
            val height = size.height

            // Cosmic nebula dust clouds
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x224A00E0), Color.Transparent),
                    center = Offset(width * 0.25f, height * 0.35f),
                    radius = width * 0.7f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(width * 0.75f, height * 0.65f),
                    radius = width * 0.6f
                )
            )

            // Star field particles
            var seed = 987654321L
            val starCount = 180
            for (i in 0 until starCount) {
                seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
                val sx = (seed % width.toLong()).toFloat()
                seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
                val sy = (seed % height.toLong()).toFloat()
                seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
                val alpha = (seed % 60 + 20) / 100f
                val sRadius = ((seed % 3 + 1) * 0.7f).dp.toPx()

                // Star core and subtle halo
                drawCircle(
                    color = primaryColor.copy(alpha = alpha * 0.4f),
                    radius = sRadius * 2.2f,
                    center = Offset(sx, sy)
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = sRadius,
                    center = Offset(sx, sy)
                )
            }
        }

        AppTextureStyle.FROSTED_GLASS_GRAIN -> {
            // Frosted smoked glass base
            drawRect(Color(0xFF10131B))

            val width = size.width
            val height = size.height

            // Frosted noise grain
            var seed = 5551212L
            val grainCount = (width * height / 200f).toInt().coerceIn(300, 2000)
            for (i in 0 until grainCount) {
                seed = (seed * 214013L + 2531011L) and 0x7fffffffL
                val gx = (seed % width.toLong()).toFloat()
                seed = (seed * 214013L + 2531011L) and 0x7fffffffL
                val gy = (seed % height.toLong()).toFloat()
                seed = (seed * 214013L + 2531011L) and 0x7fffffffL
                val alpha = (seed % 22 + 4) / 255f

                drawRect(
                    color = Color.White.copy(alpha = alpha),
                    topLeft = Offset(gx, gy),
                    size = Size(1.5f, 1.5f)
                )
            }

            // Glass refraction gradient
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x1800F0FF),
                        Color(0x08FFFFFF),
                        Color(0x120088FF),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(width, height)
                )
            )
        }

        AppTextureStyle.NONE -> {
            drawRect(Color(0xFF090A0F))
        }
    }
}
