package eu.kanade.tachiyomi.extension.anime.api

import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logcat.LogPriority
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Anime extension repositories. A repo can be pasted either as a base URL (the app then reads
 * `index.min.json` from it) or as a direct link to its index file (`.json`). APKs are read from
 * an `apk/` folder next to the index file.
 */
object AnimeExtensionRepos {

    private const val PREF_KEY = "anime_extension_repos"
    private const val DISABLED_KEY = "anime_extension_repos_disabled"
    private const val FETCH_TIMEOUT_MS = 20_000L

    private val json = Json { ignoreUnknownKeys = true }

    private val preferenceStore: PreferenceStore by lazy { Injekt.get<PreferenceStore>() }

    private val reposPref by lazy { preferenceStore.getStringSet(PREF_KEY, emptySet()) }

    private val disabledPref by lazy { preferenceStore.getStringSet(DISABLED_KEY, emptySet()) }

    fun getRepos(): List<String> = reposPref.get().sorted()

    fun isEnabled(url: String): Boolean = url !in disabledPref.get()

    fun setEnabled(url: String, enabled: Boolean) {
        val disabled = disabledPref.get()
        disabledPref.set(if (enabled) disabled - url else disabled + url)
    }

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
        disabledPref.set(disabledPref.get() - url)
    }

    /**
     * Short label for the Repos list, e.g. "Secozzi/aniyomi-extensions" for a GitHub raw link.
     * Falls back to the full URL when there is no owner/name pair in the path.
     */
    fun displayName(url: String): String {
        val path = url.substringAfter("://").substringAfter('/', "")
        val parts = path.split('/').filter { it.isNotBlank() }
        return if (parts.size >= 2) "${parts[0]}/${parts[1]}" else url
    }

    private fun normalize(rawUrl: String): String? {
        val trimmed = rawUrl.trim().trimEnd('/')
        val isHttp = trimmed.startsWith("https://") || trimmed.startsWith("http://")
        return trimmed.takeIf { isHttp && it.length > "https://".length }
    }

    /** A pasted index link is used as-is; a base URL gets `index.min.json` appended. */
    private fun indexUrlOf(repo: String): String {
        return if (repo.endsWith(".json") || repo.endsWith(".pb")) repo else "$repo/index.min.json"
    }

    /**
     * Fetches the extension list from every enabled repo in parallel. A repo that fails or
     * takes longer than [FETCH_TIMEOUT_MS] is skipped, so one bad repo never blocks the rest.
     */
    suspend fun fetchAvailable(): List<AnimeExtension.Available> = coroutineScope {
        getRepos()
            .filter { isEnabled(it) }
            .map { repo ->
                async {
                    withTimeoutOrNull(FETCH_TIMEOUT_MS) { fetchRepo(repo) } ?: run {
                        logcat(LogPriority.WARN) { "Anime repo timed out: $repo" }
                        emptyList()
                    }
                }
            }
            .awaitAll()
            .flatten()
    }

    private suspend fun fetchRepo(repo: String): List<AnimeExtension.Available> {
        val indexUrl = indexUrlOf(repo)
        if (indexUrl.endsWith(".pb")) {
            logcat(LogPriority.WARN) { "Anime repo uses a protobuf index, which is not supported yet: $indexUrl" }
            return emptyList()
        }
        val apkBase = indexUrl.substringBeforeLast('/')

        return try {
            val client = Injekt.get<NetworkHelper>().client
            val body = client.newCall(GET(indexUrl)).awaitSuccess().use { it.body.string() }
            json.decodeFromString<List<RepoIndexEntry>>(body).map { entry ->
                AnimeExtension.Available(
                    name = entry.name.substringAfter("Aniyomi: "),
                    pkgName = entry.pkg,
                    versionName = entry.version,
                    versionCode = entry.code,
                    libVersion = entry.version.substringBeforeLast('.').toDoubleOrNull() ?: 0.0,
                    lang = entry.lang,
                    isNsfw = entry.nsfw == 1,
                    apkUrl = "$apkBase/apk/${entry.apk}",
                    sourceNames = entry.sources.map { it.name },
                    iconUrl = entry.icon.takeIf { it.isNotBlank() }?.let { "$apkBase/icon/$it" },
                )
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Failed to load anime repo $indexUrl" }
            emptyList()
        }
    }

    @Serializable
    private data class RepoIndexEntry(
        val name: String,
        val pkg: String,
        val apk: String,
        val icon: String = "",
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
