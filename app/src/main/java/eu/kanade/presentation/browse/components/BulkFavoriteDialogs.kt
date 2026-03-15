package eu.kanade.presentation.browse.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.manga.DuplicateMangaDialog
import eu.kanade.tachiyomi.ui.browse.BulkFavoriteScreenModel
import eu.kanade.tachiyomi.ui.browse.BulkFavoriteScreenModel.Dialog
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Compose to shows the bulk favorite dialogs.
 *
 * @param bulkFavoriteScreenModel the screen model.
 * @param dialog the dialog to show.
 */
@Composable
fun Screen.BulkFavoriteDialogs(
    bulkFavoriteScreenModel: BulkFavoriteScreenModel,
    dialog: Dialog?,
) {
    val navigator = LocalNavigator.current
    val bulkFavoriteState by bulkFavoriteScreenModel.state.collectAsState()

    when (dialog) {
        /* Bulk-favorite actions */
        is Dialog.ChangeMangasCategory ->
            ChangeMangasCategoryDialog(
                dialog = dialog,
                navigator = navigator,
                onDismiss = bulkFavoriteScreenModel::dismissDialog,
                onConfirm = { include, exclude ->
                    bulkFavoriteScreenModel.setMangasCategories(dialog.mangas, include, exclude)
                },
            )

        is Dialog.BulkAllowDuplicate ->
            BulkAllowDuplicateDialog(
                dialog = dialog,
                navigator = navigator,
                onDismiss = bulkFavoriteScreenModel::dismissDialog,
                stopRunning = bulkFavoriteScreenModel::stopRunning,
                addFavorite = bulkFavoriteScreenModel::addFavorite,
                addFavoriteDuplicate = bulkFavoriteScreenModel::addFavoriteDuplicate,
                removeDuplicateSelectedManga = bulkFavoriteScreenModel::removeDuplicateSelectedManga,
            )

        /* Single-favorite actions for screens originally don't have it */
        is Dialog.AddDuplicateManga ->
            AddDuplicateMangaDialog(
                dialog = dialog,
                navigator = navigator,
                state = bulkFavoriteState,
                onDismiss = bulkFavoriteScreenModel::dismissDialog,
                stopRunning = bulkFavoriteScreenModel::stopRunning,
                toggleSelectionMode = bulkFavoriteScreenModel::toggleSelectionMode,
                addFavorite = bulkFavoriteScreenModel::addFavorite,
            )

        is Dialog.RemoveManga ->
            RemoveMangaDialog(
                dialog = dialog,
                onDismiss = bulkFavoriteScreenModel::dismissDialog,
                changeMangaFavorite = bulkFavoriteScreenModel::changeMangaFavorite,
            )

        else -> {}
    }
}

/**
 * Shows dialog to add a single manga to library when there are duplicates.
 */
@Composable
private fun AddDuplicateMangaDialog(
    dialog: Dialog.AddDuplicateManga,
    navigator: Navigator?,
    state: BulkFavoriteScreenModel.State,
    onDismiss: () -> Unit,
    stopRunning: () -> Unit,
    toggleSelectionMode: () -> Unit,
    addFavorite: (Manga) -> Unit,
) {
    stopRunning()

    DuplicateMangaDialog(
        duplicates = dialog.duplicates,
        onDismissRequest = onDismiss,
        onConfirm = {
            if (state.selectionMode) {
                toggleSelectionMode()
            }
            addFavorite(dialog.manga)
        },
        onOpenManga = { navigator?.push(MangaScreen(it.id)) },
        onMigrate = { /* Migration not supported in base */ },
        targetManga = dialog.manga,
    )
}

@Composable
private fun RemoveMangaDialog(
    dialog: Dialog.RemoveManga,
    onDismiss: () -> Unit,
    changeMangaFavorite: (Manga) -> Unit,
) {
    RemoveMangaDialog(
        onDismissRequest = onDismiss,
        onConfirm = { changeMangaFavorite(dialog.manga) },
        mangaToRemove = dialog.manga,
    )
}

@Composable
private fun ChangeMangasCategoryDialog(
    dialog: Dialog.ChangeMangasCategory,
    navigator: Navigator?,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>, List<Long>) -> Unit,
) {
    ChangeCategoryDialog(
        initialSelection = dialog.initialSelection,
        onDismissRequest = onDismiss,
        onEditCategories = { navigator?.push(CategoryScreen()) },
        onConfirm = onConfirm,
    )
}

/**
 * Shows dialog to bulk allow/skip multiple manga to library when there are duplicates.
 */
@Composable
private fun BulkAllowDuplicateDialog(
    dialog: Dialog.BulkAllowDuplicate,
    navigator: Navigator?,
    onDismiss: () -> Unit,
    stopRunning: () -> Unit,
    addFavorite: (startIdx: Int) -> Unit,
    addFavoriteDuplicate: (skipAllDuplicates: Boolean) -> Unit,
    removeDuplicateSelectedManga: (index: Int) -> Unit,
) {
    DuplicateMangaDialog(
        duplicates = dialog.duplicates,
        onDismissRequest = onDismiss,
        onConfirm = { addFavorite(dialog.currentIdx + 1) },
        onOpenManga = { navigator?.push(MangaScreen(it.id)) },
        onMigrate = { /* Migration not supported in base */ },
        targetManga = dialog.manga,
        bulkFavoriteManga = dialog.manga,
        onAllowAllDuplicate = { addFavoriteDuplicate(false) },
        onSkipAllDuplicate = { addFavoriteDuplicate(true) },
        onSkipDuplicate = {
            removeDuplicateSelectedManga(dialog.currentIdx)
            addFavorite(dialog.currentIdx)
        },
        stopRunning = stopRunning,
    )
}

@Composable
fun bulkSelectionButton(
    isRunning: Boolean,
    toggleSelectionMode: () -> Unit,
): AppBar.AppBarAction {
    val title = stringResource(MR.strings.action_bulk_select)
    return if (isRunning) {
        AppBar.Action(
            title = title,
            icon = Icons.Outlined.HourglassEmpty,
            onClick = {},
            enabled = false,
        )
    } else {
        AppBar.Action(
            title = title,
            icon = Icons.Outlined.Checklist,
            onClick = toggleSelectionMode,
        )
    }
}
