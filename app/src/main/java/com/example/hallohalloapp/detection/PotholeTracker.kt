package com.example.hallohalloapp.detection

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class PotholeTracker {

    private data class TrackedPothole(
        var lastSeenDetection: Detection,
        var lastSeenTime: Long
    )

    private val trackedPotholes = mutableListOf<TrackedPothole>()
    private val matchIouThreshold = 0.1f
    private val matchDistanceThreshold = 0.18f
    private val expiryMillis = 5000L

    fun processDetections(detections: List<Detection>): List<Detection> {
        val currentTime = System.currentTimeMillis()
        val newDetections = mutableListOf<Detection>()

        trackedPotholes.removeAll { currentTime - it.lastSeenTime > expiryMillis }

        for (detection in detections) {
            val existingMatch = trackedPotholes.find { isSameObject(it.lastSeenDetection, detection) }

            if (existingMatch != null) {
                existingMatch.lastSeenDetection = detection
                existingMatch.lastSeenTime = currentTime
            } else {
                trackedPotholes.add(TrackedPothole(detection, currentTime))
                newDetections.add(detection)
            }
        }

        return newDetections
    }

    private fun isSameObject(a: Detection, b: Detection): Boolean {
        if (calculateIoU(a, b) > matchIouThreshold) return true
        if (calculateCenterDistance(a, b) < matchDistanceThreshold) return true
        return false
    }

    private fun calculateCenterDistance(a: Detection, b: Detection): Float {
        val dx = a.centerX - b.centerX
        val dy = a.centerY - b.centerY
        return sqrt(dx * dx + dy * dy)
    }

    private fun calculateIoU(a: Detection, b: Detection): Float {
        val aLeft = a.centerX - a.width / 2
        val aTop = a.centerY - a.height / 2
        val aRight = a.centerX + a.width / 2
        val aBottom = a.centerY + a.height / 2

        val bLeft = b.centerX - b.width / 2
        val bTop = b.centerY - b.height / 2
        val bRight = b.centerX + b.width / 2
        val bBottom = b.centerY + b.height / 2

        val interLeft = max(aLeft, bLeft)
        val interTop = max(aTop, bTop)
        val interRight = min(aRight, bRight)
        val interBottom = min(aBottom, bBottom)

        val interArea = max(0f, interRight - interLeft) * max(0f, interBottom - interTop)
        val aArea = a.width * a.height
        val bArea = b.width * b.height
        val unionArea = aArea + bArea - interArea

        return if (unionArea <= 0f) 0f else interArea / unionArea
    }

    fun reset() {
        trackedPotholes.clear()
    }
}