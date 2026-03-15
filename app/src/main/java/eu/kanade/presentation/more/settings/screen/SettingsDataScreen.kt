package eu.kanade.presentation.more.settings.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.hippo.unifile.UniFile
import eu.kanade.presentation.theme.SoraBlue
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.displayablePath
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import eu.kanade.presentation.more.settings.screen.data.CreateBackupScreen
import eu.kanade.presentation.more.settings.screen.data.RestoreBackupScreen
import eu.kanade.tachiyomi.data.backup.create.BackupCreateJob
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.data.export.LibraryExporter
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.model.Manga
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.util.system.DeviceUtil

object SettingsDataScreen : SearchableSettings {

    val restorePreferenceKeyString = MR.strings.label_backup
    const val HELP_URL = "https://mihon.app/docs/faq/storage"

    private val SoraBlue @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
    private val FreeGrey @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline
    private val CardBackground @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceVariant
    private val GrowthGreen = Color(0xFF4ADE80)
    private val FireOrange = Color(0xFFFFA000)

    // Storage chart — high-contrast palette so every slice is distinguishable
    private val MangaColor @Composable get() = SoraBlue
    private val CacheColor  = Color(0xFFFFB300)  // Amber
    private val OtherColor  = Color(0xFFEF5350)  // Coral Red
    private val FreeColor   = Color(0xFF78909C)  // Blue-Grey

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.label_data_storage

    @Composable
    override fun getPreferences(): List<Preference> = emptyList()

