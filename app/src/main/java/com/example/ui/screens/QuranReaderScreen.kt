package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlaybackState
import com.example.audio.DownloadStatus
import com.example.audio.SurahDownloadState
import com.example.data.local.BookmarkEntity
import com.example.data.local.MushafViewModes
import com.example.data.local.UserSettings
import com.example.data.model.AyahItem
import com.example.data.model.MushafPageCatalog
import com.example.data.model.ReciterInfo
import com.example.data.model.RecitersCatalog
import com.example.data.model.SourcesCatalog
import com.example.data.model.SurahContent
import com.example.data.model.SurahInfo
import com.example.ui.components.EmeraldPillBadge
import com.example.ui.components.NoorGoldCard
import com.example.ui.components.SurahOrnamentBanner
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorTheme
import com.example.util.ArabicTextUtils
import com.example.util.ExternalUrlHelper
import kotlinx.coroutines.launch

@Composable
fun QuranReaderScreen(
    surahId: Int,
    targetAyah: Int,
    content: SurahContent?,
    allSurahs: List<SurahInfo>,
    settings: UserSettings,
    bookmarks: List<BookmarkEntity>,
    audioState: AudioPlaybackState,
    downloadsState: Map<String, SurahDownloadState>,
    getDownloadState: (String, SurahInfo) -> SurahDownloadState,
    onBack: () -> Unit,
    onLoadSurah: (Int) -> Unit,
    onSaveLastRead: (SurahInfo, Int) -> Unit,
    onToggleBookmark: (SurahInfo, AyahItem) -> Unit,
    onAdjustFontSize: (Float) -> Unit,
    onSelectViewMode: (String) -> Unit,
    onPlayAyah: (SurahInfo, Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextAyah: () -> Unit,
    onPreviousAyah: () -> Unit,
    onStopAudio: () -> Unit,
    onSelectReciter: (ReciterInfo) -> Unit,
    onConfigureRangeRepeat: (Boolean, Int, Int, Int) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetAutoAdvanceNextSurah: (Boolean) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onDownloadSurahAudio: (ReciterInfo, SurahInfo) -> Unit,
    onCancelSurahDownload: (String, Int) -> Unit,
    onDeleteSurahAudio: (String, Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedAyahForMenu by remember { mutableStateOf<AyahItem?>(null) }
    var showAudioConfigSheet by remember { mutableStateOf(false) }
    var showFontBar by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val viewMode = settings.mushafViewMode

    LaunchedEffect(surahId) {
        onLoadSurah(surahId)
    }

    LaunchedEffect(content?.ayahs, targetAyah, viewMode) {
        if (content != null && content.ayahs.isNotEmpty()) {
            onSaveLastRead(content.surahInfo, targetAyah)
            if (viewMode == MushafViewModes.LIST && targetAyah > 1) {
                val idx = content.ayahs.indexOfFirst { it.number >= targetAyah }
                if (idx >= 0) {
                    listState.scrollToItem(idx + 1)
                }
            }
        }
    }

    val surahInfo = content?.surahInfo
    val bookmarkedKeys = remember(bookmarks) { bookmarks.map { it.key }.toSet() }
    val currentDownloadState = remember(downloadsState, audioState.reciter.id, surahInfo) {
        if (surahInfo != null) getDownloadState(audioState.reciter.id, surahInfo) else SurahDownloadState()
    }

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
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("reader_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "رجوع",
                                    tint = NoorTheme.colors.onHeaderForest
                                )
                            }
                            Column {
                                Text(
                                    text = if (surahInfo != null) "سورة ${surahInfo.name}" else "القرآن الكريم",
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NoorTheme.colors.onHeaderForest
                                )
                                if (surahInfo != null) {
                                    Text(
                                        text = "${surahInfo.revelationType} • ${ArabicTextUtils.toEasternArabicNumerals(surahInfo.ayahCount)} آية",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NoorTheme.colors.goldAccent
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showFontBar = !showFontBar },
                                modifier = Modifier.testTag("reader_font_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FormatSize,
                                    contentDescription = "حجم الخط",
                                    tint = NoorTheme.colors.onHeaderForest
                                )
                            }
                            IconButton(
                                onClick = { showAudioConfigSheet = true },
                                modifier = Modifier.testTag("reader_audio_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = "القارئ وتكرار الحفظ",
                                    tint = NoorTheme.colors.goldAccent
                                )
                            }
                        }
                    }

                    // 3-Mode Mushaf Bar (قائمة | متصل | صفحات ٦٠٤)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            MushafViewModes.LIST to "قائمة",
                            MushafViewModes.FLOW to "مصحف متصل",
                            MushafViewModes.PAGES to "صفحات (٦٠٤)"
                        ).forEach { (modeKey, modeLabel) ->
                            val selected = viewMode == modeKey
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectViewMode(modeKey) },
                                label = { Text(modeLabel, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NoorTheme.colors.goldAccent,
                                    selectedLabelColor = NoorTheme.colors.headerForest,
                                    containerColor = NoorTheme.colors.onHeaderForest.copy(alpha = 0.14f),
                                    labelColor = NoorTheme.colors.onHeaderForest
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reader_mode_$modeKey")
                            )
                        }
                    }

                    AnimatedVisibility(visible = showFontBar) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NoorTheme.colors.headerForest.copy(alpha = 0.9f))
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "حجم الخط: ${ArabicTextUtils.toEasternArabicNumerals(settings.quranFontSizeSp.toInt())}",
                                style = MaterialTheme.typography.labelLarge,
                                color = NoorTheme.colors.onHeaderForest
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onAdjustFontSize(-2f) },
                                    border = BorderStroke(1.dp, NoorTheme.colors.goldAccent)
                                ) {
                                    Text("أ-", color = NoorTheme.colors.onHeaderForest)
                                }
                                OutlinedButton(
                                    onClick = { onAdjustFontSize(2f) },
                                    border = BorderStroke(1.dp, NoorTheme.colors.goldAccent)
                                ) {
                                    Text("أ+", color = NoorTheme.colors.onHeaderForest)
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (audioState.isActive) {
                AudioMiniPlayerBar(
                    audioState = audioState,
                    onTogglePlayPause = onTogglePlayPause,
                    onNext = onNextAyah,
                    onPrevious = onPreviousAyah,
                    onOpenAudioConfig = { showAudioConfigSheet = true },
                    onStop = onStopAudio
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                content == null || content.isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "جارٍ تحميل السورة الكريمة...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                content.ayahs.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "هذه السورة غير محملة محلياً بعد",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "اتصل بالإنترنت لمرة واحدة لتحميل نص السورة والتفسير الميسر وحفظها للاستخدام الدائم بدون إنترنت.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { onLoadSurah(surahId) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("إعادة المحاولة")
                        }
                    }
                }

                viewMode == MushafViewModes.PAGES -> {
                    val pageGroups = remember(content, allSurahs) {
                        MushafPageCatalog.computePageGroupsForSurah(content.surahInfo, content.ayahs, allSurahs)
                    }
                    val initialIdx = remember(pageGroups, targetAyah) {
                        MushafPageCatalog.findPageIndexForAyah(pageGroups, targetAyah)
                    }
                    val pagerState = rememberPagerState(
                        initialPage = initialIdx,
                        pageCount = { pageGroups.size.coerceAtLeast(1) }
                    )

                    val primaryColor = MaterialTheme.colorScheme.primary
                    val goldColor = NoorTheme.colors.goldAccent
                    val activeBg = NoorTheme.colors.activeAyahHighlight
                    val onSurface = MaterialTheme.colorScheme.onSurface

                    Column(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("mushaf_horizontal_pager")
                        ) { pageIdx ->
                            val pageGroup = pageGroups[pageIdx]
                            val pageScroll = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(pageScroll)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = NoorTheme.colors.badgeEmeraldBg,
                                    border = BorderStroke(1.dp, NoorTheme.colors.goldBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = MushafPageCatalog.hizbHeaderText(pageGroup.pageNumber),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "صفحة ${ArabicTextUtils.toEasternArabicNumerals(pageGroup.pageNumber)} / ٦٠٤",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (pageGroup.containsSajdah) {
                                    EmeraldPillBadge(text = "سجدة تلاوة ۩")
                                }

                                if (pageIdx == 0) {
                                    SurahOrnamentBanner(
                                        surahName = content.surahInfo.name,
                                        revelationType = content.surahInfo.revelationType,
                                        ayahCount = content.surahInfo.ayahCount
                                    )
                                    if (content.surahInfo.id != 1 && content.surahInfo.id != 9) {
                                        BasmalahHeader()
                                    }
                                    if (content.offlineNotice) {
                                        OfflinePartialNoticeBanner(onRetry = { onLoadSurah(surahId) })
                                    }
                                }

                                val pageAnnotated = buildAnnotatedString {
                                    pageGroup.ayahs.forEach { ayah ->
                                        val isPlayingThis = audioState.isActive &&
                                            audioState.surahId == content.surahInfo.id &&
                                            audioState.currentAyah == ayah.number
                                        val isBookmarked = bookmarkedKeys.contains("${content.surahInfo.id}:${ayah.number}")
                                        val isSajdah = MushafPageCatalog.isSajdahVerse(content.surahInfo.id, ayah.number)

                                        withLink(
                                            LinkAnnotation.Clickable(
                                                tag = "page_ayah_${ayah.number}",
                                                styles = TextLinkStyles(
                                                    style = SpanStyle(
                                                        background = if (isPlayingThis || isBookmarked) activeBg else androidx.compose.ui.graphics.Color.Transparent
                                                    )
                                                )
                                            ) {
                                                onSaveLastRead(content.surahInfo, ayah.number)
                                                selectedAyahForMenu = ayah
                                            }
                                        ) {
                                            withStyle(SpanStyle(color = onSurface)) {
                                                append(ayah.text)
                                            }
                                            if (isSajdah) {
                                                withStyle(SpanStyle(color = goldColor, fontWeight = FontWeight.Bold)) {
                                                    append(" ۩")
                                                }
                                            }
                                            withStyle(
                                                SpanStyle(
                                                    color = if (isBookmarked) goldColor else primaryColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            ) {
                                                append(ArabicTextUtils.formatAyahEndMarker(ayah.number))
                                                append(" ")
                                            }
                                        }
                                    }
                                }

                                NoorGoldCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    borderColor = NoorTheme.colors.goldAccent
                                ) {
                                    Text(
                                        text = pageAnnotated,
                                        modifier = Modifier.padding(18.dp),
                                        fontFamily = AmiriFontFamily,
                                        fontSize = settings.quranFontSizeSp.sp,
                                        lineHeight = (settings.quranFontSizeSp * 2.15f).sp,
                                        textAlign = TextAlign.Justify
                                    )
                                }
                            }
                        }

                        if (pageGroups.size > 1) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (pagerState.currentPage > 0) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                }
                                            }
                                        },
                                        enabled = pagerState.currentPage > 0
                                    ) {
                                        Text("الصفحة السابقة")
                                    }
                                    Text(
                                        text = "${ArabicTextUtils.toEasternArabicNumerals(pagerState.currentPage + 1)} / ${ArabicTextUtils.toEasternArabicNumerals(pageGroups.size)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            if (pagerState.currentPage < pageGroups.lastIndex) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                }
                                            }
                                        },
                                        enabled = pagerState.currentPage < pageGroups.lastIndex
                                    ) {
                                        Text("الصفحة التالية")
                                    }
                                }
                            }
                        }
                    }
                }

                viewMode == MushafViewModes.FLOW -> {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SurahOrnamentBanner(
                            surahName = content.surahInfo.name,
                            revelationType = content.surahInfo.revelationType,
                            ayahCount = content.surahInfo.ayahCount
                        )

                        if (content.surahInfo.id != 1 && content.surahInfo.id != 9) {
                            BasmalahHeader()
                        }

                        SurahOfflineAudioCard(
                            surah = content.surahInfo,
                            reciter = audioState.reciter,
                            downloadState = currentDownloadState,
                            onDownload = { onDownloadSurahAudio(audioState.reciter, content.surahInfo) },
                            onCancel = { onCancelSurahDownload(audioState.reciter.id, content.surahInfo.id) },
                            onDelete = { onDeleteSurahAudio(audioState.reciter.id, content.surahInfo.id) },
                            onPlayFirst = { onPlayAyah(content.surahInfo, 1) }
                        )

                        if (content.offlineNotice) {
                            OfflinePartialNoticeBanner(onRetry = { onLoadSurah(surahId) })
                        }

                        val primaryColor = MaterialTheme.colorScheme.primary
                        val goldColor = NoorTheme.colors.goldAccent
                        val activeBg = NoorTheme.colors.activeAyahHighlight
                        val onSurface = MaterialTheme.colorScheme.onSurface

                        val mushafAnnotated = buildAnnotatedString {
                            content.ayahs.forEach { ayah ->
                                val isPlayingThis = audioState.isActive &&
                                    audioState.surahId == content.surahInfo.id &&
                                    audioState.currentAyah == ayah.number
                                val isBookmarked = bookmarkedKeys.contains("${content.surahInfo.id}:${ayah.number}")
                                val isSajdah = MushafPageCatalog.isSajdahVerse(content.surahInfo.id, ayah.number)

                                withLink(
                                    LinkAnnotation.Clickable(
                                        tag = "ayah_${ayah.number}",
                                        styles = TextLinkStyles(
                                            style = SpanStyle(
                                                background = if (isPlayingThis || isBookmarked) activeBg else androidx.compose.ui.graphics.Color.Transparent
                                            )
                                        )
                                    ) {
                                        onSaveLastRead(content.surahInfo, ayah.number)
                                        selectedAyahForMenu = ayah
                                    }
                                ) {
                                    withStyle(SpanStyle(color = onSurface)) {
                                        append(ayah.text)
                                    }
                                    if (isSajdah) {
                                        withStyle(SpanStyle(color = goldColor, fontWeight = FontWeight.Bold)) {
                                            append(" ۩")
                                        }
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = if (isBookmarked) goldColor else primaryColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append(ArabicTextUtils.formatAyahEndMarker(ayah.number))
                                        append(" ")
                                    }
                                }
                            }
                        }

                        NoorGoldCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = mushafAnnotated,
                                modifier = Modifier.padding(18.dp),
                                fontFamily = AmiriFontFamily,
                                fontSize = settings.quranFontSizeSp.sp,
                                lineHeight = (settings.quranFontSizeSp * 2.15f).sp,
                                textAlign = TextAlign.Justify
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SurahOrnamentBanner(
                                    surahName = content.surahInfo.name,
                                    revelationType = content.surahInfo.revelationType,
                                    ayahCount = content.surahInfo.ayahCount
                                )
                                if (content.surahInfo.id != 1 && content.surahInfo.id != 9) {
                                    BasmalahHeader()
                                }
                                SurahOfflineAudioCard(
                                    surah = content.surahInfo,
                                    reciter = audioState.reciter,
                                    downloadState = currentDownloadState,
                                    onDownload = { onDownloadSurahAudio(audioState.reciter, content.surahInfo) },
                                    onCancel = { onCancelSurahDownload(audioState.reciter.id, content.surahInfo.id) },
                                    onDelete = { onDeleteSurahAudio(audioState.reciter.id, content.surahInfo.id) },
                                    onPlayFirst = { onPlayAyah(content.surahInfo, 1) }
                                )
                                if (content.offlineNotice) {
                                    OfflinePartialNoticeBanner(onRetry = { onLoadSurah(surahId) })
                                }
                            }
                        }

                        items(content.ayahs, key = { it.number }) { ayah ->
                            val isBookmarked = bookmarkedKeys.contains("${content.surahInfo.id}:${ayah.number}")
                            val isPlayingThis = audioState.isActive &&
                                audioState.surahId == content.surahInfo.id &&
                                audioState.currentAyah == ayah.number
                            val isLastRead = settings.lastReadSurahId == content.surahInfo.id &&
                                settings.lastReadAyahNumber == ayah.number
                            val isSajdah = MushafPageCatalog.isSajdahVerse(content.surahInfo.id, ayah.number)

                            AyahCard(
                                ayah = ayah,
                                fontSizeSp = settings.quranFontSizeSp,
                                isBookmarked = isBookmarked,
                                isPlaying = isPlayingThis,
                                isLastRead = isLastRead,
                                isSajdah = isSajdah,
                                onClick = {
                                    onSaveLastRead(content.surahInfo, ayah.number)
                                    selectedAyahForMenu = ayah
                                },
                                onPlay = { onPlayAyah(content.surahInfo, ayah.number) },
                                onToggleBookmark = { onToggleBookmark(content.surahInfo, ayah) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Ayah Interactive Menu & Tafsir Bottom Sheet
    val activeMenuAyah = selectedAyahForMenu
    if (activeMenuAyah != null && surahInfo != null) {
        AyahMenuBottomSheet(
            surah = surahInfo,
            ayah = activeMenuAyah,
            isBookmarked = bookmarkedKeys.contains("${surahInfo.id}:${activeMenuAyah.number}"),
            onDismiss = { selectedAyahForMenu = null },
            onPlay = {
                onPlayAyah(surahInfo, activeMenuAyah.number)
                selectedAyahForMenu = null
            },
            onToggleBookmark = {
                onToggleBookmark(surahInfo, activeMenuAyah)
            },
            onSetLastRead = {
                onSaveLastRead(surahInfo, activeMenuAyah.number)
                Toast.makeText(context, "تم الحفظ كآخر موضع قراءة", Toast.LENGTH_SHORT).show()
                selectedAyahForMenu = null
            },
            onCopy = {
                val copyText = "${activeMenuAyah.text} ﴿${surahInfo.name}: ${ArabicTextUtils.toEasternArabicNumerals(activeMenuAyah.number)}﴾"
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Ayah", copyText))
                Toast.makeText(context, "تم نسخ الآية", Toast.LENGTH_SHORT).show()
                selectedAyahForMenu = null
            },
            onShare = {
                val shareText = "${activeMenuAyah.text}\n[سورة ${surahInfo.name} - الآية ${ArabicTextUtils.toEasternArabicNumerals(activeMenuAyah.number)}]"
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(sendIntent, "مشاركة الآية"))
                selectedAyahForMenu = null
            }
        )
    }

    // Reciter & Memorization Range Repeat Bottom Sheet
    if (showAudioConfigSheet && surahInfo != null) {
        AudioConfigSheet(
            surah = surahInfo,
            audioState = audioState,
            downloadState = currentDownloadState,
            onDismiss = { showAudioConfigSheet = false },
            onSelectReciter = onSelectReciter,
            onSetPlaybackSpeed = onSetPlaybackSpeed,
            onSetAutoAdvanceNextSurah = onSetAutoAdvanceNextSurah,
            onSetSleepTimer = onSetSleepTimer,
            onDownloadSurahAudio = { reciter -> onDownloadSurahAudio(reciter, surahInfo) },
            onCancelSurahDownload = { reciterId -> onCancelSurahDownload(reciterId, surahInfo.id) },
            onDeleteSurahAudio = { reciterId -> onDeleteSurahAudio(reciterId, surahInfo.id) },
            onStartRangePlayback = { enabled, start, end, repeatTarget ->
                onConfigureRangeRepeat(enabled, start, end, repeatTarget)
                onPlayAyah(surahInfo, if (enabled) start else audioState.currentAyah.coerceIn(1, surahInfo.ayahCount))
                showAudioConfigSheet = false
            }
        )
    }
}

@Composable
private fun BasmalahHeader() {
    Text(
        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        fontFamily = AmiriFontFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun OfflinePartialNoticeBanner(onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, NoorTheme.colors.goldBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "تُعرض الآيات المختارة بدون إنترنت. اتصل لتحميل السورة كاملة.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text("تحديث")
            }
        }
    }
}

