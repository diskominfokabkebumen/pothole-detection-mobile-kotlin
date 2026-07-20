package com.example.hallohalloapp.detection

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.hallohalloapp.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PotholeDetailDialogHelper {

    fun show(fragment: Fragment, record: PotholeRecord, onDeleted: () -> Unit) {
        val context = fragment.requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_pothole_detail, null)

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

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        btnDelete.setOnClickListener {
            PotholeStorage.deleteRecord(context, record)
            dialog.dismiss()
            onDeleted()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        resolveAddress(fragment, record, tvLocation)
    }

    private fun resolveAddress(fragment: Fragment, record: PotholeRecord, tvLocation: TextView) {
        Thread {
            if (!fragment.isAdded) return@Thread

            val addressText = try {
                val geocoder = Geocoder(fragment.requireContext(), Locale("in", "ID"))
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

            if (fragment.isAdded) {
                fragment.activity?.runOnUiThread {
                    if (fragment.isAdded) {
                        tvLocation.text = addressText
                            ?: "Lokasi: ${String.format("%.5f", record.latitude)}, ${String.format("%.5f", record.longitude)}"
                    }
                }
            }
        }.start()
    }
}