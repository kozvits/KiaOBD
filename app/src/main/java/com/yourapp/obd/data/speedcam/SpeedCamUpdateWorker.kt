package com.yourapp.obd.data.speedcam

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
import kotlinx.coroutines.flow.first
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
        val prefs = dataStore.data.first()

        if (prefs[AppPrefsKeys.SPEEDCAM_AUTO_UPD] == false) {
            Log.i(TAG, "Автообновление отключено, пропускаем")
            return Result.success()
        }

        val urls = listOf(
            prefs[AppPrefsKeys.SPEEDCAM_URL1].orEmpty(),
            prefs[AppPrefsKeys.SPEEDCAM_URL2].orEmpty(),
            prefs[AppPrefsKeys.SPEEDCAM_URL3].orEmpty()
        ).filter { it.isNotBlank() }

        if (urls.isEmpty()) {
            Log.w(TAG, "Нет настроенных источников")
            return Result.success()
        }

        return try {
            val stats = repository.updateFromSources(urls)
            SpeedCamNotificationHelper.notifyUpdateSuccess(applicationContext, stats.summary)
            if (stats.isError) Result.retry() else Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Критическая ошибка обновления", e)
            SpeedCamNotificationHelper.notifyUpdateError(applicationContext, e.message ?: "Неизвестная ошибка")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
