package com.yourapp.obd.ui.dashboard

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.camera.view.PreviewView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.obd.data.bluetooth.BluetoothOBDRepository
import com.yourapp.obd.data.bluetooth.ConnectionState
import com.yourapp.obd.data.camera.CameraRepository
import com.yourapp.obd.data.db.TripDao
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

data class CurrentTripInfo(
    val distanceKm: Float = 0f,
    val durationMinutes: Long = 0,
    val avgSpeedKmh: Int = 0,
    val isInProgress: Boolean = false
)

data class AdasModuleState(
    val name: String,
    val label: String,
    val enabled: Boolean
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val obdRepository: BluetoothOBDRepository,
    private val cameraRepository: CameraRepository,
    private val accelerometerRepository: AccelerometerRepository,
    private val getOBDDataUseCase: GetOBDDataUseCase,
    private val detectAdasUseCase: DetectAdasUseCase,
    private val recordVideoUseCase: RecordVideoUseCase,
    private val dataStore: DataStore<Preferences>,
    private val tripDao: TripDao
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

    private val _currentTrip = MutableStateFlow(CurrentTripInfo())
    val currentTrip: StateFlow<CurrentTripInfo> = _currentTrip.asStateFlow()

    private val _obdAlert = MutableStateFlow<String?>(null)
    val obdAlert: StateFlow<String?> = _obdAlert.asStateFlow()

    // ADAS Quick Toggle
    private val _showAdasSheet = MutableStateFlow(false)
    val showAdasSheet: StateFlow<Boolean> = _showAdasSheet.asStateFlow()

    private val _adasModules = MutableStateFlow<List<AdasModuleState>>(emptyList())
    val adasModules: StateFlow<List<AdasModuleState>> = _adasModules.asStateFlow()

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

    companion object {
        val KEY_ENABLE_LDW = booleanPreferencesKey("adas_enable_ldw")
        val KEY_ENABLE_FCW = booleanPreferencesKey("adas_enable_fcw")
        val KEY_ENABLE_DMS = booleanPreferencesKey("adas_enable_dms")
        val KEY_ENABLE_SPEED_LIMIT = booleanPreferencesKey("adas_enable_speed_limit")
        val KEY_ENABLE_PEDESTRIAN = booleanPreferencesKey("adas_enable_pedestrian")
        val KEY_ENABLE_DISTRACTION = booleanPreferencesKey("adas_enable_distraction")
    }

    private val adasModuleKeys = listOf(
        Triple(KEY_ENABLE_LDW, "LDW", "Контроль полосы"),
        Triple(KEY_ENABLE_FCW, "FCW", "Дистанция спереди"),
        Triple(KEY_ENABLE_DMS, "DMS", "Усталость водителя"),
        Triple(KEY_ENABLE_SPEED_LIMIT, "Скорость", "Лимиты скорости"),
        Triple(KEY_ENABLE_PEDESTRIAN, "Пешеход", "Детекция пешеходов"),
        Triple(KEY_ENABLE_DISTRACTION, "Внимание", "Отвлечение водителя")
    )

    init {
        collectOBD()
        collectAlerts()
        collectDistance()
        collectImpacts()
        autoConnectOBD()
        collectTripData()
        monitorObdAlerts()
        collectAdasModuleStates()
    }

    fun bindCamera(owner: LifecycleOwner, previewView: PreviewView) {
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
                val distance = cameraRepository.lastVehicleDistanceM
                _fcwDistanceM.value = distance
                if (distance == null && _lastAlert.value is AdasAlert.ForwardCollision) {
                    _lastAlert.value = null
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    private fun collectImpacts() {
        viewModelScope.launch {
            accelerometerRepository.impactEvents().collect { recordVideoUseCase.protect() }
        }
    }

    private fun collectTripData() {
        viewModelScope.launch {
            tripDao.getAllFlow().collect { trips ->
                if (trips.isNotEmpty()) {
                    val last = trips.first()
                    val durationMinutes =
                        (last.endTimestamp - last.startTimestamp) / 1000L / 60L
                    _currentTrip.value = CurrentTripInfo(
                        distanceKm = last.distanceKm,
                        durationMinutes = durationMinutes,
                        avgSpeedKmh = last.avgSpeedKmh,
                        isInProgress = durationMinutes < 5 &&
                            System.currentTimeMillis() - last.endTimestamp < 60000
                    )
                }
            }
        }
    }

    private fun monitorObdAlerts() {
        viewModelScope.launch {
            while (true) {
                val data = _obdData.value
                _obdAlert.value = when {
                    data.coolantTempC != null && data.coolantTempC >= 100 ->
                        "⚠ ОЖ ${data.coolantTempC}°C — перегрев!"
                    data.voltageV != null && data.voltageV < 11.5f ->
                        "⚠ Напряжение ${"%.1f".format(data.voltageV)}В — низкое!"
                    data.coolantTempC != null && data.coolantTempC >= 95 ->
                        "⚡ ОЖ ${data.coolantTempC}°C — высокая температура"
                    else -> null
                }
                delay(2000)
            }
        }
    }

    private fun collectAdasModuleStates() {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                _adasModules.value = adasModuleKeys.map { (key, name, label) ->
                    AdasModuleState(name, label, prefs[key] ?: true)
                }
            }
        }
    }

    fun toggleAdasModule(moduleName: String) {
        viewModelScope.launch {
            val (key, _, _) = adasModuleKeys.first { it.second == moduleName }
            val current = dataStore.data.first()[key] ?: true
            dataStore.edit { it[key] = !current }
        }
    }

    fun showAdasSheet() { _showAdasSheet.value = true }
    fun hideAdasSheet() { _showAdasSheet.value = false }

    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch { obdRepository.connectToDevice(device) }
    }

    private fun autoConnectOBD() {
        viewModelScope.launch {
            val address = dataStore.data.first()[KEY_DEVICE_ADDRESS] ?: return@launch
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) return@launch
            val device = adapter.getRemoteDevice(address)
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

    private val KEY_DEVICE_ADDRESS = SettingsViewModel.KEY_DEVICE_ADDRESS

    override fun onCleared() {
        cameraRepository.stopRecording()
        super.onCleared()
    }
}
