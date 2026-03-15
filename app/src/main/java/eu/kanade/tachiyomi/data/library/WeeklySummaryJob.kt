package eu.kanade.tachiyomi.data.library

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.notify
import eu.kanade.tachiyomi.util.system.workManager
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.interactor.GetLibraryManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

class WeeklySummaryJob(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val getLibraryManga: GetLibraryManga = Injekt.get()
    private val chapterRepository: ChapterRepository = Injekt.get()

    override suspend fun doWork(): Result {
        val libraryManga = getLibraryManga.await()

        var totalUnread = 0
        for (libraryItem in libraryManga) {
            val chapters = chapterRepository.getChapterByMangaId(libraryItem.manga.id)
            totalUnread += chapters.count { !it.read }
        }

        if (totalUnread > 0) {
            showWeeklyDigestNotification(totalUnread)
        }

        return Result.success()
    }

    private fun showWeeklyDigestNotification(unreadCount: Int) {
        val text = "You have $unreadCount unread chapters waiting — perfect weekend reading!"
        val notification = NotificationCompat.Builder(applicationContext, Notifications.CHANNEL_WEEKLY_DIGEST)
            .setContentTitle("Weekly Reading Digest")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_mihon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        applicationContext.notify(Notifications.ID_WEEKLY_DIGEST, notification)
    }

    companion object {
        const val TAG = "WeeklySummaryJob"
        private const val WORK_NAME = "WeeklySummaryJob"

        fun setup(context: Context) {
            // Calculate initial delay to next Friday at 7 PM
            val now = LocalDateTime.now()
            val nextFriday7PM = now
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
                .withHour(19)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .let { if (it.isBefore(now) || it.isEqual(now)) it.plusWeeks(1) else it }

            val initialDelay = Duration.between(now, nextFriday7PM).toMinutes()

            val request = PeriodicWorkRequestBuilder<WeeklySummaryJob>(7, TimeUnit.DAYS)
                .addTag(TAG)
                .setInitialDelay(initialDelay, TimeUnit.MINUTES)
                .build()

            context.workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
