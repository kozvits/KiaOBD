package com.yourapp.obd.ui.dashboard

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.camera.view.PreviewView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.obd.data.bluetooth.BluetoothOBDRepository
import com.yourapp.obd.data.bluetooth.ConnectionState
import com.yourapp.obd.data.camera.CameraRepository
import com.yourapp.obd.data.sensor.AccelerometerRepository
import com.yourapp.obd.domain.model.AdasAlert
import com.yourapp.obd.domain.model.OBDData
import com.yourapp.obd.domain.usecase.DetectAdasUseCase
import com.yourapp.obd.domain.usecase.GetOBDDataUseCase
import com.yourapp.obd.domain.usecase.RecordVideoUseCase
import com.yourapp.obd.ui.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdasCalibration(
    val horizonPosition: Float  = 0.42f,
    val laneWidthPercent: Float = 0.28f,
    val vanishingPointX: Float  = 0.5f,
    val dangerZoneM: Int        = 5,
    val warningZoneM: Int       = 10,
    val cautionZoneM: Int       = 20
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val obdRepository: BluetoothOBDRepository,
    private val cameraRepository: CameraRepository,
    private val accelerometerRepository: AccelerometerRepository,
    private val getOBDDataUseCase: GetOBDDataUseCase,
    private val detectAdasUseCase: DetectAdasUseCase,
    private val recordVideoUseCase: RecordVideoUseCase,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = obdRepository.connectionState
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionState.DISCONNECTED)

    val isRecording: StateFlow<Boolean> = recordVideoUseCase.isRecording
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _obdData = MutableStateFlow(OBDData())
    val obdData: StateFlow<OBDData> = _obdData.asStateFlow()

    private val _lastAlert = MutableStateFlow<AdasAlert?>(null)
    val lastAlert: StateFlow<AdasAlert?> = _lastAlert.asStateFlow()

    private val _fcwDistanceM = MutableStateFlow<Float?>(null)
    val fcwDistanceM: StateFlow<Float?> = _fcwDistanceM.asStateFlow()

    // Калибровка ADAS из DataStore
    val adasCalibration: StateFlow<AdasCalibration> = dataStore.data.map { p ->
        AdasCalibration(
            horizonPosition  = p[SettingsViewModel.KEY_HORIZON]    ?: 0.42f,
            laneWidthPercent = p[SettingsViewModel.KEY_LANE_WIDTH]  ?: 0.28f,
            vanishingPointX  = p[SettingsViewModel.KEY_VP_X]        ?: 0.5f,
            dangerZoneM      = p[SettingsViewModel.KEY_DANGER_M]    ?: 5,
            warningZoneM     = p[SettingsViewModel.KEY_WARNING_M]   ?: 10,
            cautionZoneM     = p[SettingsViewModel.KEY_CAUTION_M]   ?: 20
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AdasCalibration())

    private var cameraIsBound = false

    init {
        collectOBD()
        collectAlerts()
        collectDistance()
        collectImpacts()
        autoConnectOBD()
    }

    fun bindCamera(owner: LifecycleOwner, previewView: PreviewView) {
        if (cameraIsBound) return
        cameraIsBound = true
        cameraRepository.bindCamera(owner, previewView)
        cameraRepository.startRecording()
    }

    private fun collectOBD() {
        viewModelScope.launch {
            getOBDDataUseCase()
                .catch { }
                .collect { data ->
                    _obdData.value = data
                    data.speedKmh?.let { cameraRepository.updateObdSpeed(it) }
                }
        }
    }

    private fun collectAlerts() {
        viewModelScope.launch {
            detectAdasUseCase().collect { _lastAlert.value = it }
        }
    }

    private fun collectDistance() {
        viewModelScope.launch {
            while (true) {
                _fcwDistanceM.value = cameraRepository.lastVehicleDistanceM
                kotlinx.coroutines.delay(100)
            }
        }
    }

    private fun collectImpacts() {
        viewModelScope.launch {
            accelerometerRepository.impactEvents().collect { recordVideoUseCase.protect() }
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch { obdRepository.connectToDevice(device) }
    }

    private fun autoConnectOBD() {
        viewModelScope.launch {
            val address = dataStore.data.first()[KEY_DEVICE_ADDRESS] ?: return@launch
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) return@launch
            val device = adapter.getRemoteDevice(address)
            // Циклическая попытка подключения: если не удалось — ждём 3с и повторяем
            while (true) {
                if (connectionState.value != ConnectionState.CONNECTED) {
                    obdRepository.connectToDevice(device)
                }
                delay(3000)
            }
        }
    }

    fun startRecording() = recordVideoUseCase.start()
    fun stopRecording()  = recordVideoUseCase.stop()

    companion object {
        val KEY_DEVICE_ADDRESS = stringPreferencesKey("obd_device_address")
    }

    override fun onCleared() {
        super.onCleared()
        cameraIsBound = false
    }
}
