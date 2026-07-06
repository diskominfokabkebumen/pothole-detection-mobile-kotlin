package com.example.hallohalloapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PotholeDetector(private val context: Context) {

    private var tflite: Interpreter? = null
    private val modelName = "yolov8_pothole.tflite"

    init {
        try {
            // Memuat file model .tflite dari folder assets ke dalam memori
            val tfliteOptions = Interpreter.Options()
            tfliteOptions.setNumThreads(4) // Menggunakan 4 core CPU agar pemrosesan lancar
            tflite = Interpreter(loadModelFile(), tfliteOptions)
            Log.d("AI_YOLO", "Model TFLite berhasil dimuat dengan sukses!")
        } catch (e: Exception) {
            Log.e("AI_YOLO", "Gagal memuat file model TFLite", e)
        }
    }

    // Fungsi internal untuk membaca berkas dari folder assets
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Fungsi utama yang bertugas mendeteksi lubang dari potongan gambar kamera
    fun detectPothole(bitmap: Bitmap): Int {
        if (tflite == null) return 0

        // 1. Preprocessing: Ubah ukuran gambar jadi 640x640 (Standar input YOLOv8) dan normalisasi piksel
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(640, 640, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0.0f, 255.0f)) // Mengubah nilai piksel menjadi rentang 0.0 - 1.0
            .build()

        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. Alokasikan wadah untuk menampung hasil output dari model YOLO
        // YOLOv8 memiliki struktur output berbentuk array multi-dimensi [1, 5, 8400]
        // Di mana 5 mewakili: x_center, y_center, width, height, dan confidence score kelas lubang
        val outputBuffer = Array(1) { Array(5) { FloatArray(8400) } }

        // 3. Jalankan inferensi kecerdasan buatan
        tflite?.run(tensorImage.buffer, outputBuffer)

        // 4. Postprocessing: Hitung berapa banyak objek lubang yang lolos batas akurasi (Confidence Threshold > 0.45)
        var detectedCount = 0
        val confidenceThreshold = 0.45f

        for (i in 0 until 8400) {
            val confidence = outputBuffer[0][4][i] // Mengambil nilai skor keyakinan kelas lubang
            if (confidence > confidenceThreshold) {
                detectedCount++
            }
        }

        // Untuk sementara, kita batasi return deteksinya agar logis jika ada frame beruntun
        return if (detectedCount > 0) 1 else 0
    }

    // Matikan interpreter jika halaman ditutup untuk membebaskan RAM ponsel
    fun close() {
        tflite?.close()
        tflite = null
    }
}