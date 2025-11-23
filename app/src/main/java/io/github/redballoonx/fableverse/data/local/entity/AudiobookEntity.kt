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
    val coverUri: String? = null,
    val totalDuration: Long = 0,        // In Millisekunden
    val currentPosition: Long = 0,      // Wo der User gestoppt hat
    val isFinished: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastPlayed: Long? = null
)