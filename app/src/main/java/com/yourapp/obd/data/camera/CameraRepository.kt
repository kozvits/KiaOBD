package com.yourapp.obd.data.camera

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.yourapp.obd.domain.model.AdasAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface CameraRepository {
    val isRecording: StateFlow<Boolean>
    val adasAlerts: SharedFlow<AdasAlert>
    fun bindCamera(owner: LifecycleOwner, previewView: PreviewView)
    fun startRecording()
    fun stopRecording()
    fun markCurrentAsProtected()
    fun getAllSegments(): List<File>
    fun deleteSegment(file: File)
    fun updateObdSpeed(speedKmh: Int)
    fun setOutputDirectory(dir: File)
    fun setMaxBufferBytes(bytes: Long)
    fun setSegmentDurationMs(ms: Long)
}

class CameraRepositoryImpl(
    @ApplicationContext private val context: Context,
    private val adasAnalyzer: AdasAnalyzer,
    private val videoRecorder: VideoRecorder
) : CameraRepository {

    override val isRecording: StateFlow<Boolean> = videoRecorder.isRecording
    override val adasAlerts: SharedFlow<AdasAlert> = adasAnalyzer.alerts

    override fun bindCamera(owner: LifecycleOwner, previewView: PreviewView) {
        videoRecorder.bindCamera(owner, previewView, adasAnalyzer)
    }

    override fun startRecording() = videoRecorder.startRecording()
    override fun stopRecording() = videoRecorder.stopRecording()
    override fun markCurrentAsProtected() = videoRecorder.markCurrentSegmentAsProtected()
    override fun getAllSegments(): List<File> = videoRecorder.getAllSegments()
    override fun deleteSegment(file: File) = videoRecorder.deleteSegment(file)
    override fun updateObdSpeed(speedKmh: Int) { adasAnalyzer.currentSpeedKmh = speedKmh }
    override fun setOutputDirectory(dir: File) = videoRecorder.setOutputDirectory(dir)
    override fun setMaxBufferBytes(bytes: Long) = videoRecorder.setMaxBufferBytes(bytes)
    override fun setSegmentDurationMs(ms: Long) = videoRecorder.setSegmentDurationMs(ms)
}
