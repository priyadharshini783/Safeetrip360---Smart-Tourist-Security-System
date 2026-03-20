package com.example.safeetrip360

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Vibrator
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            Log.e("Geofence", "Error receiving event")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        triggeringGeofences?.forEach { geofence ->
            val id = geofence.requestId

            // --- MODE 1: SAFE ZONE (EXIT ALERT) ---
            if (id.startsWith("SAFE_") && geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                val message = "🚨 ALERT: You have left the Safe Zone!"

                // 1. Show Notification (CRITICAL for testing)
                sendNotification(context, "Danger Alert", message)

                // 2. Send SMS
                sendSMS(context, "7904004217", message)
            }

            // --- MODE 2: DANGER ZONE (ENTER ALERT) ---
            if (id.startsWith("DANGER_") && geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                val message = "⚠️ WARNING: You entered a Danger Zone!"

                // 1. Show Notification
                sendNotification(context, "Safety Warning", message)

                // 2. Vibrate
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(1000)
            }
        }
    }

    // --- HELPER TO SHOW NOTIFICATION ---
//    private fun sendNotification(context: Context, title: String, content: String) {
//        // --- 1. SAVE TO HISTORY (NEW LINE) ---
//        AlertHistoryManager.saveAlert(context, title, content)
//        // -------------------------------------
//
//        val channelId = "geofence_alerts"
//        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        // ... (rest of your existing notification code) ...
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(channelId, "Safety Alerts", NotificationManager.IMPORTANCE_HIGH)
//            notificationManager.createNotificationChannel(channel)
//        }
//        // ...
//        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
//    }
    // --- HELPER TO SEND SMS ---
    // Paste this inside GeofenceBroadcastReceiver class (replace the old sendNotification)

    private fun sendNotification(context: Context, title: String, content: String) {
        // --- SAFE HISTORY SAVING (Won't crash the app) ---
        try {
            AlertHistoryManager.saveAlert(context, title, content)
        } catch (e: Exception) {
            android.util.Log.e("Geofence", "Failed to save history: ${e.message}")
        }
        // -------------------------------------------------

        val channelId = "geofence_alerts"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel (Required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Safety Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Create Builder
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        // Show Notification
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
    private fun sendSMS(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d("Geofence", "SMS Sent")
        } catch (e: Exception) {
            Log.e("Geofence", "SMS Failed: ${e.message}")
        }
    }
}