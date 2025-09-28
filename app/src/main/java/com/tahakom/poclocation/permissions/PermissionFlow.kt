package com.tahakom.poclocation.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

fun hasFineLocation(ctx: Activity) =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun hasPostNotifications(ctx: Activity) =
    if (Build.VERSION.SDK_INT >= 33)
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    else true

fun hasBackgroundLocation(ctx: Activity) =
    if (Build.VERSION.SDK_INT >= 29)
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    else true

private fun openAppSettings(ctx: Activity) {
    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", ctx.packageName, null)
    })
}

sealed interface PermissionStep {
    data object Idle : PermissionStep
    data object NeedFine : PermissionStep
    data object NeedNotifications : PermissionStep
    data object NeedBackground : PermissionStep
    data object Done : PermissionStep
}

@SuppressLint("ContextCastToActivity")
@Composable
fun PermissionCoordinator(showBackgroundRationale: Boolean, onReady: () -> Unit) {
    val ctx = LocalContext.current as Activity
    var step by remember { mutableStateOf<PermissionStep>(PermissionStep.Idle) }

    val fine = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        step = if (granted) PermissionStep.NeedNotifications else PermissionStep.NeedFine
    }
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        step = if (showBackgroundRationale) PermissionStep.NeedBackground else PermissionStep.Done
    }

    LaunchedEffect(Unit) {
        step = when {
            !hasFineLocation(ctx) -> PermissionStep.NeedFine
            !hasPostNotifications(ctx) -> PermissionStep.NeedNotifications
            showBackgroundRationale && !hasBackgroundLocation(ctx) -> PermissionStep.NeedBackground
            else -> PermissionStep.Done
        }
    }

    when (step) {
        PermissionStep.NeedFine -> SimpleDialog(
            title = "Allow precise location",
            message = "We need precise location to record your trip route."
        ) { if (Build.VERSION.SDK_INT >= 23) fine.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

        PermissionStep.NeedNotifications -> SimpleDialog(
            title = "Allow trip notification",
            message = "Android requires a persistent notification while recording."
        ) {
            if (Build.VERSION.SDK_INT >= 33) notif.launch(Manifest.permission.POST_NOTIFICATIONS)
            else step = if (showBackgroundRationale) PermissionStep.NeedBackground else PermissionStep.Done
        }

        PermissionStep.NeedBackground -> SimpleDialog(
            title = "Record in background (optional)",
            message = "Enable 'Allow all the time' in Settings to keep recording if the screen is off or you switch apps."
        ) { openAppSettings(ctx) }

        PermissionStep.Done, PermissionStep.Idle -> Unit
    }

    if (step == PermissionStep.Done) LaunchedEffect("ready") { onReady() }
}

@Composable
private fun SimpleDialog(title: String, message: String, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Continue") } },
        dismissButton = { TextButton(onClick = { /* skip */ }) { Text("Not now") } }
    )
}
