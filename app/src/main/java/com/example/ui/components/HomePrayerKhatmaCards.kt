package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.khatma.KhatmaPlanner
import com.example.khatma.KhatmaSummary
import com.example.prayer.DailyPrayerSchedule
import com.example.prayer.PrayerTimesCalculator
import com.example.ui.theme.NoorTheme
import com.example.util.ArabicTextUtils
import com.example.util.ExternalUrlHelper

@Composable
fun PrayerTimesHomeCard(
    schedule: DailyPrayerSchedule,
    alertsEnabled: Boolean,
    onOpenCityPicker: () -> Unit,
    onOpenPrayerAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NoorGoldCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_prayer_times_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = NoorTheme.colors.goldAccent
                    )
                    Column {
                        Text(
                            text = "مواقيت الصلاة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${schedule.city.nameArabic} • ${schedule.method.nameArabic}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onOpenPrayerAlerts,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("prayer_alerts_shortcut_button")
                    ) {
                        Icon(
                            imageVector = if (alertsEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                            contentDescription = "تنبيهات الأذان",
                            tint = if (alertsEnabled) NoorTheme.colors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        onClick = onOpenCityPicker,
                        shape = RoundedCornerShape(999.dp),
                        color = NoorTheme.colors.badgeEmeraldBg,
                        border = BorderStroke(1.dp, NoorTheme.colors.goldBorder),
                        modifier = Modifier.testTag("prayer_change_city_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationCity,
                                contentDescription = "تغيير المدينة",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = schedule.city.nameArabic,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Next prayer + Hijri banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NoorTheme.colors.badgeEmeraldBg,
                border = BorderStroke(1.dp, NoorTheme.colors.goldBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "الصلاة القادمة: ${schedule.nextPrayer.labelArabic}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "متبقي ${formatRemainingMinutesArabic(schedule.nextPrayerMinutesRemaining)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = NoorTheme.colors.goldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = schedule.hijriFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 6 slots displayed as 2 rows x 3 columns for balanced RTL readability
            val firstRow = schedule.slots.take(3)
            val secondRow = schedule.slots.drop(3)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    firstRow.forEach { slot ->
                        val isNext = slot.type == schedule.nextPrayer
                        PrayerSlotPill(
                            label = slot.type.labelArabic,
                            timeText = formatPrayerSlotArabic(slot.hour24, slot.minute),
                            isHighlighted = isNext,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    secondRow.forEach { slot ->
                        val isNext = slot.type == schedule.nextPrayer
                        PrayerSlotPill(
                            label = slot.type.labelArabic,
                            timeText = formatPrayerSlotArabic(slot.hour24, slot.minute),
                            isHighlighted = isNext,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HorizontalDivider(color = NoorTheme.colors.goldBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "حساب فلكي حسب المدينة يدوياً",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        ExternalUrlHelper.openVerificationUrl(
                            context,
                            "https://praytimes.org/calculation"
                        )
                    },
                    modifier = Modifier.testTag("prayer_source_link")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("المرجع الفلكي")
                }
            }
        }
    }
}

@Composable
private fun PrayerSlotPill(
    label: String,
    timeText: String,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (isHighlighted) 1.5.dp else 1.dp,
            color = if (isHighlighted) NoorTheme.colors.goldAccent else NoorTheme.colors.goldBorder
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = if (isHighlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isHighlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PrayerCityMethodDialog(
    selectedCityId: String,
    selectedMethodId: String,
    onConfirm: (cityId: String, methodId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempCityId by remember(selectedCityId) { mutableStateOf(selectedCityId) }
    var tempMethodId by remember(selectedMethodId) { mutableStateOf(selectedMethodId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "اختر المدينة وطريقة الحساب",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "المدينة (حساب يدوي بدون تحديد موقع GPS):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                PrayerTimesCalculator.cities.forEach { city ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prayer_city_option_${city.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = tempCityId == city.id,
                            onClick = {
                                tempCityId = city.id
                                tempMethodId = city.defaultMethodId
                            }
                        )
                        Text(
                            text = "${city.nameArabic} (${city.countryArabic})",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                Text(
                    text = "طريقة الحساب الفلكي:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                PrayerTimesCalculator.methods.forEach { method ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prayer_method_option_${method.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = tempMethodId == method.id,
                            onClick = { tempMethodId = method.id }
                        )
                        Text(
                            text = method.nameArabic,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(tempCityId, tempMethodId)
                    onDismiss()
                },
                modifier = Modifier.testTag("prayer_city_save_button")
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun KhatmaTrackerHomeCard(
    summary: KhatmaSummary,
    onSelectDuration: (Int) -> Unit,
    onLogToday: () -> Unit,
    onReadTargetSurah: (surahId: Int) -> Unit,
    onResetKhatma: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NoorGoldCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_khatma_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = NoorTheme.colors.goldAccent
                    )
                    Text(
                        text = "ورد الختمة اليومية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (summary.streakDays > 0) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = NoorTheme.colors.goldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${ArabicTextUtils.toEasternArabicNumerals(summary.streakDays)} يوم",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onResetKhatma,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestartAlt,
                            contentDescription = "إعادة ضبط الختمة",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = "الجزء ${ArabicTextUtils.toEasternArabicNumerals(summary.currentJuz)} • سورة ${summary.targetSurahName} • صفحة ${ArabicTextUtils.toEasternArabicNumerals(summary.currentPage)} من ${ArabicTextUtils.toEasternArabicNumerals(summary.totalPages)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LinearProgressIndicator(
                progress = { summary.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = NoorTheme.colors.badgeEmeraldBg
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KhatmaPlanner.durationPresets.forEach { days ->
                    FilterChip(
                        selected = summary.durationDays == days,
                        onClick = { onSelectDuration(days) },
                        label = { Text("${ArabicTextUtils.toEasternArabicNumerals(days)} يوماً") },
                        modifier = Modifier.testTag("khatma_preset_$days")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onLogToday,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("khatma_log_today_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (summary.isTodayComplete) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("أتممت الورد ✓", style = MaterialTheme.typography.labelMedium)
                    } else {
                        Text(
                            text = "إتمام (+${ArabicTextUtils.toEasternArabicNumerals(summary.dailyTargetPages)} ص)",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                OutlinedButton(
                    onClick = { onReadTargetSurah(summary.targetSurahId) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("khatma_read_now_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NoorTheme.colors.goldBorder)
                ) {
                    Text(
                        text = "اقرأ وردك الآن",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الهدف اليومي: ${ArabicTextUtils.toEasternArabicNumerals(summary.dailyTargetPages)} صفحة",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        ExternalUrlHelper.openVerificationUrl(context, "https://qurancomplex.gov.sa/")
                    },
                    modifier = Modifier.testTag("khatma_source_link")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("مصحف مجمع الملك فهد")
                }
            }
        }
    }
}

private fun formatPrayerSlotArabic(hour24: Int, minute: Int): String {
    val displayHour = if (hour24 > 12) hour24 - 12 else if (hour24 == 0) 12 else hour24
    val hStr = ArabicTextUtils.toEasternArabicNumerals(displayHour)
    val mStr = ArabicTextUtils.toEasternArabicNumerals(minute).padStart(2, '٠')
    val suffix = if (hour24 < 12) "ص" else "م"
    return "$hStr:$mStr $suffix"
}

private fun formatRemainingMinutesArabic(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return if (hours > 0) {
        "${ArabicTextUtils.toEasternArabicNumerals(hours)} س ${ArabicTextUtils.toEasternArabicNumerals(mins)} د"
    } else {
        "${ArabicTextUtils.toEasternArabicNumerals(mins)} دقيقة"
    }
}
