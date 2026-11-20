package com.example

import android.app.Application
import com.example.audio.QuranAudioController
import com.example.audio.QuranAudioDownloadManager
import com.example.data.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.AzkarRepository
import com.example.data.repository.HadithRepository
import com.example.data.repository.IslamicToolkitRepository
import com.example.data.repository.QuranRepository

class NoorSakinahApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var quranRepository: QuranRepository
        private set

    lateinit var azkarRepository: AzkarRepository
        private set

    lateinit var hadithRepository: HadithRepository
        private set

    lateinit var toolkitRepository: IslamicToolkitRepository
        private set

    lateinit var backupManager: BackupManager
        private set

    lateinit var audioDownloadManager: QuranAudioDownloadManager
        private set

    lateinit var audioController: QuranAudioController
        private set

    lateinit var quizRepository: com.example.data.repository.QuizRepository
        private set

    lateinit var deedsRepository: com.example.data.repository.DeedsRepository
        private set

    lateinit var shaarawyRepository: com.example.data.repository.ShaarawyRepository
        private set

    lateinit var shaarawyAudioPlayer: com.example.audio.ShaarawyAudioPlayer
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        preferencesRepository = UserPreferencesRepository(this)
        quranRepository = QuranRepository(
            context = this,
            bookmarkDao = database.bookmarkDao(),
            cachedAyahDao = database.cachedAyahDao()
        )
        azkarRepository = AzkarRepository(
            context = this,
            customZikrDao = database.customZikrDao()
        )
        hadithRepository = HadithRepository(
            context = this,
            nawawiHadithDao = database.nawawiHadithDao(),
            favoriteHadithDao = database.favoriteHadithDao()
        )
        toolkitRepository = IslamicToolkitRepository(this)
        backupManager = BackupManager(database, preferencesRepository)
        audioDownloadManager = QuranAudioDownloadManager(this)
        audioController = QuranAudioController(this, audioDownloadManager).apply {
            setSurahLookup { id -> quranRepository.getSurahById(id) }
        }
        quizRepository = com.example.data.repository.QuizRepository(this)
        deedsRepository = com.example.data.repository.DeedsRepository(this)
        shaarawyRepository = com.example.data.repository.ShaarawyRepository(this)
        shaarawyAudioPlayer = com.example.audio.ShaarawyAudioPlayer(this)
    }
}
