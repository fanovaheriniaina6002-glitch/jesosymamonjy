package com.example.data.repository

import android.content.Context
import com.example.data.local.ArchivePhotoEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.HymnCatalog
import com.example.data.local.ScheduleItemEntity
import com.example.data.local.SongDao
import com.example.data.local.SongEntity
import com.example.data.local.TraktaEntity
import com.example.data.local.UserProfileEntity
import kotlinx.coroutines.flow.Flow

class SongRepository(
    private val songDao: SongDao,
    private val appContext: Context? = null
) {

    // In-memory pre-indexed 830 hymns from HymnCatalog (60 FPS, 0 SQLite cursor lag)
    val allSongs: Flow<List<SongEntity>> = HymnCatalog.allSongsFlow
    val favoriteSongs: Flow<List<SongEntity>> = HymnCatalog.favoriteSongsFlow
    val historySongs: Flow<List<SongEntity>> = HymnCatalog.historySongsFlow
    val featuredSong: Flow<SongEntity?> = HymnCatalog.featuredSongFlow

    val allCategories: Flow<List<CategoryEntity>> = songDao.getAllCategories()
    val userProfile: Flow<UserProfileEntity?> = songDao.getUserProfile()
    val allArchivePhotos: Flow<List<ArchivePhotoEntity>> = songDao.getAllArchivePhotos()
    val allScheduleItems: Flow<List<ScheduleItemEntity>> = songDao.getAllScheduleItems()
    val allTraktaItems: Flow<List<TraktaEntity>> = songDao.getAllTraktaItems()

    fun searchSongs(query: String, categoryFilter: String = "REHETRA"): List<SongEntity> {
        return HymnCatalog.searchSongs(
            songs = HymnCatalog.allSongsFlow.value,
            rawQuery = query,
            categoryFilter = categoryFilter
        )
    }

    suspend fun toggleFavorite(song: SongEntity, context: Context? = appContext): Boolean {
        val ctx = context
        return if (ctx != null) {
            HymnCatalog.toggleFavorite(ctx, song.number)
        } else {
            val newState = !song.isFavorite
            songDao.updateFavoriteStatus(song.number, newState)
            newState
        }
    }

    fun recordSongConsulted(songNumber: Int, context: Context? = appContext) {
        val ctx = context ?: return
        HymnCatalog.recordSongConsulted(ctx, songNumber)
    }

    suspend fun saveUserProfile(profile: UserProfileEntity) {
        songDao.saveUserProfile(profile)
    }
}
