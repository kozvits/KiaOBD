package com.yourapp.obd.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val distanceKm: Float,
    val maxSpeedKmh: Int,
    val avgSpeedKmh: Int,
    val videoPath: String
)

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startTimestamp DESC")
    fun getAllFlow(): Flow<List<TripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: TripEntity): Long

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Long)
}
