package com.yourapp.obd.data.gps

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

interface LocationRepository {
    val lastLocation: StateFlow<Location?>
    fun locationFlow(): Flow<Location>
}

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationRepository {

    private val _lastLocation = MutableStateFlow<Location?>(null)
    override val lastLocation: StateFlow<Location?> = _lastLocation.asStateFlow()

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun locationFlow(): Flow<Location> = callbackFlow {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                _lastLocation.value = location
                trySend(location)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 5f, listener)
        awaitClose { manager.removeUpdates(listener) }
    }
}
