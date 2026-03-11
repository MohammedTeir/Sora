package eu.kanade.presentation.more.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.more.stats.data.StatsData
import eu.kanade.tachiyomi.source.model.SManga
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// ──────────────────────────────── helpers ────────────────────────────────────

private fun Long.toHoursMinutes(): String {
    val duration = this.toDuration(DurationUnit.MILLISECONDS)
    val h = duration.inWholeHours
    val m = duration.inWholeMinutes % 60
    return "${h}h ${m}m"
}

private val StatusColorMap = mapOf(
    SManga.ONGOING to Color(0xFF4CAF50),
    SManga.COMPLETED to Color(0xFF2196F3),
    SManga.LICENSED to Color(0xFFFF9800),
    SManga.PUBLISHING_FINISHED to Color(0xFF9C27B0),
    SManga.CANCELLED to Color(0xFFF44336),
    SManga.ON_HIATUS to Color(0xFFFFEB3B),
    SManga.UNKNOWN to Color(0xFF9E9E9E),
)

private fun statusLabel(status: Int): String = when (status) {
    SManga.ONGOING -> "Ongoing"
    SManga.COMPLETED -> "Completed"
    SManga.LICENSED -> "Licensed"
    SManga.PUBLISHING_FINISHED -> "Finished"
    SManga.CANCELLED -> "Cancelled"
    SManga.ON_HIATUS -> "On Hiatus"
    else -> "Unknown"
}

// ─────────────────────────────── root ────────────────────────────────────────

@Composable
fun StatsScreenContent(
    state: StatsScreenState.Success,
    paddingValues: PaddingValues,
    navigator: Navigator,
) {
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            item {
                TopNavBar(
                    modifier = Modifier.padding(top = systemBarsPadding.calculateTopPadding()),
                    onBack = navigator::pop,
                )
            }
            item {
                HeroCard(
                    totalReadDuration = state.overview.totalReadDuration,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            item {
                SectionTitle(
                    title = "Weekly Activity",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                WeeklyActivityChart(
                    weeklyActivity = state.weeklyActivity,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 8.dp),
                )
            }
            item {
                SectionTitle(
                    title = "Key Statistics",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                KeyStatsGrid(
                    state = state,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 8.dp),
                )
            }
            item {
                SectionTitle(
                    title = "Reading Streak",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                ReadingStreakSection(
                    streak = state.readingStreak,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 8.dp),
                )
            }
            if (state.statusDistribution.statusCounts.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "Library Status",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                    StatusDonutChart(
                        distribution = state.statusDistribution,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 8.dp),
                    )
                }
            }
            item {
                SectionTitle(
                    title = "Most Read Titles",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                TopMangaSection(
                    topManga = state.topManga,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 8.dp),
                )
            }
        }
    }
}

// ─────────────────────────── top nav bar ─────────────────────────────────────

@Composable
private fun TopNavBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Reading Statistics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Your reading insights",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        androidx.compose.material3.IconButton(
            onClick = { },
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = "Share",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ──────────────────────────── hero card ──────────────────────────────────────

@Composable
private fun HeroCard(totalReadDuration: Long, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(listOf(primary, primaryContainer)),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(onPrimary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.MenuBook,
                    contentDescription = null,
                    tint = onPrimary,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (totalReadDuration > 0L) totalReadDuration.toHoursMinutes() else "0h 0m",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = if (totalReadDuration > 0L) onPrimary else onPrimary.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Total Reading Time",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = onPrimary.copy(alpha = 0.8f),
            )
        }
    }
}

// ───────────────────────── section title ─────────────────────────────────────

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier,
    )
}

// ────────────────────────── weekly activity chart ────────────────────────────

