package eu.kanade.tachiyomi.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import eu.kanade.presentation.more.settings.screen.browse.ExtensionReposScreen
import eu.kanade.tachiyomi.ui.browse.source.SourcesFilterScreen
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreenModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import eu.kanade.presentation.browse.SourceUiModel
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsScreenModel
import eu.kanade.tachiyomi.ui.browse.extension.extensionsTab
import eu.kanade.tachiyomi.ui.browse.feed.FeedScreenModel
import eu.kanade.tachiyomi.ui.browse.feed.feedTab
import eu.kanade.tachiyomi.ui.browse.migration.sources.MigrateSourceScreenModel
import eu.kanade.tachiyomi.ui.browse.migration.sources.migrateSourceTab
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.browse.source.sourcesTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.i18n.stringResource

private val SoraBlue = Color(0xFF3B82F6)

data object BrowseTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            return TabOptions(
                index = 2u,
                title = stringResource(MR.strings.browse),
                icon = rememberVectorPainter(Icons.Outlined.Book),
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
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val layoutDirection = LocalLayoutDirection.current

        val extensionsScreenModel = rememberScreenModel { ExtensionsScreenModel() }
        val sourcesScreenModel = rememberScreenModel { SourcesScreenModel() }
        val feedScreenModel = rememberScreenModel { FeedScreenModel() }
        val migrateScreenModel = rememberScreenModel { MigrateSourceScreenModel() }
        val feedTabContent = feedTab()
        val sourcesTabContent = sourcesTab()
        val extensionsTabContent = extensionsTab(extensionsScreenModel)
        val migrateTabContent = migrateSourceTab()
        val tabs = persistentListOf(feedTabContent, sourcesTabContent, extensionsTabContent, migrateTabContent)
        val pagerState = rememberPagerState { tabs.size }
        val snackbarHostState = remember { SnackbarHostState() }
        var extensionsSearchQuery by remember { mutableStateOf("") }

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = contentPadding.calculateTopPadding() +
                            WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                        start = contentPadding.calculateStartPadding(layoutDirection),
                        end = contentPadding.calculateEndPadding(layoutDirection),
                    ),
            ) {
                // ─── Header ────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Browse",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Action button — icon and behavior change per tab
                        IconButton(
                            onClick = {
                                when (pagerState.currentPage) {
                                    0 -> feedScreenModel.openAddDialog() // Feed: add feed source
                                    1 -> navigator.push(SourcesFilterScreen()) // Sources: manage sources
                                    2 -> navigator.push(ExtensionReposScreen()) // Extensions: add repo
                                    3 -> navigator.push(GlobalSearchScreen()) // Migrate: search to migrate
                                    else -> navigator.push(GlobalSearchScreen())
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = when (pagerState.currentPage) {
                                    0 -> Icons.Outlined.Add        // Feed: Add Feed
                                    1 -> Icons.Outlined.Tune       // Sources: Manage Sources
                                    2 -> Icons.Filled.AddBox        // Extensions: Add Repository
                                    else -> Icons.Outlined.FilterList
                                },
                                contentDescription = when (pagerState.currentPage) {
                                    0 -> "Add Feed"
                                    1 -> "Manage Sources"
                                    2 -> "Add Repository"
                                    3 -> "Filter manga for migration"
                                    else -> stringResource(MR.strings.action_filter)
                                },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        // Refresh button — replaces MoreVert, functional per tab
                        IconButton(
                            onClick = {
                                scope.launch {
                                    when (pagerState.currentPage) {
                                        0 -> feedScreenModel.init()
                                        1 -> sourcesScreenModel.refresh()
                                        2 -> extensionsScreenModel.findAvailableExtensions()
                                        3 -> migrateScreenModel.refresh()
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = when (pagerState.currentPage) {
                                    1 -> "Refresh sources"
                                    2 -> "Refresh extensions"
                                    3 -> "Refresh migration list"
                                    else -> "Refresh"
                                },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // ─── Tabs ──────────────────────────────────────────────────
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f),
                    containerColor = Color.Transparent,
                    contentColor = SoraBlue,
                    divider = {}
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
                            selectedContentColor = SoraBlue,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ─── Search Bar ────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(24.dp),
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .then(
                            if (pagerState.currentPage != 2) {
                                Modifier.clickable {
                                    when (pagerState.currentPage) {
                                        1 -> navigator.push(GlobalSearchScreen())
                                        3 -> navigator.push(GlobalSearchScreen())
                                        else -> navigator.push(GlobalSearchScreen())
                                    }
                                }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    if (pagerState.currentPage == 2) {
                        BasicTextField(
                            value = extensionsSearchQuery,
                            onValueChange = { query ->
                                extensionsSearchQuery = query
                                extensionsScreenModel.search(query.ifEmpty { null })
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                            ),
                            decorationBox = { innerTextField ->
                                if (extensionsSearchQuery.isEmpty()) {
                                    Text(
                                        text = "Search extensions...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                    )
                                }
                                innerTextField()
                            },
                        )
                    } else {
                        Text(
                            text = when (pagerState.currentPage) {
                                1 -> "Search sources..."
                                3 -> "Search library to migrate..."
                                else -> "Search popular manga..."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ─── Content Pager ─────────────────────────────────────────
                HorizontalPager(
                    modifier = Modifier.weight(1f).fillMaxSize(),
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

        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage != 2 && extensionsSearchQuery.isNotEmpty()) {
                extensionsSearchQuery = ""
                extensionsScreenModel.search(null)
            }
        }

        LaunchedEffect(Unit) {
            switchToExtensionTabChannel.receiveAsFlow().collectLatest {
                pagerState.animateScrollToPage(2)
            }
        }
    }
}
