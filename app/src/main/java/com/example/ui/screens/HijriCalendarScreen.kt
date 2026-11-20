package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prayer.HijriCalendarHelper
import com.example.prayer.HijriDayEntry
import com.example.prayer.HijriMonthSummary
import com.example.ui.components.EmeraldOrnamentalHeader
import com.example.ui.components.EmeraldPillBadge
import com.example.ui.components.NoorGoldCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorTheme
import com.example.util.ArabicTextUtils
import java.util.Calendar

@Composable
fun HijriCalendarScreen(
    summary: HijriMonthSummary,
    monthOffset: Int,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToday: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedHijriDay by remember(summary.hijriYear, summary.hijriMonth) {
        mutableIntStateOf(summary.days.firstOrNull { it.isToday }?.hijriDay ?: 1)
    }
    LaunchedEffect(summary.hijriYear, summary.hijriMonth) {
        selectedHijriDay = summary.days.firstOrNull { it.isToday }?.hijriDay ?: 1
    }

    val selectedEntry: HijriDayEntry? = summary.days.firstOrNull { it.hijriDay == selectedHijriDay }
        ?: summary.days.firstOrNull()

    // Two-way converter state
    val nowCal = remember { Calendar.getInstance() }
    var isGregorianToHijri by remember { mutableStateOf(true) }
    var yearText by remember { mutableStateOf(nowCal.get(Calendar.YEAR).toString()) }
    var monthText by remember { mutableStateOf((nowCal.get(Calendar.MONTH) + 1).toString()) }
    var dayText by remember { mutableStateOf(nowCal.get(Calendar.DAY_OF_MONTH).toString()) }

    val convertedOutput = remember(isGregorianToHijri, yearText, monthText, dayText) {
        val y = yearText.toIntOrNull()
        val m = monthText.toIntOrNull()
        val d = dayText.toIntOrNull()
        if (y == null || m == null || d == null || m !in 1..12 || d !in 1..31) {
            "أدخل تاريخاً صحيحاً للتحويل"
        } else if (isGregorianToHijri) {
            if (y !in 1900..2150) "أدخل سنة ميلادية بين ١٩٠٠ و ٢١٥٠"
            else HijriCalendarHelper.convertGregorianToHijri(y, m, d)
        } else {
            if (y !in 1300..1600) "أدخل سنة هجرية بين ١٣٠٠ و ١٦٠٠"
            else HijriCalendarHelper.convertHijriToGregorian(y, m, d.coerceIn(1, 30))
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            EmeraldOrnamentalHeader {
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
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("hijri_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = NoorTheme.colors.onHeaderForest
                            )
                        }
                        Column {
                            Text(
                                text = "التقويم الهجري والإمساكية",
                                fontFamily = AmiriFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = NoorTheme.colors.onHeaderForest
                            )
                            Text(
                                text = "${summary.city.nameArabic} • ${summary.method.nameArabic}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NoorTheme.colors.goldAccent
                            )
                        }
                    }
                    if (monthOffset != 0) {
                        IconButton(
                            onClick = onResetToday,
                            modifier = Modifier.testTag("hijri_reset_today_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Today,
                                contentDescription = "العودة للشهر الحالي",
                                tint = NoorTheme.colors.goldAccent
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Month Navigation Header & Grid Card
                item {
                    NoorGoldCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hijri_calendar_grid_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onPrevMonth,
                                    modifier = Modifier.testTag("hijri_prev_month_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "الشهر السابق",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${summary.hijriMonthName} ${ArabicTextUtils.toEasternArabicNumerals(summary.hijriYear)} هـ",
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val firstG = summary.days.firstOrNull()?.gregorianLabelArabic
                                    val lastG = summary.days.lastOrNull()?.gregorianLabelArabic
                                    if (firstG != null && lastG != null) {
                                        Text(
                                            text = "$firstG — $lastG",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = onNextMonth,
                                    modifier = Modifier.testTag("hijri_next_month_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "الشهر التالي",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (monthOffset != 0) {
                                TextButton(
                                    onClick = onResetToday,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("اليوم الحالي")
                                }
                            }

                            // Weekday Headers (Sat..Fri)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                HijriCalendarHelper.rtlWeekdaysHeader.forEach { dayName ->
                                    Text(
                                        text = dayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Calendar Cells
                            val leadingEmpty = summary.firstDayOfWeekOffset
                            val totalCells = leadingEmpty + summary.days.size
                            val rowCount = (totalCells + 6) / 7

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (row in 0 until rowCount) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        for (col in 0 until 7) {
                                            val cellIndex = row * 7 + col
                                            val dayIndex = cellIndex - leadingEmpty
                                            val entry = summary.days.getOrNull(dayIndex)
                                            if (entry == null) {
                                                Spacer(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                )
                                            } else {
                                                val isSelected = entry.hijriDay == selectedHijriDay
                                                HijriDayCell(
                                                    entry = entry,
                                                    isSelected = isSelected,
                                                    onClick = { selectedHijriDay = entry.hijriDay },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Legend
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LegendDot(color = MaterialTheme.colorScheme.primary, label = "اليوم")
                                LegendDot(color = NoorTheme.colors.goldAccent, label = "الأيام البيض / مناسبة")
                            }
                        }
                    }
                }

                // Selected Day Monthly Imsakiyah & Sunnah Status Card
                if (selectedEntry != null) {
                    item {
                        NoorGoldCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hijri_selected_day_card"),
                            borderColor = NoorTheme.colors.goldAccent
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "إمساكية ${ArabicTextUtils.toEasternArabicNumerals(selectedEntry.hijriDay)} ${summary.hijriMonthName}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "الموافق ${selectedEntry.gregorianLabelArabic} ${ArabicTextUtils.toEasternArabicNumerals(selectedEntry.gregorianYear)} م",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (selectedEntry.isToday) {
                                        EmeraldPillBadge(text = "اليوم")
                                    }
                                }

                                if (selectedEntry.occasionLabel != null) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = NoorTheme.colors.badgeEmeraldBg,
                                        border = BorderStroke(1.dp, NoorTheme.colors.goldBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.EventAvailable,
                                                contentDescription = null,
                                                tint = NoorTheme.colors.goldAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "مناسبة إسلامية: ${selectedEntry.occasionLabel}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                if (selectedEntry.isWhiteDay || selectedEntry.isMonOrThu) {
                                    val fastingNote = buildList {
                                        if (selectedEntry.isWhiteDay) add("من الأيام البيض (١٣-١٥)")
                                        if (selectedEntry.isMonOrThu) add("سنة صيام الإثنين والخميس")
                                    }.joinToString(" • ")
                                    Text(
                                        text = "تذكير الصيام المستحب: $fastingNote",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NoorTheme.colors.goldAccent
                                    )
                                }

                                HorizontalDivider(color = NoorTheme.colors.goldBorder)

                                // 6-slot Imsakiyah Grid (2 rows of 3)
                                val slots = selectedEntry.schedule.slots
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    slots.chunked(3).forEach { rowSlots ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowSlots.forEach { slot ->
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = slot.type.labelArabic,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = formatPrayerSlotArabic(slot.hour24, slot.minute),
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
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

                // Month Islamic Occasions List
                val monthOccasions = summary.days.filter { it.occasionLabel != null }
                if (monthOccasions.isNotEmpty()) {
                    item {
                        NoorGoldCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "مناسبات شهر ${summary.hijriMonthName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                monthOccasions.forEach { occ ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedHijriDay = occ.hijriDay }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = occ.occasionLabel.orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        EmeraldPillBadge(
                                            text = "${ArabicTextUtils.toEasternArabicNumerals(occ.hijriDay)} ${summary.hijriMonthName}"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Two-Way Hijri <-> Gregorian Date Converter Card
                item {
                    NoorGoldCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hijri_converter_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    tint = NoorTheme.colors.goldAccent
                                )
                                Text(
                                    text = "محوّل التاريخ (هجري ↔ ميلادي)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = isGregorianToHijri,
                                    onClick = {
                                        if (!isGregorianToHijri) {
                                            isGregorianToHijri = true
                                            yearText = nowCal.get(Calendar.YEAR).toString()
                                            monthText = (nowCal.get(Calendar.MONTH) + 1).toString()
                                            dayText = nowCal.get(Calendar.DAY_OF_MONTH).toString()
                                        }
                                    },
                                    label = { Text("ميلادي ← هجري") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("converter_g2h_chip")
                                )
                                FilterChip(
                                    selected = !isGregorianToHijri,
                                    onClick = {
                                        if (isGregorianToHijri) {
                                            isGregorianToHijri = false
                                            yearText = summary.hijriYear.toString()
                                            monthText = summary.hijriMonth.toString()
                                            dayText = selectedHijriDay.toString()
                                        }
                                    },
                                    label = { Text("هجري ← ميلادي") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("converter_h2g_chip")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = dayText,
                                    onValueChange = { s -> dayText = s.filter { it.isDigit() }.take(2) },
                                    label = { Text("اليوم") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = monthText,
                                    onValueChange = { s -> monthText = s.filter { it.isDigit() }.take(2) },
                                    label = { Text("الشهر") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = yearText,
                                    onValueChange = { s -> yearText = s.filter { it.isDigit() }.take(4) },
                                    label = { Text("السنة") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.3f)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NoorTheme.colors.badgeEmeraldBg,
                                border = BorderStroke(1.dp, NoorTheme.colors.goldBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SwapHoriz,
                                        contentDescription = null,
                                        tint = NoorTheme.colors.goldAccent
                                    )
                                    Text(
                                        text = convertedOutput,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
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

@Composable
private fun HijriDayCell(
    entry: HijriDayEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        entry.isToday -> MaterialTheme.colorScheme.primary
        isSelected -> NoorTheme.colors.badgeEmeraldBg
        entry.isWhiteDay -> NoorTheme.colors.goldAccent.copy(alpha = 0.14f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val textColor = when {
        entry.isToday -> MaterialTheme.colorScheme.onPrimary
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val border = when {
        isSelected -> BorderStroke(1.5.dp, NoorTheme.colors.goldAccent)
        entry.occasionLabel != null -> BorderStroke(1.dp, NoorTheme.colors.goldAccent.copy(alpha = 0.7f))
        else -> null
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = border,
        modifier = modifier
            .aspectRatio(0.95f)
            .testTag("hijri_day_${entry.hijriDay}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = ArabicTextUtils.toEasternArabicNumerals(entry.hijriDay),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (entry.isToday || isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = ArabicTextUtils.toEasternArabicNumerals(entry.gregorianDay),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = if (entry.isToday) textColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (entry.occasionLabel != null || entry.isWhiteDay) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (entry.isToday) MaterialTheme.colorScheme.onPrimary else NoorTheme.colors.goldAccent)
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatPrayerSlotArabic(hour24: Int, minute: Int): String {
    val isAm = hour24 < 12
    val h12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    val hStr = ArabicTextUtils.toEasternArabicNumerals(h12)
    val mStr = ArabicTextUtils.toEasternArabicNumerals(minute).padStart(2, '٠')
    return "$hStr:$mStr ${if (isAm) "ص" else "م"}"
}
