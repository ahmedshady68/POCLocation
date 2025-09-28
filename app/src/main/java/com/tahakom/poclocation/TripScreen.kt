package com.tahakom.poclocation

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.tahakom.poclocation.permissions.PermissionCoordinator
import com.tahakom.poclocation.repo.TripRepository
import com.tahakom.poclocation.trip.ExternalNav
import com.tahakom.poclocation.trip.TripService

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TripViewModel @Inject constructor(
    private val repo: TripRepository
) : ViewModel() {
    fun newSessionId() = repo.newSessionId()
}

@Composable
fun TripScreenWithPermissions(vm: TripViewModel = hiltViewModel()) {
    var ready by remember { mutableStateOf(false) }
    if (!ready) PermissionCoordinator(showBackgroundRationale = true, onReady = { ready = true })
    TripScreenContent(vm = vm, enabled = ready)
}

@Composable
private fun TripScreenContent(vm: TripViewModel, enabled: Boolean) {
    val ctx = LocalContext.current
    var destLat by remember { mutableStateOf("24.7136") } // Riyadh sample
    var destLng by remember { mutableStateOf("46.6753") }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = destLat,
            onValueChange = { destLat = it },
            label = { Text("Dest Lat") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = destLng,
            onValueChange = { destLng = it },
            label = { Text("Dest Lng") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val lat = destLat.toDoubleOrNull() ?: return@Button
                val lng = destLng.toDoubleOrNull() ?: return@Button
                val sessionId = vm.newSessionId()
                // Start recorder (foreground service)
                ctx.startService(Intent(ctx, TripService::class.java).apply {
                    putExtra(TripService.EXTRA_SESSION_ID, sessionId)
                    putExtra(TripService.EXTRA_DEST_LAT, lat)
                    putExtra(TripService.EXTRA_DEST_LNG, lng)
                })
                // Launch Google Maps navigation
                ExternalNav.startGoogleMaps(ctx, lat, lng)
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Start navigate to the site") }

        Button(
            onClick = {
                ctx.startService(Intent(ctx, TripService::class.java).apply {
                    action = TripService.ACTION_STOP
                })
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("End trip (manual)") }

        Text("Note: You may need to exclude the app from battery optimizations on some devices.")
    }
}
