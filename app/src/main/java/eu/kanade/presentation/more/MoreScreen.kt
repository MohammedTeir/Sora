package eu.kanade.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudQueue
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import tachiyomi.core.common.Constants
import tachiyomi.presentation.core.components.material.Scaffold

import androidx.compose.material3.MaterialTheme

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

            // ─── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Sora",
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── Profile Card ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(20.dp))
                    .clickable { onClickProfile() },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(AccentBlue.copy(alpha = 0.4f), MaterialTheme.colorScheme.surfaceContainerHigh),
                                ),
                                shape = CircleShape,
                            )
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(34.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        if (isLoggedIn) {
                            Text(
                                text = userDisplayName.ifBlank { userEmail },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isSyncing) "Syncing…" else lastSyncDisplay.ifBlank { "Tap to sync" },
                                fontSize = 13.sp,
                                color = if (isSyncing) AccentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = "Guest Mode",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sign in to sync your library",
                                    fontSize = 14.sp,
                                    color = AccentBlue,
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
