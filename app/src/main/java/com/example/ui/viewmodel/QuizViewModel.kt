package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.NoorSakinahApp
import com.example.data.model.QuizQuestionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class QuizViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NoorSakinahApp
    private val quizRepo = app.quizRepository

    private val _questions = MutableStateFlow<List<QuizQuestionItem>>(emptyList())
    val questions: StateFlow<List<QuizQuestionItem>> = _questions.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _selectedAnswerIndex = MutableStateFlow<Int?>(null)
    val selectedAnswerIndex: StateFlow<Int?> = _selectedAnswerIndex.asStateFlow()

    private val _isAnswerSubmitted = MutableStateFlow(false)
    val isAnswerSubmitted: StateFlow<Boolean> = _isAnswerSubmitted.asStateFlow()

    private val _quizCompleted = MutableStateFlow(false)
    val quizCompleted: StateFlow<Boolean> = _quizCompleted.asStateFlow()

    init {
        loadQuiz()
    }

    fun loadQuiz() {
        val list = quizRepo.loadQuestions().shuffled()
        _questions.value = list
        _currentIndex.value = 0
        _score.value = 0
        _selectedAnswerIndex.value = null
        _isAnswerSubmitted.value = false
        _quizCompleted.value = false
    }

    fun selectAnswer(index: Int) {
        if (_isAnswerSubmitted.value) return
        _selectedAnswerIndex.value = index
    }

    fun submitAnswer() {
        val sel = _selectedAnswerIndex.value ?: return
        val qList = _questions.value
        val idx = _currentIndex.value
        if (idx !in qList.indices) return

        _isAnswerSubmitted.value = true
        if (sel == qList[idx].correctIndex) {
            _score.update { it + 1 }
        }
    }

    fun nextQuestion() {
        val qList = _questions.value
        val idx = _currentIndex.value
        if (idx + 1 < qList.size) {
            _currentIndex.update { it + 1 }
            _selectedAnswerIndex.value = null
            _isAnswerSubmitted.value = false
        } else {
            _quizCompleted.value = true
        }
    }
}
