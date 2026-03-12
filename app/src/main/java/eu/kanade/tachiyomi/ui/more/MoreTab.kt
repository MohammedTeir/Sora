package eu.kanade.tachiyomi.ui.more

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.core.preference.asState
import eu.kanade.domain.auth.AuthPreferences
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.presentation.more.MoreScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.data.auth.FirebaseAuthService
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.sync.SyncWorker
import eu.kanade.tachiyomi.ui.auth.LoginScreen
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.settings.sync.SyncSettingsScreen
import eu.kanade.tachiyomi.ui.stats.StatsScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data object MoreTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            return TabOptions(
                index = 5u,
                title = stringResource(MR.strings.label_more),
                icon = rememberVectorPainter(Icons.Outlined.MoreHoriz),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(SettingsScreen())
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { MoreScreenModel() }
        val downloadQueueState by screenModel.downloadQueueState.collectAsState()
        val authState by screenModel.authState.collectAsState()

        MoreScreen(
            downloadQueueStateProvider = { downloadQueueState },
            downloadedOnly = screenModel.downloadedOnly,
            onDownloadedOnlyChange = { screenModel.downloadedOnly = it },
            incognitoMode = screenModel.incognitoMode,
            onIncognitoModeChange = { screenModel.incognitoMode = it },
            onClickDownloadQueue = { navigator.push(DownloadQueueScreen) },
            onClickCategories = { navigator.push(CategoryScreen()) },
            onClickStats = { navigator.push(StatsScreen()) },
            onClickDataAndStorage = { navigator.push(SettingsScreen(SettingsScreen.Destination.DataAndStorage)) },
            onClickSettings = { navigator.push(SettingsScreen()) },
            onClickAbout = { navigator.push(SettingsScreen(SettingsScreen.Destination.About)) },
            // Auth
            isLoggedIn = authState.isLoggedIn,
            userDisplayName = authState.userDisplayName,
            userEmail = authState.userEmail,
            lastSyncDisplay = authState.lastSyncDisplay,
            isSyncing = authState.isSyncing,
            onClickProfile = {
                if (authState.isLoggedIn) {
                    navigator.push(SyncSettingsScreen())
                } else {
                    navigator.push(LoginScreen())
                }
            },
            onClickSignOut = {
                screenModel.signOut(context)
            },
            onClickCloudSync = { navigator.push(SyncSettingsScreen()) },
        )
    }
}

private class MoreScreenModel(
    private val downloadManager: DownloadManager = Injekt.get(),
    preferences: BasePreferences = Injekt.get(),
    private val authPrefs: AuthPreferences = Injekt.get(),
    private val syncPrefs: SyncPreferences = Injekt.get(),
    private val authService: FirebaseAuthService = Injekt.get(),
) : ScreenModel {

    var downloadedOnly by preferences.downloadedOnly().asState(screenModelScope)
    var incognitoMode by preferences.incognitoMode().asState(screenModelScope)

    private var _downloadQueueState: MutableStateFlow<DownloadQueueState> = MutableStateFlow(DownloadQueueState.Stopped)
    val downloadQueueState: StateFlow<DownloadQueueState> = _downloadQueueState.asStateFlow()

    private val _authState = MutableStateFlow(buildAuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Handle running/paused status change and queue progress updating
        screenModelScope.launchIO {
            combine(
                downloadManager.isDownloaderRunning,
                downloadManager.queueState,
            ) { isRunning, downloadQueue -> Pair(isRunning, downloadQueue.size) }
                .collectLatest { (isDownloading, downloadQueueSize) ->
                    val pendingDownloadExists = downloadQueueSize != 0
                    _downloadQueueState.value = when {
                        !pendingDownloadExists -> DownloadQueueState.Stopped
                        !isDownloading -> DownloadQueueState.Paused(downloadQueueSize)
                        else -> DownloadQueueState.Downloading(downloadQueueSize)
                    }
                }
        }

        // Observe auth preference changes to refresh state
        screenModelScope.launchIO {
            authPrefs.isLoggedIn().changes().collect {
                _authState.value = buildAuthState()
            }
        }
    }

    fun signOut(context: android.content.Context) {
        screenModelScope.launchIO {
            authService.signOut()
            authPrefs.isLoggedIn().set(false)
            authPrefs.userId().set("")
            authPrefs.userEmail().set("")
            authPrefs.userDisplayName().set("")
            authPrefs.lastSyncTime().set(0L)
            SyncWorker.cancelAllSync(context)
            _authState.value = buildAuthState()
        }
    }

    private fun buildAuthState(): AuthState {
        val isLoggedIn = authPrefs.isLoggedIn().get()
        val lastSyncTime = authPrefs.lastSyncTime().get()
        return AuthState(
            isLoggedIn = isLoggedIn,
            userDisplayName = authPrefs.userDisplayName().get(),
            userEmail = authPrefs.userEmail().get(),
            lastSyncDisplay = formatLastSync(lastSyncTime),
            isSyncing = syncPrefs.isSyncing().get(),
        )
    }

    private fun formatLastSync(timestamp: Long): String {
        if (timestamp == 0L) return ""
        return try {
            val sdf = SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault())
            "Synced ${sdf.format(Date(timestamp))}"
        } catch (e: Exception) {
            ""
        }
    }

    data class AuthState(
        val isLoggedIn: Boolean,
        val userDisplayName: String,
        val userEmail: String,
        val lastSyncDisplay: String,
        val isSyncing: Boolean,
    )
}

sealed interface DownloadQueueState {
    data object Stopped : DownloadQueueState
    data class Paused(val pending: Int) : DownloadQueueState
    data class Downloading(val pending: Int) : DownloadQueueState
}
