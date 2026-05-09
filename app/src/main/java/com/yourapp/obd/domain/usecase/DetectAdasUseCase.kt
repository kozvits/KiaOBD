package com.yourapp.obd.domain.usecase

import com.yourapp.obd.data.camera.CameraRepository
import com.yourapp.obd.domain.model.AdasAlert
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

class DetectAdasUseCase @Inject constructor(
    private val cameraRepository: CameraRepository
) {
    operator fun invoke(): SharedFlow<AdasAlert> = cameraRepository.adasAlerts
}
