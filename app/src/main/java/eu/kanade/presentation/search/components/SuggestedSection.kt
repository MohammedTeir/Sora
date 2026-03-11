package eu.kanade.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.manga.components.MangaCover
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover

private val TextWhite = Color(0xFFFFFFFF)
private val TextGrey = Color(0xFF9E9E9E)
private val PrimaryBlue = Color(0xFF2F80ED)
private val BadgeBg = Color(0xFF2F80ED)

@Composable
fun SuggestedSection(
    mangaList: List<Manga>,
    onClickManga: (Manga) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 14.dp),
        ) {
            Text(
                text = "Suggested for You",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
            )
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(18.dp),
            )
        }

        // Grid – non-scrollable inside LazyColumn parent
        val columns = 3
        val rows = (mangaList.size + columns - 1) / columns
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(rows) { rowIndex ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    repeat(columns) { colIndex ->
                        val index = rowIndex * columns + colIndex
                        if (index < mangaList.size) {
                            MangaGridCard(
                                manga = mangaList[index],
                                onClick = { onClickManga(mangaList[index]) },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MangaGridCard(
    manga: Manga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Box {
            MangaCover.Book(
                data = manga.asMangaCover(),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                contentDescription = manga.title,
            )
            // Badge
            val badgeText = if (!manga.favorite) "New" else null
            if (badgeText != null) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BadgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(top = 6.dp)) {
            Text(
                text = manga.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val genre = manga.genre?.firstOrNull()
            if (genre != null) {
                Text(
                    text = genre,
                    fontSize = 11.sp,
                    color = TextGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
