package com.yourapp.obd.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.obd.data.db.TripDao
import com.yourapp.obd.data.db.TripEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TripStats(
    val totalTrips: Int = 0,
    val totalDistanceKm: Float = 0f,
    val totalTimeMinutes: Long = 0,
    val avgSpeedKmh: Float = 0f,
    val maxSpeedKmh: Int = 0
)

@HiltViewModel
class TripHistoryViewModel @Inject constructor(
    private val tripDao: TripDao
) : ViewModel() {

    private val _trips = MutableStateFlow<List<TripEntity>>(emptyList())
    val trips: StateFlow<List<TripEntity>> = _trips.asStateFlow()

    private val _stats = MutableStateFlow(TripStats())
    val stats: StateFlow<TripStats> = _stats.asStateFlow()

    init {
        viewModelScope.launch {
            tripDao.getAllFlow().collect { tripList ->
                _trips.value = tripList
                computeStats(tripList)
            }
        }
    }

    private fun computeStats(tripList: List<TripEntity>) {
        if (tripList.isEmpty()) {
            _stats.value = TripStats()
            return
        }
        val totalDistance = tripList.sumOf { it.distanceKm.toDouble() }.toFloat()
        val totalTime = tripList.sumOf { it.endTimestamp - it.startTimestamp } / 1000L / 60L
        val maxSpeed = tripList.maxOf { it.maxSpeedKmh }
        val avgSpeed = if (totalTime > 0) {
            totalDistance / (totalTime.toFloat() / 60f)
        } else 0f

        _stats.value = TripStats(
            totalTrips = tripList.size,
            totalDistanceKm = totalDistance,
            totalTimeMinutes = totalTime,
            avgSpeedKmh = avgSpeed,
            maxSpeedKmh = maxSpeed
        )
    }

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch { tripDao.deleteById(tripId) }
    }
}

private fun Float.roundToInt(): Int = (this + 0.5f).toInt()
