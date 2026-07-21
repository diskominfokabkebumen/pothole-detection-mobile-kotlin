package com.example.hallohalloapp.adapter

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hallohalloapp.R
import com.example.hallohalloapp.model.PotholeRecord

class PotholeReportAdapter(
    private val items: List<PotholeRecord>,
    private val onItemClick: (PotholeRecord) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<PotholeReportAdapter.ViewHolder>() {

    private var selectionMode = false
    private val selectedTimestamps = mutableSetOf<Long>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageThumbnail: ImageView = view.findViewById(R.id.imgReportThumbnail)
        val tvIndex: TextView = view.findViewById(R.id.tvReportIndex)
        val tvConfidence: TextView = view.findViewById(R.id.tvReportConfidence)
        val cbSelect: CheckBox = view.findViewById(R.id.cbReportSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val confidencePercent = (item.confidence * 100).toInt()

        holder.tvIndex.text = "Lubang ${position + 1}"
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

        holder.cbSelect.visibility = if (selectionMode) View.VISIBLE else View.GONE
        holder.cbSelect.isChecked = selectedTimestamps.contains(item.timestamp)

        holder.itemView.setOnClickListener {
            if (selectionMode) {
                toggleSelection(item)
                notifyItemChanged(position)
            } else {
                onItemClick(item)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!selectionMode) {
                setSelectionMode(true)
                toggleSelection(item)
            }
            true
        }
    }

    override fun getItemCount(): Int = items.size

    private fun toggleSelection(item: PotholeRecord) {
        if (selectedTimestamps.contains(item.timestamp)) {
            selectedTimestamps.remove(item.timestamp)
        } else {
            selectedTimestamps.add(item.timestamp)
        }
        onSelectionChanged(selectedTimestamps.size)
    }

    fun setSelectionMode(enabled: Boolean) {
        selectionMode = enabled
        if (!enabled) {
            selectedTimestamps.clear()
        }
        onSelectionChanged(selectedTimestamps.size)
        notifyDataSetChanged()
    }

    fun isSelectionMode(): Boolean = selectionMode

    fun getSelectedRecords(): List<PotholeRecord> {
        return items.filter { selectedTimestamps.contains(it.timestamp) }
    }
}