package eu.kanade.tachiyomi.ui.download

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.download.service.DownloadPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DownloadQueueScreenModel(
    private val downloadManager: DownloadManager = Injekt.get(),
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
) : StateScreenModel<DownloadQueueScreenModel.State>(State()) {

    // ── Derived queue flows ────────────────────────────────────────────────

    /**
     * Trigger that fires whenever:
     *  - The queue list itself changes (items added/removed/reordered)
     *  - ANY item's status changes (QUEUE → DOWNLOADING → PAUSED → ERROR …)
     *
     * We flatMap over EVERY item's statusFlow so ALL state transitions are
     * captured, not just DOWNLOADING ones.
     */
    private val statusTrigger = downloadManager.queueState
        .flatMapLatest { downloads ->
            val statusStreams = downloads.map { dl -> dl.statusFlow.map { dl } }
            merge(*statusStreams.toTypedArray())
                .onStart { emit(downloads.firstOrNull() ?: return@onStart) }
        }
        .map { } // only need the trigger signal

    /** All non-completed downloads (active + paused + queued + error). */
    val queuedDownloads = statusTrigger
        .map {
            downloadManager.queueState.value
                .filter { it.status != Download.State.DOWNLOADED }
                .toImmutableList()
        }
        .onStart {
            emit(
                downloadManager.queueState.value
                    .filter { it.status != Download.State.DOWNLOADED }
                    .toImmutableList(),
            )
        }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    val isDownloaderRunning = downloadManager.isDownloaderRunning
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val parallelLimit = downloadPreferences.parallelSourceLimit().changes()
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            downloadPreferences.parallelSourceLimit().get(),
        )

    init {
        // Collect completed items for the "Recently Completed" section
        screenModelScope.launch {
            downloadManager.statusFlow().collect { download ->
                if (download.status == Download.State.DOWNLOADED) {
                    mutableState.update { state ->
                        val updated = (listOf(download) + state.completedDownloads
                            .filter { it.chapter.id != download.chapter.id })
                            .take(20)
                            .toImmutableList()
                        state.copy(completedDownloads = updated)
                    }
                }
            }
        }
    }

    // ── Global queue actions ───────────────────────────────────────────────

    fun resumeAll() = downloadManager.resumeAllDownloads()

    fun pauseAll() = downloadManager.pauseDownloads()

    /**
     * Cancels all queued downloads WITHOUT touching the "Recently Completed"
     * section — those are already done and should remain visible.
     */
    fun cancelAll() {
        downloadManager.clearQueue()
        // intentionally NOT clearing completedDownloads
    }

    fun clearCompleted() {
        mutableState.update { it.copy(completedDownloads = persistentListOf()) }
    }

    // ── Per-item actions ───────────────────────────────────────────────────

    fun resumeDownload(chapter: Chapter) = downloadManager.resumeDownload(chapter)

    fun pauseDownload(chapter: Chapter) = downloadManager.pauseDownload(chapter)

    fun cancelDownload(chapter: Chapter) = downloadManager.cancelDownload(chapter)

    // ── Queue ordering ─────────────────────────────────────────────────────

    fun moveToTop(download: Download) {
        val current = downloadManager.queueState.value.toMutableList()
        if (current.remove(download)) {
            current.add(0, download)
            downloadManager.reorderQueue(current)
        }
    }

    fun moveToBottom(download: Download) = downloadManager.moveDownloadToBottom(download)

    fun moveSeriesToTop(mangaId: Long) = downloadManager.moveSeriesToTop(mangaId)

    fun moveSeriesToBottom(mangaId: Long) = downloadManager.moveSeriesToBottom(mangaId)

    fun reorder(downloads: List<Download>) = downloadManager.reorderQueue(downloads)

    // ── Series-level cancel ────────────────────────────────────────────────

    fun cancelSeries(mangaId: Long) = downloadManager.cancelAllForManga(mangaId)

    // ── Parallel limit ─────────────────────────────────────────────────────

    fun setParallelLimit(value: Int) {
        downloadPreferences.parallelSourceLimit().set(value)
    }

    // ── State ──────────────────────────────────────────────────────────────

    data class State(
        val completedDownloads: ImmutableList<Download> = persistentListOf(),
    )
}
