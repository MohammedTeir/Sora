package eu.kanade.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.history.components.HistoryItem
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.i18n.MR
import java.time.LocalDate


@Composable
fun HistoryScreen(
    state: HistoryScreenModel.State,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String?) -> Unit,
    onClickCover: (mangaId: Long) -> Unit,
    onClickResume: (mangaId: Long, chapterId: Long) -> Unit,
    onClickFavorite: (mangaId: Long) -> Unit,
    onDialogChange: (HistoryScreenModel.Dialog?) -> Unit,
) {
    Scaffold(
        topBar = {
            if (state.searchQuery != null) {
                SearchToolbar(
                    searchQuery = state.searchQuery,
                    onChangeSearchQuery = onSearchQueryChange,
                    onClickCloseSearch = { onSearchQueryChange(null) },
                )
            } else {
                // ─── Header Section (Top Bar) ────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "History",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Search Button
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            // Delete Button
                            IconButton(
                                onClick = { onDialogChange(HistoryScreenModel.Dialog.DeleteAll) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Clear History",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        state.list.let {
            if (it == null) {
                LoadingScreen(Modifier.padding(contentPadding))
            } else if (it.isEmpty()) {
                val msg = if (!state.searchQuery.isNullOrEmpty()) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.information_no_recent_manga
                }
                EmptyScreen(
                    stringRes = msg,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                HistoryScreenContent(
                    history = it,
                    contentPadding = contentPadding,
                    onClickCover = { history -> onClickCover(history.mangaId) },
                    onClickResume = { history -> onClickResume(history.mangaId, history.chapterId) },
                    onClickDelete = { item -> onDialogChange(HistoryScreenModel.Dialog.Delete(item)) },
                    onClickFavorite = { history -> onClickFavorite(history.mangaId) },
                )
            }
        }
    }
}

@Composable
private fun HistoryScreenContent(
    history: List<HistoryUiModel>,
    contentPadding: PaddingValues,
    onClickCover: (HistoryWithRelations) -> Unit,
    onClickResume: (HistoryWithRelations) -> Unit,
    onClickDelete: (HistoryWithRelations) -> Unit,
    onClickFavorite: (HistoryWithRelations) -> Unit,
) {
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        items(
            items = history,
            key = { "history-${it.hashCode()}" },
            contentType = {
                when (it) {
                    is HistoryUiModel.Header -> "header"
                    is HistoryUiModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is HistoryUiModel.Header -> {
                    // Section Container: TODAY / YESTERDAY etc.
                    Text(
                        text = relativeDateText(item.date).uppercase(),
                        modifier = Modifier
                            .animateItemFastScroll()
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 12.dp, bottom = 8.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is HistoryUiModel.Item -> {
                    val value = item.item
                    HistoryItem(
                        modifier = Modifier.animateItemFastScroll(),
                        history = value,
                        onClickCover = { onClickCover(value) },
                        onClickResume = { onClickResume(value) },
                        onClickDelete = { onClickDelete(value) },
                        onClickFavorite = { onClickFavorite(value) },
                    )
                }
            }
        }
    }
}

sealed interface HistoryUiModel {
    data class Header(val date: LocalDate) : HistoryUiModel
    data class Item(val item: HistoryWithRelations) : HistoryUiModel
}

@PreviewLightDark
@Composable
internal fun HistoryScreenPreviews(
    @PreviewParameter(HistoryScreenModelStateProvider::class)
    historyState: HistoryScreenModel.State,
) {
    TachiyomiPreviewTheme {
        HistoryScreen(
            state = historyState,
            snackbarHostState = SnackbarHostState(),
            onSearchQueryChange = {},
            onClickCover = {},
            onClickResume = { _, _ -> run {} },
            onDialogChange = {},
            onClickFavorite = {},
        )
    }
}
