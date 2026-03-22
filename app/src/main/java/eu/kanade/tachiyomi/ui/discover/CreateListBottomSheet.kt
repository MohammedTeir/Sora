package eu.kanade.tachiyomi.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import coil3.compose.AsyncImage
import eu.kanade.presentation.theme.SoraBlue
import eu.kanade.tachiyomi.data.discover.SharedMangaItem
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel
import kotlinx.coroutines.launch
import tachiyomi.domain.library.model.LibraryManga

import cafe.adriel.voyager.core.screen.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen.CreateListBottomSheet(
    screenModel: DiscoverScreenModel,
    // ── FIX 3: LibraryScreenModel passed in from DiscoverScreen instead of
    // being created here with rememberScreenModel { LibraryScreenModel() }.
    //
    // The old code created a NEW LibraryScreenModel every time the sheet opened,
    // which triggered a fresh library fetch each time and lost any cached data
    // on re-open. Passing it from the parent means one instance lives as long
    // as the Discover tab is in the back stack.
    libraryScreenModel: LibraryScreenModel,
    onDismiss: () -> Unit,
) {
    val libraryState by libraryScreenModel.state.collectAsState()
    val discoverState by screenModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var listTitle by rememberSaveable { mutableStateOf("") }
    val selectedMangaIds = remember { mutableStateListOf<Long>() }

    val allLibraryManga: List<LibraryManga> = remember(libraryState.libraryData.favorites) {
        libraryState.libraryData.favorites.map { it.libraryManga }
    }

    // ── FIX 2: Auto-dismiss when upload completes successfully.
    // Previously the sheet was dismissed immediately on button tap (before the
    // upload coroutine finished), which triggered onResume → loadLists() while
    // isUploadingList was still true. The resulting isFetchingLists=true made
    // isLoading=true, so when the user reopened the sheet the button showed a
    // spinner (discoverState.isLoading == true) and was disabled — appearing stuck.
    //
    // Now we keep the sheet open while isUploadingList=true (the user sees the
    // spinner in the button), and only dismiss once the upload + refresh both
    // complete (isUploadingList=false AND importMessage is set, meaning success).
    // On failure the sheet stays open so the user can retry or edit.
    LaunchedEffect(discoverState.isUploadingList, discoverState.importMessage) {
        val uploadJustFinished = !discoverState.isUploadingList && discoverState.importMessage != null
        if (uploadJustFinished) {
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                "Share a List",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = listTitle,
                onValueChange = { listTitle = it },
                label = { Text("List Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Select manga to include (${selectedMangaIds.size} selected)",
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(allLibraryManga, key = { it.manga.id }) { libManga ->
                    val isSelected = libManga.manga.id in selectedMangaIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) selectedMangaIds.remove(libManga.manga.id)
                                else selectedMangaIds.add(libManga.manga.id)
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) selectedMangaIds.add(libManga.manga.id)
                                else selectedMangaIds.remove(libManga.manga.id)
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AsyncImage(
                            model = libManga.manga.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(6.dp)),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = libManga.manga.title,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val selectedItems = allLibraryManga
                        .filter { it.manga.id in selectedMangaIds }
                        .map { libManga ->
                            SharedMangaItem(
                                title     = libManga.manga.title,
                                sourceId  = libManga.manga.source,
                                coverUrl  = libManga.manga.thumbnailUrl ?: "",
                                sourceUrl = libManga.manga.url,
                            )
                        }
                    // ── FIX 2 (cont.): No longer call sheetState.hide() here.
                    // The LaunchedEffect above handles dismissal once the upload
                    // and subsequent loadLists() have both completed.
                    screenModel.uploadList(listTitle.trim(), selectedItems)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SoraBlue),
                // ── FIX 2 (cont.): Gate only on isUploadingList, NOT isLoading.
                // isLoading = isUploadingList || isFetchingLists.
                // Using isLoading caused the button to appear disabled/stuck
                // whenever a background loadLists() refresh was running — even
                // when no upload was in flight. Now the button is only locked
                // during an actual upload operation.
                enabled = !discoverState.isUploadingList && listTitle.isNotBlank() && selectedMangaIds.isNotEmpty(),
            ) {
                // Show spinner only during actual upload, not background refresh.
                if (discoverState.isUploadingList) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Upload List", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
