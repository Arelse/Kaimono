package eu.kanade.tachiyomi.extension.anime.api

import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logcat.LogPriority
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Anime extension repositories. Each repo is a base URL that serves an
 * `index.min.json` file listing its extensions, with APKs under `apk/`.
 */
object AnimeExtensionRepos {

    private const val PREF_KEY = "anime_extension_repos"

    private val json = Json { ignoreUnknownKeys = true }

    private val preferenceStore: PreferenceStore by lazy { Injekt.get<PreferenceStore>() }

    private val reposPref by lazy { preferenceStore.getStringSet(PREF_KEY, emptySet()) }

    fun getRepos(): List<String> = reposPref.get().sorted()

    /**
     * Adds a repo. Returns false if the URL is not a valid http(s) URL.
     */
    fun addRepo(rawUrl: String): Boolean {
        val url = normalize(rawUrl) ?: return false
        reposPref.set(reposPref.get() + url)
        return true
    }

    fun removeRepo(url: String) {
        reposPref.set(reposPref.get() - url)
    }

    private fun normalize(rawUrl: String): String? {
        val trimmed = rawUrl.trim().trimEnd('/')
        val isHttp = trimmed.startsWith("https://") || trimmed.startsWith("http://")
        return trimmed.takeIf { isHttp && it.length > "https://".length }
    }

    /**
     * Fetches the extension list from every saved repo. A repo that fails is skipped.
     */
    suspend fun fetchAvailable(): List<AnimeExtension.Available> {
        val client = Injekt.get<NetworkHelper>().client
        return getRepos().flatMap { repo ->
            try {
                val body = client.newCall(GET("$repo/index.min.json")).awaitSuccess().use { it.body.string() }
                json.decodeFromString<List<RepoIndexEntry>>(body).map { entry ->
                    AnimeExtension.Available(
                        name = entry.name.substringAfter("Aniyomi: "),
                        pkgName = entry.pkg,
                        versionName = entry.version,
                        versionCode = entry.code,
                        libVersion = entry.version.substringBeforeLast('.').toDoubleOrNull() ?: 0.0,
                        lang = entry.lang,
                        isNsfw = entry.nsfw == 1,
                        apkUrl = "$repo/apk/${entry.apk}",
                        sourceNames = entry.sources.map { it.name },
                    )
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Failed to load anime repo $repo" }
                emptyList()
            }
        }
    }

    @Serializable
    private data class RepoIndexEntry(
        val name: String,
        val pkg: String,
        val apk: String,
        val lang: String = "",
        val code: Long = 0,
        val version: String = "",
        val nsfw: Int = 0,
        val sources: List<RepoSource> = emptyList(),
    )

    @Serializable
    private data class RepoSource(
        val name: String = "",
        val lang: String = "",
        val id: Long = 0,
        val baseUrl: String = "",
    )
}
