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
        description = "Monochrome glass - matches the reference design as-is.",
    ),
    ALTERNATIVE(
        displayName = "Vision Glass",
        description = "Same palette, denser glass cards and the reader button-size control.",
    ),
    TRANSLUCENT(
        displayName = "Translucent",
        description = "Expands the glass effect across the whole app, not just cards.",
    ),
}
