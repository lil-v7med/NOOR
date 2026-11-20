package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizQuestionItem
import com.example.ui.components.EmeraldOrnamentalHeader
import com.example.ui.components.EmeraldPillBadge
import com.example.ui.components.NoorGoldCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorTheme
import com.example.util.ArabicTextUtils

@Composable
fun QuizScreen(
    questions: List<QuizQuestionItem>,
    currentIndex: Int,
    score: Int,
    selectedAnswerIndex: Int?,
    isAnswerSubmitted: Boolean,
    quizCompleted: Boolean,
    onBack: () -> Unit,
    onSelectAnswer: (Int) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onRestartQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        EmeraldOrnamentalHeader {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("quiz_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = NoorTheme.colors.onHeaderForest
                        )
                    }
                    Column {
                        Text(
                            text = "اختبار المعرفة الإسلامية",
                            fontFamily = AmiriFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NoorTheme.colors.onHeaderForest
                        )
                        Text(
                            text = "سيرة، عقيدة، وآداب إسلامية",
                            style = MaterialTheme.typography.bodySmall,
                            color = NoorTheme.colors.goldAccent
                        )
                    }
                }
                if (!quizCompleted && questions.isNotEmpty()) {
                    EmeraldPillBadge(
                        text = "السؤال ${ArabicTextUtils.toEasternArabicNumerals(currentIndex + 1)} / ${ArabicTextUtils.toEasternArabicNumerals(questions.size)}"
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (questions.isEmpty()) {
                item {
                    NoorGoldCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "جارٍ تحميل الأسئلة...",
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (quizCompleted) {
                item {
                    NoorGoldCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_result_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "أحسنت! لقد أكملت الاختبار بنجاح",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "نتيجتك النهائية: ${ArabicTextUtils.toEasternArabicNumerals(score)} من ${ArabicTextUtils.toEasternArabicNumerals(questions.size)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = NoorTheme.colors.goldAccent
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onRestartQuiz,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("restart_quiz_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("إعادة الاختبار من جديد")
                            }
                        }
                    }
                }
            } else {
                val q = questions[currentIndex]
                item {
                    NoorGoldCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_question_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EmeraldPillBadge(text = q.category)
                                Icon(
                                    imageVector = Icons.Filled.HelpOutline,
                                    contentDescription = null,
                                    tint = NoorTheme.colors.goldAccent
                                )
                            }
                            Text(
                                text = q.question,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 32.sp
                            )
                        }
                    }
                }

                items(q.options.size) { optIdx ->
                    val isSelected = selectedAnswerIndex == optIdx
                    val isCorrect = optIdx == q.correctIndex
                    val containerColor = when {
                        isAnswerSubmitted && isCorrect -> NoorTheme.colors.badgeEmeraldBg
                        isAnswerSubmitted && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        isSelected -> NoorTheme.colors.goldAccent.copy(alpha = 0.2f)
                        else -> NoorTheme.colors.cardElevated
                    }
                    val borderColor = when {
                        isAnswerSubmitted && isCorrect -> MaterialTheme.colorScheme.primary
                        isSelected -> NoorTheme.colors.goldAccent
                        else -> NoorTheme.colors.goldBorder
                    }

                    NoorGoldCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_option_$optIdx"),
                        onClick = { onSelectAnswer(optIdx) },
                        containerColor = containerColor,
                        borderColor = borderColor
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = q.options[optIdx],
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isAnswerSubmitted && isCorrect) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (isAnswerSubmitted) {
                    item {
                        NoorGoldCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = NoorTheme.colors.goldAccent
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "التوضيح والشرح:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = q.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!isAnswerSubmitted) {
                        Button(
                            onClick = onSubmitAnswer,
                            enabled = selectedAnswerIndex != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_answer_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("تأكيد الإجابة", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        Button(
                            onClick = onNextQuestion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("next_question_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = if (currentIndex + 1 < questions.size) "السؤال التالي" else "عرض النتيجة النهائية",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
