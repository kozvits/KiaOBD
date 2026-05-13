package com.yourapp.obd.data.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.yourapp.obd.domain.model.AdasAlert
import com.yourapp.obd.domain.model.AlertLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.math.sqrt

class AdasAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {

    companion object {
        private const val YOLO_INPUT_SIZE = 320
        private const val SIGN_INPUT_SIZE = 224
        private const val SSD_INPUT_SIZE = 300
        private const val FCW_TTC_THRESHOLD = 2.5f
        private const val LDW_DEVIATION_THRESHOLD = 0.15f
        private const val LDW_MIN_SPEED = 30
        private const val FCW_MIN_SPEED = 10
        private const val PEDESTRIAN_MIN_SPEED = 5
        private const val PEDESTRIAN_CONFIDENCE = 0.6f
        private const val EAR_THRESHOLD = 0.25f
        private const val PERCLOS_THRESHOLD = 0.35f
        private const val PERCLOS_WINDOW_FRAMES = 45
        private const val YAW_THRESHOLD = 30f
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _alerts = MutableSharedFlow<AdasAlert>(replay = 1, extraBufferCapacity = 16)
    val alerts: SharedFlow<AdasAlert> = _alerts.asSharedFlow()

    private var yoloInterpreter: Interpreter? = null
    private var signInterpreter: Interpreter? = null
    private var ssdInterpreter: Interpreter? = null
    private var faceLandmarker: FaceLandmarker? = null

    var currentSpeedKmh: Int = 0
    var adasEnabled = true
    var ldwEnabled = true
    var fcwEnabled = true
    var signDetectionEnabled = true
    var dmsEnabled = true
    var pedestrianEnabled = true

    private var prevBoxArea: Float = 0f
    private val earHistory = ArrayDeque<Boolean>(PERCLOS_WINDOW_FRAMES)
    private var lastDetectedSign: Int? = null

    init {
        // Удалено: loadModels() перенесено в ленивую загрузку
    }

    private fun loadModels() {
        try {
            yoloInterpreter = Interpreter(loadModelFile("yolov8n.tflite"))
        } catch (e: Exception) {
            android.util.Log.e("AdasAnalyzer", "Error loading YOLO: ${e.message}")
        }
        try {
            signInterpreter = Interpreter(loadModelFile("mobilenetv3_signs.tflite"))
        } catch (e: Exception) {
            android.util.Log.e("AdasAnalyzer", "Error loading Signs: ${e.message}")
        }
        try {
            ssdInterpreter = Interpreter(loadModelFile("mobilenet_ssd.tflite"))
        } catch (e: Exception) {
            android.util.Log.e("AdasAnalyzer", "Error loading SSD: ${e.message}")
        }
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build()
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumFaces(1)
                .build()
            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            android.util.Log.e("AdasAnalyzer", "Error loading Face: ${e.message}")
        }
    }

