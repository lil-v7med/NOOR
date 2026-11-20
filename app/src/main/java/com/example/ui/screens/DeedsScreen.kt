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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoodDeedItem
import com.example.ui.components.EmeraldOrnamentalHeader
import com.example.ui.components.EmeraldPillBadge
import com.example.ui.components.NoorGoldCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorTheme
import com.example.util.ArabicTextUtils

@Composable
fun DeedsScreen(
    deeds: List<GoodDeedItem>,
    completedIds: Set<String>,
    onBack: () -> Unit,
    onToggleDeed: (String) -> Unit,
    onResetToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedCount = completedIds.size
    val totalCount = deeds.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    Column(modifier = modifier.fillMaxSize()) {
        EmeraldOrnamentalHeader {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("deeds_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = NoorTheme.colors.onHeaderForest
                        )
                    }
                    Column {
                        Text(
                            text = "تحدي الأعمال الصالحة اليومية",
                            fontFamily = AmiriFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NoorTheme.colors.onHeaderForest
                        )
                        Text(
                            text = "استبقوا الخيرات وأعمروا يومكم بالطاعات",
                            style = MaterialTheme.typography.bodySmall,
                            color = NoorTheme.colors.goldAccent
                        )
                    }
                }
                IconButton(onClick = onResetToday, modifier = Modifier.testTag("deeds_reset_button")) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "إعادة ضبط اليوم",
                        tint = NoorTheme.colors.goldAccent
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                NoorGoldCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "إنجاز اليوم",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            EmeraldPillBadge(
                                text = "${ArabicTextUtils.toEasternArabicNumerals(completedCount)} من ${ArabicTextUtils.toEasternArabicNumerals(totalCount)} مكتمل"
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = NoorTheme.colors.goldBorder
                        )
                        if (completedCount == totalCount && totalCount > 0) {
                            Text(
                                text = "بارك الله في عملك! لقد أتممت جميع أوراد ومهام اليوم الصالحة ✓",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            items(deeds, key = { it.id }) { deed ->
                val isCompleted = completedIds.contains(deed.id)
                NoorGoldCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deed_card_${deed.id}"),
                    onClick = { onToggleDeed(deed.id) },
                    containerColor = if (isCompleted) NoorTheme.colors.badgeEmeraldBg else NoorTheme.colors.cardElevated,
                    borderColor = if (isCompleted) MaterialTheme.colorScheme.primary else NoorTheme.colors.goldBorder
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = if (isCompleted) "مكتمل" else "غير مكتمل",
                            tint = if (isCompleted) MaterialTheme.colorScheme.primary else NoorTheme.colors.goldAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = deed.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                EmeraldPillBadge(text = deed.category)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = deed.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
