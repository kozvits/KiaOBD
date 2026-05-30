package com.yourapp.obd.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yourapp.obd.data.speedcam.SpeedCamDao
import com.yourapp.obd.data.speedcam.SpeedCamEntity
import com.yourapp.obd.data.speedcam.SpeedCamUpdateLogEntity
import com.yourapp.obd.data.speedcam.SpeedCamSnapshotEntity

@Database(
    entities = [
        DtcEntity::class,
        TripEntity::class,
        SpeedCamEntity::class,
        SpeedCamUpdateLogEntity::class,
        SpeedCamSnapshotEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dtcDao(): DtcDao
    abstract fun tripDao(): TripDao
    abstract fun speedCamDao(): SpeedCamDao
}
