package eu.kanade.tachiyomi.ui.browse.source

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.BrowseSourceContent
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.collections.immutable.persistentListOf
import mihon.presentation.core.util.collectAsLazyPagingItems
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.LocalSource

@Composable
fun Screen.sourcesTab(selectedSourceId: Long?): TabContent {
    val navigator = LocalNavigator.currentOrThrow

    return TabContent(
        titleRes = MR.strings.label_sources,
        actions = persistentListOf(), 
        content = { contentPadding, snackbarHostState ->
            if (selectedSourceId == null) return@TabContent

            val screenModel = rememberScreenModel(tag = "source_$selectedSourceId") { 
                BrowseSourceScreenModel(sourceId = selectedSourceId, listingQuery = null) 
            }
            val state by screenModel.state.collectAsState()
            
            val mangaList = screenModel.mangaPagerFlowFlow.collectAsLazyPagingItems()
            
            val configuration = LocalConfiguration.current
            val columns = screenModel.getColumnsPreference(configuration.orientation)

            BrowseSourceContent(
                source = screenModel.source,
                mangaList = mangaList,
                columns = columns,
                displayMode = screenModel.displayMode,
                snackbarHostState = snackbarHostState,
                contentPadding = contentPadding,
                onWebViewClick = {
                    val source = screenModel.source as? eu.kanade.tachiyomi.source.online.HttpSource ?: return@BrowseSourceContent
                    navigator.push(WebViewScreen(source.baseUrl, source.name, source.id))
                },
                onHelpClick = { }, // Replace with actual URI handling if needed
                onLocalSourceHelpClick = { },
                onMangaClick = { manga -> 
                    navigator.push(MangaScreen(manga.id, true)) 
                },
                onMangaLongClick = { }
            )
        },
    )
}
