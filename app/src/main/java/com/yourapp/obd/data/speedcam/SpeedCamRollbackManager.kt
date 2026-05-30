package com.yourapp.obd.data.speedcam

import android.util.Log

class SpeedCamRollbackManager(
    private val dao: SpeedCamDao
) {

    companion object {
        private const val TAG = "SpeedCamRollback"
    }

    data class SnapshotInfo(
        val timestamp: Long = System.currentTimeMillis(),
        val cameraCount: Int,
        val available: Boolean
    )

    suspend fun createSnapshot(): SnapshotInfo {
        val currentCameras = dao.getAllActive()
        val entities = currentCameras.map { SpeedCamSnapshotEntity.fromEntity(it) }

        dao.clearSnapshot()
        if (entities.isNotEmpty()) {
            dao.saveSnapshot(entities)
        }

        Log.i(TAG, "Создан снапшот: ${entities.size} камер")
        return SnapshotInfo(
            cameraCount = entities.size,
            available = entities.isNotEmpty()
        )
    }

    suspend fun hasSnapshot(): Boolean {
        return dao.snapshotCount() > 0
    }

    suspend fun getSnapshotInfo(): SnapshotInfo {
        val count = dao.snapshotCount()
        return SnapshotInfo(cameraCount = count, available = count > 0)
    }

    suspend fun rollback(): Boolean {
        return try {
            val snapshots = dao.getSnapshot()
            if (snapshots.isEmpty()) {
                Log.w(TAG, "Нет снапшота для отката")
                return false
            }

            val entities = snapshots.map { it.toEntity() }

            dao.deleteAll()
            dao.upsertAll(entities)

            Log.i(TAG, "Откат выполнен: восстановлено ${entities.size} камер")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка отката: ${e.message}", e)
            false
        }
    }

    suspend fun clearSnapshot() {
        dao.clearSnapshot()
        Log.i(TAG, "Снапшот очищен")
    }
}
