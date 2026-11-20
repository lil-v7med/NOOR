package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.NoorSakinahApp
import com.example.audio.ShaarawyPlayerState
import com.example.data.model.ShaarawyLectureItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShaarawyViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NoorSakinahApp
    private val repo = app.shaarawyRepository
    private val player = app.shaarawyAudioPlayer

    val lectures: List<ShaarawyLectureItem> = repo.loadLectures()

    private val _selectedCategory = MutableStateFlow("الكل")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val playerState: StateFlow<ShaarawyPlayerState> = player.state

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun getCategories(): List<String> {
        val cats = lectures.map { it.category }.distinct()
        return listOf("الكل") + cats
    }

    fun getFilteredLectures(): List<ShaarawyLectureItem> {
        val cat = _selectedCategory.value
        return if (cat == "الكل") lectures else lectures.filter { it.category == cat }
    }

    fun playLecture(lecture: ShaarawyLectureItem) {
        player.playLecture(lecture.title, lecture.source, lecture.audioUrl)
    }

    fun togglePlayPause() = player.togglePlayPause()

    fun stopAudio() = player.stop()
}
