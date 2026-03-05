package eu.kanade.tachiyomi.ui.browse

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsScreenModel
import eu.kanade.tachiyomi.ui.browse.extension.extensionsTab
import eu.kanade.tachiyomi.ui.browse.migration.sources.migrateSourceTab
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.browse.source.sourcesTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.i18n.stringResource


data object BrowseTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 3u,
                title = stringResource(MR.strings.browse),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(GlobalSearchScreen())
    }

    private val switchToExtensionTabChannel = Channel<Unit>(1, BufferOverflow.DROP_OLDEST)

    fun showExtension() {
        switchToExtensionTabChannel.trySend(Unit)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        // Create screen models for Extension tab
        val extensionsScreenModel = rememberScreenModel { ExtensionsScreenModel() }

        // Build the tabs
        val sourcesTabContent = sourcesTab()
        val extensionsTabContent = extensionsTab(extensionsScreenModel)
        val migrateTabContent = migrateSourceTab()

        val tabs = persistentListOf(sourcesTabContent, extensionsTabContent, migrateTabContent)
        val pagerState = rememberPagerState { tabs.size }
        val snackbarHostState = remember { SnackbarHostState() }

        // Handle extension search query
        val searchQuery = extensionsScreenModel.state.let {
            @Suppress("StateFlowValueCalledInComposition")
            it.value.searchQuery
        }

        Scaffold(
            topBar = {
                val tab = tabs[pagerState.currentPage]
                val searchEnabled = tab.searchEnabled

                SearchToolbar(
                    titleContent = { AppBarTitle(stringResource(MR.strings.browse)) },
                    searchEnabled = searchEnabled,
                    searchQuery = if (searchEnabled) searchQuery else null,
                    onChangeSearchQuery = { extensionsScreenModel.search(it) },
                    actions = { AppBarActions(tab.actions) },
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { contentPadding ->
            Column(
                modifier = Modifier.padding(
                    top = contentPadding.calculateTopPadding(),
                    start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                ),
            ) {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.zIndex(1f),
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                TabText(
                                    text = stringResource(tab.titleRes),
                                    badgeCount = tab.badgeNumber,
                                )
                            },
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                HorizontalPager(
                    modifier = Modifier.fillMaxSize(),
                    state = pagerState,
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    tabs[page].content(
                        PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                        snackbarHostState,
                    )
                }
            }
        }

        // Listen for requests to switch to the Extensions tab
        LaunchedEffect(Unit) {
            switchToExtensionTabChannel.receiveAsFlow().collectLatest {
                pagerState.animateScrollToPage(1) // Extensions tab is index 1
            }
        }

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}
