package eu.kanade.domain.ui.model

/** Which engine draws the decorative gradient. CLASSIC is the original radial gradient, unchanged. */
enum class GradientStyle(val displayName: String) {
    CLASSIC("Classic"),
    SHADER("ShaderGradient"),
}

/** Geometry the ShaderGradient presets are drawn on. Codes are passed to the AGSL shader. */
enum class ShaderGradientType(val code: Float) {
    PLANE(0f),
    SPHERE(1f),
    WATER_PLANE(2f),
}

/**
 * The ten presets from shadergradient's presets.ts (ruucm/shadergradient). Colors, speed, strength,
 * density and reflection are copied from there; camera and rotation values are not used by the
 * on-device renderer.
 */
enum class ShaderGradientPreset(
    val displayName: String,
    val type: ShaderGradientType,
    val color1: Int,
    val color2: Int,
    val color3: Int,
    val speed: Float,
    val strength: Float,
    val density: Float,
    val reflection: Float,
) {
    HALO(
        "Halo", ShaderGradientType.PLANE,
        0xFFFF5005.toInt(), 0xFFDBBA95.toInt(), 0xFFD0BCE1.toInt(),
        0.4f, 4f, 1.3f, 0.1f,
    ),
    PENSIVE(
        "Pensive", ShaderGradientType.SPHERE,
        0xFF809BD6.toInt(), 0xFF910AFF.toInt(), 0xFFAF38FF.toInt(),
        0.3f, 0.4f, 0.8f, 0.5f,
    ),
    MINT(
        "Mint", ShaderGradientType.WATER_PLANE,
        0xFF94FFD1.toInt(), 0xFF6BF5FF.toInt(), 0xFFFFFFFF.toInt(),
        0.2f, 3.4f, 1.2f, 0.1f,
    ),
    INTERSTELLA(
        "Interstella", ShaderGradientType.SPHERE,
        0xFF73BFC4.toInt(), 0xFFFF810A.toInt(), 0xFF8DA0CE.toInt(),
        0.3f, 0.3f, 0.8f, 0.4f,
    ),
    NIGHTY_NIGHT(
        "Nighty Night", ShaderGradientType.WATER_PLANE,
        0xFF606080.toInt(), 0xFF8D7DCA.toInt(), 0xFF212121.toInt(),
        0.3f, 1.5f, 1.5f, 0.1f,
    ),
    VIOLA_ORIENTALIS(
        "Viola Orientalis", ShaderGradientType.SPHERE,
        0xFFFFFFFF.toInt(), 0xFFFFBB00.toInt(), 0xFF0700FF.toInt(),
        0.1f, 1f, 1.1f, 0.1f,
    ),
    UNIVERSE(
        "Universe", ShaderGradientType.WATER_PLANE,
        0xFF5606FF.toInt(), 0xFFFE8989.toInt(), 0xFF000000.toInt(),
        0.1f, 2.4f, 1.1f, 0.1f,
    ),
    SUNSET(
        "Sunset", ShaderGradientType.SPHERE,
        0xFFFF7A33.toInt(), 0xFF33A0FF.toInt(), 0xFFFFC53D.toInt(),
        0.1f, 0.4f, 1.1f, 0.1f,
    ),
    MANDARIN(
        "Mandarin", ShaderGradientType.WATER_PLANE,
        0xFFFF6A1A.toInt(), 0xFFC73C00.toInt(), 0xFFFD4912.toInt(),
        0.2f, 3f, 1.8f, 0.1f,
    ),
    COTTON_CANDY(
        "Cotton Candy", ShaderGradientType.WATER_PLANE,
        0xFFEBEDFF.toInt(), 0xFFF3F2F8.toInt(), 0xFFDBF8FF.toInt(),
        0.3f, 3f, 1f, 0.1f,
    ),
}
