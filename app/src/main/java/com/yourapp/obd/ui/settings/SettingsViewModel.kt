package com.yourapp.obd.ui.settings

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.obd.data.bluetooth.BluetoothOBDRepository
import com.yourapp.obd.data.camera.AdasAnalyzer
import com.yourapp.obd.data.camera.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val selectedDeviceAddress: String = "",
    val bufferSizeGb: Int = 4,
    val videoResolution: String = "FHD",
    val segmentDurationMin: Int = 5,
    val adasSensitivity: String = "MEDIUM",
    val ldwEnabled: Boolean = true,
    val fcwEnabled: Boolean = true,
    val signEnabled: Boolean = true,
    val dmsEnabled: Boolean = true,
    val pedestrianEnabled: Boolean = true,
    val adasWithoutObd: Boolean = false,
    // Калибровка ADAS
    val horizonPosition: Float = 0.42f,
    val laneWidthPercent: Float = 0.28f,
    val vanishingPointX: Float = 0.5f,
    val dangerZoneM: Int = 5,
    val warningZoneM: Int = 10,
    val cautionZoneM: Int = 20
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val obdRepository: BluetoothOBDRepository,
    private val cameraRepository: CameraRepository,
    private val adasAnalyzer: AdasAnalyzer
) : ViewModel() {

    companion object {
        val KEY_DEVICE_ADDRESS    = stringPreferencesKey("device_address")
        val KEY_BUFFER_SIZE_GB    = intPreferencesKey("buffer_size_gb")
        val KEY_VIDEO_RESOLUTION  = stringPreferencesKey("video_resolution")
        val KEY_SEGMENT_DURATION  = intPreferencesKey("segment_duration_min")
        val KEY_ADAS_SENSITIVITY  = stringPreferencesKey("adas_sensitivity")
        val KEY_LDW               = booleanPreferencesKey("ldw_enabled")
        val KEY_FCW               = booleanPreferencesKey("fcw_enabled")
        val KEY_SIGN              = booleanPreferencesKey("sign_enabled")
        val KEY_DMS               = booleanPreferencesKey("dms_enabled")
        val KEY_PEDESTRIAN        = booleanPreferencesKey("pedestrian_enabled")
        // Калибровка
        val KEY_HORIZON           = floatPreferencesKey("adas_horizon")
        val KEY_LANE_WIDTH        = floatPreferencesKey("adas_lane_width")
        val KEY_VP_X              = floatPreferencesKey("adas_vp_x")
        val KEY_DANGER_M          = intPreferencesKey("adas_danger_m")
        val KEY_WARNING_M         = intPreferencesKey("adas_warning_m")
        val KEY_CAUTION_M         = intPreferencesKey("adas_caution_m")
        val KEY_ADAS_WITHOUT_OBD  = booleanPreferencesKey("adas_without_obd")
    }

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                val s = _settingsState.value.copy(
                    selectedDeviceAddress = prefs[KEY_DEVICE_ADDRESS] ?: "",
                    bufferSizeGb = prefs[KEY_BUFFER_SIZE_GB] ?: 4,
                    videoResolution = prefs[KEY_VIDEO_RESOLUTION] ?: "FHD",
                    segmentDurationMin = prefs[KEY_SEGMENT_DURATION] ?: 5,
                    adasSensitivity = prefs[KEY_ADAS_SENSITIVITY] ?: "MEDIUM",
                    ldwEnabled = prefs[KEY_LDW] ?: true,
                    fcwEnabled = prefs[KEY_FCW] ?: true,
                    signEnabled = prefs[KEY_SIGN] ?: true,
                    dmsEnabled = prefs[KEY_DMS] ?: true,
                    pedestrianEnabled = prefs[KEY_PEDESTRIAN] ?: true,
                    adasWithoutObd = prefs[KEY_ADAS_WITHOUT_OBD] ?: false,
                    horizonPosition = prefs[KEY_HORIZON] ?: 0.42f,
                    laneWidthPercent = prefs[KEY_LANE_WIDTH] ?: 0.28f,
                    vanishingPointX = prefs[KEY_VP_X] ?: 0.5f,
                    dangerZoneM = prefs[KEY_DANGER_M] ?: 5,
                    warningZoneM = prefs[KEY_WARNING_M] ?: 10,
                    cautionZoneM = prefs[KEY_CAUTION_M] ?: 20
                )
                _settingsState.value = s
                adasAnalyzer.ldwEnabled = s.ldwEnabled
                adasAnalyzer.fcwEnabled = s.fcwEnabled
                adasAnalyzer.signDetectionEnabled = s.signEnabled
                adasAnalyzer.dmsEnabled = s.dmsEnabled
                adasAnalyzer.pedestrianEnabled = s.pedestrianEnabled
                adasAnalyzer.sensitivity = s.adasSensitivity
                adasAnalyzer.adasWithoutObd = s.adasWithoutObd
                adasAnalyzer.horizonPosition = s.horizonPosition
                adasAnalyzer.dangerZoneM = s.dangerZoneM
                adasAnalyzer.warningZoneM = s.warningZoneM
                adasAnalyzer.cautionZoneM = s.cautionZoneM
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun getPairedDevices(): List<BluetoothDevice> = obdRepository.getPairedDevices().toList()

    fun selectDevice(address: String) { viewModelScope.launch { dataStore.edit { it[KEY_DEVICE_ADDRESS] = address } } }
    fun setBufferSizeGb(gb: Int) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_BUFFER_SIZE_GB] = gb }
            cameraRepository.setMaxBufferBytes(gb.toLong() * 1024 * 1024 * 1024)
        }
    }
    fun setVideoResolution(r: String)   { viewModelScope.launch { dataStore.edit { it[KEY_VIDEO_RESOLUTION] = r } } }
    fun setSegmentDurationMin(m: Int)   { viewModelScope.launch { dataStore.edit { it[KEY_SEGMENT_DURATION] = m } } }
    fun setAdasSensitivity(s: String)   { adasAnalyzer.sensitivity = s; viewModelScope.launch { dataStore.edit { it[KEY_ADAS_SENSITIVITY] = s } } }
    fun setLdwEnabled(v: Boolean)       { adasAnalyzer.ldwEnabled           = v; viewModelScope.launch { dataStore.edit { it[KEY_LDW]        = v } } }
    fun setFcwEnabled(v: Boolean)       { adasAnalyzer.fcwEnabled           = v; viewModelScope.launch { dataStore.edit { it[KEY_FCW]        = v } } }
    fun setSignEnabled(v: Boolean)      { adasAnalyzer.signDetectionEnabled = v; viewModelScope.launch { dataStore.edit { it[KEY_SIGN]       = v } } }
    fun setDmsEnabled(v: Boolean)       { adasAnalyzer.dmsEnabled           = v; viewModelScope.launch { dataStore.edit { it[KEY_DMS]        = v } } }
    fun setPedestrianEnabled(v: Boolean){ adasAnalyzer.pedestrianEnabled    = v; viewModelScope.launch { dataStore.edit { it[KEY_PEDESTRIAN] = v } } }
    fun setAdasWithoutObd(v: Boolean)  { adasAnalyzer.adasWithoutObd       = v; viewModelScope.launch { dataStore.edit { it[KEY_ADAS_WITHOUT_OBD] = v } } }

    // Калибровка ADAS
    fun setHorizonPosition(v: Float)  {
        val value = v.coerceIn(0.2f, 0.7f)
        adasAnalyzer.horizonPosition = value
        viewModelScope.launch { dataStore.edit { it[KEY_HORIZON] = value } }
    }
    fun setLaneWidthPercent(v: Float) { viewModelScope.launch { dataStore.edit { it[KEY_LANE_WIDTH] = v.coerceIn(0.1f, 0.5f) } } }
    fun setVanishingPointX(v: Float)  { viewModelScope.launch { dataStore.edit { it[KEY_VP_X]       = v.coerceIn(0.2f, 0.8f) } } }
    fun setDangerZoneM(m: Int)        { adasAnalyzer.dangerZoneM = m; viewModelScope.launch { dataStore.edit { it[KEY_DANGER_M]   = m } } }
    fun setWarningZoneM(m: Int)       { adasAnalyzer.warningZoneM = m; viewModelScope.launch { dataStore.edit { it[KEY_WARNING_M]  = m } } }
    fun setCautionZoneM(m: Int)       { adasAnalyzer.cautionZoneM = m; viewModelScope.launch { dataStore.edit { it[KEY_CAUTION_M]  = m } } }
}
