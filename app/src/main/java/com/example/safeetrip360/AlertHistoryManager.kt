package com.example.safeetrip360

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AlertHistoryManager {
    private const val PREF_NAME = "alert_history_prefs"
    private const val KEY_ALERTS = "saved_alerts"

    // Fix: We now work directly with Strings to avoid the type error
    fun saveAlert(context: Context, title: String, message: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // 1. Get existing raw strings
        val existingSet = prefs.getStringSet(KEY_ALERTS, mutableSetOf()) ?: mutableSetOf()
        val currentList = existingSet.toMutableList()

        // 2. Create the new entry string
        val timestamp = SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault()).format(Date())
        // Format: "Time|Title|Message"
        val newEntry = "$timestamp|$title|$message"

        // 3. Add to the list
        currentList.add(newEntry)

        // 4. Save back to storage
        prefs.edit().putStringSet(KEY_ALERTS, currentList.toSet()).apply()
    }

    // Get all alerts and convert them to readable data
    fun getAlerts(context: Context): List<Triple<String, String, String>> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val rawSet = prefs.getStringSet(KEY_ALERTS, emptySet()) ?: emptySet()

        return rawSet.map { entry ->
            val parts = entry.split("|")
            if (parts.size >= 3) {
                Triple(parts[0], parts[1], parts[2]) // Time, Title, Message
            } else {
                Triple("", "Info", entry)
            }
        }.sortedByDescending { it.first } // Sort by time (newest first)
    }

    fun clearHistory(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}