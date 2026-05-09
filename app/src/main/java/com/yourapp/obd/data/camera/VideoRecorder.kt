package com.yourapp.obd.data.camera

import android.content.Context
import android.net.Uri
import android.os.StatFs
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class VideoRecorder(private val context: Context) {

    companion object {
        private const val SEGMENT_DURATION_MS = 5 * 60 * 1000L
        private const val MAX_STORAGE_FRACTION = 0.90
        private const val DEFAULT_MAX_BYTES = 4L * 1024 * 1024 * 1024
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var activeRecording: Recording? = null
    private var segmentJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentSegmentPath = MutableStateFlow<String?>(null)
    val currentSegmentPath: StateFlow<String?> = _currentSegmentPath.asStateFlow()

    private var outputDirectory: File = context.getExternalFilesDir("dashcam") ?: context.filesDir
    private var maxBufferBytes: Long = DEFAULT_MAX_BYTES
    private var videoCapture: VideoCapture<Recorder>? = null
    private val executor = Executors.newSingleThreadExecutor()

    fun setOutputDirectory(dir: File) { outputDirectory = dir; outputDirectory.mkdirs() }
    fun setMaxBufferBytes(bytes: Long) { maxBufferBytes = bytes }

    fun bindCamera(
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val qualitySelector = QualitySelector.fromOrderedList(
                listOf(Quality.FHD, Quality.HD)
            )
            val recorder = Recorder.Builder().setQualitySelector(qualitySelector).build()
            val vc = VideoCapture.withOutput(recorder)
            videoCapture = vc

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor, analyzer) }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview, vc, imageAnalysis
                )
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    fun startRecording() {
        segmentJob?.cancel()
        segmentJob = scope.launch {
            while (true) {
                startSegment()
                delay(SEGMENT_DURATION_MS)
                stopCurrentSegment()
                cleanupOldSegments()
            }
        }
        _isRecording.value = true
    }

    private fun startSegment() {
        val vc = videoCapture ?: return
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(outputDirectory, "dashcam_$timestamp.mp4")
        _currentSegmentPath.value = file.absolutePath
        val outputOptions = FileOutputOptions.Builder(file).build()
        activeRecording = vc.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    if (event.hasError()) {
                        file.delete()
                    }
                }
            }
    }

    private fun stopCurrentSegment() {
        activeRecording?.stop()
        activeRecording = null
    }

    fun stopRecording() {
        segmentJob?.cancel()
        stopCurrentSegment()
        _isRecording.value = false
    }

    fun markCurrentSegmentAsProtected() {
        val path = _currentSegmentPath.value ?: return
        val file = File(path)
        if (file.exists()) {
            val protected = File(outputDirectory, "protected_${file.name}")
            file.renameTo(protected)
        }
    }

    private fun cleanupOldSegments() {
        val used = outputDirectory.walk()
            .filter { it.isFile && it.name.startsWith("dashcam_") }
            .sumOf { it.length() }
        if (used <= maxBufferBytes) return
        val toDelete = outputDirectory.walk()
            .filter { it.isFile && it.name.startsWith("dashcam_") }
            .sortedBy { it.lastModified() }
            .toMutableList()
        var remaining = used
        for (file in toDelete) {
            if (remaining <= maxBufferBytes * MAX_STORAGE_FRACTION) break
            remaining -= file.length()
            file.delete()
        }
    }

    fun getAllSegments(): List<File> {
        return outputDirectory.listFiles()
            ?.filter { it.isFile && it.extension == "mp4" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun deleteSegment(file: File) {
        if (!file.name.startsWith("protected_")) {
            file.delete()
        }
    }
}
