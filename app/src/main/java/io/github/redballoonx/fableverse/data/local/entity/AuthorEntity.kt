package io.github.redballoonx.fableverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "authors")
data class AuthorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val bookCount: Int = 0  // Anzahl der Bücher (wird beim Scannen gesetzt)
)