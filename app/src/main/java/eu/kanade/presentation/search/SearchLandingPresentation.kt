package eu.kanade.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.search.components.RecentSearchSection
import eu.kanade.presentation.search.components.SearchInputBar
import eu.kanade.presentation.search.components.SuggestedSection
import eu.kanade.presentation.search.components.TrendingSection
import eu.kanade.tachiyomi.ui.search.SearchLandingScreenModel
import tachiyomi.domain.manga.model.Manga

private val BackgroundColor = Color(0xFF000000)
private val TextWhite = Color(0xFFFFFFFF)

@Composable
fun SearchLandingPresentation(
    state: SearchLandingScreenModel.State,
    trendingSearches: List<SearchLandingScreenModel.TrendingItem>,
    onSearchSubmit: (String) -> Unit,
    onClickRecent: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
    onClickTrending: (SearchLandingScreenModel.TrendingItem) -> Unit,
    onClickManga: (Manga) -> Unit,
    navigateUp: () -> Unit,
) {
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = systemBarsPadding.calculateTopPadding() + 8.dp,
                bottom = systemBarsPadding.calculateBottomPadding() + 100.dp,
            ),
        ) {
            // ─── Header ───────────────────────────────────────────────────────
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Text(
                        text = "Search",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            // ─── Search Input Bar (sticky) ────────────────────────────────────
            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundColor)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                ) {
                    SearchInputBar(onSearch = onSearchSubmit)
                }
            }

            // ─── Recent Searches ──────────────────────────────────────────────
            if (state.recentSearches.isNotEmpty()) {
                item {
                    RecentSearchSection(
                        searches = state.recentSearches,
                        onClickChip = onClickRecent,
                        onRemoveChip = onRemoveRecent,
                        onClearAll = onClearRecent,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // ─── Trending Searches ────────────────────────────────────────────
            item {
                TrendingSection(
                    items = trendingSearches,
                    onClickItem = onClickTrending,
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ─── Suggested for You ────────────────────────────────────────────
            if (state.suggestedManga.isNotEmpty()) {
                item {
                    SuggestedSection(
                        mangaList = state.suggestedManga,
                        onClickManga = onClickManga,
                    )
                }
            }
        }
    }
}
