package eu.kanade.tachiyomi.extension.anime

import android.content.Context
import android.content.Intent
import android.net.Uri
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.extension.anime.api.AnimeExtensionRepos
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.extension.anime.model.AnimeLoadResult
import eu.kanade.tachiyomi.extension.anime.util.AnimeExtensionLoader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.util.storage.getUriCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

/**
 * Keeps the anime extension state: installed, untrusted and available extensions.
 * Installs and uninstalls go through the system package installer.
 */
object AnimeExtensionManager {

    private val mutex = Mutex()

    private val _installed = MutableStateFlow<List<AnimeExtension.Installed>>(emptyList())
    val installed: StateFlow<List<AnimeExtension.Installed>> = _installed.asStateFlow()

    private val _untrusted = MutableStateFlow<List<AnimeExtension.Untrusted>>(emptyList())
    val untrusted: StateFlow<List<AnimeExtension.Untrusted>> = _untrusted.asStateFlow()

    private val _available = MutableStateFlow<List<AnimeExtension.Available>>(emptyList())
    val available: StateFlow<List<AnimeExtension.Available>> = _available.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** All sources from installed, trusted anime extensions. */
    fun installedSources(): List<AnimeSource> = _installed.value.flatMap { it.sources }

    /**
     * Reloads installed extensions and fetches the repo lists. Runs one refresh at a time.
     */
    suspend fun refresh(context: Context) {
        mutex.withLock {
            _isLoading.value = true
            try {
                val results = withContext(Dispatchers.IO) { AnimeExtensionLoader.loadInstalled(context) }
                _installed.value = results.filterIsInstance<AnimeLoadResult.Success>().map { it.extension }
                _untrusted.value = results.filterIsInstance<AnimeLoadResult.Untrusted>().map { it.extension }
                _available.value = withContext(Dispatchers.IO) { AnimeExtensionRepos.fetchAvailable() }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun trust(extension: AnimeExtension.Untrusted) {
        Injekt.get<TrustExtension>().trust(extension.pkgName, extension.versionCode, extension.signatureHash)
    }

    /**
     * Downloads the APK and opens the system installer for it.
     */
    suspend fun install(context: Context, extension: AnimeExtension.Available) {
        val dir = File(context.cacheDir, "anime_extensions").apply { mkdirs() }
        val file = File(dir, "${extension.pkgName}.apk")

        withContext(Dispatchers.IO) {
            Injekt.get<NetworkHelper>().client.newCall(GET(extension.apkUrl)).awaitSuccess().use { response ->
                file.outputStream().use { out ->
                    response.body.byteStream().copyTo(out)
                }
            }
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(file.getUriCompat(context), "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Opens the system uninstall prompt for the package.
     */
    fun uninstall(context: Context, extension: AnimeExtension.Installed) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${extension.pkgName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
