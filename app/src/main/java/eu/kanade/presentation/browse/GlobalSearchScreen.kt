package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import eu.kanade.presentation.browse.components.GlobalSearchCardRow
import eu.kanade.presentation.browse.components.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.components.GlobalSearchResultItem
import eu.kanade.presentation.browse.components.GlobalSearchToolbar
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchScreenModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.material.Scaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import tachiyomi.presentation.core.util.runOnEnterKeyPressed

@Composable
fun GlobalSearchScreen(
    state: SearchScreenModel.State,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    onClickRecent: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
    onClickTrending: (SearchScreenModel.TrendingItem) -> Unit,
) {
    val backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background
    Scaffold(
        topBar = { scrollBehavior ->
            CustomSearchHeader(
                searchQuery = state.searchQuery,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = { query ->
                    onSearch(query)
                },
                navigateUp = navigateUp
            )
        },
        containerColor = backgroundColor,
    ) { paddingValues ->
        if (state.searchQuery.isNullOrBlank()) {
            CustomSearchIdleContent(
                contentPadding = paddingValues,
                recentSearches = state.recentSearches,
                suggestedManga = state.suggestedManga,
                trendingSearches = state.trendingSearches,
                onClickRecent = onClickRecent,
                onClearRecent = onClearRecent,
                onClickTrending = onClickTrending,
                onClickSuggested = onClickItem,
            )
        } else {
            Box(modifier = Modifier.background(backgroundColor).fillMaxSize()) {
                GlobalSearchContent(
                    items = state.filteredItems,
                    contentPadding = paddingValues,
                    getManga = getManga,
                    onClickSource = onClickSource,
                    onClickItem = onClickItem,
                    onLongClickItem = onLongClickItem,
                )
            }
        }
    }
}

@Composable
fun CustomSearchHeader(
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    navigateUp: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val bgColor = androidx.compose.material3.MaterialTheme.colorScheme.background
    val onBgColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
    val surfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurfaceVariantColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    ) {
        // Title row (no profile icon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Search",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = onBgColor,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(surfaceColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = onSurfaceVariantColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = searchQuery ?: "",
                onValueChange = onChangeSearchQuery,
                textStyle = androidx.compose.ui.text.TextStyle(color = onBgColor, fontSize = 16.sp),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .runOnEnterKeyPressed {
                        if (!searchQuery.isNullOrBlank()) {
                            onSearch(searchQuery)
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = {
                        if (!searchQuery.isNullOrBlank()) {
                            onSearch(searchQuery)
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    }
                ),
                singleLine = true,
                cursorBrush = SolidColor(onBgColor),
                decorationBox = { innerTextField ->
                    if (searchQuery.isNullOrBlank()) {
                        Text("Search for manga, authors...", color = onSurfaceVariantColor, fontSize = 16.sp)
                    }
                    innerTextField()
                }
            )
            if (!searchQuery.isNullOrEmpty()) {
                IconButton(
                    onClick = { onChangeSearchQuery("") },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear",
                        tint = onSurfaceVariantColor,
                    )
                }
            }
        }
    }
}

@Composable
fun CustomSearchIdleContent(
    contentPadding: PaddingValues,
    recentSearches: List<String>,
    suggestedManga: List<Manga>,
    trendingSearches: List<SearchScreenModel.TrendingItem>,
    onClickRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
    onClickTrending: (SearchScreenModel.TrendingItem) -> Unit,
    onClickSuggested: (Manga) -> Unit,
) {
    val bgColor = androidx.compose.material3.MaterialTheme.colorScheme.background
    val onBgColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
    val surfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
    val subtleColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize().background(bgColor),
    ) {
        // Recent Searches
        if (recentSearches.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Searches", color = onBgColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Clear All", color = subtleColor, fontSize = 14.sp, modifier = Modifier.clickable { onClearRecent() })
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentSearches) { query ->
                        Box(
                            modifier = Modifier
                                .background(surfaceColor, RoundedCornerShape(20.dp))
                                .clickable { onClickRecent(query) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(query, color = onBgColor, fontSize = 14.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Trending Searches
        if (trendingSearches.isNotEmpty()) {
            item {
                Text(
                    text = "Trending Searches",
                    color = onBgColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    trendingSearches.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClickTrending(item) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.rank.toString(),
                                color = subtleColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(32.dp)
                            )
                            Column {
                                Text(item.title, color = onBgColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text(item.subtitle, color = subtleColor, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Suggested for you
        if (suggestedManga.isNotEmpty()) {
            item {
                Text(
                    text = "Suggested For You",
                    color = onBgColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                val chunked = suggestedManga.chunked(2)
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    chunked.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { manga ->
                                Column(modifier = Modifier.weight(1f).clickable { onClickSuggested(manga) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.7f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(surfaceColor)
                                    ) {
                                        eu.kanade.presentation.manga.components.MangaCover.Book(
                                            data = manga,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = manga.title,
                                        color = onBgColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = manga.artist ?: manga.author ?: "",
                                        color = subtleColor,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GlobalSearchContent(
    items: Map<CatalogueSource, SearchItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    fromSourceId: Long? = null,
) {
    val bgColor = androidx.compose.material3.MaterialTheme.colorScheme.background
    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.background(bgColor).fillMaxSize()
    ) {
        items.forEach { (source, result) ->
            item(key = source.id) {
                GlobalSearchResultItem(
                    title = fromSourceId?.let {
                        "▶ ${source.name}".takeIf { source.id == fromSourceId }
                    } ?: source.name,
                    subtitle = LocaleHelper.getLocalizedDisplayName(source.lang),
                    onClick = { onClickSource(source) },
                    modifier = Modifier.animateItem(),
                ) {
                    when (result) {
                        SearchItemResult.Loading -> {
                            GlobalSearchLoadingResultItem()
                        }
                        is SearchItemResult.Success -> {
                            GlobalSearchCardRow(
                                titles = result.result,
                                getManga = getManga,
                                onClick = onClickItem,
                                onLongClick = onLongClickItem,
                            )
                        }
                        is SearchItemResult.Error -> {
                            GlobalSearchErrorResultItem(message = result.throwable.message)
                        }
                    }
                }
            }
        }
    }
}
