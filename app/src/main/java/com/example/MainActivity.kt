package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AsmaulHusnaScreen
import com.example.ui.screens.AzkarListScreen
import com.example.ui.screens.DuaEncyclopediaScreen
import com.example.ui.screens.HadithScreen
import com.example.ui.screens.HijriCalendarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OfflineDownloadsSheet
import com.example.ui.screens.PrayerTrackerScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.screens.QuranIndexScreen
import com.example.ui.screens.QuranReaderScreen
import com.example.ui.screens.SettingsReminderSheet
import com.example.ui.screens.SourcesSheet
import com.example.ui.screens.TasbeehScreen
import com.example.ui.screens.ZakatCalculatorScreen
import com.example.ui.screens.ZikrDetailScreen
import com.example.ui.theme.NoorSakinahTheme
import com.example.ui.theme.NoorTheme
import com.example.ui.viewmodel.AppPreferencesViewModel
import com.example.ui.viewmodel.AzkarViewModel
import com.example.ui.viewmodel.HadithViewModel
import com.example.ui.viewmodel.QiblaViewModel
import com.example.ui.viewmodel.QuranViewModel
import com.example.ui.viewmodel.TasbeehViewModel
import com.example.ui.viewmodel.ToolkitViewModel
import java.util.Locale

object Routes {
    const val HOME = "home"
    const val QURAN = "quran"
    const val AZKAR = "azkar"
    const val HADITH = "hadith"
    const val QIBLA = "qibla"
    const val TASBEEH = "tasbeeh"
    const val ASMAUL_HUSNA = "asmaul_husna"
    const val DUAS = "duas"
    const val ZAKAT = "zakat"
    const val HIJRI_CALENDAR = "hijri_calendar"
    const val PRAYER_TRACKER = "prayer_tracker"
    const val QURAN_READER = "quran_reader/{surahId}/{targetAyah}"
    const val ZIKR_DETAIL = "zikr_detail/{categoryId}"

