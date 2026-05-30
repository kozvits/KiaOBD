package com.yourapp.obd.data.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.yourapp.obd.domain.model.AdasAlert
import com.yourapp.obd.domain.model.AlertLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs

class AdasAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {

    companion object {
        private const val DEFAULT_LDW_MIN_SPEED = 30
        private const val DEFAULT_LDW_DEVIATION = 0.15f
        private const val DEFAULT_FCW_MIN_SPEED = 10
        private const val VEHICLE_MIN_AREA = 500
        private const val DISTANCE_K = 150f
        private const val FCW_COOLDOWN_MS = 400L
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _alerts = MutableSharedFlow<AdasAlert>(replay = 1, extraBufferCapacity = 16)
    val alerts: SharedFlow<AdasAlert> = _alerts.asSharedFlow()

    var currentSpeedKmh: Int = 0
    var adasEnabled = true
    var ldwEnabled = true
    var fcwEnabled = true
    var signDetectionEnabled = false
    var dmsEnabled = false
    var pedestrianEnabled = false

    // Чувствительность (LOW/MEDIUM/HIGH)
    var sensitivity: String = "MEDIUM"
        set(value) {
            field = value
            applySensitivity()
        }

    @Volatile
    var lastVehicleDistanceM: Float? = null
    private var lastFcwAlertTime = 0L
    private var opencvReady = false
    private var ldwMinSpeed = DEFAULT_LDW_MIN_SPEED
    private var ldwDeviationThreshold = DEFAULT_LDW_DEVIATION
    private var fcwMinSpeed = DEFAULT_FCW_MIN_SPEED

    init {
        try {
            opencvReady = OpenCVLoader.initLocal()
            if (!opencvReady) opencvReady = OpenCVLoader.initDebug()
        } catch (_: Exception) {
            opencvReady = false
        }
        applySensitivity()
    }

    private fun applySensitivity() {
        when (sensitivity) {
            "LOW" -> {
                ldwMinSpeed = 50
                ldwDeviationThreshold = 0.25f
                fcwMinSpeed = 30
            }
            "HIGH" -> {
                ldwMinSpeed = 15
                ldwDeviationThreshold = 0.10f
                fcwMinSpeed = 5
            }
            else -> { // MEDIUM
                ldwMinSpeed = DEFAULT_LDW_MIN_SPEED
                ldwDeviationThreshold = DEFAULT_LDW_DEVIATION
                fcwMinSpeed = DEFAULT_FCW_MIN_SPEED
            }
        }
    }

    override fun analyze(image: ImageProxy) {
        if (!adasEnabled || !opencvReady) { image.close(); return }
        val bitmap = image.toBitmap()
        val rotated = rotateBitmap(bitmap, image.imageInfo.rotationDegrees.toFloat())
        image.close()

        val mat = Mat()
        Utils.bitmapToMat(rotated, mat)

        scope.launch {
            try {
                if (ldwEnabled && currentSpeedKmh >= ldwMinSpeed) analyzeLdw(mat.clone())
                if (fcwEnabled && currentSpeedKmh >= fcwMinSpeed) analyzeFcw(mat.clone())
            } finally { mat.release() }
        }
    }

    private fun analyzeLdw(mat: Mat) {
        try {
            val h = mat.rows(); val w = mat.cols()
            val roiStart = (h * 0.4).toInt()
            val roi = mat.submat(roiStart, h, 0, w)
            val gray = Mat(); Imgproc.cvtColor(roi, gray, Imgproc.COLOR_RGB2GRAY)
            val blurred = Mat(); Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            val edges = Mat(); Imgproc.Canny(blurred, edges, 40.0, 120.0)
            val lines = Mat()
            Imgproc.HoughLinesP(edges, lines, 1.0, Math.PI / 180.0, 40, 40.0, 15.0)

            var leftSum = 0.0; var leftCnt = 0
            var rightSum = 0.0; var rightCnt = 0
            val cx = w / 2.0

            for (i in 0 until lines.rows()) {
                val d = lines.get(i, 0) ?: continue
                val x1 = d[0]; val y1 = d[1]; val x2 = d[2]; val y2 = d[3]
                val dx = x2 - x1; val dy = y2 - y1
                val slope = if (dx != 0.0) dy / dx else 0.0
                if (abs(slope) < 0.3) continue
                val mx = (x1 + x2) / 2.0
                if (mx < cx && slope < 0) { leftSum += mx; leftCnt++ }
                else if (mx > cx && slope > 0) { rightSum += mx; rightCnt++ }
            }

            val lx = if (leftCnt > 0) leftSum / leftCnt else cx * 0.25
            val rx = if (rightCnt > 0) rightSum / rightCnt else cx * 1.75
            val laneW = rx - lx
            val laneC = lx + laneW / 2.0
            val dev = abs(cx - laneC) / laneW

            if (dev > ldwDeviationThreshold && laneW > 0) {
                val dir = if (cx < laneC) "LEFT" else "RIGHT"
                scope.launch { _alerts.emit(AdasAlert.LaneDeparture(dir, dev.toFloat())) }
            }
            gray.release(); blurred.release(); edges.release(); lines.release(); roi.release()
        } catch (_: Exception) {}
        mat.release()
    }

    private fun analyzeFcw(mat: Mat) {
        try {
            val h = mat.rows(); val w = mat.cols()
            val horizonY = (h * 0.4).toInt()
            val roi = mat.submat(horizonY, h, 0, w)
            val gray = Mat(); Imgproc.cvtColor(roi, gray, Imgproc.COLOR_RGB2GRAY)

            // Shadow-based vehicle detection: dark horizontal regions near road
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(35.0, 8.0))
            val blackHat = Mat()
            Imgproc.morphologyEx(gray, blackHat, Imgproc.MORPH_BLACKHAT, kernel)
            val thresh = Mat()
            Imgproc.threshold(blackHat, thresh, 25.0, 255.0, Imgproc.THRESH_BINARY)

            val closeK = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(15.0, 5.0))
            Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, closeK)

