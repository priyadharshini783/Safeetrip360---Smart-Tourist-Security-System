package com.example.safeetrip360

import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest

class GeofenceHelper(base: Context) : ContextWrapper(base) {

    private val internalPendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        // FLAG_MUTABLE is required for Android 12+
        PendingIntent.getBroadcast(this, 2607, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    // Creates the fence with a specific ID and Trigger Type (Enter/Exit)
    fun getGeofence(ID: String, lat: Double, lng: Double, radius: Float, transitionType: Int): Geofence {
        return Geofence.Builder()
            .setCircularRegion(lat, lng, radius)
            .setRequestId(ID)
            .setTransitionTypes(transitionType)
            .setLoiteringDelay(5000)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()
    }

    fun getGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
    }

    fun getPendingIntent() = internalPendingIntent
}