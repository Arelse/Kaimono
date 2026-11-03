package eu.kanade.domain.ui.model

/** Which layout the Home tab uses. DEFAULT is the original Home; MEDIA_HUB is Featured; ATSU_HOME is the new anime-first layout. */
enum class HomeLayout(val displayName: String) {
    DEFAULT("Default"),
    MEDIA_HUB("Featured"),
    ATSU_HOME("Atsu Home"),
}
