package eu.kanade.tachiyomi.ui.settings.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.launch
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SyncMangaSelectionScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { SyncMangaSelectionScreenModel() }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = {
                SelectionHeader(
                    selectedCount = state.selectedIds.size,
                    totalCount = state.manga.size,
                    onBack = {
                        screenModel.saveSelection()
                        navigator.pop()
                    },
                    onSave = {
                        screenModel.saveSelection()
                        navigator.pop()
                    },
                    onSelectAll = screenModel::selectAll,
                    onClearAll = screenModel::clearAll,
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { contentPadding ->
            if (state.manga.isEmpty()) {
                EmptyScreen(
                    message = "Your library is empty.\nAdd manga to your library first.",
                    modifier = Modifier.padding(contentPadding),
                )
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(
                    items = state.manga,
                    key = { it.id },
                ) { manga ->
                    MangaSelectionItem(
                        manga = manga,
                        isSelected = manga.id in state.selectedIds,
                        onToggle = { screenModel.toggle(manga.id) },
                    )
                }
            }
        }
    }
}

// ── Header ──────────────────────────────────────────────────────────────────

@Composable
private fun SelectionHeader(
    selectedCount: Int,
    totalCount: Int,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Select Manga to Sync",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (selectedCount == 0) {
                            "All manga ($totalCount) · tap to limit"
                        } else {
                            "$selectedCount of $totalCount selected"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSelectAll) {
                    Text("All", fontSize = 13.sp)
                }
                TextButton(onClick = onClearAll) {
                    Text("Clear", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(onClick = onSave) {
                    Text("Save", fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Manga row ───────────────────────────────────────────────────────────────

@Composable
private fun MangaSelectionItem(
    manga: Manga,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MangaCover.Book(
            data = manga.asMangaCover(),
            modifier = Modifier
                .size(width = 36.dp, height = 50.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentDescription = manga.title,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = manga.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
        )
    }
}

// ── ScreenModel ─────────────────────────────────────────────────────────────

private class SyncMangaSelectionScreenModel(
    private val getFavorites: GetFavorites = Injekt.get(),
    private val syncPrefs: SyncPreferences = Injekt.get(),
) : StateScreenModel<SyncMangaSelectionScreenModel.State>(State()) {

    data class State(
        val manga: List<Manga> = emptyList(),
        val selectedIds: Set<Long> = emptySet(),
    )

    init {
        screenModelScope.launch {
            val allManga = getFavorites.await()
            val savedIds = syncPrefs.syncSelectedMangaIds().get()
                .mapNotNull { it.toLongOrNull() }
                .toSet()
            mutableState.value = State(manga = allManga, selectedIds = savedIds)
        }
    }

    fun toggle(mangaId: Long) {
        mutableState.value = mutableState.value.let { s ->
            val newIds = if (mangaId in s.selectedIds) {
                s.selectedIds - mangaId
            } else {
                s.selectedIds + mangaId
            }
            s.copy(selectedIds = newIds)
        }
    }

    fun selectAll() {
        mutableState.value = mutableState.value.let { s ->
            s.copy(selectedIds = s.manga.map { it.id }.toSet())
        }
    }

    fun clearAll() {
        mutableState.value = mutableState.value.copy(selectedIds = emptySet())
    }

    fun saveSelection() {
        val ids = mutableState.value.selectedIds
        // If all manga are selected, treat it the same as "sync all" (empty set)
        val toSave = if (ids.size == mutableState.value.manga.size) {
            emptySet()
        } else {
            ids.map { it.toString() }.toSet()
        }
        syncPrefs.syncSelectedMangaIds().set(toSave)
    }
}