@Composable
private fun AyahCard(
    ayah: AyahItem,
    fontSizeSp: Float,
    isBookmarked: Boolean,
    isPlaying: Boolean,
    isLastRead: Boolean,
    isSajdah: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    val containerColor = when {
        isPlaying -> NoorTheme.colors.activeAyahHighlight
        else -> NoorTheme.colors.cardElevated
    }
    val borderColor = if (isPlaying || isBookmarked) NoorTheme.colors.goldAccent else NoorTheme.colors.goldBorder

    NoorGoldCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ayah_card_${ayah.number}"),
        onClick = onClick,
        containerColor = containerColor,
        borderColor = borderColor
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EmeraldPillBadge(text = "الآية ${ArabicTextUtils.toEasternArabicNumerals(ayah.number)}")
                    if (isSajdah) {
                        EmeraldPillBadge(text = "سجدة تلاوة ۩")
                    }
                    if (isLastRead) {
                        EmeraldPillBadge(text = "آخر قراءة")
                    }
                }
                Row {
                    IconButton(onClick = onPlay) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "تلاوة الآية",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "حفظ العلامة",
                            tint = if (isBookmarked) NoorTheme.colors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${ayah.text}${ArabicTextUtils.formatAyahEndMarker(ayah.number)}",
                fontFamily = AmiriFontFamily,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 2.0f).sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AyahMenuBottomSheet(
    surah: SurahInfo,
    ayah: AyahItem,
    isBookmarked: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleBookmark: () -> Unit,
    onSetLastRead: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EmeraldPillBadge(
                    text = "سورة ${surah.name} • الآية ${ArabicTextUtils.toEasternArabicNumerals(ayah.number)}"
                )
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "علامة مرجعية",
                        tint = if (isBookmarked) NoorTheme.colors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "${ayah.text}${ArabicTextUtils.formatAyahEndMarker(ayah.number)}",
                fontFamily = AmiriFontFamily,
                fontSize = 24.sp,
                lineHeight = 46.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            SourceVerificationLinkChip(
                label = "التحقق من نص الآية (Quran.com)",
                onClick = {
                    ExternalUrlHelper.openVerificationUrl(
                        context,
                        SourcesCatalog.quranAyahReferenceUrl(surah.id, ayah.number)
                    )
                },
                modifier = Modifier.testTag("ayah_quran_source_link")
            )

            HorizontalDivider(color = NoorTheme.colors.goldBorder)

            Text(
                text = "التفسير الميسر",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = ayah.tafsir.ifBlank { "التفسير الميسر غير متوفر لهذه الآية حالياً." },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SourceVerificationLinkChip(
                label = "المصدر: التفسير الميسر — مجمع الملك فهد (تحقق)",
                onClick = {
                    ExternalUrlHelper.openVerificationUrl(
                        context,
                        SourcesCatalog.muyassarTafsirUrl(surah.id, ayah.number)
                    )
                },
                modifier = Modifier.testTag("ayah_tafsir_source_link")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("استماع")
                }
                OutlinedButton(
                    onClick = onSetLastRead,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("آخر قراءة")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("نسخ")
                }
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("مشاركة")
                }
            }
        }
    }
}

