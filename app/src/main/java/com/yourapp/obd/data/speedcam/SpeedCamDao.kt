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
}
