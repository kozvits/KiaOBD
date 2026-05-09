package com.yourapp.obd.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "dtc_codes")
data class DtcEntity(
    @PrimaryKey val code: String,
    val description: String,
    val severity: String,
    val timestamp: Long
)

@Dao
interface DtcDao {
    @Query("SELECT * FROM dtc_codes ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<DtcEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(codes: List<DtcEntity>)

    @Query("DELETE FROM dtc_codes")
    suspend fun deleteAll()
}
