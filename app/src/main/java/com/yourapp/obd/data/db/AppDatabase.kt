package com.yourapp.obd.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DtcEntity::class, TripEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dtcDao(): DtcDao
    abstract fun tripDao(): TripDao
}
