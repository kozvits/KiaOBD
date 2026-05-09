package com.yourapp.obd.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class OBDConnection(private val context: Context) {

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null
    private var protocol: ELM327Protocol? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun connect(device: BluetoothDevice): ELM327Protocol? = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
            s.connect()
            socket = s
            val proto = ELM327Protocol(s.inputStream, s.outputStream)
            val ok = proto.initialize()
            if (ok) {
                protocol = proto
                proto
            } else {
                s.close()
                socket = null
                null
            }
        } catch (e: Exception) {
            socket?.close()
            socket = null
            null
        }
    }

    fun disconnect() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        protocol = null
    }

    fun isConnected(): Boolean = socket?.isConnected == true
}