    private fun loadModelFile(filename: String): MappedByteBuffer {
        val fd = context.assets.openFd(filename)
        val inputStream = FileInputStream(fd.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    override fun analyze(image: ImageProxy) {
        if (!adasEnabled) {
            image.close()
            return
        }
        
        // Ленивая загрузка моделей в фоновом потоке, если они еще не готовы
        if (yoloInterpreter == null) {
            scope.launch(Dispatchers.IO) {
                loadModels()
            }
            image.close()
            return
        }

        val bitmap = image.toBitmap()
        val rotated = rotateBitmap(bitmap, image.imageInfo.rotationDegrees.toFloat())
        image.close()
        scope.launch {
            if (ldwEnabled) analyzeLdw(rotated)
            if (fcwEnabled) analyzeFcw(rotated)
            if (signDetectionEnabled) analyzeSpeedSign(rotated)
            if (dmsEnabled) analyzeDms(rotated)
            if (pedestrianEnabled) analyzePedestrians(rotated)
        }
    }

    private fun analyzeLdw(bitmap: Bitmap) {
        if (currentSpeedKmh < LDW_MIN_SPEED) return
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        val roiStart = (gray.rows() * 0.4).toInt()
        val roi = gray.submat(roiStart, gray.rows(), 0, gray.cols())
        val blurred = Mat()
        Imgproc.GaussianBlur(roi, blurred, Size(5.0, 5.0), 0.0)
        val edges = Mat()
        Imgproc.Canny(blurred, edges, 50.0, 150.0)
        val lines = Mat()
        Imgproc.HoughLinesP(edges, lines, 1.0, Math.PI / 180.0, 50, 50.0, 20.0)

        var leftX = 0.0
        var rightX = bitmap.width.toDouble()
        var leftCount = 0
        var rightCount = 0
        val centerX = bitmap.width / 2.0

        for (i in 0 until lines.rows()) {
            val line = lines.get(i, 0) ?: continue
            val x1 = line[0]; val y1 = line[1]; val x2 = line[2]; val y2 = line[3]
            val slope = if (x2 - x1 != 0.0) (y2 - y1) / (x2 - x1) else 0.0
            if (abs(slope) < 0.3) continue
            val midX = (x1 + x2) / 2.0
            if (midX < centerX && slope < 0) { leftX += midX; leftCount++ }
            else if (midX > centerX && slope > 0) { rightX += midX; rightCount++ }
        }
        if (leftCount > 0) leftX /= leftCount
        if (rightCount > 0) rightX /= rightCount

        val laneWidth = rightX - leftX
        val vehicleCenter = bitmap.width / 2.0
        val laneCenter = leftX + laneWidth / 2.0
        val deviation = abs(vehicleCenter - laneCenter) / laneWidth

        if (deviation > LDW_DEVIATION_THRESHOLD) {
            val direction = if (vehicleCenter < laneCenter) "LEFT" else "RIGHT"
            scope.launch { _alerts.emit(AdasAlert.LaneDeparture(direction, deviation.toFloat())) }
        }
        mat.release(); gray.release(); roi.release()
        blurred.release(); edges.release(); lines.release()
    }

    private fun analyzeFcw(bitmap: Bitmap) {
        if (currentSpeedKmh < FCW_MIN_SPEED) return
        val interpreter = yoloInterpreter ?: return
        val resized = Bitmap.createScaledBitmap(bitmap, YOLO_INPUT_SIZE, YOLO_INPUT_SIZE, true)
        val input = bitmapToFloatBuffer(resized, YOLO_INPUT_SIZE)
        val outputShape = interpreter.getOutputTensor(0).shape()
        val output = Array(1) { Array(outputShape[1]) { FloatArray(outputShape[2]) } }
        interpreter.run(input, output)

        var maxArea = 0f
        for (i in 0 until outputShape[1]) {
            if (output[0][i][4] < 0.5f) continue
            val area = output[0][i][2] * output[0][i][3]
            if (area > maxArea) maxArea = area
        }
        if (maxArea > 0f && prevBoxArea > 0f) {
            val growthRate = (maxArea - prevBoxArea) / prevBoxArea
            if (growthRate > 0.05f) {
                val estimatedTtc = 1f / (growthRate * 10f)
                val level = when {
                    estimatedTtc < 1.0f -> AlertLevel.DANGER
                    estimatedTtc < 1.8f -> AlertLevel.WARNING
                    estimatedTtc < FCW_TTC_THRESHOLD -> AlertLevel.CAUTION
                    else -> null
                }
                if (level != null) {
                    scope.launch { _alerts.emit(AdasAlert.ForwardCollision(level, estimatedTtc)) }
                }
            }
        }
        prevBoxArea = maxArea
    }

    private fun analyzeSpeedSign(bitmap: Bitmap) {
        val interpreter = signInterpreter ?: return
        val resized = Bitmap.createScaledBitmap(bitmap, SIGN_INPUT_SIZE, SIGN_INPUT_SIZE, true)
        val input = bitmapToFloatBuffer(resized, SIGN_INPUT_SIZE)
        val output = Array(1) { FloatArray(12) }
        interpreter.run(input, output)
        val probs = output[0]
        val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: return
        if (probs[maxIdx] < 0.7f || maxIdx == 11) return
        val speeds = intArrayOf(20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120)
        val detectedSpeed = speeds.getOrNull(maxIdx) ?: return
        lastDetectedSign = detectedSpeed
        if (currentSpeedKmh > detectedSpeed + 10) {
            scope.launch { _alerts.emit(AdasAlert.SpeedLimitExceeded(detectedSpeed, currentSpeedKmh)) }
        }
    }

    private fun analyzeDms(bitmap: Bitmap) {
        val landmarker = faceLandmarker ?: return
        try {
            val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(bitmap).build()
            val result = landmarker.detect(mpImage) ?: return
            if (result.faceLandmarks().isEmpty()) return
            val landmarks = result.faceLandmarks()[0]
            if (landmarks.size < 387) return

            fun dist(
                a: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
                b: com.google.mediapipe.tasks.components.containers.NormalizedLandmark
            ): Float {
                val dx = a.x() - b.x(); val dy = a.y() - b.y()
                return sqrt(dx * dx + dy * dy)
            }

            val leftEar = (dist(landmarks[159], landmarks[145]) + dist(landmarks[158], landmarks[153])) /
                    (2f * dist(landmarks[133], landmarks[33]))
            val rightEar = (dist(landmarks[386], landmarks[374]) + dist(landmarks[385], landmarks[380])) /
                    (2f * dist(landmarks[362], landmarks[263]))
            val ear = (leftEar + rightEar) / 2f

            if (earHistory.size >= PERCLOS_WINDOW_FRAMES) earHistory.removeFirst()
            earHistory.addLast(ear < EAR_THRESHOLD)

            val perclos = earHistory.count { it }.toFloat() / earHistory.size
            if (perclos > PERCLOS_THRESHOLD) {
                scope.launch { _alerts.emit(AdasAlert.DriverFatigue(perclos)) }
            }

            val leftEye = landmarks[33]
            val rightEye = landmarks[263]
            val yaw = Math.toDegrees(
                kotlin.math.atan2(
                    (rightEye.x() - leftEye.x()).toDouble(),
                    (rightEye.z() - leftEye.z()).toDouble()
                )
            ).toFloat()
            if (abs(yaw) > YAW_THRESHOLD) {
                scope.launch { _alerts.emit(AdasAlert.DriverDistracted(abs(yaw))) }
            }
        } catch (_: Exception) {}
    }

    private fun analyzePedestrians(bitmap: Bitmap) {
        if (currentSpeedKmh < PEDESTRIAN_MIN_SPEED) return
        val interpreter = ssdInterpreter ?: return
        val height = bitmap.height
        val riskZoneStart = (height * 0.6f).toInt()
        val croppedBitmap = Bitmap.createBitmap(bitmap, 0, riskZoneStart, bitmap.width, height - riskZoneStart)
        val resized = Bitmap.createScaledBitmap(croppedBitmap, SSD_INPUT_SIZE, SSD_INPUT_SIZE, true)

        val inputBuffer = ByteBuffer.allocateDirect(SSD_INPUT_SIZE * SSD_INPUT_SIZE * 3)
        inputBuffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(SSD_INPUT_SIZE * SSD_INPUT_SIZE)
        resized.getPixels(pixels, 0, SSD_INPUT_SIZE, 0, 0, SSD_INPUT_SIZE, SSD_INPUT_SIZE)
        for (px in pixels) {
            inputBuffer.put(((px shr 16) and 0xFF).toByte())
            inputBuffer.put(((px shr 8) and 0xFF).toByte())
            inputBuffer.put((px and 0xFF).toByte())
        }
        inputBuffer.rewind()

        val outputLocations = Array(1) { Array(10) { FloatArray(4) } }
        val outputClasses = Array(1) { FloatArray(10) }
        val outputScores = Array(1) { FloatArray(10) }
        val numDetections = FloatArray(1)
        val outputs = mapOf(0 to outputLocations, 1 to outputClasses, 2 to outputScores, 3 to numDetections)
        interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)

        val count = numDetections[0].toInt()
        for (i in 0 until count) {
            if (outputClasses[0][i].toInt() == 0 && outputScores[0][i] > PEDESTRIAN_CONFIDENCE) {
                scope.launch { _alerts.emit(AdasAlert.PedestrianDetected(outputScores[0][i])) }
                break
            }
        }
    }

    private fun bitmapToFloatBuffer(bitmap: Bitmap, size: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * size * size * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        for (px in pixels) {
            buffer.putFloat(((px shr 16) and 0xFF) / 255f)
            buffer.putFloat(((px shr 8) and 0xFF) / 255f)
            buffer.putFloat((px and 0xFF) / 255f)
        }
        buffer.rewind()
        return buffer
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun getLastSign(): Int? = lastDetectedSign
}
