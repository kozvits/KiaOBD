package com.yourapp.obd.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yourapp.obd.data.db.AppDatabase
import com.yourapp.obd.data.db.DtcDao
import com.yourapp.obd.data.db.TripDao
import com.yourapp.obd.data.speedcam.SpeedCamDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_TO_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `speed_cameras` (
                    `id` TEXT NOT NULL,
                    `latitude` REAL NOT NULL,
                    `longitude` REAL NOT NULL,
                    `type` TEXT NOT NULL DEFAULT 'UNKNOWN',
                    `speedLimitKmh` INTEGER,
                    `direction` TEXT,
                    `road` TEXT,
                    `isActive` INTEGER NOT NULL DEFAULT 1,
                    `installedAt` INTEGER,
                    `updatedAt` INTEGER NOT NULL,
                    `hash` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_speed_cameras_lat_lng` ON `speed_cameras` (`latitude`, `longitude`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_speed_cameras_type` ON `speed_cameras` (`type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_speed_cameras_updated` ON `speed_cameras` (`updatedAt`)")
        }
    }

    private val MIGRATION_2_TO_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `speed_cameras_snapshot` (
                    `id` TEXT NOT NULL,
                    `latitude` REAL NOT NULL,
                    `longitude` REAL NOT NULL,
                    `type` TEXT NOT NULL DEFAULT 'UNKNOWN',
                    `speedLimitKmh` INTEGER,
                    `direction` TEXT,
                    `road` TEXT,
                    `isActive` INTEGER NOT NULL DEFAULT 1,
                    `installedAt` INTEGER,
                    `updatedAt` INTEGER NOT NULL,
                    `hash` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_snapshot_lat_lng` ON `speed_cameras_snapshot` (`latitude`, `longitude`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_snapshot_updated` ON `speed_cameras_snapshot` (`updatedAt`)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `speedcam_update_log` (
                    `timestamp` INTEGER NOT NULL,
                    `sourcesProcessed` INTEGER NOT NULL DEFAULT 0,
                    `sourcesFailed` INTEGER NOT NULL DEFAULT 0,
                    `newCameras` INTEGER NOT NULL DEFAULT 0,
                    `removedCameras` INTEGER NOT NULL DEFAULT 0,
                    `modifiedCameras` INTEGER NOT NULL DEFAULT 0,
                    `totalActive` INTEGER NOT NULL DEFAULT 0,
                    `rollbackAvailable` INTEGER NOT NULL DEFAULT 0,
                    `summary` TEXT NOT NULL DEFAULT '',
                    `details` TEXT NOT NULL DEFAULT '',
                    PRIMARY KEY(`timestamp`)
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_update_log_ts` ON `speedcam_update_log` (`timestamp`)")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "kia_obd.db"
        )
        .addMigrations(MIGRATION_1_TO_2, MIGRATION_2_TO_3)
        .build()
    }

    @Provides
    fun provideDtcDao(db: AppDatabase): DtcDao = db.dtcDao()

    @Provides
    fun provideTripDao(db: AppDatabase): TripDao = db.tripDao()

    @Provides
    fun provideSpeedCamDao(db: AppDatabase): SpeedCamDao = db.speedCamDao()
}
