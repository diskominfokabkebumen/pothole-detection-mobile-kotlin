package com.example.hallohalloapp.detection

import android.content.Context

object AppStatsStorage {

    private const val PREFS_NAME = "app_stats"
    private const val KEY_TOTAL_DISTANCE_METERS = "total_distance_meters"

    fun addDistance(context: Context, distanceInMetersThisSession: Double) {
        if (distanceInMetersThisSession <= 0.0) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentTotal = prefs.getFloat(KEY_TOTAL_DISTANCE_METERS, 0f)
        val newTotal = currentTotal + distanceInMetersThisSession.toFloat()
        prefs.edit().putFloat(KEY_TOTAL_DISTANCE_METERS, newTotal).apply()
    }

    fun getTotalDistanceKm(context: Context): Double {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val totalMeters = prefs.getFloat(KEY_TOTAL_DISTANCE_METERS, 0f)
        return totalMeters / 1000.0
    }
}