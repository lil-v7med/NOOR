package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.DownloadStatus
import com.example.audio.SurahDownloadState
import com.example.data.local.BookmarkEntity
import com.example.data.model.JuzInfo
import com.example.data.model.ReciterInfo
import com.example.data.model.SurahInfo
import com.example.data.repository.QuranSearchMatch
import com.example.ui.components.EmeraldOrnamentalHeader
import com.example.ui.components.EmeraldPillBadge
import com.example.ui.components.KhatamStarBadge
import com.example.ui.components.NoorGoldCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorTheme
import com.example.util.ArabicTextUtils
import com.example.util.ExternalUrlHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranIndexScreen(
    searchQuery: String,
    searchResults: List<QuranSearchMatch>,
    juzList: List<JuzInfo>,
    bookmarks: List<BookmarkEntity>,
    selectedReciter: ReciterInfo,
    downloadsState: Map<String, SurahDownloadState>,
    offlineSurahIds: Set<Int>,
    totalAudioBytes: Long,
    getDownloadState: (String, SurahInfo) -> SurahDownloadState,
    onSearchQueryChange: (String) -> Unit,
    onOpenSurah: (surahId: Int, targetAyah: Int) -> Unit,
    onRemoveBookmark: (surahId: Int, ayahNumber: Int) -> Unit,
    onDownloadSurahAudio: (ReciterInfo, SurahInfo) -> Unit,
    onCancelSurahDownload: (String, Int) -> Unit,
    onDeleteSurahAudio: (String, Int) -> Unit,
    onOpenOfflineManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("السور", "الأجزاء", "المحفوظات")

    Column(modifier = modifier.fillMaxSize()) {
        EmeraldOrnamentalHeader {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "القرآن الكريم",
                        fontFamily = AmiriFontFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = NoorTheme.colors.onHeaderForest
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "١١٤ سورة • ٣٠ جزءاً • التفسير الميسر",
                        style = MaterialTheme.typography.bodySmall,
                        color = NoorTheme.colors.goldAccent
                    )
                }
                Surface(
                    onClick = onOpenOfflineManager,
                    shape = RoundedCornerShape(999.dp),
                    color = NoorTheme.colors.cardElevated.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, NoorTheme.colors.goldAccent.copy(alpha = 0.7f)),
                    modifier = Modifier.testTag("quran_offline_manager_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = "إدارة التنزيلات",
                            tint = NoorTheme.colors.goldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "الأوفلاين (${ArabicTextUtils.toEasternArabicNumerals(offlineSurahIds.size)}/١١٤)",
                            style = MaterialTheme.typography.labelMedium,
                            color = NoorTheme.colors.onHeaderForest,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quran_search_input"),
                placeholder = {
                    Text(
                        text = "ابحث عن سورة أو آية (بدون تشكيل)...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "بحث",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "مسح البحث"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedBorderColor = NoorTheme.colors.goldAccent,
                    unfocusedBorderColor = NoorTheme.colors.goldBorder
                )
            )
        }

        if (searchQuery.isBlank()) {
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        modifier = Modifier.testTag("quran_tab_$index")
                    )
                }
            }
        }

        if (searchQuery.isNotBlank()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (searchResults.isEmpty()) {
                    item {
                        EmptyQuranState(
                            title = "لا توجد نتائج مطابقة",
                            subtitle = "جرّب البحث باسم السورة أو كلمة من الآيات، وحمّل المصحف كاملاً للبحث في كل السور"
                        )
                    }
                } else {
                    items(searchResults) { match ->
                        if (match.ayahNumber == null) {
                            val dlState = remember(downloadsState, selectedReciter.id, match.surah.id) {
                                getDownloadState(selectedReciter.id, match.surah)
                            }
                            SurahIndexCard(
                                match = match,
                                isTextOffline = offlineSurahIds.contains(match.surah.id),
                                downloadState = dlState,
                                onClick = { onOpenSurah(match.surah.id, 1) },
                                onDownload = { onDownloadSurahAudio(selectedReciter, match.surah) },
                                onCancel = { onCancelSurahDownload(selectedReciter.id, match.surah.id) },
                                onDelete = { onDeleteSurahAudio(selectedReciter.id, match.surah.id) }
                            )
                        } else {
                            NoorGoldCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpenSurah(match.surah.id, match.ayahNumber) }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        EmeraldPillBadge(text = "سورة ${match.surah.name}")
                                        Text(
                                            text = "آية ${ArabicTextUtils.toEasternArabicNumerals(match.ayahNumber)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = match.snippet.orEmpty(),
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 20.sp,
                                        lineHeight = 36.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            NoorGoldCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quran_offline_manager_banner"),
                                onClick = onOpenOfflineManager
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
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CloudDownload,
                                            contentDescription = null,
                                            tint = NoorTheme.colors.goldAccent
                                        )
                                        Column {
                                            Text(
                                                text = "المكتبة الأوفلاين وإدارة المساحة",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "المصحف: ${ArabicTextUtils.toEasternArabicNumerals(offlineSurahIds.size)} / ١١٤ سورة • الصوتيات: ${formatBytesArabic(totalAudioBytes)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    EmeraldPillBadge(text = "إدارة")
                                }
                            }
                        }

                        item {
                            Surface(
                                onClick = {
                                    ExternalUrlHelper.openVerificationUrl(context, "https://tanzil.net/")
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = NoorTheme.colors.badgeEmeraldBg.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, NoorTheme.colors.goldBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quran_index_source_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "مصدر المصحف والتلاوات: مجمع الملك فهد وتنزيل",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "القارئ الحالي للتنزيل: ${selectedReciter.nameArabic}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
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
                                            contentDescription = "فتح مصدر المصحف",
                                            tint = NoorTheme.colors.goldAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        items(searchResults, key = { it.surah.id }) { match ->
                            val dlState = remember(downloadsState, selectedReciter.id, match.surah.id) {
                                getDownloadState(selectedReciter.id, match.surah)
                            }
                            SurahIndexCard(
                                match = match,
                                isTextOffline = offlineSurahIds.contains(match.surah.id),
                                downloadState = dlState,
                                onClick = { onOpenSurah(match.surah.id, 1) },
                                onDownload = { onDownloadSurahAudio(selectedReciter, match.surah) },
                                onCancel = { onCancelSurahDownload(selectedReciter.id, match.surah.id) },
                                onDelete = { onDeleteSurahAudio(selectedReciter.id, match.surah.id) }
                            )
                        }
                    }
                }

                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(juzList, key = { it.id }) { juz ->
                            NoorGoldCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("juz_card_${juz.id}"),
                                onClick = { onOpenSurah(juz.surahId, juz.startAyah) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    KhatamStarBadge(number = juz.id)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = juz.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "يبدأ من سورة ${juz.surahName} — آية ${ArabicTextUtils.toEasternArabicNumerals(juz.startAyah)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    EmeraldPillBadge(text = "الجزء ${ArabicTextUtils.toEasternArabicNumerals(juz.id)}")
                                }
                            }
                        }
                    }
                }

                2 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (bookmarks.isEmpty()) {
                            item {
                                EmptyQuranState(
                                    title = "لا توجد آيات محفوظة بعد",
                                    subtitle = "اضغط على أي آية أثناء القراءة لحفظها كعلامة مرجعية"
                                )
                            }
                        } else {
                            items(bookmarks, key = { it.key }) { bm ->
                                NoorGoldCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("bookmark_card_${bm.key}"),
                                    onClick = { onOpenSurah(bm.surahId, bm.ayahNumber) }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
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
                                                    imageVector = Icons.Filled.Bookmark,
                                                    contentDescription = null,
                                                    tint = NoorTheme.colors.goldAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                EmeraldPillBadge(
                                                    text = "سورة ${bm.surahName} • آية ${ArabicTextUtils.toEasternArabicNumerals(bm.ayahNumber)}"
                                                )
                                            }
                                            IconButton(
                                                onClick = { onRemoveBookmark(bm.surahId, bm.ayahNumber) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DeleteOutline,
                                                    contentDescription = "حذف العلامة",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = bm.ayahText,
                                            fontFamily = AmiriFontFamily,
                                            fontSize = 20.sp,
                                            lineHeight = 36.sp,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
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
private fun SurahIndexCard(
    match: QuranSearchMatch,
    isTextOffline: Boolean,
    downloadState: SurahDownloadState,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val surah = match.surah
    NoorGoldCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("surah_card_${surah.id}"),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KhatamStarBadge(number = surah.id)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "سورة ${surah.name}",
                        fontFamily = AmiriFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isTextOffline) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "النص والتفسير متاحان بدون نت",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                val subtitle = if (downloadState.status == DownloadStatus.DOWNLOADING) {
                    "جارٍ التحميل: ${ArabicTextUtils.toEasternArabicNumerals(downloadState.completedAyahs)}/${ArabicTextUtils.toEasternArabicNumerals(surah.ayahCount)}"
                } else if (downloadState.status == DownloadStatus.COMPLETED) {
                    "${surah.revelationType} • ${ArabicTextUtils.toEasternArabicNumerals(surah.ayahCount)} آية • صوت محمّل ✓"
                } else {
                    "${surah.revelationType} • ${ArabicTextUtils.toEasternArabicNumerals(surah.ayahCount)} آية"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (downloadState.status == DownloadStatus.COMPLETED) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EmeraldPillBadge(text = "ج ${ArabicTextUtils.toEasternArabicNumerals(surah.juz)}")
                when (downloadState.status) {
                    DownloadStatus.IDLE, DownloadStatus.ERROR -> {
                        IconButton(
                            onClick = onDownload,
                            modifier = Modifier.testTag("index_download_surah_${surah.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = "تحميل تلاوة سورة ${surah.name} بدون نت",
                                tint = NoorTheme.colors.goldAccent
                            )
                        }
                    }
                    DownloadStatus.DOWNLOADING -> {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.testTag("index_cancel_surah_${surah.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = {
                                        val t = downloadState.totalAyahs.coerceAtLeast(1)
                                        downloadState.completedAyahs.toFloat() / t.toFloat()
                                    },
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "إلغاء التحميل",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("index_downloaded_surah_${surah.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDone,
                                contentDescription = "محملة بدون نت — اضغط لحذف التلاوة",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyQuranState(title: String, subtitle: String) {
    NoorGoldCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                tint = NoorTheme.colors.goldAccent,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
