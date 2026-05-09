package com.yourapp.obd.ui.dashboard

import android.bluetooth.BluetoothDevice
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val obdRepository: BluetoothOBDRepository,
    private val cameraRepository: CameraRepository,
    private val accelerometerRepository: AccelerometerRepository,
    private val getOBDDataUseCase: GetOBDDataUseCase,
    private val detectAdasUseCase: DetectAdasUseCase,
    private val recordVideoUseCase: RecordVideoUseCase
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = obdRepository.connectionState
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionState.DISCONNECTED)

    val isRecording: StateFlow<Boolean> = recordVideoUseCase.isRecording
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _obdData = MutableStateFlow(OBDData())
    val obdData: StateFlow<OBDData> = _obdData.asStateFlow()

    private val _lastAlert = MutableStateFlow<AdasAlert?>(null)
    val lastAlert: StateFlow<AdasAlert?> = _lastAlert.asStateFlow()

    init {
        collectOBD()
        collectAlerts()
        collectImpacts()
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
            detectAdasUseCase().collect { alert ->
                _lastAlert.value = alert
            }
        }
    }

    private fun collectImpacts() {
        viewModelScope.launch {
            accelerometerRepository.impactEvents().collect {
                recordVideoUseCase.protect()
            }
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            obdRepository.connectToDevice(device)
        }
    }

    fun startRecording() = recordVideoUseCase.start()
    fun stopRecording() = recordVideoUseCase.stop()
}
