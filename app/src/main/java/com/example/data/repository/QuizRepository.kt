package com.example.data.repository

import android.content.Context
import com.example.data.model.QuizQuestionItem
import org.json.JSONArray

class QuizRepository(private val context: Context) {
    fun loadQuestions(): List<QuizQuestionItem> {
        return try {
            val json = context.assets.open("quiz_questions.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                val optArr = obj.getJSONArray("options")
                val options = List(optArr.length()) { optArr.getString(it) }
                QuizQuestionItem(
                    id = obj.getString("id"),
                    category = obj.getString("category"),
                    question = obj.getString("question"),
                    options = options,
                    correctIndex = obj.getInt("correctIndex"),
                    explanation = obj.getString("explanation")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
