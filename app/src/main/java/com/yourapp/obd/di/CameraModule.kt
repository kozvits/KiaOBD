package com.yourapp.obd.di

import android.content.Context
import com.yourapp.obd.data.camera.AdasAnalyzer
import com.yourapp.obd.data.camera.CameraRepository
import com.yourapp.obd.data.camera.CameraRepositoryImpl
import com.yourapp.obd.data.camera.VideoRecorder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    fun provideAdasAnalyzer(@ApplicationContext context: Context): AdasAnalyzer {
        return AdasAnalyzer(context)
    }

    @Provides
    fun provideVideoRecorder(@ApplicationContext context: Context): VideoRecorder {
        return VideoRecorder(context)
    }

    @Provides
    fun provideCameraRepository(
        @ApplicationContext context: Context,
        adasAnalyzer: AdasAnalyzer,
        videoRecorder: VideoRecorder
    ): CameraRepository {
        return CameraRepositoryImpl(context, adasAnalyzer, videoRecorder)
    }
}