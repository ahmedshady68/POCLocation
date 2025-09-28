package com.tahakom.poclocation.repo

import com.tahakom.poclocation.data.AppDatabase
import com.tahakom.poclocation.data.TripPointEntity
import com.tahakom.poclocation.net.TripApi
import com.tahakom.poclocation.net.TripPayload
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToLong

class TripRepository @Inject constructor(
    private val db: AppDatabase,
    private val api: TripApi
) {
    suspend fun appendPoint(sessionId: String, loc: Location) = withContext(Dispatchers.IO) {
        if (loc.accuracy <= 50f) db.tripDao().insertPoint(
            TripPointEntity(
                sessionId = sessionId,
                lat = loc.latitude,
                lng = loc.longitude,
                timeMillis = loc.time,
                accuracy = loc.accuracy
            )
        )
    }

    suspend fun finishAndUpload(sessionId: String, startedAt: Long, endedAt: Long) =
        withContext(Dispatchers.IO) {
            val pts = db.tripDao().pointsForSession(sessionId)
            if (pts.size < 2) return@withContext

            val raw = pts.map { LatLng(it.lat, it.lng) }
            val simplified = PolyUtil.simplify(raw, 10.0)
            val encoded = PolyUtil.encode(simplified)

            val meters = simplified.zipWithNext().sumOf { (a, b) ->
                val out = FloatArray(1)
                Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, out)
                out[0].toDouble()
            }.roundToLong()

            val payload = TripPayload(
                tripId = sessionId,
                encodedPolyline = encoded,
                distanceMeters = meters,
                durationSeconds = ((endedAt - startedAt) / 1000.0).roundToLong(),
                startedAt = startedAt,
                endedAt = endedAt,
                averageAccuracyMeters = pts.map { it.accuracy.toDouble() }.average(),
                rawPointsCount = pts.size
            )

            runCatching { api.uploadTrip(payload) }
            db.tripDao().deleteSession(sessionId)
        }

    fun newSessionId(): String = UUID.randomUUID().toString()
}
