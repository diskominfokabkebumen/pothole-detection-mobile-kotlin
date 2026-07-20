package com.example.hallohalloapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hallohalloapp.R
import com.example.hallohalloapp.detection.PotholeDetailDialogHelper
import com.example.hallohalloapp.detection.PotholeReportAdapter
import com.example.hallohalloapp.detection.PotholeStorage

class ReportFragment : Fragment() {

    private var recyclerView: RecyclerView? = null
    private var tvEmptyReport: TextView? = null
    private var tvReportSummary: TextView? = null

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

        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

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

        recyclerView?.adapter = PotholeReportAdapter(records) { record ->
            PotholeDetailDialogHelper.show(this, record) {
                loadReportList()
            }
        }
    }
}