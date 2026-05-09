package com.yourapp.obd.ui.settings

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.obd.data.bluetooth.BluetoothOBDRepository
import com.yourapp.obd.data.camera.AdasAnalyzer
import com.yourapp.obd.data.camera.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val selectedDeviceAddress: String = "",
    val recordingFolderPath: String = "",
    val bufferSizeGb: Int = 4,
    val adasSensitivity: String = "MEDIUM",
    val ldwEnabled: Boolean = true,
    val fcwEnabled: Boolean = true,
    val signEnabled: Boolean = true,
    val dmsEnabled: Boolean = true,
    val pedestrianEnabled: Boolean = true
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
        val KEY_DEVICE_ADDRESS = stringPreferencesKey("device_address")
        val KEY_FOLDER_PATH = stringPreferencesKey("folder_path")
        val KEY_BUFFER_SIZE_GB = intPreferencesKey("buffer_size_gb")
        val KEY_ADAS_SENSITIVITY = stringPreferencesKey("adas_sensitivity")
        val KEY_LDW = booleanPreferencesKey("ldw_enabled")
        val KEY_FCW = booleanPreferencesKey("fcw_enabled")
        val KEY_SIGN = booleanPreferencesKey("sign_enabled")
        val KEY_DMS = booleanPreferencesKey("dms_enabled")
        val KEY_PEDESTRIAN = booleanPreferencesKey("pedestrian_enabled")
    }

    val settingsState = dataStore.data.map { prefs ->
        SettingsState(
            selectedDeviceAddress = prefs[KEY_DEVICE_ADDRESS] ?: "",
            recordingFolderPath = prefs[KEY_FOLDER_PATH] ?: "",
            bufferSizeGb = prefs[KEY_BUFFER_SIZE_GB] ?: 4,
            adasSensitivity = prefs[KEY_ADAS_SENSITIVITY] ?: "MEDIUM",
            ldwEnabled = prefs[KEY_LDW] ?: true,
            fcwEnabled = prefs[KEY_FCW] ?: true,
            signEnabled = prefs[KEY_SIGN] ?: true,
            dmsEnabled = prefs[KEY_DMS] ?: true,
            pedestrianEnabled = prefs[KEY_PEDESTRIAN] ?: true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun getPairedDevices(): List<BluetoothDevice> =
        obdRepository.getPairedDevices().toList()

    fun selectDevice(address: String) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_DEVICE_ADDRESS] = address }
        }
    }

    fun setFolderPath(path: String) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_FOLDER_PATH] = path }
        }
    }

    fun setBufferSizeGb(gb: Int) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_BUFFER_SIZE_GB] = gb }
            cameraRepository.setMaxBufferBytes(gb.toLong() * 1024 * 1024 * 1024)
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
}
