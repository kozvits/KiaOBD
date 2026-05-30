package com.yourapp.obd.data.speedcam

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yourapp.obd.data.prefs.AppPrefsKeys
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SpeedCamUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: SpeedCamRepository,
    private val dataStore: DataStore<Preferences>
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SpeedCamWorker"
        const val WORK_NAME = "speedcam_daily_update"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<SpeedCamUpdateWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun isScheduled(context: Context) =
            WorkManager.getInstance(context).getWorkInfosForUniqueWork(WORK_NAME).get()
                ?.any { it.state == androidx.work.WorkInfo.State.ENQUEUED || it.state == androidx.work.WorkInfo.State.RUNNING }
                ?: false
    }

    override suspend fun doWork(): Result {
        val autoUpdateEnabled = dataStore.data.let { flow ->
            var enabled = true
            flow.collect { p ->
                enabled = (p[AppPrefsKeys.SPEEDCAM_LAST_UPD]?.toLongOrNull() ?: 1L) == 1L
                return@collect
            }
            enabled
        }

        if (!autoUpdateEnabled) {
            Log.i(TAG, "Автообновление отключено, пропускаем")
            return Result.success()
        }

        val urls = dataStore.data.let { flow ->
            val urls = mutableListOf<String>()
            flow.collect { p ->
                listOf(
                    p[AppPrefsKeys.SPEEDCAM_URL1],
                    p[AppPrefsKeys.SPEEDCAM_URL2],
                    p[AppPrefsKeys.SPEEDCAM_URL3]
                ).filterNotNull().filter { it.isNotBlank() }.let { urls.addAll(it) }
                return@collect
            }
            urls
        }

        if (urls.isEmpty()) {
            Log.w(TAG, "Нет настроенных источников")
            return Result.success()
        }

        return try {
            val stats = repository.updateFromSources(urls)

            if (stats.isError) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Критическая ошибка обновления", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
