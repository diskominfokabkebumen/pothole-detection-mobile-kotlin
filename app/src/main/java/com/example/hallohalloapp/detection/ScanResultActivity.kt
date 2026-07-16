package com.example.hallohalloapp.detection

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hallohalloapp.R

class ScanResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_result)

        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        val records = intent.getSerializableExtra("pothole_records") as? ArrayList<PotholeRecord> ?: arrayListOf()

        val tvSummary = findViewById<TextView>(R.id.tvScanSummary)
        tvSummary.text = "Jumlah Gambar: ${records.size}\nTotal Lubang Jalan: ${records.size}"

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerPotholeResult)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = PotholeResultAdapter(records) { record ->
            Toast.makeText(
                this,
                "Lokasi: ${record.latitude}, ${record.longitude}",
                Toast.LENGTH_LONG
            ).show()
        }

        findViewById<Button>(R.id.btnSelesaiScan).setOnClickListener {
            finish()
        }
    }
}