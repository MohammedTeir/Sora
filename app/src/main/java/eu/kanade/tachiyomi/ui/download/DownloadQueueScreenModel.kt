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
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import eu.kanade.tachiyomi.source.model.Page
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DownloadQueueScreenModel(
    private val downloadManager: DownloadManager = Injekt.get(),
) : ScreenModel {

    val activeDownloads = combine(
        downloadManager.queueState,
        downloadManager.statusFlow().map {}.onStart { emit(Unit) }
    ) { queue, _ ->
        queue.filter { it.status == Download.State.DOWNLOADING }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingDownloads = combine(
        downloadManager.queueState,
        downloadManager.statusFlow().map {}.onStart { emit(Unit) }
    ) { queue, _ ->
        queue.filter { it.status == Download.State.QUEUE || it.status == Download.State.NOT_DOWNLOADED }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _completedDownloads = MutableStateFlow(emptyList<Download>())
    val completedDownloads = _completedDownloads.asStateFlow()

    /**
     * Map of jobs for active downloads.
     */
    private val progressJobs = mutableMapOf<Download, Job>()

    init {
        screenModelScope.launch {
            // Also listen to queueState to detect items that are finished
            downloadManager.statusFlow().collect { download ->
                if (download.status == Download.State.DOWNLOADED) {
                    val current = _completedDownloads.value.toMutableList()
                    if (current.none { it.chapter.id == download.chapter.id }) {
                        current.add(0, download) // Prepend
                        _completedDownloads.value = current
                    }
                }
            }
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
    
    fun moveDownloadToTop(download: Download) {
        val currentQueue = downloadManager.queueState.value.toMutableList()
        if (currentQueue.remove(download)) {
            currentQueue.add(0, download)
            downloadManager.reorderQueue(currentQueue)
        }
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
                .sample(50)
                .collectLatest { /* progress flows handled directly in UI via collectAsState */ }
        }
        progressJobs.remove(download)?.cancel()
        progressJobs[download] = job
    }

    private fun cancelProgressJob(download: Download) {
        progressJobs.remove(download)?.cancel()
    }
}
