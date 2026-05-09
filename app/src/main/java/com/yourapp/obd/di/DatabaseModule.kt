package com.yourapp.obd.di

import android.content.Context
import androidx.room.Room
import com.yourapp.obd.data.db.AppDatabase
import com.yourapp.obd.data.db.DtcDao
import com.yourapp.obd.data.db.TripDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "kia_obd.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideDtcDao(db: AppDatabase): DtcDao = db.dtcDao()

    @Provides
    fun provideTripDao(db: AppDatabase): TripDao = db.tripDao()
}
