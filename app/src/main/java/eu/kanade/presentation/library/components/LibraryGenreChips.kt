package eu.kanade.presentation.library.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Horizontal scrollable genre filter chips matching the Stitch design.
 * "All" chip is selected by default. Tapping a genre chip filters the library.
 */
@Composable
fun LibraryGenreChips(
    genres: List<String>,
    selectedGenre: String?,
    onGenreSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // "All" chip
        FilterChip(
            selected = selectedGenre == null,
            onClick = { onGenreSelected(null) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                labelColor = MaterialTheme.colorScheme.onSurface,
            ),
        )

        // Genre chips
        genres.forEach { genre ->
            FilterChip(
                selected = selectedGenre == genre,
                onClick = {
                    onGenreSelected(if (selectedGenre == genre) null else genre)
                },
                label = { Text(genre) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}
