package com.tahakom.poclocation.trip

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.tahakom.poclocation.MainActivity
import com.tahakom.poclocation.R
import com.tahakom.poclocation.repo.TripRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TripService : Service() {

    @Inject lateinit var repo: TripRepository
    @Inject lateinit var fused: FusedLocationProviderClient

    private val scope = CoroutineScope(Dispatchers.IO)
    private var sessionId: String = ""
    private var startedAt: Long = 0L
    private var destLat: Double = Double.NaN
    private var destLng: Double = Double.NaN

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRecording(true)
            stopSelf()
            return START_NOT_STICKY
        }
        sessionId = intent?.getStringExtra(EXTRA_SESSION_ID) ?: sessionId.takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString()
        destLat = intent?.getDoubleExtra(EXTRA_DEST_LAT, Double.NaN) ?: destLat
        destLng = intent?.getDoubleExtra(EXTRA_DEST_LNG, Double.NaN) ?: destLng
        if (startedAt == 0L) startedAt = System.currentTimeMillis()

        startInForeground()
        startLocationUpdates()
        return START_STICKY
    }

    private fun startInForeground() {
        val channelId = "trip_rec_channel"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Trip Recording", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val stopIntent = Intent(this, TripService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Recording trip…")
            .setContentText("Tap to open. Use STOP to finish.")
            .setContentIntent(openApp)
            .addAction(0, "STOP", stopPending)
            .setOngoing(true)
            .build()
        startForeground(42, notif)
    }

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val dest = if (!destLat.isNaN() && !destLng.isNaN()) Location("dest").apply {
                latitude = destLat; longitude = destLng
            } else null

            for (loc in result.locations) {
                scope.launch { repo.appendPoint(sessionId, loc) }
                if (dest != null && loc.accuracy <= 50f && loc.distanceTo(dest) <= 60f) {
                    stopRecording(true)
                    stopSelf()
                    break
                }
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateDistanceMeters(12f)
            .build()
        fused.requestLocationUpdates(req, callback, Looper.getMainLooper())
    }

    private fun stopRecording(finalize: Boolean) {
        fused.removeLocationUpdates(callback)
        if (finalize) {
            val endedAt = System.currentTimeMillis()
            scope.launch { repo.finishAndUpload(sessionId, startedAt, endedAt) }
        }
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_DEST_LAT = "dest_lat"
        const val EXTRA_DEST_LNG = "dest_lng"
        const val ACTION_STOP = "com.tahakom.locationpoc.trip.STOP"
    }
}
