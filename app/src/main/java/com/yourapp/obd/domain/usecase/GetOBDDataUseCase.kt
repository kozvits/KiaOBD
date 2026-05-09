package com.yourapp.obd.domain.usecase

import com.yourapp.obd.data.bluetooth.BluetoothOBDRepository
import com.yourapp.obd.data.bluetooth.ConnectionState
import com.yourapp.obd.domain.model.OBDData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetOBDDataUseCase @Inject constructor(
    private val repository: BluetoothOBDRepository
) {
    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    operator fun invoke(): Flow<OBDData> = repository.obdDataFlow()
}
