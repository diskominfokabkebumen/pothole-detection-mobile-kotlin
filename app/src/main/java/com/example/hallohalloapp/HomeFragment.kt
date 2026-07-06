package com.example.hallohalloapp

import android.Manifest
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
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment() {

    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Inisialisasi komponen visual sesuai ID XML
        val ivProfile = view.findViewById<ImageView>(R.id.ivProfile)
        val ivNotification = view.findViewById<ImageView>(R.id.ivNotification)
        val tvLihatSemua = view.findViewById<TextView>(R.id.tvLihatSemua)
        val btnLapor = view.findViewById<Button>(R.id.btnLapor)

        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // 1. Pindah ke Halaman Profil
        ivProfile?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .commit()
            bottomNav?.selectedItemId = R.id.nav_profile
        }

        // 2. Pindah ke Halaman Laporan (Lihat Semua)
        tvLihatSemua?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ReportFragment())
                .commit()
            bottomNav?.selectedItemId = R.id.nav_report
        }

        // 3. Tombol Notifikasi Lonceng
        ivNotification?.setOnClickListener {
            Toast.makeText(context, "Membuka daftar pemberitahuan masuk...", Toast.LENGTH_SHORT).show()
        }

        // 4. Tombol "Laporkan Jalan Rusak" -> Memicu Pop-up Dialog Konfirmasi
        btnLapor?.setOnClickListener {
            showConfirmationDialog()
        }

        return view
    }

    // Fungsi untuk menampilkan Pop-up Dialog Konfirmasi sebelum masuk ke kamera AI
    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Mulai Pemantauan Otomatis?")
        builder.setMessage("Aplikasi akan membuka kamera penuh dan melacak lubang jalan secara otomatis menggunakan AI selama Anda berkendara.")

        // Jika pengguna setuju dan klik "MULAI"
        builder.setPositiveButton("MULAI") { dialog, _ ->
            dialog.dismiss()
            // Jalankan pengecekan izin perangkat keras
            if (checkAndRequestPermissions()) {
                Toast.makeText(context, "Membuka Layar Kamera AI...", Toast.LENGTH_SHORT).show()
                // TODO: Di sini nanti kita panggil Activity baru yang layarnya landscape!
                Toast.makeText(context, "Membuka Layar Kamera AI...", Toast.LENGTH_SHORT).show()
// Perintah peluncuran halaman pemindaian horizontal baru
                val intent = android.content.Intent(activity, DetectionActivity::class.java)
                startActivity(intent)
            }
        }

        // Jika pengguna membatalkan dan klik "BATAL"
        builder.setNegativeButton("BATAL") { dialog, _ ->
            dialog.dismiss()
        }

        // Memunculkan dialog ke layar
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