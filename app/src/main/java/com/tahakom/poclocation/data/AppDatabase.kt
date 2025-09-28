package com.tahakom.poclocation.data

import androidx.room.*

@Entity(tableName = "trip_points")
data class TripPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val lat: Double,
    val lng: Double,
    val timeMillis: Long,
    val accuracy: Float
)

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPoint(p: TripPointEntity)

    @Query("SELECT * FROM trip_points WHERE sessionId = :sessionId ORDER BY timeMillis ASC")
    suspend fun pointsForSession(sessionId: String): List<TripPointEntity>

    @Query("DELETE FROM trip_points WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)
}

@Database(entities = [TripPointEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}
