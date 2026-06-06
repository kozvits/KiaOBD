package com.yourapp.obd.data.speedcam

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

object SpeedCamSourceProvider {

    const val BELARUS_ISO = "BY"

    fun getPresetById(index: Int): SpeedCamConstants.SourcePreset? =
        SpeedCamConstants.PRESETS.getOrNull(index)

    fun getAllPresets(): List<SpeedCamConstants.SourcePreset> = SpeedCamConstants.PRESETS

    fun getUrlsForPreset(preset: SpeedCamConstants.SourcePreset): Triple<String, String, String> =
        Triple(preset.url1, preset.url2, preset.url3)

    fun suggestPresetForCountry(@Suppress("UNUSED_PARAMETER") context: Context): SpeedCamConstants.SourcePreset =
        SpeedCamConstants.DEFAULT_PRESET

    fun getCountryIso(context: Context): String = try {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        (tm?.simCountryIso ?: Locale.getDefault().country).uppercase()
    } catch (_: Exception) {
        Locale.getDefault().country.uppercase()
    }

    fun buildOverpassUrl(countryIso: String? = BELARUS_ISO, bbox: String? = null): String {
        val iso = countryIso?.uppercase() ?: BELARUS_ISO
        val query = buildString {
            append("[out:json][timeout:90];")
            if (bbox != null) {
                append("(node[\"highway\"=\"speed_camera\"]($bbox);")
                append("node[\"enforcement\"=\"speed_camera\"]($bbox);")
                append("node[\"highway\"=\"speed_display\"]($bbox););")
            } else {
                append("area[\"ISO3166-1\"=\"$iso\"][admin_level=2]->.a;")
                append("(node(area.a)[\"highway\"=\"speed_camera\"];")
                append("node(area.a)[\"enforcement\"=\"speed_camera\"];")
                append("node(area.a)[\"highway\"=\"speed_display\"];);")
            }
            append("out body;")
        }
        return "https://overpass-api.de/api/interpreter?data=${java.net.URLEncoder.encode(query, "UTF-8")}"
    }

    fun extractCountryIso(@Suppress("UNUSED_PARAMETER") locale: Locale = Locale.getDefault()): String =
        BELARUS_ISO
}
