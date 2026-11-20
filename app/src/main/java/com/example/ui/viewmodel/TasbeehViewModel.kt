package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoorSakinahApp
import kotlinx.coroutines.launch

class TasbeehViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NoorSakinahApp
    private val prefsRepo = app.preferencesRepository

    val presetPhrases = listOf(
        "سبحان الله",
        "الحمد لله",
        "الله أكبر",
        "لا إله إلا الله",
        "أستغفر الله العظيم",
        "سبحان الله وبحمده",
        "لا حول ولا قوة إلا بالله",
        "اللهم صلِّ على محمد"
    )

    val targetPresets = listOf(33, 100, 1000, 0) // 0 = مفتوح (Open)

    fun increment() {
        viewModelScope.launch {
            prefsRepo.incrementTasbeeh()
        }
    }

    fun resetCycle() {
        viewModelScope.launch {
            prefsRepo.resetTasbeehCycle()
        }
    }

    fun selectTarget(target: Int) {
        viewModelScope.launch {
            prefsRepo.setTasbeehTarget(target)
        }
    }

    fun selectPhrase(phrase: String) {
        if (phrase.isBlank()) return
        viewModelScope.launch {
            prefsRepo.setTasbeehPhrase(phrase.trim())
        }
    }

    fun toggleHaptics(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setTasbeehHaptics(enabled)
        }
    }
}
