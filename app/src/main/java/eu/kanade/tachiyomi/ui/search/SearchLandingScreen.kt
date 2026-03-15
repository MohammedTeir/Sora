package eu.kanade.tachiyomi.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.search.SearchLandingPresentation
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen

class SearchLandingScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { SearchLandingScreenModel() }
        val state by screenModel.state.collectAsState()

        SearchLandingPresentation(
            state = state,
            trendingSearches = screenModel.trendingSearches,
            onSearchSubmit = { query ->
                screenModel.addRecentSearch(query)
                navigator.push(GlobalSearchScreen(query))
            },
            onClickRecent = { query ->
                screenModel.addRecentSearch(query)
                navigator.push(GlobalSearchScreen(query))
            },
            onRemoveRecent = screenModel::removeRecentSearch,
            onClearRecent = screenModel::clearRecentSearches,
            onClickTrending = { trending ->
                screenModel.addRecentSearch(trending.title)
                navigator.push(GlobalSearchScreen(trending.title))
            },
            onClickManga = { manga -> navigator.push(MangaScreen(manga.id, true)) },
            navigateUp = navigator::pop,
        )
    }
}
