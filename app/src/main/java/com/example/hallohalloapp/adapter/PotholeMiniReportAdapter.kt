package com.example.hallohalloapp.adapter

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hallohalloapp.R
import com.example.hallohalloapp.model.PotholeRecord

/**
 * Versi ringkas dari PotholeReportAdapter, khusus dipakai di rvRecentReports
 * pada Beranda (cuma nampilin beberapa laporan terbaru, kartu lebih mini).
 */
class PotholeMiniReportAdapter(
    private val items: List<PotholeRecord>,
    private val onItemClick: (PotholeRecord) -> Unit
) : RecyclerView.Adapter<PotholeMiniReportAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageThumbnail: ImageView = view.findViewById(R.id.imgHomeReportThumbnail)
        val tvIndex: TextView = view.findViewById(R.id.tvHomeReportIndex)
        val tvConfidence: TextView = view.findViewById(R.id.tvHomeReportConfidence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_home_report_mini, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val confidencePercent = (item.confidence * 100).toInt()

        holder.tvIndex.text = "Lubang jalan"
        holder.tvConfidence.text = "$confidencePercent% yakin"

        val confidenceColor = when {
            confidencePercent >= 70 -> Color.parseColor("#2E7D32")
            confidencePercent >= 40 -> Color.parseColor("#F9A825")
            else -> Color.parseColor("#C62828")
        }
        holder.tvConfidence.setTextColor(confidenceColor)

        val bitmap = BitmapFactory.decodeFile(item.imagePath)
        if (bitmap != null) {
            holder.imageThumbnail.setImageBitmap(bitmap)
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}