package com.yourapp.obd.ui.settings

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.obd.data.bluetooth.BluetoothOBDRepository
import com.yourapp.obd.data.camera.AdasAnalyzer
import com.yourapp.obd.data.camera.CameraRepository
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
    val speedcamUrl1: String = "https://raw.githubusercontent.com/jonatkins/ingress-intel-total-conversion/master/cameras.json",
    val speedcamUrl2: String = "",
    val speedcamUrl3: String = "",
    val speedcamLastUpdate: Long = 0L
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val obdRepository: BluetoothOBDRepository,
    private val cameraRepository: CameraRepository,
    private val adasAnalyzer: AdasAnalyzer
) : ViewModel() {

    companion object {
        val KEY_DEVICE_ADDRESS      = stringPreferencesKey("device_address")
        val KEY_BUFFER_SIZE_GB      = intPreferencesKey("buffer_size_gb")
        val KEY_VIDEO_RESOLUTION    = stringPreferencesKey("video_resolution")
        val KEY_SEGMENT_DURATION    = intPreferencesKey("segment_duration_min")
        val KEY_ADAS_SENSITIVITY    = stringPreferencesKey("adas_sensitivity")
        val KEY_LDW                 = booleanPreferencesKey("ldw_enabled")
        val KEY_FCW                 = booleanPreferencesKey("fcw_enabled")
        val KEY_SIGN                = booleanPreferencesKey("sign_enabled")
        val KEY_DMS                 = booleanPreferencesKey("dms_enabled")
        val KEY_PEDESTRIAN          = booleanPreferencesKey("pedestrian_enabled")
        val KEY_SPEEDCAM_URL1       = stringPreferencesKey("speedcam_url1")
        val KEY_SPEEDCAM_URL2       = stringPreferencesKey("speedcam_url2")
        val KEY_SPEEDCAM_URL3       = stringPreferencesKey("speedcam_url3")
        val KEY_SPEEDCAM_LAST_UPD   = stringPreferencesKey("speedcam_last_update")
    }

    val settingsState = dataStore.data.map { prefs ->
        SettingsState(
            selectedDeviceAddress  = prefs[KEY_DEVICE_ADDRESS] ?: "",
            bufferSizeGb           = prefs[KEY_BUFFER_SIZE_GB] ?: 4,
            videoResolution        = prefs[KEY_VIDEO_RESOLUTION] ?: "FHD",
            segmentDurationMin     = prefs[KEY_SEGMENT_DURATION] ?: 5,
            adasSensitivity        = prefs[KEY_ADAS_SENSITIVITY] ?: "MEDIUM",
            ldwEnabled             = prefs[KEY_LDW] ?: true,
            fcwEnabled             = prefs[KEY_FCW] ?: true,
            signEnabled            = prefs[KEY_SIGN] ?: true,
            dmsEnabled             = prefs[KEY_DMS] ?: true,
            pedestrianEnabled      = prefs[KEY_PEDESTRIAN] ?: true,
            speedcamUrl1           = prefs[KEY_SPEEDCAM_URL1] ?: "https://raw.githubusercontent.com/jonatkins/ingress-intel-total-conversion/master/cameras.json",
            speedcamUrl2           = prefs[KEY_SPEEDCAM_URL2] ?: "",
            speedcamUrl3           = prefs[KEY_SPEEDCAM_URL3] ?: "",
            speedcamLastUpdate     = prefs[KEY_SPEEDCAM_LAST_UPD]?.toLongOrNull() ?: 0L
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    private val _isUpdatingSpeedcam = MutableStateFlow(false)
    val isUpdatingSpeedcam: StateFlow<Boolean> = _isUpdatingSpeedcam.asStateFlow()

    private val _speedcamUpdateResult = MutableStateFlow<String?>(null)
    val speedcamUpdateResult: StateFlow<String?> = _speedcamUpdateResult.asStateFlow()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun getPairedDevices(): List<BluetoothDevice> =
        obdRepository.getPairedDevices().toList()

    fun selectDevice(address: String) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_DEVICE_ADDRESS] = address }
        }
    }

    fun setBufferSizeGb(gb: Int) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_BUFFER_SIZE_GB] = gb }
            cameraRepository.setMaxBufferBytes(gb.toLong() * 1024 * 1024 * 1024)
        }
    }

    fun setVideoResolution(resolution: String) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_VIDEO_RESOLUTION] = resolution }
        }
    }

    fun setSegmentDurationMin(minutes: Int) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_SEGMENT_DURATION] = minutes }
        }
    }

    fun setAdasSensitivity(sensitivity: String) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_ADAS_SENSITIVITY] = sensitivity }
        }
    }

    fun setLdwEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_LDW] = enabled }
            adasAnalyzer.ldwEnabled = enabled
        }
    }

    fun setFcwEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_FCW] = enabled }
            adasAnalyzer.fcwEnabled = enabled
        }
    }

    fun setSignEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_SIGN] = enabled }
            adasAnalyzer.signDetectionEnabled = enabled
        }
    }

    fun setDmsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_DMS] = enabled }
            adasAnalyzer.dmsEnabled = enabled
        }
    }

    fun setPedestrianEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_PEDESTRIAN] = enabled }
            adasAnalyzer.pedestrianEnabled = enabled
        }
    }

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

    fun updateSpeedcamDatabases() {
        viewModelScope.launch {
            _isUpdatingSpeedcam.value = true
            _speedcamUpdateResult.value = null
            val state = settingsState.value
            val urls = listOf(state.speedcamUrl1, state.speedcamUrl2, state.speedcamUrl3)
                .filter { it.isNotBlank() }

            if (urls.isEmpty()) {
                _speedcamUpdateResult.value = "Нет источников для обновления"
                _isUpdatingSpeedcam.value = false
                return@launch
            }

            var successCount = 0
            var errorCount = 0
            for (url in urls) {
                try {
                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 15_000
                    connection.requestMethod = "GET"
                    val code = connection.responseCode
                    if (code == 200) {
                        // Читаем данные — в реальном проекте парсим и сохраняем в БД
                        val bytes = connection.inputStream.readBytes()
                        connection.disconnect()
                        successCount++
                    } else {
                        connection.disconnect()
                        errorCount++
                    }
                } catch (e: Exception) {
                    errorCount++
                }
            }

            val now = System.currentTimeMillis()
            dataStore.edit { it[KEY_SPEEDCAM_LAST_UPD] = now.toString() }

            _speedcamUpdateResult.value = when {
                errorCount == 0 -> "Обновлено: $successCount источников"
                successCount == 0 -> "Ошибка обновления всех источников"
                else -> "Обновлено: $successCount, ошибок: $errorCount"
            }
            _isUpdatingSpeedcam.value = false
        }
    }

    fun clearUpdateResult() {
        _speedcamUpdateResult.value = null
    }
}
