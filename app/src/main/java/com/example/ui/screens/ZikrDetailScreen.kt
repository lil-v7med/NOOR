package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserSettings
import com.example.data.model.AzkarCategory
import com.example.ui.components.EmeraldPillBadge
import com.example.ui.components.NoorGoldCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorTheme
import com.example.util.ArabicTextUtils
import com.example.util.ExternalUrlHelper
import com.example.util.HapticHelper

@Composable
fun ZikrDetailScreen(
    category: AzkarCategory,
    settings: UserSettings,
    onBack: () -> Unit,
    onToggleTashkeel: (Boolean) -> Unit,
    onToggleFadl: (Boolean) -> Unit,
    onCategoryCompleted: (String) -> Unit
) {
    val context = LocalContext.current
    val items = category.items
    var currentIndex by remember(category.id) { mutableIntStateOf(0) }
    var isFinished by remember(category.id) { mutableStateOf(false) }

    val remainingMap = remember(category.id, items.size) {
        mutableStateMapOf<Int, Int>().apply {
            items.forEachIndexed { index, zikr -> put(index, zikr.count) }
        }
    }

    val currentItem = items.getOrNull(currentIndex)
    val remaining = remainingMap[currentIndex] ?: (currentItem?.count ?: 1)
    val totalForCard = (currentItem?.count ?: 1).coerceAtLeast(1)

    Scaffold(
        topBar = {
            Surface(
                color = NoorTheme.colors.headerForest,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("zikr_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "رجوع",
                                    tint = NoorTheme.colors.onHeaderForest
                                )
                            }
                            Column {
                                Text(
                                    text = category.title,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NoorTheme.colors.onHeaderForest
                                )
                                if (items.isNotEmpty() && !isFinished) {
                                    Text(
                                        text = "${ArabicTextUtils.toEasternArabicNumerals(currentIndex + 1)} من ${ArabicTextUtils.toEasternArabicNumerals(items.size)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NoorTheme.colors.goldAccent
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = settings.showTashkeel,
                                onClick = { onToggleTashkeel(!settings.showTashkeel) },
                                label = { Text("التشكيل", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("zikr_tashkeel_toggle")
                            )
                            FilterChip(
                                selected = settings.showFadl,
                                onClick = { onToggleFadl(!settings.showFadl) },
                                label = { Text("الفضل", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("zikr_fadl_toggle")
                            )
                        }
                    }

                    if (items.isNotEmpty()) {
                        val overallProgress = if (isFinished) 1f else (currentIndex.toFloat() / items.size.toFloat())
                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = NoorTheme.colors.goldAccent,
                            trackColor = NoorTheme.colors.headerForest
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            if (items.isEmpty()) {
                NoorGoldCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("لا توجد أذكار في هذا القسم بعد", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = onBack) { Text("العودة") }
                    }
                }
            } else if (isFinished) {
                NoorGoldCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .testTag("zikr_completed_card")
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
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "تقبل الله ذكركم",
                            fontFamily = AmiriFontFamily,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "أتممت قراءة ${category.title} كاملة بفضل الله.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    items.forEachIndexed { idx, z -> remainingMap[idx] = z.count }
                                    currentIndex = 0
                                    isFinished = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("إعادة")
                            }
                            Button(
                                onClick = onBack,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("الأذكار")
                            }
                        }
                    }
                }
            } else if (currentItem != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    AnimatedContent(
                        targetState = currentIndex,
                        modifier = Modifier.weight(1f),
                        label = "zikr_card_transition"
                    ) { idx ->
                        val item = items[idx]
                        val displayText = if (settings.showTashkeel) {
                            item.text
                        } else {
                            ArabicTextUtils.stripTashkeel(item.text)
                        }
                        val cardRemaining = remainingMap[idx] ?: item.count

                        NoorGoldCard(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("zikr_interactive_card"),
                            onClick = {
                                if (cardRemaining > 1) {
                                    remainingMap[idx] = cardRemaining - 1
                                    HapticHelper.tapPulse(context)
                                } else {
                                    remainingMap[idx] = 0
                                    HapticHelper.completionPulse(context)
                                    if (idx < items.lastIndex) {
                                        currentIndex = idx + 1
                                    } else {
                                        onCategoryCompleted(category.id)
                                        isFinished = true
                                    }
                                }
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    EmeraldPillBadge(
                                        text = "التكرار: ${ArabicTextUtils.toEasternArabicNumerals(item.count)}"
                                    )
                                    Text(
                                        text = "اضغط على البطاقة للعد",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 14.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = displayText,
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 24.sp,
                                        lineHeight = 46.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    AnimatedVisibility(visible = settings.showFadl && item.fadl.isNotBlank()) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 18.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            color = NoorTheme.colors.badgeEmeraldBg,
                                            border = BorderStroke(1.dp, NoorTheme.colors.goldBorder)
                                        ) {
                                            Text(
                                                text = "فضل الذكر: ${item.fadl}",
                                                modifier = Modifier.padding(12.dp),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    if (item.source.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            onClick = {
                                                if (item.sourceUrl.isNotBlank()) {
                                                    ExternalUrlHelper.openVerificationUrl(context, item.sourceUrl)
                                                }
                                            },
                                            enabled = item.sourceUrl.isNotBlank(),
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                            border = BorderStroke(1.dp, NoorTheme.colors.goldBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("zikr_source_link_${idx}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Verified,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Text(
                                                        text = "المصدر: ${item.source}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                if (item.sourceUrl.isNotBlank()) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Text(
                                                            text = "تحقق",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = NoorTheme.colors.goldAccent
                                                        )
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                            contentDescription = "فتح المصدر للتحقق",
                                                            tint = NoorTheme.colors.goldAccent,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Circular Countdown Ring
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val progress by animateFloatAsState(
                                        targetValue = cardRemaining.toFloat() / totalForCard.toFloat(),
                                        animationSpec = tween(220),
                                        label = "zikr_ring"
                                    )
                                    val gold = NoorTheme.colors.goldAccent
                                    val trackColor = gold.copy(alpha = 0.18f)

                                    Box(
                                        modifier = Modifier.size(84.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawArc(
                                                color = trackColor,
                                                startAngle = 0f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                            drawArc(
                                                color = gold,
                                                startAngle = -90f,
                                                sweepAngle = 360f * progress,
                                                useCenter = false,
                                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                        }
                                        Text(
                                            text = ArabicTextUtils.toEasternArabicNumerals(cardRemaining),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Previous / Next Navigation Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (currentIndex > 0) currentIndex-- },
                            enabled = currentIndex > 0,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("السابق")
                        }

                        Text(
                            text = "متبقي ${ArabicTextUtils.toEasternArabicNumerals(remaining)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedButton(
                            onClick = {
                                if (currentIndex < items.lastIndex) {
                                    currentIndex++
                                } else {
                                    onCategoryCompleted(category.id)
                                    isFinished = true
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(if (currentIndex < items.lastIndex) "التالي" else "إنهاء")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
