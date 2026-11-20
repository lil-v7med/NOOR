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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyPrayerLogEntity
import com.example.prayer.PrayerTimesCalculator
import com.example.ui.components.EmeraldOrnamentalHeader
import com.example.ui.components.EmeraldPillBadge
import com.example.ui.components.NoorGoldCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorTheme
import com.example.util.ArabicTextUtils
import com.example.util.ExternalUrlHelper

private data class PrayerSlotItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val isDone: Boolean
)

@Composable
fun PrayerTrackerScreen(
    todayLog: DailyPrayerLogEntity,
    recentLogs: List<DailyPrayerLogEntity>,
    onToggleSlot: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hijriToday = PrayerTimesCalculator.formatHijriDate()

    val fardItems = listOf(
        PrayerSlotItem("fajr", "صلاة الفجر", "ركعتان فرضاً", todayLog.fajrDone),
        PrayerSlotItem("dhuhr", "صلاة الظهر", "أربع ركعات فرضاً", todayLog.dhuhrDone),
        PrayerSlotItem("asr", "صلاة العصر", "أربع ركعات فرضاً", todayLog.asrDone),
        PrayerSlotItem("maghrib", "صلاة المغرب", "ثلاث ركعات فرضاً", todayLog.maghribDone),
        PrayerSlotItem("isha", "صلاة العشاء", "أربع ركعات فرضاً", todayLog.ishaDone)
    )

    val sunnahItems = listOf(
        PrayerSlotItem("rawatib", "السنن الرواتب", "١٢ ركعة في اليوم والليلة", todayLog.rawatibDone),
        PrayerSlotItem("duha", "صلاة الضحى", "من ركعتين إلى ثماني ركعات", todayLog.duhaDone),
        PrayerSlotItem("witr", "صلاة الوتر", "ختام صلاة الليل ولو بركعة", todayLog.witrDone)
    )

    val fullFardDaysCount = recentLogs.count { it.completedFardCount == 5 }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            EmeraldOrnamentalHeader {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("tracker_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = NoorTheme.colors.onHeaderForest
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "متابعة الصلوات والسنن",
                            fontFamily = AmiriFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NoorTheme.colors.onHeaderForest
                        )
                        Text(
                            text = hijriToday,
                            style = MaterialTheme.typography.labelSmall,
                            color = NoorTheme.colors.goldAccent
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Summary Progress Card
                item {
                    NoorGoldCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tracker_summary_card"),
                        containerColor = if (todayLog.completedFardCount == 5) {
                            NoorTheme.colors.badgeEmeraldBg
                        } else {
                            NoorTheme.colors.cardElevated
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EmeraldPillBadge(
                                    text = "الفروض: ${ArabicTextUtils.toEasternArabicNumerals(todayLog.completedFardCount)} / ٥"
                                )
                                EmeraldPillBadge(
                                    text = "السنن: ${ArabicTextUtils.toEasternArabicNumerals(todayLog.completedSunnahCount)} / ٣"
                                )
                            }

                            LinearProgressIndicator(
                                progress = { todayLog.completedFardCount / 5f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = NoorTheme.colors.goldBorder
                            )

                            Text(
                                text = if (todayLog.completedFardCount == 5) {
                                    "ما شاء الله! أتممت الصلوات الخمس المفروضة اليوم ✓"
                                } else {
                                    "اضغط على كل صلاة بعد أدائها لتسجيل إنجازك اليومي."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 5 Fard Prayers Section
                item {
                    Text(
                        text = "الصلوات الخمس المفروضة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(fardItems, key = { it.key }) { item ->
                    PrayerTrackerToggleCard(
                        item = item,
                        onToggle = { onToggleSlot(item.key) }
                    )
                }

                // Sunnah Section
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "السنن والرواتب اليومية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(sunnahItems, key = { it.key }) { item ->
                    PrayerTrackerToggleCard(
                        item = item,
                        onToggle = { onToggleSlot(item.key) }
                    )
                }

                // Hadith Reference Link for Rawatib
                item {
                    Surface(
                        onClick = {
                            ExternalUrlHelper.openVerificationUrl(
                                context,
                                "https://sunnah.com/muslim:728"
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = NoorTheme.colors.badgeEmeraldBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tracker_source_link")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "فضل السنن الرواتب (صحيح مسلم: ٧٢٨)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "فتح المرجع",
                                tint = NoorTheme.colors.goldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Recent 14-day Consistency Log
                if (recentLogs.isNotEmpty()) {
                    item {
                        NoorGoldCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "سجل المواظبة (آخر ١٤ يوماً)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    EmeraldPillBadge(
                                        text = "أيام مكتملة: ${ArabicTextUtils.toEasternArabicNumerals(fullFardDaysCount)}"
                                    )
                                }
                                HorizontalDivider(color = NoorTheme.colors.goldBorder)
                                recentLogs.forEach { log ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = log.dateKey,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "الفروض ${ArabicTextUtils.toEasternArabicNumerals(log.completedFardCount)}/٥ • السنن ${ArabicTextUtils.toEasternArabicNumerals(log.completedSunnahCount)}/٣",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (log.completedFardCount == 5) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerTrackerToggleCard(
    item: PrayerSlotItem,
    onToggle: () -> Unit
) {
    NoorGoldCard(
        onClick = onToggle,
        containerColor = if (item.isDone) NoorTheme.colors.badgeEmeraldBg else NoorTheme.colors.cardElevated,
        borderColor = if (item.isDone) NoorTheme.colors.goldAccent else NoorTheme.colors.goldBorder,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tracker_slot_${item.key}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (item.isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (item.isDone) "تمت الصلاة" else "لم تُسجل بعد",
                tint = if (item.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
