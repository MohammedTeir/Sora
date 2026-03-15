package eu.kanade.tachiyomi.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.auth.FirebaseAuthService
import logcat.LogPriority
import logcat.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val syncService: SyncService = Injekt.get()
        val authService: FirebaseAuthService = Injekt.get()

        if (!authService.isLoggedIn()) {
            logcat(LogPriority.INFO) { "SyncWorker: user not logged in, skipping" }
            return Result.success()
        }

        logcat(LogPriority.INFO) { "SyncWorker: starting background sync" }

        return when (val result = syncService.syncNow()) {
            is SyncResult.Success -> {
                logcat(LogPriority.INFO) { "SyncWorker: sync succeeded" }
                Result.success()
            }
            is SyncResult.Error -> {
                logcat(LogPriority.ERROR) { "SyncWorker: sync failed — ${result.message}" }
                if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }
        }
    }

    companion object {
        private const val PERIODIC_WORK_TAG = "sora_sync_periodic"
        private const val SINGLE_WORK_TAG = "sora_sync_single"
        private const val MAX_RETRIES = 3
        private const val SYNC_INTERVAL_HOURS = 6L

        private val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * Schedules a periodic sync every [SYNC_INTERVAL_HOURS] hours.
         * Only runs when the device has an active network connection.
         */
        fun enqueuePeriodicSync(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS,
            )
                .setConstraints(networkConstraints)
                .addTag(PERIODIC_WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
            logcat(LogPriority.INFO) { "SyncWorker: periodic sync scheduled every $SYNC_INTERVAL_HOURS hours" }
        }

        /**
         * Enqueues a one-time sync (e.g. on login or app start).
         */
        fun enqueueSingleSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkConstraints)
                .addTag(SINGLE_WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                SINGLE_WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                request,
            )
            logcat(LogPriority.INFO) { "SyncWorker: one-time sync enqueued" }
        }

        /**
         * Cancels all scheduled sync work.
         */
        fun cancelAllSync(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(PERIODIC_WORK_TAG)
            WorkManager.getInstance(context).cancelAllWorkByTag(SINGLE_WORK_TAG)
            logcat(LogPriority.INFO) { "SyncWorker: all sync work cancelled" }
        }
    }
}
