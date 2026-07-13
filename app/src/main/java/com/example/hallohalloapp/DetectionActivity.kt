package com.example.hallohalloapp

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import java.util.concurrent.Executors

class DetectionActivity : AppCompatActivity() {

    private var viewFinderDetection: PreviewView? = null
    private var tvDistanceLabel: TextView? = null
    private var tvPotholeCount: TextView? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: Location? = null
    private var totalDistanceInMeters = 0.0

    private lateinit var potholeDetector: PotholeDetector
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var totalPotholesDetected = 0
    private var isCooldownActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_detection)

        viewFinderDetection = findViewById(R.id.viewFinderDetection)
        tvDistanceLabel = findViewById(R.id.tvDistanceLabel)
        tvPotholeCount = findViewById(R.id.tvPotholeCount)
        val btnStopDetection = findViewById<Button>(R.id.btnStopDetection)

        potholeDetector = PotholeDetector(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        startCameraAndAIStream()

        setupLocationTracking()

        btnStopDetection.setOnClickListener {
            Toast.makeText(this, "Pemantauan Selesai. Menemukan $totalPotholesDetected lubang.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun startCameraAndAIStream() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(viewFinderDetection?.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Mengabaikan frame lama jika CPU sibuk
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val bitmap = imageProxy.toBitmap()
                    if (bitmap != null) {
                        val result = potholeDetector.detectPothole(bitmap)

                        if (result == 1 && !isCooldownActive) {
                            totalPotholesDetected++

                            runOnUiThread {
                                tvPotholeCount?.text = totalPotholesDetected.toString()
                            }

                            isCooldownActive = true
                            Thread {
                                Thread.sleep(1500)
                                isCooldownActive = false
                            }.start()
                        }
                    }
                    imageProxy.close()
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)

            } catch (exc: Exception) {
                Log.e("DetectionCamera", "Gagal menyatukan Kamera & AI Pothole", exc)
            }
        }, ContextCompat.getMainExecutor(this))
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        potholeDetector.close() // Bebaskan memori RAM dari TFLite Interpreter
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}