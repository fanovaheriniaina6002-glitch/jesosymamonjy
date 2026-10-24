package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val number: Int,
    val id: Int = number,
    val numberStr: String = number.toString(),
    val title: String,
    val category: String,
    val snippet: String = "",
    val preview: String = snippet,
    val fullLyrics: String = "",
    val lyrics: String = fullLyrics,
    val titleLower: String = "",
    val searchIndex: String = "",
    val verse1: String = "",
    val chorus: String = "",
    val verse2: String = "",
    val verse3: String = "",
    val isFavorite: Boolean = false,
    val isFeatured: Boolean = false
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subtitle: String,
    val songCount: Int,
    val title: String = name
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String = "Frère Aina",
    val gender: String = "male",
    val region: String = "Ambohipo foibe",
    val themeMode: String = "jour",
    val fontSizeSp: Int = 18,
    val photoUri: String = "",
    val isOnboardingCompleted: Boolean = false
)

@Entity(tableName = "archive_photos")
data class ArchivePhotoEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val subtitle: String,
    val year: String,
    val location: String,
    val category: String,
    val description: String,
    val drawableKey: String,
    val isFeatured: Boolean = false
)

@Entity(tableName = "schedule_items")
data class ScheduleItemEntity(
    @PrimaryKey val id: Int,
    val groupType: String,
    val title: String,
    val subtitle: String,
    val timeRange: String,
    val details: String,
    val isHighlighted: Boolean = false
)

@Entity(tableName = "trakta_items")
data class TraktaEntity(
    @PrimaryKey val id: Int,
    val numberLabel: String,
    val title: String,
    val author: String = "",
    val coverKey: String,
    val coverBgHex: String,
    val content: String,
    val isOffline: Boolean = true
)

fun getTraktaAuthor(traktaId: Int): String {
    return when (traktaId) {
        4, 9 -> "Jane Collins"
        3 -> "Fr et Sr Daoud"
        2, 8, 12 -> "M. A. DAOUD"
        else -> ""
    }
}

