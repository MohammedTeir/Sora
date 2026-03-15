package eu.kanade.tachiyomi.ui.stats

import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFilter
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.util.fastCountNot
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.presentation.more.stats.data.StatsData
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.history.interactor.GetTotalReadDuration
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Calendar
import java.util.concurrent.TimeUnit

class StatsScreenModel(
    private val downloadManager: DownloadManager = Injekt.get(),
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
    private val getTotalReadDuration: GetTotalReadDuration = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val preferences: LibraryPreferences = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
    private val historyRepository: HistoryRepository = Injekt.get(),
) : StateScreenModel<StatsScreenState>(StatsScreenState.Loading) {

    private val loggedInTrackers by lazy { trackerManager.loggedInTrackers() }

    init {
        screenModelScope.launchIO {
            val libraryManga = getLibraryManga.await()

            val distinctLibraryManga = libraryManga.fastDistinctBy { it.id }

            val mangaTrackMap = getMangaTrackMap(distinctLibraryManga)
            val scoredMangaTrackerMap = getScoredMangaTrackMap(mangaTrackMap)

            val meanScore = getTrackMeanScore(scoredMangaTrackerMap)

            val overviewStatData = StatsData.Overview(
                libraryMangaCount = distinctLibraryManga.size,
                completedMangaCount = distinctLibraryManga.count {
                    it.manga.status.toInt() == SManga.COMPLETED && it.unreadCount == 0L
                },
                totalReadDuration = getTotalReadDuration.await(),
            )

            val titlesStatData = StatsData.Titles(
                globalUpdateItemCount = getGlobalUpdateItemCount(libraryManga),
                startedMangaCount = distinctLibraryManga.count { it.hasStarted },
                localMangaCount = distinctLibraryManga.count { it.manga.isLocal() },
            )

            val chaptersStatData = StatsData.Chapters(
                totalChapterCount = distinctLibraryManga.sumOf { it.totalChapters }.toInt(),
                readChapterCount = distinctLibraryManga.sumOf { it.readCount }.toInt(),
                downloadCount = downloadManager.getDownloadCount(),
            )

            val trackersStatData = StatsData.Trackers(
                trackedTitleCount = mangaTrackMap.count { it.value.isNotEmpty() },
                meanScore = meanScore,
                trackerCount = loggedInTrackers.size,
            )

            val weeklyActivity = computeWeeklyActivity()
            val statusDistribution = computeStatusDistribution(distinctLibraryManga)
            val topManga = computeTopManga(distinctLibraryManga)
            val readingStreak = computeReadingStreak()

            mutableState.update {
                StatsScreenState.Success(
                    overview = overviewStatData,
                    titles = titlesStatData,
                    chapters = chaptersStatData,
                    trackers = trackersStatData,
                    weeklyActivity = weeklyActivity,
                    statusDistribution = statusDistribution,
                    topManga = topManga,
                    readingStreak = readingStreak,
                )
            }
        }
    }

    private suspend fun computeWeeklyActivity(): StatsData.WeeklyActivity {
        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val historyEntries = historyRepository.getReadingHistorySince(sevenDaysAgo)

        val dayDurationMap = mutableMapOf<Long, Long>()
        for ((readAtMs, duration) in historyEntries) {
            val dayKey = getStartOfDay(readAtMs)
            dayDurationMap[dayKey] = (dayDurationMap[dayKey] ?: 0L) + duration
        }

        val today = getStartOfDay(System.currentTimeMillis())
        val dailyData = (6 downTo 0).map { offset ->
            val dayMs = today - TimeUnit.DAYS.toMillis(offset.toLong())
            StatsData.DailyReadingActivity(
                dayMs = dayMs,
                totalReadDuration = dayDurationMap[dayMs] ?: 0L,
            )
        }

        return StatsData.WeeklyActivity(dailyData = dailyData)
    }

    private suspend fun computeReadingStreak(): StatsData.ReadingStreak {
        val allDates = historyRepository.getAllReadingDates()
        if (allDates.isEmpty()) {
            return StatsData.ReadingStreak(currentStreak = 0, longestStreak = 0)
        }

        val distinctDays = allDates
            .map { getStartOfDay(it) }
            .distinct()
            .sorted()

        val today = getStartOfDay(System.currentTimeMillis())
        val yesterday = today - TimeUnit.DAYS.toMillis(1)

        var longestStreak = 1
        var currentRun = 1
        for (i in 1 until distinctDays.size) {
            val diff = distinctDays[i] - distinctDays[i - 1]
            if (diff == TimeUnit.DAYS.toMillis(1)) {
                currentRun++
                if (currentRun > longestStreak) longestStreak = currentRun
            } else {
                currentRun = 1
            }
        }

        val lastReadDay = distinctDays.last()
        val currentStreak = if (lastReadDay == today || lastReadDay == yesterday) {
            var streak = 1
            var idx = distinctDays.size - 2
            while (idx >= 0) {
                val diff = distinctDays[idx + 1] - distinctDays[idx]
                if (diff == TimeUnit.DAYS.toMillis(1)) {
                    streak++
                    idx--
                } else {
                    break
                }
            }
            streak
        } else {
            0
        }

        return StatsData.ReadingStreak(
            currentStreak = currentStreak,
            longestStreak = maxOf(longestStreak, currentStreak),
        )
    }

    private fun computeStatusDistribution(libraryManga: List<LibraryManga>): StatsData.StatusDistribution {
        val statusCounts = libraryManga
            .groupBy { it.manga.status.toInt() }
            .mapValues { it.value.size }
        return StatsData.StatusDistribution(statusCounts = statusCounts)
    }

    private fun computeTopManga(libraryManga: List<LibraryManga>): StatsData.TopManga {
        val items = libraryManga
            .filter { it.readCount > 0 }
            .sortedByDescending { it.readCount }
            .take(3)
            .map { lm ->
                StatsData.TopMangaItem(
                    mangaId = lm.id,
                    title = lm.manga.title,
                    readCount = lm.readCount,
                    coverData = lm.manga.asMangaCover(),
                )
            }
        return StatsData.TopManga(items = items)
    }

    private fun getStartOfDay(timestampMs: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestampMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getGlobalUpdateItemCount(libraryManga: List<LibraryManga>): Int {
        val includedCategories = preferences.updateCategories().get().map { it.toLong() }
        val excludedCategories = preferences.updateCategoriesExclude().get().map { it.toLong() }
        val updateRestrictions = preferences.autoUpdateMangaRestrictions().get()

        return libraryManga.filter {
            val included = includedCategories.isEmpty() || it.categories.intersect(includedCategories).isNotEmpty()
            val excluded = it.categories.intersect(excludedCategories).isNotEmpty()
            included && !excluded
        }
            .fastCountNot {
                (MANGA_NON_COMPLETED in updateRestrictions && it.manga.status.toInt() == SManga.COMPLETED) ||
                    (MANGA_HAS_UNREAD in updateRestrictions && it.unreadCount != 0L) ||
                    (MANGA_NON_READ in updateRestrictions && it.totalChapters > 0 && !it.hasStarted)
            }
    }

    private suspend fun getMangaTrackMap(libraryManga: List<LibraryManga>): Map<Long, List<Track>> {
        val loggedInTrackerIds = loggedInTrackers.map { it.id }.toHashSet()
        return libraryManga.associate { manga ->
            val tracks = getTracks.await(manga.id)
                .fastFilter { it.trackerId in loggedInTrackerIds }

            manga.id to tracks
        }
    }

    private fun getScoredMangaTrackMap(mangaTrackMap: Map<Long, List<Track>>): Map<Long, List<Track>> {
        return mangaTrackMap.mapNotNull { (mangaId, tracks) ->
            val trackList = tracks.mapNotNull { track ->
                track.takeIf { it.score > 0.0 }
            }
            if (trackList.isEmpty()) return@mapNotNull null
            mangaId to trackList
        }.toMap()
    }

    private fun getTrackMeanScore(scoredMangaTrackMap: Map<Long, List<Track>>): Double {
        return scoredMangaTrackMap
            .map { (_, tracks) ->
                tracks.map(::get10PointScore).average()
            }
            .fastFilter { !it.isNaN() }
            .average()
    }

    private fun get10PointScore(track: Track): Double {
        val service = trackerManager.get(track.trackerId)!!
        return service.get10PointScore(track)
    }
}