@Composable
private fun AudioMiniPlayerBar(
    audioState: AudioPlaybackState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenAudioConfig: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        color = NoorTheme.colors.headerForest,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, NoorTheme.colors.goldAccent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (audioState.errorMessage != null) {
                Text(
                    text = audioState.errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = NoorTheme.colors.goldAccent,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenAudioConfig() }
                ) {
                    Text(
                        text = "سورة ${audioState.surahName} • آية ${ArabicTextUtils.toEasternArabicNumerals(audioState.currentAyah)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = NoorTheme.colors.onHeaderForest
                    )
                    val subtitle = buildString {
                        append(audioState.reciter.nameArabic)
                        if (audioState.isPlayingOffline) {
                            append(" • بدون نت ✓")
                        }
                        if (audioState.playbackSpeed != 1.0f) {
                            append(" • ${audioState.playbackSpeed}x")
                        }
                        if (audioState.sleepTimerRemainingSec > 0) {
                            val mins = audioState.sleepTimerRemainingSec / 60
                            val secs = audioState.sleepTimerRemainingSec % 60
                            val mm = ArabicTextUtils.toEasternArabicNumerals(mins).padStart(2, '٠')
                            val ss = ArabicTextUtils.toEasternArabicNumerals(secs).padStart(2, '٠')
                            append(" • مؤقت $mm:$ss")
                        }
                        if (audioState.isRangeRepeatEnabled) {
                            append(" • تكرار (${ArabicTextUtils.toEasternArabicNumerals(audioState.rangeStartAyah)}-${ArabicTextUtils.toEasternArabicNumerals(audioState.rangeEndAyah)})")
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = NoorTheme.colors.goldAccent
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevious) {
                        Icon(Icons.Filled.SkipPrevious, "السابقة", tint = NoorTheme.colors.onHeaderForest)
                    }
                    Surface(
                        shape = CircleShape,
                        color = NoorTheme.colors.goldAccent,
                        modifier = Modifier.size(42.dp)
                    ) {
                        IconButton(onClick = onTogglePlayPause) {
                            if (audioState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = NoorTheme.colors.headerForest
                                )
                            } else {
                                Icon(
                                    imageVector = if (audioState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (audioState.isPlaying) "إيقاف مؤقت" else "تشغيل",
                                    tint = NoorTheme.colors.headerForest
                                )
                            }
                        }
                    }
                    IconButton(onClick = onNext) {
                        Icon(Icons.Filled.SkipNext, "التالية", tint = NoorTheme.colors.onHeaderForest)
                    }
                    IconButton(onClick = onStop) {
                        Icon(Icons.Filled.Close, "إغلاق المشغل", tint = NoorTheme.colors.onHeaderForest)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioConfigSheet(
    surah: SurahInfo,
    audioState: AudioPlaybackState,
    downloadState: SurahDownloadState,
    onDismiss: () -> Unit,
    onSelectReciter: (ReciterInfo) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetAutoAdvanceNextSurah: (Boolean) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onDownloadSurahAudio: (ReciterInfo) -> Unit,
    onCancelSurahDownload: (String) -> Unit,
    onDeleteSurahAudio: (String) -> Unit,
    onStartRangePlayback: (Boolean, Int, Int, Int) -> Unit
) {
    val context = LocalContext.current
    var rangeEnabled by remember { mutableStateOf(audioState.isRangeRepeatEnabled) }
    var startAyah by remember { mutableIntStateOf(audioState.rangeStartAyah.coerceIn(1, surah.ayahCount)) }
    var endAyah by remember { mutableIntStateOf(audioState.rangeEndAyah.coerceIn(startAyah, surah.ayahCount)) }
    var repeatTarget by remember { mutableIntStateOf(audioState.repeatCountTarget) }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "التلاوة والتحفيظ",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "اختر القارئ",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecitersCatalog.reciters.forEach { reciter ->
                    val selected = reciter.id == audioState.reciter.id
                    NoorGoldCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelectReciter(reciter) },
                        borderColor = if (selected) NoorTheme.colors.goldAccent else NoorTheme.colors.goldBorder,
                        containerColor = if (selected) NoorTheme.colors.badgeEmeraldBg else NoorTheme.colors.cardElevated
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = reciter.nameArabic,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                            EmeraldPillBadge(text = reciter.style)
                        }
                    }
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
                    Icon(Icons.Filled.Repeat, null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("تكرار نطاق آيات للحفظ", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "كرّر مقطعاً محدداً لتثبيت الحفظ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(checked = rangeEnabled, onCheckedChange = { rangeEnabled = it })
            }

            if (rangeEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AyahStepperBox(
                        label = "من الآية",
                        value = startAyah,
                        onDecrement = { if (startAyah > 1) startAyah-- },
                        onIncrement = {
                            if (startAyah < surah.ayahCount) {
                                startAyah++
                                if (endAyah < startAyah) endAyah = startAyah
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    AyahStepperBox(
                        label = "إلى الآية",
                        value = endAyah,
                        onDecrement = { if (endAyah > startAyah) endAyah-- },
                        onIncrement = { if (endAyah < surah.ayahCount) endAyah++ },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("عدد مرات التكرار", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3 to "٣", 5 to "٥", 10 to "١٠", 0 to "مستمر ∞").forEach { (count, label) ->
                        FilterChip(
                            selected = repeatTarget == count,
                            onClick = { repeatTarget = count },
                            label = { Text(label) }
                        )
                    }
                }
            }

            HorizontalDivider(color = NoorTheme.colors.goldBorder)

            // Pro Playback Controls: Speed, Sleep Timer & Auto-Advance Next Surah
            Text(
                text = "سرعة التلاوة",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0.75f to "٠.٧٥x",
                    0.85f to "٠.٨٥x",
                    1.0f to "١.٠x"
                ).forEach { (spd, label) ->
                    FilterChip(
                        selected = kotlin.math.abs(audioState.playbackSpeed - spd) < 0.05f,
                        onClick = { onSetPlaybackSpeed(spd) },
                        label = { Text(label) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("audio_speed_$spd")
                    )
                }
            }

            Text(
                text = if (audioState.sleepTimerRemainingSec > 0) {
                    val m = audioState.sleepTimerRemainingSec / 60
                    val s = audioState.sleepTimerRemainingSec % 60
                    "مؤقت النوم التلقائي (${ArabicTextUtils.toEasternArabicNumerals(m)}:${ArabicTextUtils.toEasternArabicNumerals(s).padStart(2, '٠')})"
                } else {
                    "مؤقت النوم التلقائي"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to "إيقاف",
                    15 to "١٥ د",
                    30 to "٣٠ د",
                    60 to "٦٠ د"
                ).forEach { (mins, label) ->
                    FilterChip(
                        selected = audioState.sleepTimerMinutes == mins,
                        onClick = { onSetSleepTimer(mins) },
                        label = { Text(label) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("audio_sleep_$mins")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("الانتقال التلقائي للسورة التالية", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "استمرار التلاوة المتصلة عند انتهاء السورة الحالية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = audioState.autoAdvanceNextSurah,
                    onCheckedChange = onSetAutoAdvanceNextSurah,
                    modifier = Modifier.testTag("audio_auto_advance_switch")
                )
            }

            HorizontalDivider(color = NoorTheme.colors.goldBorder)

            SurahOfflineAudioCard(
                surah = surah,
                reciter = audioState.reciter,
                downloadState = downloadState,
                onDownload = { onDownloadSurahAudio(audioState.reciter) },
                onCancel = { onCancelSurahDownload(audioState.reciter.id) },
                onDelete = { onDeleteSurahAudio(audioState.reciter.id) },
                onPlayFirst = { onStartRangePlayback(false, 1, surah.ayahCount, 0) }
            )

            SourceVerificationLinkChip(
                label = "مصدر التلاوات الصوتية: مشروع EveryAyah",
                onClick = {
                    ExternalUrlHelper.openVerificationUrl(context, "https://everyayah.com/")
                },
                modifier = Modifier.testTag("audio_everyayah_source_link")
            )

            Button(
                onClick = { onStartRangePlayback(rangeEnabled, startAyah, endAyah, repeatTarget) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (rangeEnabled) "تشغيل التكرار الآن" else "تشغيل التلاوة")
            }
        }
    }
}

@Composable
private fun SurahOfflineAudioCard(
    surah: SurahInfo,
    reciter: ReciterInfo,
    downloadState: SurahDownloadState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onPlayFirst: () -> Unit
) {
    NoorGoldCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("surah_offline_audio_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        imageVector = if (downloadState.status == DownloadStatus.COMPLETED) Icons.Filled.CloudDone else Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = if (downloadState.status == DownloadStatus.COMPLETED) MaterialTheme.colorScheme.primary else NoorTheme.colors.goldAccent
                    )
                    Column {
                        Text(
                            text = if (downloadState.status == DownloadStatus.COMPLETED) {
                                "تلاوة السورة محمّلة بدون نت ✓"
                            } else {
                                "تحميل التلاوة للاستماع بدون نت"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "القارئ: ${reciter.nameArabic}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                when (downloadState.status) {
                    DownloadStatus.IDLE, DownloadStatus.ERROR -> {
                        OutlinedButton(
                            onClick = onDownload,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NoorTheme.colors.goldAccent),
                            modifier = Modifier.testTag("download_surah_audio_button")
                        ) {
                            Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تحميل")
                        }
                    }

                    DownloadStatus.DOWNLOADING -> {
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.testTag("cancel_surah_audio_button")
                        ) {
                            Text("إلغاء", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    DownloadStatus.COMPLETED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onPlayFirst,
                                modifier = Modifier.testTag("play_offline_surah_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "تشغيل بدون نت",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.testTag("delete_surah_audio_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "حذف التلاوة المحملة",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            if (downloadState.status == DownloadStatus.DOWNLOADING) {
                val total = downloadState.totalAyahs.coerceAtLeast(1)
                val progress = downloadState.completedAyahs.toFloat() / total.toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = NoorTheme.colors.goldBorder
                )
                Text(
                    text = "جارٍ تحميل الآية ${ArabicTextUtils.toEasternArabicNumerals(downloadState.completedAyahs)} من ${ArabicTextUtils.toEasternArabicNumerals(surah.ayahCount)}...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (!downloadState.errorMessage.isNullOrBlank()) {
                Text(
                    text = downloadState.errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SourceVerificationLinkChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = NoorTheme.colors.badgeEmeraldBg.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, NoorTheme.colors.goldBorder),
        modifier = modifier.fillMaxWidth()
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
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
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
                    contentDescription = "فتح الرابط للتحقق",
                    tint = NoorTheme.colors.goldAccent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun AyahStepperBox(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    NoorGoldCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDecrement) { Text("-") }
                Text(
                    text = ArabicTextUtils.toEasternArabicNumerals(value),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onIncrement) { Text("+") }
            }
        }
    }
}
