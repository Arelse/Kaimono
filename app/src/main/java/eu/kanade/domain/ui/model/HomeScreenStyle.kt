package eu.kanade.domain.ui.model

import androidx.compose.ui.graphics.Color

/**
 * Visual tokens for one Home Screen Switcher style. Consumed by [DiscoverTab]
 * (eu.kanade.tachiyomi.ui.discover) for the Home tab's own content, and by
 * [HomeScreen]'s floating pill navigation bar (eu.kanade.tachiyomi.ui.home) so that
 * switching styles re-skins the nav buttons too, not just the Home tab body.
 *
 * @property background Page/scaffold background behind the whole Home tab.
 * @property surface Card / nav-bar container color before alpha is applied.
 * @property accent Primary accent: buttons, selected nav item, progress fill, glow.
 * @property accentSecondary Second gradient stop. Equal to [accent] for solid (non-gradient) styles.
 * @property onAccent Content color to use on top of a solid [accent] fill.
 * @property cardGlass Whether cards/badges render as frosted glass (translucent + border)
 * instead of solid opaque panels.
 * @property ambientGlow Whether soft, oversized color blobs are blurred behind the content
 * for an ambient-light effect.
 * @property heroButtonGlass Whether the hero "Start Reading" button is a translucent glass
 * pill (bordered, tinted icon) instead of a solid filled button.
 * @property progressGradient Whether progress bars / selected pagination dots use an
 * [accent]-to-[accentSecondary] gradient instead of a flat [accent] fill.
 * @property navOpaqueAlpha Nav bar container alpha when the user's "translucent nav" setting
 * is OFF (nav should still read as a mostly-solid bar).
 * @property navTranslucentAlpha Nav bar container alpha when "translucent nav" is ON.
 * @property navActivePillAlpha Alpha of the selected tab's own highlight pill/background.
 * @property borderAlpha Alpha used for the thin hairline borders glass styles draw around
 * cards, buttons and the nav bar.
 */
data class HomeStyleTokens(
    val background: Color,
    val surface: Color,
    val accent: Color,
    val accentSecondary: Color,
    val onAccent: Color,
    val cardGlass: Boolean,
    val ambientGlow: Boolean,
    val heroButtonGlass: Boolean,
    val progressGradient: Boolean,
    val navOpaqueAlpha: Float,
    val navTranslucentAlpha: Float,
    val navActivePillAlpha: Float,
    val borderAlpha: Float,
)

/**
 * The Home Screen Switcher's selectable styles. [CLASSIC] means "no override" - the Home tab
 * and nav bar keep using the app's normal Material theme colors, exactly as before this
 * feature existed. The other five are independent, fully-specified looks; picking one of them
 * re-skins the Home tab body (header, hero banner, section chips) and the floating bottom nav
 * at the same time, since both read this same preference.
 */
enum class HomeScreenStyle(
    val displayName: String,
    val description: String,
    val tokens: HomeStyleTokens,
) {
    /** No override - use the app's own Material theme, unchanged. */
    CLASSIC(
        displayName = "Classic",
        description = "Use the app's normal theme",
        tokens = HomeStyleTokens(
            background = Color(0xFF07080C),
            surface = Color(0xFF11131A),
            accent = Color(0xFF9D4EDD),
            accentSecondary = Color(0xFF9D4EDD),
            onAccent = Color.White,
            cardGlass = false,
            ambientGlow = false,
            heroButtonGlass = false,
            progressGradient = false,
            navOpaqueAlpha = 1f,
            navTranslucentAlpha = 0.3f,
            navActivePillAlpha = 1f,
            borderAlpha = 0.1f,
        ),
    ),

    /** Solid deep-navy background, bold violet glow, opaque glass nav. */
    AURORA_VIOLET(
        displayName = "Aurora Violet",
        description = "Deep navy with a bold violet glow",
        tokens = HomeStyleTokens(
            background = Color(0xFF07080C),
            surface = Color(0xFF1E1B2E),
            accent = Color(0xFF9D4EDD),
            accentSecondary = Color(0xFF9D4EDD),
            onAccent = Color.White,
            cardGlass = false,
            ambientGlow = false,
            heroButtonGlass = false,
            progressGradient = false,
            navOpaqueAlpha = 0.9f,
            navTranslucentAlpha = 0.45f,
            navActivePillAlpha = 0.2f,
            borderAlpha = 0.1f,
        ),
    ),

    /** Black background with ambient purple/blue glow blobs and frosted glass cards. */
    NEBULA_GLASS(
        displayName = "Nebula Glass",
        description = "Ambient glow with frosted glass cards",
        tokens = HomeStyleTokens(
            background = Color(0xFF0A0A0F),
            surface = Color(0xFF121212),
            accent = Color(0xFFA855F7),
            accentSecondary = Color(0xFF3B82F6),
            onAccent = Color.White,
            cardGlass = true,
            ambientGlow = true,
            heroButtonGlass = true,
            progressGradient = false,
            navOpaqueAlpha = 0.95f,
            navTranslucentAlpha = 0.35f,
            navActivePillAlpha = 0.12f,
            borderAlpha = 0.15f,
        ),
    ),

    /** Nebula Glass's palette with stronger specular highlights and a pink-violet gradient. */
    PRISM_AURORA(
        displayName = "Prism Aurora",
        description = "Specular glass with a pink-violet gradient",
        tokens = HomeStyleTokens(
            background = Color(0xFF0A0A0F),
            surface = Color(0xFF121212),
            accent = Color(0xFFA855F7),
            accentSecondary = Color(0xFFEC4899),
            onAccent = Color.White,
            cardGlass = true,
            ambientGlow = true,
            heroButtonGlass = true,
            progressGradient = true,
            navOpaqueAlpha = 0.95f,
            navTranslucentAlpha = 0.35f,
            navActivePillAlpha = 0.15f,
            borderAlpha = 0.25f,
        ),
    ),

    /** Matte black panels, no blur, indigo accent - the restrained, high-contrast look. */
    MIDNIGHT_INDIGO(
        displayName = "Midnight Indigo",
        description = "Matte black panels with an indigo accent",
        tokens = HomeStyleTokens(
            background = Color(0xFF000000),
            surface = Color(0xFF111111),
            accent = Color(0xFF4F46E5),
            accentSecondary = Color(0xFF4F46E5),
            onAccent = Color.White,
            cardGlass = false,
            ambientGlow = false,
            heroButtonGlass = false,
            progressGradient = false,
            navOpaqueAlpha = 0.95f,
            navTranslucentAlpha = 0.5f,
            navActivePillAlpha = 1f,
            borderAlpha = 0.1f,
        ),
    ),

    /** Muted violet on near-black "space" tones, generous spacing, the quietest of the five. */
    ECLIPSE_NOIR(
        displayName = "Eclipse Noir",
        description = "Muted violet on near-black space tones",
        tokens = HomeStyleTokens(
            background = Color(0xFF030303),
            surface = Color(0xFF121214),
            accent = Color(0xFF7C5FBA),
            accentSecondary = Color(0xFF7C5FBA),
            onAccent = Color.White,
            cardGlass = true,
            ambientGlow = false,
            heroButtonGlass = true,
            progressGradient = false,
            navOpaqueAlpha = 0.95f,
            navTranslucentAlpha = 0.25f,
            navActivePillAlpha = 0.08f,
            borderAlpha = 0.1f,
        ),
    ),
}
