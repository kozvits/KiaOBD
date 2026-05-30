package com.yourapp.obd.data.speedcam

import android.util.Log
import com.yourapp.obd.domain.model.SpeedCamUpdateStats
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpeedCamUpdateLogger(
    private val logDir: File
) {

    companion object {
        private const val TAG = "SpeedCamLogger"
        private const val MAX_LOG_ENTRIES = 1000
        private const val MAX_LOG_DAYS = 90

        private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        private val FILE_DATE_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    data class LogEntry(
        val timestamp: Long,
        val level: Level,
        val operation: String,
        val message: String
    ) {
        enum class Level { INFO, WARN, ERROR }
    }

    private val logFile: File
        get() = File(logDir, "speedcam_update_${FILE_DATE_FMT.format(Date())}.log")

    fun logInfo(operation: String, message: String) {
        log(LogEntry.Level.INFO, operation, message)
        Log.i(TAG, "[$operation] $message")
    }

    fun logWarn(operation: String, message: String) {
        log(LogEntry.Level.WARN, operation, message)
        Log.w(TAG, "[$operation] $message")
    }

    fun logError(operation: String, message: String, error: Throwable? = null) {
        log(LogEntry.Level.ERROR, operation, message + (error?.let { ": ${it.message}" } ?: ""))
        Log.e(TAG, "[$operation] $message", error)
    }

    fun logUpdateResult(stats: SpeedCamUpdateStats) {
        val lines = buildString {
            appendLine("═══════════════════════════════════════════════")
            appendLine("  Обновление базы камер")
            appendLine("  Время: ${DATE_FMT.format(Date(stats.timestamp))}")
            appendLine("  Результат: ${stats.summary}")
            appendLine("  Источников обработано: ${stats.sourcesProcessed}")
            appendLine("  Источников с ошибкой: ${stats.sourcesFailed}")
            appendLine("  Новых камер: ${stats.newCameras}")
            appendLine("  Удалено камер: ${stats.removedCameras}")
            appendLine("  Изменено камер: ${stats.modifiedCameras}")
            appendLine("  Всего активных: ${stats.totalActive}")
            appendLine("  Ошибка: ${stats.isError}")
            appendLine("═══════════════════════════════════════════════")
        }
        appendToFile(logFile, lines)
        Log.i(TAG, stats.summary)
    }

    fun logDiff(new: Int, removed: Int, modified: Int, details: String = "") {
        val msg = buildString {
            append("DIFF: +$new / -$removed / ~$modified")
            if (details.isNotBlank()) append(" — $details")
        }
        appendToFile(logFile, "[${DATE_FMT.format(Date())}] $msg")
        Log.i(TAG, msg)
    }

    fun logRollback(reason: String, success: Boolean) {
        val msg = if (success) "Откат выполнен успешно: $reason" else "Ошибка отката: $reason"
        appendToFile(logFile, "[${DATE_FMT.format(Date())}] [ROLLBACK] ${if (success) "OK" else "FAIL"} — $msg")
        Log.i(TAG, "[ROLLBACK] $msg")
    }

    private fun log(level: LogEntry.Level, operation: String, message: String) {
        appendToFile(logFile, "[${DATE_FMT.format(Date())}] [$level] [$operation] $message")
    }

    private fun appendToFile(file: File, text: String) {
        try {
            if (!logDir.exists()) logDir.mkdirs()
            FileWriter(file, true).use { it.appendLine(text) }
            cleanupOldLogs()
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось записать лог: ${e.message}")
        }
    }

    private fun cleanupOldLogs() {
        try {
            val cutoff = System.currentTimeMillis() - MAX_LOG_DAYS * 24 * 60 * 60 * 1000L
            logDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoff) file.delete()
            }
        } catch (_: Exception) { }
    }

    fun getRecentLogs(lines: Int = 50): List<String> {
        val file = logFile
        if (!file.exists()) return emptyList()
        return try {
            file.readLines().takeLast(lines)
        } catch (_: Exception) { emptyList() }
    }

    suspend fun logToDatabase(
        dao: SpeedCamDao,
        stats: SpeedCamUpdateStats,
        rollbackAvailable: Boolean,
        details: String = ""
    ) {
        try {
            dao.insertUpdateLog(
                SpeedCamUpdateLogEntity(
                    timestamp = stats.timestamp,
                    sourcesProcessed = stats.sourcesProcessed,
                    sourcesFailed = stats.sourcesFailed,
                    newCameras = stats.newCameras,
                    removedCameras = stats.removedCameras,
                    modifiedCameras = stats.modifiedCameras,
                    totalActive = stats.totalActive,
                    rollbackAvailable = rollbackAvailable,
                    summary = stats.summary,
                    details = details
                )
            )
            dao.deleteOldLogs(System.currentTimeMillis() - MAX_LOG_DAYS * 24 * 60 * 60 * 1000L)
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось записать лог в БД: ${e.message}")
        }
    }
}
