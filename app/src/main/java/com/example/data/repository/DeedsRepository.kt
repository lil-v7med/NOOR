package com.example.data.repository

import android.content.Context
import com.example.data.model.GoodDeedItem
import org.json.JSONArray

class DeedsRepository(private val context: Context) {
    fun loadDeeds(): List<GoodDeedItem> {
        return try {
            val json = context.assets.open("good_deeds.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                GoodDeedItem(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    description = obj.getString("description"),
                    category = obj.getString("category")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
