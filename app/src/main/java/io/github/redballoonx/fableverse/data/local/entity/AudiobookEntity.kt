package io.github.redballoonx.fableverse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audiobooks",
    foreignKeys = [
        ForeignKey(
            entity = AuthorEntity::class,
            parentColumns = ["id"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("authorId")]
)
data class AudiobookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val folderUri: String,
    val authorId: Long,
    val albumTitle: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val coverUri: String? = null,
    val totalDuration: Long = 0,
    val currentPosition: Long = 0,
    val currentChapterIndex: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerEndTime: Long? = null,
    val isFinished: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastPlayed: Long? = null
)