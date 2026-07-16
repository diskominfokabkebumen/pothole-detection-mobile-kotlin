package com.example.hallohalloapp.detection

import java.io.Serializable

data class PotholeRecord(
    val imagePath: String,
    val latitude: Double,
    val longitude: Double,
    val confidence: Float,
    val timestamp: Long,
    var isSelected: Boolean = true
) : Serializable