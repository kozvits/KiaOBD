package com.yourapp.obd.ui.settings

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
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
import com.yourapp.obd.data.speedcam.SpeedCamNotificationHelper
import com.yourapp.obd.data.speedcam.SpeedCamRepository
import com.yourapp.obd.data.speedcam.SpeedCamUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    // Калибровка ADAS
    val horizonPosition: Float = 0.42f,
    val laneWidthPercent: Float = 0.28f,
    val vanishingPointX: Float = 0.5f,
    val dangerZoneM: Int = 5,
    val warningZoneM: Int = 10,
    val cautionZoneM: Int = 20,
    // SpeedCam
    val speedcamUrl1: String = "",
    val speedcamUrl2: String = "",
    val speedcamUrl3: String = "",
    val speedcamLastUpdate: Long = 0L,
    val speedcamAutoUpdate: Boolean = true,
    val speedcamTotalCameras: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val obdRepository: BluetoothOBDRepository,
    private val cameraRepository: CameraRepository,
    private val adasAnalyzer: AdasAnalyzer,
    private val speedCamRepository: SpeedCamRepository
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
        // SpeedCam
        val KEY_SPEEDCAM_URL1     = stringPreferencesKey("speedcam_url1")
        val KEY_SPEEDCAM_URL2     = stringPreferencesKey("speedcam_url2")
        val KEY_SPEEDCAM_URL3     = stringPreferencesKey("speedcam_url3")
        val KEY_SPEEDCAM_LAST_UPD = stringPreferencesKey("speedcam_last_update")
        val KEY_SPEEDCAM_AUTO_UPD = booleanPreferencesKey("speedcam_auto_update")
    }

    val settingsState = dataStore.data.map { p ->
        SettingsState(
            selectedDeviceAddress = p[KEY_DEVICE_ADDRESS]   ?: "",
            bufferSizeGb          = p[KEY_BUFFER_SIZE_GB]   ?: 4,
            videoResolution       = p[KEY_VIDEO_RESOLUTION]  ?: "FHD",
            segmentDurationMin    = p[KEY_SEGMENT_DURATION]  ?: 5,
            adasSensitivity       = p[KEY_ADAS_SENSITIVITY]  ?: "MEDIUM",
            ldwEnabled            = p[KEY_LDW]               ?: true,
            fcwEnabled            = p[KEY_FCW]               ?: true,
            signEnabled           = p[KEY_SIGN]              ?: true,
            dmsEnabled            = p[KEY_DMS]               ?: true,
            pedestrianEnabled     = p[KEY_PEDESTRIAN]        ?: true,
            horizonPosition       = p[KEY_HORIZON]           ?: 0.42f,
            laneWidthPercent      = p[KEY_LANE_WIDTH]        ?: 0.28f,
            vanishingPointX       = p[KEY_VP_X]              ?: 0.5f,
            dangerZoneM           = p[KEY_DANGER_M]          ?: 5,
            warningZoneM          = p[KEY_WARNING_M]         ?: 10,
            cautionZoneM          = p[KEY_CAUTION_M]          ?: 20,
            speedcamUrl1          = p[KEY_SPEEDCAM_URL1]     ?: "",
            speedcamUrl2          = p[KEY_SPEEDCAM_URL2]     ?: "",
            speedcamUrl3          = p[KEY_SPEEDCAM_URL3]     ?: "",
            speedcamLastUpdate    = p[KEY_SPEEDCAM_LAST_UPD]?.toLongOrNull() ?: 0L,
            speedcamAutoUpdate    = p[KEY_SPEEDCAM_AUTO_UPD] ?: true,
            speedcamTotalCameras  = 0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    private val _isUpdatingSpeedcam = MutableStateFlow(false)
    val isUpdatingSpeedcam: StateFlow<Boolean> = _isUpdatingSpeedcam.asStateFlow()

    private val _speedcamUpdateResult = MutableStateFlow<String?>(null)
    val speedcamUpdateResult: StateFlow<String?> = _speedcamUpdateResult.asStateFlow()

    init {
        viewModelScope.launch {
            speedCamRepository.lastUpdateResult.collect { result ->
                if (result != null) _speedcamUpdateResult.value = result
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
    fun setAdasSensitivity(s: String)   { viewModelScope.launch { dataStore.edit { it[KEY_ADAS_SENSITIVITY] = s } } }
    fun setLdwEnabled(v: Boolean)       { adasAnalyzer.ldwEnabled           = v; viewModelScope.launch { dataStore.edit { it[KEY_LDW]        = v } } }
    fun setFcwEnabled(v: Boolean)       { adasAnalyzer.fcwEnabled           = v; viewModelScope.launch { dataStore.edit { it[KEY_FCW]        = v } } }
    fun setSignEnabled(v: Boolean)      { adasAnalyzer.signDetectionEnabled = v; viewModelScope.launch { dataStore.edit { it[KEY_SIGN]       = v } } }
    fun setDmsEnabled(v: Boolean)       { adasAnalyzer.dmsEnabled           = v; viewModelScope.launch { dataStore.edit { it[KEY_DMS]        = v } } }
    fun setPedestrianEnabled(v: Boolean){ adasAnalyzer.pedestrianEnabled    = v; viewModelScope.launch { dataStore.edit { it[KEY_PEDESTRIAN] = v } } }

    // Калибровка ADAS
    fun setHorizonPosition(v: Float)  { viewModelScope.launch { dataStore.edit { it[KEY_HORIZON]    = v.coerceIn(0.2f, 0.7f) } } }
    fun setLaneWidthPercent(v: Float) { viewModelScope.launch { dataStore.edit { it[KEY_LANE_WIDTH] = v.coerceIn(0.1f, 0.5f) } } }
    fun setVanishingPointX(v: Float)  { viewModelScope.launch { dataStore.edit { it[KEY_VP_X]       = v.coerceIn(0.2f, 0.8f) } } }
    fun setDangerZoneM(m: Int)        { viewModelScope.launch { dataStore.edit { it[KEY_DANGER_M]   = m } } }
    fun setWarningZoneM(m: Int)       { viewModelScope.launch { dataStore.edit { it[KEY_WARNING_M]  = m } } }
    fun setCautionZoneM(m: Int)       { viewModelScope.launch { dataStore.edit { it[KEY_CAUTION_M]  = m } } }

    // SpeedCam
    fun setSpeedcamUrl(index: Int, url: String) {
        viewModelScope.launch {
            dataStore.edit {
                when (index) {
                    1 -> it[KEY_SPEEDCAM_URL1] = url
                    2 -> it[KEY_SPEEDCAM_URL2] = url
                    3 -> it[KEY_SPEEDCAM_URL3] = url
                }
            }
        }
    }

    fun setSpeedcamAutoUpdate(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_SPEEDCAM_AUTO_UPD] = enabled }
            if (enabled) {
                SpeedCamUpdateWorker.schedule(context)
            } else {
                SpeedCamUpdateWorker.cancel(context)
            }
        }
    }

    fun updateSpeedcamDatabases() {
        viewModelScope.launch {
            _isUpdatingSpeedcam.value = true
            _speedcamUpdateResult.value = null
            val s = settingsState.value
            val urls = listOf(s.speedcamUrl1, s.speedcamUrl2, s.speedcamUrl3).filter { it.isNotBlank() }
            if (urls.isEmpty()) {
                _speedcamUpdateResult.value = "Нет источников для обновления"
                _isUpdatingSpeedcam.value = false
                return@launch
            }
            val stats = speedCamRepository.updateFromSources(urls)
            dataStore.edit { it[KEY_SPEEDCAM_LAST_UPD] = stats.timestamp.toString() }
            _speedcamUpdateResult.value = stats.summary
            _isUpdatingSpeedcam.value = false

            SpeedCamNotificationHelper.notifyUpdateSuccess(context, stats.summary)
        }
    }

    fun clearUpdateResult() { _speedcamUpdateResult.value = null }
}
