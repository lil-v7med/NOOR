package com.example.data.repository

import android.content.Context
import com.example.data.model.ShaarawyLectureItem
import org.json.JSONArray

class ShaarawyRepository(private val context: Context) {
    fun loadLectures(): List<ShaarawyLectureItem> {
        return try {
            val json = context.assets.open("shaarawy_lectures.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                ShaarawyLectureItem(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    category = obj.getString("category"),
                    surahName = obj.getString("surahName"),
                    duration = obj.getString("duration"),
                    speaker = obj.getString("speaker"),
                    source = obj.getString("source"),
                    audioUrl = obj.getString("audioUrl")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
