package com.editog.novaagent.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeColor(val displayName: String, val primaryHex: Long, val accentHex: Long) {
    NEON_RED("Neon Red", 0xFFFF0055, 0xFFFF3377),
    NEON_BLUE("Neon Blue", 0xFF00F0FF, 0xFF0088FF),
    NEON_GREEN("Neon Green", 0xFF00FF66, 0xFF39FF14),
    NEON_YELLOW("Neon Yellow", 0xFFFFEA00, 0xFFFFFA65),
    NEON_ORANGE("Neon Orange", 0xFFFF6600, 0xFFFF9933),
    NEON_WHITE("Neon White", 0xFFFFFFFF, 0xFFCCCCCC),
    NEON_BROWN("Neon Brown", 0xFFD27D2D, 0xFFCD7F32),
    NEON_PURPLE("Neon Purple", 0xFFBF00FF, 0xFFE056FD)
}

@Serializable
enum class AppTextureStyle(val displayName: String, val description: String) {
    CARBON_FIBER("Carbon Fiber [3D Weave]", "Aerospace diagonal twill-weave carbon texture"),
    BRUSHED_TITANIUM("Brushed Titanium", "Anisotropic metallic brushed grain with specular glint"),
    HEXAGON_CYBER_MESH("Hexagon Cyber Mesh", "Honeycomb nano-mesh with glowing node intersections"),
    CIRCUIT_BOARD("Circuit Board [Cyber PCB]", "High-tech motherboard micro-traces and glowing vias"),
    PERFORATED_LEATHER("Perforated Leather", "Luxury perforated automotive leather with matte depth"),
    OBSIDIAN_GRANITE("Obsidian Dark Granite", "Textured volcanic stone with mineral grain flecks"),
    BLUEPRINT_GRID("Blueprint Technical Grid", "Isometric engineering coordinate grid with crosshairs"),
    COSMOS_STARDUST("Cosmos Stardust", "Deep galaxy field with micro stellar dust particles"),
    FROSTED_GLASS_GRAIN("Frosted Smoked Glass", "Tactile frosted glass noise with diffused refraction"),
    NONE("Pure OLED Dark", "Minimal clean pitch dark background")
}

@Serializable
enum class UiDesignStyle(val displayName: String) {
    SOFT_UI("Soft UI"),
    BRUTALISM("Brutalism / Neobrutalism"),
    AERO_GLASSMORPHISM("Aero Glassmorphism"),
    LIQUID_GLASS("Liquid Glass / Liquidmorphism"),
    AURORAMORPHISM("Auroramorphism"),
    CLAYMORPHISM("Claymorphism"),
    SKEUOMORPHISM("Skeuomorphism"),
    GLASSMORPHISM("Glassmorphism"),
    NEUMORPHISM("Neumorphism"),
    FRUTIGER_AERO("Frutiger Aero"),
    Y2K_UI("Y2K UI"),
    CYBERPUNK_UI("Cyberpunk UI"),
    HOLOGRAPHIC_UI("Holographic UI"),
    MATERIAL_DESIGN("Material Design"),
    FLUENT_DESIGN("Fluent Design"),
    METALLICMORPHISM("Metallicmorphism"),
    GLASSMORPHIC_NEUMORPHISM("Glassmorphic Neumorphism"),
    GRADIENTMORPHISM("Gradientmorphism"),
    THREE_D_MORPHISM("3D Morphism"),
    PIXEL_UI("Pixel UI"),
    RETRO_FUTURISTIC("Retro-futuristic UI"),
    PAPER_MORPHISM("Paper/Material Morphism"),
    INFLATED_UI("Inflated UI")
}

@Serializable
enum class TextAnimationStyle(val displayName: String) {
    SOLID("Solid White [Shimmer Sweep]"),
    GRADIENT("Gradient [Electric Shimmer]"),
    AURORA("Aurora [Multicolor Wave Shimmer]"),
    GLOW("Neon Glow [Specular Shimmer]"),
    GLASS("Glass [Neon Blue Shimmer]"),
    METALLIC("Metallic [Chrome/Gold Shimmer]"),
    HOLOGRAPHIC("Holographic [Rainbow Prism Shimmer]"),
    LIQUID("Liquid [Fluid Reflection Shimmer]"),
    THREE_D("3D [Extrusion Depth Shimmer]"),
    OUTLINE("Outline [Traveling Neon Shimmer]")
}

@Serializable
data class AppSettings(
    val themeColor: ThemeColor = ThemeColor.NEON_BLUE,
    val textureStyle: AppTextureStyle = AppTextureStyle.CARBON_FIBER,
    val uiDesign: UiDesignStyle = UiDesignStyle.CYBERPUNK_UI,
    val textAnimationStyle: TextAnimationStyle = TextAnimationStyle.SOLID,
    val customGlowColorHex: Long = 0xFF00F0FF,
    // Dynamic Island settings
    val dynamicIslandEnabled: Boolean = true,
    val dynamicIslandX: Int = 0,
    val dynamicIslandY: Int = 40,
    val dynamicIslandWidth: Int = 220,
    val dynamicIslandHeight: Int = 48,
    val dynamicIslandCornerRadius: Int = 24,
    val dynamicIslandAnimationDurationMs: Int = 350
)
