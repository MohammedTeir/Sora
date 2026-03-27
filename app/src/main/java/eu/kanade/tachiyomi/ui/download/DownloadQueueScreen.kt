package eu.kanade.tachiyomi.ui.download

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import eu.kanade.tachiyomi.R
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.presentation.theme.SoraBlue
import tachiyomi.domain.manga.model.asMangaCover

private val PauseOrange = Color(0xFFFF9800)
private val ErrorRed = Color(0xFFE53935)
private val BrandBlue = Color(0xff2977ff)
private val BgColor = Color.Black
private val TextMuted = Color(0xff94a3b8)
private val TextWhite = Color(0xfff1f5f9)

object DownloadQueueScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val screenModel = rememberScreenModel { DownloadQueueScreenModel() }

        val queuedDownloads by screenModel.queuedDownloads.collectAsState()
        val screenState by screenModel.state.collectAsState()
        val isRunning by screenModel.isDownloaderRunning.collectAsState()

        var selectedTab by remember { mutableStateOf(0) } // 0: ACTIVE, 1: COMPLETED, 2: ERROR

        val errorDownloads = queuedDownloads.filter { it.status == Download.State.ERROR }
        val activeQueue = queuedDownloads.filter { it.status != Download.State.ERROR }

        Scaffold(
            topBar = {
                DownloadHeader(
                    onBack = navigator::pop,
                    isRunning = isRunning,
                    onResumeAll = screenModel::resumeAll,
                    onPauseAll = screenModel::pauseAll,
                    onCancelAll = screenModel::cancelAll,
                    onClearCompleted = screenModel::clearCompleted
                )
            },
            containerColor = BgColor,
            bottomBar = {
                // Fake bottom nav just to match design. Usually global nav handles this.
                Spacer(modifier = Modifier.height(20.dp))
            }
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                // Storage Section
                StorageSection(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))

                // Tabs
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    TabItem("ACTIVE", selectedTab == 0) { selectedTab = 0 }
                    TabItem("COMPLETED", selectedTab == 1) { selectedTab = 1 }
                    TabItem("ERROR", selectedTab == 2) { selectedTab = 2 }
                }

                // List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    
                    when (selectedTab) {
                        0 -> {
                            val grouped = activeQueue.groupBy { it.manga.id }
                            grouped.values.forEach { group ->
                                item { SeriesGroup(group = group, screenModel = screenModel) }
                            }
                        }
                        1 -> {
                            items(screenState.completedDownloads) { dl ->
                                CompletedItem(download = dl) { screenModel.openChapter(context, dl) }
                            }
                        }
                        2 -> {
                            val grouped = errorDownloads.groupBy { it.manga.id }
                            grouped.values.forEach { group ->
                                item { SeriesGroup(group = group, screenModel = screenModel) }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(128.dp)) }
                }
            }
        }
    }
}

@Composable
fun TabItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) BrandBlue else Color(0xff64748b),
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )
        )
    }
}

@Composable
fun StorageSection(modifier: Modifier = Modifier) {
    val stat = StatFs(Environment.getDataDirectory().path)
    val totalBytes = stat.totalBytes
    val freeBytes = stat.availableBytes
    val usedBytes = totalBytes - freeBytes

    val totalGB = totalBytes / (1024L * 1024L * 1024L)
    val freeGB = freeBytes / (1024f * 1024f * 1024f)

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BrandBlue.copy(alpha = 0.05f))
            .border(
                BorderStroke(1.dp, BrandBlue.copy(alpha = 0.2f)),
                RoundedCornerShape(24.dp)
            )
            .padding(24.dp)
    ) {
        Text(
            text = "DEVICE STORAGE",
            color = TextMuted,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${"%.1f".format(totalGB - freeGB)} GB ",
                color = TextWhite,
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black)
            )
            Text(
                text = "/ $totalGB GB",
                color = Color(0xff64748b),
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium)
            )
        }
        Text(
            text = "${"%.1f".format(freeGB)} GB FREE",
            color = BrandBlue,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Black)
        )
    }
}

@Composable
fun SeriesGroup(group: List<Download>, screenModel: DownloadQueueScreenModel) {
    if (group.isEmpty()) return
    val first = group.first()
    val manga = first.manga
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Group Header
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(
                    modifier = Modifier.width(21.dp).height(1.dp).padding(end = 8.dp),
                    color = BrandBlue.copy(alpha = 0.4f)
                )
                Text(
                    text = "SERIES:\n${manga.title.uppercase()}",
                    color = TextMuted,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.4.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
            }
            Text(
                text = "${group.size} Items\nRemaining",
                color = BrandBlue,
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
        }

        // Main Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(BrandBlue.copy(alpha = 0.05f))
                .border(BorderStroke(1.dp, BrandBlue.copy(alpha = 0.2f)), RoundedCornerShape(24.dp))
        ) {
            // Big Item (The current downloading / next item)
            BigDownloadItem(download = first, onCancel = { screenModel.cancelDownload(first.chapter) })

            // Smaller Queued Items
            if (group.size > 1) {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    for (i in 1 until group.size) {
                        SmallDownloadItem(download = group[i], onCancel = { screenModel.cancelDownload(group[i].chapter) })
                    }
                }
            }
        }
    }
}

