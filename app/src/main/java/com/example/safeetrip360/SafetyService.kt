package com.example.safeetrip360


import android.app.*
import android.content.Intent
import android.os.*
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

// Extending Service() fixes the "'SafetyService' must extend android.app.Service" error
class SafetyService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val emergencyContact = "7904004217"

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create the notification channel required for Android 8.0+
        createNotificationChannel()

        // Create the persistent notification required for Foreground Services
        val notification = NotificationCompat.Builder(this, "safety_channel")
            .setContentTitle("SafeeTrip360 is Active")
            .setContentText("Your live location is being shared for your safety.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        // Start as foreground service to prevent the system from killing the tracking
        startForeground(1, notification)

        startLocationUpdates()

        return START_STICKY
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                // Fixed URL syntax for clickable maps link
                val mapsUrl = "https://www.google.com/maps?q=${location.latitude},${location.longitude}"
                val message = "🚨 LIVE SOS UPDATE\nMy current location: $mapsUrl"

                // Send background SMS
                try {
                    val smsManager = getSystemService(SmsManager::class.java)
                    smsManager.sendTextMessage(emergencyContact, null, message, null, null)
                } catch (e: Exception) {
                    android.util.Log.e("SafetyService", "SMS failed: ${e.message}")
                }

                // Sync with Firebase Cloud
                val user = FirebaseAuth.getInstance().currentUser
                val logData = hashMapOf(
                    "lat" to location.latitude,
                    "lng" to location.longitude,
                    "time" to FieldValue.serverTimestamp()
                )
                user?.uid?.let { uid ->
                    FirebaseFirestore.getInstance().collection("users").document(uid).collection("logs").add(logData)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (unlikely: SecurityException) {
            android.util.Log.e("SafetyService", "Lost location permission. Could not request updates.")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "safety_channel",
                "Safety Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // Stop tracking when the service is stopped
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }
}