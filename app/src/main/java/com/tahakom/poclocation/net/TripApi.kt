package com.tahakom.poclocation.net

import retrofit2.http.Body
import retrofit2.http.POST

data class TripPayload(
    val tripId: String,
    val encodedPolyline: String,
    val distanceMeters: Long,
    val durationSeconds: Long,
    val startedAt: Long,
    val endedAt: Long,
    val averageAccuracyMeters: Double,
    val rawPointsCount: Int
)

interface TripApi {
    @POST("trips")
    suspend fun uploadTrip(@Body payload: TripPayload)
}
