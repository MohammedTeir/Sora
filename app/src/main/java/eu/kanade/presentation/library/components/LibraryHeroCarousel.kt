package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.ui.library.LibraryItem
import kotlinx.coroutines.delay
import tachiyomi.domain.manga.model.MangaCover

/**
 * Hero carousel showing featured manga from the user's library.
 * Matches the Stitch redesign: large card with cover, gradient overlay,
 * title, chapter info, and genre tags.
 */
@Composable
fun LibraryHeroCarousel(
    items: List<LibraryItem>,
    onMangaClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = 0) { items.size }

    // Auto-scroll every 5 seconds
    LaunchedEffect(pagerState) {
        while (true) {
            delay(5000)
            val nextPage = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(200.dp),
    ) { page ->
        val item = items[page]
        val manga = item.libraryManga.manga
        val coverData = MangaCover(
            mangaId = manga.id,
            sourceId = manga.source,
            isMangaFavorite = manga.favorite,
            url = manga.thumbnailUrl,
            lastModified = manga.coverLastModified,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onMangaClick(manga.id) },
        ) {
            // Cover image as background
            AsyncImage(
                model = coverData,
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.4f to Color.Transparent,
                            1f to Color(0xDD000000),
                        ),
                    ),
            )

            // Content overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            ) {
                Text(
                    text = manga.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                val chapterText = "Chapter ${item.libraryManga.totalChapters}"
                val genreText = manga.genre?.take(2)?.joinToString(", ") ?: ""
                val subtitleText = if (genreText.isNotEmpty()) {
                    "$chapterText  •  $genreText"
                } else {
                    chapterText
                }

                Text(
                    text = subtitleText,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    // Page indicator dots
    if (items.size > 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(items.size.coerceAtMost(5)) { index ->
                val isSelected = pagerState.currentPage % items.size.coerceAtMost(5) == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            },
                        )
                        .then(
                            if (isSelected) {
                                Modifier.fillMaxWidth(0.06f)
                            } else {
                                Modifier.fillMaxWidth(0.02f)
                            },
                        ),
                )
            }
        }
    }
}
