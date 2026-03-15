package eu.kanade.presentation.more.stats.data

import tachiyomi.domain.manga.model.MangaCover

sealed interface StatsData {

    data class Overview(
        val libraryMangaCount: Int,
        val completedMangaCount: Int,
        val totalReadDuration: Long,
    ) : StatsData

    data class Titles(
        val globalUpdateItemCount: Int,
        val startedMangaCount: Int,
        val localMangaCount: Int,
    ) : StatsData

    data class Chapters(
        val totalChapterCount: Int,
        val readChapterCount: Int,
        val downloadCount: Int,
    ) : StatsData

    data class Trackers(
        val trackedTitleCount: Int,
        val meanScore: Double,
        val trackerCount: Int,
    ) : StatsData

    data class DailyReadingActivity(
        val dayMs: Long,
        val totalReadDuration: Long,
    )

    data class WeeklyActivity(
        val dailyData: List<DailyReadingActivity>,
    ) : StatsData

    data class StatusDistribution(
        val statusCounts: Map<Int, Int>,
    ) : StatsData

    data class TopMangaItem(
        val mangaId: Long,
        val title: String,
        val readCount: Long,
        val coverData: MangaCover,
    )

    data class TopManga(
        val items: List<TopMangaItem>,
    ) : StatsData

    data class ReadingStreak(
        val currentStreak: Int,
        val longestStreak: Int,
    ) : StatsData
}
