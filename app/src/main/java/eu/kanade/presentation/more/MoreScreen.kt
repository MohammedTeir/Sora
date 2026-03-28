package eu.kanade.presentation.more

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import tachiyomi.core.common.Constants
import tachiyomi.presentation.core.components.material.Scaffold

// ──────────────── Design Tokens ────────────────
private val AccentBlue    = Color(0xFF2F80FF)
private val AccentGreen   = Color(0xFF34C759)
private val AccentRed     = Color(0xFFFF5C5C)

@Composable
fun MoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickDataAndStorage: () -> Unit,
    onClickSettings: () -> Unit,
    onClickAbout: () -> Unit,
    onClickDiscover: () -> Unit,
    // Auth
    isLoggedIn: Boolean = false,
    userDisplayName: String = "",
    userEmail: String = "",
    lastSyncDisplay: String = "",
    isSyncing: Boolean = false,
    onClickProfile: () -> Unit = {},
    onClickSignOut: () -> Unit = {},
    onClickCloudSync: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
        ) {

            // ─── Header & Profile ───────────────────────────────────────────
            val nameToDisplay = if (isLoggedIn) {
                userDisplayName.ifBlank { userEmail.substringBefore("@") }.uppercase()
            } else {
                "GUEST"
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top App Bar like
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SORA PROFILE",
                        color = AccentBlue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Thin,
                        letterSpacing = (-0.8).sp
                    )
                    IconButton(onClick = onClickSettings) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_setting),
                            contentDescription = "Settings",
                            tint = AccentBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Avatar
                Box(contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(AccentBlue, AccentBlue)
                                )
                            )
                            .padding(4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.profileavatar),
                            contentDescription = "Profile Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(
                                    border = BorderStroke(4.dp, Color.Black),
                                    shape = CircleShape
                                )
                        )
                    }

                    // Elite Reader Badge
                    Box(
                        modifier = Modifier
                            .offset(y = 8.dp)
                            .clip(CircleShape)
                            .background(AccentBlue)
                            .border(
                                border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f)),
                                shape = CircleShape
                            )
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ELITE\nREADER",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Thin,
                            letterSpacing = 1.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Name
                Text(
                    text = nameToDisplay,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Thin,
                    letterSpacing = (-2.0).sp,
                    lineHeight = 44.sp,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Rank
                Text(
                    text = "GLOBAL RANK: #421",
                    color = AccentBlue,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Thin,
                    letterSpacing = 2.8.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Stats Column
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProfileStatCard("TOTAL CHAPTERS", "12,842")
                    ProfileStatCard("READING STREAK", "154 DAYS")
                    ProfileStatCard("IN LIBRARY", "892 VOL")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Genre Affinity
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentBlue.copy(alpha = 0.03f))
                        .androidx.compose.foundation.border(
                            1.dp,
                            AccentBlue.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "GENRE AFFINITY",
                        color = Color(0xFFF1F5F9),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Thin,
                        letterSpacing = (-0.5).sp
                    )

                    GenreBar("SHONEN", "42", Color(0xFF2977FF), 0.8f)
                    GenreBar("SEINEN", "28", Color(0xFF34D399), 0.5f)
                    GenreBar("MYSTERY", "15", Color(0xFF2977FF), 0.3f)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Reading Flow
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "READING FLOW",
                        color = Color(0xFFF1F5F9),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Thin,
                        letterSpacing = (-0.5).sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Right,
                        modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
                    )

                    ReadingFlowItem(
                        titlePrefix = "FINISHED CHAPTER 112 OF ",
                        titleHighlighted = "ONE PIECE",
                        timeStr = "2 HOURS AGO"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ReadingFlowItem(
                        titlePrefix = "NEW FAVORITE: ",
                        titleHighlighted = "SOLO LEVELING",
                        timeStr = "YESTERDAY"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ReadingFlowItem(
                        titlePrefix = "COMPLETED VOLUME 4 OF ",
                        titleHighlighted = "BERSERK",
                        timeStr = "3 DAYS AGO"
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ─── DOWNLOADS & CONTENT ─────────────────────────────────────────
            SectionHeader(title = "DOWNLOADS & CONTENT")
            SectionGroup {
                val downloadQueueState = downloadQueueStateProvider()
                val downloadSubtitle = when (downloadQueueState) {
                    DownloadQueueState.Stopped -> null
                    is DownloadQueueState.Paused -> "Paused · ${downloadQueueState.pending} pending"
                    is DownloadQueueState.Downloading -> "${downloadQueueState.pending} downloading"
                }
                MenuItem(
                    icon = Icons.Outlined.GetApp,
                    title = "Download Queue",
                    subtitle = downloadSubtitle,
                    onClick = onClickDownloadQueue,
                )
                MenuDivider()
                MenuItem(
                    icon = Icons.AutoMirrored.Outlined.Label,
                    title = "Categories",
                    onClick = onClickCategories,
                )
                MenuDivider()
                MenuItem(
                    icon = Icons.Outlined.Explore,
                    title = "Share Lists",
                    subtitle = "Discover & share reading lists",
                    onClick = onClickDiscover,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── ANALYTICS ─────────────────────────────────────────────────
            SectionHeader(title = "ANALYTICS")
            SectionGroup {
                MenuItem(
                    icon = Icons.Outlined.QueryStats,
                    title = "Statistics",
                    onClick = onClickStats,
                )
                MenuDivider()
                MenuItem(
                    icon = Icons.Outlined.Storage,
                    title = "Data & Storage",
                    onClick = onClickDataAndStorage,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── APP ─────────────────────────────────────────────────────────
            SectionHeader(title = "APP")
            SectionGroup {
                MenuItem(
                    icon = Icons.Outlined.Settings,
                    title = "Settings",
                    onClick = onClickSettings,
                )
                MenuDivider()
                MenuItem(
                    icon = Icons.Outlined.Info,
                    title = "About",
                    onClick = onClickAbout,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── SUPPORT ─────────────────────────────────────────────────────
            SectionHeader(title = "SUPPORT")
            SectionGroup {
                MenuItem(
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    title = "Help Center",
                    onClick = { uriHandler.openUri(Constants.URL_HELP) },
                )
                MenuDivider()
                MenuItem(
                    icon = Icons.Outlined.AttachMoney,
                    title = "Donate",
                    trailingText = "Support Sora",
                    trailingTextColor = AccentGreen,
                    onClick = { uriHandler.openUri(Constants.URL_DONATE) },
                )
            }

            // ─── CLOUD (shown only when logged in) ───────────────────────────
            if (isLoggedIn) {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "CLOUD")
                SectionGroup {
                    MenuItem(
                        icon = Icons.Outlined.CloudQueue,
                        title = "Cloud Sync",
                        subtitle = if (isSyncing) "Syncing…" else lastSyncDisplay.ifBlank { null },
                        onClick = onClickCloudSync,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ─── Sign Out (only when logged in) / Sign In (when guest) ────────
            if (isLoggedIn) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(20.dp),
                        )
                        .clickable { onClickSignOut() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Sign Out",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(20.dp),
                        )
                        .clickable { onClickProfile() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Sign In / Create Account",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── App Version ─────────────────────────────────────────────────
            Text(
                text = "Sora Version 2.4.0 (Build 892)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// ──────────────── Helper Composables ────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun SectionGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp)),
    ) {
        content()
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    )
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    trailingTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Optional trailing text
        if (trailingText != null) {
            Text(
                text = trailingText,
                fontSize = 13.sp,
                color = trailingTextColor,
                modifier = Modifier.padding(end = 4.dp),
            )
        }

        // Chevron
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ProfileStatCard(title: String, value: String) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp) // Proportional to original
            .clip(RoundedCornerShape(12.dp))
            .background(AccentBlue.copy(alpha = 0.03f))
            .androidx.compose.foundation.border(
                1.dp,
                AccentBlue.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            )
            .padding(24.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            color = AccentBlue,
            fontSize = 30.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = (-1.5).sp
        )
    }
}

@Composable
private fun GenreBar(title: String, badge: String, barColor: Color, fraction: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color(0xFFF1F5F9),
                fontSize = 12.sp,
                fontWeight = FontWeight.Thin,
                letterSpacing = 1.2.sp
            )
            Text(
                text = badge,
                color = barColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun ReadingFlowItem(titlePrefix: String, titleHighlighted: String, timeStr: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Vertical Timeline Line & Checkbox representation
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .androidx.compose.foundation.border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(60.dp) // Fixed line height mapping
                    .background(AccentBlue)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Card Content
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(AccentBlue.copy(alpha = 0.03f))
                .androidx.compose.foundation.border(1.dp, AccentBlue.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(20.dp)
        ) {
            Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    androidx.compose.ui.text.withStyle(
                        style = androidx.compose.ui.text.SpanStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Thin
                        )
                    ) {
                        append(titlePrefix)
                    }
                    androidx.compose.ui.text.withStyle(
                        style = androidx.compose.ui.text.SpanStyle(
                            color = AccentBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Thin
                        )
                    ) {
                        append(titleHighlighted)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeStr,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Thin,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

