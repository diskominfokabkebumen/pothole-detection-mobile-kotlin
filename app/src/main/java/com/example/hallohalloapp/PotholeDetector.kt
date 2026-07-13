package com.example.hallohalloapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

class PotholeDetector(private val context: Context) {

    private var tflite: Interpreter? = null
    private val modelName = "yolov8_pothole.tflite"

    init {
        try {
            val tfliteOptions = Interpreter.Options()
            tfliteOptions.setNumThreads(4)
            tflite = Interpreter(loadModelFile(), tfliteOptions)
            Log.d("AI_YOLO", "Model TFLite berhasil dimuat dengan sukses!")
        } catch (e: Exception) {
            Log.e("AI_YOLO", "Gagal memuat file model TFLite", e)
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun detectPothole(bitmap: Bitmap): List<Detection> {
        if (tflite == null) return emptyList()

        try {
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true)

            val inputBuffer = ByteBuffer.allocateDirect(1 * 3 * 640 * 640 * 4)
            inputBuffer.order(ByteOrder.nativeOrder())

            val pixels = IntArray(640 * 640)
            resizedBitmap.getPixels(pixels, 0, 640, 0, 0, 640, 640)

            for (channel in 0..2) {
                for (pixel in pixels) {
                    val value = when (channel) {
                        0 -> (pixel shr 16 and 0xFF)
                        1 -> (pixel shr 8 and 0xFF)
                        else -> (pixel and 0xFF)
                    }
                    inputBuffer.putFloat(value / 255.0f)
                }
            }
            inputBuffer.rewind()

            val outputBuffer = Array(1) { Array(5) { FloatArray(8400) } }
            tflite?.run(inputBuffer, outputBuffer)

            val confidenceThreshold = 0.15f
            val rawDetections = mutableListOf<Detection>()

            for (i in 0 until 8400) {
                val confidence = outputBuffer[0][4][i]
                if (confidence > confidenceThreshold) {
                    val cx = outputBuffer[0][0][i]
                    val cy = outputBuffer[0][1][i]
                    val w = outputBuffer[0][2][i]
                    val h = outputBuffer[0][3][i]
                    rawDetections.add(Detection(cx, cy, w, h, confidence))
                }
            }

            val finalDetections = nonMaxSuppression(rawDetections, iouThreshold = 0.4f)

            for (d in finalDetections) {
                Log.d("AI_DEBUG_BOX", "cx=${d.centerX} cy=${d.centerY} w=${d.width} h=${d.height} conf=${d.confidence}")
            }

            return finalDetections

        } catch (e: Exception) {
            Log.e("AI_YOLO_EXEC", "Gagal memproses frame gambar", e)
            return emptyList()
        }
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

    private fun nonMaxSuppression(detections: List<Detection>, iouThreshold: Float): List<Detection> {
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val kept = mutableListOf<Detection>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            kept.add(best)
            sorted.removeAll { calculateIoU(best, it) > iouThreshold }
        }

        return kept
    }

    fun close() {
        tflite?.close()
        tflite = null
    }
}