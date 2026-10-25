package eu.kanade.tachiyomi.extension.anime.model

sealed class AnimeLoadResult {
    data class Success(val extension: AnimeExtension.Installed) : AnimeLoadResult()

    data class Untrusted(val extension: AnimeExtension.Untrusted) : AnimeLoadResult()

    /** A package that could not be loaded, with the reason shown in the Anime Extensions tab. */
    data class Error(val pkgName: String, val reason: String) : AnimeLoadResult()
}
