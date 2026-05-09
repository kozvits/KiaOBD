package com.yourapp.obd.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.annotation.RequiresPermission
import com.yourapp.obd.domain.model.OBDData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

interface BluetoothOBDRepository {
    val connectionState: StateFlow<ConnectionState>
    fun getPairedDevices(): Set<BluetoothDevice>
    suspend fun connectToDevice(device: BluetoothDevice)
    fun disconnect()
    fun obdDataFlow(): Flow<OBDData>
    suspend fun readDtcCodes(): List<String>
    suspend fun clearDtcCodes(): Boolean
}

@Singleton
class BluetoothOBDRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BluetoothOBDRepository {

    private val connection = OBDConnection(context)
    private var protocol: ELM327Protocol? = null
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun getPairedDevices(): Set<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptySet()
        return adapter.bondedDevices ?: emptySet()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun connectToDevice(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.CONNECTING
        var backoff = 1000L
        repeat(5) { attempt ->
            val proto = connection.connect(device)
            if (proto != null) {
                protocol = proto
                _connectionState.value = ConnectionState.CONNECTED
                return
            }
            if (attempt < 4) {
                delay(backoff)
                backoff = minOf(backoff * 2, 16000L)
            }
        }
        _connectionState.value = ConnectionState.ERROR
    }

    override fun disconnect() {
        connection.disconnect()
        protocol = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun obdDataFlow(): Flow<OBDData> = flow {
        while (true) {
            val proto = protocol
            if (proto == null) {
                delay(500)
                continue
            }
            try {
                val rpm = queryRpm(proto)
                delay(100)
                val speed = querySpeed(proto)
                delay(100)
                val coolant = queryCoolant(proto)
                delay(100)
                val map = queryMap(proto)
                delay(100)
                val iat = queryIat(proto)
                delay(100)
                val throttle = queryThrottle(proto)
                delay(100)
                val fuel = queryFuel(proto)
                delay(100)
                val timing = queryTiming(proto)
                delay(100)
                emit(
                    OBDData(
                        rpm = rpm,
                        speedKmh = speed,
                        coolantTempC = coolant,
                        mapKpa = map,
                        intakeAirTempC = iat,
                        throttlePercent = throttle,
                        fuelLevelPercent = fuel,
                        timingAdvanceDeg = timing
                    )
                )
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.ERROR
                delay(2000)
            }
        }
    }

    override suspend fun readDtcCodes(): List<String> {
        val proto = protocol ?: return emptyList()
        val response = proto.sendCommandWithRetry("03") ?: return emptyList()
        return proto.parseDtcResponse(response)
    }

    override suspend fun clearDtcCodes(): Boolean {
        val proto = protocol ?: return false
        val response = proto.sendCommandWithRetry("04") ?: return false
        return !response.contains("ERROR")
    }

    private suspend fun queryRpm(proto: ELM327Protocol): Int? {
        val r = proto.sendCommandWithRetry("010C") ?: return null
        val bytes = proto.parseObdResponse(r) ?: return null
        if (bytes.size < 4) return null
        return ((bytes[2] * 256) + bytes[3]) / 4
    }

    private suspend fun querySpeed(proto: ELM327Protocol): Int? {
        val r = proto.sendCommandWithRetry("010D") ?: return null
        val bytes = proto.parseObdResponse(r) ?: return null
        if (bytes.size < 3) return null
        return bytes[2]
    }

    private suspend fun queryCoolant(proto: ELM327Protocol): Int? {
        val r = proto.sendCommandWithRetry("0105") ?: return null
        val bytes = proto.parseObdResponse(r) ?: return null
        if (bytes.size < 3) return null
        return bytes[2] - 40
    }

    private suspend fun queryMap(proto: ELM327Protocol): Int? {
        val r = proto.sendCommandWithRetry("010B") ?: return null
        val bytes = proto.parseObdResponse(r) ?: return null
        if (bytes.size < 3) return null
        return bytes[2]
    }

    private suspend fun queryIat(proto: ELM327Protocol): Int? {
        val r = proto.sendCommandWithRetry("010F") ?: return null
        val bytes = proto.parseObdResponse(r) ?: return null
        if (bytes.size < 3) return null
        return bytes[2] - 40
    }

    private suspend fun queryThrottle(proto: ELM327Protocol): Float? {
        val r = proto.sendCommandWithRetry("0111") ?: return null
        val bytes = proto.parseObdResponse(r) ?: return null
        if (bytes.size < 3) return null
        return bytes[2] * 100f / 255f
    }

    private suspend fun queryFuel(proto: ELM327Protocol): Float? {
        val r = proto.sendCommandWithRetry("012F") ?: return null
        val bytes = proto.parseObdResponse(r) ?: return null
        if (bytes.size < 3) return null
        return bytes[2] * 100f / 255f
    }

    private suspend fun queryTiming(proto: ELM327Protocol): Float? {
        val r = proto.sendCommandWithRetry("010E") ?: return null
        val bytes = proto.parseObdResponse(r) ?: return null
        if (bytes.size < 3) return null
        return bytes[2] / 2f - 64f
    }
}
