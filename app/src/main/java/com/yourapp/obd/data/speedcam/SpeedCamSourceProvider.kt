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
        val countryCode = try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            tm?.simCountryIso ?: Locale.getDefault().country
        } catch (_: Exception) {
            Locale.getDefault().country
        }

        return when (countryCode.uppercase()) {
            "FR" -> getPresetById(1) // France officiel
            "AU" -> getPresetById(5) // Australia
            "US" -> getPresetById(3) // USA
            "GB", "IT", "ES", "CH", "BE", "NL", "DE" -> getPresetById(2) // EU Lufop
            else -> getPresetById(0) // OSM via Auto button
        }
    }

    fun buildOverpassUrl(countryName: String? = null, bbox: String? = null): String {
        val query = buildString {
            append("[out:json][timeout:30];")
            if (countryName != null) {
                append("area[\"name\"=\"$countryName\"][boundary=administrative]->.a;")
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

    fun extractCountryName(locale: Locale = Locale.getDefault()): String? {
        val code = locale.country.uppercase()
        return when (code) {
            "BY" -> "Belarus"
            "FR" -> "France"
            "DE" -> "Deutschland"
            "IT" -> "Italia"
            "ES" -> "España"
            "GB" -> "United Kingdom"
            "US" -> "United States"
            "CA" -> "Canada"
            "AU" -> "Australia"
            "BR" -> "Brasil"
            "RU" -> "Russia"
            "JP" -> "Japan"
            "CN" -> "China"
            "IN" -> "India"
            "NL" -> "Nederland"
            "BE" -> "België"
            "CH" -> "Schweiz"
            "AT" -> "Österreich"
            "PL" -> "Polska"
            "SE" -> "Sverige"
            "NO" -> "Norge"
            "DK" -> "Danmark"
            "FI" -> "Suomi"
            "PT" -> "Portugal"
            "UA" -> "Ukraine"
            "KZ" -> "Kazakhstan"
            else -> null
        }
    }
}
