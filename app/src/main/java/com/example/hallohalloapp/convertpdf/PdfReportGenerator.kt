package com.example.hallohalloapp.convertpdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.location.Geocoder
import com.example.hallohalloapp.model.PotholeRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Membuat file PDF dari daftar PotholeRecord yang dipilih user di halaman Laporan.
 * 1 halaman per lubang: foto, tingkat keyakinan, alamat, waktu.
 *
 * PENTING: fungsi ini melakukan reverse-geocoding (perlu jaringan) dan menulis file,
 * jadi HARUS dipanggil dari background thread, bukan dari main/UI thread.
 */
object PdfReportGenerator {

    // Ukuran halaman A4 dalam satuan point (72 point = 1 inch)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40

    fun generate(context: Context, records: List<PotholeRecord>): File {
        val document = PdfDocument()
        val geocoder = Geocoder(context, Locale("in", "ID"))
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }
        val labelPaint = Paint().apply {
            textSize = 12f
        }
        val footerPaint = Paint().apply {
            textSize = 9f
            color = 0xFF9E9E9E.toInt()
        }

        records.forEachIndexed { index, record ->
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            var y = MARGIN.toFloat() + 20f

            canvas.drawText("Laporan Lubang Jalan #${index + 1}", MARGIN.toFloat(), y, titlePaint)
            y += 30f

            val bitmap = BitmapFactory.decodeFile(record.imagePath)
            if (bitmap != null) {
                val maxWidth = PAGE_WIDTH - (MARGIN * 2)
                val scale = maxWidth.toFloat() / bitmap.width.toFloat()
                val drawWidth = maxWidth
                val drawHeight = (bitmap.height * scale).toInt()
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, drawWidth, drawHeight, true)
                canvas.drawBitmap(scaledBitmap, MARGIN.toFloat(), y, null)
                y += drawHeight + 24f
            }

            val confidencePercent = (record.confidence * 100).toInt()
            canvas.drawText("Tingkat Keyakinan AI: $confidencePercent%", MARGIN.toFloat(), y, labelPaint)
            y += 20f

            val address = try {
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocation(record.latitude, record.longitude, 1)
                if (!results.isNullOrEmpty()) results[0].getAddressLine(0) else null
            } catch (e: Exception) {
                null
            } ?: "${String.format("%.5f", record.latitude)}, ${String.format("%.5f", record.longitude)}"

            canvas.drawText("Alamat: $address", MARGIN.toFloat(), y, labelPaint)
            y += 20f

            canvas.drawText("Waktu Terdeteksi: ${dateFormat.format(Date(record.timestamp))}", MARGIN.toFloat(), y, labelPaint)

            canvas.drawText(
                "Dibuat otomatis oleh ASPAL - Aplikasi Pintar Pendeteksi Lubang Jalan",
                MARGIN.toFloat(),
                PAGE_HEIGHT - 24f,
                footerPaint
            )

            document.finishPage(page)
        }

        val folder = File(context.getExternalFilesDir(null), "reports")
        if (!folder.exists()) folder.mkdirs()

        val fileName = "Laporan_ASPAL_${System.currentTimeMillis()}.pdf"
        val outputFile = File(folder, fileName)

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return outputFile
    }
}