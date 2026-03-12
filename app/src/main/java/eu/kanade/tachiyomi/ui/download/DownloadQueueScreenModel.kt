package eu.kanade.tachiyomi.ui.download

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.chapter.model.Chapter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DownloadQueueScreenModel(
    private val downloadManager: DownloadManager = Injekt.get(),
) : StateScreenModel<DownloadQueueScreenModel.State>(State()) {

    // ── Derived queue flows ────────────────────────────────────────────────

    /** Trigger that fires whenever any download's status changes. */
    private val statusTrigger = combine(
        downloadManager.queueState,
        downloadManager.statusFlow().map {}.onStart { emit(Unit) },
    ) { _, _ -> Unit }

    /** All non-completed downloads (active + paused + queued). */
    val queuedDownloads = statusTrigger
        .map {
            downloadManager.queueState.value
                .filter { it.status != Download.State.DOWNLOADED }
                .toImmutableList()
        }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    val isDownloaderRunning = downloadManager.isDownloaderRunning
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5_000), false)

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

    fun cancelAll() {
        downloadManager.clearQueue()
        mutableState.update { it.copy(completedDownloads = persistentListOf()) }
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

    // ── State ──────────────────────────────────────────────────────────────

    data class State(
        val completedDownloads: ImmutableList<Download> = persistentListOf(),
    )
}
