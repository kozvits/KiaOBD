package com.yourapp.obd.ui.dtc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.obd.data.bluetooth.BluetoothOBDRepository
import com.yourapp.obd.data.db.DtcDao
import com.yourapp.obd.data.db.DtcEntity
import com.yourapp.obd.domain.model.DTCCode
import com.yourapp.obd.domain.model.DtcSeverity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DtcViewModel @Inject constructor(
    private val obdRepository: BluetoothOBDRepository,
    private val dtcDao: DtcDao
) : ViewModel() {

    val dtcCodes: StateFlow<List<DTCCode>> = dtcDao.getAllFlow()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showClearDialog = MutableStateFlow(false)
    val showClearDialog: StateFlow<Boolean> = _showClearDialog.asStateFlow()

    fun readDtcCodes() {
        viewModelScope.launch {
            _isLoading.value = true
            val codes = obdRepository.readDtcCodes()
            val entities = codes.map { code ->
                DtcEntity(
                    code = code,
                    description = describeDtc(code),
                    severity = severityOf(code).name,
                    timestamp = System.currentTimeMillis()
                )
            }
            dtcDao.insertAll(entities)
            _isLoading.value = false
        }
    }

    fun requestClearDtc() { _showClearDialog.value = true }
    fun dismissClearDialog() { _showClearDialog.value = false }

    fun confirmClearDtc() {
        viewModelScope.launch {
            _showClearDialog.value = false
            _isLoading.value = true
            val ok = obdRepository.clearDtcCodes()
            if (ok) dtcDao.deleteAll()
            _isLoading.value = false
        }
    }

    private fun DtcEntity.toDomain() = DTCCode(
        code = code,
        description = description,
        severity = DtcSeverity.valueOf(severity),
        timestamp = timestamp
    )

    private fun severityOf(code: String): DtcSeverity = when {
        code.startsWith("P0") -> DtcSeverity.LOW
        code.startsWith("P1") -> DtcSeverity.MEDIUM
        else -> DtcSeverity.HIGH
    }

    private fun describeDtc(code: String): String = when (code) {
        "P0100" -> "Цепь датчика массового расхода воздуха"
        "P0101" -> "Диапазон/производительность MAF"
        "P0110" -> "Цепь датчика температуры воздуха на впуске"
        "P0115" -> "Цепь датчика температуры охлаждающей жидкости"
        "P0120" -> "Цепь датчика положения дроссельной заслонки"
        "P0130" -> "Цепь лямбда-зонда (банк 1, сенсор 1)"
        "P0171" -> "Бедная смесь (банк 1)"
        "P0172" -> "Богатая смесь (банк 1)"
        "P0300" -> "Случайные пропуски зажигания"
        "P0301" -> "Пропуск зажигания — цилиндр 1"
        "P0302" -> "Пропуск зажигания — цилиндр 2"
        "P0303" -> "Пропуск зажигания — цилиндр 3"
        "P0304" -> "Пропуск зажигания — цилиндр 4"
        "P0335" -> "Цепь датчика положения коленвала (CKP)"
        "P0340" -> "Цепь датчика положения распредвала (CMP)"
        "P0400" -> "Неисправность системы рециркуляции ОГ (EGR)"
        "P0420" -> "Эффективность катализатора ниже нормы"
        "P0500" -> "Цепь датчика скорости автомобиля"
        "P0505" -> "Система управления холостым ходом"
        "P0560" -> "Напряжение системы ниже нормы"
        "P0601" -> "Ошибка контрольной суммы ЭБУ"
        "P0605" -> "Ошибка ПЗУ ЭБУ"
        else -> "Неизвестная ошибка — обратитесь в сервис"
    }
}
