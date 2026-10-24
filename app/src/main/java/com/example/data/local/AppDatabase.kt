package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SongEntity::class,
        CategoryEntity::class,
        UserProfileEntity::class,
        ArchivePhotoEntity::class,
        ScheduleItemEntity::class,
        TraktaEntity::class
    ],
    version = 16,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            val appCtx = context.applicationContext
            HymnCatalog.ensureInitializedAsync(appCtx)
            LocalContentCatalog.ensureTraktasLoadedAsync(appCtx)
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    appCtx,
                    AppDatabase::class.java,
                    "jesosy_mamonjy_daoud_offline.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                CoroutineScope(Dispatchers.IO).launch {
                    HymnCatalog.ensureInitialized(appCtx)
                    seedIfEmpty(appCtx, instance.songDao())
                }
                instance
            }
        }

        private suspend fun seedIfEmpty(context: Context, dao: SongDao) {
            val catalogTraktas = LocalContentCatalog.loadTraktaEntities(context)
            if (catalogTraktas.isNotEmpty() && dao.getTraktaCount() != catalogTraktas.size) {
                dao.clearTraktaItems()
                dao.insertTraktaItems(catalogTraktas)
            }

            if (dao.getScheduleItemCount() == 0) {
                val defaultSchedule = listOf(
                    ScheduleItemEntity(
                        id = 1,
                        groupType = "ALAHADY",
                        title = "Sekoly Alahady sy Tanora",
                        subtitle = "Fianarana Soratra Masina",
                        timeRange = "08H30-09H30",
                        details = "Fotoana fianarana Soratra Masina ho an'ny ankizy, ny zatovo ary ny tanora rehetra ao amin'ny Fiangonana Jesosy Mamonjy M. A. Daoud.",
                        isHighlighted = false
                    ),
                    ScheduleItemEntity(
                        id = 2,
                        groupType = "ALAHADY",
                        title = "Maraina",
                        subtitle = "Hira, Vavaka sy Toriteny",
                        timeRange = "10H-12H",
                        details = "Fanompoam-pivavahana lehibe Alahady maraina: Fiderana sy fihirana, vavaka fifonana sy fisaorana, ary fitoriana ny Tenin'Andriamanitra.",
                        isHighlighted = true
                    ),
                    ScheduleItemEntity(
                        id = 3,
                        groupType = "ALAHADY",
                        title = "Hariva",
                        subtitle = "Hira, Vavaka sy Toriteny",
                        timeRange = "15H-17H",
                        details = "Fanompoam-pivavahana Alahady hariva: Hira fiderana, vavaka fangatahana ny Fanahy Masina sy fanasitranana, ary toriteny.",
                        isHighlighted = false
                    ),
                    ScheduleItemEntity(
                        id = 4,
                        groupType = "VAVAKA ANDAVANANDRO",
                        title = "Talata-Zoma",
                        subtitle = "Vavaka maraina",
                        timeRange = "05H-06H",
                        details = "Fotoam-bavaka maraina isan'andro (Talata hatramin'ny Zoma) hitondrana am-bavaka ny fiangonana, ny ankohonana ary ny firenena.",
                        isHighlighted = false
                    ),
                    ScheduleItemEntity(
                        id = 5,
                        groupType = "VAVAKA ANDAVANANDRO",
                        title = "Talata sy Alakamisy",
                        subtitle = "Fotoam-bavaka hariva",
                        timeRange = "17H-18H",
                        details = "Fotoam-bavaka sy fampianarana Soratra Masina isaky ny Talata sy Alakamisy hariva.",
                        isHighlighted = false
                    )
                )
                dao.insertScheduleItems(defaultSchedule)
            }

            val catalogPhotos = LocalContentCatalog.loadArchivePhotoEntities(context)
            if (catalogPhotos.isNotEmpty() && dao.getArchivePhotoCount() != catalogPhotos.size) {
                dao.clearArchivePhotos()
                dao.insertArchivePhotos(catalogPhotos)
            }

            if (dao.getUserProfileCount() == 0) {
                dao.saveUserProfile(
                    UserProfileEntity(
                        id = 1,
                        displayName = "Frère Aina",
                        gender = "male",
                        region = "Ambohipo foibe",
                        themeMode = "jour",
                        fontSizeSp = 18,
                        photoUri = "",
                        isOnboardingCompleted = false
                    )
                )
            }

            val defaultCategories = listOf(
                CategoryEntity("1_trakta", "Trakta", "13 boky", 13),
                CategoryEntity("2_fotoana", "Fotoana", "Ora fiangonana", 5),
                CategoryEntity("3_rakitsary", "Rakitsary", "Sary tahiry", 44)
            )
            dao.insertCategories(defaultCategories)
        }
    }
}
