package com.yourapp.obd.data.speedcam

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedCamDao {

    @Query("SELECT * FROM speed_cameras WHERE isActive = 1")
    fun getAllActiveFlow(): Flow<List<SpeedCamEntity>>

    @Query("SELECT * FROM speed_cameras WHERE isActive = 1")
    suspend fun getAllActive(): List<SpeedCamEntity>

    @Query("SELECT COUNT(*) FROM speed_cameras WHERE isActive = 1")
    suspend fun countActive(): Int

    @Query("SELECT hash FROM speed_cameras ORDER BY id")
    suspend fun getAllHashes(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cameras: List<SpeedCamEntity>)

    @Query("DELETE FROM speed_cameras WHERE id NOT IN (:activeIds)")
    suspend fun deleteExcept(activeIds: List<String>)

    @Query("DELETE FROM speed_cameras")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM speed_cameras")
    suspend fun count(): Int

    // ── Snapshot (backup) support ──────────────────────────────────────

    @Query("DELETE FROM speed_cameras_snapshot")
    suspend fun clearSnapshot()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSnapshot(cameras: List<SpeedCamSnapshotEntity>)

    @Query("SELECT * FROM speed_cameras_snapshot")
    suspend fun getSnapshot(): List<SpeedCamSnapshotEntity>

    @Query("SELECT COUNT(*) FROM speed_cameras_snapshot")
    suspend fun snapshotCount(): Int

    // ── Update history ────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpdateLog(entry: SpeedCamUpdateLogEntity)

    @Query("SELECT * FROM speedcam_update_log ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentUpdateLogs(): List<SpeedCamUpdateLogEntity>

    @Query("SELECT * FROM speedcam_update_log ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastUpdateLog(): SpeedCamUpdateLogEntity?

    @Query("DELETE FROM speedcam_update_log WHERE timestamp < :before")
    suspend fun deleteOldLogs(before: Long)
}
