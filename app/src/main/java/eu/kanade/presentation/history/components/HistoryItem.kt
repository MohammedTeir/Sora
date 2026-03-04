package eu.kanade.presentation.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.util.lang.toTimestampString
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private val CardGradientStart = Color(0xFF1C1F2A)
private val CardGradientEnd = Color(0xFF171A24)
private val AccentBlue = Color(0xFF2D7CFF)
private val GreenBadge = Color(0xFF27C267)
private val SecondaryText = Color(0xFF9AA0A6)

@Composable
fun HistoryItem(
    history: HistoryWithRelations,
    onClickCover: () -> Unit,
    onClickResume: () -> Unit,
    onClickDelete: () -> Unit,
    onClickFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isToday = remember(history.readAt) {
        val now = System.currentTimeMillis()
        history.readAt?.let { (now - it.time) < 24 * 60 * 60 * 1000 } ?: false
    }
    val readAt = remember { history.readAt?.toTimestampString() ?: "" }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClickResume),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(CardGradientStart, CardGradientEnd),
                    ),
                    shape = RoundedCornerShape(16.dp),
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // ─── Thumbnail ───────────────────────────────────────────────
                MangaCover.Book(
                    modifier = Modifier
                        .size(65.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    data = history.coverData,
                    onClick = onClickCover,
                )

                Spacer(modifier = Modifier.width(12.dp))

                // ─── Text Column ─────────────────────────────────────────────
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Title + NEW badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = history.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (isToday) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = GreenBadge,
                                        shape = RoundedCornerShape(20.dp),
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = "NEW",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    // Chapter + dot + time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (history.chapterNumber > -1) {
                            Text(
                                text = "Chapter ${formatChapterNumber(history.chapterNumber)}",
                                fontSize = 13.sp,
                                color = SecondaryText,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(SecondaryText, CircleShape),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = readAt,
                            fontSize = 13.sp,
                            color = SecondaryText,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // ─── Trailing Button ─────────────────────────────────────────
                if (isToday) {
                    // Download button for today's entries
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Transparent, CircleShape)
                            .clickable(onClick = onClickResume),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.Transparent)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier.size(38.dp),
                            ) {
                                drawCircle(
                                    color = AccentBlue,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = stringResource(MR.strings.action_download),
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                } else {
                    // Read indicator for older entries
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(SecondaryText.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = SecondaryText,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HistoryItemPreviews(
    @PreviewParameter(HistoryWithRelationsProvider::class)
    historyWithRelations: HistoryWithRelations,
) {
    TachiyomiPreviewTheme {
        Surface {
            HistoryItem(
                history = historyWithRelations,
                onClickCover = {},
                onClickResume = {},
                onClickDelete = {},
                onClickFavorite = {},
            )
        }
    }
}
