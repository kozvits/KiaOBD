package com.yourapp.obd.data.speedcam

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yourapp.obd.domain.model.SpeedCam
import com.yourapp.obd.domain.model.SpeedCamUpdateStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeedCamRepository @Inject constructor(
    private val dao: SpeedCamDao,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SpeedCamRepo"
        val KEY_LAST_UPDATE_RESULT = stringPreferencesKey("speedcam_last_result")
        val KEY_LAST_UPDATE_TIMESTAMP = longPreferencesKey("speedcam_last_update_ts")
        val KEY_ROLLBACK_AVAILABLE = stringPreferencesKey("speedcam_rollback_ts")
    }

    private val updateMutex = Mutex()
    private val logger = SpeedCamUpdateLogger(
        File(context.filesDir, "logs/speedcam")
    )
    val rollbackManager = SpeedCamRollbackManager(dao)

    val lastUpdateTimestamp: Flow<Long> = dataStore.data.map { p ->
        p[KEY_LAST_UPDATE_TIMESTAMP] ?: 0L
    }

    val lastUpdateResult: Flow<String?> = dataStore.data.map { p ->
        p[KEY_LAST_UPDATE_RESULT]
    }

    val rollbackTimestamp: Flow<Long> = dataStore.data.map { p ->
        p[KEY_ROLLBACK_AVAILABLE]?.toLongOrNull() ?: 0L
    }

    val allActiveFlow: Flow<List<SpeedCam>> = dao.getAllActiveFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun updateFromSources(urls: List<String>): SpeedCamUpdateStats {
        return updateMutex.withLock {
            performUpdate(urls)
        }
    }

    private suspend fun performUpdate(urls: List<String>): SpeedCamUpdateStats {
        logger.logInfo("UPDATE", "Начало обновления из ${urls.size} источников")

        val sources = urls.map { url ->
            val name = url.removePrefix("https://").removePrefix("http://").take(50)
            HttpSpeedCamSource(url, name)
        }

        var processed = 0
        var failed = 0
        val allCameras = mutableMapOf<String, SpeedCam>()

        for (source in sources) {
            val fetchResult = source.fetch()
            if (fetchResult.isSuccess) {
                val cameras = fetchResult.getOrThrow()
                logger.logInfo("SOURCE", "${source.name}: получено ${cameras.size} камер")
                for (cam in cameras) {
                    val existing = allCameras[cam.id]
                    if (existing == null || cam.updatedAt > existing.updatedAt) {
                        allCameras[cam.id] = cam
                    }
                }
                processed++
            } else {
                val err = fetchResult.exceptionOrNull()
                logger.logError("SOURCE", "Ошибка источника ${source.name}", err)
                failed++
            }
        }

        if (processed == 0) {
            logger.logError("UPDATE", "Все источники недоступны")
            return SpeedCamUpdateStats(
                sourcesProcessed = 0,
                sourcesFailed = failed,
                newCameras = 0,
                removedCameras = 0,
                modifiedCameras = 0,
                totalActive = dao.countActive()
            ).also { saveStats(it); saveLogToDb(it, rollbackAvailable = false) }
        }

        val existingIds = dao.getAllActive().map { it.id }.toSet()
        val validation = SpeedCamUpdateValidator.validate(allCameras.values.toList(), existingIds)

        if (validation.warnings.isNotEmpty()) {
            logger.logWarn("VALIDATE", "Предупреждения: ${validation.warnings.joinToString("; ")}")
        }
        logger.logInfo("VALIDATE",
            "валидно: ${validation.totalValid}, дубликатов: ${validation.duplicatesRemoved}, " +
            "невалидных координат: ${validation.invalidCoords}")

        if (validation.totalValid == 0) {
            logger.logError("UPDATE", "Нет валидных камер после проверки")
            return SpeedCamUpdateStats(
                sourcesProcessed = processed,
                sourcesFailed = failed,
                newCameras = 0,
                removedCameras = 0,
                modifiedCameras = 0,
                totalActive = dao.countActive()
            ).also { saveStats(it); saveLogToDb(it, rollbackAvailable = false) }
        }

        return applyUpdate(validation.valid, processed, failed)
    }

    private suspend fun applyUpdate(
        newCameras: List<SpeedCam>,
        processedSources: Int,
        failedSources: Int
    ): SpeedCamUpdateStats {
        val oldHashes = dao.getAllHashes()
        val newMap = newCameras.associateBy { it.id }

        var newCount = 0
        var modifiedCount = 0

        val oldHashMap = mutableMapOf<String, String>()
        for (h in oldHashes) {
            val idx = h.indexOf('|')
            if (idx >= 0) oldHashMap[h.substring(0, idx)] = h.substring(idx + 1)
            else oldHashMap[h] = ""
        }

        for (cam in newCameras) {
            val oldHash = oldHashMap[cam.id]
            when {
                oldHash == null -> newCount++
                cam.hash != oldHash -> modifiedCount++
            }
        }

        val oldIds = dao.getAllActive().map { it.id }.toSet()
        val removedCount = (oldIds - newMap.keys).size

        val totalChanges = newCount + removedCount + modifiedCount

        if (totalChanges == 0) {
            logger.logInfo("UPDATE", "Изменений нет. Пропускаем обновление БД.")
            val stats = SpeedCamUpdateStats(
                sourcesProcessed = processedSources,
                sourcesFailed = failedSources,
                newCameras = 0,
                removedCameras = 0,
                modifiedCameras = 0,
                totalActive = dao.countActive()
            )
            saveStats(stats)
            saveLogToDb(stats, rollbackAvailable = false)
            return stats
        }

        val snapshotInfo = rollbackManager.createSnapshot()
        logger.logInfo("SNAPSHOT", "Создан снапшот: ${snapshotInfo.cameraCount} камер")

        val entities = newCameras.map { SpeedCamEntity.fromDomain(it) }

        return try {
            dao.replaceAll(entities)

            val totalActive = dao.countActive()

            val stats = SpeedCamUpdateStats(
                sourcesProcessed = processedSources,
                sourcesFailed = failedSources,
                newCameras = newCount,
                removedCameras = removedCount,
                modifiedCameras = modifiedCount,
                totalActive = totalActive
            )

            if (newCount > 0 || modifiedCount > 0) {
                logger.logDiff(newCount, removedCount, modifiedCount)
            }

            saveStats(stats)
            saveLogToDb(stats, rollbackAvailable = true)

            logger.logUpdateResult(stats)
            Log.i(TAG, stats.summary)

            if (removedCount > 0) {
                dataStore.edit { it[KEY_ROLLBACK_AVAILABLE] = stats.timestamp.toString() }
            }

            stats
        } catch (e: Exception) {
            logger.logError("UPDATE", "Ошибка применения обновления, выполняю откат", e)

            val rollbackOk = rollbackManager.rollback()
            logger.logRollback("Ошибка обновления: ${e.message}", rollbackOk)

            if (rollbackOk) {
                rollbackManager.clearSnapshot()
            }

            val stats = SpeedCamUpdateStats(
                sourcesProcessed = processedSources,
                sourcesFailed = failedSources + 1,
                newCameras = 0,
                removedCameras = 0,
                modifiedCameras = 0,
                totalActive = dao.countActive()
            )
            saveStats(stats)
            saveLogToDb(stats, rollbackAvailable = false, details = "Откат: ${rollbackOk}")
            return stats
        }
    }

    suspend fun rollbackLastUpdate(): SpeedCamUpdateStats {
        return updateMutex.withLock {
            logger.logInfo("ROLLBACK", "Запрос отката последнего обновления")

            val hasSnap = rollbackManager.hasSnapshot()
            if (!hasSnap) {
                logger.logWarn("ROLLBACK", "Нет снапшота для отката")
                return@withLock SpeedCamUpdateStats(
                    sourcesProcessed = 0,
                    sourcesFailed = 0,
                    newCameras = 0,
                    removedCameras = 0,
                    modifiedCameras = 0,
                    totalActive = dao.countActive()
                )
            }

            val success = rollbackManager.rollback()
            logger.logRollback("Ручной откат", success)

            if (success) {
                dataStore.edit { it.remove(KEY_ROLLBACK_AVAILABLE) }
                rollbackManager.clearSnapshot()
            }

            val totalActive = dao.countActive()
            SpeedCamUpdateStats(
                sourcesProcessed = 0,
                sourcesFailed = if (success) 0 else 1,
                newCameras = 0,
                removedCameras = 0,
                modifiedCameras = 0,
                totalActive = totalActive
            ).also { saveStats(it) }
        }
    }

    suspend fun getUpdateHistory(): List<SpeedCamUpdateLogEntity> {
        return dao.getRecentUpdateLogs()
    }

    private suspend fun saveStats(stats: SpeedCamUpdateStats) {
        dataStore.edit {
            it[KEY_LAST_UPDATE_TIMESTAMP] = stats.timestamp
            it[KEY_LAST_UPDATE_RESULT] = stats.summary
        }
    }

    private suspend fun saveLogToDb(
        stats: SpeedCamUpdateStats,
        rollbackAvailable: Boolean,
        details: String = ""
    ) {
        try {
            logger.logToDatabase(dao, stats, rollbackAvailable, details)
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось сохранить лог в БД: ${e.message}")
        }
    }

    suspend fun getTotalActive(): Int = dao.countActive()

    suspend fun clearDatabase() {
        updateMutex.withLock {
            dao.deleteAll()
            dao.clearSnapshot()
            logger.logInfo("CLEAR", "База камер очищена")
        }
    }
}
