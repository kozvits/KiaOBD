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
import com.yourapp.obd.data.speedcam.SpeedCamConstants
import com.yourapp.obd.data.speedcam.SpeedCamNotificationHelper
import com.yourapp.obd.data.speedcam.SpeedCamRepository
import com.yourapp.obd.data.speedcam.SpeedCamSourceProvider
import com.yourapp.obd.data.speedcam.SpeedCamUpdateLogEntity
import com.yourapp.obd.data.speedcam.SpeedCamUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val speedcamTotalCameras: Int = 0,
    val speedcamRollbackAvailable: Boolean = false,
    val speedcamUpdateHistory: List<SpeedCamUpdateHistoryItem> = emptyList()
)

data class SpeedCamUpdateHistoryItem(
    val timestamp: Long,
    val summary: String,
    val newCount: Int,
    val removedCount: Int,
    val modifiedCount: Int,
    val totalActive: Int,
    val rollbackAvailable: Boolean
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

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    private val _isUpdatingSpeedcam = MutableStateFlow(false)
    val isUpdatingSpeedcam: StateFlow<Boolean> = _isUpdatingSpeedcam.asStateFlow()

    private val _speedcamUpdateResult = MutableStateFlow<String?>(null)
    val speedcamUpdateResult: StateFlow<String?> = _speedcamUpdateResult.asStateFlow()

    private val _isRollingBack = MutableStateFlow(false)
    val isRollingBack: StateFlow<Boolean> = _isRollingBack.asStateFlow()

    init {
        viewModelScope.launch {
            speedCamRepository.lastUpdateResult.collect { result ->
                if (result != null) {
                    _speedcamUpdateResult.value = result
                    refreshSpeedcamDbState()
                }
            }
        }
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                _settingsState.value = _settingsState.value.copy(
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
                    horizonPosition = prefs[KEY_HORIZON] ?: 0.42f,
                    laneWidthPercent = prefs[KEY_LANE_WIDTH] ?: 0.28f,
                    vanishingPointX = prefs[KEY_VP_X] ?: 0.5f,
                    dangerZoneM = prefs[KEY_DANGER_M] ?: 5,
                    warningZoneM = prefs[KEY_WARNING_M] ?: 10,
                    cautionZoneM = prefs[KEY_CAUTION_M] ?: 20,
                    speedcamUrl1 = prefs[KEY_SPEEDCAM_URL1] ?: "",
                    speedcamUrl2 = prefs[KEY_SPEEDCAM_URL2] ?: "",
                    speedcamUrl3 = prefs[KEY_SPEEDCAM_URL3] ?: "",
                    speedcamLastUpdate = prefs[KEY_SPEEDCAM_LAST_UPD]?.toLongOrNull() ?: 0L,
                    speedcamAutoUpdate = prefs[KEY_SPEEDCAM_AUTO_UPD] ?: true
                )
            }
        }
        viewModelScope.launch {
            refreshSpeedcamDbState()
        }
    }

    private suspend fun refreshSpeedcamDbState() {
        try {
            val history = speedCamRepository.getUpdateHistory()
            val snapshotInfo = speedCamRepository.rollbackManager.getSnapshotInfo()
            val totalActive = speedCamRepository.getTotalActive()
            _settingsState.value = _settingsState.value.copy(
                speedcamTotalCameras = totalActive,
                speedcamRollbackAvailable = snapshotInfo.available,
                speedcamUpdateHistory = history.map { it.toHistoryItem() }
            )
        } catch (_: Exception) { }
    }

    private fun SpeedCamUpdateLogEntity.toHistoryItem() = SpeedCamUpdateHistoryItem(
        timestamp = timestamp,
        summary = summary,
        newCount = newCameras,
        removedCount = removedCameras,
        modifiedCount = modifiedCameras,
        totalActive = totalActive,
        rollbackAvailable = rollbackAvailable
    )

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

    fun applySpeedcamPreset(index: Int) {
        val preset = SpeedCamConstants.PRESETS.getOrNull(index) ?: return
        viewModelScope.launch {
            dataStore.edit {
                it[KEY_SPEEDCAM_URL1] = preset.url1
                it[KEY_SPEEDCAM_URL2] = preset.url2
                it[KEY_SPEEDCAM_URL3] = preset.url3
            }
        }
    }

    fun applyCountryPreset() {
        val countryName = SpeedCamSourceProvider.extractCountryName()
        val overpassUrl = if (countryName != null) {
            SpeedCamSourceProvider.buildOverpassUrl(countryName)
        } else {
            SpeedCamSourceProvider.buildOverpassUrl()
        }
        viewModelScope.launch {
            dataStore.edit {
                it[KEY_SPEEDCAM_URL1] = overpassUrl
                it[KEY_SPEEDCAM_URL2] = ""
                it[KEY_SPEEDCAM_URL3] = ""
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

            refreshSpeedcamDbState()

            if (!stats.isError) {
                SpeedCamNotificationHelper.notifyUpdateSuccess(context, stats.summary)
            }
        }
    }

    fun rollbackSpeedcamUpdate() {
        viewModelScope.launch {
            _isRollingBack.value = true
            val stats = speedCamRepository.rollbackLastUpdate()
            refreshSpeedcamDbState()
            _speedcamUpdateResult.value = if (!stats.isError) {
                "Откат выполнен успешно. Всего камер: ${stats.totalActive}"
            } else {
                "Откат недоступен — нет снапшота"
            }
            _isRollingBack.value = false
        }
    }

    fun clearUpdateResult() { _speedcamUpdateResult.value = null }
}
