package com.example.hallohalloapp.detection.storage

import android.content.Context
import com.example.hallohalloapp.model.PotholeRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object PotholeStorage {

    private const val FILE_NAME = "saved_potholes.json"

    fun saveAll(context: Context, records: List<PotholeRecord>) {
        val jsonArray = JSONArray()
        for (record in records) {
            val obj = JSONObject()
            obj.put("imagePath", record.imagePath)
            obj.put("latitude", record.latitude)
            obj.put("longitude", record.longitude)
            obj.put("confidence", record.confidence)
            obj.put("timestamp", record.timestamp)
            jsonArray.put(obj)
        }
        val file = File(context.filesDir, FILE_NAME)
        file.writeText(jsonArray.toString())
    }

    fun loadAll(context: Context): List<PotholeRecord> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        val jsonArray = JSONArray(file.readText())
        val result = mutableListOf<PotholeRecord>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            result.add(
                PotholeRecord(
                    imagePath = obj.getString("imagePath"),
                    latitude = obj.getDouble("latitude"),
                    longitude = obj.getDouble("longitude"),
                    confidence = obj.getDouble("confidence").toFloat(),
                    timestamp = obj.getLong("timestamp")
                )
            )
        }
        return result
    }

    fun addRecords(context: Context, newRecords: List<PotholeRecord>) {
        val existing = loadAll(context).toMutableList()
        existing.addAll(newRecords)
        saveAll(context, existing)
    }

    fun deleteRecord(context: Context, record: PotholeRecord) {
        val existing = loadAll(context).toMutableList()
        existing.removeAll { it.imagePath == record.imagePath && it.timestamp == record.timestamp }
        saveAll(context, existing)

        try {
            File(record.imagePath).delete()
        } catch (e: Exception) {
        }
    }
}