package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.example.audio.QuranAudioDownloadManager.DownloadedSurahAudioInfo
import com.example.data.model.SurahInfo
import com.example.data.repository.FullQuranDownloadState
import com.example.ui.components.EmeraldPillBadge
import com.example.ui.components.NoorGoldCard
import com.example.ui.theme.NoorTheme
import com.example.util.ArabicTextUtils
import com.example.util.ExternalUrlHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineDownloadsSheet(
    offlineSurahCount: Int,
    fullDownloadState: FullQuranDownloadState,
    audioInventory: List<DownloadedSurahAudioInfo>,
    totalAudioBytes: Long,
    allSurahs: List<SurahInfo>,
    onStartFullQuranDownload: () -> Unit,
    onCancelFullQuranDownload: () -> Unit,
    onClearCachedQuran: () -> Unit,
    onDeleteSurahAudio: (reciterId: String, surahId: Int) -> Unit,
    onClearAllAudio: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showConfirmClearAllAudio by remember { mutableStateOf(false) }
    var showConfirmClearCachedQuran by remember { mutableStateOf(false) }

    val surahNameById = remember(allSurahs) {
        allSurahs.associate { it.id to it.name }
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
                text = "المكتبة الأوفلاين وإدارة التنزيلات",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "حمّل المصحف والتفسير الميسر كاملاً، وأدر مساحة التلاوات الصوتية المحفوظة على جهازك.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Section 1: Full Quran & Tafsir Offline Downloader
            NoorGoldCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("full_quran_offline_card")
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = NoorTheme.colors.goldAccent
                            )
                            Text(
                                text = "المصحف والتفسير الميسر",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        val countToShow = if (fullDownloadState.isDownloading) {
                            fullDownloadState.downloadedSurahsCount.coerceAtLeast(offlineSurahCount)
                        } else {
                            offlineSurahCount
                        }
                        EmeraldPillBadge(
                            text = "${ArabicTextUtils.toEasternArabicNumerals(countToShow)} / ١١٤ سورة"
                        )
                    }

                    val progressCount = if (fullDownloadState.isDownloading) {
                        fullDownloadState.downloadedSurahsCount.coerceAtLeast(offlineSurahCount)
                    } else {
                        offlineSurahCount
                    }
                    LinearProgressIndicator(
                        progress = { (progressCount.toFloat() / 114f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = NoorTheme.colors.badgeEmeraldBg
                    )

                    if (fullDownloadState.isDownloading) {
                        Text(
                            text = if (fullDownloadState.currentSurahName.isNotBlank()) {
                                "جارٍ تنزيل سورة ${fullDownloadState.currentSurahName} مع التفسير الميسر..."
                            } else {
                                "جارٍ تجهيز تنزيل السور..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = onCancelFullQuranDownload,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("cancel_full_quran_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إيقاف التحميل مؤقتاً")
                        }
                    } else if (offlineSurahCount >= 114) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "المصحف والتفسير الميسر متاحان بالكامل بدون إنترنت",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Button(
                            onClick = onStartFullQuranDownload,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("download_full_quran_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("تحميل المصحف والتفسير كاملاً بدون نت")
                        }
                    }

                    if (!fullDownloadState.errorMessage.isNullOrBlank()) {
                        Text(
                            text = fullDownloadState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    HorizontalDivider(color = NoorTheme.colors.goldBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                ExternalUrlHelper.openVerificationUrl(
                                    context,
                                    "https://qurancomplex.gov.sa/"
                                )
                            },
                            modifier = Modifier.testTag("offline_quran_source_link")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("مجمع الملك فهد للمصحف")
                        }
                        if (offlineSurahCount > 7) {
                            TextButton(
                                onClick = { showConfirmClearCachedQuran = true },
                                modifier = Modifier.testTag("clear_cached_quran_button")
                            ) {
                                Text(
                                    text = "مسح الكاش",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Audio Storage & Downloads Manager
            NoorGoldCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_storage_manager_card")
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Storage,
                                contentDescription = null,
                                tint = NoorTheme.colors.goldAccent
                            )
                            Text(
                                text = "مدير التلاوات الصوتية",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        EmeraldPillBadge(text = formatBytesArabic(totalAudioBytes))
                    }

                    if (audioInventory.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NoorTheme.colors.badgeEmeraldBg.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "لا توجد تلاوات محمّلة حالياً",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "يمكنك تحميل أي سورة صوتياً من فهرس القرآن أو من شريط التلاوة لتعمل بدون إنترنت.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "السور المحفوظة (${ArabicTextUtils.toEasternArabicNumerals(audioInventory.size)})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = { showConfirmClearAllAudio = true },
                                modifier = Modifier.testTag("clear_all_audio_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "مسح كل التلاوات",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        audioInventory.forEach { entry ->
                            val surahName = surahNameById[entry.surahId] ?: "${entry.surahId}"
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                border = BorderStroke(1.dp, NoorTheme.colors.goldBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("audio_inventory_item_${entry.reciter.id}_${entry.surahId}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.GraphicEq,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "سورة $surahName — ${entry.reciter.nameArabic}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${ArabicTextUtils.toEasternArabicNumerals(entry.fileCount)} آية • ${formatBytesArabic(entry.sizeBytes)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeleteSurahAudio(entry.reciter.id, entry.surahId) },
                                        modifier = Modifier.testTag("delete_audio_${entry.reciter.id}_${entry.surahId}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "حذف تلاوة سورة $surahName",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = NoorTheme.colors.goldBorder)

                    TextButton(
                        onClick = {
                            ExternalUrlHelper.openVerificationUrl(
                                context,
                                "https://everyayah.com/"
                            )
                        },
                        modifier = Modifier.testTag("offline_audio_source_link")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("مصدر التلاوات: مشروع EveryAyah")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showConfirmClearAllAudio) {
        AlertDialog(
            onDismissRequest = { showConfirmClearAllAudio = false },
            title = { Text("مسح كل التلاوات الصوتية؟") },
            text = { Text("سيتم حذف جميع الملفات الصوتية المحفوظة لتفريغ المساحة، ويمكنك إعادة تحميلها لاحقاً.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllAudio()
                        showConfirmClearAllAudio = false
                    }
                ) {
                    Text("حذف الكل", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearAllAudio = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showConfirmClearCachedQuran) {
        AlertDialog(
            onDismissRequest = { showConfirmClearCachedQuran = false },
            title = { Text("مسح النصوص المحفوظة مؤقتاً؟") },
            text = { Text("ستبقى السور الأساسية المدمجة متاحة بدون إنترنت، بينما تُحذف السور المحمّلة مؤقتاً من قاعدة البيانات.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearCachedQuran()
                        showConfirmClearCachedQuran = false
                    }
                ) {
                    Text("مسح", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearCachedQuran = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

fun formatBytesArabic(bytes: Long): String {
    if (bytes <= 0L) return "٠ ك.ب"
    val kb = bytes / 1024.0
    if (kb < 1024.0) {
        val rounded = kb.toInt().coerceAtLeast(1)
        return "${ArabicTextUtils.toEasternArabicNumerals(rounded)} ك.ب"
    }
    val mb = kb / 1024.0
    val formatted = String.format(Locale.US, "%.1f", mb)
        .replace('0', '٠')
        .replace('1', '١')
        .replace('2', '٢')
        .replace('3', '٣')
        .replace('4', '٤')
        .replace('5', '٥')
        .replace('6', '٦')
        .replace('7', '٧')
        .replace('8', '٨')
        .replace('9', '٩')
    return "$formatted م.ب"
}
