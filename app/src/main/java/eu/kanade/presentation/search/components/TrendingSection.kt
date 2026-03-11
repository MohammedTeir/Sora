package eu.kanade.presentation.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.ui.search.SearchLandingScreenModel

private val TextWhite = Color(0xFFFFFFFF)
private val TextGrey = Color(0xFF9E9E9E)
private val PrimaryBlue = Color(0xFF2F80ED)

@Composable
fun TrendingSection(
    items: List<SearchLandingScreenModel.TrendingItem>,
    onClickItem: (SearchLandingScreenModel.TrendingItem) -> Unit,
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
            Icon(
                imageVector = Icons.Outlined.TrendingUp,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Trending Searches",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
            )
        }

        // Items
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items.forEach { item ->
                TrendingItem(
                    item = item,
                    onClick = { onClickItem(item) },
                )
            }
        }
    }
}

@Composable
fun TrendingItem(
    item: SearchLandingScreenModel.TrendingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Rank number
        Text(
            text = item.rank.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue,
            modifier = Modifier.width(28.dp),
        )
        // Title + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite,
            )
            Text(
                text = item.subtitle,
                fontSize = 12.sp,
                color = TextGrey,
            )
        }
        // Chevron
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = TextGrey,
            modifier = Modifier.size(20.dp),
        )
    }
}
