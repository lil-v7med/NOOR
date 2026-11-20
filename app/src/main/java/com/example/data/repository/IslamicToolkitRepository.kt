package com.example.data.repository

import android.content.Context
import com.example.util.ArabicTextUtils
import org.json.JSONArray

data class AsmaulHusnaItem(
    val number: Int,
    val name: String,
    val meaning: String
)

data class DuaEntry(
    val id: String,
    val category: String,
    val title: String,
    val text: String,
    val note: String,
    val source: String,
    val sourceUrl: String
)

class IslamicToolkitRepository(private val context: Context) {

    private val asmaulHusnaCache: List<AsmaulHusnaItem> by lazy { loadAsmaulHusna() }
    private val duaCache: List<DuaEntry> by lazy { loadDuas() }

    fun getAllAsmaulHusna(): List<AsmaulHusnaItem> = asmaulHusnaCache

    fun searchAsmaulHusna(query: String): List<AsmaulHusnaItem> {
        val norm = ArabicTextUtils.normalizeForSearch(query)
        if (norm.isBlank()) return asmaulHusnaCache
        return asmaulHusnaCache.filter {
            ArabicTextUtils.normalizeForSearch(it.name).contains(norm) ||
                ArabicTextUtils.normalizeForSearch(it.meaning).contains(norm)
        }
    }

    fun getAllDuas(): List<DuaEntry> = duaCache

    fun getDuaCategories(): List<String> = listOf("الكل") + duaCache.map { it.category }.distinct()

    fun filterDuas(query: String, category: String): List<DuaEntry> {
        val norm = ArabicTextUtils.normalizeForSearch(query)
        return duaCache.filter { item ->
            val catMatch = category == "الكل" || item.category == category
            if (!catMatch) return@filter false
            if (norm.isBlank()) return@filter true
            ArabicTextUtils.normalizeForSearch(item.title).contains(norm) ||
                ArabicTextUtils.normalizeForSearch(item.text).contains(norm) ||
                ArabicTextUtils.normalizeForSearch(item.note).contains(norm)
        }
    }

    private fun loadAsmaulHusna(): List<AsmaulHusnaItem> {
        val json = context.assets.open("asmaul_husna.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            AsmaulHusnaItem(
                number = obj.getInt("number"),
                name = obj.getString("name"),
                meaning = obj.getString("meaning")
            )
        }
    }

    private fun loadDuas(): List<DuaEntry> {
        val json = context.assets.open("dua_collection.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            DuaEntry(
                id = obj.getString("id"),
                category = obj.getString("category"),
                title = obj.getString("title"),
                text = obj.getString("text"),
                note = obj.getString("note"),
                source = obj.getString("source"),
                sourceUrl = obj.getString("sourceUrl")
            )
        }
    }
}
