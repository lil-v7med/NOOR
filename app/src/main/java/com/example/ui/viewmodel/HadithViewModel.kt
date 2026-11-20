package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoorSakinahApp
import com.example.data.model.HadithItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NawawiSyncUiState(
    val isSyncing: Boolean = false,
    val isSynced: Boolean = false,
    val errorMessage: String? = null
)

class HadithViewModel(application: Application) : AndroidViewModel(application) {
    private val hadithRepo = (application as NoorSakinahApp).hadithRepository

    val categories: List<String> = hadithRepo.getCategories()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("الكل")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = hadithRepo.observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _isSyncing = MutableStateFlow(false)
    private val _syncError = MutableStateFlow<String?>(null)

    val nawawiSyncState: StateFlow<NawawiSyncUiState> = combine(
        _isSyncing,
        hadithRepo.observeIsNawawiSynced(),
        _syncError
    ) { syncing, synced, err ->
        NawawiSyncUiState(
            isSyncing = syncing,
            isSynced = synced,
            errorMessage = err
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NawawiSyncUiState())

    val hadiths: StateFlow<List<HadithItem>> = combine(
        hadithRepo.observeAllHadiths(),
        _searchQuery,
        _selectedCategory,
        favoriteIds
    ) { allItems, query, category, favIds ->
        hadithRepo.filterHadiths(
            query = query,
            category = category,
            favoriteIds = favIds,
            sourceItems = allItems
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), hadithRepo.getAllHadiths())

    init {
        viewModelScope.launch {
            hadithRepo.ensureNawawiSeeded()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleFavorite(hadithId: String) {
        viewModelScope.launch {
            hadithRepo.toggleFavorite(hadithId)
        }
    }

    fun syncNawawiFullTexts() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            val ok = hadithRepo.syncNawawiFullTexts()
            _isSyncing.value = false
            if (!ok) {
                _syncError.value = "تعذر تحميل النصوص الكاملة — تأكد من الاتصال بالإنترنت أو افتح رابط Sunnah.com"
            }
        }
    }
}
