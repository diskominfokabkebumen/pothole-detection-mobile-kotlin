package com.example.hallohalloapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class BoxOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var detections: List<Detection> = emptyList()

    private val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val textPaint = Paint().apply {
        color = Color.RED
        textSize = 40f
        style = Paint.Style.FILL
    }

    fun setDetections(newDetections: List<Detection>) {
        detections = newDetections
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (detection in detections) {
            val boxCenterX = detection.centerX * width
            val boxCenterY = detection.centerY * height
            val boxWidth = detection.width * width
            val boxHeight = detection.height * height

            val left = boxCenterX - (boxWidth / 2)
            val top = boxCenterY - (boxHeight / 2)
            val right = boxCenterX + (boxWidth / 2)
            val bottom = boxCenterY + (boxHeight / 2)

            canvas.drawRect(left, top, right, bottom, boxPaint)
            canvas.drawText(
                "Lubang ${(detection.confidence * 100).toInt()}%",
                left,
                if (top > 40) top - 10 else top + 40,
                textPaint
            )
        }
    }
}