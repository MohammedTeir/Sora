package eu.kanade.tachiyomi.ui.search

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SearchLandingScreenModel(
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
) : StateScreenModel<SearchLandingScreenModel.State>(State()) {

    val trendingSearches: List<TrendingItem> = listOf(
        TrendingItem(rank = 1, title = "One Piece", subtitle = "Action · Adventure"),
        TrendingItem(rank = 2, title = "Jujutsu Kaisen", subtitle = "Action · Supernatural"),
        TrendingItem(rank = 3, title = "Demon Slayer", subtitle = "Action · Fantasy"),
        TrendingItem(rank = 4, title = "Chainsaw Man", subtitle = "Action · Horror"),
        TrendingItem(rank = 5, title = "My Hero Academia", subtitle = "Action · Sci-Fi"),
        TrendingItem(rank = 6, title = "Attack on Titan", subtitle = "Action · Drama"),
        TrendingItem(rank = 7, title = "Bleach", subtitle = "Action · Supernatural"),
    )

    init {
        screenModelScope.launch {
            getLibraryManga.subscribe().collect { libraryManga ->
                mutableState.update { state ->
                    state.copy(
                        suggestedManga = libraryManga
                            .sortedByDescending { it.lastRead }
                            .take(9)
                            .map { it.manga }
                            .toImmutableList(),
                    )
                }
            }
        }
    }

    fun addRecentSearch(query: String) {
        if (query.isBlank()) return
        mutableState.update { state ->
            val updated = (listOf(query) + state.recentSearches.filter { it != query }).take(8)
            state.copy(recentSearches = updated.toImmutableList())
        }
    }

    fun removeRecentSearch(query: String) {
        mutableState.update { state ->
            state.copy(recentSearches = state.recentSearches.filter { it != query }.toImmutableList())
        }
    }

    fun clearRecentSearches() {
        mutableState.update { it.copy(recentSearches = persistentListOf()) }
    }

    data class State(
        val recentSearches: ImmutableList<String> = persistentListOf(),
        val suggestedManga: ImmutableList<Manga> = persistentListOf(),
    )

    data class TrendingItem(val rank: Int, val title: String, val subtitle: String)
}
