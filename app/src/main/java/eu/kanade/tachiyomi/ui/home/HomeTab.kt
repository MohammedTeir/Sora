package eu.kanade.tachiyomi.ui.home

import android.text.format.DateUtils
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import eu.kanade.presentation.history.HistoryUiModel
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.theme.LocalDarkTheme
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private val SoraBlue = Color(0xFF2977FF)

data object HomeTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_library_enter)
            return TabOptions(
                index = 0u,
                title = stringResource(MR.strings.label_home),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {}

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current

        // Dark/Light theme toggle state
        val darkThemeState = LocalDarkTheme.current
        val isDark = darkThemeState.value

        // History data for Continue Reading + Recently Read
        val historyModel = rememberScreenModel { HistoryScreenModel() }
        val historyState by historyModel.state.collectAsState()

        // Updates data for Library Updates section
        val updatesModel = rememberScreenModel { UpdatesScreenModel() }
        val updatesState by updatesModel.state.collectAsState()

        val historyItems = historyState.list
            ?.filterIsInstance<HistoryUiModel.Item>()
            ?: emptyList()
        val continueItem = historyItems.firstOrNull()?.item
        val recentItems = historyItems.drop(1).take(10).map { it.item }
        val updateItems = updatesState.items.take(10)

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // ─── Top Navigation Bar ───────────────────────────────────────
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Sora",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    // Theme toggle button
                    IconButton(onClick = { darkThemeState.value = !isDark }) {
                        Icon(
                            imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = {}) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )

            LazyColumn(
                contentPadding = PaddingValues(bottom = 90.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                // ─── Continue Reading ───────────────────────────────────
                if (continueItem != null) {
                    item {
                        SectionHeader(title = "Continue Reading")
                        Spacer(modifier = Modifier.height(8.dp))
                        ContinueReadingCard(
                            manga = continueItem,
                            onContinue = {
                                historyModel.getNextChapterForManga(
                                    continueItem.mangaId,
                                    continueItem.chapterId,
                                )
                            },
                            onCardClick = { navigator.push(MangaScreen(continueItem.mangaId)) },
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // ─── Recently Read ──────────────────────────────────────
                if (recentItems.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionHeader(title = "Recently Read", inRow = true)
                            Text(
                                text = "View All",
                                color = SoraBlue,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(recentItems) { history ->
                                RecentMangaCard(
                                    history = history,
                                    onClick = { navigator.push(MangaScreen(history.mangaId)) },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // ─── Library Updates ────────────────────────────────────
                if (updateItems.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Library Updates")
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(updateItems) { item ->
                        UpdateListItem(
                            item = item,
                            onClick = { navigator.push(MangaScreen(item.update.mangaId)) },
                            onDownload = {
                                updatesModel.downloadChapters(listOf(item), ChapterDownloadAction.START)
                            },
                        )
                    }
                }
            }
        }

        // ─── Collect Continue Reading events ────────────────────────────
        LaunchedEffect(Unit) {
            historyModel.events.collectLatest { event ->
                when (event) {
                    is HistoryScreenModel.Event.OpenChapter -> {
                        val chapter = event.chapter
                        if (chapter != null) {
                            val intent = ReaderActivity.newIntent(context, chapter.mangaId, chapter.id)
                            context.startActivity(intent)
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, inRow: Boolean = false) {
    val modifier = if (inRow) Modifier else Modifier.padding(horizontal = 16.dp)
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
        ),
        modifier = modifier,
    )
}

@Composable
private fun ContinueReadingCard(
    manga: HistoryWithRelations,
    onContinue: () -> Unit,
    onCardClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 16.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background cover image
            AsyncImage(
                model = manga.coverData,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.45f,
            )
            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC000000)),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Chapter ${manga.chapterNumber.toInt()}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.7f)),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onContinue,
                    colors = ButtonDefaults.buttonColors(containerColor = SoraBlue),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Continue", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun RecentMangaCard(
    history: HistoryWithRelations,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() },
    ) {
        AsyncImage(
            model = history.coverData,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 100.dp, height = 140.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = history.title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Ch. ${history.chapterNumber.toInt()}",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            ),
        )
    }
}

@Composable
private fun UpdateListItem(
    item: UpdatesItem,
    onClick: () -> Unit,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.update.coverData,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.update.mangaTitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.update.chapterName,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = DateUtils.getRelativeTimeSpanString(
                    item.update.dateFetch,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                ).toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                ),
            )
        }
        // Download button with status
        val downloadState = item.downloadStateProvider()
        val downloadProgress = item.downloadProgressProvider()

        when {
            downloadState == eu.kanade.tachiyomi.data.download.model.Download.State.DOWNLOADED -> {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Downloaded",
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(24.dp),
                )
            }
            downloadState == eu.kanade.tachiyomi.data.download.model.Download.State.DOWNLOADING -> {
                CircularProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = SoraBlue,
                )
            }
            downloadState == eu.kanade.tachiyomi.data.download.model.Download.State.QUEUE -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = SoraBlue,
                )
            }
            else -> {
                IconButton(onClick = onDownload) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = "Download",
                        tint = SoraBlue,
                    )
                }
            }
        }
    }
}
