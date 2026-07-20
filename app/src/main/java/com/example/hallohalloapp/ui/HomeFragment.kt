package com.example.hallohalloapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.hallohalloapp.ui.ProfileFragment
import com.example.hallohalloapp.R
import com.example.hallohalloapp.ui.ReportFragment
import com.example.hallohalloapp.detection.storage.AppStatsStorage
import com.example.hallohalloapp.detection.DetectionActivity
import com.example.hallohalloapp.detection.PotholeDetailDialogHelper
import com.example.hallohalloapp.adapter.PotholeMiniReportAdapter
import com.example.hallohalloapp.detection.storage.PotholeStorage
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class HomeFragment : Fragment() {

    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val ivProfile = view.findViewById<ImageView>(R.id.ivProfile)
        val ivNotification = view.findViewById<ImageView>(R.id.ivNotification)
        val tvLihatSemua = view.findViewById<TextView>(R.id.tvLihatSemua)
        val btnLapor = view.findViewById<Button>(R.id.btnLapor)
        val btnSelengkapnya = view.findViewById<Button>(R.id.btnSelengkapnya)
        val rvRecentReports = view.findViewById<RecyclerView>(R.id.rvRecentReports)

        rvRecentReports?.layoutManager = LinearLayoutManager(requireContext())

        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)

        ivProfile?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .commit()
            bottomNav?.selectedItemId = R.id.nav_profile
        }

        tvLihatSemua?.setOnClickListener {
            navigateToReport()
        }

        btnSelengkapnya?.setOnClickListener {
            navigateToReport()
        }


        ivNotification?.setOnClickListener {
            Toast.makeText(context, "Membuka daftar pemberitahuan masuk...", Toast.LENGTH_SHORT).show()
        }


        btnLapor?.setOnClickListener {
            showConfirmationDialog()
        }

        updateStats(view)

        return view
    }

    private fun navigateToReport() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ReportFragment())
            .commit()
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.selectedItemId = R.id.nav_report
    }

    override fun onResume() {
        super.onResume()
        view?.let { updateStats(it) }
    }

    private fun updateStats(rootView: View) {
        val tvTotalLaporan = rootView.findViewById<TextView>(R.id.tvTotalLaporan)
        val tvTotalJarak = rootView.findViewById<TextView>(R.id.tvTotalJarak)
        val tvAvgConfidence = rootView.findViewById<TextView>(R.id.tvAvgConfidence)
        val tvLaporanBulanIni = rootView.findViewById<TextView>(R.id.tvLaporanBulanIni)

        val records = PotholeStorage.loadAll(requireContext())

        tvTotalLaporan?.text = records.size.toString()

        val totalDistanceKm = AppStatsStorage.getTotalDistanceKm(requireContext())
        tvTotalJarak?.text = String.format("%.1f km", totalDistanceKm)

        val avgConfidence = if (records.isNotEmpty()) {
            (records.map { it.confidence }.average() * 100).toInt()
        } else {
            0
        }
        tvAvgConfidence?.text = "$avgConfidence%"

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val reportsThisMonth = records.count { record ->
            val recordCalendar = Calendar.getInstance().apply { timeInMillis = record.timestamp }
            recordCalendar.get(Calendar.MONTH) == currentMonth &&
                    recordCalendar.get(Calendar.YEAR) == currentYear
        }
        tvLaporanBulanIni?.text = reportsThisMonth.toString()

        val rvRecentReports = rootView.findViewById<RecyclerView>(R.id.rvRecentReports)
        val tvEmptyRecentReports = rootView.findViewById<TextView>(R.id.tvEmptyRecentReports)

        val recentReports = records.sortedByDescending { it.timestamp }.take(2)

        tvEmptyRecentReports?.visibility = if (recentReports.isEmpty()) View.VISIBLE else View.GONE
        rvRecentReports?.visibility = if (recentReports.isEmpty()) View.GONE else View.VISIBLE

        rvRecentReports?.adapter = PotholeMiniReportAdapter(recentReports) { record ->
            PotholeDetailDialogHelper.show(this, record) {
                updateStats(rootView)
            }
        }
    }

    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Mulai Pemantauan Otomatis?")
        builder.setMessage("Aplikasi akan membuka kamera penuh dan melacak lubang jalan secara otomatis menggunakan AI selama Anda berkendara.")

        builder.setPositiveButton("MULAI") { dialog, _ ->
            dialog.dismiss()
            if (checkAndRequestPermissions()) {
                Toast.makeText(context, "Membuka Layar Kamera AI...", Toast.LENGTH_SHORT).show()
                // TODO: Di sini nanti kita panggil Activity baru yang layarnya landscape!
                Toast.makeText(context, "Membuka Layar Kamera AI...", Toast.LENGTH_SHORT).show()

                val intent = Intent(activity, DetectionActivity::class.java)
                startActivity(intent)
            }
        }


        builder.setNegativeButton("BATAL") { dialog, _ ->
            dialog.dismiss()
        }


        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun checkAndRequestPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        val locationPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)

        val listPermissionsNeeded = ArrayList<String>()

        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA)
        }
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            requestPermissions(listPermissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Akses diberikan! Silakan klik Mulai lagi untuk membuka kamera AI.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Akses ditolak! Aplikasi butuh izin untuk deteksi otomatis.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}