package com.example.hallohalloapp

data class Detection(
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float,
    val confidence: Float
)