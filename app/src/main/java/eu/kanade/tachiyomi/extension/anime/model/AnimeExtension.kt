package eu.kanade.tachiyomi.extension.anime.model

import eu.kanade.tachiyomi.animesource.AnimeSource

sealed class AnimeExtension {
    abstract val name: String
    abstract val pkgName: String
    abstract val versionName: String
    abstract val versionCode: Long
    abstract val libVersion: Double
    abstract val lang: String
    abstract val isNsfw: Boolean

    /** Extension APK is installed and its sources were loaded. */
    data class Installed(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val isNsfw: Boolean,
        val sources: List<AnimeSource>,
    ) : AnimeExtension()

    /** Extension APK is installed but its signature is not trusted yet. */
    data class Untrusted(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        val signatureHash: String,
    ) : AnimeExtension() {
        override val lang: String = ""
        override val isNsfw: Boolean = false
    }

    /** Extension listed in a repository, not necessarily installed. */
    data class Available(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val isNsfw: Boolean,
        val apkUrl: String,
        val sourceNames: List<String>,
    ) : AnimeExtension()
}
