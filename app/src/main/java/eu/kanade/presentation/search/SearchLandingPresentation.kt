package eu.kanade.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
            // ─── Header ───────────────────────────────────────────────────
            item {
                Text(
                    text = "Search",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            // ─── Search Input Bar ─────────────────────────────────────────
            item {
                SearchInputBar(
                    onSearch = onSearchSubmit,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ─── Recent Searches ──────────────────────────────────────────
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

            // ─── Trending Searches ────────────────────────────────────────
            item {
                TrendingSection(
                    items = trendingSearches,
                    onClickItem = onClickTrending,
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ─── Suggested for You ────────────────────────────────────────
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
