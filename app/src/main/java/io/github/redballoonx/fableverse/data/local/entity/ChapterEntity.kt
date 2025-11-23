package io.github.redballoonx.fableverse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = AudiobookEntity::class,
            parentColumns = ["id"],
            childColumns = ["audiobookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("audiobookId")]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val audiobookId: Long,
    val title: String,
    val uri: String,
    val position: Int,           // Reihenfolge: 0, 1, 2...
    val duration: Long = 0       // In Millisekunden
)