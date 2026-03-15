package eu.kanade.tachiyomi.ui.download

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CancelScheduleSend
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.presentation.theme.SoraBlue
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen

// ── Colour palette ─────────────────────────────────────────────────────────
private val PauseOrange = Color(0xFFFF9800)
private val ErrorRed = Color(0xFFE53935)
// CompletedGreen removed — use MaterialTheme.colorScheme.tertiary at the call site instead.
// The Sora colour scheme maps tertiary to the downloaded/success green (SoraGreen = 0xFF47A84A).

object DownloadQueueScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { DownloadQueueScreenModel() }

        val queuedDownloads by screenModel.queuedDownloads.collectAsState()
        val screenState by screenModel.state.collectAsState()
        val isRunning by screenModel.isDownloaderRunning.collectAsState()
        val parallelLimit by screenModel.parallelLimit.collectAsState()

        val hasContent = queuedDownloads.isNotEmpty() ||
            screenState.completedDownloads.isNotEmpty()

        Scaffold(
            topBar = {
                DownloadQueueHeader(
                    isRunning = isRunning,
                    hasQueue = queuedDownloads.isNotEmpty(),
                    hasCompleted = screenState.completedDownloads.isNotEmpty(),
                    parallelLimit = parallelLimit,
                    onBack = navigator::pop,
                    onResumeAll = screenModel::resumeAll,
                    onPauseAll = screenModel::pauseAll,
                    onCancelAll = screenModel::cancelAll,
                    onClearCompleted = screenModel::clearCompleted,
                    onSetParallelLimit = screenModel::setParallelLimit,
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { contentPadding ->
            if (!hasContent) {
                EmptyScreen(
                    message = "Your download queue is currently empty.\nBeautiful manga adventures await you!",
                    icon = Icons.Outlined.DownloadForOffline,
                    modifier = Modifier.padding(contentPadding),
                )
                return@Scaffold
            }

            // Mutable list for drag-and-drop reordering
            val queueState = remember(queuedDownloads) { queuedDownloads.toMutableStateList() }
            val lazyListState = rememberLazyListState()

            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                queueState.add(to.index, queueState.removeAt(from.index))
                screenModel.reorder(queueState.toList())
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // ── Active / queued / paused downloads ───────────────────
                if (queueState.isNotEmpty()) {
                    item(key = "queue_header") {
                        SectionHeader(title = "QUEUE", count = queueState.size)
                    }

                    items(
                        items = queueState,
                        key = { it.chapter.id },
                    ) { download ->
                        ReorderableItem(reorderableState, download.chapter.id) { isDragging ->
                            val containerColor by animateColorAsState(
                                targetValue = if (isDragging) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                label = "drag_bg",
                            )
                            DownloadQueueItem(
                                download = download,
                                isRunning = isRunning,
                                dragModifier = Modifier.draggableHandle(),
                                containerColor = containerColor,
                                onResume = { screenModel.resumeDownload(download.chapter) },
                                onPause = { screenModel.pauseDownload(download.chapter) },
                                onCancel = { screenModel.cancelDownload(download.chapter) },
                                onMoveToTop = { screenModel.moveToTop(download) },
                                onMoveToBottom = { screenModel.moveToBottom(download) },
                                onMoveSeriesToTop = { screenModel.moveSeriesToTop(download.manga.id) },
                                onMoveSeriesToBottom = { screenModel.moveSeriesToBottom(download.manga.id) },
                                onCancelSeries = { screenModel.cancelSeries(download.manga.id) },
                            )
                        }
                    }
                }

                // ── Recently completed ───────────────────────────────────
                val completed = screenState.completedDownloads
                if (completed.isNotEmpty()) {
                    item(key = "completed_header") {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(title = "RECENTLY COMPLETED", count = completed.size)
                    }

                    items(
                        items = completed,
                        key = { "done_${it.chapter.id}" },
                    ) { download ->
                        CompletedDownloadItem(download = download)
                    }

                    item(key = "clear_btn") {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            TextButton(onClick = screenModel::clearCompleted) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Clear Completed",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Top header with global controls ────────────────────────────────────────

private val LimitOptions = listOf(1, 2, 3, 5, 10)

@Composable
private fun DownloadQueueHeader(
    isRunning: Boolean,
    hasQueue: Boolean,
    hasCompleted: Boolean,
    parallelLimit: Int,
    onBack: () -> Unit,
    onResumeAll: () -> Unit,
    onPauseAll: () -> Unit,
    onCancelAll: () -> Unit,
    onClearCompleted: () -> Unit,
    onSetParallelLimit: (Int) -> Unit,
) {
    var showOverflow by remember { mutableStateOf(false) }

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
            // Back + title
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
                Text(
                    text = "Download Queue",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Overflow menu
            Box {
                IconButton(onClick = { showOverflow = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = showOverflow,
                    onDismissRequest = { showOverflow = false },
                ) {
                    if (hasQueue) {
                        if (!isRunning) {
                            DropdownMenuItem(
                                text = { Text("Resume All") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.PlayCircle, null, tint = SoraBlue)
                                },
                                onClick = { onResumeAll(); showOverflow = false },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Pause All") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.PauseCircle, null, tint = PauseOrange)
                                },
                                onClick = { onPauseAll(); showOverflow = false },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Cancel All", color = ErrorRed) },
                            leadingIcon = {
                                Icon(Icons.Outlined.CancelScheduleSend, null, tint = ErrorRed)
                            },
                            onClick = { onCancelAll(); showOverflow = false },
                        )
                    }
                    if (hasCompleted) {
                        if (hasQueue) HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Clear Completed") },
                            leadingIcon = { Icon(Icons.Outlined.DeleteSweep, null) },
                            onClick = { onClearCompleted(); showOverflow = false },
                        )
                    }
                }
            }
        }

        // Global action chips
        if (hasQueue) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isRunning) {
                    ActionChip(
                        label = "Resume All",
                        color = SoraBlue,
                        icon = {
                            Icon(
                                Icons.Default.PlayArrow,
                                null,
                                Modifier.size(14.dp),
                                tint = Color.White,
                            )
                        },
                        onClick = onResumeAll,
                    )
                } else {
                    ActionChip(
                        label = "Pause All",
                        color = PauseOrange,
                        icon = {
                            Icon(
                                Icons.Default.Pause,
                                null,
                                Modifier.size(14.dp),
                                tint = Color.White,
                            )
                        },
                        onClick = onPauseAll,
                    )
                }
                ActionChip(
                    label = "Cancel All",
                    color = ErrorRed,
                    icon = {
                        Icon(Icons.Default.Close, null, Modifier.size(14.dp), tint = Color.White)
                    },
                    onClick = onCancelAll,
                )
            }

            // Download-limit chips
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "At a time:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LimitOptions.forEach { value ->
                    val selected = value == parallelLimit
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selected) SoraBlue else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable { onSetParallelLimit(value) }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = value.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    color: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        icon()
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

// ── Section header ─────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = count.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Queue item ─────────────────────────────────────────────────────────────

@Composable
private fun DownloadQueueItem(
    download: Download,
    isRunning: Boolean,
    dragModifier: Modifier,
    containerColor: Color,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onCancel: () -> Unit,
    onMoveToTop: () -> Unit,
    onMoveToBottom: () -> Unit,
    onMoveSeriesToTop: () -> Unit,
    onMoveSeriesToBottom: () -> Unit,
    onCancelSeries: () -> Unit,
) {
    val status by download.statusFlow.collectAsState()
    val progress by download.progressFlow.collectAsState(initial = download.progress)
    val progressFloat by animateFloatAsState(targetValue = progress / 100f, label = "dl_progress")

    var showMenu by remember { mutableStateOf(false) }

    val statusColor = when (status) {
        Download.State.DOWNLOADING -> SoraBlue
        Download.State.QUEUE       -> MaterialTheme.colorScheme.primary
        Download.State.PAUSED      -> PauseOrange
        Download.State.ERROR       -> ErrorRed
        else                       -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusLabel = when (status) {
        Download.State.DOWNLOADING -> "DOWNLOADING"
        Download.State.QUEUE       -> "QUEUED"
        Download.State.PAUSED      -> "PAUSED"
        Download.State.ERROR       -> "ERROR"
        else                       -> "WAITING"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Drag handle
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = dragModifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))

            // Manga cover
            eu.kanade.presentation.manga.components.MangaCover.Book(
                data = download.manga.asMangaCover(),
                modifier = Modifier
                    .size(width = 42.dp, height = 58.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentDescription = download.manga.title,
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Title + chapter name + status badge
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.manga.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = download.chapter.name,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Per-item action buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (status) {
                    Download.State.DOWNLOADING -> {
                        // Currently downloading → show Pause
                        ItemIconButton(onClick = onPause) {
                            Icon(
                                Icons.Default.Pause,
                                "Pause",
                                Modifier.size(20.dp),
                                tint = PauseOrange,
                            )
                        }
                    }
                    Download.State.PAUSED -> {
                        // Paused individually → show Resume
                        ItemIconButton(onClick = onResume) {
                            Icon(
                                Icons.Default.PlayArrow,
                                "Resume",
                                Modifier.size(20.dp),
                                tint = SoraBlue,
                            )
                        }
                    }
                    Download.State.ERROR -> {
                        // Error → show Retry (resume resets it to QUEUE)
                        ItemIconButton(onClick = onResume) {
                            Icon(
                                Icons.Default.PlayArrow,
                                "Retry",
                                Modifier.size(20.dp),
                                tint = ErrorRed,
                            )
                        }
                    }
                    Download.State.QUEUE -> {
                        // In queue waiting → show Pause to remove from active queue
                        ItemIconButton(onClick = onPause) {
                            Icon(
                                Icons.Default.Pause,
                                "Pause",
                                Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    else -> {
                        // Any other state → placeholder spacer so layout stays stable
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }

                ItemIconButton(onClick = onCancel) {
                    Icon(
                        Icons.Default.Close,
                        "Cancel",
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box {
                    ItemIconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            "More options",
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DownloadItemMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        onMoveToTop = { onMoveToTop(); showMenu = false },
                        onMoveToBottom = { onMoveToBottom(); showMenu = false },
                        onMoveSeriesToTop = { onMoveSeriesToTop(); showMenu = false },
                        onMoveSeriesToBottom = { onMoveSeriesToBottom(); showMenu = false },
                        onCancelThis = { onCancel(); showMenu = false },
                        onCancelSeries = { onCancelSeries(); showMenu = false },
                    )
                }
            }
        }

        // Progress row
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${(progressFloat * 100).toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
            )
        val pagesText = when {
            status == Download.State.ERROR  -> "Error — tap retry"
            status == Download.State.PAUSED -> "Paused"
            status == Download.State.QUEUE  -> "Queued…"
            download.pages != null          -> "${download.downloadedImages} / ${download.pages!!.size} pages"
            else                            -> "Waiting…"
        }
            Text(
                text = pagesText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progressFloat },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = statusColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
    }
}

// ── 3-dot item menu ────────────────────────────────────────────────────────

@Composable
private fun DownloadItemMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onMoveToTop: () -> Unit,
    onMoveToBottom: () -> Unit,
    onMoveSeriesToTop: () -> Unit,
    onMoveSeriesToBottom: () -> Unit,
    onCancelThis: () -> Unit,
    onCancelSeries: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Move to Top") },
            leadingIcon = { Icon(Icons.Outlined.KeyboardDoubleArrowUp, null, Modifier.size(18.dp)) },
            onClick = onMoveToTop,
        )
        DropdownMenuItem(
            text = { Text("Move to Bottom") },
            leadingIcon = {
                Icon(Icons.Outlined.KeyboardDoubleArrowDown, null, Modifier.size(18.dp))
            },
            onClick = onMoveToBottom,
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Move Series to Top") },
            leadingIcon = { Icon(Icons.Outlined.KeyboardDoubleArrowUp, null, Modifier.size(18.dp)) },
            onClick = onMoveSeriesToTop,
        )
        DropdownMenuItem(
            text = { Text("Move Series to Bottom") },
            leadingIcon = {
                Icon(Icons.Outlined.KeyboardDoubleArrowDown, null, Modifier.size(18.dp))
            },
            onClick = onMoveSeriesToBottom,
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Cancel This Chapter", color = ErrorRed) },
            leadingIcon = { Icon(Icons.Default.Close, null, Modifier.size(18.dp), tint = ErrorRed) },
            onClick = onCancelThis,
        )
        DropdownMenuItem(
            text = { Text("Cancel All from Series", color = ErrorRed) },
            leadingIcon = {
                Icon(Icons.Outlined.CancelScheduleSend, null, Modifier.size(18.dp), tint = ErrorRed)
            },
            onClick = onCancelSeries,
        )
    }
}

// ── Completed item ─────────────────────────────────────────────────────────

@Composable
private fun CompletedDownloadItem(download: Download) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        eu.kanade.presentation.manga.components.MangaCover.Book(
            data = download.manga.asMangaCover(),
            modifier = Modifier
                .size(width = 36.dp, height = 50.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentDescription = download.manga.title,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.manga.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = download.chapter.name,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Completed",
            tint = MaterialTheme.colorScheme.tertiary, // tertiary = SoraGreen in the Sora colour scheme
            modifier = Modifier.size(22.dp),
        )
    }
}

// ── Helper ─────────────────────────────────────────────────────────────────

@Composable
private fun ItemIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