@Composable
private fun WeeklyActivityChart(weeklyActivity: StatsData.WeeklyActivity, modifier: Modifier = Modifier) {
    val hasActivity = weeklyActivity.dailyData.any { it.totalReadDuration > 0L }
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(surface, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        if (!hasActivity) {
            EmptyState(message = "No weekly reading activity yet")
        } else {
            val maxDuration = weeklyActivity.dailyData.maxOf { it.totalReadDuration }.toFloat()
            val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    weeklyActivity.dailyData.forEach { day ->
                        val fraction = if (maxDuration > 0f) (day.totalReadDuration / maxDuration) else 0f
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            ) {
                                val barWidth = size.width
                                val barHeight = size.height * fraction
                                val cornerRadius = 8.dp.toPx()
                                if (barHeight > 0f) {
                                    drawRoundRect(
                                        color = primary,
                                        topLeft = Offset(0f, size.height - barHeight),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                    )
                                } else {
                                    drawRoundRect(
                                        color = primary.copy(alpha = 0.15f),
                                        topLeft = Offset(0f, size.height - 4.dp.toPx()),
                                        size = Size(barWidth, 4.dp.toPx()),
                                        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    weeklyActivity.dailyData.forEach { day ->
                        Text(
                            text = dayFormat.format(Date(day.dayMs)),
                            fontSize = 11.sp,
                            color = onSurface,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────── key stats grid ──────────────────────────────────

@Composable
private fun KeyStatsGrid(state: StatsScreenState.Success, modifier: Modifier = Modifier) {
    val avgChapters = if (state.overview.libraryMangaCount > 0) {
        state.chapters.readChapterCount / state.overview.libraryMangaCount
    } else {
        0
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Outlined.AutoStories,
                label = "Chapters Read",
                value = state.chapters.readChapterCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Outlined.Timer,
                label = "Time Spent",
                value = state.overview.totalReadDuration.toHoursMinutes(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Outlined.CollectionsBookmark,
                label = "Library Size",
                value = state.overview.libraryMangaCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Outlined.BookmarkBorder,
                label = "Avg per Title",
                value = "$avgChapters ch",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─────────────────────────── reading streak ──────────────────────────────────

@Composable
private fun ReadingStreakSection(streak: StatsData.ReadingStreak, modifier: Modifier = Modifier) {
    if (streak.currentStreak == 0 && streak.longestStreak == 0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            EmptyState(message = "Start reading to build your streak")
        }
        return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StreakCard(
            icon = Icons.Outlined.LocalFireDepartment,
            label = "Current Streak",
            value = "${streak.currentStreak}",
            unit = if (streak.currentStreak == 1) "day" else "days",
            iconTint = Color(0xFFFF6B35),
            modifier = Modifier.weight(1f),
        )
        StreakCard(
            icon = Icons.Outlined.Whatshot,
            label = "Longest Streak",
            value = "${streak.longestStreak}",
            unit = if (streak.longestStreak == 1) "day" else "days",
            iconTint = Color(0xFFFFB300),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StreakCard(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = unit,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─────────────────────────── status donut chart ──────────────────────────────

@Composable
private fun StatusDonutChart(distribution: StatsData.StatusDistribution, modifier: Modifier = Modifier) {
    val total = distribution.statusCounts.values.sum().toFloat()
    if (total == 0f) return

    val segments = distribution.statusCounts
        .entries
        .sortedByDescending { it.value }
        .map { (status, count) ->
            Triple(status, count, StatusColorMap[status] ?: Color(0xFF9E9E9E))
        }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 28.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    var startAngle = -90f

                    segments.forEach { (_, count, color) ->
                        val sweep = (count / total) * 360f
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep - 2f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                        startAngle += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = total.toInt().toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Titles",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                segments.forEach { (status, count, color) ->
                    val pct = ((count / total) * 100).toInt()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, CircleShape),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = statusLabel(status),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "$count ($pct%)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────── top manga ───────────────────────────────────────

@Composable
private fun TopMangaSection(topManga: StatsData.TopManga, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        if (topManga.items.isEmpty()) {
            EmptyState(message = "No reading history yet")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                topManga.items.forEachIndexed { index, item ->
                    TopMangaRow(rank = index + 1, item = item)
                    if (index < topManga.items.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopMangaRow(rank: Int, item: StatsData.TopMangaItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = when (rank) {
                        1 -> Color(0xFFFFD700)
                        2 -> Color(0xFFC0C0C0)
                        else -> Color(0xFFCD7F32)
                    },
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = rank.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        MangaCover.Book(
            data = item.coverData,
            modifier = Modifier
                .height(52.dp)
                .clip(RoundedCornerShape(6.dp)),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.readCount} chapters read",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────── empty state ─────────────────────────────────────

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
