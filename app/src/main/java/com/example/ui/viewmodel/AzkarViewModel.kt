package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoorSakinahApp
import com.example.data.model.AzkarCategory
import com.example.data.model.ZikrItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AzkarViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NoorSakinahApp
    private val azkarRepo = app.azkarRepository
    private val prefsRepo = app.preferencesRepository

    val builtInCategories: List<AzkarCategory> = azkarRepo.getBuiltInCategories()

    val customAzkar: StateFlow<List<ZikrItem>> = azkarRepo.customAzkarFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategoryId = MutableStateFlow("morning")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    fun getCategory(categoryId: String): AzkarCategory {
        return if (categoryId == "custom") {
            AzkarCategory(
                id = "custom",
                title = "أذكاري",
                subtitle = "الأدعية والأذكار الخاصة بك",
                icon = "edit_note",
                items = customAzkar.value
            )
        } else {
            azkarRepo.getCategoryById(categoryId) ?: builtInCategories.first()
        }
    }

    fun setShowTashkeel(show: Boolean) {
        viewModelScope.launch { prefsRepo.setShowTashkeel(show) }
    }

    fun setShowFadl(show: Boolean) {
        viewModelScope.launch { prefsRepo.setShowFadl(show) }
    }

    fun addCustomZikr(text: String, repeatCount: Int, fadl: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            azkarRepo.addCustomZikr(text, repeatCount, fadl)
        }
    }

    fun updateCustomZikr(id: Long, text: String, repeatCount: Int, fadl: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            azkarRepo.updateCustomZikr(id, text, repeatCount, fadl)
        }
    }

    fun deleteCustomZikr(id: Long) {
        viewModelScope.launch {
            azkarRepo.deleteCustomZikr(id)
        }
    }

    fun markCategoryCompleted(categoryId: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        viewModelScope.launch {
            prefsRepo.markAzkarCategoryCompleted(categoryId, today)
        }
    }
}
