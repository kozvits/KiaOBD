package com.yourapp.obd.data.speedcam

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yourapp.obd.domain.model.SpeedCam
import com.yourapp.obd.domain.model.SpeedCamUpdateStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeedCamRepository @Inject constructor(
    private val dao: SpeedCamDao,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private const val TAG = "SpeedCamRepo"
        val KEY_LAST_UPDATE_RESULT = stringPreferencesKey("speedcam_last_result")
        val KEY_LAST_UPDATE_TIMESTAMP = longPreferencesKey("speedcam_last_update_ts")
        val KEY_AUTO_UPDATE_ENABLED = longPreferencesKey("speedcam_auto_update")
        val KEY_DB_VERSION = longPreferencesKey("speedcam_db_version")
    }

    private val updateMutex = Mutex()

    val lastUpdateTimestamp: Flow<Long> = dataStore.data.map { p ->
        p[KEY_LAST_UPDATE_TIMESTAMP] ?: 0L
    }

    val lastUpdateResult: Flow<String?> = dataStore.data.map { p ->
        p[KEY_LAST_UPDATE_RESULT]
    }

    val isAutoUpdateEnabled: Flow<Boolean> = dataStore.data.map { p ->
        (p[KEY_AUTO_UPDATE_ENABLED] ?: 1L) == 1L
    }

    val allActiveFlow: Flow<List<SpeedCam>> = dao.getAllActiveFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun setAutoUpdateEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_UPDATE_ENABLED] = if (enabled) 1L else 0L }
    }

    suspend fun updateFromSources(urls: List<String>): SpeedCamUpdateStats {
        return updateMutex.withLock {
            performUpdate(urls)
        }
    }

    private suspend fun performUpdate(urls: List<String>): SpeedCamUpdateStats {
        val sources = urls.map { url ->
            val name = url.removePrefix("https://").removePrefix("http://").take(50)
            HttpSpeedCamSource(url, name)
        }

        var processed = 0
        var failed = 0
        val allCameras = mutableMapOf<String, SpeedCam>()

        for (source in sources) {
            when (val result = source.fetch()) {
                is Result.Success -> {
                    result.getOrThrow().forEach { cam ->
                        if (cam.latitude in -90.0..90.0 && cam.longitude in -180.0..180.0) {
                            val existing = allCameras[cam.id]
                            if (existing == null || cam.updatedAt > existing.updatedAt) {
                                allCameras[cam.id] = cam
                            }
                        } else {
                            Log.w(TAG, "Невалидные координаты: ${cam.latitude}, ${cam.longitude}")
                        }
                    }
                    processed++
                }
                is Result.Failure -> {
                    Log.e(TAG, "Ошибка источника ${source.name}: ${result.exceptionOrNull()?.message}")
                    failed++
                }
            }
        }

        if (processed == 0) {
            return SpeedCamUpdateStats(
                sourcesProcessed = 0,
                sourcesFailed = failed,
                newCameras = 0,
                removedCameras = 0,
                modifiedCameras = 0,
                totalActive = dao.countActive()
            ).also { saveStats(it) }
        }

        return applyUpdate(allCameras.values.toList(), processed, failed)
    }

    private suspend fun applyUpdate(
        newCameras: List<SpeedCam>,
        processedSources: Int,
        failedSources: Int
    ): SpeedCamUpdateStats {
        val oldHashes = dao.getAllHashes().toSet()
        val newMap = newCameras.associateBy { it.id }

        var newCount = 0
        var modifiedCount = 0
        val activeIds = mutableListOf<String>()

        for (cam in newCameras) {
            activeIds.add(cam.id)
            val oldHash = oldHashes.find { hash -> hash.startsWith(cam.id) }
            if (oldHash == null) {
                newCount++
            } else if (cam.hash != oldHash) {
                modifiedCount++
            }
        }

        val oldIds = dao.getAllActive().map { it.id }.toSet()
        val removedCount = (oldIds - newMap.keys).size

        val entities = newCameras.map { SpeedCamEntity.fromDomain(it) }

        dao.deleteExcept(activeIds)
        dao.upsertAll(entities)

        val totalActive = dao.countActive()

        val stats = SpeedCamUpdateStats(
            sourcesProcessed = processedSources,
            sourcesFailed = failedSources,
            newCameras = newCount,
            removedCameras = removedCount,
            modifiedCameras = modifiedCount,
            totalActive = totalActive
        )

        saveStats(stats)
        Log.i(TAG, stats.summary)
        return stats
    }

    private suspend fun saveStats(stats: SpeedCamUpdateStats) {
        dataStore.edit {
            it[KEY_LAST_UPDATE_TIMESTAMP] = stats.timestamp
            it[KEY_LAST_UPDATE_RESULT] = stats.summary
        }
    }

    suspend fun getTotalActive(): Int = dao.countActive()

    suspend fun clearDatabase() {
        updateMutex.withLock {
            dao.deleteAll()
        }
    }
}
