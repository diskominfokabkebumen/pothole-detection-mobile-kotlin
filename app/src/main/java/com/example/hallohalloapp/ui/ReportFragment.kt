package com.example.hallohalloapp.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hallohalloapp.R
import com.example.hallohalloapp.adapter.PotholeReportAdapter
import com.example.hallohalloapp.convertpdf.PdfReportGenerator
import com.example.hallohalloapp.detection.PotholeDetailDialogHelper
import com.example.hallohalloapp.detection.storage.PotholeStorage
import com.example.hallohalloapp.model.PotholeRecord
import com.google.android.material.button.MaterialButton
import java.io.File

class ReportFragment : Fragment() {

    private var recyclerView: RecyclerView? = null
    private var tvEmptyReport: TextView? = null
    private var tvReportSummary: TextView? = null
    private var btnToggleSelect: MaterialButton? = null
    private var layoutSelectionBar: View? = null
    private var tvSelectionCount: TextView? = null
    private var btnBuatPdf: MaterialButton? = null

    private var reportAdapter: PotholeReportAdapter? = null
    private var progressDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerReport)
        tvEmptyReport = view.findViewById(R.id.tvEmptyReport)
        tvReportSummary = view.findViewById(R.id.tvReportSummary)
        btnToggleSelect = view.findViewById(R.id.btnToggleSelect)
        layoutSelectionBar = view.findViewById(R.id.layoutSelectionBar)
        tvSelectionCount = view.findViewById(R.id.tvSelectionCount)
        btnBuatPdf = view.findViewById(R.id.btnBuatPdf)

        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

        btnToggleSelect?.setOnClickListener {
            val adapter = reportAdapter ?: return@setOnClickListener
            val enabling = !adapter.isSelectionMode()
            adapter.setSelectionMode(enabling)
            btnToggleSelect?.text = if (enabling) "BATAL" else "PILIH"
            layoutSelectionBar?.visibility = if (enabling) View.VISIBLE else View.GONE
        }

        btnBuatPdf?.setOnClickListener {
            val selected = reportAdapter?.getSelectedRecords().orEmpty()
            if (selected.isEmpty()) {
                Toast.makeText(context, "Pilih minimal 1 lubang dulu", Toast.LENGTH_SHORT).show()
            } else {
                generatePdf(selected)
            }
        }

        loadReportList()
    }

    override fun onResume() {
        super.onResume()
        loadReportList()
    }

    private fun loadReportList() {
        val records = PotholeStorage.loadAll(requireContext())

        tvReportSummary?.text = "${records.size} Lubang Tercatat"
        tvEmptyReport?.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        recyclerView?.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE

        reportAdapter = PotholeReportAdapter(
            items = records,
            onItemClick = { record ->
                PotholeDetailDialogHelper.show(this, record) {
                    loadReportList()
                }
            },
            onSelectionChanged = { count ->
                tvSelectionCount?.text = "$count dipilih"
            }
        )
        recyclerView?.adapter = reportAdapter
    }

    private fun generatePdf(records: List<PotholeRecord>) {
        showProgressDialog()

        Thread {
            val resultFile: File? = try {
                PdfReportGenerator.generate(requireContext(), records)
            } catch (e: Exception) {
                null
            }

            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                progressDialog?.dismiss()

                if (resultFile != null) {
                    showPdfReadyDialog(resultFile)
                } else {
                    Toast.makeText(context, "Gagal membuat PDF, coba lagi", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun showProgressDialog() {
        progressDialog = AlertDialog.Builder(requireContext())
            .setMessage("Membuat PDF...")
            .setCancelable(false)
            .create()
        progressDialog?.show()
    }

    private fun showPdfReadyDialog(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        AlertDialog.Builder(requireContext())
            .setTitle("PDF Berhasil Dibuat")
            .setMessage("Laporan sudah tersimpan di HP kamu.")
            .setPositiveButton("Bagikan") { dialog, _ ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Bagikan laporan PDF"))
                dialog.dismiss()
            }
            .setNeutralButton("Buka") { dialog, _ ->
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    startActivity(viewIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Tidak ada aplikasi pembuka PDF di HP ini", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Tutup", null)
            .show()

        // Keluar dari mode pilih setelah PDF berhasil dibuat
        reportAdapter?.setSelectionMode(false)
        btnToggleSelect?.text = "PILIH"
        layoutSelectionBar?.visibility = View.GONE
    }
}