            val contours = ArrayList<MatOfPoint>()
            val hier = Mat()
            Imgproc.findContours(thresh, contours, hier, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            val imgCx = w / 2
            var bestArea = 0.0; var bestBottom = 0

            for (c in contours) {
                val area = Imgproc.contourArea(c)
                if (area < VEHICLE_MIN_AREA) continue
                val rect = Imgproc.boundingRect(c)
                val ar = rect.width.toFloat() / rect.height.toFloat()
                if (ar < 0.5f || ar > 5.0f) continue
                val cx = rect.x + rect.width / 2
                if (abs(cx - imgCx) > w * 0.35) continue
                if (area > bestArea) { bestArea = area; bestBottom = rect.y + rect.height }
            }

            kernel.release(); blackHat.release(); thresh.release()
            closeK.release(); hier.release(); gray.release(); roi.release()

            if (bestArea > 0) {
                val d = (bestBottom).coerceAtLeast(1)
                val dist = DISTANCE_K * h / d

                // Smooth using exponential moving average
                val smoothed = if (lastVehicleDistanceM != null) {
                    lastVehicleDistanceM!! * 0.7f + dist * 0.3f
                } else dist
                lastVehicleDistanceM = smoothed

                val now = System.currentTimeMillis()
                if (now - lastFcwAlertTime > FCW_COOLDOWN_MS) {
                    val relSpeed = currentSpeedKmh / 3.6f
                    val ttc = if (relSpeed > 0.1f) smoothed / relSpeed else 30f
                    val level = when {
                        smoothed < 5f || ttc < 1.0f -> AlertLevel.DANGER
                        smoothed < 12f || ttc < 2.0f -> AlertLevel.WARNING
                        smoothed < 28f -> AlertLevel.CAUTION
                        else -> null
                    }
                    if (level != null) {
                        lastFcwAlertTime = now
                        scope.launch { _alerts.emit(AdasAlert.ForwardCollision(level, ttc.coerceAtMost(15f))) }
                    }
                }
            } else {
                lastVehicleDistanceM = null
            }
        } catch (_: Exception) {}
        mat.release()
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
