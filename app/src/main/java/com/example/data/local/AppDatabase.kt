package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BookmarkEntity::class,
        CustomZikrEntity::class,
        CachedAyahEntity::class,
        NawawiHadithEntity::class,
        FavoriteHadithEntity::class,
        DailyPrayerLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun customZikrDao(): CustomZikrDao
    abstract fun cachedAyahDao(): CachedAyahDao
    abstract fun nawawiHadithDao(): NawawiHadithDao
    abstract fun favoriteHadithDao(): FavoriteHadithDao
    abstract fun prayerLogDao(): PrayerLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorite_hadiths` (`hadithId` TEXT NOT NULL, `savedAt` INTEGER NOT NULL, PRIMARY KEY(`hadithId`))"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `nawawi_hadiths` (
                        `number` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `narrator` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `grade` TEXT NOT NULL,
                        `sourceUrl` TEXT NOT NULL,
                        `isFavorite` INTEGER NOT NULL DEFAULT 0,
                        `favoritedAt` INTEGER,
                        PRIMARY KEY(`number`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_prayer_logs` (
                        `dateKey` TEXT NOT NULL,
                        `fajrDone` INTEGER NOT NULL DEFAULT 0,
                        `dhuhrDone` INTEGER NOT NULL DEFAULT 0,
                        `asrDone` INTEGER NOT NULL DEFAULT 0,
                        `maghribDone` INTEGER NOT NULL DEFAULT 0,
                        `ishaDone` INTEGER NOT NULL DEFAULT 0,
                        `rawatibDone` INTEGER NOT NULL DEFAULT 0,
                        `duhaDone` INTEGER NOT NULL DEFAULT 0,
                        `witrDone` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`dateKey`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "noor_sakinah.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
