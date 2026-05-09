package com.yourapp.obd.domain.usecase

import com.yourapp.obd.data.camera.CameraRepository
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject

class RecordVideoUseCase @Inject constructor(
    private val cameraRepository: CameraRepository
) {
    val isRecording: StateFlow<Boolean> = cameraRepository.isRecording

    fun start() = cameraRepository.startRecording()
    fun stop() = cameraRepository.stopRecording()
    fun protect() = cameraRepository.markCurrentAsProtected()
    fun getAllSegments(): List<File> = cameraRepository.getAllSegments()
    fun delete(file: File) = cameraRepository.deleteSegment(file)
}
