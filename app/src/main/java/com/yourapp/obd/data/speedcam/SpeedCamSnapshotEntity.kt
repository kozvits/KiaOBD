package com.yourapp.obd.data.speedcam

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "speed_cameras_snapshot",
    indices = [
        Index(value = ["latitude", "longitude"]),
        Index(value = ["updatedAt"])
    ]
)
data class SpeedCamSnapshotEntity(
    @PrimaryKey val id: String,
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val speedLimitKmh: Int?,
    val direction: String?,
    val road: String?,
    val isActive: Boolean,
    val installedAt: Long?,
    val updatedAt: Long,
    val hash: String
) {
    fun toEntity(): SpeedCamEntity = SpeedCamEntity(
        id = id, latitude = latitude, longitude = longitude, type = type,
        speedLimitKmh = speedLimitKmh, direction = direction, road = road,
        isActive = isActive, installedAt = installedAt, updatedAt = updatedAt, hash = hash
    )

    companion object {
        fun fromEntity(e: SpeedCamEntity): SpeedCamSnapshotEntity = SpeedCamSnapshotEntity(
            id = e.id, latitude = e.latitude, longitude = e.longitude, type = e.type,
            speedLimitKmh = e.speedLimitKmh, direction = e.direction, road = e.road,
            isActive = e.isActive, installedAt = e.installedAt, updatedAt = e.updatedAt, hash = e.hash
        )
    }
}
