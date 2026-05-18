package com.yourapp.obd.domain.model

data class OBDData(
    val rpm: Int? = null,
    val speedKmh: Int? = null,
    val coolantTempC: Int? = null,
    val mapKpa: Int? = null,
    val intakeAirTempC: Int? = null,
    val throttlePercent: Float? = null,
    val fuelLevelPercent: Float? = null,
    val timingAdvanceDeg: Float? = null,
    val voltageV: Float? = null
)

enum class AlertLevel { CAUTION, WARNING, DANGER }

sealed class AdasAlert {
    data class LaneDeparture(val direction: String, val deviation: Float) : AdasAlert()
    data class ForwardCollision(val level: AlertLevel, val ttcSeconds: Float) : AdasAlert()
    data class SpeedLimitExceeded(val limitKmh: Int, val actualKmh: Int) : AdasAlert()
    data class DriverFatigue(val perclos: Float) : AdasAlert()
    data class DriverDistracted(val yawDegrees: Float) : AdasAlert()
    data class PedestrianDetected(val confidence: Float) : AdasAlert()
}

data class DTCCode(
    val code: String,
    val description: String,
    val severity: DtcSeverity,
    val timestamp: Long = System.currentTimeMillis()
)

enum class DtcSeverity { LOW, MEDIUM, HIGH }

data class TripRecord(
    val id: Long = 0,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val distanceKm: Float,
    val maxSpeedKmh: Int,
    val avgSpeedKmh: Int,
    val videoPath: String
)
