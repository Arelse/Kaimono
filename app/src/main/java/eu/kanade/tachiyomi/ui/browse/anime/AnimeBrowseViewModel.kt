package eu.kanade.tachiyomi.ui.browse.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Browses one installed anime source: its popular list, latest updates, or a search query. */
class AnimeBrowseViewModel(
    val sourceId: Long,
    startLatest: Boolean,
) : ViewModel() {

    val source: AnimeCatalogueSource? = AnimeExtensionManager.installedSources()
        .filterIsInstance<AnimeCatalogueSource>()
        .firstOrNull { it.id == sourceId }

    enum class Mode { POPULAR, LATEST, SEARCH }

    data class State(
        val mode: Mode = Mode.POPULAR,
        val query: String = "",
        val animes: List<SAnime> = emptyList(),
        val page: Int = 0,
        val hasNextPage: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State(mode = if (startLatest) Mode.LATEST else Mode.POPULAR))
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        load(page = 1)
    }

    fun showPopular() = restart(Mode.POPULAR, query = "")

    fun showLatest() = restart(Mode.LATEST, query = "")

    fun search(query: String) {
        if (query.isBlank()) showPopular() else restart(Mode.SEARCH, query = query)
    }

    fun loadNextPage() {
        val current = _state.value
        if (current.hasNextPage && !current.isLoading) load(page = current.page + 1)
    }

    private fun restart(mode: Mode, query: String) {
        _state.update { it.copy(mode = mode, query = query, animes = emptyList(), page = 0, hasNextPage = false) }
        load(page = 1)
    }

    private fun load(page: Int) {
        val src = source
        if (src == null) {
            _state.update { it.copy(isLoading = false, error = "This source is no longer installed") }
            return
        }
        val request = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    when (request.mode) {
                        Mode.POPULAR -> src.getPopularAnime(page)
                        Mode.LATEST -> src.getLatestUpdates(page)
                        Mode.SEARCH -> src.getSearchAnime(page, request.query, src.getFilterList())
                    }
                }
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { pageResult ->
                        current.copy(
                            animes = if (page == 1) pageResult.animes else current.animes + pageResult.animes,
                            page = page,
                            hasNextPage = pageResult.hasNextPage,
                            isLoading = false,
                        )
                    },
                    onFailure = { e ->
                        current.copy(isLoading = false, error = e.message ?: "Failed to load anime")
                    },
                )
            }
        }
    }
}
