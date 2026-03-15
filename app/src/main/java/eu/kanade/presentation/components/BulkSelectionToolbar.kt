package eu.kanade.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun BulkSelectionToolbar(
    selectedCount: Int,
    isRunning: Boolean,
    onClickClearSelection: () -> Unit,
    onChangeCategoryClick: () -> Unit,
    onSelectAll: (() -> Unit)? = null,
    onReverseSelection: (() -> Unit)? = null,
) {
    AppBar(
        titleContent = { Text(text = "$selectedCount") },
        actions = {
            AppBarActions(
                actions = buildList<AppBar.AppBarAction> {
                    if (onSelectAll != null) {
                        add(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_select_all),
                                icon = Icons.Filled.SelectAll,
                                onClick = onSelectAll,
                            ),
                        )
                    }
                    if (onReverseSelection != null) {
                        add(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_select_inverse),
                                icon = Icons.Outlined.FlipToBack,
                                onClick = onReverseSelection,
                            ),
                        )
                    }
                    if (isRunning) {
                        add(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_select_all),
                                icon = Icons.Outlined.HourglassEmpty,
                                onClick = {},
                                enabled = false,
                            ),
                        )
                    } else {
                        add(
                            AppBar.Action(
                                title = stringResource(MR.strings.add_to_library),
                                icon = Icons.Filled.Favorite,
                                onClick = {
                                    if (selectedCount > 0) {
                                        onChangeCategoryClick()
                                    }
                                },
                            ),
                        )
                    }
                }.toImmutableList(),
            )
        },
        isActionMode = true,
        onCancelActionMode = onClickClearSelection,
    )
}

@Preview
@Composable
private fun SelectionToolbarPreview() {
    Column {
        BulkSelectionToolbar(
            selectedCount = 9,
            isRunning = false,
            onClickClearSelection = {},
            onChangeCategoryClick = {},
        )
        BulkSelectionToolbar(
            selectedCount = 9,
            isRunning = true,
            onClickClearSelection = {},
            onChangeCategoryClick = {},
        )
    }
}