@Composable
fun BigDownloadItem(download: Download, onCancel: () -> Unit) {
    val status by download.statusFlow.collectAsState()
    val progress by download.progressFlow.collectAsState(initial = download.progress)
    
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            eu.kanade.presentation.manga.components.MangaCover.Book(
                data = download.manga.asMangaCover(),
                modifier = Modifier.fillMaxSize(),
                contentDescription = "",
            )
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            0f to Color.Black.copy(alpha=0.8f),
                            0.5f to Color.Black.copy(alpha=0.4f),
                            1f to Color.Black.copy(alpha=0.9f),
                            start = Offset(0f, Float.POSITIVE_INFINITY),
                            end = Offset(0f, 0f)
                        )
                    )
            )
            
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = download.chapter.name,
                            color = TextWhite,
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black),
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        val statusLabel = when(status) {
                            Download.State.DOWNLOADING -> "DOWNLOADING"
                            Download.State.ERROR -> "ERROR"
                            Download.State.PAUSED -> "PAUSED"
                            else -> "QUEUED"
                        }
                        Text(
                            text = statusLabel,
                            color = TextMuted,
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                    // Action button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandBlue)
                            .clickable { onCancel() }
                    ) {
                        Icon(Icons.Outlined.Close, null, tint = Color.White)
                    }
                }

                if (status == Download.State.DOWNLOADING) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$progress%\nDONE",
                            color = BrandBlue,
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                        )
                        Text(
                            text = "\nDOWNLOADING",
                            color = BrandBlue,
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp),
                            textAlign = TextAlign.End
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress / 100f)
                                .fillMaxHeight()
                                .background(BrandBlue)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmallDownloadItem(download: Download, onCancel: () -> Unit) {
    val status by download.statusFlow.collectAsState()
    
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BrandBlue.copy(alpha = 0.05f))
            .border(BorderStroke(1.dp, BrandBlue.copy(alpha = 0.1f)), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.chapter.name,
                color = TextWhite,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            val lbl = if(status == Download.State.ERROR) "ERROR" else "QUEUED"
            Text(
                text = lbl,
                color = Color(0xff64748b),
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black)
            )
        }
        Icon(
            Icons.Outlined.Close,
            contentDescription = "Cancel",
            tint = Color(0xff475569),
            modifier = Modifier
                .clickable { onCancel() }
                .padding(8.dp)
        )
    }
}

@Composable
fun CompletedItem(download: Download, onRead: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandBlue.copy(alpha = 0.05f))
            .border(BorderStroke(1.dp, BrandBlue.copy(alpha = 0.1f)), RoundedCornerShape(16.dp))
            .clickable { onRead() }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.manga.title,
                color = TextWhite,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                text = download.chapter.name,
                color = Color(0xff64748b),
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "COMPLETED",
            color = BrandBlue,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black)
        )
    }
}

@Composable
fun DownloadHeader(
    onBack: () -> Unit,
    isRunning: Boolean,
    onResumeAll: () -> Unit,
    onPauseAll: () -> Unit,
    onCancelAll: () -> Unit,
    onClearCompleted: () -> Unit
) {
    var showOverflow by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_custom_back),
                    contentDescription = "Back",
                    tint = Color.Unspecified
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "DOWNLOADS",
                color = BrandBlue,
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
            )
        }

        Box {
            IconButton(onClick = { showOverflow = true }) {
                Icon(painterResource(R.drawable.ic_custom_settings), contentDescription = "More", tint = Color.Unspecified)
            }
            DropdownMenu(
                expanded = showOverflow,
                onDismissRequest = { showOverflow = false }
            ) {
                if (!isRunning) {
                    DropdownMenuItem(text = { Text("Resume All") }, onClick = { onResumeAll(); showOverflow = false }, leadingIcon = { Icon(painterResource(R.drawable.ic_custom_resume), null, tint = Color.Unspecified, modifier = Modifier.size(24.dp)) })
                } else {
                    DropdownMenuItem(text = { Text("Pause All") }, onClick = { onPauseAll(); showOverflow = false }, leadingIcon = { Icon(painterResource(R.drawable.ic_custom_pause), null, tint = Color.Unspecified, modifier = Modifier.size(24.dp)) })
                }
                DropdownMenuItem(text = { Text("Cancel All", color = ErrorRed) }, onClick = { onCancelAll(); showOverflow = false }, leadingIcon = { Icon(Icons.Outlined.Close, null, tint = ErrorRed) })
                DropdownMenuItem(text = { Text("Clear Completed") }, onClick = { onClearCompleted(); showOverflow = false }, leadingIcon = { Icon(Icons.Outlined.DeleteSweep, null) })
            }
        }
    }
}
