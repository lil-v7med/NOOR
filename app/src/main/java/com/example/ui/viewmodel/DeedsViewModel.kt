package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.NoorSakinahApp
import com.example.data.model.GoodDeedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeedsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NoorSakinahApp
    private val deedsRepo = app.deedsRepository

    val deeds: List<GoodDeedItem> = deedsRepo.loadDeeds()

    private val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val _completedDeedIds = MutableStateFlow<Set<String>>(emptySet())
    val completedDeedIds: StateFlow<Set<String>> = _completedDeedIds.asStateFlow()

    fun toggleDeed(deedId: String) {
        _completedDeedIds.update { set ->
            if (set.contains(deedId)) set - deedId else set + deedId
        }
    }

    fun resetToday() {
        _completedDeedIds.value = emptySet()
    }
}
