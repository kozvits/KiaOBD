package com.yourapp.obd.domain.model

data class SpeedCam(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val type: SpeedCamType,
    val speedLimitKmh: Int?,
    val direction: String?,
    val road: String?,
    val isActive: Boolean,
    val installedAt: Long?,
    val updatedAt: Long,
    val hash: String
)

enum class SpeedCamType(val label: String) {
    SPEED("Стационарная камера скорости"),
    REDLIGHT("Камера проезда на красный"),
    AVERAGE("Камера средней скорости"),
    MOBILE("Передвижная камера"),
    TOLL("Камера оплаты проезда"),
    UNKNOWN("Неизвестный тип")
}

data class SpeedCamUpdateStats(
    val sourcesProcessed: Int,
    val sourcesFailed: Int,
    val newCameras: Int,
    val removedCameras: Int,
    val modifiedCameras: Int,
    val totalActive: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isError: Boolean get() = sourcesProcessed == 0
    val summary: String
        get() = buildString {
            if (isError) {
                append("Ошибка обновления всех источников")
                return@buildString
            }
            append("База камер обновлена")
            if (newCameras > 0) append(": +$newCameras новых")
            if (removedCameras > 0) append(", -$removedCameras удалено")
            if (modifiedCameras > 0) append(", ~$modifiedCameras изменено")
            append(". Всего активных: $totalActive")
        }
}
