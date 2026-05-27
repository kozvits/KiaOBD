package com.yourapp.obd.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

interface AccelerometerRepository {
    fun impactEvents(): Flow<Float>
}

@Singleton
class AccelerometerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AccelerometerRepository {

    companion object {
        private const val IMPACT_THRESHOLD_G = 2.0f
        private const val G_FORCE = 9.81f
    }

    override fun impactEvents(): Flow<Float> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (sensor == null) {
            close() // No sensor available, close the flow
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z) / G_FORCE
                if (magnitude > IMPACT_THRESHOLD_G) trySend(magnitude)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
