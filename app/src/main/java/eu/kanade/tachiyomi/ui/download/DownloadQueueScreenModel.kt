package eu.kanade.tachiyomi.ui.download

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import eu.kanade.tachiyomi.source.model.Page
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DownloadQueueScreenModel(
    private val downloadManager: DownloadManager = Injekt.get(),
) : ScreenModel {

    private val _state = MutableStateFlow(emptyList<DownloadHeaderItem>())
    val state = _state.asStateFlow()

    private val _completedDownloads = MutableStateFlow(emptyList<Download>())
    val completedDownloads = _completedDownloads.asStateFlow()

    /**
     * Map of jobs for active downloads.
     */
    private val progressJobs = mutableMapOf<Download, Job>()

    init {
        screenModelScope.launch {
            downloadManager.queueState
                .map { downloads ->
                    downloads
                        .groupBy { it.source }
                        .map { entry ->
                            DownloadHeaderItem(entry.key.id, entry.key.name, entry.value.size).apply {
                                addSubItems(0, entry.value.map { DownloadItem(it, this) })
                            }
                        }
                }
                .collect { newList -> _state.update { newList } }
        }
    }

    override fun onDispose() {
        for (job in progressJobs.values) {
            job.cancel()
        }
        progressJobs.clear()
    }

    val isDownloaderRunning = downloadManager.isDownloaderRunning
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun startDownloads() {
        downloadManager.startDownloads()
    }

    fun pauseDownloads() {
        downloadManager.pauseDownloads()
    }

    fun clearQueue() {
        downloadManager.clearQueue()
        _completedDownloads.value = emptyList()
    }

    fun clearCompleted() {
        _completedDownloads.value = emptyList()
    }

    fun reorder(downloads: List<Download>) {
        downloadManager.reorderQueue(downloads)
    }

    fun cancel(downloads: List<Download>) {
        downloadManager.cancelQueuedDownloads(downloads)
    }

    fun <R : Comparable<R>> reorderQueue(selector: (DownloadItem) -> R, reverse: Boolean = false) {
        val currentList = _state.value
        val newDownloads = mutableListOf<Download>()
        currentList.forEach { headerItem ->
            val sorted = headerItem.subItems.sortedBy(selector).toMutableList().apply {
                if (reverse) reverse()
            }
            headerItem.subItems = sorted
            newDownloads.addAll(sorted.map { it.download })
        }
        reorder(newDownloads)
    }

    fun onStatusChange(download: Download) {
        when (download.status) {
            Download.State.DOWNLOADING -> {
                launchProgressJob(download)
            }
            Download.State.DOWNLOADED -> {
                cancelProgressJob(download)
                // Add to completed list
                val current = _completedDownloads.value.toMutableList()
                if (current.none { it.chapter.id == download.chapter.id }) {
                    current.add(0, download) // Prepend
                    _completedDownloads.value = current
                }
            }
            Download.State.ERROR -> {
                cancelProgressJob(download)
            }
            else -> { /* unused */ }
        }
    }

    private fun launchProgressJob(download: Download) {
        val job = screenModelScope.launch {
            while (download.pages == null) {
                delay(50)
            }
            val progressFlows = download.pages!!.map(Page::progressFlow)
            combine(progressFlows, Array<Int>::sum)
                .distinctUntilChanged()
                .debounce(50)
                .collectLatest { /* progress flows handled directly in UI via collectAsState */ }
        }
        progressJobs.remove(download)?.cancel()
        progressJobs[download] = job
    }

    private fun cancelProgressJob(download: Download) {
        progressJobs.remove(download)?.cancel()
    }
}
