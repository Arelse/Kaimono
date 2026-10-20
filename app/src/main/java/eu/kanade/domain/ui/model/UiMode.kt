package eu.kanade.domain.ui.model

/**
 * The three app-wide UI modes selectable from the "UI Switcher" entry on the More screen.
 * All three currently share the same underlying palette (IosGlassColorScheme) - they're
 * differentiated by surface translucency/treatment, not by accent color, per the original
 * design brief (both source mockups use an identical black + #0A84FF palette).
 */
enum class UiMode(val displayName: String, val description: String) {
    DEFAULT(
        displayName = "Default",
        description = "iOS 26 Vision Pro mockup: monochrome glass cards, tab bar with a glass dock.",
    ),
    ALTERNATIVE(
        displayName = "Vision Glass",
        description = "iOS 26 Refined mockup: monochrome glass cards with outlined icons and a tab bar.",
    ),
    TRANSLUCENT(
        displayName = "Translucent",
        description = "Expands the glass effect across the whole app, not just cards.",
    ),
    GLASSMORPHISM(
        displayName = "Atsu UI",
        description = "See-through frosted surfaces everywhere, via the color scheme itself - not just a few screens.",
    ),
    NEUMORPHISM(
        displayName = "Neumorphism",
        description = "Soft, opaque extruded surfaces that nearly match the background - low contrast, shadow-driven depth, not translucency.",
    ),
    LIQUID_GLASS(
        displayName = "Liquid Glass",
        description = "Real-time refraction/blur shader ported from the liquid-glass-js WebGL " +
            "project (same edge/rim/base distortion math, Gaussian blur and gradient tint) on " +
            "Android 13+; devices below that fall back to a tinted glass card with an animated " +
            "specular sweep.",
    ),
}
