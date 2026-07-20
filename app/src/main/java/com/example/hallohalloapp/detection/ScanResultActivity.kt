package com.example.hallohalloapp.detection

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hallohalloapp.R
import com.example.hallohalloapp.adapter.PotholeResultAdapter
import com.example.hallohalloapp.detection.storage.PotholeStorage
import com.example.hallohalloapp.model.PotholeRecord

class ScanResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_result)

        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        val records = intent.getSerializableExtra("pothole_records") as? ArrayList<PotholeRecord> ?: arrayListOf()

        val tvSummary = findViewById<TextView>(R.id.tvScanSummary)
        tvSummary.text = "${records.size} Foto  •  ${records.size} Lubang Terdeteksi"

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerPotholeResult)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = PotholeResultAdapter(records) { record ->
            val mapIntent = Intent(this, MapDetailActivity::class.java)
            mapIntent.putExtra("latitude", record.latitude)
            mapIntent.putExtra("longitude", record.longitude)
            startActivity(mapIntent)
        }

        findViewById<Button>(R.id.btnSelesaiScan).setOnClickListener {
            val selected = records.filter { it.isSelected }
            PotholeStorage.addRecords(this, selected)
            Toast.makeText(this, "${selected.size} lubang tersimpan ke peta", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}