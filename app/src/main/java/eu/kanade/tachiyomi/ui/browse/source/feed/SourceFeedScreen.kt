package eu.kanade.tachiyomi.ui.browse.source.feed

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.SourceFeedScreen
import eu.kanade.presentation.browse.components.SourceFeedAddDialog
import eu.kanade.presentation.browse.components.SourceFeedDeleteDialog
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceFilterDialog
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.interactor.GetRemoteManga
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.presentation.core.screens.LoadingScreen

class SourceFeedScreen(val sourceId: Long) : Screen() {
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { SourceFeedScreenModel(sourceId) }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current

        SourceFeedScreen(
            name = screenModel.source.name,
            isLoading = state.isLoading,
            items = state.items,
            hasFilters = state.filters.isNotEmpty(),
            onFabClick = screenModel::openFilterSheet,
            onClickBrowse = { onBrowseClick(navigator, screenModel.source) },
            onClickLatest = { onLatestClick(navigator, screenModel.source) },
            onClickSavedSearch = { onSavedSearchClick(navigator, screenModel.source, it) },
            onClickDelete = screenModel::openDeleteFeed,
            onClickManga = { onMangaClick(navigator, it) },
            onClickSearch = { onSearchClick(navigator, screenModel.source, it) },
            searchQuery = state.searchQuery,
            onSearchQueryChange = screenModel::search,
            getMangaState = { screenModel.getManga(initialManga = it) },
        )

        val onDismissRequest = screenModel::dismissDialog
        when (val dialog = state.dialog) {
            is SourceFeedScreenModel.Dialog.AddFeed -> {
                SourceFeedAddDialog(
                    onDismissRequest = onDismissRequest,
                    name = dialog.name,
                    addFeed = {
                        screenModel.createFeed(dialog.feedId)
                        onDismissRequest()
                    },
                )
            }
            is SourceFeedScreenModel.Dialog.DeleteFeed -> {
                SourceFeedDeleteDialog(
                    onDismissRequest = onDismissRequest,
                    deleteFeed = {
                        screenModel.deleteFeed(dialog.feed)
                        onDismissRequest()
                    },
                )
            }
            SourceFeedScreenModel.Dialog.Filter -> {
                SourceFilterDialog(
                    onDismissRequest = onDismissRequest,
                    filters = state.filters,
                    onReset = {},
                    onFilter = {
                        screenModel.onFilter { query, _ ->
                            onBrowseClick(
                                navigator = navigator,
                                sourceId = sourceId,
                                search = query,
                            )
                        }
                    },
                    onUpdate = screenModel::setFilters,
                )
            }
            null -> Unit
        }

        BackHandler(state.searchQuery != null) {
            screenModel.search(null)
        }
    }

    private fun onMangaClick(navigator: Navigator, manga: Manga) {
        navigator.push(MangaScreen(manga.id, true))
    }

    fun onBrowseClick(navigator: Navigator, sourceId: Long, search: String? = null) {
        navigator.replace(BrowseSourceScreen(sourceId, search))
    }

    private fun onLatestClick(navigator: Navigator, source: Source) {
        navigator.replace(BrowseSourceScreen(source.id, GetRemoteManga.QUERY_LATEST))
    }

    fun onBrowseClick(navigator: Navigator, source: Source) {
        navigator.replace(BrowseSourceScreen(source.id, GetRemoteManga.QUERY_POPULAR))
    }

    private fun onSavedSearchClick(navigator: Navigator, source: Source, savedSearch: SavedSearch) {
        navigator.replace(BrowseSourceScreen(source.id, savedSearch.query))
    }

    private fun onSearchClick(navigator: Navigator, source: Source, query: String) {
        onBrowseClick(navigator, source.id, query.takeIf { it.isNotBlank() })
    }
}
