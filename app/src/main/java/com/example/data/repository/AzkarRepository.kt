package com.example.data.repository

import android.content.Context
import com.example.data.local.CustomZikrDao
import com.example.data.local.CustomZikrEntity
import com.example.data.model.AzkarCategory
import com.example.data.model.ZikrItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

class AzkarRepository(
    private val context: Context,
    private val customZikrDao: CustomZikrDao
) {
    private val cachedCategories: List<AzkarCategory> by lazy { loadHisniCategories() }

    fun getBuiltInCategories(): List<AzkarCategory> = cachedCategories

    fun getCategoryById(id: String): AzkarCategory? {
        return cachedCategories.firstOrNull { it.id == id }
    }

    val customAzkarFlow: Flow<List<ZikrItem>> = customZikrDao.getAllCustomAzkar().map { entities ->
        entities.map { entity ->
            ZikrItem(
                id = "custom_${entity.id}",
                text = entity.text,
                count = entity.repeatCount.coerceAtLeast(1),
                fadl = entity.fadl.ifBlank { "ذكر مضاف في أذكاري الخاصة" },
                isCustom = true,
                customDbId = entity.id
            )
        }
    }

    suspend fun addCustomZikr(text: String, repeatCount: Int, fadl: String) {
        customZikrDao.insertCustomZikr(
            CustomZikrEntity(
                text = text.trim(),
                repeatCount = repeatCount.coerceAtLeast(1),
                fadl = fadl.trim()
            )
        )
    }

    suspend fun updateCustomZikr(id: Long, text: String, repeatCount: Int, fadl: String) {
        customZikrDao.updateCustomZikr(
            CustomZikrEntity(
                id = id,
                text = text.trim(),
                repeatCount = repeatCount.coerceAtLeast(1),
                fadl = fadl.trim()
            )
        )
    }

    suspend fun deleteCustomZikr(id: Long) {
        customZikrDao.deleteCustomZikr(id)
    }

    private fun loadHisniCategories(): List<AzkarCategory> {
        val json = context.assets.open("azkar_hisni.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            val itemsArr = obj.getJSONArray("items")
            val items = List(itemsArr.length()) { j ->
                val itemObj = itemsArr.getJSONObject(j)
                ZikrItem(
                    id = itemObj.getString("id"),
                    text = itemObj.getString("text"),
                    count = itemObj.getInt("count"),
                    fadl = itemObj.optString("fadl", ""),
                    source = itemObj.optString("source", ""),
                    sourceUrl = itemObj.optString("sourceUrl", "")
                )
            }
            AzkarCategory(
                id = obj.getString("id"),
                title = obj.getString("title"),
                subtitle = obj.getString("subtitle"),
                icon = obj.getString("icon"),
                items = items
            )
        }
    }
}
