package com.yourapp.obd.data.speedcam

import android.util.Log
import com.yourapp.obd.domain.model.SpeedCam
import com.yourapp.obd.domain.model.SpeedCamType

object SpeedCamUpdateValidator {

    private const val TAG = "SpeedCamValidator"
    private const val MAX_DUPLICATE_DISTANCE_M = 20.0
    private const val MIN_COORD_CHANGE_M = 5.0

    data class ValidationReport(
        val valid: List<SpeedCam>,
        val duplicatesRemoved: Int,
        val invalidCoords: Int,
        val negativeSpeedLimits: Int,
        val warnings: List<String>
    ) {
        val totalValid: Int get() = valid.size
        val totalIssues: Int get() = duplicatesRemoved + invalidCoords + negativeSpeedLimits
    }

    fun validate(newCameras: List<SpeedCam>, existingIds: Set<String>): ValidationReport {
        val warnings = mutableListOf<String>()
        val validCameras = mutableListOf<SpeedCam>()
        val seenByCoords = mutableMapOf<Pair<Double, Double>, SpeedCam>()
        var duplicatesRemoved = 0
        var invalidCoords = 0
        var negativeSpeedLimits = 0

        for (cam in newCameras) {
            if (!isValidCoordinate(cam.latitude, cam.longitude)) {
                invalidCoords++
                if (invalidCoords <= 5) {
                    warnings.add("Невалидные координаты: ${cam.id} (${cam.latitude}, ${cam.longitude})")
                }
                continue
            }

            if (cam.speedLimitKmh != null && cam.speedLimitKmh <= 0) {
                negativeSpeedLimits++
                if (negativeSpeedLimits <= 3) {
                    warnings.add("Отрицательное ограничение скорости: ${cam.id} = ${cam.speedLimitKmh}")
                }
            }

            val coordKey = Pair(
                Math.round(cam.latitude * 100000.0) / 100000.0,
                Math.round(cam.longitude * 100000.0) / 100000.0
            )
            val existing = seenByCoords[coordKey]
            if (existing != null) {
                val dist = haversineDistance(cam.latitude, cam.longitude, existing.latitude, existing.longitude)
                if (dist < MAX_DUPLICATE_DISTANCE_M) {
                    duplicatesRemoved++
                    if (cam.updatedAt > existing.updatedAt) {
                        seenByCoords[coordKey] = cam
                    }
                    continue
                }
            }

            val sanitized = sanitizeCamera(cam)
            seenByCoords[coordKey] = sanitized
            validCameras.add(sanitized)
        }

        if (warnings.size > 10) {
            warnings.add("... и ещё ${warnings.size - 10} предупреждений")
        }

        return ValidationReport(
            valid = validCameras,
            duplicatesRemoved = duplicatesRemoved,
            invalidCoords = invalidCoords,
            negativeSpeedLimits = negativeSpeedLimits,
            warnings = warnings
        )
    }

    private fun isValidCoordinate(lat: Double, lng: Double): Boolean {
        return lat in -90.0..90.0 && lng in -180.0..180.0 && !(lat == 0.0 && lng == 0.0)
    }

    private fun sanitizeCamera(cam: SpeedCam): SpeedCam {
        val sanitizedType = when {
            cam.type == SpeedCamType.UNKNOWN && cam.speedLimitKmh != null -> SpeedCamType.SPEED
            else -> cam.type
        }
        val sanitizedSpeed = when {
            cam.speedLimitKmh != null && cam.speedLimitKmh < 0 -> null
            cam.speedLimitKmh != null && cam.speedLimitKmh < 20 -> null
            cam.speedLimitKmh != null && cam.speedLimitKmh > 250 -> null
            else -> cam.speedLimitKmh
        }
        return cam.copy(type = sanitizedType, speedLimitKmh = sanitizedSpeed)
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2) +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).pow(2)
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    private fun Double.pow(exp: Int): Double {
        var result = 1.0
        for (i in 0 until exp) result *= this
        return result
    }
}
