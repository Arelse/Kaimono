package eu.kanade.tachiyomi.ui.home

/**
 * Stable string keys for each bottom-nav tab, used by the tab reorder / hide preferences.
 * Kept separate from HomeScreen so the settings screen can use them too.
 */
object NavTabKeys {
    const val HOME = "home"
    const val NOVELS = "novels"
    const val LIBRARY = "library"
    const val HISTORY = "history"
    const val BROWSE = "browse"
    const val MORE = "more"

    fun defaultKeys(joined: Boolean): List<String> = if (joined) {
        listOf(HOME, NOVELS, HISTORY, BROWSE, MORE)
    } else {
        listOf(HOME, NOVELS, LIBRARY, HISTORY, BROWSE, MORE)
    }

    /** Parses the saved order string, keeping only known keys and appending any missing ones. */
    fun ordered(defaultKeys: List<String>, savedOrder: String): List<String> {
        val saved = savedOrder.split(",").map { it.trim() }.filter { it in defaultKeys }
        return saved + defaultKeys.filterNot { it in saved }
    }

    fun label(key: String, joined: Boolean): String = when (key) {
        HOME -> "Home"
        NOVELS -> if (joined) "Library" else "Novels"
        LIBRARY -> "Manga"
        HISTORY -> "History"
        BROWSE -> "Browse"
        MORE -> "Settings"
        else -> key
    }
}
