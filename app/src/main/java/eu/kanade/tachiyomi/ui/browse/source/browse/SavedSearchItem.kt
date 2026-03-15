package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.i18n.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedSearchItem(
    savedSearches: ImmutableList<SavedSearch>,
    onSavedSearch: (SavedSearch) -> Unit,
    onSavedSearchPress: (SavedSearch) -> Unit,
) {
    if (savedSearches.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsItemsPaddings.Horizontal,
                vertical = SettingsItemsPaddings.Vertical,
            ),
    ) {
        Text(
            text = "Saved searches",
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(
            modifier = Modifier.padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            savedSearches.forEach {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.combinedClickable(
                        onClick = { onSavedSearch(it) },
                        onLongClick = { onSavedSearchPress(it) },
                    ),
                ) {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
