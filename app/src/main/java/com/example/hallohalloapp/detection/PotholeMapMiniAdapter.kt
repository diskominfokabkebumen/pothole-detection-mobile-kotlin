package com.example.hallohalloapp.detection

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hallohalloapp.R

/**
 * Adapter untuk daftar horizontal "lubang terbaru" di bagian bawah halaman Peta.
 * Tap 1 item akan memindahkan kamera peta ke titik lokasi lubang tersebut.
 */
class PotholeMapMiniAdapter(
    private val items: List<PotholeRecord>,
    private val onItemClick: (PotholeRecord) -> Unit
) : RecyclerView.Adapter<PotholeMapMiniAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageThumbnail: ImageView = view.findViewById(R.id.imgMapMiniThumbnail)
        val tvConfidence: TextView = view.findViewById(R.id.tvMapMiniConfidence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_map_mini, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val confidencePercent = (item.confidence * 100).toInt()

        holder.tvConfidence.text = "$confidencePercent%"

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