package com.example.hallohalloapp.detection

import kotlin.math.max
import kotlin.math.min

class PotholeTracker {

    private data class TrackedPothole(
        var lastSeenDetection: Detection,
        var lastSeenTime: Long
    )

    private val trackedPotholes = mutableListOf<TrackedPothole>()
    private val matchIouThreshold = 0.3f
    private val expiryMillis = 3000L

    fun processDetections(detections: List<Detection>): Int {
        val currentTime = System.currentTimeMillis()
        var newCount = 0

        trackedPotholes.removeAll { currentTime - it.lastSeenTime > expiryMillis }

        for (detection in detections) {
            val existingMatch = trackedPotholes.find { calculateIoU(it.lastSeenDetection, detection) > matchIouThreshold }

            if (existingMatch != null) {
                existingMatch.lastSeenDetection = detection
                existingMatch.lastSeenTime = currentTime
            } else {
                trackedPotholes.add(TrackedPothole(detection, currentTime))
                newCount++
            }
        }

        return newCount
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