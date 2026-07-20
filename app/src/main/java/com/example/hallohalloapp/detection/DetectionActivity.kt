package com.example.hallohalloapp.detection

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.hallohalloapp.R
import com.google.android.gms.location.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max

class DetectionActivity : AppCompatActivity() {

    private var viewFinderDetection: PreviewView? = null
    private var boxOverlayView: BoxOverlayView? = null
    private var tvDistanceLabel: TextView? = null
    private var tvPotholeCount: TextView? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: Location? = null
    private var totalDistanceInMeters = 0.0

    private lateinit var potholeDetector: PotholeDetector
    private val potholeTracker = PotholeTracker()
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var totalPotholesDetected = 0
    private val capturedPotholes = mutableListOf<PotholeRecord>()

    private var cameraProvider: ProcessCameraProvider? = null

    @Volatile
    private var isShuttingDown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_detection)

        viewFinderDetection = findViewById(R.id.viewFinderDetection)
        boxOverlayView = findViewById(R.id.boxOverlayView)
        tvDistanceLabel = findViewById(R.id.tvDistanceLabel)
        tvPotholeCount = findViewById(R.id.tvPotholeCount)
        val btnStopDetection = findViewById<Button>(R.id.btnStopDetection)

        potholeDetector = PotholeDetector(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        startCameraAndAIStream()
        setupLocationTracking()

        btnStopDetection.setOnClickListener {
            stopDetectionSafely()
        }
    }

    private fun startCameraAndAIStream() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val provider: ProcessCameraProvider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(viewFinderDetection?.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (isShuttingDown) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val rawBitmap = imageProxy.toBitmap()

                    if (rawBitmap != null) {
                        val bitmap = if (rotationDegrees != 0) {
                            val matrix = Matrix()
                            matrix.postRotate(rotationDegrees.toFloat())
                            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                        } else {
                            rawBitmap
                        }

                        val detections = potholeDetector.detectPothole(bitmap)

                        if (!isShuttingDown) {
                            runOnUiThread {
                                boxOverlayView?.setDetections(detections)
                            }

                            val newDetections = potholeTracker.processDetections(detections)
                            if (newDetections.isNotEmpty()) {
                                totalPotholesDetected += newDetections.size

                                for (newDetection in newDetections) {
                                    val annotatedBitmap = drawDetectionOnBitmap(bitmap, newDetection)
                                    val imagePath = saveBitmapSnapshot(annotatedBitmap)
                                    capturedPotholes.add(
                                        PotholeRecord(
                                            imagePath = imagePath,
                                            latitude = lastLocation?.latitude ?: 0.0,
                                            longitude = lastLocation?.longitude ?: 0.0,
                                            confidence = newDetection.confidence,
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                }

                                runOnUiThread {
                                    tvPotholeCount?.text = totalPotholesDetected.toString()
                                }
                            }
                        }
                    }
                    imageProxy.close()
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                provider.unbindAll()
                provider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)

            } catch (exc: Exception) {
                Log.e("DetectionCamera", "Gagal menyatukan Kamera & AI Pothole", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun drawDetectionOnBitmap(source: Bitmap, detection: Detection): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        val boxPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = max(4f, output.width * 0.008f)
        }

        val textPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            textSize = max(24f, output.width * 0.05f)
        }

        val boxCenterX = detection.centerX * output.width
        val boxCenterY = detection.centerY * output.height
        val boxWidth = detection.width * output.width
        val boxHeight = detection.height * output.height

        val left = boxCenterX - boxWidth / 2
        val top = boxCenterY - boxHeight / 2
        val right = boxCenterX + boxWidth / 2
        val bottom = boxCenterY + boxHeight / 2

        canvas.drawRect(left, top, right, bottom, boxPaint)

        val label = "Lubang ${(detection.confidence * 100).toInt()}%"
        val labelY = if (top > textPaint.textSize + 10) top - 10 else bottom + textPaint.textSize + 10
        canvas.drawText(label, left, labelY, textPaint)

        return output
    }

    private fun saveBitmapSnapshot(bitmap: Bitmap): String {
        val folder = File(filesDir, "pothole_snapshots")
        if (!folder.exists()) folder.mkdirs()
        val file = File(folder, "pothole_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return file.absolutePath
    }

    private fun setupLocationTracking() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateDistanceMeters(1.0f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (lastLocation != null) {
                        val distanceSegment = lastLocation!!.distanceTo(location)
                        if (distanceSegment > 0.5) {
                            totalDistanceInMeters += distanceSegment
                            val distanceInKm = totalDistanceInMeters / 1000.0
                            tvDistanceLabel?.text = "Jarak: ${String.format("%.1f", distanceInKm)} km"
                        }
                    }
                    lastLocation = location
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun stopDetectionSafely() {
        isShuttingDown = true
        cameraProvider?.unbindAll()

        cameraExecutor.shutdown()
        try {
            cameraExecutor.awaitTermination(2, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Log.e("DetectionActivity", "Gagal menunggu kamera berhenti", e)
        }

        potholeDetector.close()

        AppStatsStorage.addDistance(this, totalDistanceInMeters)

        val intent = Intent(this, ScanResultActivity::class.java)
        intent.putExtra("pothole_records", ArrayList(capturedPotholes))
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isShuttingDown) {
            isShuttingDown = true
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            try {
                cameraExecutor.awaitTermination(2, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Log.e("DetectionActivity", "Gagal menunggu kamera berhenti", e)
            }
            potholeDetector.close()

            AppStatsStorage.addDistance(this, totalDistanceInMeters)
        }

        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}