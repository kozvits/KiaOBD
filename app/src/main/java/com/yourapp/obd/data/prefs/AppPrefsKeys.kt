package com.yourapp.obd.data.prefs

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object AppPrefsKeys {
    // Bluetooth
    val DEVICE_ADDRESS      = stringPreferencesKey("device_address")
    // DVR
    val BUFFER_SIZE_GB      = intPreferencesKey("buffer_size_gb")
    val VIDEO_RESOLUTION    = stringPreferencesKey("video_resolution")
    val SEGMENT_DURATION    = intPreferencesKey("segment_duration_min")
    // ADAS modules
    val ADAS_SENSITIVITY    = stringPreferencesKey("adas_sensitivity")
    val LDW_ENABLED         = booleanPreferencesKey("ldw_enabled")
    val FCW_ENABLED         = booleanPreferencesKey("fcw_enabled")
    val SIGN_ENABLED        = booleanPreferencesKey("sign_enabled")
    val DMS_ENABLED         = booleanPreferencesKey("dms_enabled")
    val PEDESTRIAN_ENABLED  = booleanPreferencesKey("pedestrian_enabled")
    // SpeedCam
    val SPEEDCAM_URL1       = stringPreferencesKey("speedcam_url1")
    val SPEEDCAM_URL2       = stringPreferencesKey("speedcam_url2")
    val SPEEDCAM_URL3       = stringPreferencesKey("speedcam_url3")
    val SPEEDCAM_LAST_UPD   = stringPreferencesKey("speedcam_last_update")
    // ADAS calibration
    val ADAS_HORIZON        = floatPreferencesKey("adas_horizon_pct")
    val ADAS_LANE_WIDTH     = floatPreferencesKey("adas_lane_width_pct")
    val ADAS_DANGER_DIST    = intPreferencesKey("adas_danger_dist_m")
    val ADAS_WARNING_DIST   = intPreferencesKey("adas_warning_dist_m")
    val ADAS_CAUTION_DIST   = intPreferencesKey("adas_caution_dist_m")
    val ADAS_SAFE_DIST      = intPreferencesKey("adas_safe_dist_m")
}
