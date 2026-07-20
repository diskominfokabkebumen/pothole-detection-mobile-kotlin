package com.example.hallohalloapp.ui

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hallohalloapp.R
import com.example.hallohalloapp.detection.PotholeMapMiniAdapter
import com.example.hallohalloapp.detection.PotholeRecord
import com.example.hallohalloapp.detection.PotholeStorage
import com.example.hallohalloapp.MapTileConfig
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MapFragment : Fragment() {

    private var mapView: MapView? = null
    private var tvEmptyMap: TextView? = null
    private var tvMapTotalPoints: TextView? = null
    private var tvMapAvgConfidence: TextView? = null
    private var rvMapRecentReports: RecyclerView? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionRequestCode = 202

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
        Configuration.getInstance().userAgentValue = requireContext().packageName
        Configuration.getInstance().osmdroidBasePath = File(requireContext().cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = File(requireContext().cacheDir, "osmdroid/tiles")

        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val map = view.findViewById<MapView>(R.id.mapView)
        tvEmptyMap = view.findViewById(R.id.tvEmptyMap)
        tvMapTotalPoints = view.findViewById(R.id.tvMapTotalPoints)
        tvMapAvgConfidence = view.findViewById(R.id.tvMapAvgConfidence)
        rvMapRecentReports = view.findViewById(R.id.rvMapRecentReports)
        mapView = map

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        map.setTileSource(MapTileConfig.CARTO_LIGHT)
        map.setMultiTouchControls(true)

        rvMapRecentReports?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        view.findViewById<ImageView>(R.id.ivMapNotification)?.setOnClickListener {
            Toast.makeText(context, "Membuka daftar pemberitahuan masuk...", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<FloatingActionButton>(R.id.fabMyLocation)?.setOnClickListener {
            goToMyLocation()
        }

        val records = PotholeStorage.loadAll(requireContext())
        refreshMapUI(records)

        if (records.isEmpty()) {
            map.controller.setZoom(5.0)
            map.controller.setCenter(GeoPoint(-2.5, 118.0))
        } else if (records.size == 1) {
            map.controller.setZoom(17.0)
            map.controller.setCenter(GeoPoint(records[0].latitude, records[0].longitude))
        } else {
            val geoPoints = records.map { GeoPoint(it.latitude, it.longitude) }
            map.post {
                val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                map.zoomToBoundingBox(boundingBox, true, 100)
            }
        }
    }

    /** Refresh marker di peta + kartu ringkasan + daftar mini bawah, sekaligus. */
    private fun refreshMapUI(records: List<PotholeRecord>) {
        addMarkersToMap(records)

        tvMapTotalPoints?.text = records.size.toString()
        val avgConfidence = if (records.isNotEmpty()) {
            (records.map { it.confidence }.average() * 100).toInt()
        } else {
            0
        }
        tvMapAvgConfidence?.text = "$avgConfidence%"

        val recentRecords = records.sortedByDescending { it.timestamp }.take(10)
        rvMapRecentReports?.adapter = PotholeMapMiniAdapter(recentRecords) { record ->
            jumpToPoint(record)
        }
    }

    private fun jumpToPoint(record: PotholeRecord) {
        val map = mapView ?: return
        val point = GeoPoint(record.latitude, record.longitude)
        map.controller.animateTo(point)
        map.controller.setZoom(18.0)
    }

    private fun addMarkersToMap(records: List<PotholeRecord>) {
        val map = mapView ?: return
        map.overlays.clear()

        tvEmptyMap?.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE

        for ((index, record) in records.withIndex()) {
            val point = GeoPoint(record.latitude, record.longitude)
            val marker = Marker(map)
            marker.position = point
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "Lubang ${index + 1}"
            marker.setOnMarkerClickListener { _, _ ->
                showPotholeDetailDialog(record)
                true
            }
            map.overlays.add(marker)
        }

        map.invalidate()
    }

    private fun showPotholeDetailDialog(record: PotholeRecord) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_pothole_detail, null)

        val imgPhoto = dialogView.findViewById<ImageView>(R.id.imgDetailPhoto)
        val tvConfidence = dialogView.findViewById<TextView>(R.id.tvDetailConfidence)
        val tvLocation = dialogView.findViewById<TextView>(R.id.tvDetailLocation)
        val tvTime = dialogView.findViewById<TextView>(R.id.tvDetailTime)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnDeleteDetail)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseDetail)

        val bitmap = BitmapFactory.decodeFile(record.imagePath)
        if (bitmap != null) {
            imgPhoto.setImageBitmap(bitmap)
        }

        val confidencePercent = (record.confidence * 100).toInt()
        tvConfidence.text = "Tingkat keyakinan: $confidencePercent%"
        tvLocation.text = "Mencari alamat..."

        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        tvTime.text = "Waktu: ${dateFormat.format(Date(record.timestamp))}"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnDelete.setOnClickListener {
            PotholeStorage.deleteRecord(requireContext(), record)
            refreshMapUI(PotholeStorage.loadAll(requireContext()))
            dialog.dismiss()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        resolveAddress(record, tvLocation)
    }

    private fun resolveAddress(record: PotholeRecord, tvLocation: TextView) {
        Thread {
            if (!isAdded) return@Thread

            val addressText = try {
                val geocoder = Geocoder(requireContext(), Locale("in", "ID"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(record.latitude, record.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    addresses[0].getAddressLine(0)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }

            if (isAdded) {
                activity?.runOnUiThread {
                    if (isAdded) {
                        tvLocation.text = addressText
                            ?: "Lokasi: ${String.format("%.5f", record.latitude)}, ${String.format("%.5f", record.longitude)}"
                    }
                }
            }
        }.start()
    }

    private fun goToMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionRequestCode
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val map = mapView ?: return@addOnSuccessListener
                val myPoint = GeoPoint(location.latitude, location.longitude)
                map.controller.animateTo(myPoint)
                map.controller.setZoom(17.0)
            } else {
                Toast.makeText(
                    context,
                    "Lokasi belum tersedia, coba lagi sebentar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == locationPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                goToMyLocation()
            } else {
                Toast.makeText(context, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDetach()
        mapView = null
        tvEmptyMap = null
        tvMapTotalPoints = null
        tvMapAvgConfidence = null
        rvMapRecentReports = null
    }
}