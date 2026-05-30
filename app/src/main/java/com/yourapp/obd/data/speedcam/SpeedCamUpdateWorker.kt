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
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yourapp.obd.data.prefs.AppPrefsKeys
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
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
        const val ONE_TIME_WORK_NAME = "speedcam_manual_update"

        fun schedule(context: Context) {
            scheduleAt(context, 3, 0)
        }

        fun scheduleAt(context: Context, hour: Int, minute: Int) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
            }

            val initialDelay = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<SpeedCamUpdateWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )

            Log.i(TAG, "Расписание установлено на $hour:${minute} ежедневно")
        }

        fun scheduleImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SpeedCamUpdateWorker>()
                .setConstraints(constraints)
                .addTag(ONE_TIME_WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )

            Log.i(TAG, "Запланировано немедленное обновление")
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
            Log.i(TAG, "Автообновление отключено в настройках, пропускаем")
            return Result.success()
        }

        val urls = listOf(
            prefs[AppPrefsKeys.SPEEDCAM_URL1].orEmpty(),
            prefs[AppPrefsKeys.SPEEDCAM_URL2].orEmpty(),
            prefs[AppPrefsKeys.SPEEDCAM_URL3].orEmpty()
        ).filter { it.isNotBlank() }

        if (urls.isEmpty()) {
            Log.w(TAG, "Нет настроенных источников для обновления")
            return Result.success()
        }

        Log.i(TAG, "Запуск обновления из ${urls.size} источников")

        return try {
            val stats = repository.updateFromSources(urls)

            if (stats.isError) {
                SpeedCamNotificationHelper.notifyUpdateError(
                    applicationContext,
                    "Не удалось обновить базу: источники недоступны. " +
                    "Обработано ${stats.sourcesProcessed}, ошибок ${stats.sourcesFailed}. " +
                    "Нажмите, чтобы повторить вручную."
                )
                if (runAttemptCount < 3) {
                    Log.w(TAG, "Попытка $runAttemptCount: ошибка, повтор через backoff")
                    Result.retry()
                } else {
                    Log.e(TAG, "Все $runAttemptCount попытки исчерпаны")
                    Result.failure()
                }
            } else {
                if (stats.totalChanges > 0) {
                    SpeedCamNotificationHelper.notifyUpdateSuccess(
                        applicationContext,
                        stats.summary
                    )
                } else {
                    Log.i(TAG, "Изменений не обнаружено, база актуальна")
                }
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Критическая ошибка обновления (попытка $runAttemptCount)", e)
            SpeedCamNotificationHelper.notifyUpdateError(
                applicationContext,
                "Ошибка обновления: ${e.message ?: "Неизвестная ошибка"}. " +
                "Нажмите, чтобы открыть настройки и повторить вручную."
            )
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
