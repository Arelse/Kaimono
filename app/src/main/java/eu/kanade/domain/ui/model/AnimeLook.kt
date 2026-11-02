package eu.kanade.domain.ui.model

/** The look of the anime screens: home, details and episode lists. Chosen in Settings > UI. */
enum class AnimeLook(val label: String, val description: String) {
    CINEMATIC("Cinematic dark", "Near-black, warm amber accent, large posters, edge-to-edge hero"),
    FROSTED("Frosted glass", "Blurred backdrops behind glass cards, cool blue accent"),
    EDITORIAL("Editorial light", "Light paper background, ink text, a single red accent"),
    EMBER("Ember", "Deep red-brown, rounded pills, bold coloured badges"),
    ;

    companion object {
        fun fromName(name: String): AnimeLook = entries.firstOrNull { it.name == name } ?: CINEMATIC
    }
}
