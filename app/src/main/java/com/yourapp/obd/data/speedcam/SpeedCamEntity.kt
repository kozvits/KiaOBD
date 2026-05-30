package com.yourapp.obd.data.speedcam

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yourapp.obd.domain.model.SpeedCam
import com.yourapp.obd.domain.model.SpeedCamType

@Entity(
    tableName = "speed_cameras",
    indices = [
        Index(value = ["latitude", "longitude"], unique = true),
        Index(value = ["type"]),
        Index(value = ["updatedAt"])
    ]
)
data class SpeedCamEntity(
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
    fun toDomain(): SpeedCam = SpeedCam(
        id = id,
        latitude = latitude,
        longitude = longitude,
        type = try { SpeedCamType.valueOf(type) } catch (_: Exception) { SpeedCamType.UNKNOWN },
        speedLimitKmh = speedLimitKmh,
        direction = direction,
        road = road,
        isActive = isActive,
        installedAt = installedAt,
        updatedAt = updatedAt,
        hash = hash
    )

    companion object {
        fun fromDomain(cam: SpeedCam): SpeedCamEntity = SpeedCamEntity(
            id = cam.id,
            latitude = cam.latitude,
            longitude = cam.longitude,
            type = cam.type.name,
            speedLimitKmh = cam.speedLimitKmh,
            direction = cam.direction,
            road = cam.road,
            isActive = cam.isActive,
            installedAt = cam.installedAt,
            updatedAt = cam.updatedAt,
            hash = cam.hash
        )
    }
}
