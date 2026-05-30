package com.yourapp.obd.data.speedcam

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "speedcam_update_log",
    indices = [Index(value = ["timestamp"])]
)
data class SpeedCamUpdateLogEntity(
    @PrimaryKey val timestamp: Long,
    val sourcesProcessed: Int,
    val sourcesFailed: Int,
    val newCameras: Int,
    val removedCameras: Int,
    val modifiedCameras: Int,
    val totalActive: Int,
    val rollbackAvailable: Boolean,
    val summary: String,
    val details: String = ""
)
