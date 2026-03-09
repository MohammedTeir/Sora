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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.hippo.unifile.UniFile
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
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

object SettingsDataScreen : SearchableSettings {

    val restorePreferenceKeyString = MR.strings.label_backup
    const val HELP_URL = "https://mihon.app/docs/faq/storage"

    private val SoraBlue @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
    private val CacheBlue @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    private val FreeGrey @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline
    private val CardBackground @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceVariant
    private val GrowthGreen = Color(0xFF4ADE80)
    private val FireOrange = Color(0xFFFFA000)

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
        val chapterCache = remember { Injekt.get<ChapterCache>() }
        
        val autoClearCache by libraryPreferences.autoClearChapterCache().collectAsState()
        val storageDirPref = storagePreferences.baseStorageDirectory()
        val storageDir by storageDirPref.collectAsState()
        val pickStorageLocation = storageLocationPicker(storageDirPref)

        var totalSpace by remember { mutableLongStateOf(0L) }
        var freeSpace by remember { mutableLongStateOf(0L) }
        var cacheSpace by remember { mutableLongStateOf(0L) }
        var mangaSpace by remember { mutableLongStateOf(0L) }
        var isCalculating by remember { mutableStateOf(true) }
        var refreshTrigger by remember { mutableStateOf(0) }

        LaunchedEffect(storageDir, refreshTrigger) {
            withContext(Dispatchers.IO) {
                isCalculating = true
                val storages = DiskUtil.getExternalStorages(context)
                val primaryStorage = storages.firstOrNull() ?: File(context.filesDir.absolutePath)
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
        val soraBlue = SoraBlue
        val cacheBlue = CacheBlue
        val freeGrey = FreeGrey
        val context = LocalContext.current
        val safeTotal = totalSpace.coerceAtLeast(1L)
        val usedSpace = (safeTotal - freeSpace).coerceAtLeast(0L)
        
        val mangaRatio = (mangaSpace.toFloat() / safeTotal).coerceIn(0f, 1f)
        val cacheRatio = (cacheSpace.toFloat() / safeTotal).coerceIn(0f, 1f)
        val freeRatio = (freeSpace.toFloat() / safeTotal).coerceIn(0f, 1f)
        
        val animManga = remember { Animatable(0f) }
        val animCache = remember { Animatable(0f) }
        val animFree = remember { Animatable(0f) }

        LaunchedEffect(mangaRatio, cacheRatio, freeRatio) {
            animManga.snapTo(0f)
            animCache.snapTo(0f)
            animFree.snapTo(0f)
            if (!isCalculating) {
                animManga.animateTo(mangaRatio * 360f, tween(1000, easing = FastOutSlowInEasing))
                animCache.animateTo(cacheRatio * 360f, tween(1000, easing = FastOutSlowInEasing))
                animFree.animateTo(freeRatio * 360f, tween(1000, easing = FastOutSlowInEasing))
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                val surfaceVariantColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    drawArc(
                        color = surfaceVariantColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    var startAngle = -90f
                    
                    val mangaSweep = animManga.value
                    if (mangaSweep > 0f) {
                        drawArc(
                            color = soraBlue,
                            startAngle = startAngle,
                            sweepAngle = mangaSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        startAngle += mangaSweep
                    }
 
                    val cacheSweep = animCache.value
                    if (cacheSweep > 0f) {
                        drawArc(
                            color = cacheBlue,
                            startAngle = startAngle,
                            sweepAngle = cacheSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        startAngle += cacheSweep
                    }
 
                    val freeSweep = animFree.value
                    if (freeSweep > 0f) {
                        drawArc(
                            color = freeGrey,
                            startAngle = startAngle,
                            sweepAngle = freeSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL USED",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isCalculating) "..." else Formatter.formatFileSize(context, usedSpace),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isCalculating) "OF ..." else "OF ${Formatter.formatFileSize(context, totalSpace).uppercase()}",
                        fontSize = 12.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StorageLegendItem(color = SoraBlue, label = "MANGA", value = if (isCalculating) "..." else Formatter.formatFileSize(context, mangaSpace))
                StorageLegendItem(color = CacheBlue, label = "CACHE", value = if (isCalculating) "..." else Formatter.formatFileSize(context, cacheSpace))
                StorageLegendItem(color = FreeGrey, label = "FREE", value = if (isCalculating) "..." else Formatter.formatFileSize(context, freeSpace))
            }
        }
    }

    @Composable
    private fun StorageLegendItem(color: Color, label: String, value: String) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = label, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, letterSpacing = 1.sp)
                Text(text = value, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
