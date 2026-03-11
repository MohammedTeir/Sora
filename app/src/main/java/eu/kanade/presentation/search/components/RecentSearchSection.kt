package eu.kanade.presentation.search.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TextGrey = Color(0xFF9E9E9E)
private val PrimaryBlue = Color(0xFF2F80ED)
private val TextWhite = Color(0xFFFFFFFF)
private val BorderGrey = Color(0xFF2C2C2E)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecentSearchSection(
    searches: List<String>,
    onClickChip: (String) -> Unit,
    onRemoveChip: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "RECENT SEARCHES",
                fontSize = 12.sp,
                color = TextGrey,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
            )
            TextButton(onClick = onClearAll) {
                Text(
                    text = "Clear All",
                    fontSize = 13.sp,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            searches.forEach { query ->
                RecentSearchChip(
                    text = query,
                    onClick = { onClickChip(query) },
                    onRemove = { onRemoveChip(query) },
                )
            }
        }
    }
}

@Composable
fun RecentSearchChip(
    text: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = modifier.border(
            width = 1.dp,
            color = BorderGrey,
            shape = RoundedCornerShape(20.dp),
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                color = TextWhite,
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Remove $text",
                    tint = TextGrey,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
