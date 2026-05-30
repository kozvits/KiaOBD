package com.yourapp.obd.data.speedcam

import com.yourapp.obd.domain.model.SpeedCam
import com.yourapp.obd.domain.model.SpeedCamType
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

object SpeedCamParser {

    private val digest: MessageDigest by lazy { MessageDigest.getInstance("SHA-256") }

    data class ParseResult(
        val cameras: List<SpeedCam>,
        val version: String?,
        val checksum: String?
    )

    private fun hash(vararg parts: String?): String {
        val input = parts.joinToString("|") { it ?: "" }
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun parseJson(json: String): ParseResult {
        val root = JSONObject(json)
        val version = root.optString("version").ifBlank { null }
        val meta = root.optJSONObject("meta")
        val checksum = meta?.optString("checksum")?.ifBlank { null }
        val cameras = mutableListOf<SpeedCam>()

        val arr = root.optJSONArray("cameras") ?: root.optJSONArray("features") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            cameras.add(parseCameraJson(item))
        }

        return ParseResult(cameras, version, checksum)
    }

    private fun parseCameraJson(obj: JSONObject): SpeedCam {
        val geometry = obj.optJSONObject("geometry")
        val props = obj.optJSONObject("properties") ?: obj

        val id = props.optString("id")
            .takeIf { it.isNotBlank() }
            ?: props.optString("camera_id")
            ?: hash(obj.toString())

        val lat: Double
        val lng: Double

        if (geometry != null && geometry.optString("type") == "Point") {
            val coords = geometry.optJSONArray("coordinates")
            lng = coords.optDouble(0, 0.0)
            lat = coords.optDouble(1, 0.0)
        } else {
            lat = obj.optDouble("lat", props.optDouble("lat", 0.0))
            lng = obj.optDouble("lng", props.optDouble("lng", props.optDouble("lon", 0.0)))
        }

        val typeStr = props.optString("type", "")
            .takeIf { it.isNotBlank() }
            ?: props.optString("camera_type", "UNKNOWN")

        val type = try { SpeedCamType.valueOf(typeStr.uppercase()) }
            catch (_: Exception) { SpeedCamType.UNKNOWN }

        val speedLimit = props.optInt("speed_limit", -1)
            .takeIf { it > 0 }
            ?: props.optInt("maxSpeed", -1).takeIf { it > 0 }
            ?: props.optInt("limit", -1).takeIf { it > 0 }

        val active = props.optBoolean("active", true)
            .takeIf { it } ?: props.optBoolean("isActive", true)
            ?: props.optBoolean("is_active", true)

        val installedAt = parseTimestamp(
            props.optString("installed_at").ifBlank { null }
                ?: props.optString("installation_date").ifBlank { null }
        )

        val updatedAt = parseTimestamp(
            props.optString("updated_at").ifBlank { null }
                ?: props.optString("update_date").ifBlank { null }
        ) ?: System.currentTimeMillis()

        val camHash = hash(id, lat.toString(), lng.toString(), typeStr, speedLimit?.toString())

        return SpeedCam(
            id = id,
            latitude = lat,
            longitude = lng,
            type = type,
            speedLimitKmh = speedLimit,
            direction = props.optString("direction").ifBlank { null },
            road = props.optString("road").ifBlank { null }
                ?: props.optString("road_name").ifBlank { null },
            isActive = active,
            installedAt = installedAt,
            updatedAt = updatedAt,
            hash = camHash
        )
    }

    fun parseCsv(csv: String): ParseResult {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return ParseResult(emptyList(), null, null)

        val headers = lines.first().split(",").map { it.trim().lowercase() }
        val cameras = lines.drop(1).mapNotNull { line ->
            val values = line.split(",").map { it.trim() }
            if (values.size < headers.size) return@mapNotNull null

            val map = headers.zip(values).toMap()

            val id = map["id"] ?: hash(line)
            val lat = map["lat"]?.toDoubleOrNull() ?: map["latitude"]?.toDoubleOrNull() ?: return@mapNotNull null
            val lng = map["lng"]?.toDoubleOrNull() ?: map["lon"]?.toDoubleOrNull() ?: map["longitude"]?.toDoubleOrNull() ?: return@mapNotNull null

            val typeStr = map["type"] ?: "UNKNOWN"
            val type = try { SpeedCamType.valueOf(typeStr.uppercase()) }
                catch (_: Exception) { SpeedCamType.UNKNOWN }

            val speedLimit = map["speed_limit"]?.toIntOrNull() ?: map["maxspeed"]?.toIntOrNull()

            SpeedCam(
                id = id,
                latitude = lat,
                longitude = lng,
                type = type,
                speedLimitKmh = speedLimit,
                direction = map["direction"]?.takeIf { it.isNotBlank() },
                road = map["road"]?.takeIf { it.isNotBlank() },
                isActive = (map["active"] ?: "true").toBooleanStrictOrNull() ?: true,
                installedAt = parseTimestamp(map["installed_at"]),
                updatedAt = parseTimestamp(map["updated_at"]) ?: System.currentTimeMillis(),
                hash = hash(id, lat.toString(), lng.toString(), typeStr, speedLimit?.toString())
            )
        }

        return ParseResult(cameras, null, null)
    }

    fun parseAuto(data: String): ParseResult {
        val trimmed = data.trim()
        return when {
            trimmed.startsWith("{") -> parseJson(trimmed)
            trimmed.startsWith("[") -> parseJson("""{"cameras": $trimmed}""")
            trimmed.contains(",") && trimmed.lines().size > 1 -> parseCsv(trimmed)
            else -> ParseResult(emptyList(), null, null)
        }
    }

    private fun parseTimestamp(value: String?): Long? {
        if (value == null) return null
        return try {
            when {
                value.contains("-") -> {
                    val formats = listOf(
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US),
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.US),
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US),
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    )
                    for (fmt in formats) {
                        try { return fmt.parse(value)?.time } catch (_: Exception) { }
                    }
                    null
                }
                value.all { it.isDigit() } -> value.toLongOrNull()
                else -> null
            }
        } catch (_: Exception) { null }
    }
}
