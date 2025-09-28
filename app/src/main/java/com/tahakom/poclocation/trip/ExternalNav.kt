package com.tahakom.poclocation.trip

import android.content.Context
import android.content.Intent
import android.net.Uri

object ExternalNav {
    fun startGoogleMaps(ctx: Context, lat: Double, lng: Double, label: String? = null) {
        val uri = Uri.parse("google.navigation:q=$lat,$lng" + (label?.let { "($it)" } ?: "") + "&mode=d")
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
