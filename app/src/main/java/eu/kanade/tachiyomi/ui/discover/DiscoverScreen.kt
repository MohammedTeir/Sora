package eu.kanade.tachiyomi.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import eu.kanade.presentation.theme.SoraBlue
import eu.kanade.tachiyomi.data.discover.SharedList
import eu.kanade.tachiyomi.ui.auth.LoginScreen

import cafe.adriel.voyager.core.screen.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen.DiscoverScreen() {
    val screenModel = rememberScreenModel { DiscoverScreenModel() }
    val state by screenModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateSheet by rememberSaveable { mutableStateOf(false) }
    val navigator = LocalNavigator.currentOrThrow

    // Show snackbar for import message — key on the message value so it only
    // fires when a NEW non-null message arrives, never on null.
    LaunchedEffect(state.importMessage) {
        val msg = state.importMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        screenModel.clearImportMessage()
    }
    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        screenModel.clearErrorMessage()
    }

    // Missing manga dialog
    if (state.missingMangaTitles.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { screenModel.clearMissingManga() },
            title = { Text("Not in your library") },
            text = {
                Column {
                    Text(
                        "${state.missingMangaTitles.size} manga from this list are not in your library yet. " +
                            "Search for them in Browse to add them.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    state.missingMangaTitles.take(10).forEach { title ->
                        Text(
                            "• $title",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.missingMangaTitles.size > 10) {
                        Text(
                            "… and ${state.missingMangaTitles.size - 10} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { screenModel.clearMissingManga() }) {
                    Text("Got it")
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Discover", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                },
                actions = {
                    IconButton(onClick = { screenModel.loadLists() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            if (state.isLoggedIn) {
                FloatingActionButton(
                    onClick = { showCreateSheet = true },
                    containerColor = SoraBlue,
                    contentColor = Color.White,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Share a List")
                }
            }
        },
    ) { padding ->
        // Full-screen spinner only on the very first load (no content yet)
        if (state.isInitialLoad && state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SoraBlue)
            }
            return@Scaffold
        }

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
            ) {
                // Login banner for unauthenticated users
                if (!state.isLoggedIn) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = SoraBlue.copy(alpha = 0.12f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Login,
                                    contentDescription = null,
                                    tint = SoraBlue,
                                    modifier = Modifier.size(24.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Sign in to share lists",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    )
                                    Text(
                                        "Share your reading lists with the community.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    )
                                }
                                OutlinedButton(onClick = { navigator.push(LoginScreen()) }) {
                                    Text("Sign In", fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // My Lists section (only when logged in and has lists)
                if (state.isLoggedIn && state.myLists.isNotEmpty()) {
                    item {
                        SectionHeader(title = "My Lists")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.myLists) { list ->
                                SharedListCard(
                                    list = list,
                                    onImport = { screenModel.importList(list) },
                                    onDelete = { screenModel.deleteMyList(list.id) },
                                    showDelete = true,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Trending section
                if (state.trendingLists.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Trending")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.trendingLists) { list ->
                                SharedListCard(
                                    list = list,
                                    onImport = { screenModel.importList(list) },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Recently Added section
                if (state.recentLists.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Recently Added")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.recentLists) { list ->
                                SharedListCard(
                                    list = list,
                                    onImport = { screenModel.importList(list) },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Empty state (not loading, no lists)
                if (!state.isLoading && state.trendingLists.isEmpty() && state.recentLists.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.Explore,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No lists yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Be the first to share your reading list!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                )
                            }
                        }
                    }
                }
            }

            // Non-blocking refresh indicator at the top of content
            if (state.isLoading && !state.isInitialLoad) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = SoraBlue,
                )
            }
        }
    }

    if (showCreateSheet) {
        CreateListBottomSheet(
            screenModel = screenModel,
            onDismiss = { showCreateSheet = false },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun SharedListCard(
    list: SharedList,
    onImport: () -> Unit,
    onDelete: (() -> Unit)? = null,
    showDelete: Boolean = false,
) {
    Card(
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Cover grid (up to 4 covers in 2x2)
            val mangaItems = list.getMangaItems()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                if (mangaItems.isEmpty()) {
                    Icon(
                        Icons.Outlined.Explore,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    )
                } else {
                    val covers = mangaItems.take(4)
                    androidx.compose.foundation.layout.Column {
                        listOf(
                            covers.getOrNull(0) to covers.getOrNull(1),
                            covers.getOrNull(2) to covers.getOrNull(3),
                        ).forEach { (left, right) ->
                            Row(modifier = Modifier.weight(1f)) {
                                if (left != null) {
                                    AsyncImage(
                                        model = left.coverUrl.ifEmpty { null },
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.weight(1f).fillMaxSize(),
                                    )
                                }
                                if (right != null) {
                                    AsyncImage(
                                        model = right.coverUrl.ifEmpty { null },
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.weight(1f).fillMaxSize(),
                                    )
                                } else if (left != null) {
                                    // Fill empty slot
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = list.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "by ${list.creatorName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${mangaItems.size} manga · ${list.importCount} imports",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (showDelete && onDelete != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onImport,
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", fontSize = 12.sp)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete list",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            } else {
                Button(
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoraBlue),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import", fontSize = 12.sp)
                }
            }
        }
    }
}
