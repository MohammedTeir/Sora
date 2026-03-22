package eu.kanade.presentation.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.browse.components.GlobalSearchCardRow
import eu.kanade.presentation.browse.components.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.components.GlobalSearchResultItem
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.SoraBlue
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchScreenModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.util.runOnEnterKeyPressed

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

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
    val bgColor = MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            SearchHeader(
                searchQuery = state.searchQuery,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                navigateUp = navigateUp,
            )
        },
        containerColor = bgColor,
    ) { paddingValues ->
        if (state.searchQuery.isNullOrBlank()) {
            SearchIdleContent(
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
            Box(modifier = Modifier.background(bgColor).fillMaxSize()) {
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

// ─────────────────────────────────────────────────────────────────────────────
// Search header — "Search" title + input pill
// ─────────────────────────────────────────────────────────────────────────────

// Kept for backward compatibility with any existing call sites
@Composable
fun CustomSearchHeader(
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    navigateUp: () -> Unit,
) = SearchHeader(searchQuery, onChangeSearchQuery, onSearch, navigateUp)

@Composable
internal fun SearchHeader(
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    navigateUp: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val bgColor = MaterialTheme.colorScheme.background
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
    ) {
        // "Search" bold heading
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Search",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = onBgColor,
            )
        }

        // Search input pill — rounded, icon + field + clear button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .background(surfaceColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = searchQuery ?: "",
                onValueChange = { onChangeSearchQuery(it) },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = onBgColor,
                    fontSize = 16.sp,
                ),
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (!searchQuery.isNullOrBlank()) {
                            onSearch(searchQuery)
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    },
                ),
                singleLine = true,
                cursorBrush = SolidColor(SoraBlue),
                decorationBox = { inner ->
                    Box {
                        if (searchQuery.isNullOrBlank()) {
                            Text(
                                text = "Search for manga, authors...",
                                color = onSurfaceVariant,
                                fontSize = 16.sp,
                            )
                        }
                        inner()
                    }
                },
            )
            if (!searchQuery.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { onChangeSearchQuery("") },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear",
                        tint = onSurfaceVariant,
                    )
                }
            }
        }
    }

    // Auto-focus the field when this screen appears
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

// ─────────────────────────────────────────────────────────────────────────────
// Idle content — shown when the query is blank
// Sections: Recent Searches → Trending Searches → Suggested for You
// ─────────────────────────────────────────────────────────────────────────────

// Kept for backward compatibility
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
) = SearchIdleContent(
    contentPadding, recentSearches, suggestedManga,
    trendingSearches, onClickRecent, onClearRecent,
    onClickTrending, onClickSuggested,
)

@Composable
internal fun SearchIdleContent(
    contentPadding: PaddingValues,
    recentSearches: List<String>,
    suggestedManga: List<Manga>,
    trendingSearches: List<SearchScreenModel.TrendingItem>,
    onClickRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
    onClickTrending: (SearchScreenModel.TrendingItem) -> Unit,
    onClickSuggested: (Manga) -> Unit,
) {
    val bgColor = MaterialTheme.colorScheme.background
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
    ) {

        // ── RECENT SEARCHES ───────────────────────────────────────────────
        if (recentSearches.isNotEmpty()) {
            item(key = "recent_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "RECENT SEARCHES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.7.sp,
                        color = subtleColor,
                    )
                    Text(
                        text = "Clear All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SoraBlue,
                        modifier = Modifier.clickable { onClearRecent() },
                    )
                }
            }

            item(key = "recent_chips") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(recentSearches, key = { "rc_$it" }) { query ->
                        Box(
                            modifier = Modifier
                                .background(surfaceColor, RoundedCornerShape(20.dp))
                                .clickable { onClickRecent(query) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = query,
                                color = onBgColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // ── TRENDING SEARCHES ─────────────────────────────────────────────
        if (trendingSearches.isNotEmpty()) {
            item(key = "trending_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TrendingUp,
                        contentDescription = null,
                        tint = SoraBlue,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = "Trending Searches",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = onBgColor,
                    )
                }
            }

            items(trendingSearches, key = { "trend_${it.rank}" }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClickTrending(item) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Blue rank number
                    Text(
                        text = item.rank.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoraBlue,
                        modifier = Modifier.width(28.dp),
                    )
                    // Title + subtitle
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontSize = 16.sp,
                            color = onBgColor,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.subtitle,
                            fontSize = 12.sp,
                            color = subtleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    // Trailing arrow hint
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = subtleColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            item(key = "trending_spacer") { Spacer(modifier = Modifier.height(28.dp)) }
        }

        // ── SUGGESTED FOR YOU ─────────────────────────────────────────────
        if (suggestedManga.isNotEmpty()) {
            item(key = "suggested_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Suggested for You",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = onBgColor,
                    )
                }
            }

            item(key = "suggested_row") {
                LazyRow(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(suggestedManga, key = { "sug_${it.id}" }) { manga ->
                        SuggestedMangaCard(
                            manga = manga,
                            onClick = { onClickSuggested(manga) },
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Suggested manga card — 111×167 cover + badge + title + genre
// All data comes from the real Manga domain model — zero mock data.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SuggestedMangaCard(
    manga: Manga,
    onClick: () -> Unit,
) {
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .width(111.dp)
            .clickable(onClick = onClick),
    ) {
        // Cover
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(167.dp)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            MangaCover.Book(
                data = manga,
                modifier = Modifier.fillMaxSize(),
                contentDescription = manga.title,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Manga title
        Text(
            text = manga.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = onBgColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // Genre — first 2 entries from the real genre list, joined by " • "
        // manga.genre is already List<String>? in the domain model.
        // Falls back to author name if no genres are stored yet.
        val genreText = manga.genre
            ?.take(2)
            ?.joinToString(" • ") { it.trim() }
            ?.takeUnless { it.isBlank() }
            ?: manga.author
            ?: ""

        if (genreText.isNotBlank()) {
            Text(
                text = genreText,
                fontSize = 10.sp,
                color = subtleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Results content — grouped per source, unchanged from original
// ─────────────────────────────────────────────────────────────────────────────

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
    val bgColor = MaterialTheme.colorScheme.background

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier
            .background(bgColor)
            .fillMaxSize(),
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
                        SearchItemResult.Loading -> GlobalSearchLoadingResultItem()
                        is SearchItemResult.Success -> GlobalSearchCardRow(
                            titles = result.result,
                            getManga = getManga,
                            onClick = onClickItem,
                            onLongClick = onLongClickItem,
                        )
                        is SearchItemResult.Error -> GlobalSearchErrorResultItem(
                            message = result.throwable.message,
                        )
                    }
                }
            }
        }
    }
}
