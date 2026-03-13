package eu.kanade.tachiyomi.ui.settings.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.ChevronRight
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.auth.AuthPreferences
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.auth.FirebaseAuthService
import eu.kanade.tachiyomi.data.sync.SyncResult
import eu.kanade.tachiyomi.data.sync.SyncService
import eu.kanade.tachiyomi.data.sync.SyncWorker
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import tachiyomi.core.common.util.lang.launchIO
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncSettingsScreen : Screen() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { SyncSettingsScreenModel() }
        val state by screenModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            screenModel.events.collect { event ->
                when (event) {
                    is SyncSettingsScreenModel.Event.SyncSuccess ->
                        snackbarHostState.showSnackbar("Sync completed successfully")
                    is SyncSettingsScreenModel.Event.SyncError ->
                        snackbarHostState.showSnackbar("Sync failed: ${event.message}")
                    is SyncSettingsScreenModel.Event.SignedOut -> navigator.pop()
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Cloud Sync") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // ─── Status Card ───────────────────────────────────────────────
                Icon(
                    imageVector = Icons.Outlined.CloudSync,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.userEmail,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.lastSyncDisplay,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                // ─── Sync on Startup ───────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sync on app start",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Automatically sync when opening the app",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.syncOnStartup,
                        onCheckedChange = { screenModel.toggleSyncOnStartup(it) },
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ─── Auto Sync ─────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto sync (every 6 hours)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Sync in the background periodically",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.autoSync,
                        onCheckedChange = { screenModel.toggleAutoSync(it) },
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                // ─── What to Sync ──────────────────────────────────────────────
                Text(
                    text = "What to sync",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))

                SyncCategoryToggle(
                    label = "Library",
                    description = "Favourite manga and their metadata",
                    checked = state.syncLibrary,
                    onCheckedChange = { screenModel.toggleSyncLibrary(it) },
                )
                Spacer(modifier = Modifier.height(12.dp))
                SyncCategoryToggle(
                    label = "Chapters",
                    description = "Read progress and bookmarks",
                    checked = state.syncChapters,
                    onCheckedChange = { screenModel.toggleSyncChapters(it) },
                )
                Spacer(modifier = Modifier.height(12.dp))
                SyncCategoryToggle(
                    label = "History",
                    description = "Reading history and timestamps",
                    checked = state.syncHistory,
                    onCheckedChange = { screenModel.toggleSyncHistory(it) },
                )
                Spacer(modifier = Modifier.height(12.dp))
                SyncCategoryToggle(
                    label = "Tracking",
                    description = "External tracker entries (MAL, AniList…)",
                    checked = state.syncTracking,
                    onCheckedChange = { screenModel.toggleSyncTracking(it) },
                )
                Spacer(modifier = Modifier.height(12.dp))
                SyncCategoryToggle(
                    label = "Categories",
                    description = "User-defined library categories",
                    checked = state.syncCategories,
                    onCheckedChange = { screenModel.toggleSyncCategories(it) },
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ─── Select Manga to Sync ──────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = state.syncLibrary) { navigator.push(SyncMangaSelectionScreen()) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select manga to sync",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (state.syncLibrary) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        )
                        Text(
                            text = if (state.selectedMangaCount == 0) {
                                "All manga"
                            } else {
                                "${state.selectedMangaCount} manga selected"
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                // ─── Sync Now Button ───────────────────────────────────────────
                Button(
                    onClick = { screenModel.syncNow() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !state.isSyncing,
                ) {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Sync Now", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ─── Sign Out ──────────────────────────────────────────────────
                TextButton(
                    onClick = { screenModel.signOut() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Sign Out", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ─── SyncCategoryToggle ────────────────────────────────────────────────────────

@Composable
private fun SyncCategoryToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ─── ScreenModel ───────────────────────────────────────────────────────────────

private class SyncSettingsScreenModel(
    private val authService: FirebaseAuthService = Injekt.get(),
    private val syncService: SyncService = Injekt.get(),
    private val authPrefs: AuthPreferences = Injekt.get(),
    private val syncPrefs: SyncPreferences = Injekt.get(),
) : StateScreenModel<SyncSettingsScreenModel.State>(
    State(
        userEmail = "",
        lastSyncDisplay = "Never synced",
        isSyncing = false,
        syncOnStartup = true,
        autoSync = true,
        syncLibrary = true,
        syncChapters = true,
        syncHistory = true,
        syncTracking = true,
        syncCategories = true,
        selectedMangaCount = 0,
    ),
) {

    private val _events = Channel<Event>(Channel.UNLIMITED)
    val events: Flow<Event> = _events.receiveAsFlow()

    init {
        mutableState.update {
            it.copy(
                userEmail = authPrefs.userEmail().get(),
                lastSyncDisplay = formatLastSync(authPrefs.lastSyncTime().get()),
                syncOnStartup = syncPrefs.syncOnStartup().get(),
                autoSync = authPrefs.autoSync().get(),
                syncLibrary = syncPrefs.syncLibrary().get(),
                syncChapters = syncPrefs.syncChapters().get(),
                syncHistory = syncPrefs.syncHistory().get(),
                syncTracking = syncPrefs.syncTracking().get(),
                syncCategories = syncPrefs.syncCategories().get(),
                selectedMangaCount = syncPrefs.syncSelectedMangaIds().get().size,
            )
        }
    }

    data class State(
        val userEmail: String,
        val lastSyncDisplay: String,
        val isSyncing: Boolean,
        val syncOnStartup: Boolean,
        val autoSync: Boolean,
        val syncLibrary: Boolean,
        val syncChapters: Boolean,
        val syncHistory: Boolean,
        val syncTracking: Boolean,
        val syncCategories: Boolean,
        val selectedMangaCount: Int,
    )

    sealed interface Event {
        data object SyncSuccess : Event
        data class SyncError(val message: String) : Event
        data object SignedOut : Event
    }

    fun syncNow() {
        screenModelScope.launchIO {
            mutableState.update { it.copy(isSyncing = true) }
            when (val result = syncService.syncNow()) {
                is SyncResult.Success -> {
                    mutableState.update {
                        it.copy(
                            isSyncing = false,
                            lastSyncDisplay = formatLastSync(System.currentTimeMillis()),
                        )
                    }
                    _events.send(Event.SyncSuccess)
                }
                is SyncResult.Error -> {
                    mutableState.update { it.copy(isSyncing = false) }
                    _events.send(Event.SyncError(result.message))
                }
            }
        }
    }

    fun toggleSyncOnStartup(enabled: Boolean) {
        syncPrefs.syncOnStartup().set(enabled)
        mutableState.update { it.copy(syncOnStartup = enabled) }
    }

    fun toggleAutoSync(enabled: Boolean) {
        authPrefs.autoSync().set(enabled)
        mutableState.update { it.copy(autoSync = enabled) }
    }

    fun toggleSyncLibrary(enabled: Boolean) {
        syncPrefs.syncLibrary().set(enabled)
        mutableState.update { it.copy(syncLibrary = enabled) }
    }

    fun toggleSyncChapters(enabled: Boolean) {
        syncPrefs.syncChapters().set(enabled)
        mutableState.update { it.copy(syncChapters = enabled) }
    }

    fun toggleSyncHistory(enabled: Boolean) {
        syncPrefs.syncHistory().set(enabled)
        mutableState.update { it.copy(syncHistory = enabled) }
    }

    fun toggleSyncTracking(enabled: Boolean) {
        syncPrefs.syncTracking().set(enabled)
        mutableState.update { it.copy(syncTracking = enabled) }
    }

    fun toggleSyncCategories(enabled: Boolean) {
        syncPrefs.syncCategories().set(enabled)
        mutableState.update { it.copy(syncCategories = enabled) }
    }

    fun signOut() {
        screenModelScope.launch {
            authService.signOut()
            authPrefs.isLoggedIn().set(false)
            authPrefs.userId().set("")
            authPrefs.userEmail().set("")
            authPrefs.userDisplayName().set("")
            authPrefs.lastSyncTime().set(0L)
            logcat(LogPriority.INFO) { "SyncSettingsScreenModel: signed out" }
            _events.send(Event.SignedOut)
        }
    }

    private fun formatLastSync(timestamp: Long): String {
        if (timestamp == 0L) return "Never synced"
        return try {
            val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            "Last synced: ${sdf.format(Date(timestamp))}"
        } catch (e: Exception) {
            "Last synced: recently"
        }
    }
}
