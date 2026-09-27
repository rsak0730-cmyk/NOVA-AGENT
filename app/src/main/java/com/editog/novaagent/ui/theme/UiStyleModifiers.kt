package com.editog.novaagent.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.editog.novaagent.data.model.UiDesignStyle

fun Modifier.applyUiStyle(
    style: UiDesignStyle,
    accentColor: Color
): Modifier {
    return when (style) {
        UiDesignStyle.SOFT_UI -> this
            .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
            .background(Color(0xFF1E212D), RoundedCornerShape(18.dp))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))

        UiDesignStyle.BRUTALISM -> this
            .border(3.dp, Color.Black, RoundedCornerShape(2.dp))
            .background(Color(0xFFFFF8E7), RoundedCornerShape(2.dp))
            .shadow(4.dp, RoundedCornerShape(2.dp), spotColor = Color.Black)

        UiDesignStyle.AERO_GLASSMORPHISM -> this
            .background(
                Brush.verticalGradient(listOf(Color(0x3300F0FF), Color(0x1A0088FF))),
                RoundedCornerShape(16.dp)
            )
            .border(1.5.dp, Brush.verticalGradient(listOf(Color(0x80FFFFFF), Color(0x2000F0FF))), RoundedCornerShape(16.dp))

        UiDesignStyle.LIQUID_GLASS -> this
            .background(
                Brush.radialGradient(listOf(Color(0x4000F0FF), Color(0x10000000))),
                RoundedCornerShape(22.dp)
            )
            .border(1.5.dp, Color(0x5500F0FF), RoundedCornerShape(22.dp))

        UiDesignStyle.AURORAMORPHISM -> this
            .background(
                Brush.linearGradient(listOf(Color(0x3300FF66), Color(0x3300F0FF), Color(0x33BF00FF))),
                RoundedCornerShape(20.dp)
            )
            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(20.dp))

        UiDesignStyle.CLAYMORPHISM -> this
            .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = accentColor.copy(alpha = 0.3f))
            .background(Color(0xFF25293A), RoundedCornerShape(24.dp))
            .border(2.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))

        UiDesignStyle.SKEUOMORPHISM -> this
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF2A2D3C), Color(0xFF151821))),
                RoundedCornerShape(12.dp)
            )
            .border(1.dp, Color(0xFF3F445B), RoundedCornerShape(12.dp))

        UiDesignStyle.GLASSMORPHISM -> this
            .background(Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(16.dp))

        UiDesignStyle.NEUMORPHISM -> this
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color.Black)
            .background(Color(0xFF161922), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x15FFFFFF), RoundedCornerShape(16.dp))

        UiDesignStyle.FRUTIGER_AERO -> this
            .background(
                Brush.verticalGradient(listOf(Color(0x6600E5FF), Color(0x22007799))),
                RoundedCornerShape(18.dp)
            )
            .border(2.dp, Color(0x88E0F7FA), RoundedCornerShape(18.dp))

        UiDesignStyle.Y2K_UI -> this
            .border(2.dp, Color(0xFFFF007F), RoundedCornerShape(4.dp))
            .background(Color(0xFF0F051D), RoundedCornerShape(4.dp))

        UiDesignStyle.CYBERPUNK_UI -> this
            .border(1.5.dp, accentColor, RoundedCornerShape(6.dp))
            .background(Color(0xE60D0F18), RoundedCornerShape(6.dp))
            .shadow(8.dp, RoundedCornerShape(6.dp), spotColor = accentColor)

        UiDesignStyle.HOLOGRAPHIC_UI -> this
            .background(
                Brush.linearGradient(listOf(Color(0x30FF007F), Color(0x3000F0FF), Color(0x30FFE600))),
                RoundedCornerShape(14.dp)
            )
            .border(1.5.dp, Brush.linearGradient(listOf(Color(0x8000F0FF), Color(0x80FF007F))), RoundedCornerShape(14.dp))

        UiDesignStyle.MATERIAL_DESIGN -> this
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(Color(0xFF1F222E), RoundedCornerShape(12.dp))

        UiDesignStyle.FLUENT_DESIGN -> this
            .background(Color(0x33202533), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))

        UiDesignStyle.METALLICMORPHISM -> this
            .background(
                Brush.linearGradient(listOf(Color(0xFF3A3D40), Color(0xFF181719), Color(0xFF55585B))),
                RoundedCornerShape(12.dp)
            )
            .border(1.dp, Color(0xFF888B90), RoundedCornerShape(12.dp))

        UiDesignStyle.GLASSMORPHIC_NEUMORPHISM -> this
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(Color(0x261A1D28), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x3300F0FF), RoundedCornerShape(16.dp))

        UiDesignStyle.GRADIENTMORPHISM -> this
            .background(
                Brush.horizontalGradient(listOf(Color(0x33FF0055), Color(0x3300F0FF))),
                RoundedCornerShape(16.dp)
            )
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))

        UiDesignStyle.THREE_D_MORPHISM -> this
            .shadow(12.dp, RoundedCornerShape(14.dp), spotColor = accentColor)
            .background(Color(0xFF181A24), RoundedCornerShape(14.dp))
            .border(2.dp, Color(0x40FFFFFF), RoundedCornerShape(14.dp))

        UiDesignStyle.PIXEL_UI -> this
            .border(2.dp, Color(0xFF00FF66), RoundedCornerShape(0.dp))
            .background(Color(0xFF0A0A0A), RoundedCornerShape(0.dp))

        UiDesignStyle.RETRO_FUTURISTIC -> this
            .background(Color(0xFF120C1F), RoundedCornerShape(10.dp))
            .border(2.dp, Color(0xFFFF5500), RoundedCornerShape(10.dp))

        UiDesignStyle.PAPER_MORPHISM -> this
            .shadow(3.dp, RoundedCornerShape(4.dp))
            .background(Color(0xFF1E2028), RoundedCornerShape(4.dp))
            .border(0.5.dp, Color(0x20FFFFFF), RoundedCornerShape(4.dp))

        UiDesignStyle.INFLATED_UI -> this
            .shadow(14.dp, RoundedCornerShape(28.dp), spotColor = accentColor.copy(alpha = 0.5f))
            .background(Color(0xFF222638), RoundedCornerShape(28.dp))
            .border(2.5.dp, Color(0x55FFFFFF), RoundedCornerShape(28.dp))
    }
}