    fun quranReader(surahId: Int, targetAyah: Int = 1) = "quran_reader/$surahId/$targetAyah"
    fun zikrDetail(categoryId: String) = "zikr_detail/$categoryId"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {

    private var pendingAzkarRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enforceArabicRtlConfiguration()
        enableEdgeToEdge()
        pendingAzkarRoute = intent?.getStringExtra("open_azkar_category")

        setContent {
            val prefsViewModel: AppPreferencesViewModel = viewModel()
            val quranViewModel: QuranViewModel = viewModel()
            val azkarViewModel: AzkarViewModel = viewModel()
            val hadithViewModel: HadithViewModel = viewModel()
            val tasbeehViewModel: TasbeehViewModel = viewModel()
            val qiblaViewModel: QiblaViewModel = viewModel()
            val toolkitViewModel: ToolkitViewModel = viewModel()

            val settings by prefsViewModel.settings.collectAsStateWithLifecycle()

            NoorSakinahTheme(darkTheme = settings.isDarkMode) {
                NoorSakinahRoot(
                    prefsViewModel = prefsViewModel,
                    quranViewModel = quranViewModel,
                    azkarViewModel = azkarViewModel,
                    hadithViewModel = hadithViewModel,
                    tasbeehViewModel = tasbeehViewModel,
                    qiblaViewModel = qiblaViewModel,
                    toolkitViewModel = toolkitViewModel,
                    pendingAzkarCategory = pendingAzkarRoute,
                    onConsumePendingAzkar = { pendingAzkarRoute = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingAzkarRoute = intent.getStringExtra("open_azkar_category")
    }

    private fun enforceArabicRtlConfiguration() {
        val arabicLocale = Locale("ar")
        Locale.setDefault(arabicLocale)
        val config = resources.configuration
        config.setLocale(arabicLocale)
        config.setLayoutDirection(arabicLocale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

@Composable
private fun NoorSakinahRoot(
    prefsViewModel: AppPreferencesViewModel,
    quranViewModel: QuranViewModel,
    azkarViewModel: AzkarViewModel,
    hadithViewModel: HadithViewModel,
    tasbeehViewModel: TasbeehViewModel,
    qiblaViewModel: QiblaViewModel,
    toolkitViewModel: ToolkitViewModel,
    pendingAzkarCategory: String?,
    onConsumePendingAzkar: () -> Unit
) {
    val navController = rememberNavController()
    val settings by prefsViewModel.settings.collectAsStateWithLifecycle()
    val backupStatusMessage by prefsViewModel.backupStatusMessage.collectAsStateWithLifecycle()
    val muezzinState by prefsViewModel.muezzinState.collectAsStateWithLifecycle()
    val prayerSchedule by prefsViewModel.prayerSchedule.collectAsStateWithLifecycle()
    val khatmaSummary by prefsViewModel.khatmaSummary.collectAsStateWithLifecycle()
    val bookmarks by quranViewModel.bookmarks.collectAsStateWithLifecycle()
    val offlineSurahIds by quranViewModel.offlineSurahIds.collectAsStateWithLifecycle()
    val fullDownloadState by quranViewModel.fullDownloadState.collectAsStateWithLifecycle()
    val audioInventory by quranViewModel.audioInventory.collectAsStateWithLifecycle()
    val totalAudioBytes by quranViewModel.totalAudioBytes.collectAsStateWithLifecycle()
    val searchQuery by quranViewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by quranViewModel.searchResults.collectAsStateWithLifecycle()
    val readerContent by quranViewModel.readerContent.collectAsStateWithLifecycle()
    val audioState by quranViewModel.audioState.collectAsStateWithLifecycle()
    val downloadsState by quranViewModel.downloadsState.collectAsStateWithLifecycle()
    val customAzkar by azkarViewModel.customAzkar.collectAsStateWithLifecycle()
    val hadiths by hadithViewModel.hadiths.collectAsStateWithLifecycle()
    val hadithQuery by hadithViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedHadithCategory by hadithViewModel.selectedCategory.collectAsStateWithLifecycle()
    val favoriteHadithIds by hadithViewModel.favoriteIds.collectAsStateWithLifecycle()
    val nawawiSyncState by hadithViewModel.nawawiSyncState.collectAsStateWithLifecycle()
    val qiblaState by qiblaViewModel.uiState.collectAsStateWithLifecycle()
    val asmaQuery by toolkitViewModel.asmaQuery.collectAsStateWithLifecycle()
    val asmaResults by toolkitViewModel.asmaResults.collectAsStateWithLifecycle()
    val duaQuery by toolkitViewModel.duaQuery.collectAsStateWithLifecycle()
    val selectedDuaCategory by toolkitViewModel.selectedDuaCategory.collectAsStateWithLifecycle()
    val duaResults by toolkitViewModel.duaResults.collectAsStateWithLifecycle()
    val zakatInput by toolkitViewModel.zakatInput.collectAsStateWithLifecycle()
    val zakatResult by toolkitViewModel.zakatResult.collectAsStateWithLifecycle()
    val hijriMonthSummary by toolkitViewModel.hijriMonthSummary.collectAsStateWithLifecycle()
    val hijriMonthOffset by toolkitViewModel.hijriMonthOffset.collectAsStateWithLifecycle()
    val todayPrayerLog by toolkitViewModel.todayPrayerLog.collectAsStateWithLifecycle()
    val recentPrayerLogs by toolkitViewModel.recentPrayerLogs.collectAsStateWithLifecycle()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSourcesSheet by remember { mutableStateOf(false) }
    var showOfflineDownloadsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(pendingAzkarCategory) {
        if (!pendingAzkarCategory.isNullOrBlank()) {
            navController.navigate(Routes.zikrDetail(pendingAzkarCategory))
            onConsumePendingAzkar()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showNavigationChrome = currentRoute in setOf(
        Routes.HOME,
        Routes.QURAN,
        Routes.AZKAR,
        Routes.HADITH,
        Routes.QIBLA,
        Routes.TASBEEH
    )

    val phoneTabs = remember {
        listOf(
            BottomNavTab(
                route = Routes.HOME,
                label = "الرئيسية",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                testTag = "nav_home"
            ),
            BottomNavTab(
                route = Routes.QURAN,
                label = "القرآن",
                selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
                unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
                testTag = "nav_quran"
            ),
            BottomNavTab(
                route = Routes.AZKAR,
                label = "الأذكار",
                selectedIcon = Icons.Filled.Favorite,
                unselectedIcon = Icons.Outlined.FavoriteBorder,
                testTag = "nav_azkar"
            ),
            BottomNavTab(
                route = Routes.HADITH,
                label = "الأحاديث",
                selectedIcon = Icons.Filled.AutoStories,
                unselectedIcon = Icons.Outlined.AutoStories,
                testTag = "nav_hadith"
            ),
            BottomNavTab(
                route = Routes.TASBEEH,
                label = "التسبيح",
                selectedIcon = Icons.Filled.TouchApp,
                unselectedIcon = Icons.Outlined.TouchApp,
                testTag = "nav_tasbeeh"
            )
        )
    }

    val tabletTabs = remember {
        phoneTabs + BottomNavTab(
            route = Routes.QIBLA,
            label = "القبلة",
            selectedIcon = Icons.Filled.Explore,
            unselectedIcon = Icons.Outlined.Explore,
            testTag = "nav_qibla"
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showNavigationChrome && !isExpandedScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        tonalElevation = 4.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        phoneTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTopTab(navController, tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = NoorTheme.colors.badgeEmeraldBg
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showNavigationChrome && isExpandedScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ) {
                        tabletTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTopTab(navController, tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = NoorTheme.colors.badgeEmeraldBg
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            settings = settings,
                            dailyVerse = prefsViewModel.dailyVerse,
                            bookmarksCount = bookmarks.size,
                            prayerSchedule = prayerSchedule,
                            khatmaSummary = khatmaSummary,
                            offlineSurahCount = offlineSurahIds.size,
                            totalAudioBytes = totalAudioBytes,
                            onToggleTheme = { prefsViewModel.toggleDarkMode() },
                            onOpenSettings = { showSettingsSheet = true },
                            onContinueReading = { sId, aNum ->
                                navController.navigate(Routes.quranReader(sId, aNum))
                            },
                            onOpenAzkarCategory = { catId ->
                                navController.navigate(Routes.zikrDetail(catId))
                            },
                            onOpenTasbeeh = {
                                navigateToTopTab(navController, Routes.TASBEEH)
                            },
                            onOpenQuranTab = {
                                navigateToTopTab(navController, Routes.QURAN)
                            },
                            onOpenQibla = {
                                navController.navigate(Routes.QIBLA)
                            },
                            onOpenSources = {
                                showSourcesSheet = true
                            },
                            onOpenOfflineManager = {
                                quranViewModel.refreshAudioInventory()
                                showOfflineDownloadsSheet = true
                            },
                            onOpenPrayerAlerts = { showSettingsSheet = true },
                            onOpenAsmaulHusna = { navController.navigate(Routes.ASMAUL_HUSNA) },
                            onOpenDuaEncyclopedia = { navController.navigate(Routes.DUAS) },
                            onOpenZakatCalculator = { navController.navigate(Routes.ZAKAT) },
                            onOpenHijriCalendar = { navController.navigate(Routes.HIJRI_CALENDAR) },
                            onOpenPrayerTracker = { navController.navigate(Routes.PRAYER_TRACKER) },
                            onSelectPrayerCityAndMethod = prefsViewModel::setPrayerCityAndMethod,
                            onSelectKhatmaDuration = prefsViewModel::setKhatmaDuration,
                            onLogKhatmaToday = prefsViewModel::logKhatmaToday,
                            onResetKhatma = prefsViewModel::resetKhatma
                        )
                    }

                    composable(Routes.QURAN) {
                        QuranIndexScreen(
                            searchQuery = searchQuery,
                            searchResults = searchResults,
                            juzList = quranViewModel.allJuz,
                            bookmarks = bookmarks,
                            selectedReciter = audioState.reciter,
                            downloadsState = downloadsState,
                            offlineSurahIds = offlineSurahIds,
                            totalAudioBytes = totalAudioBytes,
                            getDownloadState = quranViewModel::getDownloadState,
                            onSearchQueryChange = quranViewModel::updateSearchQuery,
                            onOpenSurah = { sId, targetAyah ->
                                navController.navigate(Routes.quranReader(sId, targetAyah))
                            },
                            onRemoveBookmark = quranViewModel::removeBookmark,
                            onDownloadSurahAudio = quranViewModel::downloadSurahAudio,
                            onCancelSurahDownload = quranViewModel::cancelSurahDownload,
                            onDeleteSurahAudio = quranViewModel::deleteSurahAudio,
                            onOpenOfflineManager = {
                                quranViewModel.refreshAudioInventory()
                                showOfflineDownloadsSheet = true
                            }
                        )
                    }

                    composable(Routes.AZKAR) {
                        AzkarListScreen(
                            categories = azkarViewModel.builtInCategories,
                            customAzkar = customAzkar,
                            settings = settings,
                            onOpenCategory = { catId ->
                                navController.navigate(Routes.zikrDetail(catId))
                            },
                            onAddCustomZikr = azkarViewModel::addCustomZikr,
                            onUpdateCustomZikr = azkarViewModel::updateCustomZikr,
                            onDeleteCustomZikr = azkarViewModel::deleteCustomZikr,
                            onOpenReminders = { showSettingsSheet = true }
                        )
                    }

                    composable(Routes.HADITH) {
                        HadithScreen(
                            hadiths = hadiths,
                            categories = hadithViewModel.categories,
                            selectedCategory = selectedHadithCategory,
                            searchQuery = hadithQuery,
                            favoriteIds = favoriteHadithIds,
                            nawawiSyncState = nawawiSyncState,
                            onSearchQueryChange = hadithViewModel::updateSearchQuery,
                            onSelectCategory = hadithViewModel::selectCategory,
                            onToggleFavorite = hadithViewModel::toggleFavorite,
                            onSyncNawawiFullTexts = hadithViewModel::syncNawawiFullTexts
                        )
                    }

                    composable(Routes.QIBLA) {
                        QiblaScreen(
                            uiState = qiblaState,
                            onStartSensors = qiblaViewModel::startSensors,
                            onStopSensors = qiblaViewModel::stopSensors,
                            onPermissionResult = qiblaViewModel::onPermissionResult,
                            onRefreshGps = qiblaViewModel::fetchCurrentGpsLocation,
                            onSelectCity = qiblaViewModel::selectCityPreset
                        )
                    }

                    composable(Routes.TASBEEH) {
                        TasbeehScreen(
                            settings = settings,
                            presetPhrases = tasbeehViewModel.presetPhrases,
                            targetPresets = tasbeehViewModel.targetPresets,
                            onIncrement = tasbeehViewModel::increment,
                            onResetCycle = tasbeehViewModel::resetCycle,
                            onSelectTarget = tasbeehViewModel::selectTarget,
                            onSelectPhrase = tasbeehViewModel::selectPhrase,
                            onToggleHaptics = tasbeehViewModel::toggleHaptics
                        )
                    }

                    composable(Routes.ASMAUL_HUSNA) {
                        AsmaulHusnaScreen(
                            items = asmaResults,
                            searchQuery = asmaQuery,
                            onSearchQueryChange = toolkitViewModel::updateAsmaQuery,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.DUAS) {
                        DuaEncyclopediaScreen(
                            duas = duaResults,
                            categories = toolkitViewModel.duaCategories,
                            selectedCategory = selectedDuaCategory,
                            searchQuery = duaQuery,
                            onSearchQueryChange = toolkitViewModel::updateDuaQuery,
                            onSelectCategory = toolkitViewModel::selectDuaCategory,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.ZAKAT) {
                        ZakatCalculatorScreen(
                            input = zakatInput,
                            result = zakatResult,
                            onUpdateInput = toolkitViewModel::updateZakatInput,
                            onReset = toolkitViewModel::resetZakatInput,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.HIJRI_CALENDAR) {
                        HijriCalendarScreen(
                            summary = hijriMonthSummary,
                            monthOffset = hijriMonthOffset,
                            onPrevMonth = toolkitViewModel::previousHijriMonth,
                            onNextMonth = toolkitViewModel::nextHijriMonth,
                            onResetToday = toolkitViewModel::resetHijriMonthToToday,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.PRAYER_TRACKER) {
                        PrayerTrackerScreen(
                            todayLog = todayPrayerLog,
                            recentLogs = recentPrayerLogs,
                            onToggleSlot = toolkitViewModel::togglePrayerTrackerSlot,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Routes.QURAN_READER,
                        arguments = listOf(
                            navArgument("surahId") { type = NavType.IntType },
                            navArgument("targetAyah") { type = NavType.IntType }
                        )
                    ) { backStackEntry ->
                        val surahId = backStackEntry.arguments?.getInt("surahId") ?: 1
                        val targetAyah = backStackEntry.arguments?.getInt("targetAyah") ?: 1
                        QuranReaderScreen(
                            surahId = surahId,
                            targetAyah = targetAyah,
                            content = readerContent,
                            allSurahs = quranViewModel.allSurahs,
                            settings = settings,
                            bookmarks = bookmarks,
                            audioState = audioState,
                            downloadsState = downloadsState,
                            getDownloadState = quranViewModel::getDownloadState,
                            onBack = { navController.popBackStack() },
                            onLoadSurah = quranViewModel::openSurah,
                            onSaveLastRead = quranViewModel::saveLastRead,
                            onToggleBookmark = quranViewModel::toggleBookmark,
                            onAdjustFontSize = quranViewModel::adjustFontSize,
                            onSelectViewMode = quranViewModel::setMushafViewMode,
                            onPlayAyah = quranViewModel::playAyah,
                            onTogglePlayPause = quranViewModel::togglePlayPause,
                            onNextAyah = quranViewModel::nextAyah,
                            onPreviousAyah = quranViewModel::previousAyah,
                            onStopAudio = quranViewModel::stopAudio,
                            onSelectReciter = quranViewModel::selectReciter,
                            onConfigureRangeRepeat = quranViewModel::configureRangeRepeat,
                            onSetPlaybackSpeed = quranViewModel::setPlaybackSpeed,
                            onSetAutoAdvanceNextSurah = quranViewModel::setAutoAdvanceNextSurah,
                            onSetSleepTimer = quranViewModel::setSleepTimer,
                            onDownloadSurahAudio = quranViewModel::downloadSurahAudio,
                            onCancelSurahDownload = quranViewModel::cancelSurahDownload,
                            onDeleteSurahAudio = quranViewModel::deleteSurahAudio
                        )
                    }

                    composable(
                        route = Routes.ZIKR_DETAIL,
                        arguments = listOf(
                            navArgument("categoryId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val categoryId = backStackEntry.arguments?.getString("categoryId") ?: "morning"
                        val category = azkarViewModel.getCategory(categoryId)
                        ZikrDetailScreen(
                            category = category,
                            settings = settings,
                            onBack = { navController.popBackStack() },
                            onToggleTashkeel = azkarViewModel::setShowTashkeel,
                            onToggleFadl = azkarViewModel::setShowFadl,
                            onCategoryCompleted = azkarViewModel::markCategoryCompleted
                        )
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        SettingsReminderSheet(
            settings = settings,
            backupStatusMessage = backupStatusMessage,
            muezzinState = muezzinState,
            onDismiss = { showSettingsSheet = false },
            onSetDarkMode = prefsViewModel::setDarkMode,
            onSetFontSize = prefsViewModel::setQuranFontSize,
            onUpdateMorningReminder = prefsViewModel::updateMorningReminder,
            onUpdateEveningReminder = prefsViewModel::updateEveningReminder,
            onSetPrayerAlertsEnabled = prefsViewModel::setPrayerAlertsEnabled,
            onSetPrayerSoundMode = prefsViewModel::setPrayerSoundMode,
            onSelectMuezzin = prefsViewModel::selectMuezzin,
            onDownloadMuezzin = prefsViewModel::downloadMuezzinAdhan,
            onDeleteMuezzin = prefsViewModel::deleteMuezzinAdhan,
            onSetPrayerPreAlertMinutes = prefsViewModel::setPrayerPreAlertMinutes,
            onTogglePrayerTypeAlert = prefsViewModel::togglePrayerTypeAlert,
            onExportBackup = prefsViewModel::exportBackup,
            onImportBackup = prefsViewModel::importBackup,
            onClearBackupStatus = prefsViewModel::clearBackupStatusMessage,
            onOpenSources = { showSourcesSheet = true },
            onOpenOfflineManager = {
                quranViewModel.refreshAudioInventory()
                showOfflineDownloadsSheet = true
            }
        )
    }

    if (showOfflineDownloadsSheet) {
        OfflineDownloadsSheet(
            offlineSurahCount = offlineSurahIds.size,
            fullDownloadState = fullDownloadState,
            audioInventory = audioInventory,
            totalAudioBytes = totalAudioBytes,
            allSurahs = quranViewModel.allSurahs,
            onStartFullQuranDownload = quranViewModel::startFullQuranDownload,
            onCancelFullQuranDownload = quranViewModel::cancelFullQuranDownload,
            onClearCachedQuran = quranViewModel::clearCachedQuran,
            onDeleteSurahAudio = quranViewModel::deleteSurahAudio,
            onClearAllAudio = quranViewModel::clearAllDownloadedAudio,
            onDismiss = { showOfflineDownloadsSheet = false }
        )
    }

    if (showSourcesSheet) {
        SourcesSheet(
            onDismiss = { showSourcesSheet = false }
        )
    }
}

private fun navigateToTopTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
