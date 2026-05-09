package com.yourapp.obd.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.yourapp.obd.data.bluetooth.BluetoothOBDRepository
import com.yourapp.obd.data.bluetooth.BluetoothOBDRepositoryImpl
import com.yourapp.obd.data.gps.LocationRepository
import com.yourapp.obd.data.gps.LocationRepositoryImpl
import com.yourapp.obd.data.sensor.AccelerometerRepository
import com.yourapp.obd.data.sensor.AccelerometerRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBluetoothOBDRepository(
        impl: BluetoothOBDRepositoryImpl
    ): BluetoothOBDRepository

    @Binds
    @Singleton
    abstract fun bindAccelerometerRepository(
        impl: AccelerometerRepositoryImpl
    ): AccelerometerRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl
    ): LocationRepository
}
