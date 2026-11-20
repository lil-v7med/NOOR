package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserSettings
import com.example.data.model.AyahItem
import com.example.data.model.SurahInfo
import com.example.khatma.KhatmaSummary
import com.example.prayer.DailyPrayerSchedule
import com.example.ui.components.EmeraldOrnamentalHeader
import com.example.ui.components.EmeraldPillBadge
import com.example.ui.components.KhatamStarBadge
import com.example.ui.components.KhatmaTrackerHomeCard
import com.example.ui.components.NoorGoldCard
import com.example.ui.components.PrayerCityMethodDialog
import com.example.ui.components.PrayerTimesHomeCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorTheme
import com.example.util.ArabicTextUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    settings: UserSettings,
    dailyVerse: Pair<SurahInfo, AyahItem>,
    bookmarksCount: Int,
    prayerSchedule: DailyPrayerSchedule,
    khatmaSummary: KhatmaSummary,
    offlineSurahCount: Int,
    totalAudioBytes: Long,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinueReading: (surahId: Int, ayahNumber: Int) -> Unit,
    onOpenAzkarCategory: (categoryId: String) -> Unit,
    onOpenTasbeeh: () -> Unit,
    onOpenQuranTab: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenOfflineManager: () -> Unit,
    onOpenPrayerAlerts: () -> Unit,
    onOpenAsmaulHusna: () -> Unit,
    onOpenDuaEncyclopedia: () -> Unit,
    onOpenZakatCalculator: () -> Unit,
    onOpenHijriCalendar: () -> Unit,
    onOpenPrayerTracker: () -> Unit,
    onSelectPrayerCityAndMethod: (cityId: String, methodId: String) -> Unit,
    onSelectKhatmaDuration: (Int) -> Unit,
    onLogKhatmaToday: () -> Unit,
    onResetKhatma: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPrayerCityDialog by remember { mutableStateOf(false) }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = if (hour < 12) "صباح النور والسكينة" else "مساء الذكر والطمأنينة"
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val completedToday = if (settings.completedAzkarDate == todayKey) {
        settings.completedAzkarCategories
    } else {
        emptySet()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EmeraldOrnamentalHeader {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "محراب النور",
                            fontFamily = AmiriFontFamily,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = NoorTheme.colors.onHeaderForest
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$greeting • ${prayerSchedule.hijriFormatted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NoorTheme.colors.goldAccent
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier.testTag("home_theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (settings.isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "تبديل المظهر",
                                tint = NoorTheme.colors.goldAccent
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("home_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "الإعدادات والتنبيهات",
                                tint = NoorTheme.colors.onHeaderForest
                            )
                        }
                    }
                }
            }
        }

        // Continue Reading Card
        item {
            NoorGoldCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("continue_reading_card"),
                onClick = { onContinueReading(settings.lastReadSurahId, settings.lastReadAyahNumber) }
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EmeraldPillBadge(text = "مواصلة القراءة")
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = NoorTheme.colors.goldAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KhatamStarBadge(number = settings.lastReadSurahId, size = 48.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سورة ${settings.lastReadSurahName}",
                                fontFamily = AmiriFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "توقفت عند الآية ${ArabicTextUtils.toEasternArabicNumerals(settings.lastReadAyahNumber)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { onContinueReading(settings.lastReadSurahId, settings.lastReadAyahNumber) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("continue_reading_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "متابعة التلاوة الآن",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // GROUP 1: Prayer Times, Qibla Compass & Asma Al-Husna (Grouped Together)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrayerTimesHomeCard(
                    schedule = prayerSchedule,
                    alertsEnabled = settings.prayerAlertsEnabled,
                    onOpenCityPicker = { showPrayerCityDialog = true },
                    onOpenPrayerAlerts = onOpenPrayerAlerts,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NoorGoldCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_qibla_card"),
                        onClick = onOpenQibla
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = NoorTheme.colors.badgeEmeraldBg,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Explore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                EmeraldPillBadge(text = "القبلة")
                            }
                            Text(
                                text = "اتجاه القبلة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "بوصلة الكعبة المشرفة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    NoorGoldCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_asma_card"),
                        onClick = onOpenAsmaulHusna
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = NoorTheme.colors.badgeEmeraldBg,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = NoorTheme.colors.goldAccent,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                EmeraldPillBadge(text = "الأسماء الحسنى")
                            }
                            Text(
                                text = "أسماء الله الحسنى",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "٩٩ اسماً ومعانيها",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Daily Khatma & Streak Tracker Card
        item {
            KhatmaTrackerHomeCard(
                summary = khatmaSummary,
                onSelectDuration = onSelectKhatmaDuration,
                onLogToday = onLogKhatmaToday,
                onReadTargetSurah = { surahId -> onContinueReading(surahId, 1) },
                onResetKhatma = onResetKhatma,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // GROUP 2: Morning/Evening Azkar & Duas (Supplications) Unified Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "الأذكار والأدعية اليومية",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AzkarQuickCard(
                        title = "أذكار الصباح",
                        subtitle = "١٢ ذكراً",
                        isCompleted = completedToday.contains("morning"),
                        icon = Icons.Filled.WbSunny,
                        onClick = { onOpenAzkarCategory("morning") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_morning_azkar_card")
                    )
                    AzkarQuickCard(
                        title = "أذكار المساء",
                        subtitle = "٩ أذكار",
                        isCompleted = completedToday.contains("evening"),
                        icon = Icons.Filled.NightsStay,
                        onClick = { onOpenAzkarCategory("evening") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_evening_azkar_card")
                    )
                }

                NoorGoldCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_dua_card"),
                    onClick = onOpenDuaEncyclopedia
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NoorTheme.colors.badgeEmeraldBg,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VolunteerActivism,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "جامع الأدعية والأذكار (حصن المسلم)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "أدعية القرآن والسنة والأدعية المأثورة الشاملة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        EmeraldPillBadge(text = "موسوعة")
                    }
                }
            }
        }

        // Stats Row: Lifetime Tasbeeh & Bookmarks
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NoorGoldCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_tasbeeh_stat_card"),
                    onClick = onOpenTasbeeh
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TouchApp,
                                contentDescription = null,
                                tint = NoorTheme.colors.goldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "مجموع التسبيح",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = ArabicTextUtils.toEasternArabicNumerals(settings.lifetimeTasbeeh),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                NoorGoldCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_bookmarks_stat_card"),
                    onClick = onOpenQuranTab
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = null,
                                tint = NoorTheme.colors.goldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "الآيات المحفوظة",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = ArabicTextUtils.toEasternArabicNumerals(bookmarksCount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Islamic Toolkit: Zakat Calculator & Hijri Calendar & Prayer Tracker
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "الأدوات الإسلامية",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NoorGoldCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_zakat_card"),
                        onClick = onOpenZakatCalculator
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Calculate,
                                contentDescription = null,
                                tint = NoorTheme.colors.goldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "الزكاة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "حاسبة دقيقة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    NoorGoldCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_hijri_calendar_card"),
                        onClick = onOpenHijriCalendar
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "التقويم الهجري",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "الإمساكية والمناسبات",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    NoorGoldCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_prayer_tracker_card"),
                        onClick = onOpenPrayerTracker
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = NoorTheme.colors.goldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "متابعة الصلاة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "الفروض والسنن",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Reminder Prompt Strip
        if (!settings.morningReminderEnabled && !settings.eveningReminderEnabled) {
            item {
                NoorGoldCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onClick = onOpenSettings
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NoorTheme.colors.badgeEmeraldBg,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تفعيل تنبيهات الأذكار اليومية",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "اضبط تذكيراً يومياً لأذكار الصباح والمساء",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPrayerCityDialog) {
        PrayerCityMethodDialog(
            selectedCityId = settings.prayerCityId,
            selectedMethodId = settings.prayerMethodId,
            onConfirm = onSelectPrayerCityAndMethod,
            onDismiss = { showPrayerCityDialog = false }
        )
    }
}

@Composable
private fun AzkarQuickCard(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NoorGoldCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NoorTheme.colors.goldAccent,
                    modifier = Modifier.size(24.dp)
                )
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "مكتمل",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isCompleted) "تم الإتمام اليوم ✓" else subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