    @Composable
    fun storageLocationPicker(
        storageDirPref: tachiyomi.core.common.preference.Preference<String>,
    ): ManagedActivityResultLauncher<Uri?, Uri?> {
        val context = LocalContext.current
        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            if (uri != null) {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                } catch (e: SecurityException) {
                    logcat(LogPriority.ERROR, e)
                    context.toast(MR.strings.file_picker_uri_permission_unsupported)
                }
                UniFile.fromUri(context, uri)?.let {
                    storageDirPref.set(it.uri.toString())
                }
            }
        }
    }

    @Composable
    fun storageLocationText(
        storageDirPref: tachiyomi.core.common.preference.Preference<String>,
    ): String {
        val context = LocalContext.current
        val storageDir by storageDirPref.collectAsState()
        if (storageDir == storageDirPref.defaultValue()) {
            return stringResource(MR.strings.no_location_set)
        }
        return remember(storageDir) {
            val file = UniFile.fromUri(context, storageDir.toUri())
            file?.displayablePath
        } ?: stringResource(MR.strings.invalid_location, storageDir)
    }

    private fun getUniFileSize(uniFile: UniFile?): Long {
        if (uniFile == null) return 0L
        if (uniFile.isFile) return uniFile.length()
        var size = 0L
        uniFile.listFiles()?.forEach { child ->
            size += getUniFileSize(child)
        }
        return size
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val handleBack = LocalBackPress.current
        val scope = rememberCoroutineScope()
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
        val storagePreferences = remember { Injekt.get<StoragePreferences>() }
        val backupPreferences = remember { Injekt.get<BackupPreferences>() }
        val getFavorites = remember { Injekt.get<GetFavorites>() }
        val chapterCache = remember { Injekt.get<ChapterCache>() }
        
        val autoClearCache by libraryPreferences.autoClearChapterCache().collectAsState()
        val storageDirPref = storagePreferences.baseStorageDirectory()
        val storageDir by storageDirPref.collectAsState()
        val pickStorageLocation = storageLocationPicker(storageDirPref)
        
        val lastAutoBackup by backupPreferences.lastAutoBackupTimestamp().collectAsState()

        val chooseBackup = rememberLauncherForActivityResult(
            object : ActivityResultContracts.GetContent() {
                override fun createIntent(context: Context, input: String): Intent {
                    val intent = super.createIntent(context, input)
                    return Intent.createChooser(intent, context.stringResource(MR.strings.file_select_backup))
                }
            },
        ) {
            if (it == null) {
                context.toast(MR.strings.file_null_uri_error)
                return@rememberLauncherForActivityResult
            }
            navigator.push(RestoreBackupScreen(it.toString()))
        }

        var favorites by remember { mutableStateOf<List<Manga>>(emptyList()) }
        LaunchedEffect(Unit) {
            favorites = getFavorites.await()
        }

        val saveFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/csv"),
        ) { uri ->
            uri?.let {
                scope.launch {
                    LibraryExporter.exportToCsv(
                        context = context,
                        uri = it,
                        favorites = favorites,
                        options = LibraryExporter.ExportOptions(includeTitle = true, includeAuthor = true, includeArtist = true),
                        onExportComplete = {
                            scope.launch(Dispatchers.Main) {
                                context.toast(MR.strings.library_exported)
                            }
                        },
                    )
                }
            }
        }

        var totalSpace by remember { mutableLongStateOf(0L) }
        var freeSpace by remember { mutableLongStateOf(0L) }
        var cacheSpace by remember { mutableLongStateOf(0L) }
        var mangaSpace by remember { mutableLongStateOf(0L) }
        var isCalculating by remember { mutableStateOf(true) }
        var refreshTrigger by remember { mutableStateOf(0) }

        LaunchedEffect(storageDir, refreshTrigger) {
            withContext(Dispatchers.IO) {
                isCalculating = true
                // Use context.filesDir's parent or externalFilesDir if available for accurate storage info
                val primaryStorage = context.getExternalFilesDir(null)?.parentFile?.parentFile?.parentFile
                    ?: File(context.filesDir.absolutePath)
                totalSpace = DiskUtil.getTotalStorageSpace(primaryStorage)
                freeSpace = DiskUtil.getAvailableStorageSpace(primaryStorage)
                val cacheDir = File(context.cacheDir, "chapter_disk_cache")
                cacheSpace = DiskUtil.getDirectorySize(cacheDir)

                val baseFile = UniFile.fromUri(context, storageDir.toUri())
                val downloadsFile = baseFile?.findFile("downloads")
                mangaSpace = getUniFileSize(downloadsFile)
                isCalculating = false
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Data & Storage",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { 
                                if (handleBack != null) handleBack.invoke() 
                                else navigator.pop() 
                            },
                            modifier = Modifier
                                .padding(start = 8.dp, end = 8.dp)
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                StorageChart(
                    totalSpace = totalSpace,
                    freeSpace = freeSpace,
                    cacheSpace = cacheSpace,
                    mangaSpace = mangaSpace,
                    isCalculating = isCalculating
                )

                Spacer(modifier = Modifier.height(32.dp))

                SectionTitle(title = "BACKUP & EXPORT")
                Spacer(modifier = Modifier.height(16.dp))
                CardContainer {
                    LocationItem(
                        iconVector = Icons.Outlined.Save,
                        title = "Create Backup",
                        subtitle = "Manually backup your library",
                        onClick = { navigator.push(CreateBackupScreen()) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LocationItem(
                        iconVector = Icons.Outlined.SettingsBackupRestore,
                        title = "Restore Backup",
                        subtitle = "Restore library from a backup file",
                        onClick = {
                            if (!BackupRestoreJob.isRunning(context)) {
                                if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                                    context.toast(MR.strings.restore_miui_warning)
                                }
                                chooseBackup.launch("*/*")
                            } else {
                                context.toast(MR.strings.restore_in_progress)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    run {
                        val backupInterval by backupPreferences.backupInterval().collectAsState()
                        val intervalLabel = when (backupInterval) {
                            0 -> "Disabled"
                            6 -> "Every 6 hours"
                            12 -> "Every 12 hours"
                            24 -> "Daily"
                            48 -> "Every 2 days"
                            168 -> "Weekly"
                            else -> "Every $backupInterval hours"
                        }
                        LocationItem(
                            iconVector = Icons.Outlined.Timer,
                            title = "Automatic Backup",
                            subtitle = intervalLabel,
                            onClick = {
                                // Cycle through preset intervals: 0 -> 6 -> 12 -> 24 -> 48 -> 168 -> 0
                                val next = when (backupInterval) {
                                    0 -> 6
                                    6 -> 12
                                    12 -> 24
                                    24 -> 48
                                    48 -> 168
                                    else -> 0
                                }
                                backupPreferences.backupInterval().set(next)
                                BackupCreateJob.setupTask(context, next)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LocationItem(
                        iconVector = Icons.Outlined.ImportExport,
                        title = "Export Library List",
                        subtitle = "Export your library as a CSV file",
                        onClick = { saveFileLauncher.launch("mihon_library.csv") }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                SectionTitle(title = "DOWNLOADS")
                Spacer(modifier = Modifier.height(16.dp))
                CardContainer {
                    LocationItem(
                        iconVector = Icons.Outlined.Folder,
                        title = "Download Location",
                        subtitle = storageLocationText(storageDirPref),
                        onClick = {
                            try {
                                pickStorageLocation.launch(null)
                            } catch (e: ActivityNotFoundException) {
                                context.toast(MR.strings.file_picker_error)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ToggleItem(
                        iconVector = Icons.Outlined.DeleteOutline,
                        title = "Auto-delete",
                        subtitle = "Remove finished chapters",
                        isChecked = autoClearCache,
                        onCheckedChange = { libraryPreferences.autoClearChapterCache().set(it) }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                SectionTitle(title = "PERFORMANCE")
                Spacer(modifier = Modifier.height(16.dp))
                CardContainer {
                    ActionItem(
                        iconVector = Icons.Outlined.Speed,
                        title = "Current Cache",
                        subtitle = "Reduces loading times",
                        trailingText = if (isCalculating) "..." else Formatter.formatFileSize(context, cacheSpace),
                        onClick = {
                            scope.launchNonCancellable {
                                try {
                                    val deletedFiles = chapterCache.clear()
                                    withUIContext {
                                        context.toast(context.stringResource(MR.strings.cache_deleted, deletedFiles))
                                        refreshTrigger++
                                    }
                                } catch (e: Throwable) {
                                    logcat(LogPriority.ERROR, e)
                                    withUIContext { context.toast(context.stringResource(MR.strings.cache_delete_error)) }
                                }
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    @Composable
    private fun StorageChart(
        totalSpace: Long,
        freeSpace: Long,
        cacheSpace: Long,
        mangaSpace: Long,
        isCalculating: Boolean
    ) {
        val context = LocalContext.current
        val safeTotal = totalSpace.coerceAtLeast(1L)
        val usedSpace = (safeTotal - freeSpace).coerceAtLeast(0L)
        val otherSpace = (usedSpace - mangaSpace - cacheSpace).coerceAtLeast(0L)

        // ----- Minimum visible slice angle (degrees) --------------------------------
        // Any non-zero category gets at least MIN_SWEEP degrees so it stays visible
        // even when its real share of disk is < 1%.
        val MIN_SWEEP = 10f
        val GAP_SWEEP = 2f   // gap between slices for visual separation

        data class SliceData(val bytes: Long, val color: Color, val label: String)
        val rawSlices = listOf(
            SliceData(mangaSpace, MangaColor, "MANGA"),
            SliceData(cacheSpace, CacheColor,  "CACHE"),
            SliceData(otherSpace, OtherColor,  "OTHER"),
            SliceData(freeSpace,  FreeColor,   "FREE"),
        )
        val nonZeroCount = rawSlices.count { it.bytes > 0 }
        val totalGap = GAP_SWEEP * nonZeroCount
        val budgetAfterMinAndGap = (360f - totalGap - MIN_SWEEP * nonZeroCount).coerceAtLeast(0f)
        val bytesTotal = rawSlices.sumOf { it.bytes }.coerceAtLeast(1L)

        // Final sweep for each slice: minimum + proportional share of remaining budget
        val finalSweeps = rawSlices.map { slice ->
            if (slice.bytes == 0L) 0f
            else MIN_SWEEP + (slice.bytes.toFloat() / bytesTotal) * budgetAfterMinAndGap
        }

        // Animated sweep values — one Animatable per slice
        val animValues = remember { List(4) { Animatable(0f) } }
        LaunchedEffect(finalSweeps, isCalculating) {
            animValues.forEach { it.snapTo(0f) }
            if (!isCalculating) {
                finalSweeps.forEachIndexed { i, target ->
                    animValues[i].animateTo(target, tween(900, delayMillis = i * 120, easing = FastOutSlowInEasing))
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Donut ring ───────────────────────────────────────────────────────────
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 28.dp.toPx()
                    val inset = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(inset, inset)

                    // Background track
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )

                    // Colored slices with gap between them
                    var startAngle = -90f
                    rawSlices.forEachIndexed { i, slice ->
                        val sweep = animValues[i].value
                        if (sweep > 0f) {
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            startAngle += sweep + GAP_SWEEP
                        }
                    }
                }

                // Centre label
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL USED",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isCalculating) "…" else Formatter.formatFileSize(context, usedSpace),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isCalculating) "of …" else "of ${Formatter.formatFileSize(context, totalSpace)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Legend — vertical cards in a 2×2 grid ───────────────────────────────
            val legendItems = listOf(
                Triple(MangaColor, "MANGA",  mangaSpace),
                Triple(CacheColor,  "CACHE",  cacheSpace),
                Triple(OtherColor,  "OTHER",  otherSpace),
                Triple(FreeColor,   "FREE",   freeSpace),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                legendItems.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { (color, label, bytes) ->
                            val percent = if (safeTotal > 0) (bytes.toFloat() / safeTotal * 100f) else 0f
                            val displayPct = if (percent < 0.1f && bytes > 0L) "<1" else "%.0f".format(percent)
                            StorageLegendCard(
                                color = color,
                                label = label,
                                value = if (isCalculating) "…" else Formatter.formatFileSize(context, bytes),
                                percent = if (isCalculating) "…" else "$displayPct%",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun StorageLegendCard(
        color: Color,
        label: String,
        value: String,
        percent: String,
        modifier: Modifier = Modifier,
    ) {
        Row(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Colored square indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // Percentage badge
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = percent,
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    @Composable
    private fun SectionTitle(title: String) {
        Text(
            text = title,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
    }

    @Composable
    private fun CardContainer(content: @Composable ColumnScope.() -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                .padding(20.dp),
            content = content
        )
    }

    @Composable
    private fun LocationItem(
        iconVector: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        subtitle: String,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SoraBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = iconVector, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                Text(text = subtitle, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
            }
            Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    @Composable
    private fun ToggleItem(
        iconVector: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        subtitle: String,
        isChecked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SoraBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = iconVector, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                Text(text = subtitle, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SoraBlue,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }

    @Composable
    private fun ActionItem(
        iconVector: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        subtitle: String,
        trailingText: String,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SoraBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = iconVector, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                Text(text = subtitle, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Text(text = trailingText, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
