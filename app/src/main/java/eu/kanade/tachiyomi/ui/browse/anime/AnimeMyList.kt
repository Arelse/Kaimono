package eu.kanade.tachiyomi.ui.browse.anime

import tachiyomi.core.common.preference.PreferenceStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** One anime saved to My List. */
data class AnimeListEntry(
    val sourceId: Long,
    val url: String,
    val title: String,
    val thumbnailUrl: String?,
    val addedAt: Long,
)

/**
 * My List: anime saved from the details screen. Each entry is one encoded string in preferences,
 * because the app has no anime database yet. Title is last in the encoding so it may hold any text.
 */
object AnimeMyList {

    private const val KEY = "pref_anime_my_list"
    private const val SEP = "\u001F"

    private val pref by lazy { Injekt.get<PreferenceStore>().getStringSet(KEY, emptySet()) }

    /** Saved anime, newest first. Unreadable entries are skipped. */
    fun entries(): List<AnimeListEntry> =
        pref.get().mapNotNull(::decode).sortedByDescending { it.addedAt }

    fun contains(sourceId: Long, url: String): Boolean =
        entries().any { it.sourceId == sourceId && it.url == url }

    /** Saves the anime, or removes it when it is already saved. Returns true when now saved. */
    fun toggle(sourceId: Long, url: String, title: String, thumbnailUrl: String?): Boolean {
        val current = pref.get()
        val kept = current.filter { decode(it)?.let { e -> e.sourceId == sourceId && e.url == url } != true }
        if (kept.size != current.size) {
            pref.set(kept.toSet())
            return false
        }
        val entry = encode(
            AnimeListEntry(sourceId, url, title, thumbnailUrl, System.currentTimeMillis()),
        )
        pref.set(current + entry)
        return true
    }

    private fun encode(e: AnimeListEntry): String =
        listOf(e.addedAt, e.sourceId, e.url, e.thumbnailUrl.orEmpty(), e.title).joinToString(SEP)

    private fun decode(raw: String): AnimeListEntry? {
        val parts = raw.split(SEP, limit = 5)
        if (parts.size < 5) return null
        return AnimeListEntry(
            addedAt = parts[0].toLongOrNull() ?: return null,
            sourceId = parts[1].toLongOrNull() ?: return null,
            url = parts[2],
            thumbnailUrl = parts[3].ifEmpty { null },
            title = parts[4],
        )
    }
}
