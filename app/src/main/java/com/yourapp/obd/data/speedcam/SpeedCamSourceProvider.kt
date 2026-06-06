package com.yourapp.obd.data.speedcam

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

object SpeedCamSourceProvider {

    fun getPresetById(index: Int): SpeedCamConstants.SourcePreset? =
        SpeedCamConstants.PRESETS.getOrNull(index)

    fun getAllPresets(): List<SpeedCamConstants.SourcePreset> = SpeedCamConstants.PRESETS

    fun getUrlsForPreset(preset: SpeedCamConstants.SourcePreset): Triple<String, String, String> =
        Triple(preset.url1, preset.url2, preset.url3)

    fun suggestPresetForCountry(context: Context): SpeedCamConstants.SourcePreset? {
        val countryCode = getCountryIso(context)

        return when (countryCode.uppercase()) {
            "BY" -> getPresetById(1) // Беларусь (OSM Overpass)
            "FR" -> getPresetById(5) // Франция (data.gouv.fr)
            "AU" -> getPresetById(7) // Австралия
            "US" -> getPresetById(6) // США
            "GB", "IT", "ES", "CH", "BE", "NL", "DE", "PL", "RU", "UA" -> getPresetById(4) // EU Lufop
            else -> getPresetById(0) // OSM via Auto button
        }
    }

    fun getCountryIso(context: Context): String = try {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        (tm?.simCountryIso ?: Locale.getDefault().country).uppercase()
    } catch (_: Exception) {
        Locale.getDefault().country.uppercase()
    }

    fun buildOverpassUrl(countryIso: String? = null, bbox: String? = null): String {
        val query = buildString {
            append("[out:json][timeout:45];")
            if (!countryIso.isNullOrBlank()) {
                append("area[\"ISO3166-1\"=\"${countryIso.uppercase()}\"][admin_level=2]->.a;")
                append("(node(area.a)[\"highway\"=\"speed_camera\"];")
                append("node(area.a)[\"enforcement\"=\"speed_camera\"];")
                append("node(area.a)[\"highway\"=\"speed_display\"];);")
            } else if (bbox != null) {
                append("(node[\"highway\"=\"speed_camera\"]($bbox);")
                append("node[\"enforcement\"=\"speed_camera\"]($bbox);")
                append("node[\"highway\"=\"speed_display\"]($bbox););")
            } else {
                append("(node[\"highway\"=\"speed_camera\"];")
                append("node[\"enforcement\"=\"speed_camera\"];")
                append("node[\"highway\"=\"speed_display\"];);")
            }
            append("out body;")
        }
        return "https://overpass-api.de/api/interpreter?data=${java.net.URLEncoder.encode(query, "UTF-8")}"
    }

    fun extractCountryIso(locale: Locale = Locale.getDefault()): String? {
        val code = locale.country.uppercase()
        return code.takeIf { it.length == 2 }
    }
}
