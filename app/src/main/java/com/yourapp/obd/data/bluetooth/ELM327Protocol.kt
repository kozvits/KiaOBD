package com.yourapp.obd.data.bluetooth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.io.OutputStream

class ELM327Protocol(
    private val inputStream: InputStream,
    private val outputStream: OutputStream
) {

    companion object {
        private const val CMD_TIMEOUT_MS = 2000L
        private const val MAX_RETRIES = 3
        private const val PROMPT = '>'
    }

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        sendCommand("ATZ")
        delay(1000)
        sendCommand("ATE0") ?: return@withContext false
        sendCommand("ATL0") ?: return@withContext false
        sendCommand("ATS0") ?: return@withContext false
        sendCommand("ATH0") ?: return@withContext false
        sendCommand("ATSP 3") ?: return@withContext false
        val response = sendCommandWithRetry("0100") ?: return@withContext false
        !response.contains("NO DATA") && !response.contains("ERROR")
    }

    suspend fun sendCommandWithRetry(command: String): String? {
        repeat(MAX_RETRIES) { attempt ->
            val response = sendCommand(command)
            if (response != null && !response.contains("NO DATA") && !response.contains("ERROR")) {
                return response
            }
            if (attempt < MAX_RETRIES - 1) delay(200)
        }
        return null
    }

    suspend fun sendCommand(command: String): String? = withContext(Dispatchers.IO) {
        try {
            outputStream.write("$command\r".toByteArray())
            outputStream.flush()
            readResponse()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun readResponse(): String? = withTimeoutOrNull(CMD_TIMEOUT_MS) {
        val buffer = StringBuilder()
        while (true) {
            val byte = inputStream.read()
            if (byte == -1) break
            val char = byte.toChar()
            if (char == PROMPT) break
            buffer.append(char)
        }
        buffer.toString().trim()
    }

    fun parseObdResponse(raw: String): List<Int>? {
        val clean = raw.replace("\\s".toRegex(), "")
        if (clean.contains("NODATA") || clean.contains("ERROR") || clean.length < 4) return null
        return try {
            clean.chunked(2).mapNotNull { it.toIntOrNull(16) }
        } catch (e: Exception) {
            null
        }
    }

    fun parseDtcResponse(raw: String): List<String> {
        val clean = raw.replace("\\s".toRegex(), "")
        if (clean.contains("NODATA") || clean.length < 4) return emptyList()
        val codes = mutableListOf<String>()
        var i = 4
        while (i + 3 < clean.length) {
            val high = clean.substring(i, i + 2).toIntOrNull(16) ?: break
            val low = clean.substring(i + 2, i + 4).toIntOrNull(16) ?: break
            if (high == 0 && low == 0) { i += 4; continue }
            val prefix = when ((high shr 6) and 0x03) {
                0 -> "P"
                1 -> "C"
                2 -> "B"
                3 -> "U"
                else -> "P"
            }
            val digit1 = (high shr 4) and 0x03
            val digit2 = high and 0x0F
            val digit34 = low
            codes.add("$prefix$digit1$digit2${digit34.toString(16).uppercase().padStart(2, '0')}")
            i += 4
        }
        return codes
    }
}
