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
            ?: props.optString("vitesse").toIntOrNull()?.takeIf { it > 0 }
            ?: props.optString("speed").toIntOrNull()?.takeIf { it > 0 }

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

        val delimiter = detectCsvDelimiter(lines.first())
        val headers = lines.first().split(delimiter).map { it.trim().lowercase() }
        val cameras = lines.drop(1).mapNotNull { line ->
            val values = line.split(delimiter).map { it.trim().removePrefix("+") }
            if (values.size < headers.size) return@mapNotNull null

            val raw = headers.zip(values).toMap()

            val map = mapCsvAliases(raw)

            val id = map["id"] ?: hash(line)
            val lat = map["lat"]?.toDoubleOrNull() ?: map["latitude"]?.toDoubleOrNull() ?: return@mapNotNull null
            val lng = map["lng"]?.toDoubleOrNull() ?: map["lon"]?.toDoubleOrNull() ?: map["longitude"]?.toDoubleOrNull() ?: return@mapNotNull null

            val typeStr = map["type"] ?: "UNKNOWN"
            val type = try { SpeedCamType.valueOf(typeStr.uppercase()) }
                catch (_: Exception) { SpeedCamType.UNKNOWN }

            val speedLimit = map["speed_limit"]?.toIntOrNull()
                ?: map["maxspeed"]?.toIntOrNull()
                ?: map["vitesse_vehicules_legers_kmh"]?.toIntOrNull()
                ?: map["vma"]?.toIntOrNull()

            val equipement = map["equipement"].orEmpty()
            val typeCode = typeStr.uppercase()
            val camType = when {
                type != SpeedCamType.UNKNOWN -> type.name
                typeCode in FRENCH_RADAR_TYPES -> FRENCH_RADAR_TYPES[typeCode]!!
                equipement.contains("ETT") || equipement.contains("ETF") || equipement.contains("ETD") -> "SPEED"
                equipement.contains("ETFR") || equipement.contains("RADAR_FEU") -> "REDLIGHT"
                equipement.contains("ETVM") || equipement.contains("RADAR_TRONCON") -> "AVERAGE"
                equipement.contains("MOBILE") || equipement.contains("CHANTIER") -> "MOBILE"
                else -> typeStr.uppercase().takeIf { it.isNotBlank() } ?: "UNKNOWN"
            }

            SpeedCam(
                id = id,
                latitude = lat,
                longitude = lng,
                type = try { SpeedCamType.valueOf(camType) } catch (_: Exception) { SpeedCamType.UNKNOWN },
                speedLimitKmh = speedLimit,
                direction = map["direction"]?.takeIf { it.isNotBlank() },
                road = map["road"]?.takeIf { it.isNotBlank() }
                    ?: map["route"]?.takeIf { it.isNotBlank() },
                isActive = (map["active"] ?: "true").toBooleanStrictOrNull() ?: true,
                installedAt = parseTimestamp(map["installed_at"])
                    ?: parseTimestamp(map["date_installation"]),
                updatedAt = parseTimestamp(map["updated_at"])
                    ?: parseTimestamp(map["date_heure_dernier_changement"])
                    ?: System.currentTimeMillis(),
                hash = hash(id, lat.toString(), lng.toString(), camType, speedLimit?.toString())
            )
        }

        return ParseResult(cameras, null, null)
    }

    private val FRENCH_RADAR_TYPES = mapOf(
        "ETF" to "SPEED", "ETD" to "SPEED", "ETT" to "SPEED", "ETU" to "SPEED",
        "ETVM" to "AVERAGE", "ETFR" to "REDLIGHT", "ETPN" to "REDLIGHT"
    )

    private val CSV_ALIASES = mapOf(
        "equipement" to "type",
        "type de radar" to "type",
        "numéro de radar" to "id",
        "numero de radar" to "id",
        "vitesse_vehicules_legers_kmh" to "speed_limit",
        "date_installation" to "installed_at",
        "date de mise en service" to "installed_at",
        "date_heure_dernier_changement" to "updated_at",
        "voie" to "road",
        "vma" to "speed_limit",
        "maxspeed" to "speed_limit"
    )

    private fun detectCsvDelimiter(headerLine: String): String {
        val semicolons = headerLine.count { it == ';' }
        val commas = headerLine.count { it == ',' }
        return if (semicolons > commas) ";" else ","
    }

    private fun mapCsvAliases(raw: Map<String, String>): Map<String, String> {
        val result = raw.toMutableMap()
        for ((alias, canonical) in CSV_ALIASES) {
            val value = raw[alias]
            if (value != null && canonical !in result) {
                result[canonical] = value
            }
        }
        return result
    }

    fun parseOsmJson(json: String): ParseResult {
        val root = JSONObject(json)
        val elements = root.optJSONArray("elements") ?: return ParseResult(emptyList(), root.optString("version").ifBlank { null }, null)
        val cameras = mutableListOf<SpeedCam>()
        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            val tags = el.optJSONObject("tags") ?: continue
            if (!tags.optString("highway", "").startsWith("speed") &&
                !tags.optString("enforcement", "").startsWith("speed")) continue

            val id = "osm_${el.optLong("id", 0)}"
            val lat = el.optDouble("lat", 0.0)
            val lon = el.optDouble("lon", 0.0)
            if (lat == 0.0 && lon == 0.0) continue

            val maxspeed = tags.optString("maxspeed").toIntOrNull()
            val direction = tags.optString("direction").ifBlank { null }
            val camType = when {
                tags.optString("enforcement") == "maxspeed" -> "SPEED"
                tags.optString("highway") == "speed_display" -> "SPEED"
                tags.optString("enforcement") == "red_light" -> "REDLIGHT"
                else -> "SPEED"
            }
            val road = tags.optString("name").ifBlank { null }
                ?: tags.optString("ref").ifBlank { null }

            cameras.add(SpeedCam(
                id = id,
                latitude = lat,
                longitude = lon,
                type = try { SpeedCamType.valueOf(camType) } catch (_: Exception) { SpeedCamType.UNKNOWN },
                speedLimitKmh = maxspeed,
                direction = direction,
                road = road,
                isActive = true,
                installedAt = null,
                updatedAt = System.currentTimeMillis(),
                hash = hash(id, lat.toString(), lon.toString(), camType, maxspeed?.toString())
            ))
        }
        return ParseResult(cameras, root.optString("version").ifBlank { null }, null)
    }

    fun parseAuto(data: String): ParseResult {
        val trimmed = data.trim()
        val firstLine = trimmed.lines().firstOrNull().orEmpty()
        return when {
            firstLine.contains("\"elements\"") || firstLine.contains("highway=speed_camera") -> parseOsmJson(trimmed)
            trimmed.startsWith("{") -> parseJson(trimmed)
            trimmed.startsWith("[") -> parseJson("""{"cameras": $trimmed}""")
            trimmed.lines().size > 1 && (trimmed.contains(",") || trimmed.contains(";")) -> parseCsv(trimmed)
            else -> ParseResult(emptyList(), null, null)
        }
    }

    private fun parseTimestamp(value: String?): Long? {
        if (value == null) return null
        return try {
            when {
                value.contains("-") || value.contains("/") -> {
                    val formats = listOf(
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US),
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.US),
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US),
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US),
                        java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.US),
                        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
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
