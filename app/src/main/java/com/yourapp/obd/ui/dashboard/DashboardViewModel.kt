package com.yourapp.obd.ui.dashboard

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdasCalibration(
    val horizonPct: Float   = 42f,
    val laneWidthPct: Float = 28f,
    val dangerDistM: Int    = 5,
    val warningDistM: Int   = 10,
    val cautionDistM: Int   = 20,
    val safeDistM: Int      = 30
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

    /** Параметры калибровки ADAS — читаются из DataStore */
    val adasCalibration: StateFlow<AdasCalibration> = dataStore.data.map { p ->
        AdasCalibration(
            horizonPct   = p[SettingsViewModel.KEY_ADAS_HORIZON] ?: 42f,
            laneWidthPct = p[SettingsViewModel.KEY_ADAS_LANE_WIDTH] ?: 28f,
            dangerDistM  = p[SettingsViewModel.KEY_ADAS_DANGER_DIST] ?: 5,
            warningDistM = p[SettingsViewModel.KEY_ADAS_WARNING_DIST] ?: 10,
            cautionDistM = p[SettingsViewModel.KEY_ADAS_CAUTION_DIST] ?: 20,
            safeDistM    = p[SettingsViewModel.KEY_ADAS_SAFE_DIST] ?: 30
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdasCalibration())

    private var cameraIsBound = false

    init {
        collectOBD()
        collectAlerts()
        collectImpacts()
    }

    fun bindCamera(owner: LifecycleOwner, previewView: PreviewView) {
        if (cameraIsBound) return
        cameraIsBound = true
        cameraRepository.bindCamera(owner, previewView)
        cameraRepository.startRecording()
    }

    private fun collectOBD() {
        viewModelScope.launch {
            getOBDDataUseCase().catch { }.collect { data ->
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

    private fun collectImpacts() {
        viewModelScope.launch {
            accelerometerRepository.impactEvents().collect { recordVideoUseCase.protect() }
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch { obdRepository.connectToDevice(device) }
    }

    fun startRecording() = recordVideoUseCase.start()
    fun stopRecording()  = recordVideoUseCase.stop()

    override fun onCleared() {
        super.onCleared()
        cameraIsBound = false
    }
}
