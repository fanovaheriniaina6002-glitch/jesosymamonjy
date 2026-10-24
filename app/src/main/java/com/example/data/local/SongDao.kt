package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY number ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isFeatured = 1 LIMIT 1")
    fun getFeaturedSong(): Flow<SongEntity?>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY number ASC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query(
        """
        SELECT * FROM songs 
        WHERE CAST(number AS TEXT) LIKE '%' || :query || '%' 
           OR title LIKE '%' || :query || '%' 
           OR lyrics LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%'
        ORDER BY number ASC
        """
    )
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE number = :songNumber LIMIT 1")
    suspend fun getSongByNumber(songNumber: Int): SongEntity?

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE number = :songNumber")
    suspend fun updateFavoriteStatus(songNumber: Int, isFavorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Query("SELECT COUNT(*) FROM user_profile")
    suspend fun getUserProfileCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM archive_photos ORDER BY id ASC")
    fun getAllArchivePhotos(): Flow<List<ArchivePhotoEntity>>

    @Query("SELECT COUNT(*) FROM archive_photos")
    suspend fun getArchivePhotoCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchivePhotos(photos: List<ArchivePhotoEntity>)

    @Query("DELETE FROM archive_photos")
    suspend fun clearArchivePhotos()

    @Query("SELECT * FROM schedule_items ORDER BY id ASC")
    fun getAllScheduleItems(): Flow<List<ScheduleItemEntity>>

    @Query("SELECT COUNT(*) FROM schedule_items")
    suspend fun getScheduleItemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleItems(items: List<ScheduleItemEntity>)

    @Query("SELECT * FROM trakta_items ORDER BY id ASC")
    fun getAllTraktaItems(): Flow<List<TraktaEntity>>

    @Query("SELECT COUNT(*) FROM trakta_items")
    suspend fun getTraktaCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTraktaItems(items: List<TraktaEntity>)

    @Query("DELETE FROM trakta_items")
    suspend fun clearTraktaItems()
}
