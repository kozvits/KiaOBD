package com.yourapp.obd.data.speedcam

import com.yourapp.obd.domain.model.SpeedCam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

interface SpeedCamSource {
    suspend fun fetch(): Result<List<SpeedCam>>
    val name: String
    val url: String
}

class HttpSpeedCamSource(
    override val url: String,
    override val name: String = url
) : SpeedCamSource {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 20_000L
        private const val READ_TIMEOUT_MS = 90_000L
        private const val MAX_REDIRECTS = 5
        private const val MAX_RESPONSE_BYTES = 10 * 1024 * 1024L
    }

    override suspend fun fetch(): Result<List<SpeedCam>> = withContext(Dispatchers.IO) {
        try {
            val responseBytes = downloadWithRedirects(url, 0)
            val body = responseBytes.toString(Charsets.UTF_8)
            if (body.contains("\"remark\"")) {
                val remark = SpeedCamParser.extractOverpassRemark(body)
                return@withContext Result.failure(
                    SpeedCamException("Источник $name: $remark")
                )
            }

            val result = SpeedCamParser.parseAuto(body)

            if (result.cameras.isEmpty() && body.isNotBlank() && !SpeedCamParser.isValidEmptyOsmResponse(body)) {
                return@withContext Result.failure(
                    SpeedCamException("Источник $name: получено 0 камер после парсинга")
                )
            }

            Result.success(result.cameras)
        } catch (e: Exception) {
            Result.failure(SpeedCamException("Источник $name: ${e.message}", e))
        }
    }

    @Throws(Exception::class)
    private fun downloadWithRedirects(urlString: String, redirectCount: Int): ByteArray {
        if (redirectCount > MAX_REDIRECTS) {
            throw SpeedCamException("Слишком много перенаправлений")
        }

        val conn = URL(urlString).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = CONNECT_TIMEOUT_MS.toInt()
            conn.readTimeout = READ_TIMEOUT_MS.toInt()
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("User-Agent", "KiaOBD/1.0")
            conn.setRequestProperty("Accept", "application/json, text/csv, */*")

            val responseCode = conn.responseCode
            when {
                responseCode in 300..399 -> {
                    val newUrl = conn.getHeaderField("Location") ?: throw SpeedCamException("Перенаправление без Location")
                    conn.disconnect()
                    downloadWithRedirects(newUrl, redirectCount + 1)
                }
                responseCode == 200 -> {
                    val stream = conn.inputStream
                    stream.use { it.readBytes().let { bytes ->
                        if (bytes.size > MAX_RESPONSE_BYTES) throw SpeedCamException("Ответ превышает лимит 10MB")
                        bytes
                    }}
                }
                else -> throw SpeedCamException("HTTP $responseCode: ${conn.responseMessage}")
            }
        } finally {
            conn.disconnect()
        }
    }
}

class SpeedCamException(message: String, cause: Throwable? = null) : Exception(message, cause)
