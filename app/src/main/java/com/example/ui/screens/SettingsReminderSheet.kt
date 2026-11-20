package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.core.content.ContextCompat
import com.example.data.local.PrayerSoundModes
import com.example.data.local.UserSettings
import com.example.notifications.AdhanAudioCatalog
import com.example.notifications.PrayerToneGenerator
import com.example.ui.components.NoorGoldCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorTheme
import com.example.ui.viewmodel.MuezzinDownloadUiState
import com.example.util.ArabicTextUtils
import com.example.util.ExternalUrlHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsReminderSheet(
    settings: UserSettings,
    backupStatusMessage: String?,
    muezzinState: MuezzinDownloadUiState,
    onDismiss: () -> Unit,
    onSetDarkMode: (Boolean) -> Unit,
    onSetFontSize: (Float) -> Unit,
    onUpdateMorningReminder: (Boolean, Int, Int) -> Unit,
    onUpdateEveningReminder: (Boolean, Int, Int) -> Unit,
    onSetPrayerAlertsEnabled: (Boolean) -> Unit,
    onSetPrayerSoundMode: (String) -> Unit,
    onSelectMuezzin: (String) -> Unit,
    onDownloadMuezzin: (String) -> Unit,
    onDeleteMuezzin: (String) -> Unit,
    onSetPrayerPreAlertMinutes: (Int) -> Unit,
    onTogglePrayerTypeAlert: (String) -> Unit,
    onExportBackup: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
    onClearBackupStatus: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenOfflineManager: () -> Unit
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onUpdateMorningReminder(true, settings.morningReminderHour, settings.morningReminderMinute)
        }
    }
    val prayerNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onSetPrayerAlertsEnabled(true)
        }
    }
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackup(uri)
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportBackup(uri)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "الإعدادات والتنبيهات",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Theme toggle
            NoorGoldCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الوضع الليلي",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "خلفية داكنة مريحة للقراءة الليلية",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.isDarkMode,
                        onCheckedChange = onSetDarkMode,
                        modifier = Modifier.testTag("settings_dark_mode_switch")
                    )
                }
            }

            // Font size preview
            NoorGoldCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "حجم خط المصحف",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = ArabicTextUtils.toEasternArabicNumerals(settings.quranFontSizeSp.toInt()),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = settings.quranFontSizeSp,
                        onValueChange = onSetFontSize,
                        valueRange = 18f..34f,
                        modifier = Modifier.testTag("settings_font_slider")
                    )
                    Text(
                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        fontFamily = AmiriFontFamily,
                        fontSize = settings.quranFontSizeSp.sp,
                        lineHeight = (settings.quranFontSizeSp * 1.9f).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Daily Azkar Notifications
            NoorGoldCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "تذكير الأذكار اليومية",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.WbSunny, null, tint = NoorTheme.colors.goldAccent)
                            Column {
                                Text("أذكار الصباح", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = formatTimeArabic(settings.morningReminderHour, settings.morningReminderMinute, true),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = settings.morningReminderEnabled,
                            onCheckedChange = { enable ->
                                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onUpdateMorningReminder(enable, settings.morningReminderHour, settings.morningReminderMinute)
                                }
                            },
                            modifier = Modifier.testTag("morning_reminder_switch")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5 to 30, 6 to 0, 7 to 0).forEach { (h, m) ->
                            FilterChip(
                                selected = settings.morningReminderHour == h && settings.morningReminderMinute == m,
                                onClick = {
                                    onUpdateMorningReminder(settings.morningReminderEnabled, h, m)
                                },
                                label = { Text(formatTimeArabic(h, m, true)) }
                            )
                        }
                    }

                    HorizontalDivider(color = NoorTheme.colors.goldBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.NightsStay, null, tint = NoorTheme.colors.goldAccent)
                            Column {
                                Text("أذكار المساء", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = formatTimeArabic(settings.eveningReminderHour, settings.eveningReminderMinute, false),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = settings.eveningReminderEnabled,
                            onCheckedChange = { enable ->
                                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onUpdateEveningReminder(enable, settings.eveningReminderHour, settings.eveningReminderMinute)
                                }
                            },
                            modifier = Modifier.testTag("evening_reminder_switch")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(16 to 30, 17 to 0, 18 to 0).forEach { (h, m) ->
                            FilterChip(
                                selected = settings.eveningReminderHour == h && settings.eveningReminderMinute == m,
                                onClick = {
                                    onUpdateEveningReminder(settings.eveningReminderEnabled, h, m)
                                },
                                label = { Text(formatTimeArabic(h, m, false)) }
                            )
                        }
                    }
                }
            }

            // Prayer Time Alerts & Adhan Settings
            NoorGoldCard(modifier = Modifier.fillMaxWidth()) {
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
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = NoorTheme.colors.goldAccent
                            )
                            Column {
                                Text(
                                    text = "تنبيهات مواقيت الصلاة والأذان",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "إشعار تلقائي عند دخول وقت الصلوات الخمس",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = settings.prayerAlertsEnabled,
                            onCheckedChange = { enable ->
                                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    prayerNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSetPrayerAlertsEnabled(enable)
                                }
                            },
                            modifier = Modifier.testTag("prayer_alerts_switch")
                        )
                    }

                    Text(
                        text = "نمط صوت التنبيه:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            PrayerSoundModes.ADHAN to "أذان كامل",
                            PrayerSoundModes.TAKBIR to "تكبير خفيف",
                            PrayerSoundModes.SILENT to "صامت"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = settings.prayerSoundMode == mode,
                                onClick = { onSetPrayerSoundMode(mode) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("prayer_sound_$mode")
                            )
                        }
                    }

                    if (settings.prayerSoundMode == PrayerSoundModes.ADHAN) {
                        val selectedOption = AdhanAudioCatalog.byId(settings.selectedMuezzinId)
                        val isSelectedDownloaded = muezzinState.downloadedIds.contains(selectedOption.id)
                        val isDownloadingThis = muezzinState.downloadingId == selectedOption.id

                        Text(
                            text = "اختر صوت المؤذن (تشغيل أوفلاين بعد التحميل):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AdhanAudioCatalog.muezzins.forEach { option ->
                                val downloaded = muezzinState.downloadedIds.contains(option.id)
                                FilterChip(
                                    selected = settings.selectedMuezzinId == option.id,
                                    onClick = { onSelectMuezzin(option.id) },
                                    label = {
                                        Text(
                                            if (downloaded) "${option.locationArabic} ✓" else option.locationArabic
                                        )
                                    },
                                    modifier = Modifier.testTag("muezzin_chip_${option.id}")
                                )
                            }
                        }

                        Text(
                            text = "${selectedOption.nameArabic} (${selectedOption.locationArabic})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isDownloadingThis) {
                                OutlinedButton(
                                    onClick = {},
                                    enabled = false,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("جارٍ تحميل الأذان...")
                                }
                            } else if (isSelectedDownloaded) {
                                OutlinedButton(
                                    onClick = { onDeleteMuezzin(selectedOption.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("delete_muezzin_button")
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("محمّل ✓ (حذف الملف)")
                                }
                            } else {
                                Button(
                                    onClick = { onDownloadMuezzin(selectedOption.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("download_muezzin_button")
                                ) {
                                    Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("تحميل الأذان الحقيقي (MP3)")
                                }
                            }
                        }

                        if (!muezzinState.statusMessage.isNullOrBlank()) {
                            Text(
                                text = muezzinState.statusMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                PrayerToneGenerator.playPreview(
                                    context,
                                    settings.prayerSoundMode,
                                    settings.selectedMuezzinId
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("prayer_sound_preview_button")
                        ) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تجربة الصوت")
                        }
                        OutlinedButton(
                            onClick = { PrayerToneGenerator.stop() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("prayer_sound_stop_button")
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "إيقاف الصوت", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("إيقاف")
                        }
                    }

                    if (settings.prayerSoundMode == PrayerSoundModes.ADHAN) {
                        TextButton(
                            onClick = {
                                ExternalUrlHelper.openVerificationUrl(
                                    context,
                                    "https://www.islamcan.com/audio/adhan/azan"
                                )
                            },
                            modifier = Modifier.testTag("adhan_source_verification_link")
                        ) {
                            Text("مصدر تسجيلات الأذان: IslamCan")
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = "التنبيه التمهيدي:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.prayerPreAlertMinutes == 0,
                            onClick = { onSetPrayerPreAlertMinutes(0) },
                            label = { Text("عند دخول الوقت") }
                        )
                        FilterChip(
                            selected = settings.prayerPreAlertMinutes == 10,
                            onClick = { onSetPrayerPreAlertMinutes(10) },
                            label = { Text("قبل الصلاة بـ ١٠ د") }
                        )
                    }

                    Text(
                        text = "الصلوات المفعّلة:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "FAJR" to "الفجر",
                            "DHUHR" to "الظهر",
                            "ASR" to "العصر",
                            "MAGHRIB" to "المغرب",
                            "ISHA" to "العشاء"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = settings.enabledPrayerTypes.contains(key),
                                onClick = { onTogglePrayerTypeAlert(key) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("prayer_toggle_$key")
                            )
                        }
                    }
                }
            }

            // JSON Backup & Restore (SAF - Zero Storage Permissions)
            NoorGoldCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "النسخ الاحتياطي والاسترجاع (JSON)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "احفظ نسخة من العلامات المرجعية، أذكارك المخصصة، مفضلة الأحاديث، وتقدم الختمة والتسبيح.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onClearBackupStatus()
                                exportBackupLauncher.launch("noor_sakinah_backup.json")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("backup_export_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Backup, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تصدير")
                        }
                        OutlinedButton(
                            onClick = {
                                onClearBackupStatus()
                                importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("backup_import_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Restore, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("استيراد")
                        }
                    }
                    if (!backupStatusMessage.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = backupStatusMessage,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onClearBackupStatus) {
                                Text("إخفاء")
                            }
                        }
                    }
                }
            }

            // Offline Library & Audio Storage Manager
            NoorGoldCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_offline_manager_card"),
                onClick = {
                    onDismiss()
                    onOpenOfflineManager()
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = null,
                            tint = NoorTheme.colors.goldAccent
                        )
                        Column {
                            Text(
                                text = "المكتبة الأوفلاين وإدارة التنزيلات",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تنزيل المصحف كاملاً وحذف التلاوات لإدارة المساحة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "فتح إدارة التنزيلات",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Sources & Verification References
            NoorGoldCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_sources_card"),
                onClick = {
                    onDismiss()
                    onOpenSources()
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = NoorTheme.colors.goldAccent
                        )
                        Column {
                            Text(
                                text = "المصادر والمراجع المعتمدة",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "القرآن الكريم والتفسير والأحاديث وحصن المسلم والتلاوات",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "عرض المصادر",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun formatTimeArabic(hour24: Int, minute: Int, isMorning: Boolean): String {
    val displayHour = if (hour24 > 12) hour24 - 12 else if (hour24 == 0) 12 else hour24
    val hStr = ArabicTextUtils.toEasternArabicNumerals(displayHour)
    val mStr = ArabicTextUtils.toEasternArabicNumerals(minute).padStart(2, '٠')
    val suffix = if (isMorning) "ص" else "م"
    return "$hStr:$mStr $suffix"
}
