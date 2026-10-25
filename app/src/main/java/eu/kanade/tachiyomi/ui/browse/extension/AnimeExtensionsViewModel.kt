package eu.kanade.tachiyomi.ui.browse.extension

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.anime.model.AnimeLoadResult
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeExtensionsViewModel : ViewModel() {

    private val app: Application = Injekt.get()

    data class State(
        val searchQuery: String? = null,
        val installed: List<AnimeExtension.Installed> = emptyList(),
        val untrusted: List<AnimeExtension.Untrusted> = emptyList(),
        val available: List<AnimeExtension.Available> = emptyList(),
        val loadErrors: List<AnimeLoadResult.Error> = emptyList(),
        val isLoading: Boolean = false,
    ) {
        /** Available extensions that aren't installed yet, or whose installed version is older. */
        val installedPkgNames: Set<String> get() = installed.map { it.pkgName }.toSet()
    }

    private val query = MutableStateFlow<String?>(null)

    // Loading flag and load errors are combined first to keep the outer combine within five flows.
    private val loadStatus = combine(AnimeExtensionManager.isLoading, AnimeExtensionManager.loadErrors) { loading, errors ->
        loading to errors
    }

    val state: StateFlow<State> = combine(
        query,
        AnimeExtensionManager.installed,
        AnimeExtensionManager.untrusted,
        AnimeExtensionManager.available,
        loadStatus,
    ) { searchQuery, installed, untrusted, available, (isLoading, loadErrors) ->
        val filter: (String) -> Boolean = { name ->
            searchQuery.isNullOrBlank() || name.contains(searchQuery, ignoreCase = true)
        }
        State(
            searchQuery = searchQuery,
            installed = installed.filter { filter(it.name) },
            untrusted = untrusted.filter { filter(it.name) },
            available = available.filter { filter(it.name) },
            loadErrors = loadErrors,
            isLoading = isLoading,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())

    fun search(newQuery: String?) {
        query.value = newQuery
    }

    fun refresh() {
        viewModelScope.launch { AnimeExtensionManager.refresh(app) }
    }

    fun install(extension: AnimeExtension.Available) {
        viewModelScope.launch { AnimeExtensionManager.install(app, extension) }
    }

    fun uninstall(extension: AnimeExtension.Installed) {
        AnimeExtensionManager.uninstall(app, extension)
    }

    fun uninstallUntrusted(extension: AnimeExtension.Untrusted) {
        AnimeExtensionManager.uninstallPackage(app, extension.pkgName)
    }

    fun trust(extension: AnimeExtension.Untrusted) {
        AnimeExtensionManager.trust(extension)
        refresh()
    }
}
